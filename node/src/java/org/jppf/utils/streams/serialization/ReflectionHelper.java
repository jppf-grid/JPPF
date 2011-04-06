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

package org.jppf.utils.streams.serialization;

import java.io.*;
import java.lang.reflect.*;
import java.util.*;

import org.jppf.JPPFException;

import sun.reflect.ReflectionFactory;

/**
 * 
 * @author Laurent Cohen
 */
public final class ReflectionHelper
{
	/**
	 * Constant for empty Fields arrays.
	 */
	private static final Field[] EMPTY_FIELDS = new Field[0];
	/**
	 * Constant for empty Class arrays.
	 */
	private static final Class<?>[] EMPTY_CLASSES = new Class<?>[0];

	/**
	 * Get all declared non-transient fields of the given class.
	 * @param clazz the class object from whch to extract the fields.
	 * @return an array of {@link Field} objects.
	 * @throws Exception if any error occurs.
	 */
	public static Field[] getNonTransientFields(Class<?> clazz) throws Exception
	{
		Field[] allFields = clazz.getDeclaredFields();
		if (allFields.length <= 0) return allFields;
		List<Field> fields = new ArrayList<Field>(allFields.length);
		for (Field f: allFields)
		{
			int mod = f.getModifiers();
			if (!Modifier.isTransient(mod) && !Modifier.isStatic(mod)) fields.add(f); 
		}
		return fields.toArray(EMPTY_FIELDS);
	}

	/**
	 * Get a unique string representation for the specified type. 
	 * @param clazz the type from which to get the signature.
	 * @return a string representing the ytpe.
	 * @throws Exception if any error occurs.
	 */
	public static String getSignatureFromType(Class<?> clazz) throws Exception
	{
		StringBuilder sb =  new StringBuilder();
		Class<?> tmp = clazz;
		while (tmp.isArray())
		{
			sb.append('[');
			tmp = tmp.getComponentType();
		}
		if (clazz == Byte.TYPE) sb.append('B');
		else if (tmp == Short.TYPE) sb.append('S');
		else if (tmp == Integer.TYPE) sb.append('I');
		else if (tmp == Long.TYPE) sb.append('J');
		else if (tmp == Float.TYPE) sb.append('F');
		else if (tmp == Double.TYPE) sb.append('D');
		else if (tmp == Boolean.TYPE) sb.append('Z');
		else if (tmp == Character.TYPE) sb.append('C');
		else sb.append('L').append(tmp.getName());
		return sb.toString();
	}

	/**
	 * Lookup or load the non-array class based on the sepcified signature.
	 * @param signature the class signature.
	 * @param cl the class laoder used to load the class.
	 * @return a {@link Class} object.
	 * @throws Exception if any error occurs.
	 */
	public static Class<?> getNonArrayTypeFromSignature(String signature, ClassLoader cl) throws Exception
	{
		switch(signature.charAt(0))
		{
			case 'B': return Byte.TYPE;
			case 'S': return Short.TYPE;
			case 'I': return Integer.TYPE;
			case 'J': return Long.TYPE;
			case 'F': return Float.TYPE;
			case 'D': return Double.TYPE;
			case 'C': return Character.TYPE;
			case 'Z': return Boolean.TYPE;
			case 'L': return cl.loadClass(signature.substring(1));
		}
		throw new JPPFException("Could not load type with signature '" + signature + "'");
	}

	/**
	 * Determine whether the specified class has a writeObject() method with the signature specified in {@link Serializable}.
	 * @param clazz the class to check.
	 * @return true if the class has a writeObject() method, false otherwise.
	 * @throws Exception if any error occurs.
	 */
	public static Method getWriteObjectMethod(Class<?> clazz) throws Exception
	{
		Method m = null;
		try
		{
			m = clazz.getDeclaredMethod("writeObject", ObjectOutputStream.class);
			if (m.getReturnType() != Void.TYPE) return null;
			int n = m.getModifiers();
			return (!Modifier.isStatic(n) && Modifier.isPrivate(n)) ? m : null;
		}
		catch (NoSuchMethodException e)
		{
		}
		return null;
	}

	/**
	 * Create an object without calling any of its class constructors.
	 * @param <T> the type of object to create.
	 * @param clazz the object's class.
	 * @return the newly created object.
	 * @throws Exception if any error occurs.
	 */
	public static <T> T create(Class<T> clazz) throws Exception
	{
		return create(clazz, Object.class);
	}

	/**
	 * Create an object without calling any of its class constructors,
	 * and calling the superclass no-arg constructor. 
	 * @param <T> the type of object to create.
	 * @param clazz the object's class.
	 * @param parent the object's super class.
	 * @return the newly created object.
	 * @throws Exception if any error occurs.
	 */
	public static <T> T create(Class<T> clazz, Class<? super T> parent) throws Exception
	{
		try
		{
			ReflectionFactory rf = ReflectionFactory.getReflectionFactory();
			Constructor objDef = parent.getDeclaredConstructor();
			Constructor intConstr = rf.newConstructorForSerialization(clazz, objDef);
			return clazz.cast(intConstr.newInstance());
		}
		catch (RuntimeException e)
		{
			throw e;
		}
		catch (Exception e)
		{
			throw new IllegalStateException("Cannot create object", e);
		}
	}
}
