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
package net.sf.l2j.gameserver.scripting;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.LineNumberReader;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import javax.script.ScriptContext;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineFactory;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;
import javax.script.SimpleScriptContext;

/**
 * Caches script engines and provides funcionality for executing and managing scripts.<BR>
 * @author KenM
 */
public final class L2ScriptEngineManager
{
	private static final Logger _log = Logger.getLogger(L2ScriptEngineManager.class.getName());
	
	public final static File SCRIPT_FOLDER = new File("./data/scripts");
	
	public static L2ScriptEngineManager getInstance()
	{
		return SingletonHolder._instance;
	}
	
	private final Map<String, ScriptEngine> _nameEngines = new HashMap<>();
	private final Map<String, ScriptEngine> _extEngines = new HashMap<>();
	private final List<ScriptManager<?>> _scriptManagers = new LinkedList<>();
	
	private File _currentLoadingScript;
	
	// Configs
	// TODO move to config file
	/**
	 * Informs(logs) the scripts being loaded.<BR>
	 * Apply only when executing script from files.<BR>
	 */
	private final boolean VERBOSE_LOADING = false;
	
	/**
	 * Clean an previous error log(if such exists) for the script being loaded before trying to load.<BR>
	 * Apply only when executing script from files.<BR>
	 */
	private final boolean PURGE_ERROR_LOG = true;
	
	protected L2ScriptEngineManager()
	{
		ScriptEngineManager scriptEngineManager = new ScriptEngineManager();
		List<ScriptEngineFactory> factories = scriptEngineManager.getEngineFactories();
		
		for (ScriptEngineFactory factory : factories)
		{
			try
			{
				ScriptEngine engine = factory.getScriptEngine();
				for (String name : factory.getNames())
				{
					ScriptEngine existentEngine = _nameEngines.get(name);
					
					if (existentEngine != null)
					{
						double engineVer = Double.parseDouble(factory.getEngineVersion());
						double existentEngVer = Double.parseDouble(existentEngine.getFactory().getEngineVersion());
						
						if (engineVer <= existentEngVer)
						{
							continue;
						}
					}
					
					_nameEngines.put(name, engine);
				}
				
				for (String ext : factory.getExtensions())
				{
					if (!ext.equals("java") || factory.getLanguageName().equals("java"))
					{
						_extEngines.put(ext, engine);
					}
				}
			}
			catch (Exception e)
			{
				_log.warning("Failed initializing factory. ");
				e.printStackTrace();
			}
		}
	}
	
	private ScriptEngine getEngineByName(String name)
	{
		return _nameEngines.get(name);
	}
	
	private ScriptEngine getEngineByExtension(String ext)
	{
		return _extEngines.get(ext);
	}
	
	public void executeScriptList(File list) throws IOException
	{
		File file;
		
		if (list.isFile())
		{
			LineNumberReader lnr = new LineNumberReader(new InputStreamReader(new FileInputStream(list)));
			String line;
			
			while ((line = lnr.readLine()) != null)
			{
				String[] parts = line.trim().split("#");
				
				if (parts.length > 0 && !parts[0].startsWith("#") && parts[0].length() > 0)
				{
					line = parts[0];
					
					if (line.endsWith("/**"))
					{
						line = line.substring(0, line.length() - 3);
					}
					else if (line.endsWith("/*"))
					{
						line = line.substring(0, line.length() - 2);
					}
					
					file = new File(SCRIPT_FOLDER, line);
					
					if (file.isDirectory() && parts[0].endsWith("/**"))
					{
						this.executeAllScriptsInDirectory(file, true, 32);
					}
					else if (file.isDirectory() && parts[0].endsWith("/*"))
					{
						this.executeAllScriptsInDirectory(file);
					}
					else if (file.isFile())
					{
						try
						{
							this.executeScript(file);
						}
						catch (ScriptException e)
						{
							reportScriptFileError(file, e);
						}
					}
					else
					{
						_log.warning("Failed loading: (" + file.getCanonicalPath() + ") @ " + list.getName() + ":" + lnr.getLineNumber() + " - Reason: doesnt exists or is not a file.");
					}
				}
			}
			lnr.close();
		}
		else
		{
			throw new IllegalArgumentException("Argument must be an file containing a list of scripts to be loaded");
		}
	}
	
