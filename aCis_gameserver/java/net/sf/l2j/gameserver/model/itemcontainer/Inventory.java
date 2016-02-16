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

import net.sf.l2j.Config;
import net.sf.l2j.L2DatabaseFactory;
import net.sf.l2j.gameserver.datatables.ItemTable;
import net.sf.l2j.gameserver.model.L2Object;
import net.sf.l2j.gameserver.model.L2World;
import net.sf.l2j.gameserver.model.actor.L2Playable;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.model.item.instance.ItemInstance;
import net.sf.l2j.gameserver.model.item.instance.ItemInstance.ItemLocation;
import net.sf.l2j.gameserver.model.item.kind.Item;
import net.sf.l2j.gameserver.model.item.type.ArmorType;
import net.sf.l2j.gameserver.model.item.type.EtcItemType;
import net.sf.l2j.gameserver.model.item.type.WeaponType;
import net.sf.l2j.gameserver.model.itemcontainer.listeners.OnEquipListener;
import net.sf.l2j.gameserver.model.itemcontainer.listeners.StatsListener;

/**
 * This class manages inventory
 * @author Advi
 */
public abstract class Inventory extends ItemContainer
{
	public static final int PAPERDOLL_UNDER = 0;
	public static final int PAPERDOLL_LEAR = 1;
	public static final int PAPERDOLL_REAR = 2;
	public static final int PAPERDOLL_NECK = 3;
	public static final int PAPERDOLL_LFINGER = 4;
	public static final int PAPERDOLL_RFINGER = 5;
	public static final int PAPERDOLL_HEAD = 6;
	public static final int PAPERDOLL_RHAND = 7;
	public static final int PAPERDOLL_LHAND = 8;
	public static final int PAPERDOLL_GLOVES = 9;
	public static final int PAPERDOLL_CHEST = 10;
	public static final int PAPERDOLL_LEGS = 11;
	public static final int PAPERDOLL_FEET = 12;
	public static final int PAPERDOLL_BACK = 13;
	public static final int PAPERDOLL_FACE = 14;
	public static final int PAPERDOLL_HAIR = 15;
	public static final int PAPERDOLL_HAIRALL = 16;
	public static final int PAPERDOLL_TOTALSLOTS = 17;
	
	private final ItemInstance[] _paperdoll;
	private final List<OnEquipListener> _paperdollListeners;
	
	// protected to be accessed from child classes only
	protected int _totalWeight;
	
	// used to quickly check for using of items of special type
	private int _wornMask;
	
	// Recorder of alterations in inventory
	private static final class ChangeRecorder implements OnEquipListener
	{
		private final Inventory _inventory;
		private final List<ItemInstance> _changed;
		
		/**
		 * Constructor of the ChangeRecorder
		 * @param inventory
		 */
		ChangeRecorder(Inventory inventory)
		{
			_inventory = inventory;
			_changed = new ArrayList<>();
			_inventory.addPaperdollListener(this);
		}
		
		/**
		 * Add alteration in inventory when item equipped
		 */
		@Override
		public void onEquip(int slot, ItemInstance item, L2Playable actor)
		{
			if (!_changed.contains(item))
				_changed.add(item);
		}
		
		/**
		 * Add alteration in inventory when item unequipped
		 */
		@Override
		public void onUnequip(int slot, ItemInstance item, L2Playable actor)
		{
			if (!_changed.contains(item))
				_changed.add(item);
		}
		
		/**
		 * Returns alterations in inventory
		 * @return ItemInstance[] : array of alterated items
		 */
		public ItemInstance[] getChangedItems()
		{
			return _changed.toArray(new ItemInstance[_changed.size()]);
		}
	}
	
	/**
	 * Constructor of the inventory
	 */
	protected Inventory()
	{
		_paperdoll = new ItemInstance[PAPERDOLL_TOTALSLOTS];
		_paperdollListeners = new ArrayList<>();
		
		// common
		addPaperdollListener(StatsListener.getInstance());
	}
	
