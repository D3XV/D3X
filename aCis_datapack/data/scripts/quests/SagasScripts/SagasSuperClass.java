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
package quests.SagasScripts;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.sf.l2j.Config;
import net.sf.l2j.gameserver.ai.CtrlIntention;
import net.sf.l2j.gameserver.instancemanager.QuestManager;
import net.sf.l2j.gameserver.model.L2Object;
import net.sf.l2j.gameserver.model.L2Party;
import net.sf.l2j.gameserver.model.L2Skill;
import net.sf.l2j.gameserver.model.L2World;
import net.sf.l2j.gameserver.model.actor.L2Attackable;
import net.sf.l2j.gameserver.model.actor.L2Character;
import net.sf.l2j.gameserver.model.actor.L2Npc;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.model.quest.Quest;
import net.sf.l2j.gameserver.model.quest.QuestState;
import net.sf.l2j.gameserver.network.serverpackets.MagicSkillUse;
import net.sf.l2j.util.Rnd;

public class SagasSuperClass extends Quest
{
	private static List<Quest> _scripts = new ArrayList<>();
	public int qnu;
	
	public int[] NPC = {};
	public int[] Items = {};
	public int[] Mob = {};
	
	public String qn = "SagasSuperClass";
	public int classid;
	public int prevclass;
	
	public int[] X = {};
	public int[] Y = {};
	public int[] Z = {};
	
	private final Map<L2Npc, Integer> _SpawnList = new HashMap<>();
	
	private static final String[] Text = new String[]
	{
		"PLAYERNAME! Pursued to here! However, I jumped out of the Banshouren boundaries! You look at the giant as the sign of power!",
		"... Oh ... good! So it was ... let's begin!",
		"I do not have the patience ..! I have been a giant force ...! Cough chatter ah ah ah!",
		"Paying homage to those who disrupt the orderly will be PLAYERNAME's death!",
		"Now, my soul freed from the shackles of the millennium, Halixia, to the back side I come ...",
		"Why do you interfere others' battles?",
		"This is a waste of time.. Say goodbye...!",
		"...That is the enemy",
		"...Goodness! PLAYERNAME you are still looking?",
		"PLAYERNAME ... Not just to whom the victory. Only personnel involved in the fighting are eligible to share in the victory.",
		"Your sword is not an ornament. Don't you think, PLAYERNAME?",
		"Goodness! I no longer sense a battle there now.",
		"let...",
		"Only engaged in the battle to bar their choice. Perhaps you should regret.",
		"The human nation was foolish to try and fight a giant's strength.",
		"Must...Retreat... Too...Strong.",
		"PLAYERNAME. Defeat...by...retaining...and...Mo...Hacker",
		"....! Fight...Defeat...It...Fight...Defeat...It..."
	};
	
	private static final int[] Archon_Hellisha_Norm =
	{
		18212,
		18214,
		18215,
		18216,
		18218
	};
	
	private static final int[] QuestClass =
	{
		0x05,
		0x14,
		0x15,
		0x02,
		0x03,
		0x2e,
		0x30,
		0x33,
		0x34,
		0x08,
		0x17,
		0x24,
		0x09,
		0x18,
		0x25,
		0x10,
		0x11,
		0x1e,
		0x0c,
		0x1b,
		0x28,
		0x0e,
		0x1c,
		0x29,
		0x0d,
		0x06,
		0x22,
		0x21,
		0x2b,
		0x37,
		0x39
	};
	
	public SagasSuperClass(int id, String name, String descr)
	{
		super(id, name, descr);
		qnu = id;
	}
	
	public void registerNPCs()
	{
		addStartNpc(NPC[0]);
		addAttackId(Mob[2], Mob[1]);
		addSkillSeeId(Mob[1]);
		addFirstTalkId(NPC[4]);
		
		for (int npc : NPC)
			addTalkId(npc);
		
		for (int mobid : Mob)
			addKillId(mobid);
		
		final int[] questItemIds = Items.clone();
		questItemIds[0] = 0;
		questItemIds[2] = 0; // remove Ice Crystal and Divine Stone of Wisdom
		setItemsIds(questItemIds);
		
		for (int Archon_Minion = 21646; Archon_Minion < 21652; Archon_Minion++)
			addKillId(Archon_Minion);
		
		for (int element : Archon_Hellisha_Norm)
			addKillId(element);
		
		for (int Guardian_Angel = 27214; Guardian_Angel < 27217; Guardian_Angel++)
			addKillId(Guardian_Angel);
	}
	
