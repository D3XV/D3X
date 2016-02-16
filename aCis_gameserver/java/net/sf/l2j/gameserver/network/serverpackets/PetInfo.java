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
package net.sf.l2j.gameserver.network.serverpackets;

import net.sf.l2j.gameserver.model.actor.L2Summon;
import net.sf.l2j.gameserver.model.actor.instance.L2PetInstance;
import net.sf.l2j.gameserver.model.actor.instance.L2SummonInstance;

public class PetInfo extends L2GameServerPacket
{
	private final L2Summon _summon;
	private final int _x, _y, _z, _heading;
	private final int _val;
	private final boolean _isSummoned;
	private final int _mAtkSpd, _pAtkSpd;
	private final int _runSpd, _walkSpd;
	private final int _maxHp, _maxMp;
	private int _maxFed, _curFed;
	private final float _multiplier;
	
	public PetInfo(L2Summon summon, int val)
	{
		_summon = summon;
		_isSummoned = _summon.isShowSummonAnimation();
		_x = _summon.getX();
		_y = _summon.getY();
		_z = _summon.getZ();
		_heading = _summon.getHeading();
		_mAtkSpd = _summon.getMAtkSpd();
		_pAtkSpd = _summon.getPAtkSpd();
		_multiplier = _summon.getMovementSpeedMultiplier();
		_runSpd = _summon.getTemplate().getBaseRunSpd();
		_walkSpd = _summon.getTemplate().getBaseWalkSpd();
		_maxHp = _summon.getMaxHp();
		_maxMp = _summon.getMaxMp();
		_val = val;
		
		if (_summon instanceof L2PetInstance)
		{
			L2PetInstance pet = (L2PetInstance) _summon;
			_curFed = pet.getCurrentFed(); // how fed it is
			_maxFed = pet.getMaxFed(); // max fed it can be
		}
		else if (_summon instanceof L2SummonInstance)
		{
			L2SummonInstance sum = (L2SummonInstance) _summon;
			_curFed = sum.getTimeRemaining();
			_maxFed = sum.getTotalLifeTime();
		}
	}
	
	@Override
	protected final void writeImpl()
	{
		writeC(0xb1);
		writeD(_summon.getSummonType());
		writeD(_summon.getObjectId());
		writeD(_summon.getTemplate().getIdTemplate() + 1000000);
		writeD(0); // 1=attackable
		
		writeD(_x);
		writeD(_y);
		writeD(_z);
		writeD(_heading);
		writeD(0);
		writeD(_mAtkSpd);
		writeD(_pAtkSpd);
		writeD(_runSpd);
		writeD(_walkSpd);
		writeD(_runSpd); // swim
		writeD(_walkSpd);
		writeD(_runSpd); // ? what's that
		writeD(_walkSpd);
		writeD(_runSpd); // fly
		writeD(_walkSpd);
		
		writeF(_multiplier); // movement multiplier
		writeF(1); // attack speed multiplier
		writeF(_summon.getTemplate().getCollisionRadius());
		writeF(_summon.getTemplate().getCollisionHeight());
		writeD(_summon.getWeapon()); // right hand weapon
		writeD(_summon.getArmor()); // body armor
		writeD(0); // left hand weapon
		writeC(_summon.getOwner() != null ? 1 : 0); // when pet is dead and player exit game, pet doesn't show master name
		writeC(1); // isRunning (it is always 1, walking mode is calculated from multiplier)
		writeC(_summon.isInCombat() ? 1 : 0); // attacking 1=true
		writeC(_summon.isAlikeDead() ? 1 : 0); // dead 1=true
		writeC(_isSummoned ? 2 : _val); // 0=teleported 1=default 2=summoned
		writeS(_summon.getName());
		writeS(_summon.getTitle());
		writeD(1);
		writeD(_summon.getOwner() != null ? _summon.getOwner().getPvpFlag() : 0); // 0 = white,2= purple
		writeD(_summon.getOwner() != null ? _summon.getOwner().getKarma() : 0); // karma
		writeD(_curFed); // how fed it is
		writeD(_maxFed); // max fed it can be
		writeD((int) _summon.getCurrentHp()); // current hp
		writeD(_maxHp); // max hp
		writeD((int) _summon.getCurrentMp()); // current mp
		writeD(_maxMp); // max mp
		writeD(_summon.getStat().getSp()); // sp
		writeD(_summon.getLevel()); // lvl
		writeQ(_summon.getStat().getExp());
		writeQ(_summon.getExpForThisLevel()); // 0% absolute value
		writeQ(_summon.getExpForNextLevel()); // 100% absoulte value
		writeD(_summon instanceof L2PetInstance ? _summon.getInventory().getTotalWeight() : 0); // weight
		writeD(_summon.getMaxLoad()); // max weight it can carry
		writeD(_summon.getPAtk(null)); // patk
		writeD(_summon.getPDef(null)); // pdef
		writeD(_summon.getMAtk(null, null)); // matk
		writeD(_summon.getMDef(null, null)); // mdef
		writeD(_summon.getAccuracy()); // accuracy
		writeD(_summon.getEvasionRate(null)); // evasion
		writeD(_summon.getCriticalHit(null, null)); // critical
		writeD(_summon.getStat().getMoveSpeed()); // speed
		writeD(_summon.getPAtkSpd()); // atkspeed
		writeD(_summon.getMAtkSpd()); // casting speed
		
		writeD(_summon.getAbnormalEffect()); // abnormal visual effect
		writeH(_summon.isMountable() ? 1 : 0); // ride button
		writeC(0); // c2
		
		writeH(0); // ??
		writeC(_summon.getOwner() != null ? _summon.getOwner().getTeam() : 0); // team aura (1 = blue, 2 = red)
		writeD(_summon.getSoulShotsPerHit()); // How many soulshots this servitor uses per hit
		writeD(_summon.getSpiritShotsPerHit()); // How many spiritshots this servitor uses per hit
	}
}