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

import org.jppf.utils.StringUtils;
import org.slf4j.*;

/**
 * 
 * @author Laurent Cohen
 */
public class ObjectGraphDeserializer
{
	/**
	 * Logger for this class.
	 */
	private static Logger log = LoggerFactory.getLogger(ObjectGraphDeserializer.class);
	/**
	 * Determines whether the debug level is enabled in the log configuration, without the cost of a method call.
	 */
	private static boolean debugEnabled = log.isDebugEnabled();
	/**
	 * The underlyng input stream.
	 */
	ObjectInputStream in;
	/**
	 * Holds object and class descriptors caches.
	 */
	DeserializationCaches caches = new DeserializationCaches();
	/**
	 * The class loader to use.
	 */
	ClassLoader classloader = initClassLoader();

	/**
	 * Initialize this deserializer with the specified input stream.
	 * @param in the stream from which objects are read.
	 * @throws IOException if an error occurs while reading the header.
	 */
	ObjectGraphDeserializer(ObjectInputStream in) throws IOException
	{
		this.in = in;
		byte[] header = new byte[4];
		in.read(header);
		if ( (header[0] != ObjectGraphSerializer.HEADER[0])
				|| (header[1] != ObjectGraphSerializer.HEADER[1])
				|| (header[2] != ObjectGraphSerializer.HEADER[2])
				|| (header[3] != ObjectGraphSerializer.HEADER[3]))
			throw new IOException("bad header: " + StringUtils.dumpBytes(header, 0, header.length));
	}

	/**
	 * Read an object graph from the stream.
	 * @return the root of the object graph.
	 * @throws Exception if any error occurs.
	 */
	Object read() throws Exception
	{
		readClassDescriptors();
		int rootHandle = in.readInt();
		int handle = rootHandle;
		while (handle != 0xFFFFFFFF)
		{
			readObject(handle);
			handle = in.readInt();
		}
		return caches.handleToObjectMap.get(rootHandle);
	}

