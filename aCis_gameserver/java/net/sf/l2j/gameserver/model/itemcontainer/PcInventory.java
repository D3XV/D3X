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
package net.sf.l2j.gameserver.model.itemcontainer;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;

import net.sf.l2j.L2DatabaseFactory;
import net.sf.l2j.gameserver.datatables.ItemTable;
import net.sf.l2j.gameserver.model.L2Object;
import net.sf.l2j.gameserver.model.TradeList;
import net.sf.l2j.gameserver.model.TradeList.TradeItem;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.model.item.instance.ItemInstance;
import net.sf.l2j.gameserver.model.item.instance.ItemInstance.ItemLocation;
import net.sf.l2j.gameserver.model.item.kind.Item;
import net.sf.l2j.gameserver.model.item.type.EtcItemType;
import net.sf.l2j.gameserver.model.itemcontainer.listeners.ArmorSetListener;
import net.sf.l2j.gameserver.model.itemcontainer.listeners.BowRodListener;
import net.sf.l2j.gameserver.model.itemcontainer.listeners.ItemPassiveSkillsListener;
import net.sf.l2j.gameserver.network.serverpackets.InventoryUpdate;
import net.sf.l2j.gameserver.network.serverpackets.ItemList;
import net.sf.l2j.gameserver.network.serverpackets.StatusUpdate;
import net.sf.l2j.gameserver.taskmanager.ShadowItemTaskManager;
import net.sf.l2j.gameserver.util.Util;

public class PcInventory extends Inventory
{
	public static final int ADENA_ID = 57;
	public static final int ANCIENT_ADENA_ID = 5575;
	
	private final L2PcInstance _owner;
	private ItemInstance _adena;
	private ItemInstance _ancientAdena;
	
	private int[] _blockItems = null;
	
	private int _questSlots;
	/**
	 * Block modes:
	 * <UL>
	 * <LI>-1 - no block
	 * <LI>0 - block items from _invItems, allow usage of other items
	 * <LI>1 - allow usage of items from _invItems, block other items
	 * </UL>
	 */
	private int _blockMode = -1;
	
	public PcInventory(L2PcInstance owner)
	{
		super();
		_owner = owner;
		
		addPaperdollListener(ArmorSetListener.getInstance());
		addPaperdollListener(BowRodListener.getInstance());
		addPaperdollListener(ItemPassiveSkillsListener.getInstance());
		addPaperdollListener(ShadowItemTaskManager.getInstance());
	}
	
	@Override
	public L2PcInstance getOwner()
	{
		return _owner;
	}
	
	@Override
	protected ItemLocation getBaseLocation()
	{
		return ItemLocation.INVENTORY;
	}
	
	@Override
	protected ItemLocation getEquipLocation()
	{
		return ItemLocation.PAPERDOLL;
	}
	
	public ItemInstance getAdenaInstance()
	{
		return _adena;
	}
	
	@Override
	public int getAdena()
	{
		return _adena != null ? _adena.getCount() : 0;
	}
	
	public ItemInstance getAncientAdenaInstance()
	{
		return _ancientAdena;
	}
	
	public int getAncientAdena()
	{
		return (_ancientAdena != null) ? _ancientAdena.getCount() : 0;
	}
	
	public ItemInstance[] getUniqueItems(boolean allowAdena, boolean allowAncientAdena)
	{
		return getUniqueItems(allowAdena, allowAncientAdena, true);
	}
	
	/**
	 * Returns the list of items in inventory available for transaction
	 * @param allowAdena
	 * @param allowAncientAdena
	 * @param onlyAvailable
	 * @return ItemInstance : items in inventory
	 */
	public ItemInstance[] getUniqueItems(boolean allowAdena, boolean allowAncientAdena, boolean onlyAvailable)
	{
		List<ItemInstance> list = new ArrayList<>();
		for (ItemInstance item : _items)
		{
			if (item == null)
				continue;
			
			if (!allowAdena && item.getItemId() == ADENA_ID)
				continue;
			
			if (!allowAncientAdena && item.getItemId() == ANCIENT_ADENA_ID)
				continue;
			
			boolean isDuplicate = false;
			for (ItemInstance litem : list)
			{
				if (litem.getItemId() == item.getItemId())
				{
					isDuplicate = true;
					break;
				}
			}
			if (!isDuplicate && (!onlyAvailable || (item.isSellable() && item.isAvailable(getOwner(), false, false))))
				list.add(item);
		}
		return list.toArray(new ItemInstance[list.size()]);
	}
	
