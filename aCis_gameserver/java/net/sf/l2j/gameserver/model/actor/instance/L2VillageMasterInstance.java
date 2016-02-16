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
package net.sf.l2j.gameserver.model.actor.instance;

import java.util.Iterator;
import java.util.Set;

import net.sf.l2j.Config;
import net.sf.l2j.gameserver.datatables.CharTemplateTable;
import net.sf.l2j.gameserver.datatables.ClanTable;
import net.sf.l2j.gameserver.datatables.SkillTable;
import net.sf.l2j.gameserver.datatables.SkillTreeTable;
import net.sf.l2j.gameserver.instancemanager.SiegeManager;
import net.sf.l2j.gameserver.model.L2Clan;
import net.sf.l2j.gameserver.model.L2Clan.SubPledge;
import net.sf.l2j.gameserver.model.L2ClanMember;
import net.sf.l2j.gameserver.model.L2PledgeSkillLearn;
import net.sf.l2j.gameserver.model.L2Skill;
import net.sf.l2j.gameserver.model.actor.template.NpcTemplate;
import net.sf.l2j.gameserver.model.base.ClassId;
import net.sf.l2j.gameserver.model.base.PlayerClass;
import net.sf.l2j.gameserver.model.base.SubClass;
import net.sf.l2j.gameserver.model.entity.Siege;
import net.sf.l2j.gameserver.model.olympiad.OlympiadManager;
import net.sf.l2j.gameserver.model.quest.QuestState;
import net.sf.l2j.gameserver.network.SystemMessageId;
import net.sf.l2j.gameserver.network.serverpackets.AcquireSkillList;
import net.sf.l2j.gameserver.network.serverpackets.ActionFailed;
import net.sf.l2j.gameserver.network.serverpackets.MagicSkillUse;
import net.sf.l2j.gameserver.network.serverpackets.NpcHtmlMessage;
import net.sf.l2j.gameserver.network.serverpackets.PledgeShowMemberListAll;
import net.sf.l2j.gameserver.network.serverpackets.SystemMessage;
import net.sf.l2j.gameserver.network.serverpackets.UserInfo;
import net.sf.l2j.gameserver.util.Util;
import net.sf.l2j.util.StringUtil;

/**
 * The generic villagemaster. Some childs instances depends of it for race/classe restriction.
 */
public class L2VillageMasterInstance extends L2NpcInstance
{
	public L2VillageMasterInstance(int objectId, NpcTemplate template)
	{
		super(objectId, template);
	}
	
	@Override
	public String getHtmlPath(int npcId, int val)
	{
		String filename = "";
		
		if (val == 0)
			filename = "" + npcId;
		else
			filename = npcId + "-" + val;
		
		return "data/html/villagemaster/" + filename + ".htm";
	}
	
