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
package net.sf.l2j.gameserver.handler;

import java.util.HashMap;
import java.util.Map;

import net.sf.l2j.gameserver.handler.admincommandhandlers.AdminAdmin;
import net.sf.l2j.gameserver.handler.admincommandhandlers.AdminAnnouncements;
import net.sf.l2j.gameserver.handler.admincommandhandlers.AdminBan;
import net.sf.l2j.gameserver.handler.admincommandhandlers.AdminBookmark;
import net.sf.l2j.gameserver.handler.admincommandhandlers.AdminBuffs;
import net.sf.l2j.gameserver.handler.admincommandhandlers.AdminCache;
import net.sf.l2j.gameserver.handler.admincommandhandlers.AdminCamera;
import net.sf.l2j.gameserver.handler.admincommandhandlers.AdminChangeAccessLevel;
import net.sf.l2j.gameserver.handler.admincommandhandlers.AdminCreateItem;
import net.sf.l2j.gameserver.handler.admincommandhandlers.AdminCursedWeapons;
import net.sf.l2j.gameserver.handler.admincommandhandlers.AdminDelete;
import net.sf.l2j.gameserver.handler.admincommandhandlers.AdminDoorControl;
import net.sf.l2j.gameserver.handler.admincommandhandlers.AdminEditChar;
import net.sf.l2j.gameserver.handler.admincommandhandlers.AdminEditNpc;
import net.sf.l2j.gameserver.handler.admincommandhandlers.AdminEffects;
import net.sf.l2j.gameserver.handler.admincommandhandlers.AdminEnchant;
import net.sf.l2j.gameserver.handler.admincommandhandlers.AdminExpSp;
import net.sf.l2j.gameserver.handler.admincommandhandlers.AdminGeodata;
import net.sf.l2j.gameserver.handler.admincommandhandlers.AdminGm;
import net.sf.l2j.gameserver.handler.admincommandhandlers.AdminGmChat;
import net.sf.l2j.gameserver.handler.admincommandhandlers.AdminHeal;
import net.sf.l2j.gameserver.handler.admincommandhandlers.AdminHelpPage;
import net.sf.l2j.gameserver.handler.admincommandhandlers.AdminInvul;
import net.sf.l2j.gameserver.handler.admincommandhandlers.AdminKick;
import net.sf.l2j.gameserver.handler.admincommandhandlers.AdminKnownlist;
import net.sf.l2j.gameserver.handler.admincommandhandlers.AdminLevel;
import net.sf.l2j.gameserver.handler.admincommandhandlers.AdminMaintenance;
import net.sf.l2j.gameserver.handler.admincommandhandlers.AdminMammon;
import net.sf.l2j.gameserver.handler.admincommandhandlers.AdminManor;
import net.sf.l2j.gameserver.handler.admincommandhandlers.AdminMenu;
import net.sf.l2j.gameserver.handler.admincommandhandlers.AdminMonsterRace;
import net.sf.l2j.gameserver.handler.admincommandhandlers.AdminMovieMaker;
import net.sf.l2j.gameserver.handler.admincommandhandlers.AdminOlympiad;
import net.sf.l2j.gameserver.handler.admincommandhandlers.AdminPForge;
import net.sf.l2j.gameserver.handler.admincommandhandlers.AdminPathNode;
import net.sf.l2j.gameserver.handler.admincommandhandlers.AdminPetition;
import net.sf.l2j.gameserver.handler.admincommandhandlers.AdminPledge;
import net.sf.l2j.gameserver.handler.admincommandhandlers.AdminPolymorph;
import net.sf.l2j.gameserver.handler.admincommandhandlers.AdminRepairChar;
import net.sf.l2j.gameserver.handler.admincommandhandlers.AdminRes;
import net.sf.l2j.gameserver.handler.admincommandhandlers.AdminRideWyvern;
import net.sf.l2j.gameserver.handler.admincommandhandlers.AdminShop;
import net.sf.l2j.gameserver.handler.admincommandhandlers.AdminSiege;
import net.sf.l2j.gameserver.handler.admincommandhandlers.AdminSkill;
import net.sf.l2j.gameserver.handler.admincommandhandlers.AdminSpawn;
import net.sf.l2j.gameserver.handler.admincommandhandlers.AdminTarget;
import net.sf.l2j.gameserver.handler.admincommandhandlers.AdminTeleport;
import net.sf.l2j.gameserver.handler.admincommandhandlers.AdminZone;

