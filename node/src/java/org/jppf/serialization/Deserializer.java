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

import java.io.*;
import java.lang.reflect.*;
import java.util.*;

import org.jppf.utils.*;
import org.slf4j.*;

/**
 * Instances of this class are intended to deserialize object graphs from an underlying input stream.
 * @author Laurent Cohen
 */
class Deserializer
{
	/**
	 * Logger for this class.
	 */
	private static Logger log = LoggerFactory.getLogger(Deserializer.class);
	/**
	 * Determines whether the debug level is enabled in the log configuration, without the cost of a method call.
	 */
	private static boolean traceEnabled = log.isTraceEnabled();
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
	 * Handle of thez object being read.
	 */
	int currentHandle;
	/**
	 * Descriptor the class of the object currently being written.
	 */
	ClassDescriptor currentClassDescriptor;
	/**
	 * The object currently being written.
	 */
	Object currentObject;
	/**
	 * Temporary buffer used to read arrays of primitive values from the stream.
	 */
	private byte[] buf = new byte[4096];

	/**
	 * Initialize this deserializer with the specified input stream.
	 * @param in the stream from which objects are read.
	 * @throws IOException if an error occurs while reading the header.
	 */
	Deserializer(final ObjectInputStream in) throws IOException
	{
		this.in = in;
		byte[] header = new byte[4];
		in.read(header);
		if ( (header[0] != Serializer.HEADER[0])
				|| (header[1] != Serializer.HEADER[1])
				|| (header[2] != Serializer.HEADER[2])
				|| (header[3] != Serializer.HEADER[3]))
			throw new IOException("bad header: " + StringUtils.dumpBytes(header, 0, header.length));
	}

	/**
	 * Read an object graph from the stream.
	 * @return the next object read from the stream.
	 * @throws Exception if any error occurs.
	 */
	Object readObject() throws Exception
	{
		byte b = in.readByte();
		while (b == Serializer.CLASS_HEADER)
		{
			readClassDescriptors();
			b = in.readByte();
		}
		if (b == Serializer.NULL_OBJECT_HEADER) return null;
		else if (b == Serializer.CLASS_OBJECT_HEADER) return readClassObject();
		int handle = in.readInt();
		Object o = caches.handleToObjectMap.get(handle);
		if (o != null) return o;
		readObject(handle);
		return caches.handleToObjectMap.get(handle);
	}

	/**
	 * Read the next object in the stream.
	 * @param handle the handle of the object to read.
	 * @throws Exception if any error occurs.
	 */
	@SuppressWarnings("unchecked")
	private void readObject(final int handle) throws Exception
	{
		int cdHandle = in.readInt();
		ClassDescriptor cd = caches.getDescriptor(cdHandle);
		if (cd.array) readArray(handle, cd);
		else if (cd.enumType)
		{
			String name = (String) readObject();
			//String name = in.readUTF();
			if (traceEnabled) try { log.trace("reading enum[" + cd.signature + "] : " + name); } catch(Exception e) {}
			Object val = (name == null) ? null : Enum.valueOf((Class<? extends Enum>) cd.clazz, name);
			caches.handleToObjectMap.put(handle, val);
		}
		else
		{
			Object obj = newInstance(cd);
			currentObject = obj;
			currentClassDescriptor = cd;
			if (traceEnabled) try { log.trace("reading object " + obj); } catch(Exception e) {}
			caches.handleToObjectMap.put(handle, obj);
			if (cd.hasWriteObject)
			{
				Method m = ReflectionHelper.getReadObjectMethod(cd.clazz);
				if (!m.isAccessible()) m.setAccessible(true);
				m.invoke(obj, in);
			}
			else if (cd.externalizable) ((Externalizable) obj).readExternal(in);
			else readFields(cd, obj);
		}
	}

	/**
	 * Read the next object in the stream, which is a class object.
	 * @return the class object whose handle was read.
	 * @throws Exception if any error occurs.
	 */
	@SuppressWarnings("unchecked")
	private Object readClassObject() throws Exception
	{
		int cdHandle = in.readInt();
		ClassDescriptor cd = caches.getDescriptor(cdHandle);
		return cd.clazz;
	}

	/**
	 * Read all tthe fields for the specified object.
	 * @param cd the class descritpor for the object.
	 * @param obj the object to set the fields on.
	 * @throws Exception if any error occurs.
	 */
	void readFields(final ClassDescriptor cd, final Object obj) throws Exception
	{
		ClassDescriptor tmpDesc = cd;
		while (tmpDesc != null)
		{
			readDeclaredFields(tmpDesc, obj);
			tmpDesc = caches.getDescriptor(tmpDesc.superClassHandle);
		}
	}

