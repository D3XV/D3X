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

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;

import net.sf.l2j.Config;
import net.sf.l2j.gameserver.datatables.ItemTable;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.model.item.instance.ItemInstance;
import net.sf.l2j.gameserver.model.item.kind.Item;
import net.sf.l2j.gameserver.model.itemcontainer.PcInventory;
import net.sf.l2j.gameserver.network.SystemMessageId;
import net.sf.l2j.gameserver.network.serverpackets.InventoryUpdate;
import net.sf.l2j.gameserver.network.serverpackets.StatusUpdate;
import net.sf.l2j.gameserver.network.serverpackets.SystemMessage;
import net.sf.l2j.gameserver.util.Util;

/**
 * @author Advi
 */
public class TradeList
{
	public static class TradeItem
	{
		private int _objectId;
		private final Item _item;
		private int _enchant;
		private int _count;
		private int _price;
		
		public TradeItem(ItemInstance item, int count, int price)
		{
			_objectId = item.getObjectId();
			_item = item.getItem();
			_enchant = item.getEnchantLevel();
			_count = count;
			_price = price;
		}
		
		public TradeItem(Item item, int count, int price)
		{
			_objectId = 0;
			_item = item;
			_enchant = 0;
			_count = count;
			_price = price;
		}
		
		public TradeItem(TradeItem item, int count, int price)
		{
			_objectId = item.getObjectId();
			_item = item.getItem();
			_enchant = item.getEnchant();
			_count = count;
			_price = price;
		}
		
		public void setObjectId(int objectId)
		{
			_objectId = objectId;
		}
		
		public int getObjectId()
		{
			return _objectId;
		}
		
		public Item getItem()
		{
			return _item;
		}
		
		public void setEnchant(int enchant)
		{
			_enchant = enchant;
		}
		
		public int getEnchant()
		{
			return _enchant;
		}
		
		public void setCount(int count)
		{
			_count = count;
		}
		
		public int getCount()
		{
			return _count;
		}
		
		public void setPrice(int price)
		{
			_price = price;
		}
		
		public int getPrice()
		{
			return _price;
		}
	}
	
	private static final Logger _log = Logger.getLogger(TradeList.class.getName());
	
	private final L2PcInstance _owner;
	private L2PcInstance _partner;
	private final List<TradeItem> _items;
	private String _title;
	private boolean _packaged;
	
	private boolean _confirmed = false;
	private boolean _locked = false;
	
	public TradeList(L2PcInstance owner)
	{
		_items = new ArrayList<>();
		_owner = owner;
	}
	
	public L2PcInstance getOwner()
	{
		return _owner;
	}
	
	public void setPartner(L2PcInstance partner)
	{
		_partner = partner;
	}
	
	public L2PcInstance getPartner()
	{
		return _partner;
	}
	
	public void setTitle(String title)
	{
		_title = title;
	}
	
	public String getTitle()
	{
		return _title;
	}
	
	public boolean isLocked()
	{
		return _locked;
	}
	
	public boolean isConfirmed()
	{
		return _confirmed;
	}
	
	public boolean isPackaged()
	{
		return _packaged;
	}
	
	public void setPackaged(boolean value)
	{
		_packaged = value;
	}
	
	/**
	 * Retrieves items from TradeList
	 * @return an array consisting of items.
	 */
	public TradeItem[] getItems()
	{
		return _items.toArray(new TradeItem[_items.size()]);
	}
	
	/**
	 * Returns the list of items in inventory available for transaction
	 * @param inventory The inventory to make checks on.
	 * @return ItemInstance : items in inventory
	 */
	public TradeItem[] getAvailableItems(PcInventory inventory)
	{
		List<TradeItem> list = new ArrayList<>();
		for (TradeItem item : _items)
		{
			item = new TradeItem(item, item.getCount(), item.getPrice());
			inventory.adjustAvailableItem(item);
			list.add(item);
		}
		return list.toArray(new TradeItem[list.size()]);
	}
	