	protected abstract ItemLocation getEquipLocation();
	
	/**
	 * Returns the instance of new ChangeRecorder
	 * @return ChangeRecorder
	 */
	public ChangeRecorder newRecorder()
	{
		return new ChangeRecorder(this);
	}
	
	/**
	 * Drop item from inventory and updates database
	 * @param process : String Identifier of process triggering this action
	 * @param item : ItemInstance to be dropped
	 * @param actor : L2PcInstance Player requesting the item drop
	 * @param reference : L2Object Object referencing current action like NPC selling item or previous item in transformation
	 * @return ItemInstance corresponding to the destroyed item or the updated item in inventory
	 */
	public ItemInstance dropItem(String process, ItemInstance item, L2PcInstance actor, L2Object reference)
	{
		if (item == null)
			return null;
		
		synchronized (item)
		{
			if (!_items.contains(item))
				return null;
			
			removeItem(item);
			item.setOwnerId(process, 0, actor, reference);
			item.setLocation(ItemLocation.VOID);
			item.setLastChange(ItemInstance.REMOVED);
			
			item.updateDatabase();
			refreshWeight();
		}
		return item;
	}
	
	/**
	 * Drop item from inventory by using its <B>objectID</B> and updates database
	 * @param process : String Identifier of process triggering this action
	 * @param objectId : int Item Instance identifier of the item to be dropped
	 * @param count : int Quantity of items to be dropped
	 * @param actor : L2PcInstance Player requesting the item drop
	 * @param reference : L2Object Object referencing current action like NPC selling item or previous item in transformation
	 * @return ItemInstance corresponding to the destroyed item or the updated item in inventory
	 */
	public ItemInstance dropItem(String process, int objectId, int count, L2PcInstance actor, L2Object reference)
	{
		ItemInstance item = getItemByObjectId(objectId);
		if (item == null)
			return null;
		
		synchronized (item)
		{
			if (!_items.contains(item))
				return null;
			
			// Adjust item quantity and create new instance to drop
			// Directly drop entire item
			if (item.getCount() > count)
			{
				item.changeCount(process, -count, actor, reference);
				item.setLastChange(ItemInstance.MODIFIED);
				item.updateDatabase();
				
				item = ItemTable.getInstance().createItem(process, item.getItemId(), count, actor, reference);
				item.updateDatabase();
				refreshWeight();
				return item;
			}
		}
		return dropItem(process, item, actor, reference);
	}
	
	/**
	 * Adds item to inventory for further adjustments and Equip it if necessary (itemlocation defined)<BR>
	 * <BR>
	 * @param item : ItemInstance to be added from inventory
	 */
	@Override
	protected void addItem(ItemInstance item)
	{
		super.addItem(item);
		if (item.isEquipped())
			equipItem(item);
	}
	
	/**
	 * Removes item from inventory for further adjustments.
	 * @param item : ItemInstance to be removed from inventory
	 */
	@Override
	protected boolean removeItem(ItemInstance item)
	{
		// Unequip item if equipped
		for (int i = 0; i < _paperdoll.length; i++)
		{
			if (_paperdoll[i] == item)
				unEquipItemInSlot(i);
		}
		return super.removeItem(item);
	}
	
	/**
	 * @param slot The slot to check.
	 * @return The ItemInstance item in the paperdoll slot.
	 */
	public ItemInstance getPaperdollItem(int slot)
	{
		return _paperdoll[slot];
	}
	
	/**
	 * @return The list of worn ItemInstance items.
	 */
	public List<ItemInstance> getPaperdollItems()
	{
		final List<ItemInstance> itemsList = new ArrayList<>();
		
		for (final ItemInstance item : _paperdoll)
		{
			if (item != null)
				itemsList.add(item);
		}
		return itemsList;
	}
	