public class AdminCommandHandler
{
	private final Map<Integer, IAdminCommandHandler> _datatable = new HashMap<>();
	
	public static AdminCommandHandler getInstance()
	{
		return SingletonHolder._instance;
	}
	
	protected AdminCommandHandler()
	{
		registerAdminCommandHandler(new AdminAdmin());
		registerAdminCommandHandler(new AdminAnnouncements());
		registerAdminCommandHandler(new AdminBan());
		registerAdminCommandHandler(new AdminBookmark());
		registerAdminCommandHandler(new AdminBuffs());
		registerAdminCommandHandler(new AdminCache());
		registerAdminCommandHandler(new AdminCamera());
		registerAdminCommandHandler(new AdminChangeAccessLevel());
		registerAdminCommandHandler(new AdminCreateItem());
		registerAdminCommandHandler(new AdminCursedWeapons());
		registerAdminCommandHandler(new AdminDelete());
		registerAdminCommandHandler(new AdminDoorControl());
		registerAdminCommandHandler(new AdminEditChar());
		registerAdminCommandHandler(new AdminEditNpc());
		registerAdminCommandHandler(new AdminEffects());
		registerAdminCommandHandler(new AdminEnchant());
		registerAdminCommandHandler(new AdminExpSp());
		registerAdminCommandHandler(new AdminGeodata());
		registerAdminCommandHandler(new AdminGm());
		registerAdminCommandHandler(new AdminGmChat());
		registerAdminCommandHandler(new AdminHeal());
		registerAdminCommandHandler(new AdminHelpPage());
		registerAdminCommandHandler(new AdminInvul());
		registerAdminCommandHandler(new AdminKick());
		registerAdminCommandHandler(new AdminKnownlist());
		registerAdminCommandHandler(new AdminLevel());
		registerAdminCommandHandler(new AdminMaintenance());
		registerAdminCommandHandler(new AdminMammon());
		registerAdminCommandHandler(new AdminManor());
		registerAdminCommandHandler(new AdminMenu());
		registerAdminCommandHandler(new AdminMonsterRace());
		registerAdminCommandHandler(new AdminMovieMaker());
		registerAdminCommandHandler(new AdminOlympiad());
		registerAdminCommandHandler(new AdminPathNode());
		registerAdminCommandHandler(new AdminPetition());
		registerAdminCommandHandler(new AdminPForge());
		registerAdminCommandHandler(new AdminPledge());
		registerAdminCommandHandler(new AdminPolymorph());
		registerAdminCommandHandler(new AdminRepairChar());
		registerAdminCommandHandler(new AdminRes());
		registerAdminCommandHandler(new AdminRideWyvern());
		registerAdminCommandHandler(new AdminShop());
		registerAdminCommandHandler(new AdminSiege());
		registerAdminCommandHandler(new AdminSkill());
		registerAdminCommandHandler(new AdminSpawn());
		registerAdminCommandHandler(new AdminTarget());
		registerAdminCommandHandler(new AdminTeleport());
		registerAdminCommandHandler(new AdminZone());
	}
	
	public void registerAdminCommandHandler(IAdminCommandHandler handler)
	{
		for (String id : handler.getAdminCommandList())
			_datatable.put(id.hashCode(), handler);
	}
	
	public IAdminCommandHandler getAdminCommandHandler(String adminCommand)
	{
		String command = adminCommand;
		
		if (adminCommand.indexOf(" ") != -1)
			command = adminCommand.substring(0, adminCommand.indexOf(" "));
		
		return _datatable.get(command.hashCode());
	}
	
	public int size()
	{
		return _datatable.size();
	}
	
	private static class SingletonHolder
	{
		protected static final AdminCommandHandler _instance = new AdminCommandHandler();
	}
}