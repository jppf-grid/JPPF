/*
 * JPPF.
 * Copyright (C) 2005-2012 JPPF Team.
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
import java.lang.reflect.Array;
import java.util.*;

import org.jppf.utils.SerializationUtils;
import org.slf4j.*;

/**
 * Instances of this class are intended to serialize object graphs to an underlying output stream.
 * @author Laurent Cohen
 * @exclude
 */
class Serializer
{
  /**
   * The stream header ('JPPF' in ascii).
   */
  static final byte[] HEADER = { 74, 80, 80, 70 };
  /**
   * Handle for null references.
   */
  static final byte[] NULL_HANDLE = { 0, 0, 0, 0 };
  /**
   * Header written before a class descriptor.
   */
  static final byte CLASS_HEADER = 1;
  /**
   * Header written before a serialized object.
   */
  static final byte OBJECT_HEADER = 2;
  /**
   * Header written before a serialized object.
   */
  static final byte NULL_OBJECT_HEADER = 3;
  /**
   * Special treatment when an object to serialize is a class.
   */
  static final byte CLASS_OBJECT_HEADER = 4;
  /**
   * Logger for this class.
   */
  private static Logger log = LoggerFactory.getLogger(Serializer.class);
  /**
   * Determines whether the debug level is enabled in the log configuration, without the cost of a method call.
   */
  private static boolean traceEnabled = log.isTraceEnabled();
  /**
   * The stream serialized data is written to.
   */
  ObjectOutputStream out;
  /**
   * Holds all class and object descriptors.
   */
  SerializationCaches caches = new SerializationCaches();
  /**
   * Determines if the root has already been written to the stream.
   */
  private boolean rootWritten = false;
  /**
   * Descriptor the class of the object currently being written.
   */
  ClassDescriptor currentClassDescriptor;
  /**
   * The object currently being written.
   */
  Object currentObject;
  /**
   * Temporary buffer used to write arrays of primitive values to the stream.
   */
  private byte[] buf = new byte[4096];

  /**
   * Initialize this serializer with the specified output stream, and write the header.
   * @param out the stream to which the serialized data is written.
   * @throws IOException if an error occurs while writing the header.
   */
  Serializer(final ObjectOutputStream out) throws IOException
  {
    this.out = out;
    out.write(HEADER);
  }

  /**
   * Write the specified object to the output stream.
   * @param obj the object to write.
   * @throws Exception if any error occurs.
   */
  void writeObject(final Object obj) throws Exception
  {
    if (obj == null) out.writeByte(NULL_OBJECT_HEADER);
    else if (obj instanceof Class) writeClassObject((Class) obj);
    else
    {
      Integer handle = caches.objectHandleMap.get(obj);
      if (handle == null)
      {
        handle = caches.newObjectHandle(obj);
        writeObject(obj, handle);
      }
      else
      {
        out.writeByte(OBJECT_HEADER);
        out.writeInt(handle);
      }
    }
  }

  /**
   * Write the specified object to the output stream.
   * @param obj the object to write.
   * @param handle the object's handle
   * @throws Exception if any error occurs.
   */
  private void writeObject(final Object obj, final int handle) throws Exception
  {
    Map<Class<?>, ClassDescriptor> map = new HashMap<Class<?>, ClassDescriptor>();
    ClassDescriptor cd = caches.getClassDescriptor(obj.getClass(), map);
    currentObject = obj;
    currentClassDescriptor = cd;
    writeClassDescriptors(map);
    map = null;
    out.writeByte(OBJECT_HEADER);
    out.writeInt(handle);
    out.writeInt(cd.handle);
    //if (traceEnabled) try { log.trace("writing object " + obj + ", handle=" + handle + ", class=" + obj.getClass() + ", cd=" + cd); } catch(Exception e) {}
    if (cd.hasWriteObject)
    {
      if (!cd.writeObjectMethod.isAccessible()) cd.writeObjectMethod.setAccessible(true);
      cd.writeObjectMethod.invoke(obj, out);
    }
    else if (cd.externalizable) ((Externalizable) obj).writeExternal(out);
    else if (cd.array) writeArray(obj, cd);
    else if (cd.enumType)
    {
      String name = ((Enum) obj).name();
      writeObject(name);
    }
    else writeFields(obj, cd);
  }

