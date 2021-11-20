/*
 * JPPF.
 * Copyright (C) 2005-2019 JPPF Team.
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
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.atomic.AtomicInteger;

import org.jppf.utils.StringUtils;
import org.slf4j.*;

/**
 * Instances of this class are intended to serialize object graphs to an underlying output stream.
 * @author Laurent Cohen
 * @exclude
 */
class Serializer {
  /**
   * Logger for this class.
   */
  private static final Logger log = LoggerFactory.getLogger(Serializer.class);
  /**
   * Determines whether the debug level is enabled in the log configuration, without the cost of a method call.
   */
  private static final boolean traceEnabled = log.isTraceEnabled();
  /**
   * The stream header ('JPPF' in ascii, '4A 50 50 46' in hexadecimal).
   */
  static final byte[] HEADER = { 74, 80, 80, 70 };
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
   * Special treatment when an object to serialize is a class.
   */
  static final byte STRING_HEADER = 5;
  /**
   * 
   */
  static final AtomicInteger instanceCount = new AtomicInteger(0);
  /**
   * 
   */
  final int instanceNumber = instanceCount.incrementAndGet();
  /**
   * The stream serialized data is written to.
   */
  ObjectOutputStream out;
  /**
   * Holds all class and object descriptors.
   */
  final SerializationCaches caches = new SerializationCaches();
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
  private final byte[] buf = new byte[SerializationUtils.TEMP_BUFFER_SIZE];

  /**
   * Initialize this serializer with the specified output stream, and write the header.
   * @param out the stream to which the serialized data is written.
   */
  Serializer(final ObjectOutputStream out) {
    this.out = out;
  }

  /**
   * Write the specified object to the output stream.
   * @param obj the object to write.
   * @throws Exception if any error occurs.
   */
  void writeObject(final Object obj) throws Exception {
    if (obj == null) out.writeByte(NULL_OBJECT_HEADER);
    else if (obj instanceof Class) writeClassObject((Class<?>) obj);
    else {
      Integer handle = caches.objectHandleMap.get(obj);
      final boolean isString = obj instanceof String;
      if (handle == null) {
        handle = caches.newObjectHandle(obj);
        if (traceEnabled) try { log.trace("writing handle = {}, object = {}", handle, StringUtils.toIdentityString(obj)); } catch(@SuppressWarnings("unused") final Exception e) {}
        if (isString) {
          writeHeaderAndHandle(STRING_HEADER, handle);
          writeString((String) obj);
        } else writeObject(obj, handle);
      } else {
        writeHeaderAndHandle(OBJECT_HEADER, handle);
      }
    }
  }

  /**
   * Write the specified object to the output stream.
   * @param obj the object to write.
   * @param handle the object's handle
   * @throws Exception if any error occurs.
   */
  private void writeObject(final Object obj, final int handle) throws Exception {
    final Map<Class<?>, ClassDescriptor> cdMap = SerializationCaches.createClassKeyMap();
    final ClassDescriptor cd = caches.getClassDescriptor(obj.getClass(), cdMap);
    currentObject = obj;
    currentClassDescriptor = cd;
    writeHeaderAndHandle(OBJECT_HEADER, handle);
    writeString(cd.signature);
    //if (traceEnabled) try { log.trace("writing object " + obj + ", handle=" + handle + ", class=" + obj.getClass() + ", cd=" + cd); } catch(Exception e) {}
    if (cd.array) writeArray(obj, cd);
    else if (cd.enumType) writeString(((Enum<?>) obj).name());
    else {
      final SerializationHandler serializationHandler = SerializationReflectionHelper.getSerializationHandler(cd.clazz);
      if (serializationHandler != null) serializationHandler.writeObject(obj, this, cd);
      else writeFields(obj, cd);
    }
  }

  /**
   * Write the specified object to the output stream.
   * @param obj the object to write.
   * @throws Exception if any error occurs.
   */
  private void writeClassObject(final Class<?> obj) throws Exception {
    final Map<Class<?>, ClassDescriptor> cdMap = new IdentityHashMap<>();
    final ClassDescriptor cd = caches.getClassDescriptor(obj, cdMap);
    caches.objectHandleMap.get(obj);
    currentObject = obj;
    currentClassDescriptor = cd;
    out.writeByte(CLASS_OBJECT_HEADER);
    writeString(cd.signature);
  }

