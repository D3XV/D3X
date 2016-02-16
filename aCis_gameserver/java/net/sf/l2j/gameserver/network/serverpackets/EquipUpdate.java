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
package net.sf.l2j.gameserver.network.serverpackets;

import net.sf.l2j.Config;
import net.sf.l2j.gameserver.model.item.instance.ItemInstance;
import net.sf.l2j.gameserver.model.item.kind.Item;

public class EquipUpdate extends L2GameServerPacket
{
	private final ItemInstance _item;
	private final int _change;
	
	public EquipUpdate(ItemInstance item, int change)
	{
		_item = item;
		_change = change;
	}
	
	@Override
	protected final void writeImpl()
	{
		int bodypart = 0;
		writeC(0x4b);
		writeD(_change);
		writeD(_item.getObjectId());
		switch (_item.getItem().getBodyPart())
		{
			case Item.SLOT_L_EAR:
				bodypart = 0x01;
				break;
			case Item.SLOT_R_EAR:
				bodypart = 0x02;
				break;
			case Item.SLOT_NECK:
				bodypart = 0x03;
				break;
			case Item.SLOT_R_FINGER:
				bodypart = 0x04;
				break;
			case Item.SLOT_L_FINGER:
				bodypart = 0x05;
				break;
			case Item.SLOT_HEAD:
				bodypart = 0x06;
				break;
			case Item.SLOT_R_HAND:
				bodypart = 0x07;
				break;
			case Item.SLOT_L_HAND:
				bodypart = 0x08;
				break;
			case Item.SLOT_GLOVES:
				bodypart = 0x09;
				break;
			case Item.SLOT_CHEST:
				bodypart = 0x0a;
				break;
			case Item.SLOT_LEGS:
				bodypart = 0x0b;
				break;
			case Item.SLOT_FEET:
				bodypart = 0x0c;
				break;
			case Item.SLOT_BACK:
				bodypart = 0x0d;
				break;
			case Item.SLOT_LR_HAND:
				bodypart = 0x0e;
				break;
			case Item.SLOT_HAIR:
				bodypart = 0x0f;
				break;
		}
		
		if (Config.DEBUG)
			_log.fine("body:" + bodypart);
		writeD(bodypart);
	}
}