	public static int getPaperdollIndex(int slot)
	{
		switch (slot)
		{
			case Item.SLOT_UNDERWEAR:
				return PAPERDOLL_UNDER;
			case Item.SLOT_R_EAR:
				return PAPERDOLL_REAR;
			case Item.SLOT_L_EAR:
				return PAPERDOLL_LEAR;
			case Item.SLOT_NECK:
				return PAPERDOLL_NECK;
			case Item.SLOT_R_FINGER:
				return PAPERDOLL_RFINGER;
			case Item.SLOT_L_FINGER:
				return PAPERDOLL_LFINGER;
			case Item.SLOT_HEAD:
				return PAPERDOLL_HEAD;
			case Item.SLOT_R_HAND:
			case Item.SLOT_LR_HAND:
				return PAPERDOLL_RHAND;
			case Item.SLOT_L_HAND:
				return PAPERDOLL_LHAND;
			case Item.SLOT_GLOVES:
				return PAPERDOLL_GLOVES;
			case Item.SLOT_CHEST:
			case Item.SLOT_FULL_ARMOR:
			case Item.SLOT_ALLDRESS:
				return PAPERDOLL_CHEST;
			case Item.SLOT_LEGS:
				return PAPERDOLL_LEGS;
			case Item.SLOT_FEET:
				return PAPERDOLL_FEET;
			case Item.SLOT_BACK:
				return PAPERDOLL_BACK;
			case Item.SLOT_FACE:
			case Item.SLOT_HAIRALL:
				return PAPERDOLL_FACE;
			case Item.SLOT_HAIR:
				return PAPERDOLL_HAIR;
		}
		return -1;
	}
	
	/**
	 * @param slot Item slot identifier
	 * @return the ItemInstance item in the paperdoll Item slot
	 */
	public ItemInstance getPaperdollItemByL2ItemId(int slot)
	{
		int index = getPaperdollIndex(slot);
		if (index == -1)
			return null;
		
		return _paperdoll[index];
	}
	
	/**
	 * Returns the ID of the item in the paperdol slot
	 * @param slot : int designating the slot
	 * @return int designating the ID of the item
	 */
	public int getPaperdollItemId(int slot)
	{
		ItemInstance item = _paperdoll[slot];
		if (item != null)
			return item.getItemId();
		
		return 0;
	}
	
	public int getPaperdollAugmentationId(int slot)
	{
		ItemInstance item = _paperdoll[slot];
		if (item != null)
		{
			if (item.getAugmentation() != null)
				return item.getAugmentation().getAugmentationId();
		}
		return 0;
	}
	
	/**
	 * Returns the objectID associated to the item in the paperdoll slot
	 * @param slot : int pointing out the slot
	 * @return int designating the objectID
	 */
	public int getPaperdollObjectId(int slot)
	{
		ItemInstance item = _paperdoll[slot];
		if (item != null)
			return item.getObjectId();
		
		return 0;
	}
	
	/**
	 * Adds new inventory's paperdoll listener
	 * @param listener PaperdollListener pointing out the listener
	 */
	public synchronized void addPaperdollListener(OnEquipListener listener)
	{
		assert !_paperdollListeners.contains(listener);
		_paperdollListeners.add(listener);
	}
	
	/**
	 * Removes a paperdoll listener
	 * @param listener PaperdollListener pointing out the listener to be deleted
	 */
	public synchronized void removePaperdollListener(OnEquipListener listener)
	{
		_paperdollListeners.remove(listener);
	}
	
