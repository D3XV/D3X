/*
 * Copyright 2010 InC-Gaming, nBd. All rights reserved.
 */
package net.sf.l2j.gameserver.xmlfactory;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.xml.parsers.DocumentBuilderFactory;

import net.sf.l2j.gameserver.templates.StatsSet;

import org.w3c.dom.Document;
import org.w3c.dom.Node;

/**
 * @author nBd
 */
public final class XMLParser
{
	private static Logger _log = Logger.getLogger(XMLParser.class.getName());
	
	private final File _file;
	private final ArrayList<StatsSet> _sets;
	private final String _type;
	
	public XMLParser(File file, String type)
	{
		_file = file;
		_type = type;
		_sets = new ArrayList<>();
	}
	
	public List<StatsSet> parseDocument()
	{
		if (_file == null)
		{
			_log.log(Level.WARNING, "XMLParser: Couldn't find the XML File!");
			return null;
		}
		parse();
		
		return _sets;
	}
	
	private Document parse()
	{
		Document doc;
		try
		{
			DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
			factory.setValidating(false);
			factory.setIgnoringComments(true);
			doc = factory.newDocumentBuilder().parse(_file);
		}
		catch (Exception e)
		{
			_log.log(Level.SEVERE, "XMLParser: Error loading file " + _file, e);
			return null;
		}
		
		try
		{
			parseDocument(doc);
		}
		catch (Exception e)
		{
			_log.log(Level.SEVERE, "XMLParser: Error in file " + _file, e);
			return null;
		}
		return doc;
	}
	
	private void parseDocument(Document doc)
	{
		for (Node n = doc.getFirstChild(); n != null; n = n.getNextSibling())
		{
			if ("list".equalsIgnoreCase(n.getNodeName()))
			{
				for (Node d = n.getFirstChild(); d != null; d = d.getNextSibling())
				{
					if (_type.equalsIgnoreCase(d.getNodeName()))
						parseItem(d);
				}
			}
		}
	}
	
	private void parseItem(Node n)
	{
		StatsSet set = new StatsSet();
		
		try
		{
			set.set("id", Integer.parseInt(n.getAttributes().getNamedItem("id").getNodeValue()));
		}
		catch (Exception e)
		{
			// Empty Catch
		}
		
		Node first = n.getFirstChild();
		for (n = first; n != null; n = n.getNextSibling())
		{
			if ("set".equalsIgnoreCase(n.getNodeName()))
				parseBeanSet(n, set);
		}
		_sets.add(set);
	}
	
	private static void parseBeanSet(Node n, StatsSet set)
	{
		if (n == null)
			return;
		
		String name = n.getAttributes().getNamedItem("name").getNodeValue().trim();
		String value = n.getAttributes().getNamedItem("val").getNodeValue().trim();
		
		set.set(name, value);
	}
}