	/**
	 * Returns the list of items in inventory available for transaction Allows an item to appear twice if and only if there is a difference in enchantment level.
	 * @param allowAdena
	 * @param allowAncientAdena
	 * @return ItemInstance : items in inventory
	 */
	public ItemInstance[] getUniqueItemsByEnchantLevel(boolean allowAdena, boolean allowAncientAdena)
	{
		return getUniqueItemsByEnchantLevel(allowAdena, allowAncientAdena, true);
	}
	
	public ItemInstance[] getUniqueItemsByEnchantLevel(boolean allowAdena, boolean allowAncientAdena, boolean onlyAvailable)
	{
		List<ItemInstance> list = new ArrayList<>();
		for (ItemInstance item : _items)
		{
			if (item == null)
				continue;
			
			if (!allowAdena && item.getItemId() == ADENA_ID)
				continue;
			
			if (!allowAncientAdena && item.getItemId() == ANCIENT_ADENA_ID)
				continue;
			
			boolean isDuplicate = false;
			for (ItemInstance litem : list)
			{
				if ((litem.getItemId() == item.getItemId()) && (litem.getEnchantLevel() == item.getEnchantLevel()))
				{
					isDuplicate = true;
					break;
				}
			}
			if (!isDuplicate && (!onlyAvailable || (item.isSellable() && item.isAvailable(getOwner(), false, false))))
				list.add(item);
		}
		return list.toArray(new ItemInstance[list.size()]);
	}
	
	/**
	 * @param itemId
	 * @return
	 * @see net.sf.l2j.gameserver.model.itemcontainer.PcInventory#getAllItemsByItemId(int, boolean)
	 */
	public ItemInstance[] getAllItemsByItemId(int itemId)
	{
		return getAllItemsByItemId(itemId, true);
	}
	
	/**
	 * Returns the list of all items in inventory that have a given item id.
	 * @param itemId : ID of item
	 * @param includeEquipped : include equipped items
	 * @return ItemInstance[] : matching items from inventory
	 */
	public ItemInstance[] getAllItemsByItemId(int itemId, boolean includeEquipped)
	{
		List<ItemInstance> list = new ArrayList<>();
		for (ItemInstance item : _items)
		{
			if (item == null)
				continue;
			
			if (item.getItemId() == itemId && (includeEquipped || !item.isEquipped()))
				list.add(item);
		}
		return list.toArray(new ItemInstance[list.size()]);
	}
	
	/**
	 * @param itemId
	 * @param enchantment
	 * @return
	 * @see net.sf.l2j.gameserver.model.itemcontainer.PcInventory#getAllItemsByItemId(int, int, boolean)
	 */
	public ItemInstance[] getAllItemsByItemId(int itemId, int enchantment)
	{
		return getAllItemsByItemId(itemId, enchantment, true);
	}
	
	/**
	 * Returns the list of all items in inventory that have a given item id AND a given enchantment level.
	 * @param itemId : ID of item
	 * @param enchantment : enchant level of item
	 * @param includeEquipped : include equipped items
	 * @return ItemInstance[] : matching items from inventory
	 */
	public ItemInstance[] getAllItemsByItemId(int itemId, int enchantment, boolean includeEquipped)
	{
		List<ItemInstance> list = new ArrayList<>();
		for (ItemInstance item : _items)
		{
			if (item == null)
				continue;
			
			if ((item.getItemId() == itemId) && (item.getEnchantLevel() == enchantment) && (includeEquipped || !item.isEquipped()))
				list.add(item);
		}
		return list.toArray(new ItemInstance[list.size()]);
	}
	
	/**
	 * Returns the list of items in inventory available for transaction
	 * @param allowAdena
	 * @param allowNonTradeable
	 * @return ItemInstance : items in inventory
	 */
	public ItemInstance[] getAvailableItems(boolean allowAdena, boolean allowNonTradeable)
	{
		List<ItemInstance> list = new ArrayList<>();
		for (ItemInstance item : _items)
		{
			if (item != null && item.isAvailable(getOwner(), allowAdena, allowNonTradeable) && canManipulateWithItemId(item.getItemId()))
				list.add(item);
		}
		return list.toArray(new ItemInstance[list.size()]);
	}
	
	/**
	 * Get all augmented items
	 * @return
	 */
	public ItemInstance[] getAugmentedItems()
	{
		List<ItemInstance> list = new ArrayList<>();
		for (ItemInstance item : _items)
		{
			if (item != null && item.isAugmented())
				list.add(item);
		}
		return list.toArray(new ItemInstance[list.size()]);
	}
	
