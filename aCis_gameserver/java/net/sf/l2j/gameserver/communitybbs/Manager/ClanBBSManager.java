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
package net.sf.l2j.gameserver.communitybbs.Manager;

import java.util.StringTokenizer;

import net.sf.l2j.gameserver.cache.HtmCache;
import net.sf.l2j.gameserver.datatables.ClanTable;
import net.sf.l2j.gameserver.model.L2Clan;
import net.sf.l2j.gameserver.model.L2ClanMember;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.network.SystemMessageId;
import net.sf.l2j.util.StringUtil;

public class ClanBBSManager extends BaseBBSManager
{
	private static final String HOME_BAR = "<table width=610 bgcolor=A7A19A><tr><td width=5></td><td width=605><a action=\"bypass _bbsclan;home;%clanid%\">[GO TO MY CLAN]</a></td></tr></table>";
	
	protected ClanBBSManager()
	{
	}
	
	public static ClanBBSManager getInstance()
	{
		return SingletonHolder._instance;
	}
	
	@Override
	public void parseCmd(String command, L2PcInstance activeChar)
	{
		if (command.equalsIgnoreCase("_bbsclan"))
		{
			if (activeChar.getClan() == null)
				sendClanList(activeChar, 1);
			else
				sendClanDetails(activeChar, activeChar.getClan().getClanId());
		}
		else if (command.startsWith("_bbsclan"))
		{
			StringTokenizer st = new StringTokenizer(command, ";");
			st.nextToken();
			
			final String clanCommand = st.nextToken();
			if (clanCommand.equalsIgnoreCase("clan"))
				sendClanList(activeChar, Integer.parseInt(st.nextToken()));
			else if (clanCommand.equalsIgnoreCase("home"))
				sendClanDetails(activeChar, Integer.parseInt(st.nextToken()));
			else if (clanCommand.equalsIgnoreCase("mail"))
				sendClanMail(activeChar, Integer.parseInt(st.nextToken()));
			else if (clanCommand.equalsIgnoreCase("management"))
				sendClanManagement(activeChar, Integer.parseInt(st.nextToken()));
			else if (clanCommand.equalsIgnoreCase("notice"))
			{
				if (st.hasMoreTokens())
				{
					final String noticeCommand = st.nextToken();
					if (!noticeCommand.isEmpty() && activeChar.getClan() != null)
						activeChar.getClan().setNoticeEnabledAndStore(Boolean.parseBoolean(noticeCommand));
				}
				sendClanNotice(activeChar, activeChar.getClanId());
			}
		}
		else
			super.parseCmd(command, activeChar);
	}
	
	@Override
	public void parseWrite(String ar1, String ar2, String ar3, String ar4, String ar5, L2PcInstance activeChar)
	{
		if (ar1.equalsIgnoreCase("intro"))
		{
			if (Integer.valueOf(ar2) != activeChar.getClanId())
				return;
			
			final L2Clan clan = ClanTable.getInstance().getClan(activeChar.getClanId());
			if (clan == null)
				return;
			
			clan.setIntroduction(ar3, true);
			sendClanManagement(activeChar, Integer.valueOf(ar2));
		}
		else if (ar1.equals("notice"))
		{
			activeChar.getClan().setNoticeAndStore(ar4);
			sendClanNotice(activeChar, activeChar.getClanId());
		}
		else if (ar1.equalsIgnoreCase("mail"))
		{
			if (Integer.valueOf(ar2) != activeChar.getClanId())
				return;
			
			final L2Clan clan = ClanTable.getInstance().getClan(activeChar.getClanId());
			if (clan == null)
				return;
			
			// Retrieve clans members, and store them under a String.
			final StringBuffer membersList = new StringBuffer();
			
			for (L2ClanMember player : clan.getMembers())
			{
				if (player != null)
				{
					if (membersList.length() > 0)
						membersList.append(";");
					
					membersList.append(player.getName());
				}
			}
			MailBBSManager.getInstance().sendLetter(membersList.toString(), ar4, ar5, activeChar);
			sendClanDetails(activeChar, activeChar.getClanId());
		}
		else
			super.parseWrite(ar1, ar2, ar3, ar4, ar5, activeChar);
	}
	
	@Override
	protected String getFolder()
	{
		return "clan/";
	}
	
	private static void sendClanMail(L2PcInstance activeChar, int clanId)
	{
		final L2Clan clan = ClanTable.getInstance().getClan(clanId);
		if (clan == null)
			return;
		
		if (activeChar.getClanId() != clanId || !activeChar.isClanLeader())
		{
			activeChar.sendPacket(SystemMessageId.ONLY_THE_CLAN_LEADER_IS_ENABLED);
			sendClanList(activeChar, 1);
			return;
		}
		
		String content = HtmCache.getInstance().getHtm(CB_PATH + "clan/clanhome-mail.htm");
		content = content.replaceAll("%clanid%", Integer.toString(clanId));
		content = content.replaceAll("%clanName%", clan.getName());
		separateAndSend(content, activeChar);
	}
	
	private static void sendClanManagement(L2PcInstance activeChar, int clanId)
	{
		final L2Clan clan = ClanTable.getInstance().getClan(clanId);
		if (clan == null)
			return;
		
		if (activeChar.getClanId() != clanId || !activeChar.isClanLeader())
		{
			activeChar.sendPacket(SystemMessageId.ONLY_THE_CLAN_LEADER_IS_ENABLED);
			sendClanList(activeChar, 1);
			return;
		}
		
		String content = HtmCache.getInstance().getHtm(CB_PATH + "clan/clanhome-management.htm");
		content = content.replaceAll("%clanid%", Integer.toString(clan.getClanId()));
		send1001(content, activeChar);
		send1002(activeChar, clan.getIntroduction(), "", "");
	}
	
