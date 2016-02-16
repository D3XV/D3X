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
package net.sf.l2j.gameserver.network.clientpackets;

import java.util.ArrayList;
import java.util.List;

import net.sf.l2j.Config;
import net.sf.l2j.gameserver.instancemanager.CastleManager;
import net.sf.l2j.gameserver.instancemanager.CastleManorManager;
import net.sf.l2j.gameserver.instancemanager.CastleManorManager.CropProcure;

/**
 * Format: (ch) dd [dddc] d - manor id d - size [ d - crop id d - sales d - price c - reward type ]
 * @author l3x
 */
public class RequestSetCrop extends L2GameClientPacket
{
	private int _size;
	private int _manorId;
	private int[] _items; // _size*4
	
	@Override
	protected void readImpl()
	{
		_manorId = readD();
		_size = readD();
		if (_size * 13 > _buf.remaining() || _size > 500)
		{
			_size = 0;
			return;
		}
		_items = new int[_size * 4];
		for (int i = 0; i < _size; i++)
		{
			int itemId = readD();
			_items[i * 4 + 0] = itemId;
			int sales = readD();
			_items[i * 4 + 1] = sales;
			int price = readD();
			_items[i * 4 + 2] = price;
			int type = readC();
			_items[i * 4 + 3] = type;
		}
	}
	
	@Override
	protected void runImpl()
	{
		if (_size < 1)
			return;
		
		List<CropProcure> crops = new ArrayList<>();
		for (int i = 0; i < _size; i++)
		{
			int id = _items[i * 4 + 0];
			int sales = _items[i * 4 + 1];
			int price = _items[i * 4 + 2];
			int type = _items[i * 4 + 3];
			if (id > 0)
			{
				CropProcure s = CastleManorManager.getInstance().getNewCropProcure(id, sales, type, price, sales);
				crops.add(s);
			}
		}
		
		CastleManager.getInstance().getCastleById(_manorId).setCropProcure(crops, CastleManorManager.PERIOD_NEXT);
		if (Config.ALT_MANOR_SAVE_ALL_ACTIONS)
			CastleManager.getInstance().getCastleById(_manorId).saveCropData(CastleManorManager.PERIOD_NEXT);
	}
}