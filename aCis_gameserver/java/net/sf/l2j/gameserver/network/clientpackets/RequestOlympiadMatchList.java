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
package net.sf.l2j.gameserver.network.clientpackets;

import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.model.olympiad.Olympiad;
import net.sf.l2j.gameserver.model.olympiad.OlympiadGameManager;
import net.sf.l2j.gameserver.model.olympiad.OlympiadGameTask;
import net.sf.l2j.gameserver.network.serverpackets.NpcHtmlMessage;
import net.sf.l2j.util.StringUtil;

/**
 * format ch c: (id) 0xD0 h: (subid) 0x13
 * @author -Wooden-
 */
public final class RequestOlympiadMatchList extends L2GameClientPacket
{
	@Override
	protected void readImpl()
	{
	}
	
	@Override
	protected void runImpl()
	{
		final L2PcInstance activeChar = getClient().getActiveChar();
		if (activeChar == null || !activeChar.inObserverMode())
			return;
		
		NpcHtmlMessage message = new NpcHtmlMessage(0);
		StringBuilder list = new StringBuilder(1500);
		OlympiadGameTask task;
		
		message.setFile(Olympiad.OLYMPIAD_HTML_PATH + "olympiad_arena_observe_list.htm");
		for (int i = 0; i <= 21; i++)
		{
			task = OlympiadGameManager.getInstance().getOlympiadTask(i);
			if (task != null)
			{
				StringUtil.append(list, "<tr><td fixwidth=10><a action=\"bypass arenachange ", String.valueOf(i), "\">", String.valueOf(i + 1), "</a></td><td fixwidth=80>");
				
				if (task.isGameStarted())
				{
					if (task.isInTimerTime())
						StringUtil.append(list, "&$907;"); // Counting In Progress
					else if (task.isBattleStarted())
						StringUtil.append(list, "&$829;"); // In Progress
					else
						StringUtil.append(list, "&$908;"); // Terminate
						
					StringUtil.append(list, "</td><td>", task.getGame().getPlayerNames()[0], "&nbsp; / &nbsp;", task.getGame().getPlayerNames()[1]);
				}
				else
					StringUtil.append(list, "&$906;", "</td><td>&nbsp;"); // Initial State
					
				StringUtil.append(list, "</td><td><font color=\"aaccff\"></font></td></tr>");
			}
		}
		message.replace("%list%", list.toString());
		activeChar.sendPacket(message);
	}
}