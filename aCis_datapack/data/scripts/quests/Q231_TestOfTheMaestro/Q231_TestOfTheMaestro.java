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
package quests.Q231_TestOfTheMaestro;

import net.sf.l2j.gameserver.ai.CtrlIntention;
import net.sf.l2j.gameserver.model.actor.L2Attackable;
import net.sf.l2j.gameserver.model.actor.L2Npc;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.model.base.ClassId;
import net.sf.l2j.gameserver.model.quest.Quest;
import net.sf.l2j.gameserver.model.quest.QuestState;
import net.sf.l2j.gameserver.network.serverpackets.SocialAction;

public class Q231_TestOfTheMaestro extends Quest
{
	private static final String qn = "Q231_TestOfTheMaestro";
	
	private final static int RECOMMENDATION_OF_BALANKI = 2864;
	private final static int RECOMMENDATION_OF_FILAUR = 2865;
	private final static int RECOMMENDATION_OF_ARIN = 2866;
	private final static int LETTER_OF_SOLDER_DETACHMENT = 2868;
	private final static int PAINT_OF_KAMURU = 2869;
	private final static int NECKLACE_OF_KAMURU = 2870;
	private final static int PAINT_OF_TELEPORT_DEVICE = 2871;
	private final static int TELEPORT_DEVICE = 2872;
	private final static int ARCHITECTURE_OF_KRUMA = 2873;
	private final static int REPORT_OF_KRUMA = 2874;
	private final static int INGREDIENTS_OF_ANTIDOTE = 2875;
	private final static int STINGER_WASP_NEEDLE = 2876;
	private final static int MARSH_SPIDER_WEB = 2877;
	private final static int BLOOD_OF_LEECH = 2878;
	private final static int BROKEN_TELEPORT_DEVICE = 2916;
	
	// Rewards
	private final static int MARK_OF_MAESTRO = 2867;
	private final static int DIMENSIONAL_DIAMOND = 7562;
	
	// NPCs
	private final static int LOCKIRIN = 30531;
	private final static int SPIRON = 30532;
	private final static int BALANKI = 30533;
	private final static int KEEF = 30534;
	private final static int FILAUR = 30535;
	private final static int ARIN = 30536;
	private final static int TOMA = 30556;
	private final static int CROTO = 30671;
	private final static int DUBABAH = 30672;
	private final static int LORAIN = 30673;
	
	// Monsters
	private static final int KING_BUGBEAR = 20150;
	private static final int GIANT_MIST_LEECH = 20225;
	private static final int STINGER_WASP = 20229;
	private static final int MARSH_SPIDER = 20233;
	private static final int EVIL_EYE_LORD = 27133;
	
	public Q231_TestOfTheMaestro()
	{
		super(231, qn, "Test Of The Maestro");
		
		setItemsIds(RECOMMENDATION_OF_BALANKI, RECOMMENDATION_OF_FILAUR, RECOMMENDATION_OF_ARIN, LETTER_OF_SOLDER_DETACHMENT, PAINT_OF_KAMURU, NECKLACE_OF_KAMURU, PAINT_OF_TELEPORT_DEVICE, TELEPORT_DEVICE, ARCHITECTURE_OF_KRUMA, REPORT_OF_KRUMA, INGREDIENTS_OF_ANTIDOTE, STINGER_WASP_NEEDLE, MARSH_SPIDER_WEB, BLOOD_OF_LEECH, BROKEN_TELEPORT_DEVICE);
		
		addStartNpc(LOCKIRIN);
		addTalkId(LOCKIRIN, SPIRON, BALANKI, KEEF, FILAUR, ARIN, TOMA, CROTO, DUBABAH, LORAIN);
		
		addKillId(GIANT_MIST_LEECH, STINGER_WASP, MARSH_SPIDER, EVIL_EYE_LORD);
	}
	
