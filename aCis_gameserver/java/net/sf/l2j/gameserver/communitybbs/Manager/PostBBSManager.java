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

import java.text.DateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.StringTokenizer;

import net.sf.l2j.gameserver.communitybbs.BB.Forum;
import net.sf.l2j.gameserver.communitybbs.BB.Post;
import net.sf.l2j.gameserver.communitybbs.BB.Topic;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.util.StringUtil;

public class PostBBSManager extends BaseBBSManager
{
	private final Map<Topic, Post> _postByTopic;
	
	public static PostBBSManager getInstance()
	{
		return SingletonHolder._instance;
	}
	
	protected PostBBSManager()
	{
		_postByTopic = new HashMap<>();
	}
	
	@Override
	public void parseCmd(String command, L2PcInstance activeChar)
	{
		if (command.startsWith("_bbsposts;read;"))
		{
			StringTokenizer st = new StringTokenizer(command, ";");
			st.nextToken();
			st.nextToken();
			
			int idf = Integer.parseInt(st.nextToken());
			int idp = Integer.parseInt(st.nextToken());
			
			String index = null;
			if (st.hasMoreTokens())
				index = st.nextToken();
			
			int ind = 0;
			if (index == null)
				ind = 1;
			else
				ind = Integer.parseInt(index);
			
			showPost((TopicBBSManager.getInstance().getTopicByID(idp)), ForumsBBSManager.getInstance().getForumByID(idf), activeChar, ind);
		}
		else if (command.startsWith("_bbsposts;edit;"))
		{
			StringTokenizer st = new StringTokenizer(command, ";");
			st.nextToken();
			st.nextToken();
			
			int idf = Integer.parseInt(st.nextToken());
			int idt = Integer.parseInt(st.nextToken());
			int idp = Integer.parseInt(st.nextToken());
			
			showEditPost((TopicBBSManager.getInstance().getTopicByID(idt)), ForumsBBSManager.getInstance().getForumByID(idf), activeChar, idp);
		}
		else
			super.parseCmd(command, activeChar);
	}
	
	@Override
	public void parseWrite(String ar1, String ar2, String ar3, String ar4, String ar5, L2PcInstance activeChar)
	{
		StringTokenizer st = new StringTokenizer(ar1, ";");
		int idf = Integer.parseInt(st.nextToken());
		int idt = Integer.parseInt(st.nextToken());
		int idp = Integer.parseInt(st.nextToken());
		
		final Forum forum = ForumsBBSManager.getInstance().getForumByID(idf);
		if (forum == null)
		{
			separateAndSend("<html><body><br><br><center>The forum named '" + idf + "' doesn't exist.</center></body></html>", activeChar);
			return;
		}
		
		final Topic topic = forum.getTopic(idt);
		if (topic == null)
		{
			separateAndSend("<html><body><br><br><center>The topic named '" + idt + "' doesn't exist.</center></body></html>", activeChar);
			return;
		}
		
		final Post post = getPostByTopic(topic);
		if (post.getCPost(idp) == null)
		{
			separateAndSend("<html><body><br><br><center>The post named '" + idp + "' doesn't exist.</center></body></html>", activeChar);
			return;
		}
		
		post.getCPost(idp).postTxt = ar4;
		post.updateText(idp);
		parseCmd("_bbsposts;read;" + forum.getID() + ";" + topic.getID(), activeChar);
	}
	
	public Post getPostByTopic(Topic t)
	{
		Post post = _postByTopic.get(t);
		if (post == null)
		{
			post = load(t);
			_postByTopic.put(t, post);
		}
		return post;
	}
	
	public void delPostByTopic(Topic t)
	{
		_postByTopic.remove(t);
	}
	
	public void addPostByTopic(Post p, Topic t)
	{
		if (_postByTopic.get(t) == null)
			_postByTopic.put(t, p);
	}
	
	private static Post load(Topic t)
	{
		return new Post(t);
	}
	
	private void showEditPost(Topic topic, Forum forum, L2PcInstance activeChar, int idp)
	{
		if (forum == null || topic == null)
		{
			separateAndSend("<html><body><br><br><center>This forum and/or topic don't exit.</center></body></html>", activeChar);
			return;
		}
		
		Post p = getPostByTopic(topic);
		if (p == null)
		{
			separateAndSend("<html><body><br><br><center>This post doesn't exist.</center></body></html>", activeChar);
			return;
		}
		
		showHtmlEditPost(topic, activeChar, forum, p);
	}
	
