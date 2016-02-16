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
package quests.Q161_FruitOfTheMotherTree;

import net.sf.l2j.gameserver.model.actor.L2Npc;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.model.base.Race;
import net.sf.l2j.gameserver.model.quest.Quest;
import net.sf.l2j.gameserver.model.quest.QuestState;

public class Q161_FruitOfTheMotherTree extends Quest
{
	private static final String qn = "Q161_FruitOfTheMotherTree";
	
	// NPCs
	private static final int ANDELLIA = 30362;
	private static final int THALIA = 30371;
	
	// Items
	private static final int ANDELLIA_LETTER = 1036;
	private static final int MOTHERTREE_FRUIT = 1037;
	
	public Q161_FruitOfTheMotherTree()
	{
		super(161, qn, "Fruit of the Mothertree");
		
		setItemsIds(ANDELLIA_LETTER, MOTHERTREE_FRUIT);
		
		addStartNpc(ANDELLIA);
		addTalkId(ANDELLIA, THALIA);
	}
	
	@Override
	public String onAdvEvent(String event, L2Npc npc, L2PcInstance player)
	{
		String htmltext = event;
		QuestState st = player.getQuestState(qn);
		if (st == null)
			return htmltext;
		
		if (event.equalsIgnoreCase("30362-04.htm"))
		{
			st.setState(STATE_STARTED);
			st.set("cond", "1");
			st.playSound(QuestState.SOUND_ACCEPT);
			st.giveItems(ANDELLIA_LETTER, 1);
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
				if (player.getRace() != Race.Elf)
					htmltext = "30362-00.htm";
				else if (player.getLevel() < 3)
					htmltext = "30362-02.htm";
				else
					htmltext = "30362-03.htm";
				break;
			
			case STATE_STARTED:
				int cond = st.getInt("cond");
				switch (npc.getNpcId())
				{
					case ANDELLIA:
						if (cond == 1)
							htmltext = "30362-05.htm";
						else if (cond == 2)
						{
							htmltext = "30362-06.htm";
							st.takeItems(MOTHERTREE_FRUIT, 1);
							st.rewardItems(57, 1000);
							st.rewardExpAndSp(1000, 0);
							st.playSound(QuestState.SOUND_FINISH);
							st.exitQuest(false);
						}
						break;
					
					case THALIA:
						if (cond == 1)
						{
							htmltext = "30371-01.htm";
							st.set("cond", "2");
							st.playSound(QuestState.SOUND_MIDDLE);
							st.takeItems(ANDELLIA_LETTER, 1);
							st.giveItems(MOTHERTREE_FRUIT, 1);
						}
						else if (cond == 2)
							htmltext = "30371-02.htm";
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
		new Q161_FruitOfTheMotherTree();
	}
}