	/**
	 * Equips an item in the given slot of the paperdoll. <U><I>Remark :</I></U> The item <B>HAS TO BE</B> already in the inventory
	 * @param slot : int pointing out the slot of the paperdoll
	 * @param item : ItemInstance pointing out the item to add in slot
	 * @return ItemInstance designating the item placed in the slot before
	 */
	public synchronized ItemInstance setPaperdollItem(int slot, ItemInstance item)
	{
		ItemInstance old = _paperdoll[slot];
		if (old != item)
		{
			if (old != null)
			{
				_paperdoll[slot] = null;
				// Put old item from paperdoll slot to base location
				old.setLocation(getBaseLocation());
				old.setLastChange(ItemInstance.MODIFIED);
				
				// delete armor mask flag (in case of two-piece armor it does not matter, we need to deactivate mask too)
				_wornMask &= ~old.getItem().getItemMask();
				
				// Notify all paperdoll listener in order to unequip old item in slot
				for (OnEquipListener listener : _paperdollListeners)
				{
					if (listener == null)
						continue;
					
					listener.onUnequip(slot, old, (L2Playable) getOwner());
				}
				old.updateDatabase();
			}
			// Add new item in slot of paperdoll
			if (item != null)
			{
				_paperdoll[slot] = item;
				item.setLocation(getEquipLocation(), slot);
				item.setLastChange(ItemInstance.MODIFIED);
				
				// activate mask (check 2nd armor part for two-piece armors)
				Item armor = item.getItem();
				if (armor.getBodyPart() == Item.SLOT_CHEST)
				{
					ItemInstance legs = _paperdoll[PAPERDOLL_LEGS];
					if (legs != null && legs.getItem().getItemMask() == armor.getItemMask())
						_wornMask |= armor.getItemMask();
				}
				else if (armor.getBodyPart() == Item.SLOT_LEGS)
				{
					ItemInstance legs = _paperdoll[PAPERDOLL_CHEST];
					if (legs != null && legs.getItem().getItemMask() == armor.getItemMask())
						_wornMask |= armor.getItemMask();
				}
				else
					_wornMask |= armor.getItemMask();
				
				for (OnEquipListener listener : _paperdollListeners)
				{
					if (listener == null)
						continue;
					
					listener.onEquip(slot, item, (L2Playable) getOwner());
				}
				item.updateDatabase();
			}
		}
		return old;
	}
	
	/**
	 * Return the mask of worn item
	 * @return int
	 */
	public int getWornMask()
	{
		return _wornMask;
	}
	
	public int getSlotFromItem(ItemInstance item)
	{
		int slot = -1;
		int location = item.getLocationSlot();
		
		switch (location)
		{
			case PAPERDOLL_UNDER:
				slot = Item.SLOT_UNDERWEAR;
				break;
			case PAPERDOLL_LEAR:
				slot = Item.SLOT_L_EAR;
				break;
			case PAPERDOLL_REAR:
				slot = Item.SLOT_R_EAR;
				break;
			case PAPERDOLL_NECK:
				slot = Item.SLOT_NECK;
				break;
			case PAPERDOLL_RFINGER:
				slot = Item.SLOT_R_FINGER;
				break;
			case PAPERDOLL_LFINGER:
				slot = Item.SLOT_L_FINGER;
				break;
			case PAPERDOLL_HAIR:
				slot = Item.SLOT_HAIR;
				break;
			case PAPERDOLL_FACE:
				slot = Item.SLOT_FACE;
				break;
			case PAPERDOLL_HEAD:
				slot = Item.SLOT_HEAD;
				break;
			case PAPERDOLL_RHAND:
				slot = Item.SLOT_R_HAND;
				break;
			case PAPERDOLL_LHAND:
				slot = Item.SLOT_L_HAND;
				break;
			case PAPERDOLL_GLOVES:
				slot = Item.SLOT_GLOVES;
				break;
			case PAPERDOLL_CHEST:
				slot = item.getItem().getBodyPart();
				break;// fall through
			case PAPERDOLL_LEGS:
				slot = Item.SLOT_LEGS;
				break;
			case PAPERDOLL_BACK:
				slot = Item.SLOT_BACK;
				break;
			case PAPERDOLL_FEET:
				slot = Item.SLOT_FEET;
				break;
		}
		
		return slot;
	}
	
	/**
	 * Unequips item in body slot and returns alterations.
	 * @param slot : int designating the slot of the paperdoll
	 * @return ItemInstance[] : list of changes
	 */
	public ItemInstance[] unEquipItemInBodySlotAndRecord(int slot)
	{
		Inventory.ChangeRecorder recorder = newRecorder();
		
		try
		{
			unEquipItemInBodySlot(slot);
		}
		finally
		{
			removePaperdollListener(recorder);
		}
		return recorder.getChangedItems();
	}
	
