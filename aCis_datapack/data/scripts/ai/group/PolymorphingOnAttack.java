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

import java.util.HashMap;
import java.util.Map;

import net.sf.l2j.gameserver.model.actor.L2Attackable;
import net.sf.l2j.gameserver.model.actor.L2Npc;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.model.quest.QuestEventType;
import net.sf.l2j.gameserver.network.clientpackets.Say2;
import net.sf.l2j.gameserver.network.serverpackets.CreatureSay;
import net.sf.l2j.util.Rnd;

/**
 * @author Slyce
 */
public class PolymorphingOnAttack extends AbstractNpcAI
{
	private static final Map<Integer, Integer[]> MOBSPAWNS = new HashMap<>();
	{
		MOBSPAWNS.put(21258, new Integer[]
		{
			21259,
			100,
			100,
			-1
		}); // Fallen Orc Shaman -> Sharp Talon Tiger (always polymorphs)
		MOBSPAWNS.put(21261, new Integer[]
		{
			21262,
			100,
			20,
			0
		}); // Ol Mahum Transcender 1st stage
		MOBSPAWNS.put(21262, new Integer[]
		{
			21263,
			100,
			10,
			1
		}); // Ol Mahum Transcender 2nd stage
		MOBSPAWNS.put(21263, new Integer[]
		{
			21264,
			100,
			5,
			2
		}); // Ol Mahum Transcender 3rd stage
		MOBSPAWNS.put(21265, new Integer[]
		{
			21271,
			100,
			33,
			0
		}); // Cave Ant Larva -> Cave Ant
		MOBSPAWNS.put(21266, new Integer[]
		{
			21269,
			100,
			100,
			-1
		}); // Cave Ant Larva -> Cave Ant (always polymorphs)
		MOBSPAWNS.put(21267, new Integer[]
		{
			21270,
			100,
			100,
			-1
		}); // Cave Ant Larva -> Cave Ant Soldier (always polymorphs)
		MOBSPAWNS.put(21271, new Integer[]
		{
			21272,
			66,
			10,
			1
		}); // Cave Ant -> Cave Ant Soldier
		MOBSPAWNS.put(21272, new Integer[]
		{
			21273,
			33,
			5,
			2
		}); // Cave Ant Soldier -> Cave Noble Ant
		MOBSPAWNS.put(21521, new Integer[]
		{
			21522,
			100,
			30,
			-1
		}); // Claws of Splendor
		MOBSPAWNS.put(21527, new Integer[]
		{
			21528,
			100,
			30,
			-1
		}); // Anger of Splendor
		MOBSPAWNS.put(21533, new Integer[]
		{
			21534,
			100,
			30,
			-1
		}); // Alliance of Splendor
		MOBSPAWNS.put(21537, new Integer[]
		{
			21538,
			100,
			30,
			-1
		}); // Fang of Splendor
	}
	
	protected static final String[][] MOBTEXTS =
	{
		new String[]
		{
			"Enough fooling around. Get ready to die!",
			"You idiot! I've just been toying with you!",
			"Now the fun starts!"
		},
		new String[]
		{
			"I must admit, no one makes my blood boil quite like you do!",
			"Now the battle begins!",
			"Witness my true power!"
		},
		new String[]
		{
			"Prepare to die!",
			"I'll double my strength!",
			"You have more skill than I thought"
		}
	};
	
	public PolymorphingOnAttack(String name, String descr)
	{
		super(name, descr);
		registerMobs(MOBSPAWNS.keySet(), QuestEventType.ON_ATTACK);
	}
	
	@Override
	public String onAttack(L2Npc npc, L2PcInstance attacker, int damage, boolean isPet)
	{
		if (npc.isVisible() && !npc.isDead())
		{
			final Integer[] tmp = MOBSPAWNS.get(npc.getNpcId());
			if (tmp != null)
			{
				if (npc.getCurrentHp() <= (npc.getMaxHp() * tmp[1] / 100.0) && Rnd.get(100) < tmp[2])
				{
					if (tmp[3] >= 0)
					{
						String text = MOBTEXTS[tmp[3]][Rnd.get(MOBTEXTS[tmp[3]].length)];
						npc.broadcastPacket(new CreatureSay(npc.getObjectId(), Say2.ALL, npc.getName(), text));
					}
					npc.deleteMe();
					
					L2Attackable newNpc = (L2Attackable) addSpawn(tmp[0], npc.getX(), npc.getY(), npc.getZ() + 10, npc.getHeading(), false, 0, true);
					attack(newNpc, ((isPet) ? attacker.getPet() : attacker));
				}
			}
		}
		return super.onAttack(npc, attacker, damage, isPet);
	}
	
	public static void main(String[] args)
	{
		new PolymorphingOnAttack(PolymorphingOnAttack.class.getSimpleName(), "ai/group");
	}
}