	/**
	 * Read the fields declared by the class described by the specified class descriptor.
	 * @param cd the class descriptor to use.
	 * @param obj the object ot set the field values on.
	 * @throws Exception if any error occurs.
	 */
	@SuppressWarnings("unchecked")
	private void readDeclaredFields(final ClassDescriptor cd, final Object obj) throws Exception
	{
		for (int i=0; i<cd.fields.length; i++)
		{
			FieldDescriptor fd = cd.fields[i];
			if (traceEnabled) try { log.trace("reading field '" + fd.name + "' of object " + obj); } catch(Exception e) {}
			ClassDescriptor typeDesc = caches.getDescriptor(fd.typeHandle);
			Field field = cd.clazz.getDeclaredField(fd.name);
			if (!field.isAccessible()) field.setAccessible(true);
			if (typeDesc.primitive)
			{
				switch(typeDesc.signature.charAt(0))
				{
					case 'B': field.setByte(obj, (byte) in.read()); break;
					case 'S': field.setShort(obj, in.readShort()); break;
					case 'I': field.setInt(obj, in.readInt()); break;
					case 'J': field.setLong(obj, in.readLong()); break;
					case 'F': field.setFloat(obj, in.readFloat()); break;
					case 'D': field.setDouble(obj, in.readDouble()); break;
					case 'C': field.setChar(obj, in.readChar()); break;
					case 'Z': field.setBoolean(obj, in.readBoolean()); break;
				}
			}
			else if (typeDesc.enumType)
			{
				String name = (String) readObject();
				//String name = in.readUTF();
				if (traceEnabled) try { log.trace("reading enum[" + typeDesc.signature + "] : " + name); } catch(Exception e) {}
				Object val = (name == null) ? null : Enum.valueOf((Class<? extends Enum>) typeDesc.clazz, name);
				field.set(obj, val);
			}
			else
			{
				Object val = readObject();
				field.set(obj, val);
			}
		}
	}

	/**
	 * Create and read an array from the stream.
	 * @param handle the handle of the array.
	 * @param cd the class descriptor for the array's class.
	 * @throws Exception if any error occurs.
	 */
	@SuppressWarnings("unchecked")
	private void readArray(final int handle, final ClassDescriptor cd) throws Exception
	{
		int len = in.readInt();
		if (traceEnabled) try { log.trace("reading array with signature=" + cd.signature + ", length=" + len); } catch(Exception e) {}
		ClassDescriptor eltDesc = caches.getDescriptor(cd.componentTypeHandle);
		Object obj = null;
		if (eltDesc.primitive)
		{
			switch(eltDesc.signature.charAt(0))
			{
				case 'B': byte[] barray = new byte[len];       obj = barray; in.read(barray, 0, len); break;
				case 'S': obj = readShortArray(len); break;
				case 'I': obj = readIntArray(len); break;
				case 'J': obj = readLongArray(len); break;
				case 'F': obj = readFloatArray(len); break;
				case 'D': obj = readDoubleArray(len); break;
				case 'C': obj = readCharArray(len); break;
				case 'Z': obj = readBooleanArray(len); break;
				/*
				case 'S': short[] sarray = new short[len];     obj = sarray; for (int i=0; i<len; i++) sarray[i] = in.readShort(); break;
				case 'I': int[] iarray = new int[len];         obj = iarray; for (int i=0; i<len; i++) iarray[i] = in.readInt(); break;
				case 'J': long[] larray = new long[len];       obj = larray; for (int i=0; i<len; i++) larray[i] = in.readLong(); break;
				case 'F': float[] farray = new float[len];     obj = farray; for (int i=0; i<len; i++) farray[i] = in.readFloat(); break;
				case 'D': double[] darray = new double[len];   obj = darray; for (int i=0; i<len; i++) darray[i] = in.readDouble(); break;
				case 'C': char[] carray = new char[len];       obj = carray; for (int i=0; i<len; i++) carray[i] = in.readChar(); break;
				case 'Z': boolean[] zarray = new boolean[len]; obj = zarray; for (int i=0; i<len; i++) zarray[i] = in.readBoolean(); break;
				 */
			}
			caches.handleToObjectMap.put(handle, obj);
		}
		else if (eltDesc.enumType)
		{
			obj = Array.newInstance(eltDesc.clazz, len);
			caches.handleToObjectMap.put(handle, obj);
			for (int i=0; i<len; i++)
			{
				String name = (String) readObject();
				//String name = in.readUTF();
				if (traceEnabled) try { log.trace("writing enum[" + eltDesc.signature + "] : " + name); } catch(Exception e) {}
				Object val = (name == null) ? null : Enum.valueOf((Class<Enum>) eltDesc.clazz, name);
				Array.set(obj, i, val);
			}
		}
		else
		{
			obj = Array.newInstance(eltDesc.clazz, len);
			caches.handleToObjectMap.put(handle, obj);
			for (int i=0; i<len; i++)
			{
				Object ref = readObject();
				Array.set(obj, i, ref);
			}
		}
	}

