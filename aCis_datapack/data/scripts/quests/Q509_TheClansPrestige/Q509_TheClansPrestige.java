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
package quests.Q509_TheClansPrestige;

import net.sf.l2j.gameserver.model.L2Clan;
import net.sf.l2j.gameserver.model.actor.L2Npc;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.model.quest.Quest;
import net.sf.l2j.gameserver.model.quest.QuestState;
import net.sf.l2j.gameserver.network.SystemMessageId;
import net.sf.l2j.gameserver.network.serverpackets.PledgeShowInfoUpdate;
import net.sf.l2j.gameserver.network.serverpackets.SystemMessage;
import net.sf.l2j.gameserver.util.Util;
import net.sf.l2j.util.Rnd;

public class Q509_TheClansPrestige extends Quest
{
	private static final String qn = "Q509_TheClansPrestige";
	
	// NPCs
	private static final int VALDIS = 31331;
	
	// Items
	private static final int DAIMON_EYES = 8489;
	private static final int HESTIA_FAIRY_STONE = 8490;
	private static final int NUCLEUS_OF_LESSER_GOLEM = 8491;
	private static final int FALSTON_FANG = 8492;
	private static final int SHAID_TALON = 8493;
	
	// Raid Bosses
	private static final int DAIMON_THE_WHITE_EYED = 25290;
	private static final int HESTIA_GUARDIAN_DEITY = 25293;
	private static final int PLAGUE_GOLEM = 25523;
	private static final int DEMON_AGENT_FALSTON = 25322;
	private static final int QUEEN_SHYEED = 25514;
	
	// Reward list (itemId, minClanPoints, maxClanPoints)
	private static final int reward_list[][] =
	{
		{
			DAIMON_THE_WHITE_EYED,
			DAIMON_EYES,
			180,
			215
		},
		{
			HESTIA_GUARDIAN_DEITY,
			HESTIA_FAIRY_STONE,
			430,
			465
		},
		{
			PLAGUE_GOLEM,
			NUCLEUS_OF_LESSER_GOLEM,
			380,
			415
		},
		{
			DEMON_AGENT_FALSTON,
			FALSTON_FANG,
			220,
			255
		},
		{
			QUEEN_SHYEED,
			SHAID_TALON,
			130,
			165
		}
	};
	
	// Radar
	private static final int radar[][] =
	{
		{
			186320,
			-43904,
			-3175
		},
		{
			134672,
			-115600,
			-1216
		},
		{
			170000,
			-59900,
			-3848
		},
		{
			93296,
			-75104,
			-1824
		},
		{
			79635,
			-55612,
			-5980
		}
	};
	
	public Q509_TheClansPrestige()
	{
		super(509, qn, "The Clan's Prestige");
		
		setItemsIds(DAIMON_EYES, HESTIA_FAIRY_STONE, NUCLEUS_OF_LESSER_GOLEM, FALSTON_FANG, SHAID_TALON);
		
		addStartNpc(VALDIS);
		addTalkId(VALDIS);
		
		addKillId(DAIMON_THE_WHITE_EYED, HESTIA_GUARDIAN_DEITY, PLAGUE_GOLEM, DEMON_AGENT_FALSTON, QUEEN_SHYEED);
	}
	
	@Override
	public String onAdvEvent(String event, L2Npc npc, L2PcInstance player)
	{
		String htmltext = event;
		QuestState st = player.getQuestState(qn);
		if (st == null)
			return htmltext;
		
		if (Util.isDigit(event))
		{
			htmltext = "31331-" + event + ".htm";
			st.setState(STATE_STARTED);
			st.set("cond", "1");
			st.set("raid", event);
			st.playSound(QuestState.SOUND_ACCEPT);
			
			int evt = Integer.parseInt(event);
			
			int x = radar[evt - 1][0];
			int y = radar[evt - 1][1];
			int z = radar[evt - 1][2];
			
			if (x + y + z > 0)
				st.addRadar(x, y, z);
		}
		else if (event.equalsIgnoreCase("31331-6.htm"))
		{
			st.playSound(QuestState.SOUND_FINISH);
			st.exitQuest(true);
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
		
		L2Clan clan = player.getClan();
		
		switch (st.getState())
		{
			case STATE_CREATED:
				if (!player.isClanLeader())
					htmltext = "31331-0a.htm";
				else if (clan.getLevel() < 6)
					htmltext = "31331-0b.htm";
				else
					htmltext = "31331-0c.htm";
				break;
			
			case STATE_STARTED:
				final int raid = st.getInt("raid");
				final int item = reward_list[raid - 1][1];
				
				if (!st.hasQuestItems(item))
					htmltext = "31331-" + raid + "a.htm";
				else
				{
					final int reward = Rnd.get(reward_list[raid - 1][2], reward_list[raid - 1][3]);
					
					htmltext = "31331-" + raid + "b.htm";
					st.takeItems(item, 1);
					clan.addReputationScore(reward);
					player.sendPacket(SystemMessage.getSystemMessage(SystemMessageId.CLAN_QUEST_COMPLETED_AND_S1_POINTS_GAINED).addNumber(reward));
					clan.broadcastToOnlineMembers(new PledgeShowInfoUpdate(clan));
				}
				break;
		}
		
		return htmltext;
	}
	
	@Override
	public String onKill(L2Npc npc, L2PcInstance player, boolean isPet)
	{
		// Retrieve the qs of the clan leader.
		QuestState st = getClanLeaderQuestState(player, npc);
		if (st == null || !st.isStarted())
			return null;
		
		// Reward only if quest is setup on good index.
		int raid = st.getInt("raid");
		if (reward_list[raid - 1][0] == npc.getNpcId())
			st.dropItemsAlways(reward_list[raid - 1][1], 1, 1);
		
		return null;
	}
	
	public static void main(String[] args)
	{
		new Q509_TheClansPrestige();
	}
}