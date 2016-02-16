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
package quests.Q027_ChestCaughtWithABaitOfWind;

import net.sf.l2j.gameserver.model.actor.L2Npc;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.model.quest.Quest;
import net.sf.l2j.gameserver.model.quest.QuestState;

public class Q027_ChestCaughtWithABaitOfWind extends Quest
{
	private static final String qn = "Q027_ChestCaughtWithABaitOfWind";
	
	// NPCs
	private static final int LANOSCO = 31570;
	private static final int SHALING = 31442;
	
	// Items
	private static final int LARGE_BLUE_TREASURE_CHEST = 6500;
	private static final int STRANGE_BLUEPRINT = 7625;
	private static final int BLACK_PEARL_RING = 880;
	
	public Q027_ChestCaughtWithABaitOfWind()
	{
		super(27, qn, "Chest caught with a bait of wind");
		
		setItemsIds(STRANGE_BLUEPRINT);
		
		addStartNpc(LANOSCO);
		addTalkId(LANOSCO, SHALING);
	}
	
	@Override
	public String onAdvEvent(String event, L2Npc npc, L2PcInstance player)
	{
		String htmltext = event;
		QuestState st = player.getQuestState(qn);
		if (st == null)
			return htmltext;
		
		if (event.equalsIgnoreCase("31570-04.htm"))
		{
			st.setState(STATE_STARTED);
			st.set("cond", "1");
			st.playSound(QuestState.SOUND_ACCEPT);
		}
		else if (event.equalsIgnoreCase("31570-07.htm"))
		{
			if (st.hasQuestItems(LARGE_BLUE_TREASURE_CHEST))
			{
				st.set("cond", "2");
				st.takeItems(LARGE_BLUE_TREASURE_CHEST, 1);
				st.giveItems(STRANGE_BLUEPRINT, 1);
			}
			else
				htmltext = "31570-08.htm";
		}
		else if (event.equalsIgnoreCase("31434-02.htm"))
		{
			if (st.hasQuestItems(STRANGE_BLUEPRINT))
			{
				htmltext = "31434-02.htm";
				st.takeItems(STRANGE_BLUEPRINT, 1);
				st.giveItems(BLACK_PEARL_RING, 1);
				st.playSound(QuestState.SOUND_FINISH);
				st.exitQuest(false);
			}
			else
				htmltext = "31434-03.htm";
		}
		
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
				if (player.getLevel() < 27)
					htmltext = "31570-02.htm";
				else
				{
					QuestState st2 = player.getQuestState("Q050_LanoscosSpecialBait");
					if (st2 != null && st2.isCompleted())
						htmltext = "31570-01.htm";
					else
						htmltext = "31570-03.htm";
				}
				break;
			
			case STATE_STARTED:
				int cond = st.getInt("cond");
				switch (npc.getNpcId())
				{
					case LANOSCO:
						if (cond == 1)
							htmltext = (!st.hasQuestItems(LARGE_BLUE_TREASURE_CHEST)) ? "31570-06.htm" : "31570-05.htm";
						else if (cond == 2)
							htmltext = "31570-09.htm";
						break;
					
					case SHALING:
						if (cond == 2)
							htmltext = "31434-01.htm";
						break;
				}
				break;
			
			case STATE_COMPLETED:
				htmltext = getAlreadyCompletedMsg();
				break;
		}
		
		return htmltext;
	}
	
	public static void main(String[] args)
	{
		new Q027_ChestCaughtWithABaitOfWind();
	}
}