	private void cast(L2Npc npc, L2Character target, int skillId, int level)
	{
		target.broadcastPacket(new MagicSkillUse(target, target, skillId, level, 6000, 1));
		target.broadcastPacket(new MagicSkillUse(npc, npc, skillId, level, 6000, 1));
	}
	
	public void AddSpawn(QuestState st, L2Npc mob)
	{
		_SpawnList.put(mob, st.getPlayer().getObjectId());
	}
	
	public void DeleteSpawn(QuestState st, L2Npc npc)
	{
		if (_SpawnList.containsKey(npc))
		{
			_SpawnList.remove(npc);
			npc.deleteMe();
		}
	}
	
	public QuestState findRightState(L2Npc npc)
	{
		if (_SpawnList.containsKey(npc))
		{
			final L2PcInstance player = L2World.getInstance().getPlayer(_SpawnList.get(npc));
			if (player != null)
				return player.getQuestState(qn);
		}
		return null;
	}
	
	public void giveHallishaMark(QuestState st2)
	{
		if (st2.getInt("spawned") == 0)
		{
			if (st2.getQuestItemsCount(Items[3]) >= 700)
			{
				st2.takeItems(Items[3], 20);
				L2Npc Archon = addSpawn(Mob[1], st2.getPlayer(), false, 0, true);
				AddSpawn(st2, Archon);
				st2.set("spawned", "1");
				startQuestTimer("Archon Hellisha has despawned", 600000, Archon, st2.getPlayer(), false);
				
				// Attack player
				((L2Attackable) Archon).addDamageHate(st2.getPlayer(), 0, 99999);
				Archon.getAI().setIntention(CtrlIntention.ATTACK, st2.getPlayer(), null);
			}
			else
			{
				st2.giveItems(Items[3], 1);
				st2.playSound(QuestState.SOUND_ITEMGET);
			}
		}
	}
	
	public QuestState findQuest(L2PcInstance player)
	{
		QuestState st = player.getQuestState(qn);
		if (st != null && player.getClassId().getId() == QuestClass[qnu - 70])
			return st;
		
		return null;
	}
	
	public int getClassId(L2PcInstance player)
	{
		return classid;
	}
	
	public int getPrevClass(L2PcInstance player)
	{
		return prevclass;
	}
	
