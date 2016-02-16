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
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;

import net.sf.l2j.Config;
import net.sf.l2j.gameserver.ThreadPoolManager;
import net.sf.l2j.gameserver.ai.CtrlIntention;
import net.sf.l2j.gameserver.datatables.SpawnTable;
import net.sf.l2j.gameserver.model.actor.L2Attackable;
import net.sf.l2j.gameserver.model.actor.L2Character;
import net.sf.l2j.gameserver.model.actor.L2Npc;
import net.sf.l2j.gameserver.model.actor.L2Playable;
import net.sf.l2j.gameserver.model.actor.L2Vehicle;
import net.sf.l2j.gameserver.model.zone.L2ZoneType;
import net.sf.l2j.gameserver.model.zone.type.L2DerbyTrackZone;
import net.sf.l2j.gameserver.model.zone.type.L2PeaceZone;
import net.sf.l2j.gameserver.model.zone.type.L2TownZone;

public final class L2WorldRegion
{
	private final Map<Integer, L2Object> _visibleObjects = new ConcurrentHashMap<>();
	private final Map<Integer, L2Playable> _allPlayable = new ConcurrentHashMap<>();
	
	private final List<L2WorldRegion> _surroundingRegions = new ArrayList<>();
	private final List<L2ZoneType> _zones = new ArrayList<>();
	
	private final int _tileX, _tileY;
	
	private boolean _active;
	private ScheduledFuture<?> _neighborsTask;
	
	public L2WorldRegion(int pTileX, int pTileY)
	{
		_tileX = pTileX;
		_tileY = pTileY;
		
		_active = Config.GRIDS_ALWAYS_ON;
	}
	
	public List<L2ZoneType> getZones()
	{
		return _zones;
	}
	
	public void addZone(L2ZoneType zone)
	{
		_zones.add(zone);
	}
	
	public void removeZone(L2ZoneType zone)
	{
		_zones.remove(zone);
	}
	
	public void revalidateZones(L2Character character)
	{
		// do NOT update the world region while the character is still in the process of teleporting
		// Once the teleport is COMPLETED, revalidation occurs safely, at that time.
		
		if (character.isTeleporting())
			return;
		
		for (L2ZoneType z : getZones())
		{
			if (z != null)
				z.revalidateInZone(character);
		}
	}
	
	public void removeFromZones(L2Character character)
	{
		for (L2ZoneType z : getZones())
		{
			if (z != null)
				z.removeCharacter(character);
		}
	}
	
	public boolean containsZone(int zoneId)
	{
		for (L2ZoneType z : getZones())
		{
			if (z.getId() == zoneId)
				return true;
		}
		return false;
	}
	
	public boolean checkEffectRangeInsidePeaceZone(L2Skill skill, final int x, final int y, final int z)
	{
		final int range = skill.getEffectRange();
		final int up = y + range;
		final int down = y - range;
		final int left = x + range;
		final int right = x - range;
		
		for (L2ZoneType e : getZones())
		{
			if ((e instanceof L2TownZone && ((L2TownZone) e).isPeaceZone()) || e instanceof L2DerbyTrackZone || e instanceof L2PeaceZone)
			{
				if (e.isInsideZone(x, up, z))
					return false;
				
				if (e.isInsideZone(x, down, z))
					return false;
				
				if (e.isInsideZone(left, y, z))
					return false;
				
				if (e.isInsideZone(right, y, z))
					return false;
				
				if (e.isInsideZone(x, y, z))
					return false;
			}
		}
		return true;
	}
	
	public void onDeath(L2Character character)
	{
		for (L2ZoneType z : getZones())
		{
			if (z != null)
				z.onDieInside(character);
		}
	}
	
	public void onRevive(L2Character character)
	{
		for (L2ZoneType z : getZones())
		{
			if (z != null)
				z.onReviveInside(character);
		}
	}
	
	/** Task of AI notification */
	public class NeighborsTask implements Runnable
	{
		private final boolean _isActivating;
		
		public NeighborsTask(boolean isActivating)
		{
			_isActivating = isActivating;
		}
		
		@Override
		public void run()
		{
			if (_isActivating)
			{
				// for each neighbor, if it's not active, activate.
				for (L2WorldRegion neighbor : getSurroundingRegions())
					neighbor.setActive(true);
			}
			else
			{
				if (areNeighborsEmpty())
					setActive(false);
				
				// check and deactivate
				for (L2WorldRegion neighbor : getSurroundingRegions())
					if (neighbor.areNeighborsEmpty())
						neighbor.setActive(false);
			}
		}
	}
	
	private void switchAI(boolean isOn)
	{
		if (!isOn)
		{
			for (L2Object o : _visibleObjects.values())
			{
				if (o instanceof L2Attackable)
				{
					L2Attackable mob = (L2Attackable) o;
					
					// Set target to null and cancel Attack or Cast
					mob.setTarget(null);
					
					// Stop movement
					mob.stopMove(null);
					
					// Stop all active skills effects in progress on the L2Character
					mob.stopAllEffects();
					
					mob.clearAggroList();
					mob.getAttackByList().clear();
					mob.getKnownList().removeAllKnownObjects();
					
					// stop the ai tasks
					if (mob.hasAI())
					{
						mob.getAI().setIntention(CtrlIntention.IDLE);
						mob.getAI().stopAITask();
					}
				}
				else if (o instanceof L2Vehicle)
					((L2Vehicle) o).getKnownList().removeAllKnownObjects();
			}
		}
		else
		{
			for (L2Object o : _visibleObjects.values())
			{
				if (o instanceof L2Attackable)
					((L2Attackable) o).getStatus().startHpMpRegeneration();
				else if (o instanceof L2Npc)
					((L2Npc) o).startRandomAnimationTimer();
			}
		}
	}
	
