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
package net.sf.l2j.gameserver.model.actor.instance;

import java.util.List;

import net.sf.l2j.gameserver.datatables.MultisellData;
import net.sf.l2j.gameserver.model.actor.template.NpcTemplate;
import net.sf.l2j.gameserver.model.entity.Hero;
import net.sf.l2j.gameserver.model.olympiad.CompetitionType;
import net.sf.l2j.gameserver.model.olympiad.Olympiad;
import net.sf.l2j.gameserver.model.olympiad.OlympiadGameManager;
import net.sf.l2j.gameserver.model.olympiad.OlympiadGameTask;
import net.sf.l2j.gameserver.model.olympiad.OlympiadManager;
import net.sf.l2j.gameserver.network.serverpackets.ActionFailed;
import net.sf.l2j.gameserver.network.serverpackets.ExHeroList;
import net.sf.l2j.gameserver.network.serverpackets.NpcHtmlMessage;
import net.sf.l2j.util.StringUtil;

/**
 * Olympiad Npcs
 * @author godson && Tryskell
 */
public class L2OlympiadManagerInstance extends L2NpcInstance
{
	private static final int GATE_PASS = 6651;
	
	public L2OlympiadManagerInstance(int objectId, NpcTemplate template)
	{
		super(objectId, template);
	}
	
	@Override
	public String getHtmlPath(int npcId, int val)
	{
		// Only used by Olympiad managers. Monument of Heroes don't use "Chat" bypass.
		String filename = "noble";
		
		if (val > 0)
			filename = "noble_" + val;
		
		return filename + ".htm";
	}
	
	@Override
	public void showChatWindow(L2PcInstance player, int val)
	{
		int npcId = getTemplate().getNpcId();
		String filename = getHtmlPath(npcId, val);
		
		switch (npcId)
		{
			case 31688: // Olympiad managers
				if (player.isNoble() && val == 0)
					filename = "noble_main.htm";
				break;
			
			case 31690: // Monuments of Heroes
			case 31769:
			case 31770:
			case 31771:
			case 31772:
				if (player.isHero() || Hero.getInstance().isInactiveHero(player.getObjectId()))
					filename = "hero_main.htm";
				else
					filename = "hero_main2.htm";
				break;
		}
		
		// Send a Server->Client NpcHtmlMessage containing the text of the L2Npc to the L2PcInstance
		NpcHtmlMessage html = new NpcHtmlMessage(getObjectId());
		html.setFile("data/html/olympiad/" + filename);
		
		// Hidden option for players who are in inactive mode.
		if (filename == "hero_main.htm")
		{
			String hiddenText = "";
			if (Hero.getInstance().isInactiveHero(player.getObjectId()))
				hiddenText = "<a action=\"bypass -h npc_%objectId%_Olympiad 5\">\"I want to be a Hero.\"</a><br>";
			
			html.replace("%hero%", hiddenText);
		}
		html.replace("%objectId%", getObjectId());
		player.sendPacket(html);
		
		// Send a Server->Client ActionFailed to the L2PcInstance in order to avoid that the client wait another packet
		player.sendPacket(ActionFailed.STATIC_PACKET);
	}
	