	@Override
	public void onBypassFeedback(L2PcInstance player, String command)
	{
		String[] commandStr = command.split(" ");
		String actualCommand = commandStr[0];
		
		String cmdParams = "";
		String cmdParams2 = "";
		
		if (commandStr.length >= 2)
			cmdParams = commandStr[1];
		if (commandStr.length >= 3)
			cmdParams2 = commandStr[2];
		
		if (actualCommand.equalsIgnoreCase("create_clan"))
		{
			if (cmdParams.isEmpty())
				return;
			
			ClanTable.getInstance().createClan(player, cmdParams);
		}
		else if (actualCommand.equalsIgnoreCase("create_academy"))
		{
			if (cmdParams.isEmpty())
				return;
			
			createSubPledge(player, cmdParams, null, L2Clan.SUBUNIT_ACADEMY, 5);
		}
		else if (actualCommand.equalsIgnoreCase("rename_pledge"))
		{
			if (cmdParams.isEmpty() || cmdParams2.isEmpty())
				return;
			
			renameSubPledge(player, Integer.valueOf(cmdParams), cmdParams2);
		}
		else if (actualCommand.equalsIgnoreCase("create_royal"))
		{
			if (cmdParams.isEmpty())
				return;
			
			createSubPledge(player, cmdParams, cmdParams2, L2Clan.SUBUNIT_ROYAL1, 6);
		}
		else if (actualCommand.equalsIgnoreCase("create_knight"))
		{
			if (cmdParams.isEmpty())
				return;
			
			createSubPledge(player, cmdParams, cmdParams2, L2Clan.SUBUNIT_KNIGHT1, 7);
		}
		else if (actualCommand.equalsIgnoreCase("assign_subpl_leader"))
		{
			if (cmdParams.isEmpty())
				return;
			
			assignSubPledgeLeader(player, cmdParams, cmdParams2);
		}
		else if (actualCommand.equalsIgnoreCase("create_ally"))
		{
			if (cmdParams.isEmpty())
				return;
			
			if (player.getClan() == null)
				player.sendPacket(SystemMessageId.ONLY_CLAN_LEADER_CREATE_ALLIANCE);
			else
				player.getClan().createAlly(player, cmdParams);
		}
		else if (actualCommand.equalsIgnoreCase("dissolve_ally"))
		{
			player.getClan().dissolveAlly(player);
		}
		else if (actualCommand.equalsIgnoreCase("dissolve_clan"))
		{
			dissolveClan(player);
		}
		else if (actualCommand.equalsIgnoreCase("change_clan_leader"))
		{
			if (cmdParams.isEmpty())
				return;
			
			changeClanLeader(player, cmdParams);
		}
		else if (actualCommand.equalsIgnoreCase("recover_clan"))
		{
			recoverClan(player);
		}
		else if (actualCommand.equalsIgnoreCase("increase_clan_level"))
		{
			if (player.getClan().levelUpClan(player))
				player.broadcastPacket(new MagicSkillUse(player, player, 5103, 1, 0, 0));
		}
		else if (actualCommand.equalsIgnoreCase("learn_clan_skills"))
		{
			showPledgeSkillList(player);
		}
		else if (command.startsWith("Subclass"))
		{
			// Subclasses may not be changed while a skill is in use.
			if (player.isCastingNow() || player.isAllSkillsDisabled())
			{
				player.sendPacket(SystemMessageId.SUBCLASS_NO_CHANGE_OR_CREATE_WHILE_SKILL_IN_USE);
				return;
			}
			
			// Affecting subclasses (add/del/change) if registered in Olympiads makes you ineligible to compete.
			if (OlympiadManager.getInstance().isRegisteredInComp(player))
				OlympiadManager.getInstance().unRegisterNoble(player);
			
			NpcHtmlMessage html = new NpcHtmlMessage(getObjectId());
			
			int cmdChoice = 0;
			int paramOne = 0;
			int paramTwo = 0;
			
			try
			{
				cmdChoice = Integer.parseInt(command.substring(9, 10).trim());
				
				int endIndex = command.indexOf(' ', 11);
				if (endIndex == -1)
					endIndex = command.length();
				
				paramOne = Integer.parseInt(command.substring(11, endIndex).trim());
				if (command.length() > endIndex)
					paramTwo = Integer.parseInt(command.substring(endIndex).trim());
			}
			catch (Exception NumberFormatException)
			{
			}
			
			switch (cmdChoice)
			{
				case 0: // Subclass change menu
					html.setFile("data/html/villagemaster/SubClass.htm");
					break;
				case 1: // Add Subclass - Initial
					// Subclasses may not be added while a summon is active.
					if (player.getPet() != null)
					{
						player.sendPacket(SystemMessageId.CANT_SUBCLASS_WITH_SUMMONED_SERVITOR);
						return;
					}
					
					// Subclasses may not be added while you are over your weight limit.
					if (!player.isInventoryUnder80(true) || player.getWeightPenalty() > 0)
					{
						player.sendPacket(SystemMessageId.NOT_SUBCLASS_WHILE_OVERWEIGHT);
						return;
					}
					
					// Avoid giving player an option to add a new sub class, if they have three already.
					if (player.getTotalSubClasses() >= 3)
					{
						html.setFile("data/html/villagemaster/SubClass_Fail.htm");
						break;
					}
					
					html.setFile("data/html/villagemaster/SubClass_Add.htm");
					final StringBuilder content1 = StringUtil.startAppend(200);
					Set<PlayerClass> subsAvailable = getAvailableSubClasses(player);
					
					if (subsAvailable != null && !subsAvailable.isEmpty())
					{
						for (PlayerClass subClass : subsAvailable)
							StringUtil.append(content1, "<a action=\"bypass -h npc_%objectId%_Subclass 4 ", String.valueOf(subClass.ordinal()), "\" msg=\"1268;", formatClassForDisplay(subClass), "\">", formatClassForDisplay(subClass), "</a><br>");
					}
					else
					{
						player.sendMessage("There are no sub classes available at this time.");
						return;
					}
					html.replace("%list%", content1.toString());
					break;
				case 2: // Change Class - Initial
					// Subclasses may not be changed while a summon is active.
					if (player.getPet() != null)
					{
						player.sendPacket(SystemMessageId.CANT_SUBCLASS_WITH_SUMMONED_SERVITOR);
						return;
					}
					
					// Subclasses may not be changed while a you are over your weight limit.
					if (!player.isInventoryUnder80(true) || player.getWeightPenalty() > 0)
					{
						player.sendPacket(SystemMessageId.NOT_SUBCLASS_WHILE_OVERWEIGHT);
						return;
					}
					
					if (player.getSubClasses().isEmpty())
						html.setFile("data/html/villagemaster/SubClass_ChangeNo.htm");
					else
					{
						final StringBuilder content2 = StringUtil.startAppend(200);
						
						if (checkVillageMaster(player.getBaseClass()))
							StringUtil.append(content2, "<a action=\"bypass -h npc_%objectId%_Subclass 5 0\">", CharTemplateTable.getInstance().getClassNameById(player.getBaseClass()), "</a><br>");
						
						for (Iterator<SubClass> subList = iterSubClasses(player); subList.hasNext();)
						{
							SubClass subClass = subList.next();
							if (checkVillageMaster(subClass.getClassDefinition()))
								StringUtil.append(content2, "<a action=\"bypass -h npc_%objectId%_Subclass 5 ", String.valueOf(subClass.getClassIndex()), "\">", formatClassForDisplay(subClass.getClassDefinition()), "</a><br>");
						}
						
						if (content2.length() > 0)
						{
							html.setFile("data/html/villagemaster/SubClass_Change.htm");
							html.replace("%list%", content2.toString());
						}
						else
							html.setFile("data/html/villagemaster/SubClass_ChangeNotFound.htm");
					}
					break;
				case 3: // Change/Cancel Subclass - Initial
					if (player.getSubClasses() == null || player.getSubClasses().isEmpty())
					{
						html.setFile("data/html/villagemaster/SubClass_ModifyEmpty.htm");
						break;
					}
					
					// custom value
					if (player.getTotalSubClasses() > 3)
					{
						html.setFile("data/html/villagemaster/SubClass_ModifyCustom.htm");
						final StringBuilder content3 = StringUtil.startAppend(200);
						int classIndex = 1;
						
						for (Iterator<SubClass> subList = iterSubClasses(player); subList.hasNext();)
						{
							SubClass subClass = subList.next();
							StringUtil.append(content3, "Sub-class ", String.valueOf(classIndex++), "<br>", "<a action=\"bypass -h npc_%objectId%_Subclass 6 ", String.valueOf(subClass.getClassIndex()), "\">", CharTemplateTable.getInstance().getClassNameById(subClass.getClassId()), "</a><br>");
						}
						html.replace("%list%", content3.toString());
					}
					else
					{
						// retail html contain only 3 subclasses
						html.setFile("data/html/villagemaster/SubClass_Modify.htm");
						if (player.getSubClasses().containsKey(1))
							html.replace("%sub1%", CharTemplateTable.getInstance().getClassNameById(player.getSubClasses().get(1).getClassId()));
						else
							html.replace("<a action=\"bypass -h npc_%objectId%_Subclass 6 1\">%sub1%</a><br>", "");
						
						if (player.getSubClasses().containsKey(2))
							html.replace("%sub2%", CharTemplateTable.getInstance().getClassNameById(player.getSubClasses().get(2).getClassId()));
						else
							html.replace("<a action=\"bypass -h npc_%objectId%_Subclass 6 2\">%sub2%</a><br>", "");
						
						if (player.getSubClasses().containsKey(3))
							html.replace("%sub3%", CharTemplateTable.getInstance().getClassNameById(player.getSubClasses().get(3).getClassId()));
						else
							html.replace("<a action=\"bypass -h npc_%objectId%_Subclass 6 3\">%sub3%</a><br>", "");
					}
					break;
				case 4: // Add Subclass - Action (Subclass 4 x[x])
					if (!player.getFloodProtectors().getSubclass().tryPerformAction("addSubclass"))
						return;
					
					boolean allowAddition = true;
					
					if (player.getTotalSubClasses() >= 3)
						allowAddition = false;
					
					if (player.getLevel() < 75)
						allowAddition = false;
					
					if (allowAddition)
					{
						if (!player.getSubClasses().isEmpty())
						{
							for (Iterator<SubClass> subList = iterSubClasses(player); subList.hasNext();)
							{
								SubClass subClass = subList.next();
								
								if (subClass.getLevel() < 75)
								{
									allowAddition = false;
									break;
								}
							}
						}
					}
					
					/*
					 * If quest checking is enabled, verify if the character has completed the Mimir's Elixir (Path to Subclass) and Fate's Whisper (A Grade Weapon) quests by checking for instances of their unique reward items. If they both exist, remove both unique items and continue with adding
					 * the sub-class.
					 */
					if (allowAddition && !Config.ALT_GAME_SUBCLASS_WITHOUT_QUESTS)
						allowAddition = checkQuests(player);
					
					if (allowAddition && isValidNewSubClass(player, paramOne))
					{
						if (!player.addSubClass(paramOne, player.getTotalSubClasses() + 1))
							return;
						
						player.setActiveClass(player.getTotalSubClasses());
						
						html.setFile("data/html/villagemaster/SubClass_AddOk.htm");
						player.sendPacket(SystemMessageId.ADD_NEW_SUBCLASS); // Subclass added.
					}
					else
						html.setFile("data/html/villagemaster/SubClass_Fail.htm");
					break;
				case 5: // Change Class - Action
					if (!player.getFloodProtectors().getSubclass().tryPerformAction("changeSubclass"))
						return;
					
					if (player.getClassIndex() == paramOne)
					{
						html.setFile("data/html/villagemaster/SubClass_Current.htm");
						break;
					}
					
					if (paramOne == 0)
					{
						if (!checkVillageMaster(player.getBaseClass()))
							return;
					}
					else
					{
						try
						{
							if (!checkVillageMaster(player.getSubClasses().get(paramOne).getClassDefinition()))
								return;
						}
						catch (NullPointerException e)
						{
							return;
						}
					}
					
					player.setActiveClass(paramOne);
					
					player.sendPacket(SystemMessageId.SUBCLASS_TRANSFER_COMPLETED); // Transfer completed.
					return;
				case 6: // Change/Cancel Subclass - Choice
					// validity check
					if (paramOne < 1 || paramOne > 3)
						return;
					
					subsAvailable = getAvailableSubClasses(player);
					
					// another validity check
					if (subsAvailable == null || subsAvailable.isEmpty())
					{
						player.sendMessage("There are no sub classes available at this time.");
						return;
					}
					
					final StringBuilder content6 = StringUtil.startAppend(200);
					for (PlayerClass subClass : subsAvailable)
						StringUtil.append(content6, "<a action=\"bypass -h npc_%objectId%_Subclass 7 ", String.valueOf(paramOne), " ", String.valueOf(subClass.ordinal()), "\" msg=\"1445;", "\">", formatClassForDisplay(subClass), "</a><br>");
					
					switch (paramOne)
					{
						case 1:
							html.setFile("data/html/villagemaster/SubClass_ModifyChoice1.htm");
							break;
						case 2:
							html.setFile("data/html/villagemaster/SubClass_ModifyChoice2.htm");
							break;
						case 3:
							html.setFile("data/html/villagemaster/SubClass_ModifyChoice3.htm");
							break;
						default:
							html.setFile("data/html/villagemaster/SubClass_ModifyChoice.htm");
					}
					html.replace("%list%", content6.toString());
					break;
				case 7: // Change Subclass - Action
					if (!player.getFloodProtectors().getSubclass().tryPerformAction("changeSubclass"))
						return;
					
					if (!isValidNewSubClass(player, paramTwo))
						return;
					
					if (player.modifySubClass(paramOne, paramTwo))
					{
						player.abortCast();
						player.stopAllEffectsExceptThoseThatLastThroughDeath(); // all effects from old subclass stopped!
						player.stopCubics();
						player.setActiveClass(paramOne);
						
						html.setFile("data/html/villagemaster/SubClass_ModifyOk.htm");
						player.sendPacket(SystemMessageId.ADD_NEW_SUBCLASS); // Subclass added.
					}
					else
					{
						player.setActiveClass(0); // Also updates _classIndex plus switching _classid to baseclass.
						
						player.sendMessage("The sub class could not be added, you have been reverted to your base class.");
						return;
					}
					break;
			}
			
			html.replace("%objectId%", getObjectId());
			player.sendPacket(html);
		}
		else
		{
			// this class dont know any other commands, let forward the command to the parent class
			super.onBypassFeedback(player, command);
		}
	}
	
