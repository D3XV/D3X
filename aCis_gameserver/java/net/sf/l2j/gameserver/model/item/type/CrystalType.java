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
package net.sf.l2j.gameserver.model.item.type;

/**
 * Crystal Type enumerated.
 * @author Adry_85
 */
public enum CrystalType
{
	NONE(0, 0, 0, 0),
	D(1, 1458, 11, 90),
	C(2, 1459, 6, 45),
	B(3, 1460, 11, 67),
	A(4, 1461, 19, 144),
	S(5, 1462, 25, 250);
	
	private final int _id;
	private final int _crystalId;
	private final int _crystalEnchantBonusArmor;
	private final int _crystalEnchantBonusWeapon;
	
	private CrystalType(int id, int crystalId, int crystalEnchantBonusArmor, int crystalEnchantBonusWeapon)
	{
		_id = id;
		_crystalId = crystalId;
		_crystalEnchantBonusArmor = crystalEnchantBonusArmor;
		_crystalEnchantBonusWeapon = crystalEnchantBonusWeapon;
	}
	
	/**
	 * Gets the crystal type ID.
	 * @return the crystal type ID
	 */
	public int getId()
	{
		return _id;
	}
	
	/**
	 * Gets the item ID of the crystal.
	 * @return the item ID of the crystal
	 */
	public int getCrystalId()
	{
		return _crystalId;
	}
	
	public int getCrystalEnchantBonusArmor()
	{
		return _crystalEnchantBonusArmor;
	}
	
	public int getCrystalEnchantBonusWeapon()
	{
		return _crystalEnchantBonusWeapon;
	}
	
	public boolean isGreater(CrystalType crystalType)
	{
		return getId() > crystalType.getId();
	}
	
	public boolean isLesser(CrystalType crystalType)
	{
		return getId() < crystalType.getId();
	}
}