	private void showPost(Topic topic, Forum forum, L2PcInstance activeChar, int ind)
	{
		if (forum == null || topic == null)
			separateAndSend("<html><body><br><br><center>This forum and/or topic don't exist.</center></body></html>", activeChar);
		else if (forum.getType() == Forum.MEMO)
			showMemoPost(topic, activeChar, forum);
		else
			separateAndSend("<html><body><br><br><center>The forum named '" + forum.getName() + "' isn't implemented.</center></body></html>", activeChar);
	}
	
	private static void showHtmlEditPost(Topic topic, L2PcInstance activeChar, Forum forum, Post p)
	{
		final String html = StringUtil.concat("<html>" + "<body><br><br>" + "<table border=0 width=610><tr><td width=10></td><td width=600 align=left>" + "<a action=\"bypass _bbshome\">HOME</a>&nbsp;>&nbsp;<a action=\"bypass _bbsmemo\">", forum.getName(), " Form</a>" + "</td></tr>" + "</table>" + "<img src=\"L2UI.squareblank\" width=\"1\" height=\"10\">" + "<center>" + "<table border=0 cellspacing=0 cellpadding=0>" + "<tr><td width=610><img src=\"sek.cbui355\" width=\"610\" height=\"1\"><br1><img src=\"sek.cbui355\" width=\"610\" height=\"1\"></td></tr>" + "</table>" + "<table fixwidth=610 border=0 cellspacing=0 cellpadding=0>" + "<tr><td><img src=\"l2ui.mini_logo\" width=5 height=20></td></tr>" + "<tr>" + "<td><img src=\"l2ui.mini_logo\" width=5 height=1></td>" + "<td align=center FIXWIDTH=60 height=29>&$413;</td>" + "<td FIXWIDTH=540>", topic.getName(), "</td>" + "<td><img src=\"l2ui.mini_logo\" width=5 height=1></td>" + "</tr></table>" + "<table fixwidth=610 border=0 cellspacing=0 cellpadding=0>" + "<tr><td><img src=\"l2ui.mini_logo\" width=5 height=10></td></tr>" + "<tr>" + "<td><img src=\"l2ui.mini_logo\" width=5 height=1></td>" + "<td align=center FIXWIDTH=60 height=29 valign=top>&$427;</td>" + "<td align=center FIXWIDTH=540><MultiEdit var =\"Content\" width=535 height=313></td>" + "<td><img src=\"l2ui.mini_logo\" width=5 height=1></td>" + "</tr>" + "<tr><td><img src=\"l2ui.mini_logo\" width=5 height=10></td></tr>" + "</table>" + "<table fixwidth=610 border=0 cellspacing=0 cellpadding=0>" + "<tr><td><img src=\"l2ui.mini_logo\" width=5 height=10></td></tr>" + "<tr>" + "<td><img src=\"l2ui.mini_logo\" width=5 height=1></td>" + "<td align=center FIXWIDTH=60 height=29>&nbsp;</td>" + "<td align=center FIXWIDTH=70><button value=\"&$140;\" action=\"Write Post ", String.valueOf(forum.getID()), ";", String.valueOf(topic.getID()), ";0 _ Content Content Content\" back=\"l2ui_ch3.smallbutton2_down\" width=65 height=20 fore=\"l2ui_ch3.smallbutton2\" ></td>" + "<td align=center FIXWIDTH=70><button value = \"&$141;\" action=\"bypass _bbsmemo\" back=\"l2ui_ch3.smallbutton2_down\" width=65 height=20 fore=\"l2ui_ch3.smallbutton2\"> </td>" + "<td align=center FIXWIDTH=400>&nbsp;</td>" + "<td><img src=\"l2ui.mini_logo\" width=5 height=1></td>" + "</tr></table>" + "</center>" + "</body>" + "</html>");
		send1001(html, activeChar);
		send1002(activeChar, p.getCPost(0).postTxt, topic.getName(), DateFormat.getInstance().format(new Date(topic.getDate())));
	}
	