	/**
	 * Sets item in slot of the paperdoll to null value
	 * @param pdollSlot : int designating the slot
	 * @return ItemInstance designating the item in slot before change
	 */
	public ItemInstance unEquipItemInSlot(int pdollSlot)
	{
		return setPaperdollItem(pdollSlot, null);
	}
	
	/**
	 * Unepquips item in slot and returns alterations
	 * @param slot : int designating the slot
	 * @return ItemInstance[] : list of items altered
	 */
	public ItemInstance[] unEquipItemInSlotAndRecord(int slot)
	{
		Inventory.ChangeRecorder recorder = newRecorder();
		
		try
		{
			unEquipItemInSlot(slot);
			if (getOwner() instanceof L2PcInstance)
				((L2PcInstance) getOwner()).refreshExpertisePenalty();
		}
		finally
		{
			removePaperdollListener(recorder);
		}
		return recorder.getChangedItems();
	}
	
	/**
	 * Unequips item in slot (i.e. equips with default value)
	 * @param slot : int designating the slot
	 * @return the instance of the item.
	 */
	public ItemInstance unEquipItemInBodySlot(int slot)
	{
		if (Config.DEBUG)
			_log.fine("--- unequip body slot:" + slot);
		
		int pdollSlot = -1;
		
		switch (slot)
		{
			case Item.SLOT_L_EAR:
				pdollSlot = PAPERDOLL_LEAR;
				break;
			case Item.SLOT_R_EAR:
				pdollSlot = PAPERDOLL_REAR;
				break;
			case Item.SLOT_NECK:
				pdollSlot = PAPERDOLL_NECK;
				break;
			case Item.SLOT_R_FINGER:
				pdollSlot = PAPERDOLL_RFINGER;
				break;
			case Item.SLOT_L_FINGER:
				pdollSlot = PAPERDOLL_LFINGER;
				break;
			case Item.SLOT_HAIR:
				pdollSlot = PAPERDOLL_HAIR;
				break;
			case Item.SLOT_FACE:
				pdollSlot = PAPERDOLL_FACE;
				break;
			case Item.SLOT_HAIRALL:
				setPaperdollItem(PAPERDOLL_FACE, null);
				pdollSlot = PAPERDOLL_FACE;
				break;
			case Item.SLOT_HEAD:
				pdollSlot = PAPERDOLL_HEAD;
				break;
			case Item.SLOT_R_HAND:
			case Item.SLOT_LR_HAND:
				pdollSlot = PAPERDOLL_RHAND;
				break;
			case Item.SLOT_L_HAND:
				pdollSlot = PAPERDOLL_LHAND;
				break;
			case Item.SLOT_GLOVES:
				pdollSlot = PAPERDOLL_GLOVES;
				break;
			case Item.SLOT_CHEST:
			case Item.SLOT_FULL_ARMOR:
			case Item.SLOT_ALLDRESS:
				pdollSlot = PAPERDOLL_CHEST;
				break;
			case Item.SLOT_LEGS:
				pdollSlot = PAPERDOLL_LEGS;
				break;
			case Item.SLOT_BACK:
				pdollSlot = PAPERDOLL_BACK;
				break;
			case Item.SLOT_FEET:
				pdollSlot = PAPERDOLL_FEET;
				break;
			case Item.SLOT_UNDERWEAR:
				pdollSlot = PAPERDOLL_UNDER;
				break;
			default:
				_log.info("Unhandled slot type: " + slot);
		}
		if (pdollSlot >= 0)
		{
			ItemInstance old = setPaperdollItem(pdollSlot, null);
			if (old != null)
			{
				if (getOwner() instanceof L2PcInstance)
					((L2PcInstance) getOwner()).refreshExpertisePenalty();
			}
			return old;
		}
		return null;
	}
	