	protected boolean checkQuests(L2PcInstance player)
	{
		// Noble players can add subbclasses without quests
		if (player.isNoble())
			return true;
		
		QuestState qs = player.getQuestState("Q234_FatesWhisper");
		if (qs == null || !qs.isCompleted())
			return false;
		
		qs = player.getQuestState("Q235_MimirsElixir");
		if (qs == null || !qs.isCompleted())
			return false;
		
		return true;
	}
	
	private final Set<PlayerClass> getAvailableSubClasses(L2PcInstance player)
	{
		// get player base class
		final int currentBaseId = player.getBaseClass();
		final ClassId baseCID = ClassId.values()[currentBaseId];
		
		// we need 2nd occupation ID
		final int baseClassId;
		if (baseCID.level() > 2)
			baseClassId = baseCID.getParent().ordinal();
		else
			baseClassId = currentBaseId;
		
		/**
		 * If the race of your main class is Elf or Dark Elf, you may not select each class as a subclass to the other class, and you may not select Overlord and Warsmith class as a subclass. You may not select a similar class as the subclass. The occupations classified as similar classes are as
		 * follows: Treasure Hunter, Plainswalker and Abyss Walker Hawkeye, Silver Ranger and Phantom Ranger Paladin, Dark Avenger, Temple Knight and Shillien Knight Warlocks, Elemental Summoner and Phantom Summoner Elder and Shillien Elder Swordsinger and Bladedancer Sorcerer, Spellsinger and
		 * Spellhowler
		 */
		Set<PlayerClass> availSubs = PlayerClass.values()[baseClassId].getAvailableSubclasses(player);
		
		if (availSubs != null && !availSubs.isEmpty())
		{
			for (Iterator<PlayerClass> availSub = availSubs.iterator(); availSub.hasNext();)
			{
				PlayerClass pclass = availSub.next();
				
				// check for the village master
				if (!checkVillageMaster(pclass))
				{
					availSub.remove();
					continue;
				}
				
				// scan for already used subclasses
				int availClassId = pclass.ordinal();
				ClassId cid = ClassId.values()[availClassId];
				for (Iterator<SubClass> subList = iterSubClasses(player); subList.hasNext();)
				{
					SubClass prevSubClass = subList.next();
					ClassId subClassId = ClassId.values()[prevSubClass.getClassId()];
					
					if (subClassId.equalsOrChildOf(cid))
					{
						availSub.remove();
						break;
					}
				}
			}
		}
		
		return availSubs;
	}
	
