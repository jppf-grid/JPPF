/*
 * JPPF.
 * Copyright (C) 2005-2015 JPPF Team.
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
  private static Logger log = LoggerFactory.getLogger(Serializer.class);
  /**
   * Determines whether the debug level is enabled in the log configuration, without the cost of a method call.
   */
  private static boolean traceEnabled = log.isTraceEnabled();
  /**
   * The stream header ('JPPF' in ascii).
   */
  static final byte[] HEADER = { 74, 80, 80, 70 };
  /**
   * .
   */
  private static final int[] HANDLE_MAX_VALUES = { 128, 128 << 8, 128 << 16 };
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
   * The stream serialized data is written to.
   */
  ObjectOutputStream out;
  /**
   * Holds all class and object descriptors.
   */
  SerializationCaches caches = new SerializationCaches();
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
  private byte[] buf = new byte[SerializationUtils.TEMP_BUFFER_SIZE];

  /**
   * Initialize this serializer with the specified output stream, and write the header.
   * @param out the stream to which the serialized data is written.
   * @throws IOException if an error occurs while writing the header.
   */
  Serializer(final ObjectOutputStream out) throws IOException {
    this.out = out;
    out.write(HEADER);
  }

  /**
   * Write the specified object to the output stream.
   * @param obj the object to write.
   * @throws Exception if any error occurs.
   */
  void writeObject(final Object obj) throws Exception {
    if (obj == null) out.writeByte(NULL_OBJECT_HEADER);
    else if (obj instanceof Class) writeClassObject((Class) obj);
    else {
      boolean isString = obj instanceof String;
      Integer handle = caches.objectHandleMap.get(obj);
      if (handle == null) {
        handle = caches.newObjectHandle(obj);
        if (traceEnabled) try { log.trace("writing handle = {}, object = {}", handle, StringUtils.toIdentityString(obj)); } catch(Exception e) {}
        if (isString) {
          writeHeaderAndHandle(STRING_HEADER, handle);
          writeString((String) obj);
        } else writeObject(obj, handle);
      } else {
        writeHeaderAndHandle(isString ? STRING_HEADER : OBJECT_HEADER, handle);
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
    Map<Class<?>, ClassDescriptor> cdMap = SerializationCaches.createClassKeyMap();
    ClassDescriptor cd = caches.getClassDescriptor(obj.getClass(), cdMap);
    currentObject = obj;
    currentClassDescriptor = cd;
    writeClassDescriptors(cdMap);
    writeHeaderAndHandle(OBJECT_HEADER, handle);
    writeClassHandle(cd.handle);
    //if (traceEnabled) try { log.trace("writing object " + obj + ", handle=" + handle + ", class=" + obj.getClass() + ", cd=" + cd); } catch(Exception e) {}
    if (cd.array) writeArray(obj, cd);
    else if (cd.enumType) writeString(((Enum) obj).name());
    else writeFields(obj, cd);
  }

  /**
   * Write the specified object to the output stream.
   * @param obj the object to write.
   * @throws Exception if any error occurs.
   */
  private void writeClassObject(final Class obj) throws Exception {
    Map<Class<?>, ClassDescriptor> cdMap = new IdentityHashMap<>();
    ClassDescriptor cd = caches.getClassDescriptor(obj, cdMap);
    currentObject = obj;
    currentClassDescriptor = cd;
    writeClassDescriptors(cdMap);
    writeHeaderAndHandle(CLASS_OBJECT_HEADER, cd.handle);
  }

  /**
   * Write the all fields, including those declared in the superclasses, for the specified object.
   * @param obj the object whose fields are to be written.
   * @param cd the object's class descriptor.
   * @throws Exception if any error occurs.
   */
  void writeFields(final Object obj, final ClassDescriptor cd) throws Exception {
    ClassDescriptor tmpDesc = cd;
    Deque<ClassDescriptor> stack = new LinkedBlockingDeque<>();
    while (tmpDesc != null) {
      stack.addFirst(tmpDesc);
      tmpDesc = tmpDesc.superClass;
    }
    for (ClassDescriptor desc: stack) {
      if (desc.hasReadWriteObject) {
        Method m = desc.writeObjectMethod;
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
    for (int i=0; i<cd.fields.length; i++) {
      FieldDescriptor fd = cd.fields[i];
      //if (traceEnabled) try { log.trace("writing field '" + fd.name + "' of object " + obj); } catch(Exception e) {}
      Object val = fd.field.get(obj);
      if (fd.type.primitive) {
        switch(fd.type.signature.charAt(0)) {
          case 'B': out.write((Integer) val); break;
          case 'S': out.writeShort((Short) val); break;
          case 'I': writeInt((Integer) val); break;
          case 'J': writeLong((Long) val); break;
          case 'F': writeFloat((Float) val); break;
          case 'D': writeDouble((Double) val); break;
          case 'C': out.writeChar((Integer) val); break;
          case 'Z': out.writeBoolean((Boolean) val); break;
        }
      }
      else if (fd.type.enumType) writeObject(val == null ? null : ((Enum) val).name());
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
    int n = Array.getLength(obj);
    writeInt(n);
    ClassDescriptor eltDesc = cd.componentType;
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
        Object val = Array.get(obj, i);
        String name = (val == null) ? null : ((Enum) val).name();
        writeObject(name);
      }
    } else {
      for (int i=0; i<n; i++) {
        Object val = Array.get(obj, i);
        writeObject(val);
      }
    }
  }

  /**
   * Write all class descriptors to the output stream.
   * @param map a class to descriptor association map.
   * @throws Exception if any error occurs.
   */
  void writeClassDescriptors(final Map<Class<?>, ClassDescriptor> map) throws Exception {
    if (map.isEmpty()) return;
    out.writeByte(CLASS_HEADER);
    writeInt(map.size());
    for (Map.Entry<Class<?>, ClassDescriptor> entry: map.entrySet()) {
      ClassDescriptor cd = entry.getValue();
      cd.write(this);
      if (traceEnabled) try { log.trace("wrote handle={}, cd={}", cd.handle, cd); } catch(Exception e) {}
    }
  }

  /**
   * Write an array of booleans to the stream.
   * @param array the array of boolean values to write.
   * @throws Exception if any error occurs.
   */
  void writeBooleanArray(final boolean[] array) throws Exception {
    for (int count=0; count < array.length;) {
      int n = Math.min(buf.length, array.length - count);
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
      int n = Math.min(buf.length / 2, array.length - count);
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
      int n = Math.min(buf.length / 2, array.length - count);
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
      int n = Math.min(buf.length / 4, array.length - count);
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
      int n = Math.min(buf.length / 8, array.length - count);
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
      int n = Math.min(buf.length / 4, array.length - count);
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
      int n = Math.min(buf.length / 8, array.length - count);
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
    int len = s.length();
    boolean ascii = SerializationUtils.isASCII(s);
    SerializationUtils.writeStringLength(out, ascii, len, buf);
    if (len == 0) return;
    if (ascii) {
      char[] chars = SerializationReflectionHelper.getStringValue(s);
      for (int count=0; count<len;) {
        int n = Math.min(buf.length, len - count);
        for (int i=0; i<n; i++) buf[i] = (byte) (chars[count++] & 0x7F);
        out.write(buf, 0, n);
      }
    } else if (len <= 65535/3) out.writeUTF(s); // for writeUTF() : max bytes = 64k-1, max bytes per char = 3
    else {
      char[] chars = SerializationReflectionHelper.getStringValue(s);
      for (int count=0; count<len;) {
        int n = Math.min(buf.length / 2, len - count);
        //for (int i=0; i<2*n; i+=2) SerializationUtils.writeChar(s.charAt(count++), buf, i);
        for (int i=0; i<2*n; i+=2) SerializationUtils.writeChar(chars[count++], buf, i);
        out.write(buf, 0, 2*n);
      }
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
    for (int i=0; i<HANDLE_MAX_VALUES.length; i++) {
      if (handle < HANDLE_MAX_VALUES[i]) {
        n = (byte) (i + 1);
        break;
      }
    }
    b |= (n << 4);
    buf[0] = b;
    for (int i=8*(n-1), pos=1; i>=0; i-=8) buf[pos++] = (byte) ((handle >>> i) & 0xFF);
    out.write(buf, 0, n+1);
  }


  /**
   * Write the handle of a class descriptor to the underlying stream.
   * @param handle the handle to write.
   * @throws Exception if any error occurs.
   */
  void writeClassHandle(final int handle)  throws Exception {
    byte n = 3;
    for (int i=0; i<2; i++) {
      if (handle < HANDLE_MAX_VALUES[i]) {
        n = (byte) (i + 1);
        break;
      }
    }
    buf[0] = n;
    for (int i=8*(n-1), pos=1; i>=0; i-=8) buf[pos++] = (byte) ((handle >>> i) & 0xFF);
    out.write(buf, 0, n+1);
  }

  /**
   * Write an int to the stream.
   * @param value the int value to write.
   * @throws Exception if any error occurs.
   */
  void writeInt(final int value) throws Exception {
    SerializationUtils.writeVarInt(out, value, buf);
  }

  /**
   * Write a long to the stream.
   * @param value the long value to write.
   * @throws Exception if any error occurs.
   */
  void writeLong(final long value) throws Exception {
    SerializationUtils.writeVarLong(out, value, buf);
  }

  /**
   * Write a float to the stream.
   * @param value the float value to write.
   * @throws Exception if any error occurs.
   */
  void writeFloat(final float value) throws Exception {
    writeInt(Float.floatToIntBits(value));
  }

  /**
   * Write a double to the stream.
   * @param value the double value to write.
   * @throws Exception if any error occurs.
   */
  void writeDouble(final double value) throws Exception {
   writeLong(Double.doubleToLongBits(value));
  }
}