	/**
	 * Read all the class descriptors availablke in the input stream.
	 * @throws Exception if any error occurs.
	 */
	private void readClassDescriptors() throws Exception
	{
		int n = in.readInt();
		List<ClassDescriptor> list = new ArrayList<ClassDescriptor>(n > 0 ? n : 10);
		for (int i=0; i<n; i++)
		{
			ClassDescriptor cd = new ClassDescriptor();
			cd.read(in);
			caches.handleToDescriptorMap.put(cd.handle, cd);
			list.add(cd);
			if (traceEnabled) try { log.trace("read " + cd); } catch(Exception e) {}
		}
		caches.initializeDescriptorClasses(list, classloader);
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
	private Object newInstance(final ClassDescriptor cd) throws Exception
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
	 * Read an array of boolean values.
	 * @param len the length of the array to read.
	 * @return a boolean[] of the specified length.
	 * @throws Exception if any error occurs.
	 */
	private boolean[] readBooleanArray(final int len) throws Exception
	{
		boolean[] array = new boolean[len];
		for (int count=0; count<len;)
		{
			int n = Math.min(buf.length, len-count);
			in.read(buf, 0, n);
			for (int i=0; i<n; i++) array[count+i] = buf[i] != 0;
			count += n;
		}
		return array;
	}

	/**
	 * Read an array of char values.
	 * @param len the length of the array to read.
	 * @return a char[] of the specified length.
	 * @throws Exception if any error occurs.
	 */
	private char[] readCharArray(final int len) throws Exception
	{
		char[] array = new char[len];
		for (int count=0; count<len;)
		{
			int n = Math.min(buf.length/2, len-count);
			in.read(buf, 0, 2*n);
			for (int i=0; i<n; i++) array[count+i] = SerializationUtils.readChar(buf, 2*i);
			count += n;
		}
		return array;
	}

	/**
	 * Read an array of short values.
	 * @param len the length of the array to read.
	 * @return a short[] of the specified length.
	 * @throws Exception if any error occurs.
	 */
	private short[] readShortArray(final int len) throws Exception
	{
		short[] array = new short[len];
		for (int count=0; count<len;)
		{
			int n = Math.min(buf.length/2, len-count);
			in.read(buf, 0, 2*n);
			for (int i=0; i<n; i++) array[count+i] = SerializationUtils.readShort(buf, 2*i);
			count += n;
		}
		return array;
	}

	/**
	 * Read an array of int values.
	 * @param len the length of the array to read.
	 * @return a int[] of the specified length.
	 * @throws Exception if any error occurs.
	 */
	private int[] readIntArray(final int len) throws Exception
	{
		int[] array = new int[len];
		for (int count=0; count<len;)
		{
			int n = Math.min(buf.length/4, len-count);
			in.read(buf, 0, 4*n);
			for (int i=0; i<n; i++) array[count+i] = SerializationUtils.readInt(buf, 4*i);
			count += n;
		}
		return array;
	}

	/**
	 * Read an array of long values.
	 * @param len the length of the array to read.
	 * @return a long[] of the specified length.
	 * @throws Exception if any error occurs.
	 */
	private long[] readLongArray(final int len) throws Exception
	{
		long[] array = new long[len];
		for (int count=0; count<len;)
		{
			int n = Math.min(buf.length/8, len-count);
			in.read(buf, 0, 8*n);
			for (int i=0; i<n; i++) array[count+i] = SerializationUtils.readLong(buf, 8*i);
			count += n;
		}
		return array;
	}

	/**
	 * Read an array of float values.
	 * @param len the length of the array to read.
	 * @return a float[] of the specified length.
	 * @throws Exception if any error occurs.
	 */
	private float[] readFloatArray(final int len) throws Exception
	{
		float[] array = new float[len];
		for (int count=0; count<len;)
		{
			int n = Math.min(buf.length/4, len-count);
			in.read(buf, 0, 4*n);
			for (int i=0; i<n; i++) array[count+i] = Float.intBitsToFloat(SerializationUtils.readInt(buf, 4*i));
			count += n;
		}
		return array;
	}

	/**
	 * Read an array of double values.
	 * @param len the length of the array to read.
	 * @return a double[] of the specified length.
	 * @throws Exception if any error occurs.
	 */
	private double[] readDoubleArray(final int len) throws Exception
	{
		double[] array = new double[len];
		for (int count=0; count<len;)
		{
			int n = Math.min(buf.length/8, len-count);
			in.read(buf, 0, 8*n);
			for (int i=0; i<n; i++) array[count+i] = Double.longBitsToDouble(SerializationUtils.readLong(buf, 8*i));
			count += n;
		}
		return array;
	}
}
