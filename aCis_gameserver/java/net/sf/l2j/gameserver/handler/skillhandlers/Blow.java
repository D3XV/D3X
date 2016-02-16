/*
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later
 * version.
 * 
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 * 
 * You should have received a copy of the GNU General Public License along with
 * this program. If not, see <http://www.gnu.org/licenses/>.
 */
package net.sf.l2j.gameserver.handler.skillhandlers;

import net.sf.l2j.gameserver.handler.ISkillHandler;
import net.sf.l2j.gameserver.model.L2Effect;
import net.sf.l2j.gameserver.model.L2Object;
import net.sf.l2j.gameserver.model.L2Skill;
import net.sf.l2j.gameserver.model.ShotType;
import net.sf.l2j.gameserver.model.actor.L2Character;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.network.SystemMessageId;
import net.sf.l2j.gameserver.network.serverpackets.SystemMessage;
import net.sf.l2j.gameserver.skills.Env;
import net.sf.l2j.gameserver.skills.Formulas;
import net.sf.l2j.gameserver.skills.basefuncs.Func;
import net.sf.l2j.gameserver.templates.skills.L2SkillType;

/**
 * @author Steuf
 */
public class Blow implements ISkillHandler
{
	private static final L2SkillType[] SKILL_IDS =
	{
		L2SkillType.BLOW
	};
	
	public final static int FRONT = 50;
	public final static int SIDE = 60;
	public final static int BEHIND = 70;
	
	@Override
	public void useSkill(L2Character activeChar, L2Skill skill, L2Object[] targets)
	{
		if (activeChar.isAlikeDead())
			return;
		
		final boolean ss = activeChar.isChargedShot(ShotType.SOULSHOT);
		
		for (L2Object obj : targets)
		{
			if (!(obj instanceof L2Character))
				continue;
			
			final L2Character target = ((L2Character) obj);
			if (target.isAlikeDead())
				continue;
			
			byte _successChance = SIDE;
			
			if (activeChar.isBehindTarget())
				_successChance = BEHIND;
			else if (activeChar.isInFrontOfTarget())
				_successChance = FRONT;
			
			// If skill requires Crit or skill requires behind, calculate chance based on DEX, Position and on self BUFF
			boolean success = true;
			if ((skill.getCondition() & L2Skill.COND_BEHIND) != 0)
				success = (_successChance == BEHIND);
			if ((skill.getCondition() & L2Skill.COND_CRIT) != 0)
				success = (success && Formulas.calcBlow(activeChar, target, _successChance));
			
			if (success)
			{
				// Calculate skill evasion
				boolean skillIsEvaded = Formulas.calcPhysicalSkillEvasion(target, skill);
				if (skillIsEvaded)
				{
					if (activeChar instanceof L2PcInstance)
						((L2PcInstance) activeChar).sendPacket(SystemMessage.getSystemMessage(SystemMessageId.S1_DODGES_ATTACK).addCharName(target));
					
					if (target instanceof L2PcInstance)
						((L2PcInstance) target).sendPacket(SystemMessage.getSystemMessage(SystemMessageId.AVOIDED_S1_ATTACK).addCharName(activeChar));
					
					// no futher calculations needed.
					continue;
				}
				
				// Calculate skill reflect
				final byte reflect = Formulas.calcSkillReflect(target, skill);
				if (skill.hasEffects())
				{
					if (reflect == Formulas.SKILL_REFLECT_SUCCEED)
					{
						activeChar.stopSkillEffects(skill.getId());
						skill.getEffects(target, activeChar);
						activeChar.sendPacket(SystemMessage.getSystemMessage(SystemMessageId.YOU_FEEL_S1_EFFECT).addSkillName(skill));
					}
					else
					{
						final byte shld = Formulas.calcShldUse(activeChar, target, skill);
						target.stopSkillEffects(skill.getId());
						if (Formulas.calcSkillSuccess(activeChar, target, skill, shld, true))
						{
							skill.getEffects(activeChar, target, new Env(shld, false, false, false));
							target.sendPacket(SystemMessage.getSystemMessage(SystemMessageId.YOU_FEEL_S1_EFFECT).addSkillName(skill));
						}
						else
							activeChar.sendPacket(SystemMessage.getSystemMessage(SystemMessageId.S1_RESISTED_YOUR_S2).addCharName(target).addSkillName(skill));
					}
				}
				
				byte shld = Formulas.calcShldUse(activeChar, target, skill);
				
				// Crit rate base crit rate for skill, modified with STR bonus
				boolean crit = false;
				if (Formulas.calcCrit(skill.getBaseCritRate() * 10 * Formulas.getSTRBonus(activeChar)))
					crit = true;
				
				double damage = (int) Formulas.calcBlowDamage(activeChar, target, skill, shld, ss);
				if (crit)
				{
					damage *= 2;
					
					// Vicious Stance is special after C5, and only for BLOW skills
					L2Effect vicious = activeChar.getFirstEffect(312);
					if (vicious != null && damage > 1)
					{
						for (Func func : vicious.getStatFuncs())
						{
							final Env env = new Env();
							env.setCharacter(activeChar);
							env.setTarget(target);
							env.setSkill(skill);
							env.setValue(damage);
							
							func.calc(env);
							damage = (int) env.getValue();
						}
					}
				}
				
				target.reduceCurrentHp(damage, activeChar, skill);
				
				// vengeance reflected damage
				if ((reflect & Formulas.SKILL_REFLECT_VENGEANCE) != 0)
				{
					if (target instanceof L2PcInstance)
						target.sendPacket(SystemMessage.getSystemMessage(SystemMessageId.COUNTERED_S1_ATTACK).addCharName(activeChar));
					
					if (activeChar instanceof L2PcInstance)
						activeChar.sendPacket(SystemMessage.getSystemMessage(SystemMessageId.S1_PERFORMING_COUNTERATTACK).addCharName(target));
					
					// Formula from Diego post, 700 from rpg tests
					double vegdamage = (700 * target.getPAtk(activeChar) / activeChar.getPDef(target));
					activeChar.reduceCurrentHp(vegdamage, target, skill);
				}
				
				// Manage cast break of the target (calculating rate, sending message...)
				Formulas.calcCastBreak(target, damage);
				
				if (activeChar instanceof L2PcInstance)
					((L2PcInstance) activeChar).sendDamageMessage(target, (int) damage, false, true, false);
			}
			else
				activeChar.sendPacket(SystemMessage.getSystemMessage(SystemMessageId.ATTACK_FAILED));
			
			// Possibility of a lethal strike
			Formulas.calcLethalHit(activeChar, target, skill);
			
			if (skill.hasSelfEffects())
			{
				final L2Effect effect = activeChar.getFirstEffect(skill.getId());
				if (effect != null && effect.isSelfEffect())
					effect.exit();
				
				skill.getEffectsSelf(activeChar);
			}
			activeChar.setChargedShot(ShotType.SOULSHOT, skill.isStaticReuse());
		}
	}
	
	@Override
	public L2SkillType[] getSkillIds()
	{
		return SKILL_IDS;
	}
}