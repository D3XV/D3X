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
package net.sf.l2j.loginserver.network.gameserverpackets;

import java.security.GeneralSecurityException;
import java.security.interfaces.RSAPrivateKey;
import java.util.logging.Logger;

import javax.crypto.Cipher;

import net.sf.l2j.loginserver.network.clientpackets.ClientBasePacket;

/**
 * @author -Wooden-
 */
public class BlowFishKey extends ClientBasePacket
{
	byte[] _key;
	protected static final Logger _log = Logger.getLogger(BlowFishKey.class.getName());
	
	/**
	 * @param decrypt
	 * @param privateKey
	 */
	public BlowFishKey(byte[] decrypt, RSAPrivateKey privateKey)
	{
		super(decrypt);
		int size = readD();
		byte[] tempKey = readB(size);
		try
		{
			byte[] tempDecryptKey;
			Cipher rsaCipher = Cipher.getInstance("RSA/ECB/nopadding");
			rsaCipher.init(Cipher.DECRYPT_MODE, privateKey);
			tempDecryptKey = rsaCipher.doFinal(tempKey);
			// there are nulls before the key we must remove them
			int i = 0;
			int len = tempDecryptKey.length;
			for (; i < len; i++)
			{
				if (tempDecryptKey[i] != 0)
					break;
			}
			_key = new byte[len - i];
			System.arraycopy(tempDecryptKey, i, _key, 0, len - i);
		}
		catch (GeneralSecurityException e)
		{
			_log.severe("Error While decrypting blowfish key (RSA)");
			e.printStackTrace();
		}
	}
	
	public byte[] getKey()
	{
		return _key;
	}
}