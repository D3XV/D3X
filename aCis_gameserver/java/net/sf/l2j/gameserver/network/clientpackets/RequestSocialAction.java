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
import net.sf.l2j.gameserver.ai.CtrlIntention;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.network.SystemMessageId;
import net.sf.l2j.gameserver.network.serverpackets.SocialAction;
import net.sf.l2j.gameserver.util.Util;

public class RequestSocialAction extends L2GameClientPacket
{
	private int _actionId;
	
	@Override
	protected void readImpl()
	{
		_actionId = readD();
	}
	
	@Override
	protected void runImpl()
	{
		final L2PcInstance activeChar = getClient().getActiveChar();
		if (activeChar == null)
			return;
		
		// You cannot do anything while fishing
		if (activeChar.isFishing())
		{
			activeChar.sendPacket(SystemMessageId.CANNOT_DO_WHILE_FISHING_3);
			return;
		}
		
		// check if the actionId is allowed
		if (_actionId < 2 || _actionId > 13)
		{
			Util.handleIllegalPlayerAction(activeChar, activeChar.getName() + " of account " + activeChar.getAccountName() + " requested an internal Social Action.", Config.DEFAULT_PUNISH);
			return;
		}
		
		if (!activeChar.isInStoreMode() && activeChar.getActiveRequester() == null && !activeChar.isAlikeDead() && (!activeChar.isAllSkillsDisabled() || activeChar.isInDuel()) && activeChar.getAI().getIntention() == CtrlIntention.IDLE)
		{
			if (Config.DEBUG)
				_log.fine("Social Action: " + _actionId);
			
			activeChar.broadcastPacket(new SocialAction(activeChar, _actionId));
		}
	}
}