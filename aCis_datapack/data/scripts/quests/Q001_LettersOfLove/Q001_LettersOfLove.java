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
package quests.Q001_LettersOfLove;

import net.sf.l2j.gameserver.model.actor.L2Npc;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.model.quest.Quest;
import net.sf.l2j.gameserver.model.quest.QuestState;

public class Q001_LettersOfLove extends Quest
{
	private static final String qn = "Q001_LettersOfLove";
	
	// Npcs
	private static final int DARIN = 30048;
	private static final int ROXXY = 30006;
	private static final int BAULRO = 30033;
	
	// Items
	private static final int DARINGS_LETTER = 687;
	private static final int RAPUNZELS_KERCHIEF = 688;
	private static final int DARINGS_RECEIPT = 1079;
	private static final int BAULROS_POTION = 1080;
	
	// Reward
	private static final int NECKLACE = 906;
	
	public Q001_LettersOfLove()
	{
		super(1, qn, "Letters of Love");
		
		setItemsIds(DARINGS_LETTER, RAPUNZELS_KERCHIEF, DARINGS_RECEIPT, BAULROS_POTION);
		
		addStartNpc(DARIN);
		addTalkId(DARIN, ROXXY, BAULRO);
	}
	
	@Override
	public String onAdvEvent(String event, L2Npc npc, L2PcInstance player)
	{
		String htmltext = event;
		QuestState st = player.getQuestState(qn);
		if (st == null)
			return htmltext;
		
		if (event.equalsIgnoreCase("30048-06.htm"))
		{
			st.setState(STATE_STARTED);
			st.set("cond", "1");
			st.playSound(QuestState.SOUND_ACCEPT);
			st.giveItems(DARINGS_LETTER, 1);
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
				htmltext = (player.getLevel() < 2) ? "30048-01.htm" : "30048-02.htm";
				break;
			
			case STATE_STARTED:
				int cond = st.getInt("cond");
				switch (npc.getNpcId())
				{
					case DARIN:
						if (cond == 1)
							htmltext = "30048-07.htm";
						else if (cond == 2)
						{
							htmltext = "30048-08.htm";
							st.set("cond", "3");
							st.playSound(QuestState.SOUND_MIDDLE);
							st.takeItems(RAPUNZELS_KERCHIEF, 1);
							st.giveItems(DARINGS_RECEIPT, 1);
						}
						else if (cond == 3)
							htmltext = "30048-09.htm";
						else if (cond == 4)
						{
							htmltext = "30048-10.htm";
							st.takeItems(BAULROS_POTION, 1);
							st.giveItems(NECKLACE, 1);
							st.playSound(QuestState.SOUND_FINISH);
							st.exitQuest(false);
						}
						break;
					
					case ROXXY:
						if (cond == 1)
						{
							htmltext = "30006-01.htm";
							st.set("cond", "2");
							st.playSound(QuestState.SOUND_MIDDLE);
							st.takeItems(DARINGS_LETTER, 1);
							st.giveItems(RAPUNZELS_KERCHIEF, 1);
						}
						else if (cond == 2)
							htmltext = "30006-02.htm";
						else if (cond > 2)
							htmltext = "30006-03.htm";
						break;
					
					case BAULRO:
						if (cond == 3)
						{
							htmltext = "30033-01.htm";
							st.set("cond", "4");
							st.playSound(QuestState.SOUND_MIDDLE);
							st.takeItems(DARINGS_RECEIPT, 1);
							st.giveItems(BAULROS_POTION, 1);
						}
						else if (cond == 4)
							htmltext = "30033-02.htm";
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
		new Q001_LettersOfLove();
	}
}