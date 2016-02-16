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

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import net.sf.l2j.Config;
import net.sf.l2j.gameserver.ai.CtrlIntention;
import net.sf.l2j.gameserver.datatables.SkillTable;
import net.sf.l2j.gameserver.datatables.SkillTable.FrequentSkill;
import net.sf.l2j.gameserver.geoengine.PathFinding;
import net.sf.l2j.gameserver.instancemanager.GrandBossManager;
import net.sf.l2j.gameserver.model.L2CharPosition;
import net.sf.l2j.gameserver.model.L2Skill;
import net.sf.l2j.gameserver.model.actor.L2Attackable;
import net.sf.l2j.gameserver.model.actor.L2Npc;
import net.sf.l2j.gameserver.model.actor.instance.L2GrandBossInstance;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.model.quest.QuestEventType;
import net.sf.l2j.gameserver.model.zone.type.L2BossZone;
import net.sf.l2j.gameserver.network.serverpackets.PlaySound;
import net.sf.l2j.gameserver.network.serverpackets.SocialAction;
import net.sf.l2j.gameserver.network.serverpackets.SpecialCamera;
import net.sf.l2j.gameserver.templates.StatsSet;
import net.sf.l2j.gameserver.util.Util;
import net.sf.l2j.util.Rnd;

/**
 * That AI is heavily based on Valakas/Baium scripts.<br>
 * It uses the 29019 dummy id in order to register it (addBoss and statsSet), but 3 different templates according the situation.
 * @author Tryskell
 */
public class Antharas extends AbstractNpcAI
{
	private static final L2BossZone ANTHARAS_LAIR = GrandBossManager.getInstance().getZoneById(110001);
	
	private static final int[] ANTHARAS_IDS =
	{
		29066,
		29067,
		29068
	};
	
	public static final int ANTHARAS = 29019; // Dummy Antharas id used for status updates only.
	
	public static final byte DORMANT = 0; // No one has entered yet. Entry is unlocked.
	public static final byte WAITING = 1; // Someone has entered, triggering a 30 minute window for additional people to enter. Entry is unlocked.
	public static final byte FIGHTING = 2; // Antharas is engaged in battle, annihilating his foes. Entry is locked.
	public static final byte DEAD = 3; // Antharas has been killed. Entry is locked.
	
	private long _timeTracker = 0; // Time tracker for last attack on Antharas.
	private L2PcInstance _actualVictim; // Actual target of Antharas.
	private final List<L2Npc> _monsters = new CopyOnWriteArrayList<>(); // amount of Antharas minions.
	
	private int _antharasId; // The current Antharas, used when server shutdowns.
	private L2Skill _skillRegen; // The regen skill used by Antharas.
	private int _minionTimer; // The timer used by minions in order to spawn.
	
	public Antharas(String name, String descr)
	{
		super(name, descr);
		
		int[] allIds =
		{
			29066,
			29067,
			29068,
			29069,
			29070,
			29071,
			29072,
			29073,
			29074,
			29075,
			29076
		};
		
		registerMobs(ANTHARAS_IDS, QuestEventType.ON_ATTACK, QuestEventType.ON_SPAWN);
		registerMobs(allIds, QuestEventType.ON_KILL);
		
		final StatsSet info = GrandBossManager.getInstance().getStatsSet(ANTHARAS);
		
		switch (GrandBossManager.getInstance().getBossStatus(ANTHARAS))
		{
			case DEAD: // Launch the timer to set DORMANT, or set DORMANT directly if timer expired while offline.
				long temp = (info.getLong("respawn_time") - System.currentTimeMillis());
				if (temp > 0)
					startQuestTimer("antharas_unlock", temp, null, null, false);
				else
					GrandBossManager.getInstance().setBossStatus(ANTHARAS, DORMANT);
				break;
			
			case WAITING: // Launch beginning timer.
				startQuestTimer("beginning", Config.WAIT_TIME_ANTHARAS, null, null, false);
				break;
			
			case FIGHTING:
				final int loc_x = info.getInteger("loc_x");
				final int loc_y = info.getInteger("loc_y");
				final int loc_z = info.getInteger("loc_z");
				final int heading = info.getInteger("heading");
				final int hp = info.getInteger("currentHP");
				final int mp = info.getInteger("currentMP");
				
				// Update Antharas informations.
				updateAntharas();
				
				final L2Npc antharas = addSpawn(_antharasId, loc_x, loc_y, loc_z, heading, false, 0, false);
				GrandBossManager.getInstance().addBoss(ANTHARAS, (L2GrandBossInstance) antharas);
				
				antharas.setCurrentHpMp(hp, mp);
				antharas.setRunning();
				
				// stores current time for inactivity task.
				_timeTracker = System.currentTimeMillis();
				
				startQuestTimer("regen_task", 60000, antharas, null, true);
				startQuestTimer("skill_task", 2000, antharas, null, true);
				startQuestTimer("minions_spawn", _minionTimer, antharas, null, true);
				break;
		}
	}
	