	/**
	 * @return Item List size
	 */
	public int getItemCount()
	{
		return _items.size();
	}
	
	/**
	 * Adjust available item from Inventory by the one in this list
	 * @param item : ItemInstance to be adjusted
	 * @return TradeItem representing adjusted item
	 */
	public TradeItem adjustAvailableItem(ItemInstance item)
	{
		if (item.isStackable())
		{
			for (TradeItem exclItem : _items)
			{
				if (exclItem.getItem().getItemId() == item.getItemId())
				{
					if (item.getCount() <= exclItem.getCount())
						return null;
					
					return new TradeItem(item, item.getCount() - exclItem.getCount(), item.getReferencePrice());
				}
			}
		}
		return new TradeItem(item, item.getCount(), item.getReferencePrice());
	}
	
	/**
	 * Adjust ItemRequest by corresponding item in this list using its <b>ObjectId</b>
	 * @param item : ItemRequest to be adjusted
	 */
	public void adjustItemRequest(ItemRequest item)
	{
		for (TradeItem filtItem : _items)
		{
			if (filtItem.getObjectId() == item.getObjectId())
			{
				if (filtItem.getCount() < item.getCount())
					item.setCount(filtItem.getCount());
				return;
			}
		}
		item.setCount(0);
	}
	
	/**
	 * Add simplified item to TradeList
	 * @param objectId : int
	 * @param count : int
	 * @return
	 */
	public synchronized TradeItem addItem(int objectId, int count)
	{
		return addItem(objectId, count, 0);
	}
	
	/**
	 * Add item to TradeList
	 * @param objectId : int
	 * @param count : int
	 * @param price : int
	 * @return
	 */
	public synchronized TradeItem addItem(int objectId, int count, int price)
	{
		if (isLocked())
		{
			_log.warning(_owner.getName() + ": Attempt to modify locked TradeList!");
			return null;
		}
		
		L2Object o = L2World.getInstance().findObject(objectId);
		if (!(o instanceof ItemInstance))
		{
			_log.warning(_owner.getName() + ": Attempt to add invalid item to TradeList!");
			return null;
		}
		
		ItemInstance item = (ItemInstance) o;
		
		if (!item.isTradable() || item.isQuestItem())
			return null;
		
		if (count <= 0 || count > item.getCount())
			return null;
		
		if (!item.isStackable() && count > 1)
		{
			_log.warning(_owner.getName() + ": Attempt to add non-stackable item to TradeList with count > 1!");
			return null;
		}
		
		if ((Integer.MAX_VALUE / count) < price)
		{
			_log.warning(_owner.getName() + ": Attempt to overflow adena !");
			return null;
		}
		
		for (TradeItem checkitem : _items)
		{
			if (checkitem.getObjectId() == objectId)
				return null;
		}
		
		TradeItem titem = new TradeItem(item, count, price);
		_items.add(titem);
		
		// If Player has already confirmed this trade, invalidate the confirmation
		invalidateConfirmation();
		return titem;
	}
	
	/**
	 * Add item to TradeList
	 * @param itemId : int
	 * @param count : int
	 * @param price : int
	 * @return
	 */
	public synchronized TradeItem addItemByItemId(int itemId, int count, int price)
	{
		if (isLocked())
		{
			_log.warning(_owner.getName() + ": Attempt to modify locked TradeList!");
			return null;
		}
		
		Item item = ItemTable.getInstance().getTemplate(itemId);
		if (item == null)
		{
			_log.warning(_owner.getName() + ": Attempt to add invalid item to TradeList!");
			return null;
		}
		
		if (!item.isTradable() || item.isQuestItem())
			return null;
		
		if (!item.isStackable() && count > 1)
		{
			_log.warning(_owner.getName() + ": Attempt to add non-stackable item to TradeList with count > 1!");
			return null;
		}
		
		if ((Integer.MAX_VALUE / count) < price)
		{
			_log.warning(_owner.getName() + ": Attempt to overflow adena !");
			return null;
		}
		
		TradeItem titem = new TradeItem(item, count, price);
		_items.add(titem);
		
		// If Player has already confirmed this trade, invalidate the confirmation
		invalidateConfirmation();
		return titem;
	}
	
