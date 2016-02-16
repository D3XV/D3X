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
package ai.group;

import ai.AbstractNpcAI;

import net.sf.l2j.gameserver.datatables.SkillTable;
import net.sf.l2j.gameserver.model.L2Effect;
import net.sf.l2j.gameserver.model.actor.L2Npc;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.model.quest.QuestEventType;
import net.sf.l2j.util.Rnd;

/**
 * Hot Spring Disease AI
 * @author devO, Sandro, Tryskell
 */
public class HotSpringDisease extends AbstractNpcAI
{
	// Diseases
	private static final int MALARIA = 4554;
	
	// Chance
	private static final int DISEASE_CHANCE = 1;
	
	// Monsters
	private static final int[][] MONSTERS_DISEASES =
	{
		{
			21314,
			21316,
			21317,
			21319,
			21321,
			21322
		},
		{
			4551,
			4552,
			4553,
			4552,
			4551,
			4553
		}
	};
	
	public HotSpringDisease(String name, String descr)
	{
		super(name, descr);
		registerMobs(MONSTERS_DISEASES[0], QuestEventType.ON_ATTACK_ACT);
	}
	
	@Override
	public String onAttackAct(L2Npc npc, L2PcInstance victim)
	{
		for (int i = 0; i < 6; i++)
		{
			if (MONSTERS_DISEASES[0][i] != npc.getNpcId())
				continue;
			
			tryToApplyEffect(npc, victim, MALARIA);
			tryToApplyEffect(npc, victim, MONSTERS_DISEASES[1][i]);
		}
		return super.onAttackAct(npc, victim);
	}
	
	private void tryToApplyEffect(L2Npc npc, L2PcInstance victim, int skillId)
	{
		if (Rnd.get(100) < DISEASE_CHANCE)
		{
			int level = 1;
			
			L2Effect[] effects = victim.getAllEffects();
			if (effects.length != 0 || effects != null)
			{
				for (L2Effect e : effects)
				{
					if (e.getSkill().getId() != skillId)
						continue;
					
					level += e.getSkill().getLevel();
					e.exit();
				}
			}
			
			if (level > 10)
				level = 10;
			
			SkillTable.getInstance().getInfo(skillId, level).getEffects(npc, victim);
		}
	}
	
	public static void main(String[] args)
	{
		new HotSpringDisease(HotSpringDisease.class.getSimpleName(), "ai/group");
	}
}