	@Override
	public String onAdvEvent(String event, L2Npc npc, L2PcInstance player)
	{
		// Regeneration && inactivity task
		if (event.equalsIgnoreCase("regen_task"))
		{
			// Inactivity task - 30min
			if (_timeTracker + 1800000 < System.currentTimeMillis())
			{
				// Set it dormant.
				GrandBossManager.getInstance().setBossStatus(ANTHARAS, DORMANT);
				
				// Drop all players from the zone.
				ANTHARAS_LAIR.oustAllPlayers();
				
				// Drop tasks.
				dropTimers(npc);
				
				// Delete current instance of Antharas.
				npc.deleteMe();
				return null;
			}
			_skillRegen.getEffects(npc, npc);
		}
		// Spawn cinematic, regen_task and choose of skill.
		else if (event.equalsIgnoreCase("spawn_1"))
			ANTHARAS_LAIR.broadcastPacket(new SpecialCamera(npc.getObjectId(), 700, 13, -19, 0, 20000, 0, 0, 1, 0));
		else if (event.equalsIgnoreCase("spawn_2"))
		{
			npc.broadcastPacket(new SocialAction(npc, 1));
			ANTHARAS_LAIR.broadcastPacket(new SpecialCamera(npc.getObjectId(), 700, 13, 0, 6000, 20000, 0, 0, 1, 0));
		}
		else if (event.equalsIgnoreCase("spawn_3"))
			ANTHARAS_LAIR.broadcastPacket(new SpecialCamera(npc.getObjectId(), 3700, 0, -3, 0, 10000, 0, 0, 1, 0));
		else if (event.equalsIgnoreCase("spawn_4"))
		{
			npc.broadcastPacket(new SocialAction(npc, 2));
			ANTHARAS_LAIR.broadcastPacket(new SpecialCamera(npc.getObjectId(), 1100, 0, -3, 22000, 30000, 0, 0, 1, 0));
		}
		else if (event.equalsIgnoreCase("spawn_5"))
			ANTHARAS_LAIR.broadcastPacket(new SpecialCamera(npc.getObjectId(), 1100, 0, -3, 300, 7000, 0, 0, 1, 0));
		else if (event.equalsIgnoreCase("spawn_6"))
		{
			// stores current time for inactivity task.
			_timeTracker = System.currentTimeMillis();
			
			GrandBossManager.getInstance().setBossStatus(ANTHARAS, FIGHTING);
			npc.setIsInvul(false);
			npc.setRunning();
			
			startQuestTimer("regen_task", 60000, npc, null, true);
			startQuestTimer("skill_task", 2000, npc, null, true);
			startQuestTimer("minions_spawn", _minionTimer, npc, null, true);
		}
		else if (event.equalsIgnoreCase("skill_task"))
			callSkillAI(npc);
		else if (event.equalsIgnoreCase("minions_spawn"))
		{
			boolean isBehemoth = Rnd.get(100) < 60;
			int mobNumber = isBehemoth ? 2 : 3;
			
			// Set spawn.
			for (int i = 0; i < mobNumber; i++)
			{
				if (_monsters.size() > 9)
					break;
				
				final int npcId = isBehemoth ? 29069 : Rnd.get(29070, 29076);
				final L2Npc dragon = addSpawn(npcId, npc.getX() + Rnd.get(-200, 200), npc.getY() + Rnd.get(-200, 200), npc.getZ(), 0, false, 0, true);
				((L2Attackable) dragon).setIsRaidMinion(true);
				
				_monsters.add(dragon);
				
				final L2PcInstance victim = getRandomPlayer(dragon);
				if (victim != null)
					attack(((L2Attackable) dragon), victim);
				
				if (!isBehemoth)
					startQuestTimer("self_destruct", (_minionTimer / 3), dragon, null, false);
			}
		}
		else if (event.equalsIgnoreCase("self_destruct"))
		{
			L2Skill skill;
			switch (npc.getNpcId())
			{
				case 29070:
				case 29071:
				case 29072:
				case 29073:
				case 29074:
				case 29075:
					skill = SkillTable.getInstance().getInfo(5097, 1);
					break;
				default:
					skill = SkillTable.getInstance().getInfo(5094, 1);
			}
			npc.doCast(skill);
		}
		// Cinematic
		else if (event.equalsIgnoreCase("beginning"))
		{
			updateAntharas();
			
			final L2Npc antharas = addSpawn(_antharasId, 181323, 114850, -7623, 32542, false, 0, false);
			GrandBossManager.getInstance().addBoss(ANTHARAS, (L2GrandBossInstance) antharas);
			antharas.setIsInvul(true);
			
			// Launch the cinematic, and tasks (regen + skill).
			startQuestTimer("spawn_1", 16, antharas, null, false);
			startQuestTimer("spawn_2", 3016, antharas, null, false);
			startQuestTimer("spawn_3", 13016, antharas, null, false);
			startQuestTimer("spawn_4", 13216, antharas, null, false);
			startQuestTimer("spawn_5", 24016, antharas, null, false);
			startQuestTimer("spawn_6", 25916, antharas, null, false);
		}
		// spawn of Teleport Cube.
		else if (event.equalsIgnoreCase("die_1"))
		{
			addSpawn(31859, 177615, 114941, -7709, 0, false, 900000, false);
			startQuestTimer("remove_players", 900000, null, null, false);
		}
		else if (event.equalsIgnoreCase("antharas_unlock"))
			GrandBossManager.getInstance().setBossStatus(ANTHARAS, DORMANT);
		else if (event.equalsIgnoreCase("remove_players"))
			ANTHARAS_LAIR.oustAllPlayers();
		
		return super.onAdvEvent(event, npc, player);
	}
	