	/**
	 * Remove item from TradeList
	 * @param objectId : int
	 * @param itemId : int
	 * @param count : int
	 * @return
	 */
	public synchronized TradeItem removeItem(int objectId, int itemId, int count)
	{
		if (isLocked())
		{
			_log.warning(_owner.getName() + ": Attempt to modify locked TradeList!");
			return null;
		}
		
		for (TradeItem titem : _items)
		{
			if (titem.getObjectId() == objectId || titem.getItem().getItemId() == itemId)
			{
				// If Partner has already confirmed this trade, invalidate the confirmation
				if (_partner != null)
				{
					TradeList partnerList = _partner.getActiveTradeList();
					if (partnerList == null)
					{
						_log.warning(_partner.getName() + ": Trading partner (" + _partner.getName() + ") is invalid in this trade!");
						return null;
					}
					partnerList.invalidateConfirmation();
				}
				
				// Reduce item count or complete item
				if (count != -1 && titem.getCount() > count)
					titem.setCount(titem.getCount() - count);
				else
					_items.remove(titem);
				
				return titem;
			}
		}
		return null;
	}
	
	/**
	 * Update items in TradeList according their quantity in owner inventory
	 */
	public synchronized void updateItems()
	{
		for (TradeItem titem : _items)
		{
			ItemInstance item = _owner.getInventory().getItemByObjectId(titem.getObjectId());
			if (item == null || titem.getCount() < 1)
				removeItem(titem.getObjectId(), -1, -1);
			else if (item.getCount() < titem.getCount())
				titem.setCount(item.getCount());
		}
	}
	
	/**
	 * Lockes TradeList, no further changes are allowed
	 */
	public void lock()
	{
		_locked = true;
	}
	
	/**
	 * Clears item list
	 */
	public synchronized void clear()
	{
		_items.clear();
		_locked = false;
	}
	
	/**
	 * Confirms TradeList
	 * @return : boolean
	 */
	public boolean confirm()
	{
		if (_confirmed)
			return true; // Already confirmed
			
		// If Partner has already confirmed this trade, proceed exchange
		if (_partner != null)
		{
			TradeList partnerList = _partner.getActiveTradeList();
			if (partnerList == null)
			{
				_log.warning(_partner.getName() + ": Trading partner (" + _partner.getName() + ") is invalid in this trade!");
				return false;
			}
			
			// Synchronization order to avoid deadlock
			TradeList sync1, sync2;
			if (getOwner().getObjectId() > partnerList.getOwner().getObjectId())
			{
				sync1 = partnerList;
				sync2 = this;
			}
			else
			{
				sync1 = this;
				sync2 = partnerList;
			}
			
			synchronized (sync1)
			{
				synchronized (sync2)
				{
					_confirmed = true;
					if (partnerList.isConfirmed())
					{
						partnerList.lock();
						lock();
						if (!partnerList.validate())
							return false;
						if (!validate())
							return false;
						
						doExchange(partnerList);
					}
					else
						_partner.onTradeConfirm(_owner);
				}
			}
		}
		else
			_confirmed = true;
		
		return _confirmed;
	}
	
	/**
	 * Cancels TradeList confirmation
	 */
	public void invalidateConfirmation()
	{
		_confirmed = false;
	}
	
	/**
	 * Validates TradeList with owner inventory
	 * @return true if ok, false otherwise.
	 */
	private boolean validate()
	{
		// Check for Owner validity
		if (_owner == null || L2World.getInstance().getPlayer(_owner.getObjectId()) == null)
		{
			_log.warning("Invalid owner of TradeList");
			return false;
		}
		
		// Check for Item validity
		for (TradeItem titem : _items)
		{
			ItemInstance item = _owner.checkItemManipulation(titem.getObjectId(), titem.getCount());
			if (item == null)
			{
				_log.warning(_owner.getName() + ": Invalid Item in TradeList");
				return false;
			}
		}
		return true;
	}
	
