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
package quests.Q508_AClansReputation;

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

public class Q508_AClansReputation extends Quest
{
	private static final String qn = "Q508_AClansReputation";
	
	// NPC
	private static final int SIR_ERIC_RODEMAI = 30868;
	
	// Items
	private static final int NUCLEUS_OF_FLAMESTONE_GIANT = 8494;
	private static final int THEMIS_SCALE = 8277;
	private static final int NUCLEUS_OF_HEKATON_PRIME = 8279;
	private static final int TIPHON_SHARD = 8280;
	private static final int GLAKI_NUCLEUS = 8281;
	private static final int RAHHA_FANG = 8282;
	
	// Raidbosses
	private static final int FLAMESTONE_GIANT = 25524;
	private static final int PALIBATI_QUEEN_THEMIS = 25252;
	private static final int HEKATON_PRIME = 25140;
	private static final int GARGOYLE_LORD_TIPHON = 25255;
	private static final int LAST_LESSER_GIANT_GLAKI = 25245;
	private static final int RAHHA = 25051;
	
	// Reward list (itemId, minClanPoints, maxClanPoints)
	private static final int reward_list[][] =
	{
		{
			PALIBATI_QUEEN_THEMIS,
			THEMIS_SCALE,
			65,
			100
		},
		{
			HEKATON_PRIME,
			NUCLEUS_OF_HEKATON_PRIME,
			40,
			75
		},
		{
			GARGOYLE_LORD_TIPHON,
			TIPHON_SHARD,
			30,
			65
		},
		{
			LAST_LESSER_GIANT_GLAKI,
			GLAKI_NUCLEUS,
			105,
			140
		},
		{
			RAHHA,
			RAHHA_FANG,
			40,
			75
		},
		{
			FLAMESTONE_GIANT,
			NUCLEUS_OF_FLAMESTONE_GIANT,
			60,
			95
		}
	};
	
	// Radar
	private static final int radar[][] =
	{
		{
			192346,
			21528,
			-3648
		},
		{
			191979,
			54902,
			-7658
		},
		{
			170038,
			-26236,
			-3824
		},
		{
			171762,
			55028,
			-5992
		},
		{
			117232,
			-9476,
			-3320
		},
		{
			144218,
			-5816,
			-4722
		}
	};
	
	public Q508_AClansReputation()
	{
		super(508, qn, "A Clan's Reputation");
		
		setItemsIds(THEMIS_SCALE, NUCLEUS_OF_HEKATON_PRIME, TIPHON_SHARD, GLAKI_NUCLEUS, RAHHA_FANG, NUCLEUS_OF_FLAMESTONE_GIANT);
		
		addStartNpc(SIR_ERIC_RODEMAI);
		addTalkId(SIR_ERIC_RODEMAI);
		
		addKillId(FLAMESTONE_GIANT, PALIBATI_QUEEN_THEMIS, HEKATON_PRIME, GARGOYLE_LORD_TIPHON, LAST_LESSER_GIANT_GLAKI, RAHHA);
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
			htmltext = "30868-" + event + ".htm";
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
		else if (event.equalsIgnoreCase("30868-7.htm"))
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
					htmltext = "30868-0a.htm";
				else if (clan.getLevel() < 5)
					htmltext = "30868-0b.htm";
				else
					htmltext = "30868-0c.htm";
				break;
			
			case STATE_STARTED:
				final int raid = st.getInt("raid");
				final int item = reward_list[raid - 1][1];
				
				if (!st.hasQuestItems(item))
					htmltext = "30868-" + raid + "a.htm";
				else
				{
					final int reward = Rnd.get(reward_list[raid - 1][2], reward_list[raid - 1][3]);
					
					htmltext = "30868-" + raid + "b.htm";
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
		// Retrieve the qS of the clan leader.
		QuestState st = getClanLeaderQuestState(player, npc);
		if (st == null || !st.isStarted())
			return null;
		
		// Reward only if quest is setup on good index.
		final int raid = st.getInt("raid");
		if (reward_list[raid - 1][0] == npc.getNpcId())
			st.dropItemsAlways(reward_list[raid - 1][1], 1, 1);
		
		return null;
	}
	
	public static void main(String[] args)
	{
		new Q508_AClansReputation();
	}
}