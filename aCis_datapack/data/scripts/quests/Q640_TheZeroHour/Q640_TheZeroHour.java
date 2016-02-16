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
package quests.Q640_TheZeroHour;

import net.sf.l2j.gameserver.model.actor.L2Npc;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.model.quest.Quest;
import net.sf.l2j.gameserver.model.quest.QuestState;
import net.sf.l2j.gameserver.util.Util;

public class Q640_TheZeroHour extends Quest
{
	private static final String qn = "Q640_TheZeroHour";
	
	// NPC
	private static final int KAHMAN = 31554;
	
	// Item
	private static final int FANG_OF_STAKATO = 8085;
	
	private static final int[][] REWARDS =
	{
		{
			12,
			4042,
			1
		},
		{
			6,
			4043,
			1
		},
		{
			6,
			4044,
			1
		},
		{
			81,
			1887,
			10
		},
		{
			33,
			1888,
			5
		},
		{
			30,
			1889,
			10
		},
		{
			150,
			5550,
			10
		},
		{
			131,
			1890,
			10
		},
		{
			123,
			1893,
			5
		}
	};
	
	public Q640_TheZeroHour()
	{
		super(640, qn, "The Zero Hour");
		
		setItemsIds(FANG_OF_STAKATO);
		
		addStartNpc(KAHMAN);
		addTalkId(KAHMAN);
		
		// All "spiked" stakatos types, except babies and cannibalistic followers.
		addKillId(22105, 22106, 22107, 22108, 22109, 22110, 22111, 22113, 22114, 22115, 22116, 22117, 22118, 22119, 22121);
	}
	
	@Override
	public String onAdvEvent(String event, L2Npc npc, L2PcInstance player)
	{
		String htmltext = event;
		QuestState st = player.getQuestState(qn);
		if (st == null)
			return htmltext;
		
		if (event.equalsIgnoreCase("31554-02.htm"))
		{
			st.setState(STATE_STARTED);
			st.set("cond", "1");
			st.playSound(QuestState.SOUND_ACCEPT);
		}
		else if (event.equalsIgnoreCase("31554-05.htm"))
		{
			if (!st.hasQuestItems(FANG_OF_STAKATO))
				htmltext = "31554-06.htm";
		}
		else if (event.equalsIgnoreCase("31554-08.htm"))
		{
			st.playSound(QuestState.SOUND_FINISH);
			st.exitQuest(true);
		}
		else if (Util.isDigit(event))
		{
			int reward[] = REWARDS[Integer.parseInt(event)];
			
			if (st.getQuestItemsCount(FANG_OF_STAKATO) >= reward[0])
			{
				htmltext = "31554-09.htm";
				st.takeItems(FANG_OF_STAKATO, reward[0]);
				st.rewardItems(reward[1], reward[2]);
			}
			else
				htmltext = "31554-06.htm";
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
				if (player.getLevel() < 66)
					htmltext = "31554-00.htm";
				else
				{
					QuestState st2 = player.getQuestState("Q109_InSearchOfTheNest");
					htmltext = (st2 != null && st2.isCompleted()) ? "31554-01.htm" : "31554-10.htm";
				}
				break;
			
			case STATE_STARTED:
				htmltext = (st.hasQuestItems(FANG_OF_STAKATO)) ? "31554-04.htm" : "31554-03.htm";
				break;
		}
		
		return htmltext;
	}
	
	@Override
	public String onKill(L2Npc npc, L2PcInstance player, boolean isPet)
	{
		L2PcInstance partyMember = getRandomPartyMemberState(player, npc, STATE_STARTED);
		if (partyMember == null)
			return null;
		
		partyMember.getQuestState(qn).dropItemsAlways(FANG_OF_STAKATO, 1, 0);
		
		return null;
	}
	
	public static void main(String[] args)
	{
		new Q640_TheZeroHour();
	}
}