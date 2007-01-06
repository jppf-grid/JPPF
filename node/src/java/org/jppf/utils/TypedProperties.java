/*
 * Java Parallel Processing Framework.
 * Copyright (C) 2005-2007 JPPF Team.
 * http://www.jppf.org
 *
 * This library is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as published by the
 * Free Software Foundation; either version 2.1 of the License, or (at your
 * option) any later version.
 *
 * This library is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this library; if not, write to the Free Software Foundation,
 * Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA
 */
package org.jppf.utils;


import java.util.Properties;

/**
 * Extension of the <code>java.util.Properties</code> class to handle the conversion of
 * string values to other types.
 * @author Laurent Cohen
 */
public class TypedProperties extends Properties
{
	/**
	 * Get the string value of a property with a specified name.
	 * @param key the name of the property to look for.
	 * @param defValue a default value to return if the property is not found.
	 * @return the value of the property as a string, or the default value if it is not found.
	 */
	public String getString(String key, String defValue)
	{
		String val = getProperty(key);
		if (val == null) return defValue;
		return val;
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
				intVal = Integer.parseInt(val);
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
				longVal = Long.parseLong(val);
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
				floatVal = Float.parseFloat(val);
			}
			catch(NumberFormatException e)
			{
			}
		}
		return floatVal;
	}
	
	/**
	 * Get the single precision value of a property with a specified name.
	 * @param key the name of the property to look for.
	 * @return the value of the property as a float, or zero if it is not found.
	 */
	public double getFloat(String key)
	{
		return getFloat(key, 0f);
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
				doubleVal = Double.parseDouble(val);
			}
			catch(NumberFormatException e)
			{
			}
		}
		return doubleVal;
	}
	
	/**
	 * Get the double precision value of a property with a specified name.
	 * @param key the name of the property to look for.
	 * @return the value of the property as a double, or zero if it is not found.
	 */
	public double getDouble(String key)
	{
		return getDouble(key, 0d);
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
		if (val != null) booleanVal = Boolean.valueOf(val).booleanValue();
		return booleanVal;
	}
	
	/**
	 * Get the boolean value of a property with a specified name.
	 * @param key the name of the property to look for.
	 * @return the value of the property as a boolean, or false if it is not found.
	 */
	public boolean getBoolean(String key)
	{
		return getBoolean(key, false);
	}
}
