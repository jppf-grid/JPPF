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
import java.lang.reflect.Array;
import java.security.*;
import java.util.*;

import org.slf4j.*;

/**
 * Instances of this class are intented to serialize object graphs to an underlying output stream.
 * @author Laurent Cohen
 */
public class ObjectGraphSerializer
{
	/**
	 * The stream header ('JPPF' in ascii).
	 */
	static final byte[] HEADER = new byte[] { 74, 80, 80, 70 };
	/**
	 * Handle for null references.
	 */
	static final byte[] NULL_HANDLE = new byte[] { 0, 0, 0, 0 };
	/**
	 * Handle for null references.
	 */
	private static final byte[] FINAL_MARKER = new byte[] { (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF };
	/**
	 * Logger for this class.
	 */
	private static Logger log = LoggerFactory.getLogger(ObjectGraphSerializer.class);
	/**
	 * Determines whether the debug level is enabled in the log configuration, without the cost of a method call.
	 */
	private static boolean debugEnabled = log.isDebugEnabled();
	/**
	 * The stream serialized data is written to.
	 */
	ObjectOutputStream out;
	/**
	 * Holds all class and object descriptors.
	 */
	SerializationCaches caches = new SerializationCaches();

	/**
	 * Initialize this serializer with the specified output stream, and write the header.
	 * @param out the stream to which the serialized data is written.
	 * @throws IOException if an error occurs while writing the header.
	 */
	public ObjectGraphSerializer(ObjectOutputStream out) throws IOException
	{
		this.out = out;
		out.write(HEADER);
	}

	/**
	 * Explore an object graph from the specified root.
	 * @param o any object, can be null.
	 * @throws Exception if any error occurs.
	 */
	public void exploreRoot(Object o) throws Exception
	{
		initializeObjectHandle(o);
		writeClassDescriptors();
		//out.writeInt(caches.objectHandleMap.size());
		int handle = caches.objectHandleMap.remove(o);
		writeObject(o, handle);
		Set<Map.Entry<Object, Integer>> entries = caches.objectHandleMap.entrySet();
		for (Map.Entry<Object, Integer> entry: entries) writeObject(entry.getKey(), entry.getValue());
		caches.objectHandleMap.put(o, handle);
		out.write(FINAL_MARKER);
	}

	/**
	 * Write the specified object to the output stream.
	 * @param o ther object to write.
	 * @param handle the object's handle
	 * @throws Exception if any error occurs.
	 */
	private void writeObject(Object o, int handle) throws Exception
	{
		if (o == null) out.write(NULL_HANDLE);
		else
		{
			ClassDescriptor cd = caches.getClassDescriptor(o.getClass());
			out.writeInt(handle);
			out.writeInt(cd.handle);
			if (debugEnabled) log.debug("writing object " + o + ", handle=" + handle + ", class=" + o.getClass() + ", cd=" + cd);
			if (cd.hasWriteObject) cd.writeObjectMethod.invoke(o, out);
			else if (cd.externalizable) ((Externalizable) o).writeExternal(out);
			else if (cd.array) writeArray(o, cd);
			else writeFields(o, cd);
		}
	}

	/**
	 * Write the all fields, including those declared in the superclasses, for the specified object.
	 * @param o the object whose fields are to be written.
	 * @param cd the object's class descriptor.
	 * @throws Exception if any error occurs.
	 */
	private void writeFields(Object o, ClassDescriptor cd) throws Exception
	{
		ClassDescriptor tmpDesc = cd;
		while (tmpDesc != null)
		{
			writeDeclaredFields(o, tmpDesc);
			tmpDesc = tmpDesc.superClass;
		}
	}

	/**
	 * Write the fields for the specified object and class decriptor.
	 * @param o the object whose fields are to be written.
	 * @param cd the object's class descriptor.
	 * @throws Exception if any error occurs.
	 */
	private void writeDeclaredFields(final Object o, ClassDescriptor cd) throws Exception
	{
		for (final FieldDescriptor fd: cd.fields)
		{
			Object val = null;
			GetFieldValueAction action = new GetFieldValueAction(fd.field, o);
			val = AccessController.doPrivileged(action);
			if (action.getException() != null) throw action.getException();
			if (fd.type.primitive)
			{
				switch(fd.type.signature.charAt(0))
				{
					case 'B': out.writeByte((Integer) val); break;
					case 'S': out.writeShort((Short) val); break;
					case 'I': out.writeInt((Integer) val); break;
					case 'J': out.writeLong((Long) val); break;
					case 'F': out.writeFloat((Float) val); break;
					case 'D': out.writeDouble((Double) val); break;
					case 'C': out.writeChar((Integer) val); break;
					case 'Z': out.writeBoolean((Boolean) val); break;
				}
			}
			else
			{
				int handle = (val == null) ? 0 : caches.getObjectHandle(val);
				out.writeInt(handle);
			}
		}
	}

	/**
	 * Write the length and elements for the specified array.
	 * @param o the array to write.
	 * @param cd the array's class descriptor.
	 * @throws Exception if any error occurs.
	 */
	private void writeArray(Object o, ClassDescriptor cd) throws Exception
	{
		int n = Array.getLength(o);
		out.writeInt(n);
		ClassDescriptor eltDesc = cd.componentType;
		if (eltDesc.primitive)
		{
			switch(eltDesc.signature.charAt(0))
			{
				case 'B': for (byte v: (byte[]) o) out.writeByte(v); break;
				case 'S': for (short v: (short[]) o) out.writeShort(v); break;
				case 'I': for (int v: (int[]) o) out.writeInt(v); break;
				case 'J': for (long v: (long[]) o) out.writeLong(v); break;
				case 'F': for (float v: (float[]) o) out.writeFloat(v); break;
				case 'D': for (double v: (double[]) o) out.writeDouble(v); break;
				case 'C': for (char v: (char[]) o) out.writeChar(v); break;
				case 'Z': for (boolean v: (boolean[]) o) out.writeBoolean(v); break;
			}
		}
		else
		{
			for (int i=0; i<n; i++)
			{
				Object val = Array.get(o, i);
				int handle = (val == null) ? 0 : caches.getObjectHandle(val);
				out.writeInt(handle);
			}
		}
	}

	/**
	 * Initialize all object and class descriptors and handles for the specified object graph.
	 * @param o the object graph root.
	 * @throws Exception if any error occurs.
	 */
	private void initializeObjectHandle(Object o) throws Exception
	{
		if (o == null) return;
		if (caches.objectHandleMap.get(o) != null) return;
		caches.getObjectHandle(o);
		ClassDescriptor cd = caches.getClassDescriptor(o.getClass());
		if (cd == null)
		{
			cd = new ClassDescriptor(o.getClass());
		}
		if (cd.primitive || cd.hasWriteObject || cd.externalizable) return;
		if (cd.array)
		{
			cd.componentType = caches.getClassDescriptor(cd.clazz.getComponentType());
			if (!cd.componentType.primitive)
			{
				int len = Array.getLength(o);
				for (int i=0; i<len; i++) initializeObjectHandle(Array.get(o, i));
			}
		}
		else
		{
			ClassDescriptor tmp = cd;
			while (tmp != null)
			{
				initializeObjectHandle(o, tmp);
				tmp = tmp.superClass;
			}
		}
	}

	/**
	 * Intiialize the object handles for the fields of a single object, relative to the specified class descriptor.
	 * @param o the object for which to initialize the handles.
	 * @param cd the class descriptor for the object's class or one of its super classes.
	 * @throws Exception if any error occurs.
	 */
	private void initializeObjectHandle(Object o, ClassDescriptor cd) throws Exception
	{
		for (FieldDescriptor fd: cd.fields)
		{
			ClassDescriptor fieldDesc = fd.type;
			if (fieldDesc.primitive || fieldDesc.hasWriteObject || fieldDesc.externalizable) continue;
			if (!fd.field.isAccessible()) fd.field.setAccessible(true);
			Object val = fd.field.get(o);
			initializeObjectHandle(val);
		}
	}

	/**
	 * Write all class descriptors to the output stream.
	 * @throws IOException if any error occurs.
	 */
	private void writeClassDescriptors() throws IOException
	{
		out.writeInt(caches.classToDescMap.size());
		Set<Map.Entry<Class<?>, ClassDescriptor>> entries = caches.classToDescMap.entrySet();
		for (Map.Entry<Class<?>, ClassDescriptor> entry: entries) entry.getValue().write(out);
	}
}
