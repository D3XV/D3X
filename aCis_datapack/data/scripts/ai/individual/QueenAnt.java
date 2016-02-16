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
package ai.individual;

import ai.AbstractNpcAI;

import java.util.ArrayList;
import java.util.List;

import net.sf.l2j.Config;
import net.sf.l2j.gameserver.ai.CtrlIntention;
import net.sf.l2j.gameserver.datatables.SkillTable.FrequentSkill;
import net.sf.l2j.gameserver.instancemanager.GrandBossManager;
import net.sf.l2j.gameserver.model.L2Skill;
import net.sf.l2j.gameserver.model.actor.L2Attackable;
import net.sf.l2j.gameserver.model.actor.L2Npc;
import net.sf.l2j.gameserver.model.actor.L2Playable;
import net.sf.l2j.gameserver.model.actor.instance.L2GrandBossInstance;
import net.sf.l2j.gameserver.model.actor.instance.L2MonsterInstance;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.model.holder.SkillHolder;
import net.sf.l2j.gameserver.model.quest.QuestEventType;
import net.sf.l2j.gameserver.model.zone.type.L2BossZone;
import net.sf.l2j.gameserver.network.serverpackets.MagicSkillUse;
import net.sf.l2j.gameserver.network.serverpackets.PlaySound;
import net.sf.l2j.gameserver.network.serverpackets.SocialAction;
import net.sf.l2j.gameserver.templates.StatsSet;
import net.sf.l2j.util.Rnd;

/**
 * Queen Ant AI
 * @author Emperorc
 */
public class QueenAnt extends AbstractNpcAI
{
	private static final L2BossZone AQ_LAIR = GrandBossManager.getInstance().getZoneById(110012);
	
	private static final int QUEEN = 29001;
	private static final int LARVA = 29002;
	private static final int NURSE = 29003;
	private static final int GUARD = 29004;
	private static final int ROYAL = 29005;
	
	private static final int[] MOBS =
	{
		QUEEN,
		LARVA,
		NURSE,
		GUARD,
		ROYAL
	};
	
	private static final int QUEEN_X = -21610;
	private static final int QUEEN_Y = 181594;
	private static final int QUEEN_Z = -5734;
	
	// Status Tracking
	private static final byte ALIVE = 0; // Queen Ant is spawned.
	private static final byte DEAD = 1; // Queen Ant has been killed.
	
	private static final SkillHolder HEAL1 = new SkillHolder(4020, 1);
	private static final SkillHolder HEAL2 = new SkillHolder(4024, 1);
	
	private static final List<L2MonsterInstance> _nurses = new ArrayList<>(5);
	
	private L2MonsterInstance _queen = null;
	private L2MonsterInstance _larva = null;
	
	public QueenAnt(String name, String descr)
	{
		super(name, descr);
		
		registerMobs(MOBS, QuestEventType.ON_SPAWN, QuestEventType.ON_KILL, QuestEventType.ON_AGGRO_RANGE_ENTER);
		addFactionCallId(NURSE);
		
		StatsSet info = GrandBossManager.getInstance().getStatsSet(QUEEN);
		if (GrandBossManager.getInstance().getBossStatus(QUEEN) == DEAD)
		{
			// load the unlock date and time for queen ant from DB
			long temp = info.getLong("respawn_time") - System.currentTimeMillis();
			
			// the unlock time has not yet expired.
			if (temp > 0)
				startQuestTimer("queen_unlock", temp, null, null, false);
			// the time has already expired while the server was offline. Immediately spawn queen ant.
			else
			{
				L2GrandBossInstance queen = (L2GrandBossInstance) addSpawn(QUEEN, QUEEN_X, QUEEN_Y, QUEEN_Z, 0, false, 0, false);
				GrandBossManager.getInstance().setBossStatus(QUEEN, ALIVE);
				spawnBoss(queen);
			}
		}
		else
		{
			int loc_x = info.getInteger("loc_x");
			int loc_y = info.getInteger("loc_y");
			int loc_z = info.getInteger("loc_z");
			int heading = info.getInteger("heading");
			int hp = info.getInteger("currentHP");
			int mp = info.getInteger("currentMP");
			if (!AQ_LAIR.isInsideZone(loc_x, loc_y, loc_z))
			{
				loc_x = QUEEN_X;
				loc_y = QUEEN_Y;
				loc_z = QUEEN_Z;
			}
			
			L2GrandBossInstance queen = (L2GrandBossInstance) addSpawn(QUEEN, loc_x, loc_y, loc_z, heading, false, 0, false);
			queen.setCurrentHpMp(hp, mp);
			spawnBoss(queen);
		}
	}
	
