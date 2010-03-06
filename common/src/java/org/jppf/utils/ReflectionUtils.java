/*
 * JPPF.
 * Copyright (C) 2005-2010 JPPF Team.
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

import java.lang.annotation.Annotation;
import java.lang.reflect.*;
import java.util.*;

import org.jppf.server.protocol.JPPFRunnable;

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
				attrName = getter.getName().substring(getter.getName().startsWith("get") ? 3 : 2);
				attrName = attrName.substring(0, 1).toLowerCase() + attrName.substring(1);
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

	/**
	 * Determines whether a class has a JPPF-annotated method and can be executed as a task.
	 * @param clazz the class to check.
	 * @return true if the class can be executed as a task, false otherwise.
	 */
	public static boolean isJPPFAnnotated(Class<?> clazz)
	{
		return getJPPFAnnotatedElement(clazz) != null;
	}

	/**
	 * Determines whether a class has a JPPF-annotated method and can be executed as a task.
	 * @param clazz the class to check.
	 * @return true if the class can be executed as a task, false otherwise.
	 */
	public static AnnotatedElement getJPPFAnnotatedElement(Class<?> clazz)
	{
		if (clazz == null) return null;
		for (Method m: clazz.getDeclaredMethods())
		{
			if (isJPPFAnnotated(m)) return m;
		}
		for (Constructor c: clazz.getDeclaredConstructors())
		{
			if (isJPPFAnnotated(c)) return c;
		}
		return null;
	}

	/**
	 * Determines whether a method is JPPF-annotated and can be executed as a task.
	 * @param annotatedElement the method to check.
	 * @return true if the method can be executed as a task, false otherwise.
	 */
	public static boolean isJPPFAnnotated(AnnotatedElement annotatedElement)
	{
		if (annotatedElement == null) return false;
		//Annotation[] annotations = annotatedElement.getDeclaredAnnotations();
		Annotation[] annotations = annotatedElement.getAnnotations();
		for (Annotation a: annotations)
		{
			if (JPPFRunnable.class.equals(a.annotationType())) return true;
		}
		return false;
	}

	/**
	 * Get the method with the specified name, in the specified declaring class, that matches
	 * the number, order and types of the specified arguments.
	 * @param clazz the class in which to look for the method.
	 * @param name the name of the method to look for.
	 * @param args the argments of the method.
	 * @return a matching <code>Method</code> instance, or null if no match could be found.
	 */
	public static Method getMatchingMethod(Class clazz, String name, Object[] args)
	{
		Class[] argTypes = createTypeArray(args);
		Method[] methods = clazz.getDeclaredMethods();
		for (Method m: methods)
		{
			if (!m.getName().equals(name)) continue;
			if (matchingTypes(argTypes, m.getParameterTypes())) return m;
		}
		return null;
	}

	/**
	 * Get the constructor with in the specified declaring class, that matches
	 * the number, order and types of the specified arguments.
	 * @param clazz the class in which to look for the contructor.
	 * @param args the argments of the method.
	 * @return a matching <code>Constructor</code> instance, or null if no match could be found.
	 */
	public static Constructor getMatchingConstructor(Class clazz, Object[] args)
	{
		Class[] argTypes = createTypeArray(args);
		Constructor[] constructors = clazz.getDeclaredConstructors();
		for (Constructor c: constructors)
		{
			if (matchingTypes(argTypes, c.getParameterTypes())) return c;
		}
		return null;
	}

	/**
	 * Determine whether a set of (possibly null) types looseley matches another set of types.
	 * @param argTypes the types to match, null values are considered wildcards (matching any type).
	 * @param types the set of types to match. 
	 * @return true if the methods match, false otherwise.
	 */
	public static boolean matchingTypes(Class[] argTypes, Class[] types)
	{
		if (argTypes.length != types.length) return false;
		for (int i=0; i<types.length; i++)
		{
			if (argTypes[i] != null)
			{
				Class c = types[i].isPrimitive() ? mapPrimitveType(types[i]) :  types[i];
				if (!c.isAssignableFrom(argTypes[i])) return false;
			}
		}
		return true;
	}

	/**
	 * Map a primitive type to itsa corresponding wrapper type.
	 * @param type a primtive type.
	 * @return a <code>Class</code> instance.
	 */
	public static Class mapPrimitveType(Class type)
	{
		if (Boolean.TYPE.equals(type)) return Boolean.class;
		else if (Byte.TYPE.equals(type)) return Byte.class;
		else if (Byte.TYPE.equals(type)) return Byte.class;
		else if (Short.TYPE.equals(type)) return Short.class;
		else if (Integer.TYPE.equals(type)) return Integer.class;
		else if (Long.TYPE.equals(type)) return Long.class;
		else if (Float.TYPE.equals(type)) return Float.class;
		else if (Double.TYPE.equals(type)) return Double.class;
		return type;
	}

	/**
	 * Generate an array of the types of the specified arguments.
	 * If they array of arguments is null this method will return an empty array.
	 * @param args the arguments to get the types from.
	 * @return an array of <code>Class</code> instances.
	 */
	public static Class[] createTypeArray(Object[] args)
	{
		if ((args == null) || (args.length == 0)) return new Class[0];
		Class[] argTypes = new Class[args.length];
		for (int i=0; i<args.length; i++)
		{
			argTypes[i] = (args[i] != null) ? args[i].getClass() : null;
		}
		return argTypes;
	}
}
