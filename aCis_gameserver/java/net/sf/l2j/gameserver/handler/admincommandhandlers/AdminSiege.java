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

import java.util.StringTokenizer;

import net.sf.l2j.gameserver.datatables.ClanTable;
import net.sf.l2j.gameserver.handler.IAdminCommandHandler;
import net.sf.l2j.gameserver.instancemanager.AuctionManager;
import net.sf.l2j.gameserver.instancemanager.CastleManager;
import net.sf.l2j.gameserver.instancemanager.ClanHallManager;
import net.sf.l2j.gameserver.model.L2Clan;
import net.sf.l2j.gameserver.model.L2Object;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.model.entity.Castle;
import net.sf.l2j.gameserver.model.entity.ClanHall;
import net.sf.l2j.gameserver.model.zone.type.L2ClanHallZone;
import net.sf.l2j.gameserver.network.SystemMessageId;
import net.sf.l2j.gameserver.network.serverpackets.NpcHtmlMessage;
import net.sf.l2j.gameserver.network.serverpackets.SystemMessage;

/**
 * This class handles all siege commands
 */
public class AdminSiege implements IAdminCommandHandler
{
	private static final String[] ADMIN_COMMANDS =
	{
		"admin_siege",
		"admin_add_attacker",
		"admin_add_defender",
		"admin_add_guard",
		"admin_list_siege_clans",
		"admin_clear_siege_list",
		"admin_move_defenders",
		"admin_spawn_doors",
		"admin_endsiege",
		"admin_startsiege",
		"admin_setcastle",
		"admin_removecastle",
		"admin_clanhall",
		"admin_clanhallset",
		"admin_clanhalldel",
		"admin_clanhallopendoors",
		"admin_clanhallclosedoors",
		"admin_clanhallteleportself"
	};
	
	@Override
	public boolean useAdminCommand(String command, L2PcInstance activeChar)
	{
		StringTokenizer st = new StringTokenizer(command, " ");
		command = st.nextToken(); // Get actual command
		
		// Get castle
		Castle castle = null;
		ClanHall clanhall = null;
		
		if (command.startsWith("admin_clanhall"))
			clanhall = ClanHallManager.getInstance().getClanHallById(Integer.parseInt(st.nextToken()));
		else if (st.hasMoreTokens())
			castle = CastleManager.getInstance().getCastle(st.nextToken());
		
		if (clanhall == null && (castle == null || castle.getCastleId() < 0))
		{
			showCastleSelectPage(activeChar);
			return true;
		}
		
		L2Object target = activeChar.getTarget();
		L2PcInstance player = null;
		if (target instanceof L2PcInstance)
			player = (L2PcInstance) target;
		
		if (castle != null)
		{
			if (command.equalsIgnoreCase("admin_add_attacker"))
			{
				if (player == null)
					activeChar.sendPacket(SystemMessageId.TARGET_IS_INCORRECT);
				else
					castle.getSiege().registerAttacker(player);
			}
			else if (command.equalsIgnoreCase("admin_add_defender"))
			{
				if (player == null)
					activeChar.sendPacket(SystemMessageId.TARGET_IS_INCORRECT);
				else
					castle.getSiege().registerDefender(player);
			}
			else if (command.equalsIgnoreCase("admin_add_guard"))
			{
				try
				{
					int npcId = Integer.parseInt(st.nextToken());
					castle.getSiege().getSiegeGuardManager().addSiegeGuard(activeChar, npcId);
				}
				catch (Exception e)
				{
					activeChar.sendMessage("Usage: //add_guard npcId");
				}
			}
			else if (command.equalsIgnoreCase("admin_clear_siege_list"))
			{
				castle.getSiege().clearSiegeClan();
			}
			else if (command.equalsIgnoreCase("admin_endsiege"))
			{
				castle.getSiege().endSiege();
			}
			else if (command.equalsIgnoreCase("admin_list_siege_clans"))
			{
				castle.getSiege().listRegisterClan(activeChar);
				return true;
			}
			else if (command.equalsIgnoreCase("admin_move_defenders"))
			{
				activeChar.sendPacket(SystemMessage.sendString("Not implemented yet."));
			}
			else if (command.equalsIgnoreCase("admin_setcastle"))
			{
				if (player == null || player.getClan() == null)
					activeChar.sendPacket(SystemMessageId.TARGET_IS_INCORRECT);
				else if (player.getClan().hasCastle())
					activeChar.sendMessage(player.getName() + "'s clan already owns a castle.");
				else
					castle.setOwner(player.getClan());
			}
			else if (command.equalsIgnoreCase("admin_removecastle"))
			{
				L2Clan clan = ClanTable.getInstance().getClan(castle.getOwnerId());
				if (clan != null)
					castle.removeOwner(clan);
				else
					activeChar.sendMessage("Unable to remove castle for this clan.");
			}
			else if (command.equalsIgnoreCase("admin_spawn_doors"))
			{
				castle.spawnDoors(false);
			}
			else if (command.equalsIgnoreCase("admin_startsiege"))
			{
				castle.getSiege().startSiege();
			}
			showSiegePage(activeChar, castle.getName());
		}
		else if (clanhall != null)
		{
			if (command.equalsIgnoreCase("admin_clanhallset"))
			{
				if (player == null || player.getClan() == null)
					activeChar.sendPacket(SystemMessageId.TARGET_IS_INCORRECT);
				else if (!ClanHallManager.getInstance().isFree(clanhall.getId()))
					activeChar.sendMessage("This ClanHall isn't free!");
				else if (!player.getClan().hasHideout())
				{
					ClanHallManager.getInstance().setOwner(clanhall.getId(), player.getClan());
					if (AuctionManager.getInstance().getAuction(clanhall.getId()) != null)
						AuctionManager.getInstance().getAuction(clanhall.getId()).deleteAuctionFromDB();
				}
				else
					activeChar.sendMessage("You have already a ClanHall!");
			}
			else if (command.equalsIgnoreCase("admin_clanhalldel"))
			{
				if (!ClanHallManager.getInstance().isFree(clanhall.getId()))
				{
					ClanHallManager.getInstance().setFree(clanhall.getId());
					AuctionManager.getInstance().initNPC(clanhall.getId());
				}
				else
					activeChar.sendMessage("This ClanHall is already Free!");
			}
			else if (command.equalsIgnoreCase("admin_clanhallopendoors"))
			{
				clanhall.openCloseDoors(true);
			}
			else if (command.equalsIgnoreCase("admin_clanhallclosedoors"))
			{
				clanhall.openCloseDoors(false);
			}
			else if (command.equalsIgnoreCase("admin_clanhallteleportself"))
			{
				L2ClanHallZone zone = clanhall.getZone();
				if (zone != null)
					activeChar.teleToLocation(zone.getSpawnLoc(), 0);
			}
			showClanHallPage(activeChar, clanhall);
		}
		return true;
	}
	
