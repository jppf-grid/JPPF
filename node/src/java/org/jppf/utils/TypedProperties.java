/*
 * JPPF.
 * Copyright (C) 2005-2011 JPPF Team.
 * http://www.jppf.org
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jppf.utils;


import java.io.*;
import java.net.*;
import java.util.*;

/**
 * Extension of the <code>java.util.Properties</code> class to handle the conversion of
 * string values to other types.
 * @author Laurent Cohen
 */
public class TypedProperties extends Properties
{
	/**
	 * Default constructor.
	 */
	public TypedProperties()
	{
	}

	/**
	 * Initialize this object with a set of existing properties.
	 * This will copy into the present object all map entries such that both key and value are strings.
	 * @param map the properties to be copied. No reference to this parameter is kept in this TypedProperties object.
	 */
	public TypedProperties(Map<Object, Object> map)
	{
		if (map != null)
		{
			Set<Map.Entry<Object, Object>> entries = map.entrySet();
			for (Map.Entry<Object, Object> entry: entries)
			{
				if ((entry.getKey() instanceof String) && (entry.getValue() instanceof String))
				{
					setProperty((String) entry.getKey(), (String) entry.getValue());
				}
			}
		}
	}

	/**
	 * Get the string value of a property with a specified name.
	 * @param key the name of the property to look for.
	 * @return the value of the property as a string, or null if it is not found.
	 */
	public String getString(String key)
	{
		return getString(key, null);
	}
	
	/**
	 * Get the string value of a property with a specified name.
	 * @param key the name of the property to look for.
	 * @param defValue a default value to return if the property is not found.
	 * @return the value of the property as a string, or the default value if it is not found.
	 */
	public String getString(String key, String defValue)
	{
		String val = getProperty(key);
		return (val == null) ? defValue : val;
	}
	
	/**
	 * Get the integer value of a property with a specified name.
	 * @param key the name of the property to look for.
	 * @return the value of the property as an int, or zero if it is not found.
	 */
	public int getInt(String key)
	{
		return getInt(key, 0);
	}
	
	/**
	 * Get the integer value of a property with a specified name.
	 * @param key the name of the property to look for.
	 * @param defValue a default value to return if the property is not found.
	 * @return the value of the property as an int, or the default value if it is not found.
	 */
	public int getInt(String key, int defValue)
	{
		int intVal = defValue;
		String val = getProperty(key, null);
		if (val != null)
		{
			try
			{
				intVal = Integer.parseInt(val.trim());
			}
			catch(NumberFormatException e)
			{
			}
		}
		return intVal;
	}
	
	/**
	 * Get the long integer value of a property with a specified name.
	 * @param key the name of the property to look for.
	 * @return the value of the property as a long, or zero if it is not found.
	 */
	public long getLong(String key)
	{
		return getLong(key, 0L);
	}
	
	/**
	 * Get the long integer value of a property with a specified name.
	 * @param key the name of the property to look for.
	 * @param defValue a default value to return if the property is not found.
	 * @return the value of the property as a long, or the default value if it is not found.
	 */
	public long getLong(String key, long defValue)
	{
		long longVal = defValue;
		String val = getProperty(key, null);
		if (val != null)
		{
			try
			{
				longVal = Long.parseLong(val.trim());
			}
			catch(NumberFormatException e)
			{
			}
		}
		return longVal;
	}
	
	/**
	 * Get the single precision value of a property with a specified name.
	 * @param key the name of the property to look for.
	 * @return the value of the property as a float, or zero if it is not found.
	 */
	public float getFloat(String key)
	{
		return getFloat(key, 0f);
	}
	
	/**
	 * Get the single precision value of a property with a specified name.
	 * @param key the name of the property to look for.
	 * @param defValue a default value to return if the property is not found.
	 * @return the value of the property as a float, or the default value if it is not found.
	 */
	public float getFloat(String key, float defValue)
	{
		float floatVal = defValue;
		String val = getProperty(key, null);
		if (val != null)
		{
			try
			{
				floatVal = Float.parseFloat(val.trim());
			}
			catch(NumberFormatException e)
			{
			}
		}
		return floatVal;
	}
	
	/**
	 * Get the double precision value of a property with a specified name.
	 * If the key is not found a default value of 0d is returned.
	 * @param key the name of the property to look for.
	 * @return the value of the property as a double, or zero if it is not found.
	 */
	public double getDouble(String key)
	{
		return getDouble(key, 0d);
	}
	