	private static void sendClanNotice(L2PcInstance activeChar, int clanId)
	{
		final L2Clan clan = ClanTable.getInstance().getClan(clanId);
		if (clan == null || activeChar.getClanId() != clanId)
			return;
		
		if (clan.getLevel() < 2)
		{
			activeChar.sendPacket(SystemMessageId.NO_CB_IN_MY_CLAN);
			sendClanList(activeChar, 1);
			return;
		}
		
		String content = HtmCache.getInstance().getHtm(CB_PATH + "clan/clanhome-notice.htm");
		content = content.replaceAll("%clanid%", Integer.toString(clan.getClanId()));
		content = content.replace("%enabled%", "[" + String.valueOf(clan.isNoticeEnabled()) + "]");
		content = content.replace("%flag%", String.valueOf(!clan.isNoticeEnabled()));
		send1001(content, activeChar);
		send1002(activeChar, clan.getNotice(), "", "");
	}
	
	private static void sendClanList(L2PcInstance activeChar, int index)
	{
		String content = HtmCache.getInstance().getHtm(CB_PATH + "clan/clanlist.htm");
		
		// Player got a clan, show the associated header.
		String homeBar = "";
		
		final L2Clan playerClan = activeChar.getClan();
		if (playerClan != null)
		{
			homeBar = HOME_BAR;
			homeBar = homeBar.replace("%clanid%", Integer.toString(playerClan.getClanId()));
		}
		content = content.replace("%homebar%", homeBar);
		
		if (index < 1)
			index = 1;
		
		// List of clans.
		final StringBuilder html = new StringBuilder();
		
		int i = 0;
		for (L2Clan cl : ClanTable.getInstance().getClans())
		{
			if (i > (index + 1) * 7)
				break;
			
			if (i++ >= (index - 1) * 7)
				StringUtil.append(html, "<table width=610><tr><td width=5></td><td width=150 align=center><a action=\"bypass _bbsclan;home;", Integer.toString(cl.getClanId()), "\">", cl.getName(), "</a></td><td width=150 align=center>", cl.getLeaderName(), "</td><td width=100 align=center>", Integer.toString(cl.getLevel()), "</td><td width=200 align=center>", Integer.toString(cl.getMembersCount()), "</td><td width=5></td></tr></table><br1><img src=\"L2UI.Squaregray\" width=605 height=1><br1>");
		}
		html.append("<table><tr>");
		
		if (index == 1)
			html.append("<td><button action=\"\" back=\"l2ui_ch3.prev1_down\" fore=\"l2ui_ch3.prev1\" width=16 height=16></td>");
		else
			StringUtil.append(html, "<td><button action=\"_bbsclan;clan;", Integer.toString(index - 1), "\" back=\"l2ui_ch3.prev1_down\" fore=\"l2ui_ch3.prev1\" width=16 height=16 ></td>");
		
		i = 0;
		int nbp = ClanTable.getInstance().getClans().length / 8;
		if (nbp * 8 != ClanTable.getInstance().getClans().length)
			nbp++;
		
		for (i = 1; i <= nbp; i++)
		{
			if (i == index)
				StringUtil.append(html, "<td> ", Integer.toString(i), " </td>");
			else
				StringUtil.append(html, "<td><a action=\"bypass _bbsclan;clan;", Integer.toString(i), "\"> ", Integer.toString(i), " </a></td>");
		}
		
		if (index == nbp)
			html.append("<td><button action=\"\" back=\"l2ui_ch3.next1_down\" fore=\"l2ui_ch3.next1\" width=16 height=16></td>");
		else
			StringUtil.append(html, "<td><button action=\"bypass _bbsclan;clan;", Integer.toString(index + 1), "\" back=\"l2ui_ch3.next1_down\" fore=\"l2ui_ch3.next1\" width=16 height=16 ></td>");
		
		html.append("</tr></table>");
		
		content = content.replace("%clanlist%", html.toString());
		separateAndSend(content, activeChar);
	}
	
	private static void sendClanDetails(L2PcInstance activeChar, int clanId)
	{
		final L2Clan clan = ClanTable.getInstance().getClan(clanId);
		if (clan == null)
			return;
		
		if (clan.getLevel() < 2)
		{
			activeChar.sendPacket(SystemMessageId.NO_CB_IN_MY_CLAN);
			sendClanList(activeChar, 1);
			return;
		}
		
		// Load different HTM following player case, 3 possibilites : randomer, member, clan leader.
		String content;
		if (activeChar.getClanId() != clanId)
			content = HtmCache.getInstance().getHtm(CB_PATH + "clan/clanhome.htm");
		else if (activeChar.isClanLeader())
			content = HtmCache.getInstance().getHtm(CB_PATH + "clan/clanhome-leader.htm");
		else
			content = HtmCache.getInstance().getHtm(CB_PATH + "clan/clanhome-member.htm");
		
		content = content.replaceAll("%clanid%", Integer.toString(clan.getClanId()));
		content = content.replace("%clanIntro%", clan.getIntroduction());
		content = content.replace("%clanName%", clan.getName());
		content = content.replace("%clanLvL%", Integer.toString(clan.getLevel()));
		content = content.replace("%clanMembers%", Integer.toString(clan.getMembersCount()));
		content = content.replaceAll("%clanLeader%", clan.getLeaderName());
		content = content.replace("%allyName%", (clan.getAllyId() > 0) ? clan.getAllyName() : "");
		separateAndSend(content, activeChar);
	}
	
	private static class SingletonHolder
	{
		protected static final ClanBBSManager _instance = new ClanBBSManager();
	}
}