	/**
	 * Equips item and returns list of alterations<BR>
	 * <B>If you dont need return value use {@link Inventory#equipItem(ItemInstance)} instead</B>
	 * @param item : ItemInstance corresponding to the item
	 * @return ItemInstance[] : list of alterations
	 */
	public ItemInstance[] equipItemAndRecord(ItemInstance item)
	{
		Inventory.ChangeRecorder recorder = newRecorder();
		
		try
		{
			equipItem(item);
		}
		finally
		{
			removePaperdollListener(recorder);
		}
		return recorder.getChangedItems();
	}
	
	/**
	 * Equips item in slot of paperdoll.
	 * @param item : ItemInstance designating the item and slot used.
	 */
	public void equipItem(ItemInstance item)
	{
		if (getOwner() instanceof L2PcInstance)
		{
			// Can't equip item if you are in shop mod or hero item and you're not hero.
			if (((L2PcInstance) getOwner()).isInStoreMode() || (!((L2PcInstance) getOwner()).isHero() && item.isHeroItem()))
				return;
		}
		
		int targetSlot = item.getItem().getBodyPart();
		
		// check if player wear formal
		ItemInstance formal = getPaperdollItem(PAPERDOLL_CHEST);
		if (formal != null && formal.getItem().getBodyPart() == Item.SLOT_ALLDRESS)
		{
			// only chest target can pass this
			switch (targetSlot)
			{
				case Item.SLOT_LR_HAND:
				case Item.SLOT_L_HAND:
				case Item.SLOT_R_HAND:
					unEquipItemInBodySlotAndRecord(Item.SLOT_ALLDRESS);
					break;
				case Item.SLOT_LEGS:
				case Item.SLOT_FEET:
				case Item.SLOT_GLOVES:
				case Item.SLOT_HEAD:
					return;
			}
		}
		
		switch (targetSlot)
		{
			case Item.SLOT_LR_HAND:
				setPaperdollItem(PAPERDOLL_LHAND, null);
				setPaperdollItem(PAPERDOLL_RHAND, item);
				break;
			
			case Item.SLOT_L_HAND:
				ItemInstance rh = getPaperdollItem(PAPERDOLL_RHAND);
				if (rh != null && rh.getItem().getBodyPart() == Item.SLOT_LR_HAND && !((rh.getItemType() == WeaponType.BOW && item.getItemType() == EtcItemType.ARROW) || (rh.getItemType() == WeaponType.FISHINGROD && item.getItemType() == EtcItemType.LURE)))
					setPaperdollItem(PAPERDOLL_RHAND, null);
				
				setPaperdollItem(PAPERDOLL_LHAND, item);
				break;
			
			case Item.SLOT_R_HAND:
				// dont care about arrows, listener will unequip them (hopefully)
				setPaperdollItem(PAPERDOLL_RHAND, item);
				break;
			
			case Item.SLOT_L_EAR:
			case Item.SLOT_R_EAR:
			case Item.SLOT_L_EAR | Item.SLOT_R_EAR:
				if (_paperdoll[PAPERDOLL_LEAR] == null)
					setPaperdollItem(PAPERDOLL_LEAR, item);
				else if (_paperdoll[PAPERDOLL_REAR] == null)
					setPaperdollItem(PAPERDOLL_REAR, item);
				else
				{
					if (_paperdoll[PAPERDOLL_REAR].getItemId() == item.getItemId())
						setPaperdollItem(PAPERDOLL_LEAR, item);
					else if (_paperdoll[PAPERDOLL_LEAR].getItemId() == item.getItemId())
						setPaperdollItem(PAPERDOLL_REAR, item);
					else
						setPaperdollItem(PAPERDOLL_LEAR, item);
				}
				break;
			
			case Item.SLOT_L_FINGER:
			case Item.SLOT_R_FINGER:
			case Item.SLOT_L_FINGER | Item.SLOT_R_FINGER:
				if (_paperdoll[PAPERDOLL_LFINGER] == null)
					setPaperdollItem(PAPERDOLL_LFINGER, item);
				else if (_paperdoll[PAPERDOLL_RFINGER] == null)
					setPaperdollItem(PAPERDOLL_RFINGER, item);
				else
				{
					if (_paperdoll[PAPERDOLL_RFINGER].getItemId() == item.getItemId())
						setPaperdollItem(PAPERDOLL_LFINGER, item);
					else if (_paperdoll[PAPERDOLL_LFINGER].getItemId() == item.getItemId())
						setPaperdollItem(PAPERDOLL_RFINGER, item);
					else
						setPaperdollItem(PAPERDOLL_LFINGER, item);
				}
				break;
			
			case Item.SLOT_NECK:
				setPaperdollItem(PAPERDOLL_NECK, item);
				break;
			
			case Item.SLOT_FULL_ARMOR:
				setPaperdollItem(PAPERDOLL_LEGS, null);
				setPaperdollItem(PAPERDOLL_CHEST, item);
				break;
			
			case Item.SLOT_CHEST:
				setPaperdollItem(PAPERDOLL_CHEST, item);
				break;
			
			case Item.SLOT_LEGS:
				// handle full armor
				ItemInstance chest = getPaperdollItem(PAPERDOLL_CHEST);
				if (chest != null && chest.getItem().getBodyPart() == Item.SLOT_FULL_ARMOR)
					setPaperdollItem(PAPERDOLL_CHEST, null);
				
				setPaperdollItem(PAPERDOLL_LEGS, item);
				break;
			
			case Item.SLOT_FEET:
				setPaperdollItem(PAPERDOLL_FEET, item);
				break;
			
			case Item.SLOT_GLOVES:
				setPaperdollItem(PAPERDOLL_GLOVES, item);
				break;
			
			case Item.SLOT_HEAD:
				setPaperdollItem(PAPERDOLL_HEAD, item);
				break;
			
			case Item.SLOT_FACE:
				ItemInstance hair = getPaperdollItem(PAPERDOLL_HAIR);
				if (hair != null && hair.getItem().getBodyPart() == Item.SLOT_HAIRALL)
					setPaperdollItem(PAPERDOLL_HAIR, null);
				
				setPaperdollItem(PAPERDOLL_FACE, item);
				break;
			
			case Item.SLOT_HAIR:
				ItemInstance face = getPaperdollItem(PAPERDOLL_FACE);
				if (face != null && face.getItem().getBodyPart() == Item.SLOT_HAIRALL)
					setPaperdollItem(PAPERDOLL_FACE, null);
				
				setPaperdollItem(PAPERDOLL_HAIR, item);
				break;
			
			case Item.SLOT_HAIRALL:
				setPaperdollItem(PAPERDOLL_FACE, null);
				setPaperdollItem(PAPERDOLL_HAIR, item);
				break;
			
			case Item.SLOT_UNDERWEAR:
				setPaperdollItem(PAPERDOLL_UNDER, item);
				break;
			
			case Item.SLOT_BACK:
				setPaperdollItem(PAPERDOLL_BACK, item);
				break;
			
			case Item.SLOT_ALLDRESS:
				// formal dress
				setPaperdollItem(PAPERDOLL_LEGS, null);
				setPaperdollItem(PAPERDOLL_LHAND, null);
				setPaperdollItem(PAPERDOLL_RHAND, null);
				setPaperdollItem(PAPERDOLL_HEAD, null);
				setPaperdollItem(PAPERDOLL_FEET, null);
				setPaperdollItem(PAPERDOLL_GLOVES, null);
				setPaperdollItem(PAPERDOLL_CHEST, item);
				break;
			
			default:
				_log.warning("Unknown body slot " + targetSlot + " for Item ID:" + item.getItemId());
		}
	}
	
