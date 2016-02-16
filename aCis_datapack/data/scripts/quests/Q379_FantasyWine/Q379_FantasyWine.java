/*
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with
 * this program. If not, see <http://www.gnu.org/licenses/>.
 */
package quests.Q379_FantasyWine;

import net.sf.l2j.gameserver.model.actor.L2Npc;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.model.quest.Quest;
import net.sf.l2j.gameserver.model.quest.QuestState;
import net.sf.l2j.util.Rnd;

public class Q379_FantasyWine extends Quest
{
	private static final String qn = "Q379_FantasyWine";
	
	// NPCs
	private static final int HARLAN = 30074;
	
	// Monsters
	private static final int ENKU_CHAMPION = 20291;
	private static final int ENKU_SHAMAN = 20292;
	
	// Items
	private static final int LEAF = 5893;
	private static final int STONE = 5894;
	
	public Q379_FantasyWine()
	{
		super(379, qn, "Fantasy Wine");
		
		setItemsIds(LEAF, STONE);
		
		addStartNpc(HARLAN);
		addTalkId(HARLAN);
		
		addKillId(ENKU_CHAMPION, ENKU_SHAMAN);
	}
	
	@Override
	public String onAdvEvent(String event, L2Npc npc, L2PcInstance player)
	{
		String htmltext = event;
		QuestState st = player.getQuestState(qn);
		if (st == null)
			return htmltext;
		
		if (event.equalsIgnoreCase("30074-3.htm"))
		{
			st.setState(STATE_STARTED);
			st.set("cond", "1");
			st.playSound(QuestState.SOUND_ACCEPT);
		}
		else if (event.equalsIgnoreCase("30074-6.htm"))
		{
			st.takeItems(LEAF, 80);
			st.takeItems(STONE, 100);
			
			final int rand = Rnd.get(10);
			if (rand < 3)
			{
				htmltext = "30074-6.htm";
				st.giveItems(5956, 1);
			}
			else if (rand < 9)
			{
				htmltext = "30074-7.htm";
				st.giveItems(5957, 1);
			}
			else
			{
				htmltext = "30074-8.htm";
				st.giveItems(5958, 1);
			}
			
			st.playSound(QuestState.SOUND_FINISH);
			st.exitQuest(true);
		}
		else if (event.equalsIgnoreCase("30074-2a.htm"))
			st.exitQuest(true);
		
		return htmltext;
	}
	
	@Override
	public String onTalk(L2Npc npc, L2PcInstance player)
	{
		QuestState st = player.getQuestState(qn);
		String htmltext = getNoQuestMsg();
		if (st == null)
			return htmltext;
		
		switch (st.getState())
		{
			case STATE_CREATED:
				htmltext = (player.getLevel() < 20) ? "30074-0a.htm" : "30074-0.htm";
				break;
			
			case STATE_STARTED:
				final int leaf = st.getQuestItemsCount(LEAF);
				final int stone = st.getQuestItemsCount(STONE);
				
				if (leaf == 80 && stone == 100)
					htmltext = "30074-5.htm";
				else if (leaf == 80)
					htmltext = "30074-4a.htm";
				else if (stone == 100)
					htmltext = "30074-4b.htm";
				else
					htmltext = "30074-4.htm";
				break;
		}
		
		return htmltext;
	}
	
	@Override
	public String onKill(L2Npc npc, L2PcInstance player, boolean isPet)
	{
		QuestState st = checkPlayerState(player, npc, STATE_STARTED);
		if (st == null)
			return null;
		
		if (npc.getNpcId() == ENKU_CHAMPION)
		{
			if (st.dropItemsAlways(LEAF, 1, 80) && st.getQuestItemsCount(STONE) >= 100)
				st.set("cond", "2");
		}
		else if (st.dropItemsAlways(STONE, 1, 100) && st.getQuestItemsCount(LEAF) >= 80)
			st.set("cond", "2");
		
		return null;
	}
	
	public static void main(String[] args)
	{
		new Q379_FantasyWine();
	}
}