	/*
	 * Check new subclass classId for validity (villagemaster race/type is not contains in previous subclasses, but in allowed subclasses) Base class not added into allowed subclasses.
	 */
	private final boolean isValidNewSubClass(L2PcInstance player, int classId)
	{
		if (!checkVillageMaster(classId))
			return false;
		
		final ClassId cid = ClassId.values()[classId];
		for (Iterator<SubClass> subList = iterSubClasses(player); subList.hasNext();)
		{
			SubClass sub = subList.next();
			ClassId subClassId = ClassId.values()[sub.getClassId()];
			
			if (subClassId.equalsOrChildOf(cid))
				return false;
		}
		
		// get player base class
		final int currentBaseId = player.getBaseClass();
		final ClassId baseCID = ClassId.values()[currentBaseId];
		
		// we need 2nd occupation ID
		final int baseClassId;
		if (baseCID.level() > 2)
			baseClassId = baseCID.getParent().ordinal();
		else
			baseClassId = currentBaseId;
		
		Set<PlayerClass> availSubs = PlayerClass.values()[baseClassId].getAvailableSubclasses(player);
		if (availSubs == null || availSubs.isEmpty())
			return false;
		
		boolean found = false;
		for (PlayerClass pclass : availSubs)
		{
			if (pclass.ordinal() == classId)
			{
				found = true;
				break;
			}
		}
		
		return found;
	}
	
