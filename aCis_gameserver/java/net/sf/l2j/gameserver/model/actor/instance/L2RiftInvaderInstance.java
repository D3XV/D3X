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
package net.sf.l2j.gameserver.model.actor.instance;

import net.sf.l2j.gameserver.model.actor.template.NpcTemplate;

public class L2RiftInvaderInstance extends L2MonsterInstance
{
	// Not longer needed since rift monster targeting control now is handled by the room zones for any mob
	public L2RiftInvaderInstance(int objectId, NpcTemplate template)
	{
		super(objectId, template);
	}
}