  /**
   * Write the specified object to the output stream.
   * @param obj the object to write.
   * @throws Exception if any error occurs.
   */
  private void writeClassObject(final Class obj) throws Exception
  {
    Map<Class<?>, ClassDescriptor> map = new HashMap<Class<?>, ClassDescriptor>();
    ClassDescriptor cd = caches.getClassDescriptor(obj, map);
    currentObject = obj;
    currentClassDescriptor = cd;
    writeClassDescriptors(map);
    out.writeByte(CLASS_OBJECT_HEADER);
    out.writeInt(cd.handle);
  }

  /**
   * Write the all fields, including those declared in the superclasses, for the specified object.
   * @param obj the object whose fields are to be written.
   * @param cd the object's class descriptor.
   * @throws Exception if any error occurs.
   */
  void writeFields(final Object obj, final ClassDescriptor cd) throws Exception
  {
    ClassDescriptor tmpDesc = cd;
    while (tmpDesc != null)
    {
      writeDeclaredFields(obj, tmpDesc);
      tmpDesc = tmpDesc.superClass;
    }
  }

  /**
   * Write the fields for the specified object and class descriptor.
   * @param obj the object whose fields are to be written.
   * @param cd the object's class descriptor.
   * @throws Exception if any error occurs.
   */
  private void writeDeclaredFields(final Object obj, final ClassDescriptor cd) throws Exception
  {
    for (int i=0; i<cd.fields.length; i++)
    {
      FieldDescriptor fd = cd.fields[i];
      //if (traceEnabled) try { log.trace("writing field '" + fd.name + "' of object " + obj); } catch(Exception e) {}
      if (!fd.field.isAccessible()) fd.field.setAccessible(true);
      Object val = fd.field.get(obj);
      if (fd.type.primitive)
      {
        switch(fd.type.signature.charAt(0))
        {
          case 'B': out.write((Integer) val); break;
          case 'S': out.writeShort((Short) val); break;
          case 'I': out.writeInt((Integer) val); break;
          case 'J': out.writeLong((Long) val); break;
          case 'F': out.writeFloat((Float) val); break;
          case 'D': out.writeDouble((Double) val); break;
          case 'C': out.writeChar((Integer) val); break;
          case 'Z': out.writeBoolean((Boolean) val); break;
        }
      }
      else if (fd.type.enumType)
      {
        String name = (val == null) ? null : ((Enum) val).name();
        writeObject(name);
      }
      else writeObject(val);
    }
  }

  /**
   * Write the length and elements for the specified array.
   * @param obj the array to write.
   * @param cd the array's class descriptor.
   * @throws Exception if any error occurs.
   */
  private void writeArray(final Object obj, final ClassDescriptor cd) throws Exception
  {
    int n = Array.getLength(obj);
    out.writeInt(n);
    ClassDescriptor eltDesc = cd.componentType;
    if (eltDesc.primitive)
    {
      switch(eltDesc.signature.charAt(0))
      {
        case 'B': out.write((byte[]) obj, 0, n); break;
        case 'S': writeShortArray((short[]) obj); break;
        case 'I': writeIntArray((int[]) obj); break;
        case 'J': writeLongArray((long[]) obj); break;
        case 'F': writeFloatArray((float[]) obj); break;
        case 'D': writeDoubleArray((double[]) obj); break;
        case 'C': writeCharArray((char[]) obj); break;
        case 'Z': writeBooleanArray((boolean[]) obj); break;
      }
    }
    else if (eltDesc.enumType)
    {
      for (int i=0; i<n; i++)
      {
        Object val = Array.get(obj, i);
        String name = (val == null) ? null : ((Enum) val).name();
        writeObject(name);
      }
    }
    else
    {
      for (int i=0; i<n; i++)
      {
        Object val = Array.get(obj, i);
        writeObject(val);
      }
    }
  }