	private void spawnBoss(L2GrandBossInstance npc)
	{
		if (Rnd.get(100) < 33)
			AQ_LAIR.movePlayersTo(-19480, 187344, -5600);
		else if (Rnd.get(100) < 50)
			AQ_LAIR.movePlayersTo(-17928, 180912, -5520);
		else
			AQ_LAIR.movePlayersTo(-23808, 182368, -5600);
		
		GrandBossManager.getInstance().addBoss(npc);
		startQuestTimer("action", 10000, npc, null, true);
		startQuestTimer("heal", 1000, null, null, true);
		npc.broadcastPacket(new PlaySound(1, "BS02_D", 1, npc.getObjectId(), npc.getX(), npc.getY(), npc.getZ()));
		
		_queen = npc;
		_larva = (L2MonsterInstance) addSpawn(LARVA, -21600, 179482, -5846, Rnd.get(360), false, 0, false);
	}
	
	@Override
	public String onAdvEvent(String event, L2Npc npc, L2PcInstance player)
	{
		if (event.equalsIgnoreCase("heal"))
		{
			boolean notCasting;
			final boolean larvaNeedHeal = _larva != null && _larva.getCurrentHp() < _larva.getMaxHp();
			final boolean queenNeedHeal = _queen != null && _queen.getCurrentHp() < _queen.getMaxHp();
			for (L2MonsterInstance nurse : _nurses)
			{
				if (nurse == null || nurse.isDead() || nurse.isCastingNow())
					continue;
				
				notCasting = nurse.getAI().getIntention() != CtrlIntention.CAST;
				if (larvaNeedHeal)
				{
					if (nurse.getTarget() != _larva || notCasting)
					{
						nurse.setTarget(_larva);
						nurse.useMagic(Rnd.nextBoolean() ? HEAL1.getSkill() : HEAL2.getSkill());
					}
					continue;
				}
				
				if (queenNeedHeal)
				{
					if (nurse.getLeader() == _larva) // skip larva's minions
						continue;
					
					if (nurse.getTarget() != _queen || notCasting)
					{
						nurse.setTarget(_queen);
						nurse.useMagic(HEAL1.getSkill());
					}
					continue;
				}
				
				// if nurse not casting - remove target
				if (notCasting && nurse.getTarget() != null)
					nurse.setTarget(null);
			}
		}
		else if (event.equalsIgnoreCase("action") && npc != null)
		{
			if (Rnd.get(3) == 0)
			{
				if (Rnd.get(2) == 0)
					npc.broadcastPacket(new SocialAction(npc, 3));
				else
					npc.broadcastPacket(new SocialAction(npc, 4));
			}
		}
		else if (event.equalsIgnoreCase("queen_unlock"))
		{
			L2GrandBossInstance queen = (L2GrandBossInstance) addSpawn(QUEEN, QUEEN_X, QUEEN_Y, QUEEN_Z, 0, false, 0, false);
			GrandBossManager.getInstance().setBossStatus(QUEEN, ALIVE);
			spawnBoss(queen);
		}
		return super.onAdvEvent(event, npc, player);
	}
	
	@Override
	public String onSpawn(L2Npc npc)
	{
		final L2MonsterInstance mob = (L2MonsterInstance) npc;
		switch (npc.getNpcId())
		{
			case LARVA:
				mob.setIsImmobilized(true);
				mob.setIsMortal(false);
				mob.setIsRaidMinion(true);
				break;
			case NURSE:
				mob.disableCoreAI(true);
				mob.setIsRaidMinion(true);
				_nurses.add(mob);
				break;
			case ROYAL:
			case GUARD:
				mob.setIsRaidMinion(true);
				break;
		}
		return super.onSpawn(npc);
	}
	