	/**
	 * Returns the list of items in inventory available for transaction adjusetd by tradeList
	 * @param tradeList
	 * @return ItemInstance : items in inventory
	 */
	public TradeItem[] getAvailableItems(TradeList tradeList)
	{
		List<TradeItem> list = new ArrayList<>();
		for (ItemInstance item : _items)
		{
			if (item != null && item.isAvailable(getOwner(), false, false))
			{
				TradeItem adjItem = tradeList.adjustAvailableItem(item);
				if (adjItem != null)
					list.add(adjItem);
			}
		}
		return list.toArray(new TradeItem[list.size()]);
	}
	
	/**
	 * Adjust TradeItem according his status in inventory
	 * @param item : ItemInstance to be adjusten
	 */
	public void adjustAvailableItem(TradeItem item)
	{
		boolean notAllEquipped = false;
		for (ItemInstance adjItem : getItemsByItemId(item.getItem().getItemId()))
		{
			if (adjItem.isEquipable())
			{
				if (!adjItem.isEquipped())
					notAllEquipped |= true;
			}
			else
			{
				notAllEquipped |= true;
				break;
			}
		}
		if (notAllEquipped)
		{
			ItemInstance adjItem = getItemByItemId(item.getItem().getItemId());
			item.setObjectId(adjItem.getObjectId());
			item.setEnchant(adjItem.getEnchantLevel());
			
			if (adjItem.getCount() < item.getCount())
				item.setCount(adjItem.getCount());
			
			return;
		}
		
		item.setCount(0);
	}
	
	/**
	 * Adds adena to PCInventory
	 * @param process : String Identifier of process triggering this action
	 * @param count : int Quantity of adena to be added
	 * @param actor : L2PcInstance Player requesting the item add
	 * @param reference : L2Object Object referencing current action like NPC selling item or previous item in transformation
	 */
	public void addAdena(String process, int count, L2PcInstance actor, L2Object reference)
	{
		if (count > 0)
			addItem(process, ADENA_ID, count, actor, reference);
	}
	
	/**
	 * Removes adena to PCInventory
	 * @param process : String Identifier of process triggering this action
	 * @param count : int Quantity of adena to be removed
	 * @param actor : L2PcInstance Player requesting the item add
	 * @param reference : L2Object Object referencing current action like NPC selling item or previous item in transformation
	 * @return true if successful.
	 */
	public boolean reduceAdena(String process, int count, L2PcInstance actor, L2Object reference)
	{
		if (count > 0)
			return destroyItemByItemId(process, ADENA_ID, count, actor, reference) != null;
		
		return false;
	}
	
	/**
	 * Adds specified amount of ancient adena to player inventory.
	 * @param process : String Identifier of process triggering this action
	 * @param count : int Quantity of adena to be added
	 * @param actor : L2PcInstance Player requesting the item add
	 * @param reference : L2Object Object referencing current action like NPC selling item or previous item in transformation
	 */
	public void addAncientAdena(String process, int count, L2PcInstance actor, L2Object reference)
	{
		if (count > 0)
			addItem(process, ANCIENT_ADENA_ID, count, actor, reference);
	}
	
	/**
	 * Removes specified amount of ancient adena from player inventory.
	 * @param process : String Identifier of process triggering this action
	 * @param count : int Quantity of adena to be removed
	 * @param actor : L2PcInstance Player requesting the item add
	 * @param reference : L2Object Object referencing current action like NPC selling item or previous item in transformation
	 * @return true if successful.
	 */
	public boolean reduceAncientAdena(String process, int count, L2PcInstance actor, L2Object reference)
	{
		if (count > 0)
			return destroyItemByItemId(process, ANCIENT_ADENA_ID, count, actor, reference) != null;
		
		return false;
	}
	
	/**
	 * Adds item in inventory and checks _adena and _ancientAdena
	 * @param process : String Identifier of process triggering this action
	 * @param item : ItemInstance to be added
	 * @param actor : L2PcInstance Player requesting the item add
	 * @param reference : L2Object Object referencing current action like NPC selling item or previous item in transformation
	 * @return ItemInstance corresponding to the new item or the updated item in inventory
	 */
	@Override
	public ItemInstance addItem(String process, ItemInstance item, L2PcInstance actor, L2Object reference)
	{
		item = super.addItem(process, item, actor, reference);
		if (item == null)
			return null;
		
		if (item.getItemId() == ADENA_ID && !item.equals(_adena))
			_adena = item;
		else if (item.getItemId() == ANCIENT_ADENA_ID && !item.equals(_ancientAdena))
			_ancientAdena = item;
		
		return item;
	}
	