	@Override
	public String onAdvEvent(String event, L2Npc npc, L2PcInstance player)
	{
		QuestState st = player.getQuestState(qn);
		String htmltext = "";
		if (st != null)
		{
			if (event.equalsIgnoreCase("0-011.htm") || event.equalsIgnoreCase("0-012.htm") || event.equalsIgnoreCase("0-013.htm") || event.equalsIgnoreCase("0-014.htm") || event.equalsIgnoreCase("0-015.htm"))
				htmltext = event;
			else if (event.equalsIgnoreCase("accept"))
			{
				st.set("cond", "1");
				st.setState(STATE_STARTED);
				st.playSound(QuestState.SOUND_ACCEPT);
				st.giveItems(Items[10], 1);
				htmltext = "0-03.htm";
			}
			else if (event.equalsIgnoreCase("0-1"))
			{
				if (player.getLevel() < 76)
				{
					htmltext = "0-02.htm";
					if (st.isCreated())
						st.exitQuest(true);
				}
				else
					htmltext = "0-05.htm";
			}
			else if (event.equalsIgnoreCase("0-2"))
			{
				if (player.getLevel() >= 76)
				{
					st.exitQuest(false);
					st.set("cond", "0");
					htmltext = "0-07.htm";
					st.takeItems(Items[10], -1);
					st.rewardExpAndSp(2299404, 0);
					st.giveItems(57, 5000000);
					st.giveItems(6622, 1);
					
					int Class = getClassId(player);
					player.setClassId(Class);
					if (!player.isSubClassActive() && player.getBaseClass() == getPrevClass(player))
						player.setBaseClass(Class);
					
					player.broadcastUserInfo();
					cast(npc, player, 4339, 1);
				}
				else
				{
					st.takeItems(Items[10], -1);
					st.playSound(QuestState.SOUND_MIDDLE);
					st.set("cond", "20");
					htmltext = "0-08.htm";
				}
			}
			else if (event.equalsIgnoreCase("1-3"))
			{
				st.set("cond", "3");
				htmltext = "1-05.htm";
			}
			else if (event.equalsIgnoreCase("1-4"))
			{
				st.set("cond", "4");
				st.takeItems(Items[0], 1);
				if (Items[11] != 0)
					st.takeItems(Items[11], 1);
				st.giveItems(Items[1], 1);
				htmltext = "1-06.htm";
			}
			else if (event.equalsIgnoreCase("2-1"))
			{
				st.set("cond", "2");
				htmltext = "2-05.htm";
			}
			else if (event.equalsIgnoreCase("2-2"))
			{
				st.set("cond", "5");
				st.takeItems(Items[1], 1);
				st.giveItems(Items[4], 1);
				htmltext = "2-06.htm";
			}
			else if (event.equalsIgnoreCase("3-5"))
			{
				htmltext = "3-07.htm";
			}
			else if (event.equalsIgnoreCase("3-6"))
			{
				st.set("cond", "11");
				htmltext = "3-02.htm";
			}
			else if (event.equalsIgnoreCase("3-7"))
			{
				st.set("cond", "12");
				htmltext = "3-03.htm";
			}
			else if (event.equalsIgnoreCase("3-8"))
			{
				st.set("cond", "13");
				st.takeItems(Items[2], 1);
				st.giveItems(Items[7], 1);
				htmltext = "3-08.htm";
			}
			else if (event.equalsIgnoreCase("4-1"))
			{
				htmltext = "4-010.htm";
			}
			else if (event.equalsIgnoreCase("4-2"))
			{
				st.giveItems(Items[9], 1);
				st.set("cond", "18");
				st.playSound(QuestState.SOUND_MIDDLE);
				htmltext = "4-011.htm";
			}
			else if (event.equalsIgnoreCase("4-3"))
			{
				st.giveItems(Items[9], 1);
				st.set("cond", "18");
				npc.broadcastNpcSay(Text[13]);
				st.set("Quest0", "0");
				cancelQuestTimer("Mob_2 has despawned", npc, player);
				st.playSound(QuestState.SOUND_MIDDLE);
				DeleteSpawn(st, npc);
				return null;
			}
			else if (event.equalsIgnoreCase("5-1"))
			{
				st.set("cond", "6");
				st.takeItems(Items[4], 1);
				cast(npc, player, 4546, 1);
				st.playSound(QuestState.SOUND_MIDDLE);
				htmltext = "5-02.htm";
			}
			else if (event.equalsIgnoreCase("6-1"))
			{
				st.set("cond", "8");
				st.takeItems(Items[5], 1);
				cast(npc, player, 4546, 1);
				st.playSound(QuestState.SOUND_MIDDLE);
				htmltext = "6-03.htm";
			}
			else if (event.equalsIgnoreCase("7-1"))
			{
				if (st.getInt("spawned") == 1)
					htmltext = "7-03.htm";
				else if (st.getInt("spawned") == 0)
				{
					L2Npc Mob_1 = addSpawn(Mob[0], X[0], Y[0], Z[0], 0, false, 0, true);
					st.set("spawned", "1");
					startQuestTimer("Mob_1 Timer 1", 500, Mob_1, player, false);
					startQuestTimer("Mob_1 has despawned", 300000, Mob_1, player, false);
					AddSpawn(st, Mob_1);
					htmltext = "7-02.htm";
				}
				else
					htmltext = "7-04.htm";
			}
			else if (event.equalsIgnoreCase("7-2"))
			{
				st.set("cond", "10");
				st.takeItems(Items[6], 1);
				cast(npc, player, 4546, 1);
				st.playSound(QuestState.SOUND_MIDDLE);
				htmltext = "7-06.htm";
			}
			else if (event.equalsIgnoreCase("8-1"))
			{
				st.set("cond", "14");
				st.takeItems(Items[7], 1);
				cast(npc, player, 4546, 1);
				st.playSound(QuestState.SOUND_MIDDLE);
				htmltext = "8-02.htm";
			}
			else if (event.equalsIgnoreCase("9-1"))
			{
				st.set("cond", "17");
				st.takeItems(Items[8], 1);
				cast(npc, player, 4546, 1);
				st.playSound(QuestState.SOUND_MIDDLE);
				htmltext = "9-03.htm";
			}
			else if (event.equalsIgnoreCase("10-1"))
			{
				if (st.getInt("Quest0") == 0)
				{
					// Spawn NPC and mob fighting each other, and register them in _Spawnlist.
					L2Npc Mob_3 = addSpawn(Mob[2], X[1], Y[1], Z[1], 0, false, 0, true);
					L2Npc Mob_2 = addSpawn(NPC[4], X[2], Y[2], Z[2], 0, false, 0, true);
					AddSpawn(st, Mob_3);
					AddSpawn(st, Mob_2);
					
					st.set("Mob_2", String.valueOf(Mob_2.getObjectId()));
					
					st.set("Quest0", "1");
					st.set("Quest1", "45");
					
					startQuestTimer("Mob_3 Timer 1", 500, Mob_3, player, false);
					startQuestTimer("Mob_2 Timer 1", 500, Mob_2, player, false);
					
					startQuestTimer("Mob_3 has despawned", 59000, Mob_3, player, false);
					startQuestTimer("Mob_2 has despawned", 60000, Mob_2, player, false);
					
					htmltext = "10-02.htm";
				}
				else if (st.getInt("Quest1") == 45)
					htmltext = "10-03.htm";
				else
					htmltext = "10-04.htm";
			}
			else if (event.equalsIgnoreCase("10-2"))
			{
				st.set("cond", "19");
				st.takeItems(Items[9], 1);
				cast(npc, player, 4546, 1);
				st.playSound(QuestState.SOUND_MIDDLE);
				htmltext = "10-06.htm";
			}
			else if (event.equalsIgnoreCase("11-9"))
			{
				st.set("cond", "15");
				htmltext = "11-03.htm";
			}
			else if (event.equalsIgnoreCase("Mob_1 Timer 1"))
			{
				// Attack player
				((L2Attackable) npc).addDamageHate(st.getPlayer(), 0, 99999);
				npc.getAI().setIntention(CtrlIntention.ATTACK, st.getPlayer(), null);
				
				npc.broadcastNpcSay(Text[0].replace("PLAYERNAME", player.getName()));
				return null;
			}
			else if (event.equalsIgnoreCase("Mob_1 has despawned"))
			{
				npc.broadcastNpcSay(Text[1]);
				st.set("spawned", "0");
				DeleteSpawn(st, npc);
				return null;
			}
			else if (event.equalsIgnoreCase("Archon Hellisha has despawned"))
			{
				st.set("spawned", "0");
				DeleteSpawn(st, npc);
				return null;
			}
			else if (event.equalsIgnoreCase("Mob_3 Timer 1"))
			{
				// Search the NPC.
				L2Npc Mob_2 = (L2Npc) L2World.getInstance().findObject(st.getInt("Mob_2"));
				if (Mob_2 == null)
					return null;
				
				if (_SpawnList.containsKey(Mob_2) && _SpawnList.get(Mob_2) == player.getObjectId())
				{
					((L2Attackable) npc).addDamageHate(Mob_2, 0, 99999);
					npc.getAI().setIntention(CtrlIntention.ATTACK, Mob_2, null);
					
					npc.broadcastNpcSay(Text[14]);
				}
				return null;
			}
			else if (event.equalsIgnoreCase("Mob_3 has despawned"))
			{
				npc.broadcastNpcSay(Text[15]);
				st.set("Quest0", "2");
				DeleteSpawn(st, npc);
				return null;
			}
			else if (event.equalsIgnoreCase("Mob_2 Timer 1"))
			{
				npc.broadcastNpcSay(Text[7]);
				startQuestTimer("Mob_2 Timer 2", 1500, npc, player, false);
				if (st.getInt("Quest1") == 45)
					st.set("Quest1", "0");
				return null;
			}
			else if (event.equalsIgnoreCase("Mob_2 Timer 2"))
			{
				npc.broadcastNpcSay(Text[8].replace("PLAYERNAME", player.getName()));
				startQuestTimer("Mob_2 Timer 3", 10000, npc, player, false);
				return null;
			}
			else if (event.equalsIgnoreCase("Mob_2 Timer 3"))
			{
				if (st.getInt("Quest0") == 0)
				{
					startQuestTimer("Mob_2 Timer 3", 13000, npc, player, false);
					if (Rnd.get(2) == 0)
						npc.broadcastNpcSay(Text[9].replace("PLAYERNAME", player.getName()));
					else
						npc.broadcastNpcSay(Text[10].replace("PLAYERNAME", player.getName()));
				}
				return null;
			}
			else if (event.equalsIgnoreCase("Mob_2 has despawned"))
			{
				st.set("Quest1", String.valueOf(st.getInt("Quest1") + 1));
				if (st.getInt("Quest0") == 1 || st.getInt("Quest0") == 2 || st.getInt("Quest1") > 3)
				{
					st.set("Quest0", "0");
					if (st.getInt("Quest0") == 1)
						npc.broadcastNpcSay(Text[11]);
					else
						npc.broadcastNpcSay(Text[12]);
					DeleteSpawn(st, npc);
				}
				else
					startQuestTimer("Mob_2 has despawned", 1000, npc, player, false);
				return null;
			}
		}
		else
			return null;
		return htmltext;
	}
	
