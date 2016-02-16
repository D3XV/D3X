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
package net.sf.l2j.gameserver.skills.basefuncs;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.logging.Level;
import java.util.logging.Logger;

import net.sf.l2j.gameserver.skills.Env;
import net.sf.l2j.gameserver.skills.Stats;
import net.sf.l2j.gameserver.skills.conditions.Condition;

/**
 * @author mkizub
 */
public final class FuncTemplate
{
	protected static final Logger _log = Logger.getLogger(FuncTemplate.class.getName());
	
	public Condition attachCond;
	public Condition applayCond;
	public final Class<?> func;
	public final Constructor<?> constructor;
	public final Stats stat;
	public final int order;
	public final Lambda lambda;
	
	public FuncTemplate(Condition pAttachCond, Condition pApplayCond, String pFunc, Stats pStat, int pOrder, Lambda pLambda)
	{
		attachCond = pAttachCond;
		applayCond = pApplayCond;
		stat = pStat;
		order = pOrder;
		lambda = pLambda;
		
		try
		{
			func = Class.forName("net.sf.l2j.gameserver.skills.basefuncs.Func" + pFunc);
		}
		catch (ClassNotFoundException e)
		{
			throw new RuntimeException(e);
		}
		
		try
		{
			constructor = func.getConstructor(new Class[]
			{
				Stats.class, // stats to update
				Integer.TYPE, // order of execution
				Object.class, // owner
				Lambda.class
			// value for function
			});
		}
		catch (NoSuchMethodException e)
		{
			throw new RuntimeException(e);
		}
	}
	
	public Func getFunc(Env env, Object owner)
	{
		if (attachCond != null && !attachCond.test(env))
			return null;
		
		try
		{
			Func f = (Func) constructor.newInstance(stat, order, owner, lambda);
			if (applayCond != null)
				f.setCondition(applayCond);
			return f;
		}
		catch (IllegalAccessException e)
		{
			_log.log(Level.WARNING, "", e);
			return null;
		}
		catch (InstantiationException e)
		{
			_log.log(Level.WARNING, "", e);
			return null;
		}
		catch (InvocationTargetException e)
		{
			_log.log(Level.WARNING, "", e);
			return null;
		}
	}
}