	protected boolean checkVillageMasterRace(PlayerClass pclass)
	{
		return true;
	}
	
	protected boolean checkVillageMasterTeachType(PlayerClass pclass)
	{
		return true;
	}
	
	/*
	 * Returns true if this classId allowed for master
	 */
	public final boolean checkVillageMaster(int classId)
	{
		return checkVillageMaster(PlayerClass.values()[classId]);
	}
	
	/*
	 * Returns true if this PlayerClass is allowed for master
	 */
	public final boolean checkVillageMaster(PlayerClass pclass)
	{
		return checkVillageMasterRace(pclass) && checkVillageMasterTeachType(pclass);
	}
	
	private static final String formatClassForDisplay(PlayerClass className)
	{
		String classNameStr = className.toString();
		char[] charArray = classNameStr.toCharArray();
		
		for (int i = 1; i < charArray.length; i++)
		{
			if (Character.isUpperCase(charArray[i]))
				classNameStr = classNameStr.substring(0, i) + " " + classNameStr.substring(i);
		}
		
		return classNameStr;
	}
	
	private static final Iterator<SubClass> iterSubClasses(L2PcInstance player)
	{
		return player.getSubClasses().values().iterator();
	}
	
	private static final void dissolveClan(L2PcInstance player)
	{
		if (!player.isClanLeader())
		{
			player.sendPacket(SystemMessageId.YOU_ARE_NOT_AUTHORIZED_TO_DO_THAT);
			return;
		}
		
		final L2Clan clan = player.getClan();
		if (clan.getAllyId() != 0)
		{
			player.sendPacket(SystemMessageId.CANNOT_DISPERSE_THE_CLANS_IN_ALLY);
			return;
		}
		
		if (clan.isAtWar())
		{
			player.sendPacket(SystemMessageId.CANNOT_DISSOLVE_WHILE_IN_WAR);
			return;
		}
		
		if (clan.hasCastle() || clan.hasHideout())
		{
			player.sendPacket(SystemMessageId.CANNOT_DISSOLVE_WHILE_OWNING_CLAN_HALL_OR_CASTLE);
			return;
		}
		
		for (Siege siege : SiegeManager.getSieges())
		{
			if (siege.isInProgress())
			{
				if (siege.getAttackerClan(clan) != null || siege.getDefenderClan(clan) != null)
				{
					player.sendPacket(SystemMessageId.CANNOT_DISSOLVE_WHILE_IN_SIEGE);
					return;
				}
			}
			else if (siege.getAttackerClan(clan) != null || siege.getDefenderClan(clan) != null || siege.getDefenderWaitingClan(clan) != null)
			{
				player.sendPacket(SystemMessageId.CANNOT_DISSOLVE_CAUSE_CLAN_WILL_PARTICIPATE_IN_CASTLE_SIEGE);
				return;
			}
		}
		
		if (clan.getDissolvingExpiryTime() > System.currentTimeMillis())
		{
			player.sendPacket(SystemMessageId.DISSOLUTION_IN_PROGRESS);
			return;
		}
		
		if (Config.ALT_CLAN_DISSOLVE_DAYS > 0)
		{
			clan.setDissolvingExpiryTime(System.currentTimeMillis() + Config.ALT_CLAN_DISSOLVE_DAYS * 86400000L);
			clan.updateClanInDB();
			
			ClanTable.getInstance().scheduleRemoveClan(clan);
		}
		else
			ClanTable.getInstance().destroyClan(clan.getClanId());
		
		// The clan leader should take the XP penalty of a full death.
		player.deathPenalty(false, false, false);
	}
	