	@Override
	public void onBypassFeedback(L2PcInstance player, String command)
	{
		if (command.startsWith("OlympiadNoble"))
		{
			final NpcHtmlMessage html = new NpcHtmlMessage(getObjectId());
			if (player.isCursedWeaponEquipped())
			{
				html.setFile(Olympiad.OLYMPIAD_HTML_PATH + "noble_cant_cw.htm");
				player.sendPacket(html);
				return;
			}
			
			if (player.getClassIndex() != 0)
			{
				html.setFile(Olympiad.OLYMPIAD_HTML_PATH + "noble_cant_sub.htm");
				html.replace("%objectId%", getObjectId());
				player.sendPacket(html);
				return;
			}
			
			if (!player.isNoble() || (player.getClassId().level() < 3))
			{
				html.setFile(Olympiad.OLYMPIAD_HTML_PATH + "noble_cant_thirdclass.htm");
				html.replace("%objectId%", getObjectId());
				player.sendPacket(html);
				return;
			}
			
			int val = Integer.parseInt(command.substring(14));
			switch (val)
			{
				case 1: // Unregister
					OlympiadManager.getInstance().unRegisterNoble(player);
					break;
				
				case 2: // Show waiting list
					final int nonClassed = OlympiadManager.getInstance().getRegisteredNonClassBased().size();
					final int classed = OlympiadManager.getInstance().getRegisteredClassBased().size();
					
					html.setFile(Olympiad.OLYMPIAD_HTML_PATH + "noble_registered.htm");
					html.replace("%listClassed%", classed);
					html.replace("%listNonClassed%", nonClassed);
					html.replace("%objectId%", getObjectId());
					player.sendPacket(html);
					break;
				
				case 3: // There are %points% Grand Olympiad points granted for this event.
					int points = Olympiad.getInstance().getNoblePoints(player.getObjectId());
					html.setFile(Olympiad.OLYMPIAD_HTML_PATH + "noble_points1.htm");
					html.replace("%points%", points);
					html.replace("%objectId%", getObjectId());
					player.sendPacket(html);
					break;
				
				case 4: // register non classed based
					OlympiadManager.getInstance().registerNoble(player, CompetitionType.NON_CLASSED);
					break;
				
				case 5: // register classed based
					OlympiadManager.getInstance().registerNoble(player, CompetitionType.CLASSED);
					break;
				
				case 6: // request tokens reward
					html.setFile(Olympiad.OLYMPIAD_HTML_PATH + ((Olympiad.getInstance().getNoblessePasses(player, false) > 0) ? "noble_settle.htm" : "noble_nopoints2.htm"));
					html.replace("%objectId%", getObjectId());
					player.sendPacket(html);
					break;
				
				case 7: // Rewards
					MultisellData.getInstance().separateAndSend(102, player, false, getCastle().getTaxRate());
					break;
				
				case 10: // Give tokens to player
					player.addItem("Olympiad", GATE_PASS, Olympiad.getInstance().getNoblessePasses(player, true), player, true);
					break;
				
				default:
					_log.warning("Olympiad: Couldnt send packet for request: " + val);
					break;
			}
		}
		else if (command.startsWith("Olympiad"))
		{
			int val = Integer.parseInt(command.substring(9, 10));
			
			NpcHtmlMessage reply = new NpcHtmlMessage(getObjectId());
			switch (val)
			{
				case 2: // Show rank for a specific class, example >> Olympiad 1_88
					int classId = Integer.parseInt(command.substring(11));
					if (classId >= 88 && classId <= 118)
					{
						List<String> names = Olympiad.getInstance().getClassLeaderBoard(classId);
						reply.setFile(Olympiad.OLYMPIAD_HTML_PATH + "noble_ranking.htm");
						
						int index = 1;
						for (String name : names)
						{
							reply.replace("%place" + index + "%", index);
							reply.replace("%rank" + index + "%", name);
							
							index++;
							if (index > 10)
								break;
						}
						
						for (; index <= 10; index++)
						{
							reply.replace("%place" + index + "%", "");
							reply.replace("%rank" + index + "%", "");
						}
						
						reply.replace("%objectId%", getObjectId());
						player.sendPacket(reply);
					}
					break;
				
				case 3: // Spectator overview
					StringBuilder list = new StringBuilder(2000);
					OlympiadGameTask task;
					
					reply.setFile(Olympiad.OLYMPIAD_HTML_PATH + "olympiad_observe_list.htm");
					for (int i = 0; i <= 21; i++)
					{
						task = OlympiadGameManager.getInstance().getOlympiadTask(i);
						if (task != null)
						{
							StringUtil.append(list, "<a action=\"bypass arenachange ", String.valueOf(i), "\">Arena ", String.valueOf(i + 1), "&nbsp;");
							
							if (task.isGameStarted())
							{
								if (task.isInTimerTime())
									StringUtil.append(list, "(&$907;)"); // Counting In Progress
								else if (task.isBattleStarted())
									StringUtil.append(list, "(&$829;)"); // In Progress
								else
									StringUtil.append(list, "(&$908;)"); // Terminate
									
								StringUtil.append(list, "&nbsp;", task.getGame().getPlayerNames()[0], "&nbsp; : &nbsp;", task.getGame().getPlayerNames()[1]);
							}
							else
								StringUtil.append(list, "(&$906;)", "</td><td>&nbsp;"); // Initial State
								
							StringUtil.append(list, "</a><br>");
						}
					}
					reply.replace("%list%", list.toString());
					reply.replace("%objectId%", getObjectId());
					player.sendPacket(reply);
					break;
				
				case 4: // Send heroes list.
					player.sendPacket(new ExHeroList());
					break;
				
				case 5: // Hero pending state.
					if (Hero.getInstance().isInactiveHero(player.getObjectId()))
					{
						reply.setFile(Olympiad.OLYMPIAD_HTML_PATH + "hero_confirm.htm");
						reply.replace("%objectId%", getObjectId());
						player.sendPacket(reply);
					}
					break;
				
				case 6: // Hero confirm action.
					if (Hero.getInstance().isInactiveHero(player.getObjectId()))
					{
						if (player.isSubClassActive() || player.getLevel() < 76)
						{
							player.sendMessage("You may only become an hero on a main class whose level is 75 or more.");
							return;
						}
						
						Hero.getInstance().activateHero(player);
					}
					break;
				
				case 7: // Main panel
					reply.setFile(Olympiad.OLYMPIAD_HTML_PATH + "hero_main.htm");
					
					String hiddenText = "";
					if (Hero.getInstance().isInactiveHero(player.getObjectId()))
						hiddenText = "<a action=\"bypass -h npc_%objectId%_Olympiad 5\">\"I want to be a Hero.\"</a><br>";
					
					reply.replace("%hero%", hiddenText);
					reply.replace("%objectId%", getObjectId());
					player.sendPacket(reply);
					break;
				
				default:
					_log.warning("Olympiad: Couldnt send packet for request: " + val);
					break;
			}
		}
		else
			super.onBypassFeedback(player, command);
	}
}