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
package net.sf.l2j.gameserver.taskmanager;

import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import net.sf.l2j.gameserver.ThreadPoolManager;
import net.sf.l2j.gameserver.model.actor.L2Character;
import net.sf.l2j.gameserver.model.actor.L2Playable;
import net.sf.l2j.gameserver.model.actor.L2Summon;
import net.sf.l2j.gameserver.model.actor.instance.L2CubicInstance;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.network.serverpackets.AutoAttackStop;

/**
 * Turns off attack stance of {@link L2Character} after PERIOD ms.
 * @author Luca Baldi, Hasha
 */
public final class AttackStanceTaskManager implements Runnable
{
	private static final long ATTACK_STANCE_PERIOD = 15000; // 15 seconds
	
	private final Map<L2Character, Long> _characters = new ConcurrentHashMap<>();
	
	public final static AttackStanceTaskManager getInstance()
	{
		return SingletonHolder._instance;
	}
	
	protected AttackStanceTaskManager()
	{
		// Run task each second.
		ThreadPoolManager.getInstance().scheduleAiAtFixedRate(this, 1000, 1000);
	}
	
	/**
	 * Adds {@link L2Character} to the AttackStanceTask.
	 * @param character : {@link L2Character} to be added and checked.
	 */
	public final void add(L2Character character)
	{
		if (character instanceof L2Playable)
		{
			for (L2CubicInstance cubic : character.getActingPlayer().getCubics().values())
				if (cubic.getId() != L2CubicInstance.LIFE_CUBIC)
					cubic.doAction();
		}
		
		_characters.put(character, System.currentTimeMillis() + ATTACK_STANCE_PERIOD);
	}
	
	/**
	 * Removes {@link L2Character} from the AttackStanceTask.
	 * @param character : {@link L2Character} to be removed.
	 */
	public final void remove(L2Character character)
	{
		if (character instanceof L2Summon)
			character = character.getActingPlayer();
		
		_characters.remove(character);
	}
	
	/**
	 * Tests if {@link L2Character} is in AttackStanceTask.
	 * @param character : {@link L2Character} to be removed.
	 * @return boolean : True when {@link L2Character} is in attack stance.
	 */
	public final boolean isInAttackStance(L2Character character)
	{
		if (character instanceof L2Summon)
			character = character.getActingPlayer();
		
		return _characters.containsKey(character);
	}
	
	@Override
	public final void run()
	{
		// List is empty, skip.
		if (_characters.isEmpty())
			return;
		
		// Get current time.
		final long time = System.currentTimeMillis();
		
		// Loop all characters.
		for (Iterator<Map.Entry<L2Character, Long>> iterator = _characters.entrySet().iterator(); iterator.hasNext();)
		{
			// Get entry of current iteration.
			Map.Entry<L2Character, Long> entry = iterator.next();
			
			// Time hasn't passed yet, skip.
			if (time < entry.getValue())
				continue;
			
			// Get character.
			final L2Character character = entry.getKey();
			
			// Stop character attack stance animation.
			character.broadcastPacket(new AutoAttackStop(character.getObjectId()));
			
			// Stop pet attack stance animation.
			if (character instanceof L2PcInstance && ((L2PcInstance) character).getPet() != null)
				((L2PcInstance) character).getPet().broadcastPacket(new AutoAttackStop(((L2PcInstance) character).getPet().getObjectId()));
			
			// Inform character AI and remove task.
			character.getAI().setAutoAttacking(false);
			iterator.remove();
		}
	}
	
	private static class SingletonHolder
	{
		protected static final AttackStanceTaskManager _instance = new AttackStanceTaskManager();
	}
}