	/**
	 * Transfers all TradeItems from inventory to partner
	 * @param partner
	 * @param ownerIU
	 * @param partnerIU
	 * @return true if ok, false otherwise.
	 */
	private boolean TransferItems(L2PcInstance partner, InventoryUpdate ownerIU, InventoryUpdate partnerIU)
	{
		for (TradeItem titem : _items)
		{
			ItemInstance oldItem = _owner.getInventory().getItemByObjectId(titem.getObjectId());
			if (oldItem == null)
				return false;
			
			ItemInstance newItem = _owner.getInventory().transferItem("Trade", titem.getObjectId(), titem.getCount(), partner.getInventory(), _owner, _partner);
			if (newItem == null)
				return false;
			
			// Add changes to inventory update packets
			if (ownerIU != null)
			{
				if (oldItem.getCount() > 0 && oldItem != newItem)
					ownerIU.addModifiedItem(oldItem);
				else
					ownerIU.addRemovedItem(oldItem);
			}
			
			if (partnerIU != null)
			{
				if (newItem.getCount() > titem.getCount())
					partnerIU.addModifiedItem(newItem);
				else
					partnerIU.addNewItem(newItem);
			}
		}
		return true;
	}
	
	/**
	 * Count items slots
	 * @param partner
	 * @return
	 */
	public int countItemsSlots(L2PcInstance partner)
	{
		int slots = 0;
		
		for (TradeItem item : _items)
		{
			if (item == null)
				continue;
			
			Item template = ItemTable.getInstance().getTemplate(item.getItem().getItemId());
			if (template == null)
				continue;
			
			if (!template.isStackable())
				slots += item.getCount();
			else if (partner.getInventory().getItemByItemId(item.getItem().getItemId()) == null)
				slots++;
		}
		return slots;
	}
	
	/**
	 * @return weight of items in tradeList
	 */
	public int calcItemsWeight()
	{
		int weight = 0;
		
		for (TradeItem item : _items)
		{
			if (item == null)
				continue;
			
			Item template = ItemTable.getInstance().getTemplate(item.getItem().getItemId());
			if (template == null)
				continue;
			
			weight += item.getCount() * template.getWeight();
		}
		return Math.min(weight, Integer.MAX_VALUE);
	}
	
	/**
	 * Proceeds with trade
	 * @param partnerList
	 */
	private void doExchange(TradeList partnerList)
	{
		boolean success = false;
		
		// check weight and slots
		if ((!getOwner().getInventory().validateWeight(partnerList.calcItemsWeight())) || !(partnerList.getOwner().getInventory().validateWeight(calcItemsWeight())))
		{
			partnerList.getOwner().sendPacket(SystemMessageId.WEIGHT_LIMIT_EXCEEDED);
			getOwner().sendPacket(SystemMessageId.WEIGHT_LIMIT_EXCEEDED);
		}
		else if ((!getOwner().getInventory().validateCapacity(partnerList.countItemsSlots(getOwner()))) || (!partnerList.getOwner().getInventory().validateCapacity(countItemsSlots(partnerList.getOwner()))))
		{
			partnerList.getOwner().sendPacket(SystemMessageId.SLOTS_FULL);
			getOwner().sendPacket(SystemMessageId.SLOTS_FULL);
		}
		else
		{
			// Prepare inventory update packet
			InventoryUpdate ownerIU = new InventoryUpdate();
			InventoryUpdate partnerIU = new InventoryUpdate();
			
			// Transfer items
			partnerList.TransferItems(getOwner(), partnerIU, ownerIU);
			TransferItems(partnerList.getOwner(), ownerIU, partnerIU);
			
			// Send inventory update packet
			_owner.sendPacket(ownerIU);
			_partner.sendPacket(partnerIU);
			
			// Update current load as well
			StatusUpdate playerSU = new StatusUpdate(_owner);
			playerSU.addAttribute(StatusUpdate.CUR_LOAD, _owner.getCurrentLoad());
			_owner.sendPacket(playerSU);
			playerSU = new StatusUpdate(_partner);
			playerSU.addAttribute(StatusUpdate.CUR_LOAD, _partner.getCurrentLoad());
			_partner.sendPacket(playerSU);
			
			success = true;
		}
		// Finish the trade
		partnerList.getOwner().onTradeFinish(success);
		getOwner().onTradeFinish(success);
	}
	