	@Override
	public String onTalk(L2Npc npc, L2PcInstance player)
	{
		String htmltext = getNoQuestMsg();
		QuestState st = player.getQuestState(qn);
		if (st != null)
		{
			int npcId = npc.getNpcId();
			int cond = st.getInt("cond");
			if (st.isCompleted() && npcId == NPC[0])
				htmltext = getAlreadyCompletedMsg();
			else if (player.getClassId().getId() == getPrevClass(player))
			{
				if (cond == 0)
				{
					if (npcId == NPC[0])
						htmltext = "0-01.htm";
				}
				else if (cond == 1)
				{
					if (npcId == NPC[0])
						htmltext = "0-04.htm";
					else if (npcId == NPC[2])
						htmltext = "2-01.htm";
				}
				else if (cond == 2)
				{
					if (npcId == NPC[2])
						htmltext = "2-02.htm";
					else if (npcId == NPC[1])
						htmltext = "1-01.htm";
				}
				else if (cond == 3)
				{
					if (npcId == NPC[1] && st.hasQuestItems(Items[0]))
					{
						htmltext = "1-02.htm";
						if (Items[11] == 0 || st.hasQuestItems(Items[11]))
							htmltext = "1-03.htm";
					}
				}
				else if (cond == 4)
				{
					if (npcId == NPC[1])
						htmltext = "1-04.htm";
					else if (npcId == NPC[2])
						htmltext = "2-03.htm";
				}
				else if (cond == 5)
				{
					if (npcId == NPC[2])
						htmltext = "2-04.htm";
					else if (npcId == NPC[5])
						htmltext = "5-01.htm";
				}
				else if (cond == 6)
				{
					if (npcId == NPC[5])
						htmltext = "5-03.htm";
					else if (npcId == NPC[6])
						htmltext = "6-01.htm";
				}
				else if (cond == 7)
				{
					if (npcId == NPC[6])
						htmltext = "6-02.htm";
				}
				else if (cond == 8)
				{
					if (npcId == NPC[6])
						htmltext = "6-04.htm";
					else if (npcId == NPC[7])
						htmltext = "7-01.htm";
				}
				else if (cond == 9)
				{
					if (npcId == NPC[7])
						htmltext = "7-05.htm";
				}
				else if (cond == 10)
				{
					if (npcId == NPC[7])
						htmltext = "7-07.htm";
					else if (npcId == NPC[3])
						htmltext = "3-01.htm";
				}
				else if (cond == 11 || cond == 12)
				{
					if (npcId == NPC[3])
					{
						if (st.hasQuestItems(Items[2]))
							htmltext = "3-05.htm";
						else
							htmltext = "3-04.htm";
					}
				}
				else if (cond == 13)
				{
					if (npcId == NPC[3])
						htmltext = "3-06.htm";
					else if (npcId == NPC[8])
						htmltext = "8-01.htm";
				}
				else if (cond == 14)
				{
					if (npcId == NPC[8])
						htmltext = "8-03.htm";
					else if (npcId == NPC[11])
						htmltext = "11-01.htm";
				}
				else if (cond == 15)
				{
					if (npcId == NPC[11])
						htmltext = "11-02.htm";
					else if (npcId == NPC[9])
						htmltext = "9-01.htm";
				}
				else if (cond == 16)
				{
					if (npcId == NPC[9])
						htmltext = "9-02.htm";
				}
				else if (cond == 17)
				{
					if (npcId == NPC[9])
						htmltext = "9-04.htm";
					else if (npcId == NPC[10])
						htmltext = "10-01.htm";
				}
				else if (cond == 18)
				{
					if (npcId == NPC[10])
						htmltext = "10-05.htm";
				}
				else if (cond == 19)
				{
					if (npcId == NPC[10])
						htmltext = "10-07.htm";
					else if (npcId == NPC[0])
						htmltext = "0-06.htm";
				}
				else if (cond == 20)
				{
					if (npcId == NPC[0])
					{
						if (player.getLevel() >= 76)
						{
							htmltext = "0-09.htm";
							st.exitQuest(false);
							st.set("cond", "0");
							st.rewardExpAndSp(2299404, 0);
							st.giveItems(57, 5000000);
							st.giveItems(6622, 1);
							int Class = getClassId(player);
							int prevClass = getPrevClass(player);
							player.setClassId(Class);
							if (!player.isSubClassActive() && player.getBaseClass() == prevClass)
								player.setBaseClass(Class);
							player.broadcastUserInfo();
							cast(npc, player, 4339, 1);
						}
						else
							htmltext = "0-010.htm";
					}
				}
			}
		}
		return htmltext;
	}
	