  /**
   * Write the all fields, including those declared in the superclasses, for the specified object.
   * @param obj the object whose fields are to be written.
   * @param cd the object's class descriptor.
   * @throws Exception if any error occurs.
   */
  void writeFields(final Object obj, final ClassDescriptor cd) throws Exception {
    ClassDescriptor tmpDesc = cd;
    final Deque<ClassDescriptor> stack = new LinkedBlockingDeque<>();
    while (tmpDesc != null) {
      stack.addFirst(tmpDesc);
      tmpDesc = tmpDesc.superClass;
    }
    for (final ClassDescriptor desc: stack) {
      /*final SerializationHandler handler = SerializationReflectionHelper.getSerializationHandler(desc.clazz);
      if (handler != null) handler.writeDeclaredFields(this, desc, obj);
      else*/ if (desc.hasReadWriteObject) {
        final Method m = desc.writeObjectMethod;
        //if (traceEnabled) try { log.trace("invoking writeObject() for class=" + desc + " on object " + obj.hashCode()); } catch(Exception e) { log.trace(e.getMessage(), e); }
        try {
          tmpDesc = currentClassDescriptor;
          currentClassDescriptor = desc;
          m.invoke(obj, out);
        } finally {
          currentClassDescriptor = tmpDesc;
        }
      }
      else if (desc.externalizable) ((Externalizable) obj).writeExternal(out);
      else writeDeclaredFields(obj, desc);
    }
  }

  /**
   * Write the fields for the specified object and class descriptor.
   * @param obj the object whose fields are to be written.
   * @param cd the object's class descriptor.
   * @throws Exception if any error occurs.
   */
  void writeDeclaredFields(final Object obj, final ClassDescriptor cd) throws Exception {
    for (FieldDescriptor fd: cd.fields) {
      //if (traceEnabled) try { log.trace("writing field '" + fd.name + "' of object " + obj); } catch(Exception e) {}
      final Object val = fd.field.get(obj);
      if (fd.type.primitive) {
        switch(fd.type.signature.charAt(0)) {
          case 'B': out.write(((Byte) val).intValue()); break;
          case 'S': out.writeShort((Short) val); break;
          case 'I': writeInt((Integer) val); break;
          case 'J': writeLong((Long) val); break;
          case 'F': writeFloat((Float) val); break;
          case 'D': writeDouble((Double) val); break;
          case 'C': out.writeChar((Character) val); break;
          case 'Z': out.writeBoolean((Boolean) val); break;
        }
      }
      else if (fd.type.enumType) writeObject(val == null ? null : ((Enum<?>) val).name());
      else writeObject(val);
    }
  }

  /**
   * Write the length and elements for the specified array.
   * @param obj the array to write.
   * @param cd the array's class descriptor.
   * @throws Exception if any error occurs.
   */
  private void writeArray(final Object obj, final ClassDescriptor cd) throws Exception {
    final int n = Array.getLength(obj);
    writeInt(n);
    final ClassDescriptor eltDesc = cd.componentType;
    if (eltDesc.primitive) {
      switch(eltDesc.signature.charAt(0)) {
        case 'B': out.write((byte[]) obj, 0, n); break;
        case 'S': writeShortArray((short[]) obj); break;
        case 'I': writeIntArray((int[]) obj); break;
        case 'J': writeLongArray((long[]) obj); break;
        case 'F': writeFloatArray((float[]) obj); break;
        case 'D': writeDoubleArray((double[]) obj); break;
        case 'C': writeCharArray((char[]) obj); break;
        case 'Z': writeBooleanArray((boolean[]) obj); break;
      }
    } else if (eltDesc.enumType) {
      for (int i=0; i<n; i++) {
        final Object val = Array.get(obj, i);
        final String name = (val == null) ? null : ((Enum<?>) val).name();
        writeObject(name);
      }
    } else {
      for (int i=0; i<n; i++) {
        final Object val = Array.get(obj, i);
        writeObject(val);
      }
    }
  }

  /**
   * Write an array of booleans to the stream.
   * @param array the array of boolean values to write.
   * @throws Exception if any error occurs.
   */
  void writeBooleanArray(final boolean[] array) throws Exception {
    for (int count=0; count < array.length;) {
      final int n = Math.min(buf.length, array.length - count);
      for (int i=0; i<n; i++) SerializationUtils.writeBoolean(array[count++], buf, i);
      out.write(buf, 0, n);
    }
  }

  /**
   * Write an array of chars to the stream.
   * @param array the array of char values to write.
   * @throws Exception if any error occurs.
   */
  void writeCharArray(final char[] array) throws Exception {
    for (int count=0; count<array.length;) {
      final int n = Math.min(buf.length / 2, array.length - count);
      for (int i=0; i<2*n; i+=2) SerializationUtils.writeChar(array[count++], buf, i);
      out.write(buf, 0, 2*n);
    }
  }

  /**
   * Write an array of chars to the stream.
   * @param array the array of char values to write.
   * @throws Exception if any error occurs.
   */
  void writeShortArray(final short[] array) throws Exception {
    for (int count=0; count < array.length;) {
      final int n = Math.min(buf.length / 2, array.length - count);
      for (int i=0; i<2*n; i+=2) SerializationUtils.writeShort(array[count++], buf, i);
      out.write(buf, 0, 2*n);
    }
  }