	/**
	 * Read all the class descriptors availablke in the input stream.
	 * @throws Exception if any error occurs.
	 */
	private void readClassDescriptors() throws Exception
	{
		int n = in.readInt();
		for (int i=0; i<n; i++)
		{
			ClassDescriptor cd = new ClassDescriptor();
			cd.read(in);
			caches.handleToDescriptorMap.put(cd.handle, cd);
			if (debugEnabled) log.debug("read " + cd);
		}
		Set<Map.Entry<Integer, ClassDescriptor>> entries = caches.handleToDescriptorMap.entrySet();
		for (Map.Entry<Integer, ClassDescriptor> entry: entries)
		{
			ClassDescriptor cd = entry.getValue();
			if (cd.clazz != null) continue;
			if (cd.array)
			{
				List<ClassDescriptor> types = new ArrayList<ClassDescriptor>();
				ClassDescriptor tmp = cd;
				while (tmp != null)
				{
					types.add(tmp);
					tmp = tmp.array ? caches.getDescriptor(tmp.componentTypeHandle) : null;
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

	/**
	 * Read the next object in the stream.
	 * @param handle the handle of the object to read.
	 * @throws Exception if any error occurs.
	 */
	private void readObject(int handle) throws Exception
	{
		int cdHandle = in.readInt();
		ClassDescriptor cd = caches.getDescriptor(cdHandle);
		if (cd.array) readArray(handle, cd);
		else
		{
			//Object o = cd.clazz.newInstance();
			Object o = newInstance(cd);
			processPendingReferences(handle, o);
			if (cd.hasWriteObject)
			{
				Method m = ReflectionHelper.getWriteObjectMethod(cd.clazz);
				m.invoke(o, in);
			}
			else if (cd.externalizable) ((Externalizable) o).readExternal(in);
			else readFields(cd, o);
		}
	}

	/**
	 * Read all tthe fields for the specified object.
	 * @param cd the class descritpor for the object.
	 * @param o the object to set the fields on.
	 * @throws Exception if any error occurs.
	 */
	private void readFields(ClassDescriptor cd, Object o) throws Exception
	{
		ClassDescriptor tmpDesc = cd;
		while (tmpDesc != null)
		{
			readDeclaredFields(tmpDesc, o);
			tmpDesc = caches.getDescriptor(tmpDesc.superClassHandle);
		}
	}

	/**
	 * Read the fields declared by the class described by the specified class descriptor.
	 * @param cd the class descriptor to use.
	 * @param o the object ot set the field values on.
	 * @throws Exception if any error occurs.
	 */
	private void readDeclaredFields(ClassDescriptor cd, Object o) throws Exception
	{
		for (FieldDescriptor fd: cd.fields)
		{
			ClassDescriptor typeDesc = caches.getDescriptor(fd.typeHandle);
			Field field = cd.clazz.getDeclaredField(fd.name);
			if (!field.isAccessible()) field.setAccessible(true);
			if (typeDesc.primitive)
			{
				switch(typeDesc.signature.charAt(0))
				{
					case 'B': field.setByte(o, in.readByte()); break;
					case 'S': field.setShort(o, in.readShort()); break;
					case 'I': field.setInt(o, in.readInt()); break;
					case 'J': field.setLong(o, in.readLong()); break;
					case 'F': field.setFloat(o, in.readFloat()); break;
					case 'D': field.setDouble(o, in.readDouble()); break;
					case 'C': field.setChar(o, in.readChar()); break;
					case 'Z': field.setBoolean(o, in.readBoolean()); break;
				}
			}
			else
			{
				int valHandle = in.readInt();
				Object val = caches.handleToObjectMap.get(valHandle);
				if (val == null) caches.addPendingReference(valHandle, new PendingFieldReference(cd, fd, o));
				else field.set(o, val);
			}
		}
	}

	/**
	 * Create and read an array from the stream.
	 * @param handle the handle of the array.
	 * @param cd the class descriptor for the array's class.
	 * @throws Exception if any error occurs.
	 */
	private void readArray(int handle, ClassDescriptor cd) throws Exception
	{
		int len = in.readInt();
		ClassDescriptor eltDesc = caches.getDescriptor(cd.componentTypeHandle);
		Object o = null;
		if (eltDesc.primitive)
		{
			switch(eltDesc.signature.charAt(0))
			{
				case 'B': byte[] barray = new byte[len];      processPendingReferences(handle, barray); for (int i=0; i<len; i++) barray[i] = in.readByte(); break;
				case 'S': short[] sarray = new short[len];    processPendingReferences(handle, sarray); for (int i=0; i<len; i++) sarray[i] = in.readShort(); break;
				case 'I': int[] iarray = new int[len];        processPendingReferences(handle, iarray); for (int i=0; i<len; i++) iarray[i] = in.readInt(); break;
				case 'J': long[] larray = new long[len];      processPendingReferences(handle, larray); for (int i=0; i<len; i++) larray[i] = in.readLong(); break;
				case 'F': float[] farray = new float[len];    processPendingReferences(handle, farray); for (int i=0; i<len; i++) farray[i] = in.readFloat(); break;
				case 'D': double[] darray = new double[len];  processPendingReferences(handle, darray); for (int i=0; i<len; i++) darray[i] = in.readDouble(); break;
				case 'C': char[] carray = new char[len];      processPendingReferences(handle, carray); for (int i=0; i<len; i++) carray[i] = in.readChar(); break;
				case 'Z': boolean[] array = new boolean[len]; processPendingReferences(handle, array);  for (int i=0; i<len; i++) array[i] = in.readBoolean(); break;
			}
		}
		else
		{
			o = Array.newInstance(eltDesc.clazz, len);
			processPendingReferences(handle, o);
			for (int i=0; i<len; i++)
			{
				int eltHandle = in.readInt();
				Object ref = caches.handleToObjectMap.get(eltHandle);
				if (ref != null) Array.set(o, i, ref);
				else caches.addPendingReference(eltHandle, new PendingArrayElementReference(i, o));
			}
		}
	}

	/**
	 * Process all the pending references for the specified object.
	 * @param handle the object's handle.
	 * @param o the actual object.
	 * @throws Exception if any error occurs.
	 */
	private void processPendingReferences(int handle, Object o) throws Exception
	{
		caches.handleToObjectMap.put(handle, o);
		List<PendingReference> list = caches.pendingRefMap.remove(handle);
		if (list == null) return;
		for (PendingReference ref: list)
		{
			if (ref instanceof PendingFieldReference)
			{
				PendingFieldReference fieldRef = (PendingFieldReference) ref;
				Field field = fieldRef.cd.clazz.getDeclaredField(fieldRef.fd.name);
				if (!field.isAccessible()) field.setAccessible(true);
				field.set(fieldRef.o, o);
			}
			else
			{
				PendingArrayElementReference eltRef = (PendingArrayElementReference) ref;
				Array.set(eltRef.o, eltRef.pos, o);
			}
		}
	}

	/**
	 * Initialize the class laoder to use.
	 * @return a {@link ClassLoader} instance.
	 */
	private ClassLoader initClassLoader()
	{
		ClassLoader cl = Thread.currentThread().getContextClassLoader();
		return cl != null ? cl : getClass().getClassLoader();
	}

	/**
	 * Create a new instance of the class described by the specified descriptor.
	 * @param cd the class descriptor to use.
	 * @return a new instance of the class.
	 * @throws Exception if any error occurs.
	 */
	private Object newInstance(ClassDescriptor cd) throws Exception
	{
		/*
		Constructor<?>[] constructors = cd.clazz.getDeclaredConstructors();
		//Arrays.sort(constructors, new ConstructorComparator());
		for (Constructor c: constructors)
		{
			if (c.getParameterTypes().length == 0)
			{
				if (!c.isAccessible()) c.setAccessible(true);
				return c.newInstance();
			}
		}
		return null;
		*/
		return ReflectionHelper.create(cd.clazz);
	}

	/**
	 * Compares two constructors based on their number of parameters.
	 */
	private static class ConstructorComparator implements Comparator<Constructor<?>>
	{
		/**
		 * {@inheritDoc}
		 */
		public int compare(Constructor<?> c1, Constructor<?> c2)
		{
			int n1 = c1.getParameterTypes().length;
			int n2 = c2.getParameterTypes().length;
			return n1 < n2 ? -1 : (n1 > n2 ? 1 : 0);
		}
	}
}
