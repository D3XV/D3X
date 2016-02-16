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
package net.sf.l2j.gameserver.model;

public final class L2EnchantSkillLearn
{
	private final int _id;
	private final int _level;
	private final int _baseLvl;
	private final int _prevLevel;
	private final int _enchant;
	
	public L2EnchantSkillLearn(int id, int lvl, int baseLvl, int prevLvl, int enchant)
	{
		_id = id;
		_level = lvl;
		_baseLvl = baseLvl;
		_prevLevel = prevLvl;
		_enchant = enchant;
	}
	
	/**
	 * @return Returns the id.
	 */
	public int getId()
	{
		return _id;
	}
	
	/**
	 * @return Returns the level.
	 */
	public int getLevel()
	{
		return _level;
	}
	
	/**
	 * @return Returns the minLevel.
	 */
	public int getBaseLevel()
	{
		return _baseLvl;
	}
	
	/**
	 * @return Returns the minSkillLevel.
	 */
	public int getPrevLevel()
	{
		return _prevLevel;
	}
	
	/**
	 * @return Returns the minSkillLevel.
	 */
	public int getEnchant()
	{
		return _enchant;
	}
}