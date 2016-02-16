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

/**
 * Angel spawns... When one of the angels in the keys dies, the other angel will spawn.
 */
public class PolymorphingAngel extends AbstractNpcAI
{
	private static final Map<Integer, Integer> ANGELSPAWNS = new HashMap<>();
	{
		ANGELSPAWNS.put(20830, 20859);
		ANGELSPAWNS.put(21067, 21068);
		ANGELSPAWNS.put(21062, 21063);
		ANGELSPAWNS.put(20831, 20860);
		ANGELSPAWNS.put(21070, 21071);
	}
	
	public PolymorphingAngel(String name, String descr)
	{
		super(name, descr);
		
		for (int mob : ANGELSPAWNS.keySet())
			addKillId(mob);
	}
	
	@Override
	public String onKill(L2Npc npc, L2PcInstance killer, boolean isPet)
	{
		final L2Attackable newNpc = (L2Attackable) addSpawn(ANGELSPAWNS.get(npc.getNpcId()), npc, false, 0, false);
		attack(newNpc, killer);
		
		return super.onKill(npc, killer, isPet);
	}
	
	public static void main(String[] args)
	{
		new PolymorphingAngel(PolymorphingAngel.class.getSimpleName(), "ai/group");
	}
}