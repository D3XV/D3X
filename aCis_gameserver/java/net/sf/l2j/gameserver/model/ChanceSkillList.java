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
package net.sf.l2j.gameserver.model;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import net.sf.l2j.gameserver.datatables.SkillTable;
import net.sf.l2j.gameserver.handler.ISkillHandler;
import net.sf.l2j.gameserver.handler.SkillHandler;
import net.sf.l2j.gameserver.model.actor.L2Character;
import net.sf.l2j.gameserver.network.serverpackets.MagicSkillLaunched;
import net.sf.l2j.gameserver.network.serverpackets.MagicSkillUse;
import net.sf.l2j.gameserver.skills.effects.EffectChanceSkillTrigger;
import net.sf.l2j.gameserver.templates.skills.L2SkillType;

/**
 * CT2.3: Added support for allowing effect as a chance skill trigger (DrHouse)
 * @author kombat
 */
public class ChanceSkillList extends ConcurrentHashMap<IChanceSkillTrigger, ChanceCondition>
{
	protected static final Logger _log = Logger.getLogger(ChanceSkillList.class.getName());
	private static final long serialVersionUID = 1L;
	
	private final L2Character _owner;
	
	public ChanceSkillList(L2Character owner)
	{
		super();
		_owner = owner;
	}
	
	public L2Character getOwner()
	{
		return _owner;
	}
	
	public void onHit(L2Character target, boolean ownerWasHit, boolean wasCrit)
	{
		int event;
		if (ownerWasHit)
		{
			event = ChanceCondition.EVT_ATTACKED | ChanceCondition.EVT_ATTACKED_HIT;
			if (wasCrit)
				event |= ChanceCondition.EVT_ATTACKED_CRIT;
		}
		else
		{
			event = ChanceCondition.EVT_HIT;
			if (wasCrit)
				event |= ChanceCondition.EVT_CRIT;
		}
		
		onChanceSkillEvent(event, target);
	}
	
	public void onEvadedHit(L2Character attacker)
	{
		onChanceSkillEvent(ChanceCondition.EVT_EVADED_HIT, attacker);
	}
	
	public void onSkillHit(L2Character target, boolean ownerWasHit, boolean wasMagic, boolean wasOffensive)
	{
		int event;
		if (ownerWasHit)
		{
			event = ChanceCondition.EVT_HIT_BY_SKILL;
			if (wasOffensive)
			{
				event |= ChanceCondition.EVT_HIT_BY_OFFENSIVE_SKILL;
				event |= ChanceCondition.EVT_ATTACKED;
			}
			else
			{
				event |= ChanceCondition.EVT_HIT_BY_GOOD_MAGIC;
			}
		}
		else
		{
			event = ChanceCondition.EVT_CAST;
			event |= wasMagic ? ChanceCondition.EVT_MAGIC : ChanceCondition.EVT_PHYSICAL;
			event |= wasOffensive ? ChanceCondition.EVT_MAGIC_OFFENSIVE : ChanceCondition.EVT_MAGIC_GOOD;
		}
		
		onChanceSkillEvent(event, target);
	}
	
	public void onStart()
	{
		onChanceSkillEvent(ChanceCondition.EVT_ON_START, _owner);
	}
	
	public void onActionTime()
	{
		onChanceSkillEvent(ChanceCondition.EVT_ON_ACTION_TIME, _owner);
	}
	
	public void onExit()
	{
		onChanceSkillEvent(ChanceCondition.EVT_ON_EXIT, _owner);
	}
	
	public void onChanceSkillEvent(int event, L2Character target)
	{
		if (_owner.isDead())
			return;
		
		for (Map.Entry<IChanceSkillTrigger, ChanceCondition> entry : entrySet())
		{
			IChanceSkillTrigger trigger = entry.getKey();
			ChanceCondition cond = entry.getValue();
			
			if (cond != null && cond.trigger(event))
			{
				if (trigger instanceof L2Skill)
					makeCast((L2Skill) trigger, target);
				else if (trigger instanceof EffectChanceSkillTrigger)
					makeCast((EffectChanceSkillTrigger) trigger, target);
			}
		}
	}
	
	private void makeCast(L2Skill skill, L2Character target)
	{
		try
		{
			if (skill.getWeaponDependancy(_owner) && skill.checkCondition(_owner, target, false))
			{
				if (skill.triggersChanceSkill()) // skill will trigger another skill, but only if its not chance skill
				{
					skill = SkillTable.getInstance().getInfo(skill.getTriggeredChanceId(), skill.getTriggeredChanceLevel());
					if (skill == null || skill.getSkillType() == L2SkillType.NOTDONE)
						return;
				}
				
				if (_owner.isSkillDisabled(skill))
					return;
				
				if (skill.getReuseDelay() > 0)
					_owner.disableSkill(skill, skill.getReuseDelay());
				
				L2Object[] targets = skill.getTargetList(_owner, false, target);
				
				if (targets.length == 0)
					return;
				
				L2Character firstTarget = (L2Character) targets[0];
				
				ISkillHandler handler = SkillHandler.getInstance().getSkillHandler(skill.getSkillType());
				
				_owner.broadcastPacket(new MagicSkillLaunched(_owner, skill.getId(), skill.getLevel(), targets));
				_owner.broadcastPacket(new MagicSkillUse(_owner, firstTarget, skill.getId(), skill.getLevel(), 0, 0));
				
				// Launch the magic skill and calculate its effects
				// TODO: once core will support all possible effects, use effects (not handler)
				if (handler != null)
					handler.useSkill(_owner, skill, targets);
				else
					skill.useSkill(_owner, targets);
			}
		}
		catch (Exception e)
		{
			_log.log(Level.WARNING, "", e);
		}
	}
	
	private void makeCast(EffectChanceSkillTrigger effect, L2Character target)
	{
		try
		{
			if (effect == null || !effect.triggersChanceSkill())
				return;
			
			L2Skill triggered = SkillTable.getInstance().getInfo(effect.getTriggeredChanceId(), effect.getTriggeredChanceLevel());
			if (triggered == null)
				return;
			L2Character caster = triggered.getTargetType() == L2Skill.SkillTargetType.TARGET_SELF ? _owner : effect.getEffector();
			
			if (caster == null || triggered.getSkillType() == L2SkillType.NOTDONE || caster.isSkillDisabled(triggered))
				return;
			
			if (triggered.getReuseDelay() > 0)
				caster.disableSkill(triggered, triggered.getReuseDelay());
			
			L2Object[] targets = triggered.getTargetList(caster, false, target);
			
			if (targets.length == 0)
				return;
			
			L2Character firstTarget = (L2Character) targets[0];
			
			ISkillHandler handler = SkillHandler.getInstance().getSkillHandler(triggered.getSkillType());
			
			_owner.broadcastPacket(new MagicSkillLaunched(_owner, triggered.getId(), triggered.getLevel(), targets));
			_owner.broadcastPacket(new MagicSkillUse(_owner, firstTarget, triggered.getId(), triggered.getLevel(), 0, 0));
			
			// Launch the magic skill and calculate its effects
			// TODO: once core will support all possible effects, use effects (not handler)
			if (handler != null)
				handler.useSkill(caster, triggered, targets);
			else
				triggered.useSkill(caster, targets);
		}
		catch (Exception e)
		{
			_log.log(Level.WARNING, "", e);
		}
	}
}