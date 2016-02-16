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
package quests.Q613_ProveYourCourage;

import net.sf.l2j.gameserver.model.actor.L2Npc;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.model.quest.Quest;
import net.sf.l2j.gameserver.model.quest.QuestState;

public class Q613_ProveYourCourage extends Quest
{
	private static final String qn = "Q613_ProveYourCourage";
	
	// Items
	private static final int HEAD_OF_HEKATON = 7240;
	private static final int FEATHER_OF_VALOR = 7229;
	private static final int VARKA_ALLIANCE_3 = 7223;
	
	public Q613_ProveYourCourage()
	{
		super(613, qn, "Prove your courage!");
		
		setItemsIds(HEAD_OF_HEKATON);
		
		addStartNpc(31377); // Ashas Varka Durai
		addTalkId(31377);
		
		addKillId(25299); // Hekaton
	}
	
	@Override
	public String onAdvEvent(String event, L2Npc npc, L2PcInstance player)
	{
		String htmltext = event;
		QuestState st = player.getQuestState(qn);
		if (st == null)
			return htmltext;
		
		if (event.equalsIgnoreCase("31377-04.htm"))
		{
			st.setState(STATE_STARTED);
			st.set("cond", "1");
			st.playSound(QuestState.SOUND_ACCEPT);
		}
		else if (event.equalsIgnoreCase("31377-07.htm"))
		{
			if (st.hasQuestItems(HEAD_OF_HEKATON))
			{
				st.takeItems(HEAD_OF_HEKATON, -1);
				st.giveItems(FEATHER_OF_VALOR, 1);
				st.rewardExpAndSp(10000, 0);
				st.playSound(QuestState.SOUND_FINISH);
				st.exitQuest(true);
			}
			else
			{
				htmltext = "31377-06.htm";
				st.set("cond", "1");
				st.playSound(QuestState.SOUND_ACCEPT);
			}
		}
		
		return htmltext;
	}
	
	@Override
	public String onTalk(L2Npc npc, L2PcInstance player)
	{
		String htmltext = getNoQuestMsg();
		QuestState st = player.getQuestState(qn);
		if (st == null)
			return htmltext;
		
		switch (st.getState())
		{
			case STATE_CREATED:
				if (player.getLevel() < 75)
					htmltext = "31377-03.htm";
				else if (player.getAllianceWithVarkaKetra() <= -3 && st.hasQuestItems(VARKA_ALLIANCE_3) && !st.hasQuestItems(FEATHER_OF_VALOR))
					htmltext = "31377-01.htm";
				else
					htmltext = "31377-02.htm";
				break;
			
			case STATE_STARTED:
				htmltext = (st.hasQuestItems(HEAD_OF_HEKATON)) ? "31377-05.htm" : "31377-06.htm";
				break;
		}
		
		return htmltext;
	}
	
	@Override
	public String onKill(L2Npc npc, L2PcInstance player, boolean isPet)
	{
		for (L2PcInstance partyMember : getPartyMembers(player, npc, "cond", "1"))
		{
			if (partyMember.getAllianceWithVarkaKetra() <= -3)
			{
				QuestState st = partyMember.getQuestState(qn);
				if (st.hasQuestItems(VARKA_ALLIANCE_3))
				{
					st.set("cond", "2");
					st.playSound(QuestState.SOUND_MIDDLE);
					st.giveItems(HEAD_OF_HEKATON, 1);
				}
			}
		}
		
		return null;
	}
	
	public static void main(String[] args)
	{
		new Q613_ProveYourCourage();
	}
}