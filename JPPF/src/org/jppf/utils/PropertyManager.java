/*
 * Java Parallel Processing Framework.
 * Copyright (C) 2005 Laurent Cohen.
 * lcohen@osp-chicago.com
 * 
 * This program is free software; you can redistribute it and/or modify it under the terms of
 * the GNU General Public License as published by the Free Software Foundation;
 * either version 2 of the License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License along with this program;
 * if not, write to the Free Software Foundation, Inc.,
 * 59 Temple Place, Suite 330,
 * Boston, MA 02111-1307 USA
 */
package org.jppf.utils;

import java.io.*;
import java.util.*;

/**
 * Utility class for managing multiple properties files through a common API.
 * @author Laurent Cohen
 */
public class PropertyManager
{
	private static TypedProperties master = init();
	private static Map<String, TypedProperties> properties = new HashMap<String, TypedProperties>();
	
	private static TypedProperties init()
	{
		TypedProperties props = null;
		try
		{
			props = new TypedProperties();
			String masterFile = System.getProperty("master.properties.file", "master.properties");
			InputStream is = null;
			try
			{
				is = new FileInputStream(masterFile);
			}
			catch(IOException e) {}
			if (is == null)
			{
				is = PropertyManager.class.getClassLoader().getResourceAsStream(masterFile);
			}
			if (is == null) throw new Exception("Could not find the master properties file");
			props.load(is);
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}
		return props;
	}
	
	private static TypedProperties getResource(String name)
	{
		TypedProperties props = properties.get(name);
		if (props == null)
		{
			String path = master.getProperty(name);
			if (path != null) props = loadResource(path);
		}
		if (props == null) props = new TypedProperties();
		return props;
	}

	/**
	 * Loads a resource file. First attempts to load it from the classpath,
	 * then from the file system if it is not found in the classpath. 
	 * @param path path of the resource file to load
	 * @return a Properties instance
	 */
	private static TypedProperties loadResource(String path)
	{
		TypedProperties props = null;
		// first search in the class path
		InputStream is = null;
		try
		{
			is = PropertyManager.class.getClassLoader().getResourceAsStream(path);
			// if not found in the classpath, search in the file system
			if (is == null) is = new FileInputStream(path);
			if (is != null)
			{
				props = new TypedProperties();
				props.load(is);
			}
		}
		catch(IOException e)
		{
			e.printStackTrace();
		}
		
		return props;
	}

	private static void addResource(String key, InputStream is)
	{
		try
		{
			TypedProperties props = new TypedProperties();
			props.load(is);
			addResource(key, props);
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}
	}

	private static void addResource(String key, TypedProperties props)
	{
		properties.put(key, props);
	}

	public static String getString(String resName, String key, String defValue)
	{
		return getResource(resName).getString(resName, key, defValue);
	}
	
	public static String getString(String resName, String key)
	{
		return getResource(resName).getString(resName, key, null);
	}
	
	public static int getInt(String resName, String key, int defValue)
	{
		return getResource(resName).getInt(resName, key, defValue);
	}
	
	public static int getInt(String resName, String key)
	{
		return getResource(resName).getInt(resName, key, 0);
	}
	
	public static double getDouble(String resName, String key, double defValue)
	{
		return getResource(resName).getDouble(resName, key, defValue);
	}
	
	public static double getDouble(String resName, String key)
	{
		return getResource(resName).getDouble(resName, key, 0d);
	}
	
	public static boolean getBoolean(String resName, String key, boolean defValue)
	{
		return getResource(resName).getBoolean(resName, key, defValue);
	}
	
	public static boolean getBoolean(String resName, String key)
	{
		return getResource(resName).getBoolean(resName, key, false);
	}
}