	@Override
	public String onFirstTalk(L2Npc npc, L2PcInstance player)
	{
		String htmltext = "";
		QuestState st = player.getQuestState(qn);
		int npcId = npc.getNpcId();
		if (st != null)
		{
			int cond = st.getInt("cond");
			if (npcId == NPC[4])
			{
				if (cond == 17)
				{
					QuestState st2 = findRightState(npc);
					if (st2 != null)
					{
						player.setLastQuestNpcObject(npc.getObjectId());
						if (st == st2)
						{
							if (st.getInt("Tab") == 1)
							{
								if (st.getInt("Quest0") == 0)
									htmltext = "4-04.htm";
								else if (st.getInt("Quest0") == 1)
									htmltext = "4-06.htm";
							}
							else
							{
								if (st.getInt("Quest0") == 0)
									htmltext = "4-01.htm";
								else if (st.getInt("Quest0") == 1)
									htmltext = "4-03.htm";
							}
						}
						else
						{
							if (st.getInt("Tab") == 1)
							{
								if (st.getInt("Quest0") == 0)
									htmltext = "4-05.htm";
								else if (st.getInt("Quest0") == 1)
									htmltext = "4-07.htm";
							}
							else
							{
								if (st.getInt("Quest0") == 0)
									htmltext = "4-02.htm";
							}
						}
					}
				}
				else if (cond == 18)
					htmltext = "4-08.htm";
			}
		}
		if (htmltext == "")
			npc.showChatWindow(player);
		return htmltext;
	}
	