	private void showMemoPost(Topic topic, L2PcInstance activeChar, Forum forum)
	{
		Post p = getPostByTopic(topic);
		Locale locale = Locale.getDefault();
		DateFormat dateFormat = DateFormat.getDateInstance(DateFormat.FULL, locale);
		
		String mes = p.getCPost(0).postTxt.replace(">", "&gt;");
		mes = mes.replace("<", "&lt;");
		mes = mes.replace("\n", "<br1>");
		
		final String html = StringUtil.concat("<html><body><br><br>" + "<table border=0 width=610><tr><td width=10></td><td width=600 align=left>" + "<a action=\"bypass _bbshome\">HOME</a>&nbsp;>&nbsp;<a action=\"bypass _bbsmemo\">Memo Form</a>" + "</td></tr>" + "</table>" + "<img src=\"L2UI.squareblank\" width=\"1\" height=\"10\">" + "<center>" + "<table border=0 cellspacing=0 cellpadding=0 bgcolor=333333>" + "<tr><td height=10></td></tr>" + "<tr>" + "<td fixWIDTH=55 align=right valign=top>&$413; : &nbsp;</td>" + "<td fixWIDTH=380 valign=top>", topic.getName(), "</td>" + "<td fixwidth=5></td>" + "<td fixwidth=50></td>" + "<td fixWIDTH=120></td>" + "</tr>" + "<tr><td height=10></td></tr>" + "<tr>" + "<td align=right><font color=\"AAAAAA\" >&$417; : &nbsp;</font></td>" + "<td><font color=\"AAAAAA\">", topic.getOwnerName() + "</font></td>" + "<td></td>" + "<td><font color=\"AAAAAA\">&$418; :</font></td>" + "<td><font color=\"AAAAAA\">", dateFormat.format(p.getCPost(0).postDate), "</font></td>" + "</tr>" + "<tr><td height=10></td></tr>" + "</table>" + "<br>" + "<table border=0 cellspacing=0 cellpadding=0>" + "<tr>" + "<td fixwidth=5></td>" + "<td FIXWIDTH=600 align=left>", mes, "</td>" + "<td fixqqwidth=5></td>" + "</tr>" + "</table>" + "<br>" + "<img src=\"L2UI.squareblank\" width=\"1\" height=\"5\">" + "<img src=\"L2UI.squaregray\" width=\"610\" height=\"1\">" + "<img src=\"L2UI.squareblank\" width=\"1\" height=\"5\">" + "<table border=0 cellspacing=0 cellpadding=0 FIXWIDTH=610>" + "<tr>" + "<td width=50>" + "<button value=\"&$422;\" action=\"bypass _bbsmemo\" back=\"l2ui_ch3.smallbutton2_down\" width=65 height=20 fore=\"l2ui_ch3.smallbutton2\">" + "</td>" + "<td width=560 align=right><table border=0 cellspacing=0><tr>" + "<td FIXWIDTH=300></td><td><button value = \"&$424;\" action=\"bypass _bbsposts;edit;", String.valueOf(forum.getID()), ";", String.valueOf(topic.getID()), ";0\" back=\"l2ui_ch3.smallbutton2_down\" width=65 height=20 fore=\"l2ui_ch3.smallbutton2\" ></td>&nbsp;" + "<td><button value = \"&$425;\" action=\"bypass _bbstopics;del;", String.valueOf(forum.getID()), ";", String.valueOf(topic.getID()), "\" back=\"l2ui_ch3.smallbutton2_down\" width=65 height=20 fore=\"l2ui_ch3.smallbutton2\" ></td>&nbsp;" + "<td><button value = \"&$421;\" action=\"bypass _bbstopics;crea;", String.valueOf(forum.getID()), "\" back=\"l2ui_ch3.smallbutton2_down\" width=65 height=20 fore=\"l2ui_ch3.smallbutton2\" ></td>&nbsp;" + "</tr></table>" + "</td>" + "</tr>" + "</table>" + "<br>" + "<br>" + "<br></center>" + "</body>" + "</html>");
		separateAndSend(html, activeChar);
	}
	
	private static class SingletonHolder
	{
		protected static final PostBBSManager _instance = new PostBBSManager();
	}
}