	public void executeAllScriptsInDirectory(File dir)
	{
		this.executeAllScriptsInDirectory(dir, false, 0);
	}
	
	public void executeAllScriptsInDirectory(File dir, boolean recurseDown, int maxDepth)
	{
		this.executeAllScriptsInDirectory(dir, recurseDown, maxDepth, 0);
	}
	
	private void executeAllScriptsInDirectory(File dir, boolean recurseDown, int maxDepth, int currentDepth)
	{
		if (dir.isDirectory())
		{
			for (File file : dir.listFiles())
			{
				if (file.isDirectory() && recurseDown && maxDepth > currentDepth)
				{
					if (VERBOSE_LOADING)
					{
						_log.info("Entering folder: " + file.getName());
					}
					this.executeAllScriptsInDirectory(file, recurseDown, maxDepth, currentDepth + 1);
				}
				else if (file.isFile())
				{
					try
					{
						String name = file.getName();
						int lastIndex = name.lastIndexOf('.');
						String extension;
						if (lastIndex != -1)
						{
							extension = name.substring(lastIndex + 1);
							ScriptEngine engine = getEngineByExtension(extension);
							if (engine != null)
							{
								this.executeScript(engine, file);
							}
						}
					}
					catch (FileNotFoundException e)
					{
						// should never happen
						e.printStackTrace();
					}
					catch (ScriptException e)
					{
						reportScriptFileError(file, e);
						// e.printStackTrace();
					}
				}
			}
		}
		else
		{
			throw new IllegalArgumentException("The argument directory either doesnt exists or is not an directory.");
		}
	}
	
	public void executeScript(File file) throws ScriptException, FileNotFoundException
	{
		String name = file.getName();
		int lastIndex = name.lastIndexOf('.');
		String extension;
		if (lastIndex != -1)
		{
			extension = name.substring(lastIndex + 1);
		}
		else
		{
			throw new ScriptException("Script file (" + name + ") doesnt has an extension that identifies the ScriptEngine to be used.");
		}
		
		ScriptEngine engine = getEngineByExtension(extension);
		if (engine == null)
		{
			throw new ScriptException("No engine registered for extension (" + extension + ")");
		}
		this.executeScript(engine, file);
	}
	
	public void executeScript(String engineName, File file) throws FileNotFoundException, ScriptException
	{
		ScriptEngine engine = getEngineByName(engineName);
		if (engine == null)
		{
			throw new ScriptException("No engine registered with name (" + engineName + ")");
		}
		this.executeScript(engine, file);
	}
	