	@Override
	public String onAttack(L2Npc npc, L2PcInstance player, int damage, boolean isPet)
	{
		QuestState st2 = findRightState(npc);
		if (st2 == null)
			return super.onAttack(npc, player, damage, isPet);
		
		int cond = st2.getInt("cond");
		QuestState st = player.getQuestState(qn);
		int npcId = npc.getNpcId();
		if (npcId == Mob[2] && st == st2 && cond == 17)
		{
			st.set("Quest0", String.valueOf(st.getInt("Quest0") + 1));
			if (st.getInt("Quest0") == 1)
				npc.broadcastNpcSay(Text[16].replace("PLAYERNAME", player.getName()));
			if (st.getInt("Quest0") > 15)
			{
				st.set("Quest0", "1");
				npc.broadcastNpcSay(Text[17]);
				cancelQuestTimer("Mob_3 has despawned", npc, st2.getPlayer());
				st.set("Tab", "1");
				DeleteSpawn(st, npc);
			}
		}
		else if (npcId == Mob[1] && cond == 15)
		{
			if (st != st2 || (st == st2 && player.isInParty()))
			{
				npc.broadcastNpcSay(Text[5]);
				cancelQuestTimer("Archon Hellisha has despawned", npc, st2.getPlayer());
				st2.set("spawned", "0");
				DeleteSpawn(st2, npc);
			}
		}
		return super.onAttack(npc, player, damage, isPet);
	}
	