	private static final void recoverClan(L2PcInstance player)
	{
		if (!player.isClanLeader())
		{
			player.sendPacket(SystemMessageId.YOU_ARE_NOT_AUTHORIZED_TO_DO_THAT);
			return;
		}
		
		final L2Clan clan = player.getClan();
		clan.setDissolvingExpiryTime(0);
		clan.updateClanInDB();
	}
	
	private static final void changeClanLeader(L2PcInstance player, String target)
	{
		if (!player.isClanLeader())
		{
			player.sendPacket(SystemMessageId.YOU_ARE_NOT_AUTHORIZED_TO_DO_THAT);
			return;
		}
		
		if (player.getName().equalsIgnoreCase(target))
			return;
		
		// little exploit fix
		if (player.isFlying())
		{
			player.sendMessage("You must dismount the wyvern to change the clan leader.");
			return;
		}
		
		final L2Clan clan = player.getClan();
		final L2ClanMember member = clan.getClanMember(target);
		
		if (member == null)
		{
			player.sendPacket(SystemMessage.getSystemMessage(SystemMessageId.S1_DOES_NOT_EXIST).addString(target));
			return;
		}
		
		if (!member.isOnline())
		{
			player.sendPacket(SystemMessageId.INVITED_USER_NOT_ONLINE);
			return;
		}
		
		clan.setNewLeader(member);
	}
	
