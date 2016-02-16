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

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.sf.l2j.gameserver.instancemanager.CastleManorManager.CropProcure;
import net.sf.l2j.gameserver.model.L2Manor;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.model.item.instance.ItemInstance;

/**
 * format(packet 0xFE) ch dd [ddddcdcdddc] c - id h - sub id d - manor id d - size [ d - Object id d - crop id d - seed level c d - reward 1 id c d - reward 2 id d - manor d - buy residual d - buy price d - reward ]
 * @author l3x
 */
public class ExShowSellCropList extends L2GameServerPacket
{
	private int _manorId = 1;
	private final Map<Integer, ItemInstance> _cropsItems;
	private final Map<Integer, CropProcure> _castleCrops;
	
	public ExShowSellCropList(L2PcInstance player, int manorId, List<CropProcure> crops)
	{
		_manorId = manorId;
		_castleCrops = new HashMap<>();
		_cropsItems = new HashMap<>();
		
		List<Integer> allCrops = L2Manor.getInstance().getAllCrops();
		for (int cropId : allCrops)
		{
			ItemInstance item = player.getInventory().getItemByItemId(cropId);
			if (item != null)
				_cropsItems.put(cropId, item);
		}
		
		for (CropProcure crop : crops)
		{
			if (_cropsItems.containsKey(crop.getId()) && crop.getAmount() > 0)
				_castleCrops.put(crop.getId(), crop);
		}
	}
	
	@Override
	public void writeImpl()
	{
		writeC(0xFE);
		writeH(0x21);
		
		writeD(_manorId); // manor id
		writeD(_cropsItems.size()); // size
		
		for (ItemInstance item : _cropsItems.values())
		{
			writeD(item.getObjectId()); // Object id
			writeD(item.getItemId()); // crop id
			writeD(L2Manor.getInstance().getSeedLevelByCrop(item.getItemId())); // seed level
			writeC(1);
			writeD(L2Manor.getInstance().getRewardItem(item.getItemId(), 1)); // reward 1 id
			writeC(1);
			writeD(L2Manor.getInstance().getRewardItem(item.getItemId(), 2)); // reward 2 id
			
			if (_castleCrops.containsKey(item.getItemId()))
			{
				CropProcure crop = _castleCrops.get(item.getItemId());
				writeD(_manorId); // manor
				writeD(crop.getAmount()); // buy residual
				writeD(crop.getPrice()); // buy price
				writeC(crop.getReward()); // reward
			}
			else
			{
				writeD(0xFFFFFFFF); // manor
				writeD(0); // buy residual
				writeD(0); // buy price
				writeC(0); // reward
			}
			writeD(item.getCount()); // my crops
		}
	}
}