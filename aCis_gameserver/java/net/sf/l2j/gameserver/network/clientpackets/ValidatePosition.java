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
package net.sf.l2j.gameserver.network.clientpackets;

import net.sf.l2j.Config;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.model.zone.ZoneId;
import net.sf.l2j.gameserver.network.serverpackets.GetOnVehicle;
import net.sf.l2j.gameserver.network.serverpackets.ValidateLocation;

public class ValidatePosition extends L2GameClientPacket
{
	private int _x;
	private int _y;
	private int _z;
	private int _heading;
	private int _data;
	
	@Override
	protected void readImpl()
	{
		_x = readD();
		_y = readD();
		_z = readD();
		_heading = readD();
		_data = readD();
	}
	
	@Override
	protected void runImpl()
	{
		final L2PcInstance activeChar = getClient().getActiveChar();
		if (activeChar == null || activeChar.isTeleporting() || activeChar.inObserverMode())
			return;
		
		final int realX = activeChar.getX();
		final int realY = activeChar.getY();
		int realZ = activeChar.getZ();
		
		if (Config.DEVELOPER)
			_log.fine("C(S) pos: " + _x + "(" + realX + ") " + _y + "(" + realY + ") " + _z + "(" + realZ + ") / " + _heading + "(" + activeChar.getHeading() + ")");
		
		if (_x == 0 && _y == 0)
		{
			if (realX != 0) // in this case this seems like a client error
				return;
		}
		
		int dx, dy, dz;
		double diffSq;
		
		if (activeChar.isInBoat())
		{
			if (Config.COORD_SYNCHRONIZE == 2)
			{
				dx = _x - activeChar.getInVehiclePosition().getX();
				dy = _y - activeChar.getInVehiclePosition().getY();
				dz = _z - activeChar.getInVehiclePosition().getZ();
				diffSq = (dx * dx + dy * dy);
				if (diffSq > 250000)
					sendPacket(new GetOnVehicle(activeChar.getObjectId(), _data, activeChar.getInVehiclePosition()));
			}
			return;
		}
		
		if (activeChar.isFalling(_z))
			return; // disable validations during fall to avoid "jumping"
			
		dx = _x - realX;
		dy = _y - realY;
		dz = _z - realZ;
		diffSq = (dx * dx + dy * dy);
		
		if (activeChar.isFlying() || activeChar.isInsideZone(ZoneId.WATER))
		{
			activeChar.setXYZ(realX, realY, _z);
			if (diffSq > 90000) // validate packet, may also cause z bounce if close to land
				activeChar.sendPacket(new ValidateLocation(activeChar));
		}
		else if (diffSq < 360000) // if too large, messes observation
		{
			if (Config.COORD_SYNCHRONIZE == -1) // Only Z coordinate synched to server,
			// mainly used when no geodata but can be used also with geodata
			{
				activeChar.setXYZ(realX, realY, _z);
				return;
			}
			if (Config.COORD_SYNCHRONIZE == 1) // Trusting also client x,y coordinates (should not be used with geodata)
			{
				// Heading changed on client = possible obstacle
				if (!activeChar.isMoving() || !activeChar.validateMovementHeading(_heading))
				{
					// character is not moving, take coordinates from client
					if (diffSq < 2500) // 50*50 - attack won't work fluently if even small differences are corrected
						activeChar.setXYZ(realX, realY, _z);
					else
						activeChar.setXYZ(_x, _y, _z);
				}
				else
					activeChar.setXYZ(realX, realY, _z);
				
				activeChar.setHeading(_heading);
				return;
			}
			// Sync 2 (or other), intended for geodata. Sends a validation packet to client when too far from server calculated real coordinate.
			// Due to geodata/zone errors, some Z axis checks are made. (maybe a temporary solution)
			// Important: this code part must work together with L2Character.updatePosition
			if (Config.GEODATA > 0 && (diffSq > 250000 || Math.abs(dz) > 200))
			{
				if (Math.abs(dz) > 200 && Math.abs(dz) < 1500 && Math.abs(_z - activeChar.getClientZ()) < 800)
				{
					activeChar.setXYZ(realX, realY, _z);
					realZ = _z;
				}
				else
				{
					if (Config.DEVELOPER)
						_log.info(activeChar.getName() + ": Synchronizing position Server --> Client");
					
					activeChar.sendPacket(new ValidateLocation(activeChar));
				}
			}
		}
		
		activeChar.setClientX(_x);
		activeChar.setClientY(_y);
		activeChar.setClientZ(_z);
		activeChar.setClientHeading(_heading); // No real need to validate heading.
	}
}