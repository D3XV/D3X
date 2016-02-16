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
package net.sf.l2j.gameserver.handler.admincommandhandlers;

import java.io.File;
import java.util.StringTokenizer;

import javax.script.ScriptException;

import net.sf.l2j.Config;
import net.sf.l2j.gameserver.cache.CrestCache;
import net.sf.l2j.gameserver.cache.HtmCache;
import net.sf.l2j.gameserver.datatables.AdminCommandAccessRights;
import net.sf.l2j.gameserver.datatables.AnnouncementTable;
import net.sf.l2j.gameserver.datatables.DoorTable;
import net.sf.l2j.gameserver.datatables.GmListTable;
import net.sf.l2j.gameserver.datatables.ItemTable;
import net.sf.l2j.gameserver.datatables.MultisellData;
import net.sf.l2j.gameserver.datatables.NpcTable;
import net.sf.l2j.gameserver.datatables.NpcWalkerRoutesTable;
import net.sf.l2j.gameserver.datatables.SkillTable;
import net.sf.l2j.gameserver.datatables.TeleportLocationTable;
import net.sf.l2j.gameserver.handler.IAdminCommandHandler;
import net.sf.l2j.gameserver.instancemanager.CursedWeaponsManager;
import net.sf.l2j.gameserver.instancemanager.QuestManager;
import net.sf.l2j.gameserver.instancemanager.ZoneManager;
import net.sf.l2j.gameserver.model.L2Object;
import net.sf.l2j.gameserver.model.L2World;
import net.sf.l2j.gameserver.model.actor.L2Character;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.network.SystemMessageId;
import net.sf.l2j.gameserver.scripting.L2ScriptEngineManager;
import net.sf.l2j.gameserver.util.Util;

/**
 * This class handles following admin commands:
 * <ul>
 * <li>admin/admin1/admin2/admin3/admin4 : the different admin menus.</li>
 * <li>gmlist : includes/excludes active character from /gmlist results.</li>
 * <li>kill : handles the kill command.</li>
 * <li>silence : toggles private messages acceptance mode.</li>
 * <li>tradeoff : toggles trade acceptance mode.</li>
 * <li>reload : reloads specified component.</li>
 * <li>script_load : loads following script. MUSTN'T be used instead of //reload quest !</li>
 * </ul>
 */
public class AdminAdmin implements IAdminCommandHandler
{
	private static final String[] ADMIN_COMMANDS =
	{
		"admin_admin",
		"admin_admin1",
		"admin_admin2",
		"admin_admin3",
		"admin_admin4",
		"admin_gmlist",
		"admin_kill",
		"admin_silence",
		"admin_tradeoff",
		"admin_reload",
		"admin_script_load"
	};
	
