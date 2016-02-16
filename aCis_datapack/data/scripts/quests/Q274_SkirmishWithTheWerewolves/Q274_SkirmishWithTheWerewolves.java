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
package quests.Q274_SkirmishWithTheWerewolves;

import net.sf.l2j.gameserver.model.actor.L2Npc;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.model.base.Race;
import net.sf.l2j.gameserver.model.quest.Quest;
import net.sf.l2j.gameserver.model.quest.QuestState;
import net.sf.l2j.util.Rnd;

public class Q274_SkirmishWithTheWerewolves extends Quest
{
	private static final String qn = "Q274_SkirmishWithTheWerewolves";
	
	// Needed items
	private static final int NECKLACE_OF_VALOR = 1507;
	private static final int NECKLACE_OF_COURAGE = 1506;
	
	// Items
	private static final int MARAKU_WEREWOLF_HEAD = 1477;
	private static final int MARAKU_WOLFMEN_TOTEM = 1501;
	
	public Q274_SkirmishWithTheWerewolves()
	{
		super(274, qn, "Skirmish with the Werewolves");
		
		setItemsIds(MARAKU_WEREWOLF_HEAD, MARAKU_WOLFMEN_TOTEM);
		
		addStartNpc(30569);
		addTalkId(30569);
		
		addKillId(20363, 20364);
	}
	
	@Override
	public String onAdvEvent(String event, L2Npc npc, L2PcInstance player)
	{
		QuestState st = player.getQuestState(qn);
		String htmltext = event;
		if (st == null)
			return htmltext;
		
		if (event.equalsIgnoreCase("30569-03.htm"))
		{
			st.setState(STATE_STARTED);
			st.set("cond", "1");
			st.playSound(QuestState.SOUND_ACCEPT);
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
				if (player.getRace() != Race.Orc)
					htmltext = "30569-00.htm";
				else if (player.getLevel() < 9)
					htmltext = "30569-01.htm";
				else if (st.hasAtLeastOneQuestItem(NECKLACE_OF_COURAGE, NECKLACE_OF_VALOR))
					htmltext = "30569-02.htm";
				else
					htmltext = "30569-07.htm";
				break;
			
			case STATE_STARTED:
				if (st.getInt("cond") == 1)
					htmltext = "30569-04.htm";
				else
				{
					htmltext = "30569-05.htm";
					
					int amount = 3500 + st.getQuestItemsCount(MARAKU_WOLFMEN_TOTEM) * 600;
					
					st.takeItems(MARAKU_WEREWOLF_HEAD, -1);
					st.takeItems(MARAKU_WOLFMEN_TOTEM, -1);
					st.rewardItems(57, amount);
					
					st.playSound(QuestState.SOUND_FINISH);
					st.exitQuest(true);
				}
				break;
		}
		
		return htmltext;
	}
	
	@Override
	public String onKill(L2Npc npc, L2PcInstance player, boolean isPet)
	{
		QuestState st = checkPlayerCondition(player, npc, "cond", "1");
		if (st == null)
			return null;
		
		if (st.dropItemsAlways(MARAKU_WEREWOLF_HEAD, 1, 40))
			st.set("cond", "2");
		
		if (Rnd.get(100) < 6)
			st.giveItems(MARAKU_WOLFMEN_TOTEM, 1);
		
		return null;
	}
	
	public static void main(String[] args)
	{
		new Q274_SkirmishWithTheWerewolves();
	}
}