	private static final void createSubPledge(L2PcInstance player, String clanName, String leaderName, int pledgeType, int minClanLvl)
	{
		if (!player.isClanLeader())
		{
			player.sendPacket(SystemMessageId.YOU_ARE_NOT_AUTHORIZED_TO_DO_THAT);
			return;
		}
		
		final L2Clan clan = player.getClan();
		if (clan.getLevel() < minClanLvl)
		{
			if (pledgeType == L2Clan.SUBUNIT_ACADEMY)
				player.sendPacket(SystemMessageId.YOU_DO_NOT_MEET_CRITERIA_IN_ORDER_TO_CREATE_A_CLAN_ACADEMY);
			else
				player.sendPacket(SystemMessageId.YOU_DO_NOT_MEET_CRITERIA_IN_ORDER_TO_CREATE_A_MILITARY_UNIT);
			
			return;
		}
		
		if (!Util.isAlphaNumeric(clanName))
		{
			player.sendPacket(SystemMessageId.CLAN_NAME_INVALID);
			return;
		}
		
		if (clanName.length() < 2 || clanName.length() > 16)
		{
			player.sendPacket(SystemMessageId.CLAN_NAME_LENGTH_INCORRECT);
			return;
		}
		
		for (L2Clan tempClan : ClanTable.getInstance().getClans())
		{
			if (tempClan.getSubPledge(clanName) != null)
			{
				if (pledgeType == L2Clan.SUBUNIT_ACADEMY)
					player.sendPacket(SystemMessage.getSystemMessage(SystemMessageId.S1_ALREADY_EXISTS).addString(clanName));
				else
					player.sendPacket(SystemMessageId.ANOTHER_MILITARY_UNIT_IS_ALREADY_USING_THAT_NAME);
				
				return;
			}
		}
		
		if (pledgeType != L2Clan.SUBUNIT_ACADEMY)
		{
			if (clan.getClanMember(leaderName) == null || clan.getClanMember(leaderName).getPledgeType() != 0)
			{
				if (pledgeType >= L2Clan.SUBUNIT_KNIGHT1)
					player.sendPacket(SystemMessageId.CAPTAIN_OF_ORDER_OF_KNIGHTS_CANNOT_BE_APPOINTED);
				else if (pledgeType >= L2Clan.SUBUNIT_ROYAL1)
					player.sendPacket(SystemMessageId.CAPTAIN_OF_ROYAL_GUARD_CANNOT_BE_APPOINTED);
				
				return;
			}
		}
		
		final int leaderId = pledgeType != L2Clan.SUBUNIT_ACADEMY ? clan.getClanMember(leaderName).getObjectId() : 0;
		if (clan.createSubPledge(player, pledgeType, leaderId, clanName) == null)
			return;
		
		SystemMessage sm;
		if (pledgeType == L2Clan.SUBUNIT_ACADEMY)
		{
			sm = SystemMessage.getSystemMessage(SystemMessageId.THE_S1S_CLAN_ACADEMY_HAS_BEEN_CREATED);
			sm.addString(player.getClan().getName());
		}
		else if (pledgeType >= L2Clan.SUBUNIT_KNIGHT1)
		{
			sm = SystemMessage.getSystemMessage(SystemMessageId.THE_KNIGHTS_OF_S1_HAVE_BEEN_CREATED);
			sm.addString(player.getClan().getName());
		}
		else if (pledgeType >= L2Clan.SUBUNIT_ROYAL1)
		{
			sm = SystemMessage.getSystemMessage(SystemMessageId.THE_ROYAL_GUARD_OF_S1_HAVE_BEEN_CREATED);
			sm.addString(player.getClan().getName());
		}
		else
			sm = SystemMessage.getSystemMessage(SystemMessageId.CLAN_CREATED);
		player.sendPacket(sm);
		
		if (pledgeType != L2Clan.SUBUNIT_ACADEMY)
		{
			final L2ClanMember leaderSubPledge = clan.getClanMember(leaderName);
			final L2PcInstance leaderPlayer = leaderSubPledge.getPlayerInstance();
			if (leaderPlayer != null)
			{
				leaderPlayer.setPledgeClass(L2ClanMember.calculatePledgeClass(leaderPlayer));
				leaderPlayer.sendPacket(new UserInfo(leaderPlayer));
			}
		}
	}
	
	private static final void renameSubPledge(L2PcInstance player, int pledgeType, String pledgeName)
	{
		if (!player.isClanLeader())
		{
			player.sendPacket(SystemMessageId.YOU_ARE_NOT_AUTHORIZED_TO_DO_THAT);
			return;
		}
		
		final L2Clan clan = player.getClan();
		final SubPledge subPledge = player.getClan().getSubPledge(pledgeType);
		
		if (subPledge == null)
		{
			player.sendMessage("Pledge doesn't exist.");
			return;
		}
		
		if (!Util.isAlphaNumeric(pledgeName))
		{
			player.sendPacket(SystemMessageId.CLAN_NAME_INVALID);
			return;
		}
		
		if (pledgeName.length() < 2 || pledgeName.length() > 16)
		{
			player.sendPacket(SystemMessageId.CLAN_NAME_LENGTH_INCORRECT);
			return;
		}
		
		subPledge.setName(pledgeName);
		clan.updateSubPledgeInDB(subPledge.getId());
		clan.broadcastToOnlineMembers(new PledgeShowMemberListAll(clan, subPledge.getId()));
		player.sendMessage("Pledge name have been changed to: " + pledgeName);
	}
	
