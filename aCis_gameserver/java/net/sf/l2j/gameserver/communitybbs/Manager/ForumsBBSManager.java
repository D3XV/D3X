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
package net.sf.l2j.gameserver.communitybbs.Manager;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.logging.Level;

import net.sf.l2j.L2DatabaseFactory;
import net.sf.l2j.gameserver.communitybbs.BB.Forum;

public class ForumsBBSManager extends BaseBBSManager
{
	private final List<Forum> _table;
	private int _lastid = 1;
	
	public static ForumsBBSManager getInstance()
	{
		return SingletonHolder._instance;
	}
	
	protected ForumsBBSManager()
	{
		_table = new CopyOnWriteArrayList<>();
		
		try (Connection con = L2DatabaseFactory.getInstance().getConnection())
		{
			PreparedStatement statement = con.prepareStatement("SELECT forum_id FROM forums WHERE forum_type=0");
			ResultSet result = statement.executeQuery();
			
			while (result.next())
				addForum(new Forum(result.getInt("forum_id"), null));
			
			result.close();
			statement.close();
		}
		catch (Exception e)
		{
			_log.log(Level.WARNING, "Data error on Forum (root): " + e.getMessage(), e);
		}
	}
	
	public void initRoot()
	{
		for (Forum f : _table)
			f.vload();
		
		_log.info("Loaded " + _table.size() + " forums. Last forum id used: " + _lastid);
	}
	
	public void addForum(Forum ff)
	{
		if (ff == null)
			return;
		
		_table.add(ff);
		
		if (ff.getID() > _lastid)
			_lastid = ff.getID();
	}
	
	public Forum getForumByName(String Name)
	{
		for (Forum f : _table)
		{
			if (f.getName().equals(Name))
				return f;
		}
		return null;
	}
	
	public Forum createNewForum(String name, Forum parent, int type, int perm, int oid)
	{
		Forum forum = new Forum(name, parent, type, perm, oid);
		forum.insertIntoDb();
		
		return forum;
	}
	
	public int getANewID()
	{
		return ++_lastid;
	}
	
	public Forum getForumByID(int id)
	{
		for (Forum f : _table)
		{
			if (f.getID() == id)
				return f;
		}
		return null;
	}
	
	private static class SingletonHolder
	{
		protected static final ForumsBBSManager _instance = new ForumsBBSManager();
	}
}