	/**
	 * Buy items from this PrivateStore list
	 * @param player
	 * @param items
	 * @return int: result of trading. 0 - ok, 1 - canceled (no adena), 2 - failed (item error)
	 */
	public synchronized int privateStoreBuy(L2PcInstance player, Set<ItemRequest> items)
	{
		if (_locked)
			return 1;
		
		if (!validate())
		{
			lock();
			return 1;
		}
		
		if (!_owner.isOnline() || !player.isOnline())
			return 1;
		
		int slots = 0;
		int weight = 0;
		int totalPrice = 0;
		
		final PcInventory ownerInventory = _owner.getInventory();
		final PcInventory playerInventory = player.getInventory();
		
		for (ItemRequest item : items)
		{
			boolean found = false;
			
			for (TradeItem ti : _items)
			{
				if (ti.getObjectId() == item.getObjectId())
				{
					if (ti.getPrice() == item.getPrice())
					{
						if (ti.getCount() < item.getCount())
							item.setCount(ti.getCount());
						found = true;
					}
					break;
				}
			}
			// item with this objectId and price not found in tradelist
			if (!found)
			{
				if (isPackaged())
				{
					Util.handleIllegalPlayerAction(player, "[TradeList.privateStoreBuy()] Player " + player.getName() + " tried to cheat the package sell and buy only a part of the package! Ban this player for bot usage!", Config.DEFAULT_PUNISH);
					return 2;
				}
				
				item.setCount(0);
				continue;
			}
			
			// check for overflow in the single item
			if ((Integer.MAX_VALUE / item.getCount()) < item.getPrice())
			{
				// private store attempting to overflow - disable it
				lock();
				return 1;
			}
			
			totalPrice += item.getCount() * item.getPrice();
			// check for overflow of the total price
			if (Integer.MAX_VALUE < totalPrice || totalPrice < 0)
			{
				// private store attempting to overflow - disable it
				lock();
				return 1;
			}
			
			// Check if requested item is available for manipulation
			ItemInstance oldItem = _owner.checkItemManipulation(item.getObjectId(), item.getCount());
			if (oldItem == null || !oldItem.isTradable())
			{
				// private store sell invalid item - disable it
				lock();
				return 2;
			}
			
			Item template = ItemTable.getInstance().getTemplate(item.getItemId());
			if (template == null)
				continue;
			weight += item.getCount() * template.getWeight();
			if (!template.isStackable())
				slots += item.getCount();
			else if (playerInventory.getItemByItemId(item.getItemId()) == null)
				slots++;
		}
		
		if (totalPrice > playerInventory.getAdena())
		{
			player.sendPacket(SystemMessageId.YOU_NOT_ENOUGH_ADENA);
			return 1;
		}
		
		if (!playerInventory.validateWeight(weight))
		{
			player.sendPacket(SystemMessageId.WEIGHT_LIMIT_EXCEEDED);
			return 1;
		}
		
		if (!playerInventory.validateCapacity(slots))
		{
			player.sendPacket(SystemMessageId.SLOTS_FULL);
			return 1;
		}
		
		// Prepare inventory update packets
		final InventoryUpdate ownerIU = new InventoryUpdate();
		final InventoryUpdate playerIU = new InventoryUpdate();
		
		final ItemInstance adenaItem = playerInventory.getAdenaInstance();
		if (!playerInventory.reduceAdena("PrivateStore", totalPrice, player, _owner))
		{
			player.sendPacket(SystemMessageId.YOU_NOT_ENOUGH_ADENA);
			return 1;
		}
		
		playerIU.addItem(adenaItem);
		ownerInventory.addAdena("PrivateStore", totalPrice, _owner, player);
		
		boolean ok = true;
		
		// Transfer items
		for (ItemRequest item : items)
		{
			if (item.getCount() == 0)
				continue;
			
			// Check if requested item is available for manipulation
			ItemInstance oldItem = _owner.checkItemManipulation(item.getObjectId(), item.getCount());
			if (oldItem == null)
			{
				// should not happens - validation already done
				lock();
				ok = false;
				break;
			}
			
			// Proceed with item transfer
			ItemInstance newItem = ownerInventory.transferItem("PrivateStore", item.getObjectId(), item.getCount(), playerInventory, _owner, player);
			if (newItem == null)
			{
				ok = false;
				break;
			}
			removeItem(item.getObjectId(), -1, item.getCount());
			
			// Add changes to inventory update packets
			if (oldItem.getCount() > 0 && oldItem != newItem)
				ownerIU.addModifiedItem(oldItem);
			else
				ownerIU.addRemovedItem(oldItem);
			if (newItem.getCount() > item.getCount())
				playerIU.addModifiedItem(newItem);
			else
				playerIU.addNewItem(newItem);
			
			// Send messages about the transaction to both players
			if (newItem.isStackable())
			{
				SystemMessage msg = SystemMessage.getSystemMessage(SystemMessageId.S1_PURCHASED_S3_S2_S);
				msg.addString(player.getName());
				msg.addItemName(newItem.getItemId());
				msg.addNumber(item.getCount());
				_owner.sendPacket(msg);
				
				msg = SystemMessage.getSystemMessage(SystemMessageId.PURCHASED_S3_S2_S_FROM_S1);
				msg.addString(_owner.getName());
				msg.addItemName(newItem.getItemId());
				msg.addNumber(item.getCount());
				player.sendPacket(msg);
			}
			else
			{
				SystemMessage msg = SystemMessage.getSystemMessage(SystemMessageId.S1_PURCHASED_S2);
				msg.addString(player.getName());
				msg.addItemName(newItem.getItemId());
				_owner.sendPacket(msg);
				
				msg = SystemMessage.getSystemMessage(SystemMessageId.PURCHASED_S2_FROM_S1);
				msg.addString(_owner.getName());
				msg.addItemName(newItem.getItemId());
				player.sendPacket(msg);
			}
		}
		
		// Send inventory update packet
		_owner.sendPacket(ownerIU);
		player.sendPacket(playerIU);
		if (ok)
			return 0;
		
		return 2;
	}
	