  /**
   * Write an array of ints to the stream.
   * @param array the array of int values to write.
   * @throws Exception if any error occurs.
   */
  void writeIntArray(final int[] array) throws Exception {
    for (int count=0; count < array.length;) {
      final int n = Math.min(buf.length / 4, array.length - count);
      for (int i=0; i<4*n; i+=4) SerializationUtils.writeInt(array[count++], buf, i);
      out.write(buf, 0, 4*n);
    }
  }

  /**
   * Write an array of longs to the stream.
   * @param array the array of long values to write.
   * @throws Exception if any error occurs.
   */
  void writeLongArray(final long[] array) throws Exception {
    for (int count=0; count < array.length;) {
      final int n = Math.min(buf.length / 8, array.length - count);
      for (int i=0; i<8*n; i+=8) SerializationUtils.writeLong(array[count++], buf, i);
      out.write(buf, 0, 8*n);
    }
  }

  /**
   * Write an array of floats to the stream.
   * @param array the array of float values to write.
   * @throws Exception if any error occurs.
   */
  void writeFloatArray(final float[] array) throws Exception {
    for (int count=0; count < array.length;) {
      final int n = Math.min(buf.length / 4, array.length - count);
      for (int i=0; i<4*n; i+=4) SerializationUtils.writeInt(Float.floatToIntBits(array[count++]), buf, i);
      out.write(buf, 0, 4*n);
    }
  }

  /**
   * Write an array of doubles to the stream.
   * @param array the array of double values to write.
   * @throws Exception if any error occurs.
   */
  void writeDoubleArray(final double[] array) throws Exception {
    for (int count=0; count < array.length;) {
      final int n = Math.min(buf.length / 8, array.length - count);
      for (int i=0; i<8*n; i+=8) SerializationUtils.writeLong(Double.doubleToLongBits(array[count++]), buf, i);
      out.write(buf, 0, 8*n);
    }
  }

  /**
   * Write a string to the underlying stream.
   * @param s the string to write.
   * @throws Exception if any error occurs.
   */
  void writeString(final String s) throws Exception {
    final int len = s.length();
    final char[] chars = SerializationReflectionHelper.getStringValue(s);
    final boolean ascii = SerializationUtils.isASCII(chars);
    SerializationUtils.writeStringLength(out, ascii, len, buf);
    if (len == 0) return;
    if (ascii) {
      if (traceEnabled) log.trace("writing ASCII string for len={}", len);
      for (int count=0; count<len;) {
        final int n = Math.min(buf.length, len - count);
        //for (int i=0; i<n; i++) buf[i] = (byte) (chars[count++] & 0x7F);
        for (int i=0; i<n; i++) buf[i] = (byte) chars[count++];
        out.write(buf, 0, n);
      }
    } else if (len <= 65535/3) {
      if (traceEnabled) log.trace("calling writeUTF() for len={}", len);
      out.writeUTF(s); // for writeUTF() : max bytes = 64k-1, max bytes per char = 3
    } else {
      if (traceEnabled) log.trace("writing normal string for len={}", len);
      writeCharArray(chars);
    }
  }

  /**
   * Write the header byte and handle of the next serialized entity to the underlying stream.
   * @param header the header byte to write.
   * @param handle the handle to write.
   * @throws Exception if any error occurs.
   */
  void writeHeaderAndHandle(final byte header, final int handle)  throws Exception {
    byte b = header;
    byte n = 4;
    for (int i=0; i<SerializationUtils.INT_MAX_VALUES.length; i++) {
      if (handle < SerializationUtils.INT_MAX_VALUES[i]) {
        n = (byte) (i + 1);
        break;
      }
    }
    b |= (n << 4) & 0xF0;
    buf[0] = b;
    for (int i=8*(n-1), pos=1; i>=0; i-=8) buf[pos++] = (byte) ((handle >>> i) & 0xFF);
    out.write(buf, 0, n+1);
  }


  /**
   * Write an int to the stream.
   * @param value the int value to write.
   * @return the number of bytes written to the buffer.
   * @throws Exception if any error occurs.
   */
  int writeInt(final int value) throws Exception {
    final int n = SerializationUtils.writeVarInt(out, value, buf);
    return n;
  }

  /**
   * Write a long to the stream.
   * @param value the long value to write.
   * @return the number of bytes written to the buffer.
   * @throws Exception if any error occurs.
   */
  int writeLong(final long value) throws Exception {
    final int n = SerializationUtils.writeVarLong(out, value, buf);
    return n;
  }

  /**
   * Write a float to the stream.
   * @param value the float value to write.
   * @return the number of bytes written to the buffer.
   * @throws Exception if any error occurs.
   */
  int writeFloat(final float value) throws Exception {
    return writeInt(Float.floatToIntBits(value));
  }

  /**
   * Write a double to the stream.
   * @param value the double value to write.
   * @return the number of bytes written to the buffer.
   * @throws Exception if any error occurs.
   */
  int writeDouble(final double value) throws Exception {
   return writeLong(Double.doubleToLongBits(value));
  }
}