	@Override
	public String onSkillSee(L2Npc npc, L2PcInstance player, L2Skill skill, L2Object[] targets, boolean isPet)
	{
		if (_SpawnList.containsKey(npc) && _SpawnList.get(npc) != player.getObjectId())
		{
			L2PcInstance quest_player = L2World.getInstance().getPlayer(_SpawnList.get(npc));
			if (quest_player == null)
				return null;
			
			for (L2Object obj : targets)
			{
				if (obj == quest_player || obj == npc)
				{
					QuestState st2 = findRightState(npc);
					if (st2 == null)
						return null;
					
					npc.broadcastNpcSay(Text[5]);
					cancelQuestTimer("Archon Hellisha has despawned", npc, st2.getPlayer());
					st2.set("spawned", "0");
					DeleteSpawn(st2, npc);
				}
			}
		}
		return super.onSkillSee(npc, player, skill, targets, isPet);
	}
	
	@Override
	public String onKill(L2Npc npc, L2PcInstance player, boolean isPet)
	{
		int npcId = npc.getNpcId();
		
		for (int Archon_Minion = 21646; Archon_Minion < 21652; Archon_Minion++)
		{
			if (npcId == Archon_Minion)
			{
				L2Party party = player.getParty();
				if (party != null)
				{
					List<QuestState> PartyQuestMembers = new ArrayList<>();
					for (L2PcInstance player1 : party.getPartyMembers())
					{
						QuestState st1 = findQuest(player1);
						if (st1 != null && player1.isInsideRadius(player, Config.ALT_PARTY_RANGE2, false, false))
						{
							if (st1.getInt("cond") == 15)
								PartyQuestMembers.add(st1);
						}
					}
					if (PartyQuestMembers.size() > 0)
					{
						QuestState st2 = PartyQuestMembers.get(Rnd.get(PartyQuestMembers.size()));
						giveHallishaMark(st2);
					}
				}
				else
				{
					QuestState st1 = findQuest(player);
					if (st1 != null)
					{
						if (st1.getInt("cond") == 15)
							giveHallishaMark(st1);
					}
				}
				return super.onKill(npc, player, isPet);
			}
		}
		
		for (int element : Archon_Hellisha_Norm)
		{
			if (npcId == element)
			{
				QuestState st1 = findQuest(player);
				if (st1 != null)
				{
					if (st1.getInt("cond") == 15)
					{
						npc.broadcastNpcSay(Text[4]);
						st1.giveItems(Items[8], 1);
						st1.takeItems(Items[3], -1);
						st1.set("cond", "16");
						st1.playSound(QuestState.SOUND_MIDDLE);
					}
				}
				return super.onKill(npc, player, isPet);
			}
		}
		
		for (int Guardian_Angel = 27214; Guardian_Angel < 27217; Guardian_Angel++)
		{
			if (npcId == Guardian_Angel)
			{
				QuestState st1 = findQuest(player);
				if (st1 != null)
				{
					if (st1.getInt("cond") == 6)
					{
						if (st1.getInt("kills") < 9)
							st1.set("kills", String.valueOf(st1.getInt("kills") + 1));
						else
						{
							st1.playSound(QuestState.SOUND_MIDDLE);
							st1.giveItems(Items[5], 1);
							st1.set("cond", "7");
						}
					}
				}
				return super.onKill(npc, player, isPet);
			}
		}
		
		QuestState st = player.getQuestState(qn);
		if (st != null && npcId != Mob[2])
		{
			QuestState st2 = findRightState(npc);
			if (st2 == null)
				return super.onKill(npc, player, isPet);
			
			int cond = st.getInt("cond");
			if (npcId == Mob[0] && cond == 8)
			{
				if (!player.isInParty())
				{
					if (st == st2)
					{
						npc.broadcastNpcSay(Text[12]);
						st.giveItems(Items[6], 1);
						st.set("cond", "9");
						st.playSound(QuestState.SOUND_MIDDLE);
					}
				}
				cancelQuestTimer("Mob_1 has despawned", npc, st2.getPlayer());
				st2.set("spawned", "0");
				DeleteSpawn(st2, npc);
			}
			else if (npcId == Mob[1] && cond == 15)
			{
				if (!player.isInParty())
				{
					if (st == st2)
					{
						npc.broadcastNpcSay(Text[4]);
						st.giveItems(Items[8], 1);
						st.takeItems(Items[3], -1);
						st.set("cond", "16");
						st.playSound(QuestState.SOUND_MIDDLE);
					}
					else
						npc.broadcastNpcSay(Text[5]);
				}
				cancelQuestTimer("Archon Hellisha has despawned", npc, st2.getPlayer());
				st2.set("spawned", "0");
				DeleteSpawn(st2, npc);
			}
		}
		else
		{
			if (npcId == Mob[0])
			{
				st = findRightState(npc);
				if (st != null)
				{
					cancelQuestTimer("Mob_1 has despawned", npc, st.getPlayer());
					st.set("spawned", "0");
					DeleteSpawn(st, npc);
				}
			}
			else if (npcId == Mob[1])
			{
				st = findRightState(npc);
				if (st != null)
				{
					cancelQuestTimer("Archon Hellisha has despawned", npc, st.getPlayer());
					st.set("spawned", "0");
					DeleteSpawn(st, npc);
				}
			}
		}
		return super.onKill(npc, player, isPet);
	}
	