	public void executeScript(ScriptEngine engine, File file) throws FileNotFoundException, ScriptException
	{
		BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(file)));
		
		if (VERBOSE_LOADING)
		{
			_log.info("Loading Script: " + file.getAbsolutePath());
		}
		
		if (PURGE_ERROR_LOG)
		{
			String name = file.getAbsolutePath() + ".error.log";
			File errorLog = new File(name);
			if (errorLog.isFile())
			{
				errorLog.delete();
			}
		}
		
		ScriptContext context = new SimpleScriptContext();
		context.setAttribute("mainClass", getClassForFile(file).replace('/', '.').replace('\\', '.'), ScriptContext.ENGINE_SCOPE);
		context.setAttribute(ScriptEngine.FILENAME, file.getName(), ScriptContext.ENGINE_SCOPE);
		context.setAttribute("classpath", SCRIPT_FOLDER.getAbsolutePath(), ScriptContext.ENGINE_SCOPE);
		context.setAttribute("sourcepath", SCRIPT_FOLDER.getAbsolutePath(), ScriptContext.ENGINE_SCOPE);
		setCurrentLoadingScript(file);
		try
		{
			engine.eval(reader, context);
		}
		finally
		{
			setCurrentLoadingScript(null);
			engine.getContext().removeAttribute(ScriptEngine.FILENAME, ScriptContext.ENGINE_SCOPE);
			engine.getContext().removeAttribute("mainClass", ScriptContext.ENGINE_SCOPE);
		}
	}
	
	public static String getClassForFile(File script)
	{
		String path = script.getAbsolutePath();
		String scpPath = SCRIPT_FOLDER.getAbsolutePath();
		if (path.startsWith(scpPath))
		{
			int idx = path.lastIndexOf('.');
			return path.substring(scpPath.length() + 1, idx);
		}
		return null;
	}
	
	public static ScriptContext getScriptContext(ScriptEngine engine)
	{
		return engine.getContext();
	}
	
	public ScriptContext getScriptContext(String engineName)
	{
		ScriptEngine engine = getEngineByName(engineName);
		if (engine == null)
		{
			throw new IllegalStateException("No engine registered with name (" + engineName + ")");
		}
		return getScriptContext(engine);
	}
	
	public static Object eval(ScriptEngine engine, String script, ScriptContext context) throws ScriptException
	{
		return context != null ? engine.eval(script, context) : engine.eval(script);
	}
	
	public Object eval(String engineName, String script) throws ScriptException
	{
		return this.eval(engineName, script, null);
	}
	
	public Object eval(String engineName, String script, ScriptContext context) throws ScriptException
	{
		ScriptEngine engine = getEngineByName(engineName);
		if (engine == null)
		{
			throw new ScriptException("No engine registered with name (" + engineName + ")");
		}
		return eval(engine, script, context);
	}
	
	public static Object eval(ScriptEngine engine, String script) throws ScriptException
	{
		return eval(engine, script, null);
	}
	
	public static void reportScriptFileError(File script, ScriptException e)
	{
		String dir = script.getParent();
		String name = script.getName() + ".error.log";
		if (dir != null)
		{
			final File file = new File(dir + "/" + name);
			try (FileOutputStream fos = new FileOutputStream(file))
			{
				String errorHeader = "Error on: " + file.getCanonicalPath() + "\r\nLine: " + e.getLineNumber() + " - Column: " + e.getColumnNumber() + "\r\n\r\n";
				fos.write(errorHeader.getBytes());
				fos.write(e.getMessage().getBytes());
				_log.warning("Failed executing script: " + script.getAbsolutePath() + ". See " + file.getName() + " for details.");
			}
			catch (IOException ioe)
			{
				_log.warning("Failed executing script: " + script.getAbsolutePath() + "\r\n" + e.getMessage() + "Additionally failed when trying to write an error report on script directory. Reason: " + ioe.getMessage());
				ioe.printStackTrace();
			}
		}
		else
		{
			_log.warning("Failed executing script: " + script.getAbsolutePath() + "\r\n" + e.getMessage() + "Additionally failed when trying to write an error report on script directory.");
		}
	}
	
	public void registerScriptManager(ScriptManager<?> manager)
	{
		_scriptManagers.add(manager);
	}
	
	public void removeScriptManager(ScriptManager<?> manager)
	{
		_scriptManagers.remove(manager);
	}
	
	public List<ScriptManager<?>> getScriptManagers()
	{
		return _scriptManagers;
		
	}
	
	/**
	 * @param currentLoadingScript The currentLoadingScript to set.
	 */
	protected void setCurrentLoadingScript(File currentLoadingScript)
	{
		_currentLoadingScript = currentLoadingScript;
	}
	
	/**
	 * @return Returns the currentLoadingScript.
	 */
	protected File getCurrentLoadingScript()
	{
		return _currentLoadingScript;
	}
	
	private static class SingletonHolder
	{
		protected static final L2ScriptEngineManager _instance = new L2ScriptEngineManager();
	}
}