	/**
	 * Adds item in inventory and checks _adena and _ancientAdena
	 * @param process : String Identifier of process triggering this action
	 * @param itemId : int Item Identifier of the item to be added
	 * @param count : int Quantity of items to be added
	 * @param actor : L2PcInstance Player requesting the item creation
	 * @param reference : L2Object Object referencing current action like NPC selling item or previous item in transformation
	 * @return ItemInstance corresponding to the new item or the updated item in inventory
	 */
	@Override
	public ItemInstance addItem(String process, int itemId, int count, L2PcInstance actor, L2Object reference)
	{
		ItemInstance item = super.addItem(process, itemId, count, actor, reference);
		if (item == null)
			return null;
		
		if (item.getItemId() == ADENA_ID && !item.equals(_adena))
			_adena = item;
		else if (item.getItemId() == ANCIENT_ADENA_ID && !item.equals(_ancientAdena))
			_ancientAdena = item;
		
		if (actor != null)
		{
			// Send inventory update packet
			InventoryUpdate playerIU = new InventoryUpdate();
			playerIU.addItem(item);
			actor.sendPacket(playerIU);
			
			// Update current load as well
			StatusUpdate su = new StatusUpdate(actor);
			su.addAttribute(StatusUpdate.CUR_LOAD, actor.getCurrentLoad());
			actor.sendPacket(su);
		}
		
		return item;
	}
	
	/**
	 * Transfers item to another inventory and checks _adena and _ancientAdena
	 * @param process : String Identifier of process triggering this action
	 * @param objectId : int Item Identifier of the item to be transfered
	 * @param count : int Quantity of items to be transfered
	 * @param actor : L2PcInstance Player requesting the item transfer
	 * @param reference : L2Object Object referencing current action like NPC selling item or previous item in transformation
	 * @return ItemInstance corresponding to the new item or the updated item in inventory
	 */
	@Override
	public ItemInstance transferItem(String process, int objectId, int count, ItemContainer target, L2PcInstance actor, L2Object reference)
	{
		ItemInstance item = super.transferItem(process, objectId, count, target, actor, reference);
		
		if (_adena != null && (_adena.getCount() <= 0 || _adena.getOwnerId() != getOwnerId()))
			_adena = null;
		
		if (_ancientAdena != null && (_ancientAdena.getCount() <= 0 || _ancientAdena.getOwnerId() != getOwnerId()))
			_ancientAdena = null;
		
		return item;
	}
	
	/**
	 * Destroy item from inventory and checks _adena and _ancientAdena
	 * @param process : String Identifier of process triggering this action
	 * @param item : ItemInstance to be destroyed
	 * @param actor : L2PcInstance Player requesting the item destroy
	 * @param reference : L2Object Object referencing current action like NPC selling item or previous item in transformation
	 * @return ItemInstance corresponding to the destroyed item or the updated item in inventory
	 */
	@Override
	public ItemInstance destroyItem(String process, ItemInstance item, L2PcInstance actor, L2Object reference)
	{
		return this.destroyItem(process, item, item.getCount(), actor, reference);
	}
	
	/**
	 * Destroy item from inventory and checks _adena and _ancientAdena
	 * @param process : String Identifier of process triggering this action
	 * @param item : ItemInstance to be destroyed
	 * @param actor : L2PcInstance Player requesting the item destroy
	 * @param reference : L2Object Object referencing current action like NPC selling item or previous item in transformation
	 * @return ItemInstance corresponding to the destroyed item or the updated item in inventory
	 */
	@Override
	public ItemInstance destroyItem(String process, ItemInstance item, int count, L2PcInstance actor, L2Object reference)
	{
		item = super.destroyItem(process, item, count, actor, reference);
		
		if (_adena != null && _adena.getCount() <= 0)
			_adena = null;
		
		if (_ancientAdena != null && _ancientAdena.getCount() <= 0)
			_ancientAdena = null;
		
		return item;
	}
	
