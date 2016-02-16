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

import net.sf.l2j.gameserver.model.actor.L2Npc;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;

/**
 * Those monsters don't attack at sight players owning itemId 8064, 8065 or 8067.
 * @author Tryskell
 */
public class GatekeeperZombies extends AbstractNpcAI
{
	public GatekeeperZombies(String name, String descr)
	{
		super(name, descr);
		addAggroRangeEnterId(22136);
	}
	
	@Override
	public String onAggroRangeEnter(L2Npc npc, L2PcInstance player, boolean isPet)
	{
		if (player.getInventory().hasAtLeastOneItem(8064, 8065, 8067))
			return null;
		
		return super.onAggroRangeEnter(npc, player, isPet);
	}
	
	public static void main(String[] args)
	{
		new GatekeeperZombies(GatekeeperZombies.class.getSimpleName(), "ai/group");
	}
}