	@Override
	public boolean useAdminCommand(String command, L2PcInstance activeChar)
	{
		if (command.startsWith("admin_admin"))
			showMainPage(activeChar, command);
		else if (command.startsWith("admin_gmlist"))
		{
			final boolean visibleStatus = GmListTable.getInstance().isGmVisible(activeChar);
			
			GmListTable.getInstance().showOrHideGm(activeChar, !visibleStatus);
			activeChar.sendMessage((visibleStatus) ? "Registered into GMList." : "Removed from GMList.");
		}
		else if (command.startsWith("admin_kill"))
		{
			StringTokenizer st = new StringTokenizer(command, " ");
			st.nextToken(); // skip command
			
			if (!st.hasMoreTokens())
			{
				final L2Object obj = activeChar.getTarget();
				if (!(obj instanceof L2Character))
					activeChar.sendPacket(SystemMessageId.INCORRECT_TARGET);
				else
					kill(activeChar, (L2Character) obj);
				
				return true;
			}
			
			String firstParam = st.nextToken();
			L2PcInstance player = L2World.getInstance().getPlayer(firstParam);
			if (player != null)
			{
				if (st.hasMoreTokens())
				{
					String secondParam = st.nextToken();
					if (Util.isDigit(secondParam))
					{
						int radius = Integer.parseInt(secondParam);
						for (L2Character knownChar : player.getKnownList().getKnownTypeInRadius(L2Character.class, radius))
						{
							if (knownChar.equals(activeChar))
								continue;
							
							kill(activeChar, knownChar);
						}
						activeChar.sendMessage("Killed all characters within a " + radius + " unit radius around " + player.getName() + ".");
					}
					else
						activeChar.sendMessage("Invalid radius.");
				}
				else
					kill(activeChar, player);
			}
			else if (Util.isDigit(firstParam))
			{
				int radius = Integer.parseInt(firstParam);
				for (L2Character knownChar : activeChar.getKnownList().getKnownTypeInRadius(L2Character.class, radius))
				{
					if (knownChar.equals(activeChar))
						continue;
					
					kill(activeChar, knownChar);
				}
				activeChar.sendMessage("Killed all characters within a " + radius + " unit radius.");
			}
		}
		else if (command.startsWith("admin_silence"))
		{
			if (activeChar.isInRefusalMode()) // already in message refusal mode
			{
				activeChar.setInRefusalMode(false);
				activeChar.sendPacket(SystemMessageId.MESSAGE_ACCEPTANCE_MODE);
			}
			else
			{
				activeChar.setInRefusalMode(true);
				activeChar.sendPacket(SystemMessageId.MESSAGE_REFUSAL_MODE);
			}
		}
		else if (command.startsWith("admin_tradeoff"))
		{
			try
			{
				String mode = command.substring(15);
				if (mode.equalsIgnoreCase("on"))
				{
					activeChar.setTradeRefusal(true);
					activeChar.sendMessage("Trade refusal enabled");
				}
				else if (mode.equalsIgnoreCase("off"))
				{
					activeChar.setTradeRefusal(false);
					activeChar.sendMessage("Trade refusal disabled");
				}
			}
			catch (Exception ex)
			{
				if (activeChar.getTradeRefusal())
				{
					activeChar.setTradeRefusal(false);
					activeChar.sendMessage("Trade refusal disabled");
				}
				else
				{
					activeChar.setTradeRefusal(true);
					activeChar.sendMessage("Trade refusal enabled");
				}
			}
		}
		else if (command.startsWith("admin_reload"))
		{
			StringTokenizer st = new StringTokenizer(command);
			st.nextToken();
			try
			{
				String type = st.nextToken();
				if (type.startsWith("acar"))
				{
					AdminCommandAccessRights.getInstance().reload();
					activeChar.sendMessage("Admin commands rights have been reloaded.");
				}
				else if (type.startsWith("announcement"))
				{
					AnnouncementTable.getInstance().reload();
					activeChar.sendMessage("The content of announcements.xml has been reloaded.");
				}
				else if (type.startsWith("config"))
				{
					Config.load();
					activeChar.sendMessage("Configs files have been reloaded.");
				}
				else if (type.startsWith("crest"))
				{
					CrestCache.getInstance().reload();
					activeChar.sendMessage("Crests have been reloaded.");
				}
				else if (type.startsWith("cw"))
				{
					CursedWeaponsManager.getInstance().reload();
					activeChar.sendMessage("Cursed weapons have been reloaded.");
				}
				else if (type.startsWith("door"))
				{
					DoorTable.getInstance().reload();
					activeChar.sendMessage("Doors instance has been reloaded.");
				}
				else if (type.startsWith("htm"))
				{
					HtmCache.getInstance().reload();
					activeChar.sendMessage("The HTM cache has been reloaded.");
				}
				else if (type.startsWith("item"))
				{
					ItemTable.getInstance().reload();
					activeChar.sendMessage("Items' templates have been reloaded.");
				}
				else if (type.equals("multisell"))
				{
					MultisellData.getInstance().reload();
					activeChar.sendMessage("The multisell instance has been reloaded.");
				}
				else if (type.equals("npc"))
				{
					NpcTable.getInstance().reloadAllNpc();
					activeChar.sendMessage("NPCs templates have been reloaded.");
				}
				else if (type.startsWith("npcwalker"))
				{
					NpcWalkerRoutesTable.getInstance().reload();
					activeChar.sendMessage("NPCwalkers' routes have been reloaded.");
				}
				else if (type.startsWith("quest"))
				{
					if (st.hasMoreTokens())
					{
						int qId = Integer.parseInt(st.nextToken());
						if (QuestManager.getInstance().reload(qId))
							activeChar.sendMessage("Quest " + qId + " has been reloaded.");
						else
							activeChar.sendMessage("Quest " + qId + " failed reloading.");
					}
					else
						activeChar.sendMessage("Usage : //reload quest questNumber.");
				}
				else if (type.startsWith("scripts"))
				{
					QuestManager.getInstance().reloadAllQuests();
					activeChar.sendMessage("All scripts have been reloaded.");
				}
				else if (type.startsWith("skill"))
				{
					SkillTable.getInstance().reload();
					activeChar.sendMessage("Skills' XMLs have been reloaded.");
				}
				else if (type.startsWith("teleport"))
				{
					TeleportLocationTable.getInstance().reload();
					activeChar.sendMessage("The teleport location table has been reloaded.");
				}
				else if (type.startsWith("zone"))
				{
					ZoneManager.getInstance().reload();
					activeChar.sendMessage("Zones have been reloaded.");
				}
			}
			catch (Exception e)
			{
				activeChar.sendMessage("Usage : //reload <acar|announcement|config|crest|door>");
				activeChar.sendMessage("Usage : //reload <htm|item|multisell|npc|npcwalker|quest>");
				activeChar.sendMessage("Usage : //reload <scripts|skill|teleport|zone>");
			}
		}
		// This provides a way to load new scripts without having to reboot the server.
		// If a script is already loaded, quest_reload should be used.
		else if (command.startsWith("admin_script_load"))
		{
			String[] parts = command.split(" ");
			if (parts.length < 2)
				activeChar.sendMessage("Example: //script_load quests/questFolder/filename.ext");
			else
			{
				File file = new File(L2ScriptEngineManager.SCRIPT_FOLDER, parts[1]);
				if (file.isFile())
				{
					try
					{
						L2ScriptEngineManager.getInstance().executeScript(file);
					}
					catch (ScriptException e)
					{
						activeChar.sendMessage("Failed loading: " + parts[1]);
						L2ScriptEngineManager.reportScriptFileError(file, e);
					}
					catch (Exception e)
					{
						activeChar.sendMessage("Failed loading: " + parts[1]);
					}
				}
				else
					activeChar.sendMessage("Current file hasn't been found: " + parts[1]);
			}
			
		}
		return true;
	}
	