	/**
	 * Destroys item from inventory and checks _adena and _ancientAdena
	 * @param process : String Identifier of process triggering this action
	 * @param objectId : int Item Instance identifier of the item to be destroyed
	 * @param count : int Quantity of items to be destroyed
	 * @param actor : L2PcInstance Player requesting the item destroy
	 * @param reference : L2Object Object referencing current action like NPC selling item or previous item in transformation
	 * @return ItemInstance corresponding to the destroyed item or the updated item in inventory
	 */
	@Override
	public ItemInstance destroyItem(String process, int objectId, int count, L2PcInstance actor, L2Object reference)
	{
		ItemInstance item = getItemByObjectId(objectId);
		if (item == null)
			return null;
		
		return this.destroyItem(process, item, count, actor, reference);
	}
	
	/**
	 * Destroy item from inventory by using its <B>itemId</B> and checks _adena and _ancientAdena
	 * @param process : String Identifier of process triggering this action
	 * @param itemId : int Item identifier of the item to be destroyed
	 * @param count : int Quantity of items to be destroyed
	 * @param actor : L2PcInstance Player requesting the item destroy
	 * @param reference : L2Object Object referencing current action like NPC selling item or previous item in transformation
	 * @return ItemInstance corresponding to the destroyed item or the updated item in inventory
	 */
	@Override
	public ItemInstance destroyItemByItemId(String process, int itemId, int count, L2PcInstance actor, L2Object reference)
	{
		ItemInstance item = getItemByItemId(itemId);
		if (item == null)
			return null;
		
		return this.destroyItem(process, item, count, actor, reference);
	}
	
	/**
	 * Drop item from inventory and checks _adena and _ancientAdena
	 * @param process : String Identifier of process triggering this action
	 * @param item : ItemInstance to be dropped
	 * @param actor : L2PcInstance Player requesting the item drop
	 * @param reference : L2Object Object referencing current action like NPC selling item or previous item in transformation
	 * @return ItemInstance corresponding to the destroyed item or the updated item in inventory
	 */
	@Override
	public ItemInstance dropItem(String process, ItemInstance item, L2PcInstance actor, L2Object reference)
	{
		item = super.dropItem(process, item, actor, reference);
		
		if (_adena != null && (_adena.getCount() <= 0 || _adena.getOwnerId() != getOwnerId()))
			_adena = null;
		
		if (_ancientAdena != null && (_ancientAdena.getCount() <= 0 || _ancientAdena.getOwnerId() != getOwnerId()))
			_ancientAdena = null;
		
		return item;
	}
	
	/**
	 * Drop item from inventory by using its <B>objectID</B> and checks _adena and _ancientAdena
	 * @param process : String Identifier of process triggering this action
	 * @param objectId : int Item Instance identifier of the item to be dropped
	 * @param count : int Quantity of items to be dropped
	 * @param actor : L2PcInstance Player requesting the item drop
	 * @param reference : L2Object Object referencing current action like NPC selling item or previous item in transformation
	 * @return ItemInstance corresponding to the destroyed item or the updated item in inventory
	 */
	@Override
	public ItemInstance dropItem(String process, int objectId, int count, L2PcInstance actor, L2Object reference)
	{
		ItemInstance item = super.dropItem(process, objectId, count, actor, reference);
		
		if (_adena != null && (_adena.getCount() <= 0 || _adena.getOwnerId() != getOwnerId()))
			_adena = null;
		
		if (_ancientAdena != null && (_ancientAdena.getCount() <= 0 || _ancientAdena.getOwnerId() != getOwnerId()))
			_ancientAdena = null;
		
		return item;
	}
	
	/**
	 * <b>Overloaded</b>, when removes item from inventory, remove also owner shortcuts.
	 * @param item : ItemInstance to be removed from inventory
	 */
	@Override
	protected boolean removeItem(ItemInstance item)
	{
		// Removes any reference to the item from Shortcut bar
		getOwner().removeItemFromShortCut(item.getObjectId());
		
		// Removes active Enchant Scroll
		if (item.equals(getOwner().getActiveEnchantItem()))
			getOwner().setActiveEnchantItem(null);
		
		if (item.getItemId() == ADENA_ID)
			_adena = null;
		else if (item.getItemId() == ANCIENT_ADENA_ID)
			_ancientAdena = null;
		
		synchronized (_items)
		{
			if (item.isQuestItem())
			{
				_questSlots--;
				if (_questSlots < 0)
				{
					_questSlots = 0;
					_log.warning(this + ": QuestInventory size < 0!");
				}
			}
			return super.removeItem(item);
		}
	}
	
	/**
	 * Refresh the weight of equipment loaded
	 */
	@Override
	public void refreshWeight()
	{
		super.refreshWeight();
		getOwner().refreshOverloaded();
	}
	