	@Override
	public String onSpawn(L2Npc npc)
	{
		npc.disableCoreAI(true);
		return super.onSpawn(npc);
	}
	
	@Override
	public String onAttack(L2Npc npc, L2PcInstance attacker, int damage, boolean isPet)
	{
		if (npc.isInvul())
			return null;
		
		if (!ANTHARAS_LAIR.isInsideZone(attacker))
		{
			attacker.teleToLocation(82698, 148638, -3473, 0);
			return null;
		}
		
		// Debuff strider-mounted players.
		if (attacker.getMountType() == 1)
		{
			final L2Skill skill = SkillTable.getInstance().getInfo(4258, 1);
			if (attacker.getFirstEffect(skill) == null)
			{
				npc.setTarget(attacker);
				npc.doCast(skill);
			}
		}
		_timeTracker = System.currentTimeMillis();
		
		return super.onAttack(npc, attacker, damage, isPet);
	}
	
	@Override
	public String onKill(L2Npc npc, L2PcInstance killer, boolean isPet)
	{
		if (npc.getNpcId() == _antharasId)
		{
			// Drop tasks.
			dropTimers(npc);
			
			// Launch death animation.
			ANTHARAS_LAIR.broadcastPacket(new SpecialCamera(npc.getObjectId(), 1200, 20, -10, 10000, 13000, 0, 0, 0, 0));
			ANTHARAS_LAIR.broadcastPacket(new PlaySound(1, "BS01_D", 0, 0, 0, 0, 0));
			startQuestTimer("die_1", 8000, null, null, false);
			
			GrandBossManager.getInstance().setBossStatus(ANTHARAS, DEAD);
			
			long respawnTime = (long) Config.SPAWN_INTERVAL_ANTHARAS + Rnd.get(-Config.RANDOM_SPAWN_TIME_ANTHARAS, Config.RANDOM_SPAWN_TIME_ANTHARAS);
			respawnTime *= 3600000;
			
			startQuestTimer("antharas_unlock", respawnTime, null, null, false);
			
			StatsSet info = GrandBossManager.getInstance().getStatsSet(ANTHARAS);
			info.set("respawn_time", System.currentTimeMillis() + respawnTime);
			GrandBossManager.getInstance().setStatsSet(ANTHARAS, info);
		}
		else
		{
			cancelQuestTimer("self_destruct", npc, null);
			_monsters.remove(npc);
		}
		
		return super.onKill(npc, killer, isPet);
	}
	
