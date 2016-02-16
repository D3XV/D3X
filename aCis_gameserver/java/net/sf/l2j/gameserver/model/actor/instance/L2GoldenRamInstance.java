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

import java.util.StringTokenizer;

import net.sf.l2j.gameserver.datatables.MultisellData;
import net.sf.l2j.gameserver.datatables.SkillTable;
import net.sf.l2j.gameserver.model.actor.template.NpcTemplate;
import net.sf.l2j.gameserver.model.quest.QuestState;
import net.sf.l2j.gameserver.network.serverpackets.ActionFailed;
import net.sf.l2j.gameserver.network.serverpackets.NpcHtmlMessage;

/**
 * This instance leads behaviors of Golden Ram mofos, where shown htm is different according to your quest condition. Abercrombie shows you multisells, Selina shows you Buffs list, when Pierce shows you "Quest" link.<br>
 * <br>
 * Kahman shows you only different htm. He's enthusiastic lazy-ass.
 * @author Tryskell
 */
public class L2GoldenRamInstance extends L2NpcInstance
{
	private static final String qn = "Q628_HuntOfTheGoldenRamMercenaryForce";
	
	private static final int[][] data =
	{
		{
			4404,
			2,
			2
		},
		{
			4405,
			2,
			2
		},
		{
			4393,
			3,
			3
		},
		{
			4400,
			2,
			3
		},
		{
			4397,
			1,
			3
		},
		{
			4399,
			2,
			3
		},
		{
			4401,
			1,
			6
		},
		{
			4402,
			2,
			6
		}
	};
	
	private static final int GOLDEN_RAM = 7251;
	
	public L2GoldenRamInstance(int objectId, NpcTemplate template)
	{
		super(objectId, template);
	}
	
	@Override
	public void showChatWindow(L2PcInstance player, int val)
	{
		int npcId = getNpcId();
		String filename = "data/html/default/" + npcId + ".htm";
		
		final QuestState st = player.getQuestState(qn);
		if (st != null)
		{
			int cond = st.getInt("cond");
			
			switch (npcId)
			{
				case 31553:
				case 31554:
					// Captain Pierce && Kahman ; different behavior if you got at least one badge.
					if (cond >= 2)
						filename = "data/html/default/" + npcId + "-1.htm";
					break;
				
				case 31555:
				case 31556:
					// Abercrombie and Selina
					if (cond == 2)
						filename = "data/html/default/" + npcId + "-1.htm";
					else if (cond == 3)
						filename = "data/html/default/" + npcId + "-2.htm";
					break;
			}
		}
		
		NpcHtmlMessage html = new NpcHtmlMessage(getObjectId());
		html.setFile(filename);
		html.replace("%objectId%", getObjectId());
		player.sendPacket(html);
		player.sendPacket(ActionFailed.STATIC_PACKET);
	}
	
	@Override
	public void onBypassFeedback(L2PcInstance player, String command)
	{
		final QuestState qs = player.getQuestState(qn);
		StringTokenizer st = new StringTokenizer(command, " ");
		String actualCommand = st.nextToken(); // Get actual command
		
		if (actualCommand.contains("buff"))
		{
			if (qs != null && qs.getInt("cond") == 3)
			{
				// Search the next token, which is a number between 0 and 7.
				int[] buffData = data[Integer.valueOf(st.nextToken())];
				
				int coins = buffData[2];
				int val = 3;
				
				if (qs.getQuestItemsCount(GOLDEN_RAM) >= coins)
				{
					qs.takeItems(GOLDEN_RAM, coins);
					setTarget(player);
					doCast(SkillTable.getInstance().getInfo(buffData[0], buffData[1]));
					val = 4;
				}
				
				NpcHtmlMessage html = new NpcHtmlMessage(getObjectId());
				html.setFile("data/html/default/31556-" + val + ".htm");
				player.sendPacket(html);
				return;
			}
		}
		else if (command.startsWith("gmultisell"))
		{
			if (qs != null && qs.getInt("cond") == 3)
				MultisellData.getInstance().separateAndSend(Integer.parseInt(command.substring(10).trim()), player, false, getCastle().getTaxRate());
		}
		else
			super.onBypassFeedback(player, command);
	}
}