	/**
	 * Equips pet item in slot of paperdoll. Concerning pets, armors go to chest location, and weapon to R-hand.
	 * @param item : ItemInstance designating the item and slot used.
	 */
	public void equipPetItem(ItemInstance item)
	{
		if (getOwner() instanceof L2PcInstance)
		{
			// Can't equip item if you are in shop mod.
			if (((L2PcInstance) getOwner()).isInStoreMode())
				return;
		}
		
		// Verify first if item is a pet item.
		if (item.isPetItem())
		{
			// Check then about type of item : armor or weapon. Feed the correct slot.
			if (item.getItemType() == WeaponType.PET)
				setPaperdollItem(PAPERDOLL_RHAND, item);
			else if (item.getItemType() == ArmorType.PET)
				setPaperdollItem(PAPERDOLL_CHEST, item);
		}
	}
	
	/**
	 * Refresh the weight of equipment loaded
	 */
	@Override
	protected void refreshWeight()
	{
		int weight = 0;
		
		for (ItemInstance item : _items)
		{
			if (item != null && item.getItem() != null)
				weight += item.getItem().getWeight() * item.getCount();
		}
		
		_totalWeight = weight;
	}
	
	/**
	 * Returns the totalWeight.
	 * @return int
	 */
	public int getTotalWeight()
	{
		return _totalWeight;
	}
	
