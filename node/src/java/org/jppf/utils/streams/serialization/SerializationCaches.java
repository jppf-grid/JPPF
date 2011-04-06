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

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

import org.slf4j.*;

/**
 * 
 * @author Laurent Cohen
 */
public class SerializationCaches
{
	/**
	 * Logger for this class.
	 */
	private static Logger log = LoggerFactory.getLogger(SerializationCaches.class);
	/**
	 * Determines whether the debug level is enabled in the log configuration, without the cost of a method call.
	 */
	private static boolean debugEnabled = log.isDebugEnabled();
	/**
	 * List of all primitive types.
	 */
	static final Class<?>[] PRIMITIVE_TYPES = { Byte.TYPE, Short.TYPE, Integer.TYPE, Long.TYPE, Float.TYPE, Double.TYPE, Character.TYPE, Boolean.TYPE/*, Void.TYPE*/ };
	/**
	 * Mapping of primitive types to their descriptor.
	 */
	private static Map<Class<?>, ClassDescriptor> globalTypesMap = initGlobalTypes();
	/**
	 * Mapping of classes to their descriptor.
	 */
	Map<Class<?>, ClassDescriptor> classToDescMap = new IdentityHashMap<Class<?>, ClassDescriptor>();
	/**
	 * Mapping of objects to their handle.
	 */
	Map<Object, Integer> objectHandleMap = new IdentityHashMap<Object, Integer>();
	/**
	 * Counter for the class handles.
	 */
	private AtomicInteger classHandleCount = new AtomicInteger(0);
	/**
	 * Counter for the object handles.
	 */
	private AtomicInteger objectHandleCount = new AtomicInteger(0);

	/**
	 * Initialize the descriptors for all primitive types.
	 * @return a mapping of primitive types to their class descriptor.
	 */
	private static Map<Class<?>, ClassDescriptor> initGlobalTypes()
	{
		Map<Class<?>, ClassDescriptor> map = new IdentityHashMap<Class<?>, ClassDescriptor>();
		AtomicInteger counter = new AtomicInteger(0);
		try
		{
			for (Class<?> c: PRIMITIVE_TYPES) getClassDescriptorGeneric(map, c, counter);
			getClassDescriptorGeneric(map, Object.class, counter);
			getClassDescriptorGeneric(map, String.class, counter);
		}
		catch (Exception e)
		{
			log.error("error initializing global types", e);
		}

		/*
		int handle = 0;
		try
		{
			for (Class<?> c: PRIMITIVE_TYPES)
			{
				ClassDescriptor cd = new ClassDescriptor(c);
				cd.handle = ++handle;
				map.put(c, cd);
			}
			ClassDescriptor cd = new ClassDescriptor(Object.class);
			cd.handle = ++handle;
			map.put(Object.class, cd);
			cd = new ClassDescriptor(String.class);
			cd.handle = ++handle;
			map.put(String.class, cd);
		}
		catch (Exception e)
		{
			log.error("error initializing global types", e);
		}
		*/
		return map;
	}

	/**
	 * Default constructor.
	 */
	SerializationCaches()
	{
		classToDescMap.putAll(globalTypesMap);
		classHandleCount.set(classToDescMap.size() + 1);
	}

	/**
	 * Get the descriptor for the specified class, and created it if needed.
	 * @param clazz the class for which to get a descriptor.
	 * @return a {@link ClassDescriptor} object.
	 * @throws Exception if nay error occurs.
	 */
	ClassDescriptor getClassDescriptor(Class<?> clazz) throws Exception
	{
		return getClassDescriptorGeneric(classToDescMap, clazz, classHandleCount);
		/*
		ClassDescriptor cd = classToDescMap.get(clazz);
		if (cd == null) cd = addClass(clazz);
		return cd;
		*/
	}

	/**
	 * Add a class mapping.
	 * @param clazz the class to map to a descriptor.
	 * @return the {@link ClassDescriptor} object that was created.
	 * @throws Exception if any error occurs.
	 */
	ClassDescriptor addClass(Class<?> clazz) throws Exception
	{
		ClassDescriptor cd = new ClassDescriptor(clazz);
		cd.handle = classHandleCount.incrementAndGet();
		classToDescMap.put(clazz, cd);
		for (FieldDescriptor fd: cd.fields) fd.type = getClassDescriptor(fd.field.getType());
		Class<?> tmpClazz = clazz.getSuperclass();
		if ((tmpClazz != null) && (tmpClazz != Object.class)) cd.superClass = getClassDescriptor(tmpClazz);
		if (debugEnabled) log.debug("created " + cd);
		/*
		ClassDescriptor tmpDesc = cd;
		while ((tmpClazz != null) && (tmpClazz != Object.class))
		{
			tmpDesc.superClass = getClassDescriptor(tmpClazz);
			tmpDesc = tmpDesc.superClass;
			tmpClazz = tmpClazz.getSuperclass();
		}
		*/
		return cd;
	}

	/**
	 * Get the handle of the specified object, and create it if needed.
	 * @param o the object for which to get a handle.
	 * @return the handle as an int cvalue.
	 */
	int getObjectHandle(Object o)
	{
		if (o == null) return 0;
		Integer handle = objectHandleMap.get(o);
		if (handle == null)
		{
			handle = objectHandleCount.incrementAndGet();
			objectHandleMap.put(o, handle);
			if (debugEnabled) log.debug("created handle " + handle);
		}
		return handle;
	}

	/**
	 * Get the descriptor for the specified class, and create it if needed.
	 * @param map the map that contains the handle to class descrioptor associations.
	 * @param clazz the class for which to get a descriptor.
	 * @param counter the handle as an auto-incrementing counter.
	 * @return a {@link ClassDescriptor} object.
	 * @throws Exception if nay error occurs.
	 */
	static ClassDescriptor getClassDescriptorGeneric(Map<Class<?>, ClassDescriptor> map, Class<?> clazz, AtomicInteger counter) throws Exception
	{
		ClassDescriptor cd = map.get(clazz);
		if (cd == null) cd = addClassGeneric(map, clazz, counter);
		return cd;
	}

	/**
	 * Add a class mapping.
	 * @param map the map that contains the handle to class descrioptor associations.
	 * @param clazz the class to map to a descriptor.
	 * @param counter the handle as an auto-incrementing counter.
	 * @return the {@link ClassDescriptor} object that was created.
	 * @throws Exception if any error occurs.
	 */
	static ClassDescriptor addClassGeneric(Map<Class<?>, ClassDescriptor> map, Class<?> clazz, AtomicInteger counter) throws Exception
	{
		ClassDescriptor cd = new ClassDescriptor(clazz);
		cd.handle = counter.incrementAndGet();
		map.put(clazz, cd);
		for (FieldDescriptor fd: cd.fields) fd.type = getClassDescriptorGeneric(map, fd.field.getType(), counter);
		Class<?> tmpClazz = clazz.getSuperclass();
		if ((tmpClazz != null) && (tmpClazz != Object.class)) cd.superClass = getClassDescriptorGeneric(map, tmpClazz, counter);
		if (debugEnabled) log.debug("created " + cd);
		/*
		ClassDescriptor tmpDesc = cd;
		while ((tmpClazz != null) && (tmpClazz != Object.class))
		{
			tmpDesc.superClass = getClassDescriptor(tmpClazz);
			tmpDesc = tmpDesc.superClass;
			tmpClazz = tmpClazz.getSuperclass();
		}
		*/
		return cd;
	}

}