	/**
	 * Sell items to this PrivateStore list
	 * @param player
	 * @param items
	 * @return : boolean true if success
	 */
	public synchronized boolean privateStoreSell(L2PcInstance player, ItemRequest[] items)
	{
		if (_locked)
			return false;
		
		if (!_owner.isOnline() || !player.isOnline())
			return false;
		
		boolean ok = false;
		
		final PcInventory ownerInventory = _owner.getInventory();
		final PcInventory playerInventory = player.getInventory();
		
		// Prepare inventory update packet
		final InventoryUpdate ownerIU = new InventoryUpdate();
		final InventoryUpdate playerIU = new InventoryUpdate();
		
		int totalPrice = 0;
		
		for (ItemRequest item : items)
		{
			// searching item in tradelist using itemId
			boolean found = false;
			
			for (TradeItem ti : _items)
			{
				if (ti.getItem().getItemId() == item.getItemId())
				{
					// price should be the same
					if (ti.getPrice() == item.getPrice())
					{
						// if requesting more than available - decrease count
						if (ti.getCount() < item.getCount())
							item.setCount(ti.getCount());
						found = item.getCount() > 0;
					}
					break;
				}
			}
			// not found any item in the tradelist with same itemId and price
			// maybe another player already sold this item ?
			if (!found)
				continue;
			
			// check for overflow in the single item
			if ((Integer.MAX_VALUE / item.getCount()) < item.getPrice())
			{
				lock();
				break;
			}
			
			int _totalPrice = totalPrice + item.getCount() * item.getPrice();
			// check for overflow of the total price
			if (Integer.MAX_VALUE < _totalPrice || _totalPrice < 0)
			{
				lock();
				break;
			}
			
			if (ownerInventory.getAdena() < _totalPrice)
				continue;
			
			// TODO: eventually test and fix this.
			
			// Check if requested item is available for manipulation
			int objectId = item.getObjectId();
			ItemInstance oldItem = player.checkItemManipulation(objectId, item.getCount());
			// private store - buy use same objectId for buying several non-stackable items
			if (oldItem == null)
			{
				// searching other items using same itemId
				oldItem = playerInventory.getItemByItemId(item.getItemId());
				if (oldItem == null)
					continue;
				
				objectId = oldItem.getObjectId();
				oldItem = player.checkItemManipulation(objectId, item.getCount());
				if (oldItem == null)
					continue;
			}
			if (oldItem.getItemId() != item.getItemId())
			{
				Util.handleIllegalPlayerAction(player, player + " is cheating with sell items", Config.DEFAULT_PUNISH);
				return false;
			}
			
			if (!oldItem.isTradable())
				continue;
			
			// Proceed with item transfer
			ItemInstance newItem = playerInventory.transferItem("PrivateStore", objectId, item.getCount(), ownerInventory, player, _owner);
			if (newItem == null)
				continue;
			
			removeItem(-1, item.getItemId(), item.getCount());
			ok = true;
			
			// increase total price only after successful transaction
			totalPrice = _totalPrice;
			
			// Add changes to inventory update packets
			if (oldItem.getCount() > 0 && oldItem != newItem)
				playerIU.addModifiedItem(oldItem);
			else
				playerIU.addRemovedItem(oldItem);
			if (newItem.getCount() > item.getCount())
				ownerIU.addModifiedItem(newItem);
			else
				ownerIU.addNewItem(newItem);
			
			// Send messages about the transaction to both players
			if (newItem.isStackable())
			{
				SystemMessage msg = SystemMessage.getSystemMessage(SystemMessageId.PURCHASED_S3_S2_S_FROM_S1);
				msg.addString(player.getName());
				msg.addItemName(newItem.getItemId());
				msg.addNumber(item.getCount());
				_owner.sendPacket(msg);
				
				msg = SystemMessage.getSystemMessage(SystemMessageId.S1_PURCHASED_S3_S2_S);
				msg.addString(_owner.getName());
				msg.addItemName(newItem.getItemId());
				msg.addNumber(item.getCount());
				player.sendPacket(msg);
			}
			else
			{
				SystemMessage msg = SystemMessage.getSystemMessage(SystemMessageId.PURCHASED_S2_FROM_S1);
				msg.addString(player.getName());
				msg.addItemName(newItem.getItemId());
				_owner.sendPacket(msg);
				
				msg = SystemMessage.getSystemMessage(SystemMessageId.S1_PURCHASED_S2);
				msg.addString(_owner.getName());
				msg.addItemName(newItem.getItemId());
				player.sendPacket(msg);
			}
		}
		
		// Transfer adena
		if (totalPrice > 0)
		{
			if (totalPrice > ownerInventory.getAdena())
				return false;
			
			final ItemInstance adenaItem = ownerInventory.getAdenaInstance();
			ownerInventory.reduceAdena("PrivateStore", totalPrice, _owner, player);
			ownerIU.addItem(adenaItem);
			
			playerInventory.addAdena("PrivateStore", totalPrice, player, _owner);
			playerIU.addItem(playerInventory.getAdenaInstance());
		}
		
		if (ok)
		{
			// Send inventory update packet
			_owner.sendPacket(ownerIU);
			player.sendPacket(playerIU);
		}
		return ok;
	}
}