	/**
	 * Return the ItemInstance of the arrows needed for this bow.<BR>
	 * <BR>
	 * @param bow : L2Item designating the bow
	 * @return ItemInstance pointing out arrows for bow
	 */
	public ItemInstance findArrowForBow(Item bow)
	{
		if (bow == null)
			return null;
		
		int arrowsId = 0;
		
		switch (bow.getCrystalType())
		{
			default:
			case NONE:
				arrowsId = 17;
				break; // Wooden arrow
			case D:
				arrowsId = 1341;
				break; // Bone arrow
			case C:
				arrowsId = 1342;
				break; // Fine steel arrow
			case B:
				arrowsId = 1343;
				break; // Silver arrow
			case A:
				arrowsId = 1344;
				break; // Mithril arrow
			case S:
				arrowsId = 1345;
				break; // Shining arrow
		}
		
		// Get the ItemInstance corresponding to the item identifier and return it
		return getItemByItemId(arrowsId);
	}
	
	/**
	 * Get back items in inventory from database
	 */
	@Override
	public void restore()
	{
		try (Connection con = L2DatabaseFactory.getInstance().getConnection())
		{
			PreparedStatement statement = con.prepareStatement("SELECT object_id, item_id, count, enchant_level, loc, loc_data, custom_type1, custom_type2, mana_left, time FROM items WHERE owner_id=? AND (loc=? OR loc=?) ORDER BY loc_data");
			statement.setInt(1, getOwnerId());
			statement.setString(2, getBaseLocation().name());
			statement.setString(3, getEquipLocation().name());
			ResultSet inv = statement.executeQuery();
			
			while (inv.next())
			{
				ItemInstance item = ItemInstance.restoreFromDb(getOwnerId(), inv);
				if (item == null)
					continue;
				
				if (getOwner() instanceof L2PcInstance)
				{
					if (!((L2PcInstance) getOwner()).isHero() && item.isHeroItem())
						item.setLocation(ItemLocation.INVENTORY);
				}
				
				L2World.getInstance().storeObject(item);
				
				// If stackable item is found in inventory just add to current quantity
				if (item.isStackable() && getItemByItemId(item.getItemId()) != null)
					addItem("Restore", item, getOwner().getActingPlayer(), null);
				else
					addItem(item);
			}
			inv.close();
			statement.close();
			refreshWeight();
		}
		catch (Exception e)
		{
			_log.log(Level.WARNING, "Could not restore inventory: " + e.getMessage(), e);
		}
	}
	
	/**
	 * Re-notify to paperdoll listeners every equipped item
	 */
	public void reloadEquippedItems()
	{
		for (ItemInstance element : _paperdoll)
		{
			if (element == null)
				continue;
			
			int slot = element.getLocationSlot();
			
			for (OnEquipListener listener : _paperdollListeners)
			{
				if (listener == null)
					continue;
				
				listener.onUnequip(slot, element, (L2Playable) getOwner());
				listener.onEquip(slot, element, (L2Playable) getOwner());
			}
		}
	}
}