	/**
	 * Get the double precision value of a property with a specified name.
	 * @param key the name of the property to look for.
	 * @param defValue a default value to return if the property is not found.
	 * @return the value of the property as a double, or the default value if it is not found.
	 */
	public double getDouble(String key, double defValue)
	{
		double doubleVal = defValue;
		String val = getProperty(key, null);
		if (val != null)
		{
			try
			{
				doubleVal = Double.parseDouble(val.trim());
			}
			catch(NumberFormatException e)
			{
			}
		}
		return doubleVal;
	}
	
	/**
	 * Get the boolean value of a property with a specified name.
	 * If the key is not found a default value of false is returned.
	 * @param key the name of the property to look for.
	 * @return the value of the property as a boolean, or <code>false</code> if it is not found.
	 */
	public boolean getBoolean(String key)
	{
		return getBoolean(key, false);
	}

	/**
	 * Get the boolean value of a property with a specified name.
	 * @param key the name of the property to look for.
	 * @param defValue a default value to return if the property is not found.
	 * @return the value of the property as a boolean, or the default value if it is not found.
	 */
	public boolean getBoolean(String key, boolean defValue)
	{
		boolean booleanVal = defValue;
		String val = getProperty(key, null);
		if (val != null) booleanVal = Boolean.valueOf(val.trim()).booleanValue();
		return booleanVal;
	}
	
	/**
	 * Get the char value of a property with a specified name.
	 * If the key is not found a default value of ' ' is returned.
	 * @param key the name of the property to look for.
	 * @return the value of the property as a char, or the default value ' ' (space character) if it is not found.
	 */
	public char getChar(String key)
	{
		return getChar(key, ' ');
	}
	
	/**
	 * Get the char value of a property with a specified name.
	 * If the value has more than one character, the first one will be used.
	 * @param key the name of the property to look for.
	 * @param defValue a default value to return if the property is not found.
	 * @return the value of the property as a char, or the default value if it is not found.
	 */
	public char getChar(String key, char defValue)
	{
		char charVal = defValue;
		String val = getProperty(key, null);
		if ((val != null) && (val.length() > 0)) charVal = val.charAt(0);
		return charVal;
	}
	
	/**
	 * Get the value of a property with the specified name as a set of a properties.
	 * @param key the name of the property to look for.
	 * Its value is the path to another properties file. Relative paths are evaluated against the current application directory. 
	 * @return the value of the property as another set of properties, or null if it is not found.
	 */
	public TypedProperties getProperties(String key)
	{
		return getProperties(key, null);
	}

	/**
	 * Get the value of a property with the specified name as a set of properties.
	 * @param key the name of the property to look for.
	 * Its value is the path to another properties file. Relative paths are evaluated against the current application directory. 
	 * @param def a default value to return if the property is not found.
	 * @return the value of the property as another set of properties, or the default value if it is not found.
	 */
	public TypedProperties getProperties(String key, TypedProperties def)
	{
		String path = getString(key);
		File file = new File(path);
		if (!file.exists()) return def;
		TypedProperties res = new TypedProperties();
		try
		{
			res.load(new BufferedInputStream(new FileInputStream(file)));
		}
		catch(IOException e)
		{
			return def;
		}
		return res;
	}

	/**
	 * Get the value of a property with the specified name as an {@link InetAddress}.
	 * @param key the name of the property to retrieve.
	 * @return the property as an {@link InetAddress} instance, or null if the property is not defined or the host doesn't exist. 
	 */
	public InetAddress getInetAddress(String key)
	{
		return getInetAddress(key, null);
	}

	/**
	 * Get the value of a property with the specified name as an {@link InetAddress}.
	 * @param key the name of the property to retrieve.
	 * @param def the default value to use if the property is not defined.
	 * @return the property as an {@link InetAddress} instance, or the sdpecified default value if the property is not defined. 
	 */
	public InetAddress getInetAddress(String key, InetAddress def)
	{
		String val = getString(key);
		if (val == null) return def;
		try
		{
			return InetAddress.getByName(val);
		}
		catch(UnknownHostException e)
		{
			return def;
		}
	}

	/**
	 * Convert this set of properties into a string.
	 * @return a representation of this object as a string.
	 */
	public String asString()
	{
		StringBuilder sb = new StringBuilder();
		Set<Map.Entry<Object, Object>> entries = entrySet();
		for (Map.Entry<Object, Object> entry: entries)
		{
			if ((entry.getKey() instanceof String) && (entry.getValue() instanceof String))
			{
				sb.append(entry.getKey()).append(" = ").append(entry.getValue()).append("\n");
			}
		}
		return sb.toString();
	}

	/**
	 * Load this set of properties from a string.
	 * @param source the string to load from.
	 * @throws IOException if any error occurs.
	 */
	public void loadString(String source) throws IOException
	{
		if (source == null) return;
		ByteArrayInputStream bais = new ByteArrayInputStream(source.getBytes());
		load(bais);
		bais.close();
	}
}
