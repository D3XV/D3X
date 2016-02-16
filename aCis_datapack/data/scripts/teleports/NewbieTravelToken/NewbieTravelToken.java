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
package teleports.NewbieTravelToken;

import java.util.HashMap;
import java.util.Map;

import net.sf.l2j.gameserver.model.actor.L2Npc;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.model.quest.Quest;
import net.sf.l2j.gameserver.model.quest.QuestState;

public class NewbieTravelToken extends Quest
{
	private final static Map<String, int[]> data = new HashMap<>();
	{
		data.put("30600", new int[]
		{
			12160,
			16554,
			-4583
		}); // DE
		data.put("30601", new int[]
		{
			115594,
			-177993,
			-912
		}); // DW
		data.put("30599", new int[]
		{
			45470,
			48328,
			-3059
		}); // EV
		data.put("30602", new int[]
		{
			-45067,
			-113563,
			-199
		}); // OV
		data.put("30598", new int[]
		{
			-84053,
			243343,
			-3729
		}); // TI
	}
	
	private final static int TOKEN = 8542;
	
	public NewbieTravelToken(int questId, String name, String descr)
	{
		super(questId, name, descr);
		
		addStartNpc(30598, 30599, 30600, 30601, 30602);
		addTalkId(30598, 30599, 30600, 30601, 30602);
	}
	
	@Override
	public String onAdvEvent(String event, L2Npc npc, L2PcInstance player)
	{
		QuestState st = player.getQuestState(getName());
		if (st == null)
			st = newQuestState(player);
		
		if (data.containsKey(event))
		{
			int x = data.get(event)[0];
			int y = data.get(event)[1];
			int z = data.get(event)[2];
			
			if (st.getQuestItemsCount(TOKEN) != 0)
			{
				st.takeItems(TOKEN, 1);
				st.getPlayer().teleToLocation(x, y, z, 0);
			}
			else
				return "notoken.htm";
		}
		st.exitQuest(true);
		return super.onAdvEvent(event, npc, player);
	}
	
	@Override
	public String onTalk(L2Npc npc, L2PcInstance player)
	{
		String htmltext = "";
		QuestState st = player.getQuestState(getName());
		int npcId = npc.getNpcId();
		
		if (player.getLevel() >= 20)
		{
			htmltext = "wronglevel.htm";
			st.exitQuest(true);
		}
		else
			htmltext = npcId + ".htm";
		
		return htmltext;
	}
	
	public static void main(String[] args)
	{
		new NewbieTravelToken(-1, "NewbieTravelToken", "teleports");
	}
}