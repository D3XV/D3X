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
package quests.Q633_InTheForgottenVillage;

import java.util.HashMap;
import java.util.Map;

import net.sf.l2j.gameserver.model.actor.L2Npc;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.model.quest.Quest;
import net.sf.l2j.gameserver.model.quest.QuestState;

public class Q633_InTheForgottenVillage extends Quest
{
	private static final String qn = "Q633_InTheForgottenVillage";
	
	// NPCS
	private static final int MINA = 31388;
	
	// ITEMS
	private static final int RIB_BONE = 7544;
	private static final int ZOMBIE_LIVER = 7545;
	
	// MOBS / DROP chances
	private static final Map<Integer, Integer> MOBS = new HashMap<>();
	{
		MOBS.put(21557, 328000); // Bone Snatcher
		MOBS.put(21558, 328000); // Bone Snatcher
		MOBS.put(21559, 337000); // Bone Maker
		MOBS.put(21560, 337000); // Bone Shaper
		MOBS.put(21563, 342000); // Bone Collector
		MOBS.put(21564, 348000); // Skull Collector
		MOBS.put(21565, 351000); // Bone Animator
		MOBS.put(21566, 359000); // Skull Animator
		MOBS.put(21567, 359000); // Bone Slayer
		MOBS.put(21572, 365000); // Bone Sweeper
		MOBS.put(21574, 383000); // Bone Grinder
		MOBS.put(21575, 383000); // Bone Grinder
		MOBS.put(21580, 385000); // Bone Caster
		MOBS.put(21581, 395000); // Bone Puppeteer
		MOBS.put(21583, 397000); // Bone Scavenger
		MOBS.put(21584, 401000); // Bone Scavenger
	}
	
	private static final Map<Integer, Integer> UNDEADS = new HashMap<>();
	{
		UNDEADS.put(21553, 347000); // Trampled Man
		UNDEADS.put(21554, 347000); // Trampled Man
		UNDEADS.put(21561, 450000); // Sacrificed Man
		UNDEADS.put(21578, 501000); // Behemoth Zombie
		UNDEADS.put(21596, 359000); // Requiem Lord
		UNDEADS.put(21597, 370000); // Requiem Behemoth
		UNDEADS.put(21598, 441000); // Requiem Behemoth
		UNDEADS.put(21599, 395000); // Requiem Priest
		UNDEADS.put(21600, 408000); // Requiem Behemoth
		UNDEADS.put(21601, 411000); // Requiem Behemoth
	}
	
	public Q633_InTheForgottenVillage()
	{
		super(633, qn, "In the Forgotten Village");
		
		setItemsIds(RIB_BONE, ZOMBIE_LIVER);
		
		addStartNpc(MINA);
		addTalkId(MINA);
		
		for (int i : MOBS.keySet())
			addKillId(i);
		
		for (int i : UNDEADS.keySet())
			addKillId(i);
	}
	
	@Override
	public String onAdvEvent(String event, L2Npc npc, L2PcInstance player)
	{
		String htmltext = event;
		QuestState st = player.getQuestState(qn);
		if (st == null)
			return htmltext;
		
		if (event.equalsIgnoreCase("31388-04.htm"))
		{
			st.setState(STATE_STARTED);
			st.set("cond", "1");
			st.playSound(QuestState.SOUND_ACCEPT);
		}
		else if (event.equalsIgnoreCase("31388-10.htm"))
		{
			st.takeItems(RIB_BONE, -1);
			st.playSound(QuestState.SOUND_GIVEUP);
			st.exitQuest(true);
		}
		else if (event.equalsIgnoreCase("31388-09.htm"))
		{
			if (st.getQuestItemsCount(RIB_BONE) >= 200)
			{
				htmltext = "31388-08.htm";
				st.takeItems(RIB_BONE, 200);
				st.rewardItems(57, 25000);
				st.rewardExpAndSp(305235, 0);
				st.playSound(QuestState.SOUND_FINISH);
			}
			st.set("cond", "1");
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
				htmltext = (player.getLevel() < 65) ? "31388-03.htm" : "31388-01.htm";
				break;
			
			case STATE_STARTED:
				final int cond = st.getInt("cond");
				if (cond == 1)
					htmltext = "31388-06.htm";
				else if (cond == 2)
					htmltext = "31388-05.htm";
				break;
		}
		
		return htmltext;
	}
	
	@Override
	public String onKill(L2Npc npc, L2PcInstance player, boolean isPet)
	{
		int npcId = npc.getNpcId();
		
		if (UNDEADS.containsKey(npcId))
		{
			L2PcInstance partyMember = getRandomPartyMemberState(player, npc, STATE_STARTED);
			if (partyMember == null)
				return null;
			
			partyMember.getQuestState(qn).dropItems(ZOMBIE_LIVER, 1, 0, UNDEADS.get(npcId));
		}
		else if (MOBS.containsKey(npcId))
		{
			L2PcInstance partyMember = getRandomPartyMember(player, npc, "1");
			if (partyMember == null)
				return null;
			
			QuestState st = partyMember.getQuestState(qn);
			
			if (st.dropItems(RIB_BONE, 1, 200, MOBS.get(npcId)))
				st.set("cond", "2");
		}
		
		return null;
	}
	
	public static void main(String[] args)
	{
		new Q633_InTheForgottenVillage();
	}
}