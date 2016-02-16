/*
 * This program is free software: you can redistribute it and/or modify it under the terms of the
 * GNU General Public License as published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License along with this program. If
 * not, see <http://www.gnu.org/licenses/>.
 */
package net.sf.l2j.gameserver.util;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;

import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.network.L2GameClient;
import net.sf.l2j.util.StringUtil;

/**
 * Flood protector implementation.
 * @author fordfrog
 */
public final class FloodProtectorAction
{
	private static final Logger _log = Logger.getLogger(FloodProtectorAction.class.getName());
	
	private final L2GameClient _client;
	private final FloodProtectorConfig _config;
	
	private volatile long _lastTime = System.currentTimeMillis();
	private final AtomicInteger _count = new AtomicInteger(0);
	
	private boolean _logged;
	
	private volatile boolean _punishmentInProgress;
	
	/**
	 * Creates new instance of FloodProtectorAction.
	 * @param client client for which flood protection is being created
	 * @param config flood protector configuration
	 */
	public FloodProtectorAction(final L2GameClient client, final FloodProtectorConfig config)
	{
		super();
		_client = client;
		_config = config;
	}
	
	/**
	 * Checks whether the request is flood protected or not.
	 * @param command command issued or short command description
	 * @return true if action is allowed, otherwise false
	 */
	public boolean tryPerformAction(final String command)
	{
		final long time = System.currentTimeMillis();
		
		if (time < _lastTime || _punishmentInProgress)
		{
			if (_config.LOG_FLOODING && !_logged && _log.isLoggable(Level.WARNING))
			{
				log(" called command ", command, " ~", String.valueOf((_config.FLOOD_PROTECTION_INTERVAL - (_lastTime - time))), " ms after previous command");
				_logged = true;
			}
			
			_count.incrementAndGet();
			
			if (!_punishmentInProgress && _config.PUNISHMENT_LIMIT > 0 && _count.get() >= _config.PUNISHMENT_LIMIT && _config.PUNISHMENT_TYPE != null)
			{
				_punishmentInProgress = true;
				
				if ("kick".equals(_config.PUNISHMENT_TYPE))
					kickPlayer();
				else if ("ban".equals(_config.PUNISHMENT_TYPE))
					banAccount();
				else if ("jail".equals(_config.PUNISHMENT_TYPE))
					jailChar();
				
				_punishmentInProgress = false;
			}
			
			return false;
		}
		
		if (_count.get() > 0)
		{
			if (_config.LOG_FLOODING && _log.isLoggable(Level.WARNING))
				log(" issued ", String.valueOf(_count), " extra requests within ~", String.valueOf(_config.FLOOD_PROTECTION_INTERVAL), " ms");
		}
		
		_lastTime = time + _config.FLOOD_PROTECTION_INTERVAL;
		_logged = false;
		_count.set(0);
		
		return true;
	}
	
	/**
	 * Kick player from game (close network connection).
	 */
	private void kickPlayer()
	{
		if (_client.getActiveChar() != null)
			_client.getActiveChar().logout(false);
		else
			_client.closeNow();
		
		if (_log.isLoggable(Level.WARNING))
			log("kicked for flooding");
	}
	
	/**
	 * Bans char account and logs out the char.
	 */
	private void banAccount()
	{
		if (_client.getActiveChar() != null)
		{
			_client.getActiveChar().setPunishLevel(L2PcInstance.PunishLevel.ACC, _config.PUNISHMENT_TIME);
			
			if (_log.isLoggable(Level.WARNING))
				log(" banned for flooding ", _config.PUNISHMENT_TIME <= 0 ? "forever" : "for " + _config.PUNISHMENT_TIME + " mins");
			
			_client.getActiveChar().logout();
		}
		else
			log(" unable to ban account: no active player");
	}
	
	/**
	 * Jails char.
	 */
	private void jailChar()
	{
		if (_client.getActiveChar() != null)
		{
			_client.getActiveChar().setPunishLevel(L2PcInstance.PunishLevel.JAIL, _config.PUNISHMENT_TIME);
			
			if (_log.isLoggable(Level.WARNING))
				log(" jailed for flooding ", _config.PUNISHMENT_TIME <= 0 ? "forever" : "for " + _config.PUNISHMENT_TIME + " mins");
		}
		else
			log(" unable to jail: no active player");
	}
	
	private void log(String... lines)
	{
		final StringBuilder output = StringUtil.startAppend(100, _config.FLOOD_PROTECTOR_TYPE, ": ");
		String address = null;
		try
		{
			if (!_client.isDetached())
				address = _client.getConnection().getInetAddress().getHostAddress();
		}
		catch (Exception e)
		{
		}
		
		switch (_client.getState())
		{
			case IN_GAME:
				if (_client.getActiveChar() != null)
				{
					StringUtil.append(output, _client.getActiveChar().getName());
					StringUtil.append(output, "(", String.valueOf(_client.getActiveChar().getObjectId()), ") ");
				}
			case AUTHED:
				if (_client.getAccountName() != null)
					StringUtil.append(output, _client.getAccountName(), " ");
			case CONNECTED:
				if (address != null)
					StringUtil.append(output, address);
				break;
			default:
				throw new IllegalStateException("Missing state on switch");
		}
		
		StringUtil.append(output, lines);
		_log.warning(output.toString());
	}
}