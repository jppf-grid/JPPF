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

import java.lang.reflect.Method;
import java.util.*;

/**
 * Collection of static utility methods for dealing with reflection-based APIs.
 * @author Laurent Cohen
 */
public class ReflectionUtils
{
	/**
	 * Generates a string that displays all return values for all getters of an object.
	 * This method uses a new line character a field separator.
	 * @param o the object whose getter return values are to be dumped into a string.
	 * @return a string with the classname, hashcode, and the value of
	 * each attribute that has a corresponding getter.
	 */
	public static String dumpObject(Object o)
	{
		return dumpObject(o, "\n");
	}

	/**
	 * Generates a string that displays all return values for all getters of an object.
	 * @param o the object whose getter return values are to be dumped into a string.
	 * @param separator separator between field values, like a comma or a new line.
	 * @return a string with the classname, hashcode, and the value of
	 * each attribute that has a corresponding getter.
	 */
	public static String dumpObject(Object o, String separator)
	{
		//String separator = "\n";
		if (o == null) return "null";
		Class clazz = o.getClass();
		StringBuilder sb = new StringBuilder();

		sb.append(clazz.getName()).append("@").append(Integer.toHexString(o.hashCode())).append(separator);
		Method[] methods = clazz.getMethods();
		Method getter = null;
		// we want the attributes in ascending alphabetical order
		SortedMap<String, Object> attrMap = new TreeMap<String, Object>();
		for (int i=0; i<methods.length; i++)
		{
			if (isGetter(methods[i]))
			{
				getter = methods[i];
				String attrName = null;
				if (getter.getName().startsWith("get"))
					attrName = getter.getName().substring(3);
				else attrName = getter.getName().substring(2);
				attrName =
					attrName.substring(0, 1).toLowerCase() + attrName.substring(1);
				Object value = null;
				try
				{
					value = getter.invoke(o, (Object[])null);
					if (value == null) value = "null";
				}
				catch(Exception e)
				{
					value = "*Error: "+e.getMessage()+"*";
				}
				attrMap.put(attrName, value);
			}
		}
		Iterator<Map.Entry<String, Object>> it = attrMap.entrySet().iterator();
		while (it.hasNext())
		{
			Map.Entry<String, Object> entry = it.next();
			sb.append(entry.getKey()).append("=").append(entry.getValue());
			if (it.hasNext()) sb.append(separator);
		}
		return sb.toString();
	}

	/**
	 * Determines whether a method is a getter (accessor), according to Sun's naming conventions.
	 * @param meth the method to analyse.
	 * @return true if the method is a getter, false otherwise.
	 */
	public static boolean isGetter(Method meth)
	{
		Class type = meth.getReturnType();
		if (Void.TYPE.equals(type)) return false;
		if (!meth.getName().startsWith("get") && !meth.getName().startsWith("is"))
			return false;
		if (meth.getName().startsWith("is"))
		{
			if (!Boolean.class.equals(type) && !Boolean.TYPE.equals(type))
				return false;
		}
		Class[] paramTypes = meth.getParameterTypes();
		if ((paramTypes != null) && (paramTypes.length > 0)) return false;
		return true;
	}

	/**
	 * Determines whether a method is a setter (mutator), according to Sun's naming conventions.
	 * @param meth the method to analyse.
	 * @return true if the method is a setter, false otherwise.
	 */
	public static boolean isSetter(Method meth)
	{
		Class type = meth.getReturnType();
		if (!Void.TYPE.equals(type)) return false;
		if (!meth.getName().startsWith("set")) return false;
		Class[] paramTypes = meth.getParameterTypes();
		if ((paramTypes == null) || (paramTypes.length != 1)) return false;
		return true;
	}

	/**
	 * Get a getter with a given name from a class.
	 * @param clazz the class enclosing the getter.
	 * @param name the name of the getter to look for.
	 * @return a <code>Method</code> object, or null if the class has no getter with the specfied name.
	 */
	public static Method getGetter(Class clazz, String name)
	{
		Method[] methods = clazz.getMethods();
		Method getter = null;
		for (int i=0; i<methods.length; i++)
		{
			if (isGetter(methods[i]) && name.equals(methods[i].getName()))
			{
				getter = methods[i];
				break;
			}
		}
		return getter;
	}

	/**
	 * Get a setter with a given name from a class.
	 * @param clazz the class enclosing the setter.
	 * @param name the name of the setter to look for.
	 * @return a <code>Method</code> object, or null if the class has no setter with the specfied name.
	 */
	public static Method getSetter(Class clazz, String name)
	{
		Method[] methods = clazz.getMethods();
		Method setter = null;
		for (int i=0; i<methods.length; i++)
		{
			if (isSetter(methods[i]) && name.equals(methods[i].getName()))
			{
				setter = methods[i];
				break;
			}
		}
		return setter;
	}

	/**
	 * Get a getter corresponding to a specified instance variable name from a class.
	 * @param clazz the class enclosing the instance variable.
	 * @param attrName the name of the instance variable to look for.
	 * @return a <code>Method</code> object, or null if the class has no getter for the specfied
	 * instance variable name.
	 */
	public static Method getGetterForAttribute(Class clazz, String attrName)
	{
		String basename =
			attrName.substring(0, 1).toUpperCase() + attrName.substring(1);
		Method method = getGetter(clazz, "get"+basename);
		if (method == null) method = getGetter(clazz, "is"+basename);
		return method;
	}

	/**
	 * Get a setter corresponding to a specified instance variable name from a class.
	 * @param clazz the class enclosing the instance variable.
	 * @param attrName the name of the instance variable to look for.
	 * @return a <code>Method</code> object, or null if the class has no setter for the specfied
	 * instance variable name.
	 */
	public static Method getSetterForAttribute(Class clazz, String attrName)
	{
		String basename =
			attrName.substring(0, 1).toUpperCase() + attrName.substring(1);
		return getSetter(clazz, "set"+basename);
	}
	
	/**
	 * Obtain all the getters or setters of a specified class.
	 * @param clazz the class to get the methods from.
	 * @param getters if true, indicates that the getters should be looked up, otherwise it should be the setters.
	 * @return an array of <code>Method</code> instances.
	 */
	public static Method[] getAllBeanMethods(Class clazz, boolean getters)
	{
		List<Method> methodList = new ArrayList<Method>();
		Method[] allMethods = clazz.getMethods();
		for (Method meth: allMethods)
		{
			if ((getters && isGetter(meth)) || (!getters && isSetter(meth)))
			{
				methodList.add(meth);
			}
		}
		return methodList.toArray(new Method[0]);
	}

	/**
	 * Perform a deep copy of an object.
	 * This method uses reflection to perform the copy through public accessors and mutators (getters and setters).
	 * @param o the object to copy.
	 * @return an object whose state is a copy of that of the input object.
	 */
	public static Object deepCopy(Object o)
	{
		
		return null;
	}
}