	@Override
	public String onAdvEvent(String event, L2Npc npc, L2PcInstance player)
	{
		String htmltext = event;
		QuestState st = player.getQuestState(qn);
		if (st == null)
			return htmltext;
		
		// LOCKIRIN
		if (event.equalsIgnoreCase("30531-04a.htm"))
		{
			st.setState(STATE_STARTED);
			st.set("cond", "1");
			st.playSound(QuestState.SOUND_ACCEPT);
			st.giveItems(DIMENSIONAL_DIAMOND, 23);
		}
		// BALANKI
		else if (event.equalsIgnoreCase("30533-02.htm"))
			st.set("bCond", "1");
		// CROTO
		else if (event.equalsIgnoreCase("30671-02.htm"))
			st.giveItems(PAINT_OF_KAMURU, 1);
		// TOMA
		else if (event.equalsIgnoreCase("30556-05.htm"))
		{
			st.takeItems(PAINT_OF_TELEPORT_DEVICE, 1);
			st.giveItems(BROKEN_TELEPORT_DEVICE, 1);
			player.teleToLocation(140352, -194133, -3146, 0);
			startQuestTimer("spawn_bugbears", 5000, null, player, false);
		}
		// LORAIN
		else if (event.equalsIgnoreCase("30673-04.htm"))
		{
			st.set("fCond", "2");
			st.takeItems(BLOOD_OF_LEECH, -1);
			st.takeItems(INGREDIENTS_OF_ANTIDOTE, 1);
			st.takeItems(MARSH_SPIDER_WEB, -1);
			st.takeItems(STINGER_WASP_NEEDLE, -1);
			st.giveItems(REPORT_OF_KRUMA, 1);
		}
		// Spawns 3 King Bugbears
		else if (event.equalsIgnoreCase("spawn_bugbears"))
		{
			final L2Attackable bugbear1 = (L2Attackable) addSpawn(KING_BUGBEAR, 140333, -194153, -3138, 0, false, 200000, true);
			bugbear1.addDamageHate(player, 0, 999);
			bugbear1.getAI().setIntention(CtrlIntention.ATTACK, player);
			
			final L2Attackable bugbear2 = (L2Attackable) addSpawn(KING_BUGBEAR, 140395, -194147, -3146, 0, false, 200000, true);
			bugbear2.addDamageHate(player, 0, 999);
			bugbear2.getAI().setIntention(CtrlIntention.ATTACK, player);
			
			final L2Attackable bugbear3 = (L2Attackable) addSpawn(KING_BUGBEAR, 140304, -194082, -3157, 0, false, 200000, true);
			bugbear3.addDamageHate(player, 0, 999);
			bugbear3.getAI().setIntention(CtrlIntention.ATTACK, player);
			
			return null;
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
				if (player.getClassId() != ClassId.artisan)
					htmltext = "30531-01.htm";
				else if (player.getLevel() < 39)
					htmltext = "30531-02.htm";
				else
					htmltext = "30531-03.htm";
				break;
			
			case STATE_STARTED:
				switch (npc.getNpcId())
				{
					case LOCKIRIN:
						int cond = st.getInt("cond");
						if (cond == 1)
							htmltext = "30531-05.htm";
						else if (cond == 2)
						{
							htmltext = "30531-06.htm";
							st.takeItems(RECOMMENDATION_OF_ARIN, 1);
							st.takeItems(RECOMMENDATION_OF_BALANKI, 1);
							st.takeItems(RECOMMENDATION_OF_FILAUR, 1);
							st.giveItems(MARK_OF_MAESTRO, 1);
							st.rewardExpAndSp(46000, 5900);
							player.broadcastPacket(new SocialAction(player, 3));
							st.playSound(QuestState.SOUND_FINISH);
							st.exitQuest(false);
						}
						break;
					
					case SPIRON:
						htmltext = "30532-01.htm";
						break;
					
					case KEEF:
						htmltext = "30534-01.htm";
						break;
					
					// Part 1
					case BALANKI:
						int bCond = st.getInt("bCond");
						if (bCond == 0)
							htmltext = "30533-01.htm";
						else if (bCond == 1)
							htmltext = "30533-03.htm";
						else if (bCond == 2)
						{
							htmltext = "30533-04.htm";
							st.set("bCond", "3");
							st.takeItems(LETTER_OF_SOLDER_DETACHMENT, 1);
							st.giveItems(RECOMMENDATION_OF_BALANKI, 1);
							
							if (st.hasQuestItems(RECOMMENDATION_OF_ARIN, RECOMMENDATION_OF_FILAUR))
							{
								st.set("cond", "2");
								st.playSound(QuestState.SOUND_MIDDLE);
							}
						}
						else if (bCond == 3)
							htmltext = "30533-05.htm";
						break;
					
					case CROTO:
						bCond = st.getInt("bCond");
						if (bCond == 1)
						{
							if (!st.hasQuestItems(PAINT_OF_KAMURU))
								htmltext = "30671-01.htm";
							else if (!st.hasQuestItems(NECKLACE_OF_KAMURU))
								htmltext = "30671-03.htm";
							else
							{
								htmltext = "30671-04.htm";
								st.set("bCond", "2");
								st.takeItems(NECKLACE_OF_KAMURU, 1);
								st.takeItems(PAINT_OF_KAMURU, 1);
								st.giveItems(LETTER_OF_SOLDER_DETACHMENT, 1);
							}
						}
						else if (bCond > 1)
							htmltext = "30671-05.htm";
						break;
					
					case DUBABAH:
						htmltext = "30672-01.htm";
						break;
					
					// Part 2
					case ARIN:
						int aCond = st.getInt("aCond");
						if (aCond == 0)
						{
							htmltext = "30536-01.htm";
							st.set("aCond", "1");
							st.giveItems(PAINT_OF_TELEPORT_DEVICE, 1);
						}
						else if (aCond == 1)
							htmltext = "30536-02.htm";
						else if (aCond == 2)
						{
							htmltext = "30536-03.htm";
							st.set("aCond", "3");
							st.takeItems(TELEPORT_DEVICE, -1);
							st.giveItems(RECOMMENDATION_OF_ARIN, 1);
							
							if (st.hasQuestItems(RECOMMENDATION_OF_BALANKI, RECOMMENDATION_OF_FILAUR))
							{
								st.set("cond", "2");
								st.playSound(QuestState.SOUND_MIDDLE);
							}
						}
						else if (aCond == 3)
							htmltext = "30536-04.htm";
						break;
					
					case TOMA:
						aCond = st.getInt("aCond");
						if (aCond == 1)
						{
							if (!st.hasQuestItems(BROKEN_TELEPORT_DEVICE))
								htmltext = "30556-01.htm";
							else if (!st.hasQuestItems(TELEPORT_DEVICE))
							{
								htmltext = "30556-06.htm";
								st.set("aCond", "2");
								st.takeItems(BROKEN_TELEPORT_DEVICE, 1);
								st.giveItems(TELEPORT_DEVICE, 5);
							}
						}
						else if (aCond > 1)
							htmltext = "30556-07.htm";
						break;
					
					// Part 3
					case FILAUR:
						int fCond = st.getInt("fCond");
						if (fCond == 0)
						{
							htmltext = "30535-01.htm";
							st.set("fCond", "1");
							st.giveItems(ARCHITECTURE_OF_KRUMA, 1);
						}
						else if (fCond == 1)
							htmltext = "30535-02.htm";
						else if (fCond == 2)
						{
							htmltext = "30535-03.htm";
							st.set("fCond", "3");
							st.takeItems(REPORT_OF_KRUMA, 1);
							st.giveItems(RECOMMENDATION_OF_FILAUR, 1);
							
							if (st.hasQuestItems(RECOMMENDATION_OF_BALANKI, RECOMMENDATION_OF_ARIN))
							{
								st.set("cond", "2");
								st.playSound(QuestState.SOUND_MIDDLE);
							}
						}
						else if (fCond == 3)
							htmltext = "30535-04.htm";
						break;
					
					case LORAIN:
						fCond = st.getInt("fCond");
						if (fCond == 1)
						{
							if (!st.hasQuestItems(REPORT_OF_KRUMA))
							{
								if (!st.hasQuestItems(INGREDIENTS_OF_ANTIDOTE))
								{
									htmltext = "30673-01.htm";
									st.takeItems(ARCHITECTURE_OF_KRUMA, 1);
									st.giveItems(INGREDIENTS_OF_ANTIDOTE, 1);
								}
								else if (st.getQuestItemsCount(STINGER_WASP_NEEDLE) < 10 || st.getQuestItemsCount(MARSH_SPIDER_WEB) < 10 || st.getQuestItemsCount(BLOOD_OF_LEECH) < 10)
									htmltext = "30673-02.htm";
								else
									htmltext = "30673-03.htm";
							}
						}
						else if (fCond > 1)
							htmltext = "30673-05.htm";
						break;
				}
				break;
			
			case STATE_COMPLETED:
				htmltext = getAlreadyCompletedMsg();
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
		
		switch (npc.getNpcId())
		{
			case GIANT_MIST_LEECH:
				if (st.hasQuestItems(INGREDIENTS_OF_ANTIDOTE))
					st.dropItemsAlways(BLOOD_OF_LEECH, 1, 10);
				break;
			
			case STINGER_WASP:
				if (st.hasQuestItems(INGREDIENTS_OF_ANTIDOTE))
					st.dropItemsAlways(STINGER_WASP_NEEDLE, 1, 10);
				break;
			
			case MARSH_SPIDER:
				if (st.hasQuestItems(INGREDIENTS_OF_ANTIDOTE))
					st.dropItemsAlways(MARSH_SPIDER_WEB, 1, 10);
				break;
			
			case EVIL_EYE_LORD:
				if (st.hasQuestItems(PAINT_OF_KAMURU))
					st.dropItemsAlways(NECKLACE_OF_KAMURU, 1, 1);
				break;
		}
		
		return null;
	}
	
	public static void main(String[] args)
	{
		new Q231_TestOfTheMaestro();
	}
}