	public boolean isActive()
	{
		return _active;
	}
	
	/**
	 * Check if all 9 neighbors (including self) are inactive or active but with no players.
	 * @return true if the above condition is met.
	 */
	public boolean areNeighborsEmpty()
	{
		// if this region is occupied, return false.
		if (isActive() && !_allPlayable.isEmpty())
			return false;
		
		// if any one of the neighbors is occupied, return false
		for (L2WorldRegion neighbor : _surroundingRegions)
		{
			if (neighbor.isActive() && !neighbor._allPlayable.isEmpty())
				return false;
		}
		
		// in all other cases, return true.
		return true;
	}
	
	/**
	 * This function turns this region's AI and geodata on or off
	 * @param value
	 */
	public void setActive(boolean value)
	{
		if (_active == value)
			return;
		
		_active = value;
		
		// turn the AI on or off to match the region's activation.
		switchAI(value);
	}
	
	/**
	 * Immediately sets self as active and starts a timer to set neighbors as active this timer is to avoid turning on neighbors in the case when a person just teleported into a region and then teleported out immediately...there is no reason to activate all the neighbors in that case.
	 */
	private void startActivation()
	{
		// first set self to active and do self-tasks...
		setActive(true);
		
		// if the timer to deactivate neighbors is running, cancel it.
		synchronized (this)
		{
			if (_neighborsTask != null)
			{
				_neighborsTask.cancel(true);
				_neighborsTask = null;
			}
			
			// then, set a timer to activate the neighbors
			_neighborsTask = ThreadPoolManager.getInstance().scheduleGeneral(new NeighborsTask(true), 1000 * Config.GRID_NEIGHBOR_TURNON_TIME);
		}
	}
	
	/**
	 * starts a timer to set neighbors (including self) as inactive this timer is to avoid turning off neighbors in the case when a person just moved out of a region that he may very soon return to. There is no reason to turn self & neighbors off in that case.
	 */
	private void startDeactivation()
	{
		// if the timer to activate neighbors is running, cancel it.
		synchronized (this)
		{
			if (_neighborsTask != null)
			{
				_neighborsTask.cancel(true);
				_neighborsTask = null;
			}
			
			// start a timer to "suggest" a deactivate to self and neighbors.
			// suggest means: first check if a neighbor has L2PcInstances in it. If not, deactivate.
			_neighborsTask = ThreadPoolManager.getInstance().scheduleGeneral(new NeighborsTask(false), 1000 * Config.GRID_NEIGHBOR_TURNOFF_TIME);
		}
	}
	
	/**
	 * Add the L2Object in the L2ObjectHashSet(L2Object) _visibleObjects containing L2Object visible in this L2WorldRegion <BR>
	 * If L2Object is a L2PcInstance, Add the L2PcInstance in the L2ObjectHashSet(L2PcInstance) _allPlayable containing L2PcInstance of all player in game in this L2WorldRegion <BR>
	 * Assert : object.getCurrentWorldRegion() == this
	 * @param object
	 */
	public void addVisibleObject(L2Object object)
	{
		if (object == null)
			return;
		
		assert object.getWorldRegion() == this;
		
		_visibleObjects.put(object.getObjectId(), object);
		
		if (object instanceof L2Playable)
		{
			_allPlayable.put(object.getObjectId(), (L2Playable) object);
			
			// if this is the first player to enter the region, activate self & neighbors
			if ((_allPlayable.size() == 1) && (!Config.GRIDS_ALWAYS_ON))
				startActivation();
		}
	}
	
	/**
	 * Remove the L2Object from the L2ObjectHashSet(L2Object) _visibleObjects in this L2WorldRegion <BR>
	 * <BR>
	 * If L2Object is a L2PcInstance, remove it from the L2ObjectHashSet(L2PcInstance) _allPlayable of this L2WorldRegion <BR>
	 * Assert : object.getCurrentWorldRegion() == this || object.getCurrentWorldRegion() == null
	 * @param object
	 */
	public void removeVisibleObject(L2Object object)
	{
		if (object == null)
			return;
		
		assert object.getWorldRegion() == this || object.getWorldRegion() == null;
		
		_visibleObjects.remove(object.getObjectId());
		
		if (object instanceof L2Playable)
		{
			_allPlayable.remove(object.getObjectId());
			
			if (_allPlayable.isEmpty() && !Config.GRIDS_ALWAYS_ON)
				startDeactivation();
		}
	}
	
	public void addSurroundingRegion(L2WorldRegion region)
	{
		_surroundingRegions.add(region);
	}
	
	/**
	 * @return the FastList _surroundingRegions containing all L2WorldRegion around the current L2WorldRegion
	 */
	public List<L2WorldRegion> getSurroundingRegions()
	{
		return _surroundingRegions;
	}
	
	public Map<Integer, L2Playable> getVisiblePlayable()
	{
		return _allPlayable;
	}
	
	public Map<Integer, L2Object> getVisibleObjects()
	{
		return _visibleObjects;
	}
	
	public String getName()
	{
		return "(" + _tileX + ", " + _tileY + ")";
	}
	
	/**
	 * Deleted all spawns in the world.
	 */
	public void deleteVisibleNpcSpawns()
	{
		for (L2Object obj : _visibleObjects.values())
		{
			if (obj instanceof L2Npc)
			{
				((L2Npc) obj).deleteMe();
				
				final L2Spawn spawn = ((L2Npc) obj).getSpawn();
				if (spawn != null)
				{
					spawn.stopRespawn();
					SpawnTable.getInstance().deleteSpawn(spawn, false);
				}
			}
		}
	}
}