	private static final void assignSubPledgeLeader(L2PcInstance player, String clanName, String leaderName)
	{
		if (!player.isClanLeader())
		{
			player.sendPacket(SystemMessageId.YOU_ARE_NOT_AUTHORIZED_TO_DO_THAT);
			return;
		}
		
		if (leaderName.length() > 16)
		{
			player.sendPacket(SystemMessageId.NAMING_CHARNAME_UP_TO_16CHARS);
			return;
		}
		
		if (player.getName().equals(leaderName))
		{
			player.sendPacket(SystemMessageId.CAPTAIN_OF_ROYAL_GUARD_CANNOT_BE_APPOINTED);
			return;
		}
		
		final L2Clan clan = player.getClan();
		final SubPledge subPledge = player.getClan().getSubPledge(clanName);
		
		if (null == subPledge || subPledge.getId() == L2Clan.SUBUNIT_ACADEMY)
		{
			player.sendPacket(SystemMessageId.CLAN_NAME_INVALID);
			return;
		}
		
		final L2ClanMember leaderSubPledge = clan.getClanMember(leaderName);
		
		if (leaderSubPledge == null || leaderSubPledge.getPledgeType() != 0)
		{
			if (subPledge.getId() >= L2Clan.SUBUNIT_KNIGHT1)
				player.sendPacket(SystemMessageId.CAPTAIN_OF_ORDER_OF_KNIGHTS_CANNOT_BE_APPOINTED);
			else if (subPledge.getId() >= L2Clan.SUBUNIT_ROYAL1)
				player.sendPacket(SystemMessageId.CAPTAIN_OF_ROYAL_GUARD_CANNOT_BE_APPOINTED);
			
			return;
		}
		
		// Avoid naming sub pledges with the same captain
		if (clan.isSubPledgeLeader(leaderSubPledge.getObjectId()))
		{
			if (subPledge.getId() >= L2Clan.SUBUNIT_KNIGHT1)
				player.sendPacket(SystemMessageId.CAPTAIN_OF_ORDER_OF_KNIGHTS_CANNOT_BE_APPOINTED);
			else if (subPledge.getId() >= L2Clan.SUBUNIT_ROYAL1)
				player.sendPacket(SystemMessageId.CAPTAIN_OF_ROYAL_GUARD_CANNOT_BE_APPOINTED);
			
			return;
		}
		
		subPledge.setLeaderId(leaderSubPledge.getObjectId());
		clan.updateSubPledgeInDB(subPledge.getId());
		
		final L2PcInstance leaderPlayer = leaderSubPledge.getPlayerInstance();
		if (leaderPlayer != null)
		{
			leaderPlayer.setPledgeClass(L2ClanMember.calculatePledgeClass(leaderPlayer));
			leaderPlayer.sendPacket(new UserInfo(leaderPlayer));
		}
		
		clan.broadcastToOnlineMembers(new PledgeShowMemberListAll(clan, subPledge.getId()), SystemMessage.getSystemMessage(SystemMessageId.S1_HAS_BEEN_SELECTED_AS_CAPTAIN_OF_S2).addString(leaderName).addString(clanName));
	}
	
	/**
	 * this displays PledgeSkillList to the player.
	 * @param player
	 */
	public static final void showPledgeSkillList(L2PcInstance player)
	{
		if (player.getClan() == null || !player.isClanLeader())
		{
			NpcHtmlMessage html = new NpcHtmlMessage(0);
			html.setFile("data/html/villagemaster/NotClanLeader.htm");
			player.sendPacket(html);
			player.sendPacket(ActionFailed.STATIC_PACKET);
			return;
		}
		
		AcquireSkillList asl = new AcquireSkillList(AcquireSkillList.SkillType.Clan);
		boolean empty = true;
		
		for (L2PledgeSkillLearn psl : SkillTreeTable.getInstance().getAvailablePledgeSkills(player))
		{
			L2Skill sk = SkillTable.getInstance().getInfo(psl.getId(), psl.getLevel());
			if (sk == null)
				continue;
			
			asl.addSkill(psl.getId(), psl.getLevel(), psl.getLevel(), psl.getRepCost(), 0);
			empty = false;
		}
		
		if (empty)
		{
			if (player.getClan().getLevel() < 8)
			{
				SystemMessage sm = SystemMessage.getSystemMessage(SystemMessageId.DO_NOT_HAVE_FURTHER_SKILLS_TO_LEARN_S1);
				sm.addNumber(Math.max(player.getClan().getLevel() + 1, 5));
				player.sendPacket(sm);
			}
			else
			{
				NpcHtmlMessage html = new NpcHtmlMessage(1);
				html.setFile("data/html/villagemaster/NoMoreSkills.htm");
				player.sendPacket(html);
			}
		}
		else
			player.sendPacket(asl);
		
		player.sendPacket(ActionFailed.STATIC_PACKET);
	}
}