	@Override
	public boolean unload()
	{
		// if sub classes aren't loaded, just unload superclass
		if (_scripts.isEmpty())
			return super.unload();
		
		// unload all subclasses
		for (int index = 0; index < _scripts.size(); index++)
		{
			if (_scripts.get(index) == null)
				continue;
			
			QuestManager.getInstance().removeQuest(_scripts.get(index));
		}
		_scripts.clear();
		
		// now unload superclass
		return super.unload();
	}
	
	public static void main(String[] args)
	{
		// initialize superclass
		new SagasSuperClass(-1, "SagasSuperClass", "Saga's SuperClass");
		
		// initialize subclasses
		_scripts.add(new SagaOfEvasSaint());
		_scripts.add(new SagaOfEvasTemplar());
		_scripts.add(new SagaOfTheAdventurer());
		_scripts.add(new SagaOfTheArcanaLord());
		_scripts.add(new SagaOfTheArchmage());
		_scripts.add(new SagaOfTheCardinal());
		_scripts.add(new SagaOfTheDominator());
		_scripts.add(new SagaOfTheDoomcryer());
		_scripts.add(new SagaOfTheDreadnought());
		_scripts.add(new SagaOfTheDuelist());
		_scripts.add(new SagaOfTheElementalMaster());
		_scripts.add(new SagaOfTheFortuneSeeker());
		_scripts.add(new SagaOfTheGhostHunter());
		_scripts.add(new SagaOfTheGhostSentinel());
		_scripts.add(new SagaOfTheGrandKhavatari());
		_scripts.add(new SagaOfTheHellKnight());
		_scripts.add(new SagaOfTheHierophant());
		_scripts.add(new SagaOfTheMaestro());
		_scripts.add(new SagaOfTheMoonlightSentinel());
		_scripts.add(new SagaOfTheMysticMuse());
		_scripts.add(new SagaOfThePhoenixKnight());
		_scripts.add(new SagaOfTheSagittarius());
		_scripts.add(new SagaOfTheShillienSaint());
		_scripts.add(new SagaOfTheShillienTemplar());
		_scripts.add(new SagaOfTheSoultaker());
		_scripts.add(new SagaOfTheSpectralDancer());
		_scripts.add(new SagaOfTheSpectralMaster());
		_scripts.add(new SagaOfTheStormScreamer());
		_scripts.add(new SagaOfTheSwordMuse());
		_scripts.add(new SagaOfTheTitan());
		_scripts.add(new SagaOfTheWindRider());
	}
}