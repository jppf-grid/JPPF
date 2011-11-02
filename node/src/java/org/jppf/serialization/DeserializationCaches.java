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

package org.jppf.serialization;

import java.lang.reflect.Array;
import java.util.*;

import org.slf4j.*;

/**
 * Instances of this class handle the caching and lookup of class descriptors and objects during deserialization.
 * @author Laurent Cohen
 */
class DeserializationCaches
{
	/**
	 * Logger for this class.
	 */
	private static Logger log = LoggerFactory.getLogger(DeserializationCaches.class);
	/**
	 * Mapping of handles to corresponding class descriptors.
	 */
	Map<Integer, ClassDescriptor> handleToDescriptorMap = new HashMap<Integer, ClassDescriptor>();
	/**
	 * Mapping of handles to corresponding objects.
	 */
	Map<Integer, Object> handleToObjectMap = new HashMap<Integer, Object>();

	/**
	 * Default constructor.
	 */
	DeserializationCaches()
	{
		Set<Map.Entry<Class<?>, ClassDescriptor>> entries = SerializationCaches.globalTypesMap.entrySet();
		List<ClassDescriptor> list = new ArrayList<ClassDescriptor>(entries.size());
		for (Map.Entry<Class<?>, ClassDescriptor> entry: entries)
		{
			ClassDescriptor cd = entry.getValue();
			ClassDescriptor cd2 = new ClassDescriptor();
			cd2.signature = cd.signature;
			cd2.primitive = cd.primitive;
			cd2.array = cd.array;
			cd2.externalizable = cd.externalizable;
			cd2.hasWriteObject = cd.hasWriteObject;
			cd2.enumType = cd.enumType;
			cd2.handle = cd.handle;
			if (cd.superClass != null) cd2.superClassHandle = cd.superClass.handle;
			if (cd.componentType != null) cd2.componentTypeHandle = cd.componentType.handle;
			if (cd.fields.length > 0)
			{
				cd2.fields = new FieldDescriptor[cd.fields.length];
				for (int i=0; i<cd.fields.length; i++)
				{
					FieldDescriptor fd = cd.fields[i];
					FieldDescriptor fd2 = new FieldDescriptor();
					fd2.name = fd.name;
					if (fd.type != null) fd2.typeHandle = fd.type.handle;
					cd2.fields[i] = fd2;
				}
			}
			handleToDescriptorMap.put(cd2.handle, cd2);
			list.add(cd2);
		}
		try
		{
			initializeDescriptorClasses(list, getClass().getClassLoader());
		}
		catch (Exception e)
		{
			log.error(e.getMessage(), e);
		}
	}

	/**
	 * Get the class dezscriptor assocateed with the specified handfe.
	 * @param handle the handle to lookup.
	 * @return a {@link ClassDescriptor} instance.
	 */
	ClassDescriptor getDescriptor(final int handle)
	{
		return handleToDescriptorMap.get(handle);
	}

	/**
	 * Initialize the class object for each recently loaded class descriptor, including array types.
	 * @param list the list of class descriptor to process.
	 * @param classloader used to load the classes.
	 * @throws Exception if any error occurs.
	 */
	void initializeDescriptorClasses(final Collection<ClassDescriptor> list, final ClassLoader classloader) throws Exception
	{
		for (ClassDescriptor cd: list)
		{
			if (cd.clazz != null) continue;
			if (cd.array)
			{
				List<ClassDescriptor> types = new ArrayList<ClassDescriptor>();
				ClassDescriptor tmp = cd;
				while (tmp != null)
				{
					types.add(tmp);
					tmp = tmp.array ? getDescriptor(tmp.componentTypeHandle) : null;
				}
				for (int i=types.size()-1; i>=0; i--)
				{
					tmp = types.get(i);
					if (tmp.clazz != null) continue;
					if (!tmp.array) tmp.clazz = ReflectionHelper.getNonArrayTypeFromSignature(tmp.signature, classloader);
					else
					{
						Class<?> clazz = types.get(i+1).clazz;
						Object array = Array.newInstance(clazz, 0);
						tmp.clazz = array.getClass();
					}
				}
			}
			else cd.clazz = ReflectionHelper.getNonArrayTypeFromSignature(cd.signature, classloader);
		}
	}
}