  /**
   * Write all class descriptors to the output stream.
   * @param map a class to descriptor association map.
   * @throws IOException if any error occurs.
   */
  private void writeClassDescriptors(final Map<Class<?>, ClassDescriptor> map) throws IOException
  {
    if (map.isEmpty()) return;
    out.writeByte(CLASS_HEADER);
    out.writeInt(map.size());
    for (Map.Entry<Class<?>, ClassDescriptor> entry: map.entrySet()) entry.getValue().write(out);
  }

  /**
   * Write an array of booleans to the stream.
   * @param array the array of boolean values to write.
   * @throws Exception if any error occurs.
   */
  private void writeBooleanArray(final boolean[] array) throws Exception
  {
    for (int count=0; count < array.length;)
    {
      int n = Math.min(buf.length, array.length - count);
      for (int i=0; i<n; i++) SerializationUtils.writeBoolean(array[count+i], buf, i);
      out.write(buf, 0, n);
      count += n;
    }
  }

  /**
   * Write an array of chars to the stream.
   * @param array the array of char values to write.
   * @throws Exception if any error occurs.
   */
  private void writeCharArray(final char[] array) throws Exception
  {
    for (int count=0; count < array.length;)
    {
      int n = Math.min(buf.length / 2, array.length - count);
      for (int i=0; i<n; i++) SerializationUtils.writeChar(array[count+i], buf, 2*i);
      out.write(buf, 0, 2*n);
      count += n;
    }
  }

  /**
   * Write an array of chars to the stream.
   * @param array the array of char values to write.
   * @throws Exception if any error occurs.
   */
  private void writeShortArray(final short[] array) throws Exception
  {
    for (int count=0; count < array.length;)
    {
      int n = Math.min(buf.length / 2, array.length - count);
      for (int i=0; i<n; i++) SerializationUtils.writeShort(array[count+i], buf, 2*i);
      out.write(buf, 0, 2*n);
      count += n;
    }
  }

  /**
   * Write an array of ints to the stream.
   * @param array the array of int values to write.
   * @throws Exception if any error occurs.
   */
  private void writeIntArray(final int[] array) throws Exception
  {
    for (int count=0; count < array.length;)
    {
      int n = Math.min(buf.length / 4, array.length - count);
      for (int i=0; i<n; i++) SerializationUtils.writeInt(array[count+i], buf, 4*i);
      out.write(buf, 0, 4*n);
      count += n;
    }
  }

  /**
   * Write an array of longs to the stream.
   * @param array the array of long values to write.
   * @throws Exception if any error occurs.
   */
  private void writeLongArray(final long[] array) throws Exception
  {
    for (int count=0; count < array.length;)
    {
      int n = Math.min(buf.length / 8, array.length - count);
      for (int i=0; i<n; i++) SerializationUtils.writeLong(array[count+i], buf, 8*i);
      out.write(buf, 0, 8*n);
      count += n;
    }
  }

  /**
   * Write an array of floats to the stream.
   * @param array the array of float values to write.
   * @throws Exception if any error occurs.
   */
  private void writeFloatArray(final float[] array) throws Exception
  {
    for (int count=0; count < array.length;)
    {
      int n = Math.min(buf.length / 4, array.length - count);

      for (int i=0; i<n; i++) SerializationUtils.writeInt(Float.floatToIntBits(array[count+i]), buf, 4*i);
      out.write(buf, 0, 4*n);
      count += n;
    }
  }

  /**
   * Write an array of doubles to the stream.
   * @param array the array of double values to write.
   * @throws Exception if any error occurs.
   */
  private void writeDoubleArray(final double[] array) throws Exception
  {
    for (int count=0; count < array.length;)
    {
      int n = Math.min(buf.length / 8, array.length - count);
      for (int i=0; i<n; i++) SerializationUtils.writeLong(Double.doubleToLongBits(array[count+i]), buf, 8*i);
      out.write(buf, 0, 8*n);
      count += n;
    }
  }
}
