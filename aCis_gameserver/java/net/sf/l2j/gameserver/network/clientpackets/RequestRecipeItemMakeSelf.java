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

import net.sf.l2j.gameserver.datatables.RecipeTable;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;

/**
 * @author Administrator
 */
public final class RequestRecipeItemMakeSelf extends L2GameClientPacket
{
	private int _id;
	
	@Override
	protected void readImpl()
	{
		_id = readD();
	}
	
	@Override
	protected void runImpl()
	{
		if (!getClient().getFloodProtectors().getManufacture().tryPerformAction("makeSelf"))
			return;
		
		final L2PcInstance activeChar = getClient().getActiveChar();
		if (activeChar == null)
			return;
		
		if (activeChar.getPrivateStoreType() == L2PcInstance.STORE_PRIVATE_MANUFACTURE || activeChar.isInCraftMode())
			return;
		
		RecipeTable.getInstance().requestMakeItem(activeChar, _id);
	}
}