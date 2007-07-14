/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * 	 http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jppf.utils;

import java.io.*;
import java.util.*;

/**
 * Utility class for managing multiple properties files through a common API.
 * Each properties file is lazily loaded, the first time it is accessed.
 * @author Laurent Cohen
 */
public final class PropertyManager
{
	/**
	 * Mapping of string keys to the actual properties files paths managed by this property manager. 
	 */
	private static TypedProperties master = init();
	/**
	 * Mapping of resource keys to actual TypedProperties instances.
	 */
	private static Map<String, TypedProperties> properties = new HashMap<String, TypedProperties>();

	/**
	 * This class should NEVER be instanciated.
	 */
	private PropertyManager()
	{
	}

	/**
	 * Initialization of the property manager by loading the managed resource paths from a properties file.
	 * The &quot;master&quot; properties file lookup is performed in the following sequence:
	 * <ol>
	 * <li>if the <i>master.properties.file</i> system property is defined, it will be used as resource path</li>
	 * <li>if no resource path is explicitly defined, the name &quot;master.properties&quot; is used</li>
	 * <li>if the resource path is found on the file system</li>
	 * <li>otherwise the system will attempt to load it from the classpath</li>
	 * </ol>
	 * @return a TypedProperties instance if the master file could be loaded, null otherwise.
	 */
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
	
	/**
	 * Get the properties mapped to a specified resource key.
	 * @param name the resource key in string format.
	 * @return a TypedProperties instance.
	 */
	private static TypedProperties getResource(String name)
	{
		TypedProperties props = properties.get(name);
		if (props == null)
		{
			String path = master.getString(name);
			if (path != null) props = loadResource(path);
			if (props == null) props = new TypedProperties();
			properties.put(name, props);
		}
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

	/**
	 * Get the string value of a property with a specified name, from a resource with a specified key.
	 * @param resName the name of the resource where the propewrty is to be looked for
	 * @param key the name of the property to look for.
	 * @param defValue a default value to return if the property is not found in the specified resource.
	 * @return the value of the property as a string, or the default value if it is not found.
	 */
	public static String getString(String resName, String key, String defValue)
	{
		return getResource(resName).getString(key, defValue);
	}
	
	/**
	 * Get the string value of a property with a specified name, from a resource with a specified key.
	 * @param resName the name of the resource where the property is to be looked for
	 * @param key the name of the property to look for.
	 * @return the value of the property as a string, or null if it is not found.
	 */
	public static String getString(String resName, String key)
	{
		return getResource(resName).getString(key, null);
	}
	
	/**
	 * Get the integer value of a property with a specified name, from a resource with a specified key.
	 * @param resName the name of the resource where the property is to be looked for
	 * @param key the name of the property to look for.
	 * @param defValue a default value to return if the property is not found in the specified resource.
	 * @return the value of the property as an int, or the default value if it is not found.
	 */
	public static int getInt(String resName, String key, int defValue)
	{
		return getResource(resName).getInt(key, defValue);
	}
	
	/**
	 * Get the integer value of a property with a specified name, from a resource with a specified key.
	 * @param resName the name of the resource where the property is to be looked for
	 * @param key the name of the property to look for.
	 * @return the value of the property as an int, or zero if it is not found.
	 */
	public static int getInt(String resName, String key)
	{
		return getResource(resName).getInt(key, 0);
	}
	
	/**
	 * Get the double precision value of a property with a specified name, from a resource with a specified key.
	 * @param resName the name of the resource where the property is to be looked for
	 * @param key the name of the property to look for.
	 * @param defValue a default value to return if the property is not found in the specified resource.
	 * @return the value of the property as a double, or the default value if it is not found.
	 */
	public static double getDouble(String resName, String key, double defValue)
	{
		return getResource(resName).getDouble(key, defValue);
	}
	
	/**
	 * Get the double precision value of a property with a specified name, from a resource with a specified key.
	 * @param resName the name of the resource where the property is to be looked for
	 * @param key the name of the property to look for.
	 * @return the value of the property as a double, or zero if it is not found.
	 */
	public static double getDouble(String resName, String key)
	{
		return getResource(resName).getDouble(key, 0d);
	}
	
	/**
	 * Get the boolean value of a property with a specified name, from a resource with a specified key.
	 * @param resName the name of the resource where the property is to be looked for
	 * @param key the name of the property to look for.
	 * @param defValue a default value to return if the property is not found in the specified resource.
	 * @return the value of the property as a boolean, or the default value if it is not found.
	 */
	public static boolean getBoolean(String resName, String key, boolean defValue)
	{
		return getResource(resName).getBoolean(key, defValue);
	}
	
	/**
	 * Get the boolean value of a property with a specified name, from a resource with a specified key.
	 * @param resName the name of the resource where the property is to be looked for
	 * @param key the name of the property to look for.
	 * @return the value of the property as a boolean, or false if it is not found.
	 */
	public static boolean getBoolean(String resName, String key)
	{
		return getResource(resName).getBoolean(key, false);
	}
}