	private void callSkillAI(L2Npc npc)
	{
		if (npc.isInvul() || npc.isCastingNow())
			return;
		
		// Pickup a target if no or dead victim. 10% luck he decides to reconsiders his target.
		if (_actualVictim == null || _actualVictim.isDead() || !(npc.getKnownList().knowsObject(_actualVictim)) || Rnd.get(10) == 0)
			_actualVictim = getRandomPlayer(npc);
		
		// If result is still null, Antharas will roam. Don't go deeper in skill AI.
		if (_actualVictim == null)
		{
			if (Rnd.get(10) == 0)
			{
				int x = npc.getX();
				int y = npc.getY();
				int z = npc.getZ();
				
				int posX = x + Rnd.get(-1400, 1400);
				int posY = y + Rnd.get(-1400, 1400);
				
				if (PathFinding.getInstance().canMoveToTarget(x, y, z, posX, posY, z))
					npc.getAI().setIntention(CtrlIntention.MOVE_TO, new L2CharPosition(posX, posY, z, 0));
			}
			return;
		}
		
		final L2Skill skill = getRandomSkill(npc);
		
		// Cast the skill or follow the target.
		if (Util.checkIfInRange((skill.getCastRange() < 600) ? 600 : skill.getCastRange(), npc, _actualVictim, true))
		{
			npc.getAI().setIntention(CtrlIntention.IDLE);
			npc.setTarget(_actualVictim);
			npc.doCast(skill);
		}
		else
			npc.getAI().setIntention(CtrlIntention.FOLLOW, _actualVictim, null);
	}
	