	/**
	 * Get back items in inventory from database
	 */
	@Override
	public void restore()
	{
		super.restore();
		_adena = getItemByItemId(ADENA_ID);
		_ancientAdena = getItemByItemId(ANCIENT_ADENA_ID);
	}
	
	public static int[][] restoreVisibleInventory(int objectId)
	{
		int[][] paperdoll = new int[0x12][3];
		try (Connection con = L2DatabaseFactory.getInstance().getConnection())
		{
			PreparedStatement statement2 = con.prepareStatement("SELECT object_id,item_id,loc_data,enchant_level FROM items WHERE owner_id=? AND loc='PAPERDOLL'");
			statement2.setInt(1, objectId);
			ResultSet invdata = statement2.executeQuery();
			
			while (invdata.next())
			{
				int slot = invdata.getInt("loc_data");
				paperdoll[slot][0] = invdata.getInt("object_id");
				paperdoll[slot][1] = invdata.getInt("item_id");
				paperdoll[slot][2] = invdata.getInt("enchant_level");
			}
			
			invdata.close();
			statement2.close();
		}
		catch (Exception e)
		{
			_log.log(Level.WARNING, "Could not restore inventory: " + e.getMessage(), e);
		}
		return paperdoll;
	}
	
	public boolean validateCapacity(ItemInstance item)
	{
		int slots = 0;
		
		if (!(item.isStackable() && getItemByItemId(item.getItemId()) != null) && item.getItemType() != EtcItemType.HERB)
			slots++;
		
		return validateCapacity(slots, item.isQuestItem());
	}
	
	public boolean validateCapacityByItemId(int ItemId)
	{
		int slots = 0;
		Item item = ItemTable.getInstance().getTemplate(ItemId);
		ItemInstance invItem = getItemByItemId(ItemId);
		if (!(invItem != null && invItem.isStackable()))
			slots++;
		
		return validateCapacity(slots, item.isQuestItem());
	}
	
	@Override
	public boolean validateCapacity(int slots)
	{
		return validateCapacity(slots, false);
	}
	
	public boolean validateCapacity(int slots, boolean questItem)
	{
		if (!questItem)
			return (_items.size() - _questSlots + slots <= _owner.getInventoryLimit());
		
		return _questSlots + slots <= L2PcInstance.getQuestInventoryLimit();
	}
	
	@Override
	public boolean validateWeight(int weight)
	{
		return (_totalWeight + weight <= _owner.getMaxLoad());
	}
	
	/**
	 * Set inventory block for specified IDs<br>
	 * array reference is used for {@link PcInventory#_blockItems}
	 * @param items array of Ids to block/allow
	 * @param mode blocking mode {@link PcInventory#_blockMode}
	 */
	public void setInventoryBlock(int[] items, int mode)
	{
		_blockMode = mode;
		_blockItems = items;
		
		_owner.sendPacket(new ItemList(_owner, false));
	}
	
	/**
	 * Unblock blocked itemIds
	 */
	public void unblock()
	{
		_blockMode = -1;
		_blockItems = null;
		
		_owner.sendPacket(new ItemList(_owner, false));
	}
	
	/**
	 * Check if player inventory is in block mode.
	 * @return true if some itemIds blocked
	 */
	public boolean hasInventoryBlock()
	{
		return (_blockMode > -1 && _blockItems != null && _blockItems.length > 0);
	}
	
	/**
	 * Return block mode
	 * @return int {@link PcInventory#_blockMode}
	 */
	public int getBlockMode()
	{
		return _blockMode;
	}
	
	/**
	 * Return TIntArrayList with blocked item ids
	 * @return TIntArrayList
	 */
	public int[] getBlockItems()
	{
		return _blockItems;
	}
	
	/**
	 * Check if player can use item by itemid
	 * @param itemId int
	 * @return true if can use
	 */
	public boolean canManipulateWithItemId(int itemId)
	{
		if ((_blockMode == 0 && Util.contains(_blockItems, itemId)) || (_blockMode == 1 && !Util.contains(_blockItems, itemId)))
			return false;
		return true;
	}
	
	@Override
	protected void addItem(ItemInstance item)
	{
		synchronized (_items)
		{
			if (item.isQuestItem())
				_questSlots++;
			
			super.addItem(item);
		}
	}
	
	public int getSize(boolean quest)
	{
		if (quest)
			return _questSlots;
		
		return getSize() - _questSlots;
	}
	
	@Override
	public String toString()
	{
		return getClass().getSimpleName() + "[" + _owner + "]";
	}
}