	@Override
	public String onFactionCall(L2Npc npc, L2Npc caller, L2PcInstance attacker, boolean isPet)
	{
		if (caller == null || npc == null)
			return super.onFactionCall(npc, caller, attacker, isPet);
		
		if (!npc.isCastingNow() && npc.getAI().getIntention() != CtrlIntention.CAST)
		{
			if (caller.getCurrentHp() < caller.getMaxHp())
			{
				npc.setTarget(caller);
				((L2Attackable) npc).useMagic(HEAL1.getSkill());
			}
		}
		return null;
	}
	
	@Override
	public String onAggroRangeEnter(L2Npc npc, L2PcInstance player, boolean isPet)
	{
		if (npc == null)
			return null;
		
		final boolean isMage;
		final L2Playable character;
		if (isPet)
		{
			isMage = false;
			character = player.getPet();
		}
		else
		{
			isMage = player.isMageClass();
			character = player;
		}
		
		if (character == null)
			return null;
		
		if (!Config.RAID_DISABLE_CURSE && character.getLevel() - npc.getLevel() > 8)
		{
			L2Skill curse = null;
			if (isMage)
			{
				if (!character.isMuted() && Rnd.get(4) == 0)
					curse = FrequentSkill.RAID_CURSE.getSkill();
			}
			else
			{
				if (!character.isParalyzed() && Rnd.get(4) == 0)
					curse = FrequentSkill.RAID_CURSE2.getSkill();
			}
			
			if (curse != null)
			{
				npc.broadcastPacket(new MagicSkillUse(npc, character, curse.getId(), curse.getLevel(), 300, 0));
				curse.getEffects(npc, character);
			}
			
			((L2Attackable) npc).stopHating(character); // for calling again
			return null;
		}
		return super.onAggroRangeEnter(npc, player, isPet);
	}
	
	@Override
	public String onKill(L2Npc npc, L2PcInstance killer, boolean isPet)
	{
		// Acts only once.
		if (GrandBossManager.getInstance().getBossStatus(QUEEN) == ALIVE)
		{
			int npcId = npc.getNpcId();
			if (npcId == QUEEN)
			{
				npc.broadcastPacket(new PlaySound(1, "BS02_D", 1, npc.getObjectId(), npc.getX(), npc.getY(), npc.getZ()));
				GrandBossManager.getInstance().setBossStatus(QUEEN, DEAD);
				
				long respawnTime = (long) Config.SPAWN_INTERVAL_AQ + Rnd.get(-Config.RANDOM_SPAWN_TIME_AQ, Config.RANDOM_SPAWN_TIME_AQ);
				respawnTime *= 3600000;
				
				startQuestTimer("queen_unlock", respawnTime, null, null, false);
				cancelQuestTimer("action", npc, null);
				cancelQuestTimer("heal", null, null);
				
				// also save the respawn time so that the info is maintained past reboots
				StatsSet info = GrandBossManager.getInstance().getStatsSet(QUEEN);
				info.set("respawn_time", System.currentTimeMillis() + respawnTime);
				GrandBossManager.getInstance().setStatsSet(QUEEN, info);
				
				_nurses.clear();
				_larva.deleteMe();
				_larva = null;
				_queen = null;
			}
			else
			{
				if (npcId == ROYAL)
				{
					L2MonsterInstance mob = (L2MonsterInstance) npc;
					if (mob.getLeader() != null)
						mob.getLeader().getMinionList().onMinionDie(mob, (280 + Rnd.get(40)) * 1000);
				}
				else if (npcId == NURSE)
				{
					L2MonsterInstance mob = (L2MonsterInstance) npc;
					_nurses.remove(mob);
					if (mob.getLeader() != null)
						mob.getLeader().getMinionList().onMinionDie(mob, 10000);
				}
			}
		}
		return super.onKill(npc, killer, isPet);
	}
	
	public static void main(String[] args)
	{
		new QueenAnt(QueenAnt.class.getSimpleName(), "ai/individual");
	}
}