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

/**
 * Format: (ch)ddddd
 */
public class ExConfirmVariationGemstone extends L2GameServerPacket
{
	private final int _gemstoneObjId;
	private final int _unk1;
	private final int _gemstoneCount;
	private final int _unk2;
	private final int _unk3;
	
	public ExConfirmVariationGemstone(int gemstoneObjId, int count)
	{
		_gemstoneObjId = gemstoneObjId;
		_unk1 = 1;
		_gemstoneCount = count;
		_unk2 = 1;
		_unk3 = 1;
	}
	
	@Override
	protected void writeImpl()
	{
		writeC(0xfe);
		writeH(0x54);
		writeD(_gemstoneObjId);
		writeD(_unk1);
		writeD(_gemstoneCount);
		writeD(_unk2);
		writeD(_unk3);
	}
}