	private static void showCastleSelectPage(L2PcInstance activeChar)
	{
		int i = 0;
		NpcHtmlMessage adminReply = new NpcHtmlMessage(0);
		adminReply.setFile("data/html/admin/castles.htm");
		StringBuilder cList = new StringBuilder();
		for (Castle castle : CastleManager.getInstance().getCastles())
		{
			if (castle != null)
			{
				String name = castle.getName();
				cList.append("<td fixwidth=90><a action=\"bypass -h admin_siege " + name + "\">" + name + "</a></td>");
				i++;
			}
			if (i > 2)
			{
				cList.append("</tr><tr>");
				i = 0;
			}
		}
		adminReply.replace("%castles%", cList.toString());
		cList.setLength(0);
		
		i = 0;
		for (ClanHall clanhall : ClanHallManager.getInstance().getClanHalls().values())
		{
			if (clanhall != null)
			{
				cList.append("<td fixwidth=134><a action=\"bypass -h admin_clanhall " + clanhall.getId() + "\">");
				cList.append(clanhall.getName() + "</a></td>");
				i++;
			}
			if (i > 1)
			{
				cList.append("</tr><tr>");
				i = 0;
			}
		}
		adminReply.replace("%clanhalls%", cList.toString());
		cList.setLength(0);
		
		i = 0;
		for (ClanHall clanhall : ClanHallManager.getInstance().getFreeClanHalls().values())
		{
			if (clanhall != null)
			{
				cList.append("<td fixwidth=134><a action=\"bypass -h admin_clanhall " + clanhall.getId() + "\">");
				cList.append(clanhall.getName() + "</a></td>");
				i++;
			}
			if (i > 1)
			{
				cList.append("</tr><tr>");
				i = 0;
			}
		}
		adminReply.replace("%freeclanhalls%", cList.toString());
		activeChar.sendPacket(adminReply);
	}
	
	private static void showSiegePage(L2PcInstance activeChar, String castleName)
	{
		NpcHtmlMessage adminReply = new NpcHtmlMessage(0);
		adminReply.setFile("data/html/admin/castle.htm");
		adminReply.replace("%castleName%", castleName);
		activeChar.sendPacket(adminReply);
	}
	
	private static void showClanHallPage(L2PcInstance activeChar, ClanHall clanhall)
	{
		NpcHtmlMessage adminReply = new NpcHtmlMessage(0);
		adminReply.setFile("data/html/admin/clanhall.htm");
		adminReply.replace("%clanhallName%", clanhall.getName());
		adminReply.replace("%clanhallId%", clanhall.getId());
		L2Clan owner = ClanTable.getInstance().getClan(clanhall.getOwnerId());
		if (owner == null)
			adminReply.replace("%clanhallOwner%", "None");
		else
			adminReply.replace("%clanhallOwner%", owner.getName());
		activeChar.sendPacket(adminReply);
	}
	
	@Override
	public String[] getAdminCommandList()
	{
		return ADMIN_COMMANDS;
	}
}