	@Override
	public String[] getAdminCommandList()
	{
		return ADMIN_COMMANDS;
	}
	
	private static void kill(L2PcInstance activeChar, L2Character target)
	{
		if (target instanceof L2PcInstance)
		{
			if (!((L2PcInstance) target).isGM())
				target.stopAllEffects(); // e.g. invincibility effect
			target.reduceCurrentHp(target.getMaxHp() + target.getMaxCp() + 1, activeChar, null);
		}
		else if (target.isChampion())
			target.reduceCurrentHp(target.getMaxHp() * Config.CHAMPION_HP + 1, activeChar, null);
		else
			target.reduceCurrentHp(target.getMaxHp() + 1, activeChar, null);
	}
	
	private static void showMainPage(L2PcInstance activeChar, String command)
	{
		int mode = 0;
		String filename = null;
		try
		{
			mode = Integer.parseInt(command.substring(11));
		}
		catch (Exception e)
		{
		}
		
		switch (mode)
		{
			case 1:
				filename = "main";
				break;
			case 2:
				filename = "game";
				break;
			case 3:
				filename = "effects";
				break;
			case 4:
				filename = "server";
				break;
			default:
				filename = "main";
				break;
		}
		AdminHelpPage.showHelpPage(activeChar, filename + "_menu.htm");
	}
}