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

/**
 * @author Emperorc
 */
public class SagaOfTheHellKnight extends SagasSuperClass
{
	public static String qn1 = "Q095_SagaOfTheHellKnight";
	public static int qnu = 95;
	public static String qna = "Saga of the Hell Knight";
	
	public SagaOfTheHellKnight()
	{
		super(qnu, qn1, qna);
		
		NPC = new int[]
		{
			31582,
			31623,
			31297,
			31297,
			31599,
			31646,
			31647,
			31653,
			31654,
			31655,
			31656,
			31297
		};
		
		Items = new int[]
		{
			7080,
			7532,
			7081,
			7510,
			7293,
			7324,
			7355,
			7386,
			7417,
			7448,
			7086,
			0
		};
		
		Mob = new int[]
		{
			27258,
			27244,
			27263
		};
		
		qn = qn1;
		classid = 91;
		prevclass = 0x06;
		
		X = new int[]
		{
			164650,
			47391,
			47429
		};
		
		Y = new int[]
		{
			-74121,
			-56929,
			-56923
		};
		
		Z = new int[]
		{
			-2871,
			-2370,
			-2383
		};
		
		registerNPCs();
	}
}