	/**
	 * Pick a random skill.<br>
	 * The use is based on current HPs ratio.
	 * @param npc Antharas
	 * @return a usable skillId
	 */
	private static L2Skill getRandomSkill(L2Npc npc)
	{
		final double hpRatio = npc.getCurrentHp() / npc.getMaxHp();
		
		// Find enemies surrounding Antharas.
		final int[] playersAround = getPlayersCountInPositions(1100, npc, false);
		
		if (hpRatio < 0.25)
		{
			if (Rnd.get(100) < 30)
				return FrequentSkill.ANTHARAS_MOUTH.getSkill();
			
			if (playersAround[1] >= 10 && Rnd.get(100) < 80)
				return FrequentSkill.ANTHARAS_TAIL.getSkill();
			
			if (playersAround[0] >= 10)
			{
				if (Rnd.get(100) < 40)
					return FrequentSkill.ANTHARAS_DEBUFF.getSkill();
				
				if (Rnd.get(100) < 10)
					return FrequentSkill.ANTHARAS_JUMP.getSkill();
			}
			
			if (Rnd.get(100) < 10)
				return FrequentSkill.ANTHARAS_METEOR.getSkill();
		}
		else if (hpRatio < 0.5)
		{
			if (playersAround[1] >= 10 && Rnd.get(100) < 80)
				return FrequentSkill.ANTHARAS_TAIL.getSkill();
			
			if (playersAround[0] >= 10)
			{
				if (Rnd.get(100) < 40)
					return FrequentSkill.ANTHARAS_DEBUFF.getSkill();
				
				if (Rnd.get(100) < 10)
					return FrequentSkill.ANTHARAS_JUMP.getSkill();
			}
			
			if (Rnd.get(100) < 7)
				return FrequentSkill.ANTHARAS_METEOR.getSkill();
		}
		else if (hpRatio < 0.75)
		{
			if (playersAround[1] >= 10 && Rnd.get(100) < 80)
				return FrequentSkill.ANTHARAS_TAIL.getSkill();
			
			if (playersAround[0] >= 10 && Rnd.get(100) < 10)
				return FrequentSkill.ANTHARAS_JUMP.getSkill();
			
			if (Rnd.get(100) < 5)
				return FrequentSkill.ANTHARAS_METEOR.getSkill();
		}
		else
		{
			if (playersAround[1] >= 10 && Rnd.get(100) < 80)
				return FrequentSkill.ANTHARAS_TAIL.getSkill();
			
			if (Rnd.get(100) < 3)
				return FrequentSkill.ANTHARAS_METEOR.getSkill();
		}
		
		if (Rnd.get(100) < 6)
			return FrequentSkill.ANTHARAS_BREATH.getSkill();
		
		if (Rnd.get(100) < 50)
			return FrequentSkill.ANTHARAS_NORMAL_ATTACK.getSkill();
		
		if (Rnd.get(100) < 5)
		{
			if (Rnd.get(100) < 50)
				return FrequentSkill.ANTHARAS_FEAR.getSkill();
			
			return FrequentSkill.ANTHARAS_SHORT_FEAR.getSkill();
		}
		
		return FrequentSkill.ANTHARAS_NORMAL_ATTACK_EX.getSkill();
	}
	
	/**
	 * Update Antharas informations depending about how much players joined the fight.<br>
	 * Used when server restarted and Antharas is fighting, or used while the cinematic occurs (after the 30min timer).
	 */
	private void updateAntharas()
	{
		final int playersNumber = ANTHARAS_LAIR.getAllowedPlayers().size();
		if (playersNumber < 45)
		{
			_antharasId = ANTHARAS_IDS[0];
			_skillRegen = SkillTable.getInstance().getInfo(4239, 1);
			_minionTimer = 180000;
		}
		else if (playersNumber < 63)
		{
			_antharasId = ANTHARAS_IDS[1];
			_skillRegen = SkillTable.getInstance().getInfo(4240, 1);
			_minionTimer = 150000;
		}
		else
		{
			_antharasId = ANTHARAS_IDS[2];
			_skillRegen = SkillTable.getInstance().getInfo(4241, 1);
			_minionTimer = 120000;
		}
	}
	
	/**
	 * Drop timers, meaning Antharas is dead or inactivity task occured.
	 */
	private void dropTimers(L2Npc npc)
	{
		cancelQuestTimer("regen_task", npc, null);
		cancelQuestTimer("skill_task", npc, null);
		cancelQuestTimer("minions_spawn", npc, null);
		
		for (L2Npc mob : _monsters)
		{
			cancelQuestTimer("self_destruct", mob, null);
			mob.deleteMe();
		}
		_monsters.clear();
	}
	
	public static void main(String[] args)
	{
		new Antharas(Antharas.class.getSimpleName(), "ai/individual");
	}
}