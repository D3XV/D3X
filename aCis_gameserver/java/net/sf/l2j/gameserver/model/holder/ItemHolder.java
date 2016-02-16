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
package net.sf.l2j.gameserver.model.holder;

/**
 * Holder for item id-count.
 * @author UnAfraid
 */
public class ItemHolder
{
	private int _id;
	private int _count;
	
	public ItemHolder(int id, int count)
	{
		_id = id;
		_count = count;
	}
	
	/**
	 * @return the item/object identifier.
	 */
	public int getId()
	{
		return _id;
	}
	
	/**
	 * @return the item count.
	 */
	public int getCount()
	{
		return _count;
	}
	
	/**
	 * @param id : The new value to set.
	 */
	public void setId(int id)
	{
		_id = id;
	}
	
	/**
	 * @param count : The new value to set.
	 */
	public void setCount(int count)
	{
		_count = count;
	}
	
	@Override
	public String toString()
	{
		return getClass().getSimpleName() + ": Id: " + _id + " Count: " + _count;
	}
}