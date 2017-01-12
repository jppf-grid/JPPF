/*
 * JPPF.
 * Copyright (C) 2005-2016 JPPF Team.
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
import java.util.Deque;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.atomic.AtomicInteger;

import org.jppf.serialization.SerializationUtils.StringLengthDesc;
import org.jppf.utils.StringUtils;
import org.slf4j.*;

/**
 * Instances of this class are intended to deserialize object graphs from an underlying input stream.
 * @author Laurent Cohen
 * @exclude
 */
class Deserializer {
  /**
   * Logger for this class.
   */
  private static Logger log = LoggerFactory.getLogger(Deserializer.class);
  /**
   * Determines whether the debug level is enabled in the log configuration, without the cost of a method call.
   */
  private static boolean traceEnabled = log.isTraceEnabled();
  /**
   * Count of instances of this class, used for debugging.
   */
  static final AtomicInteger instanceCount = new AtomicInteger(0);
  /**
   * Sequence number for this instance, used for debugging.
   */
  final int instanceNumber = instanceCount.incrementAndGet();
  /**
   * The underlying input stream.
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
   * Handle of the object being read.
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
  byte[] buf = new byte[SerializationUtils.TEMP_BUFFER_SIZE];

  /**
   * Initialize this deserializer with the specified input stream.
   * @param in the stream from which objects are read.
   */
  Deserializer(final ObjectInputStream in) {
    this.in = in;
  }

  /**
   * Read an object graph from the stream.
   * @return the next object read from the stream.
   * @throws Exception if any error occurs.
   */
  Object readObject() throws Exception {
    byte header = in.readByte();
    int type = header & 0x0F;
    if (type == Serializer.NULL_OBJECT_HEADER) return null;
    else if (type == Serializer.CLASS_OBJECT_HEADER) return readClassObject(header);
    int handle = readHandle(header);
    if (traceEnabled) try { log.trace("read object header={}, handle={}", header, handle); } catch(@SuppressWarnings("unused") Exception e) {}
    Object obj = caches.handleToObjectMap.get(handle);
    if (obj != null) return obj;
    if (traceEnabled) log.trace("reading object with handle = {}", handle);
    if (type == Serializer.STRING_HEADER) {
      if (traceEnabled) log.trace("reading string");
      obj = readString();
      caches.handleToObjectMap.put(handle, obj);
      if (traceEnabled) log.trace("read string = {}", obj);
      return obj;
    }
    readObject(handle);
    Object o = caches.handleToObjectMap.get(handle);
    if (traceEnabled) log.trace(String.format("read object %s, header=%d, handle=%d", o, header, handle));
    return o;
  }

  /**
   * Read the next object in the stream.
   * @param handle the handle of the object to read.
   * @throws Exception if any error occurs.
   */
  @SuppressWarnings("unchecked")
  private void readObject(final int handle) throws Exception {
    if (traceEnabled) log.trace("reading object with handle = {}", handle);
    String sig = readString();
    ClassDescriptor cd = caches.getDescriptor(sig, classloader);
    if (cd.array) readArray(handle, cd);
    else if (cd.enumType) {
      String name = readString();
      if (traceEnabled) try { log.trace("reading enum[" + cd.signature + "] : " + name); } catch(@SuppressWarnings("unused") Exception e) {}
      @SuppressWarnings("rawtypes")
      Object val = (name == null) ? null : Enum.valueOf((Class<? extends Enum>) cd.clazz, name);
      caches.handleToObjectMap.put(handle, val);
    } else {
      Object obj = newInstance(cd);
      currentObject = obj;
      currentClassDescriptor = cd;
      if (traceEnabled) try { log.trace("reading handle={}, object={}", handle, StringUtils.toIdentityString(obj)); } catch(@SuppressWarnings("unused") Exception e) {}
      caches.handleToObjectMap.put(handle, obj);
      readFields(cd, obj);
    }
  }

  /**
   * Read the next object in the stream, which is a class object.
   * @param header indicates the number fo bytes for the handle.
   * @return the class object whose handle was read.
   * @throws Exception if any error occurs.
   */
  private Object readClassObject(final byte header) throws Exception {
    String handle = readString();
    return caches.getClassFromHandle(handle, classloader);
  }

  /**
   * Read all the fields for the specified object.
   * @param cd the class descriptor for the object.
   * @param obj the object to set the fields on.
   * @throws Exception if any error occurs.
   */
  void readFields(final ClassDescriptor cd, final Object obj) throws Exception {
    ClassDescriptor tmpDesc = cd;
    if (traceEnabled) try { log.trace("reading fields for object = {}, class = {}", StringUtils.toIdentityString(obj), cd); } catch(@SuppressWarnings("unused") Exception e) {}
    Deque<ClassDescriptor> stack = new LinkedBlockingDeque<>();
    while (tmpDesc != null) {
      stack.addFirst(tmpDesc);
      tmpDesc = tmpDesc.superClass;
    }
    for (ClassDescriptor desc: stack) {
      SerializationHandler handler = SerializationReflectionHelper.getSerializationHandler(desc.clazz);
      if (handler != null) handler.readDeclaredFields(this, desc, obj);
      else if (desc.hasReadWriteObject) {
        Method m = desc.readObjectMethod;
        if (traceEnabled) try { log.trace("invoking readObject() for object = {}, class = {}", StringUtils.toIdentityString(obj), desc); } catch(@SuppressWarnings("unused") Exception e) {}
        try {
          tmpDesc = currentClassDescriptor;
          currentClassDescriptor = desc;
          m.invoke(obj, in);
        } finally {
          currentClassDescriptor = tmpDesc;
        }
      }
      else if (desc.externalizable) ((Externalizable) obj).readExternal(in);
      else readDeclaredFields(desc, obj);
    }
  }

  /**
   * Read the fields declared by the class described by the specified class descriptor.
   * @param cd the class descriptor to use.
   * @param obj the object to set the field values on.
   * @throws Exception if any error occurs.
   */
  @SuppressWarnings("unchecked")
  void readDeclaredFields(final ClassDescriptor cd, final Object obj) throws Exception {
    if (traceEnabled) try { log.trace("reading declared fields for object = {}, class = {}", StringUtils.toIdentityString(obj), cd); } catch(@SuppressWarnings("unused") Exception e) {}
    for (FieldDescriptor fd: cd.fields) {
      //if (traceEnabled) try { log.trace("reading field '{}' of object {}", fd, obj); } catch(@SuppressWarnings("unused") Exception e) {}
      ClassDescriptor typeDesc = fd.type;
      if (fd.field == null) fd.field = cd.clazz.getDeclaredField(fd.name);
      Field field = fd.field;
      if (typeDesc.primitive) {
        switch(typeDesc.signature.charAt(0)) {
          case 'B': field.setByte(obj, (byte) in.read()); break;
          case 'S': field.setShort(obj, in.readShort()); break;
          case 'I': field.setInt(obj, readInt()); break;
          case 'J': field.setLong(obj, readLong()); break;
          case 'F': field.setFloat(obj, readFloat()); break;
          case 'D': field.setDouble(obj, readDouble()); break;
          case 'C': field.setChar(obj, in.readChar()); break;
          case 'Z': field.setBoolean(obj, in.readBoolean()); break;
        }
      } else if (typeDesc.enumType) {
        String name = (String) readObject();
        if (traceEnabled) try { log.trace("reading enum[" + typeDesc.signature + "] : " + name); } catch(@SuppressWarnings("unused") Exception e) {}
        @SuppressWarnings("rawtypes")
        Object val = (name == null) ? null : Enum.valueOf((Class<? extends Enum>) field.getType(), name);
        field.set(obj, val);
      } else {
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
  private void readArray(final int handle, final ClassDescriptor cd) throws Exception {
    int len = readInt();
    if (traceEnabled) try { log.trace("reading array with signature=" + cd.signature + ", length=" + len); } catch(@SuppressWarnings("unused") Exception e) {}
    ClassDescriptor compCd = cd.componentType;
    if (compCd == null) {
      Class<?> compClass = cd.clazz.getComponentType();
      compCd = caches.classToDescMap.get(compClass);
      if (compCd == null) {
        compCd = new ClassDescriptor(compClass);
        caches.classToDescMap.put(compClass, compCd);
      }
    }
    Object obj = null;
    if (compCd.primitive) {
      switch(compCd.signature.charAt(0)) {
        case 'B': obj = readByteArray(len); break;
        case 'S': obj = readShortArray(len); break;
        case 'I': obj = readIntArray(len); break;
        case 'J': obj = readLongArray(len); break;
        case 'F': obj = readFloatArray(len); break;
        case 'D': obj = readDoubleArray(len); break;
        case 'C': obj = readCharArray(len); break;
        case 'Z': obj = readBooleanArray(len); break;
      }
      caches.handleToObjectMap.put(handle, obj);
    } else if (compCd.enumType) {
      obj = Array.newInstance(compCd.clazz, len);
      caches.handleToObjectMap.put(handle, obj);
      for (int i=0; i<len; i++) {
        String name = (String) readObject();
        if (traceEnabled) try { log.trace("read enum name {}", name); } catch(@SuppressWarnings("unused") Exception e) {}
        @SuppressWarnings("rawtypes")
        Object val = (name == null) ? null : Enum.valueOf((Class<Enum>) compCd.clazz, name);
        Array.set(obj, i, val);
      }
    } else {
      obj = Array.newInstance(compCd.clazz, len);
      caches.handleToObjectMap.put(handle, obj);
      for (int i=0; i<len; i++) {
        Object ref = readObject();
        Array.set(obj, i, ref);
      }
    }
  }

  /**
   * Initialize the class loader to use.
   * @return a {@link ClassLoader} instance.
   */
  private ClassLoader initClassLoader() {
    ClassLoader cl = Thread.currentThread().getContextClassLoader();
    return cl != null ? cl : getClass().getClassLoader();
  }

  /**
   * Create a new instance of the class described by the specified descriptor.
   * @param cd the class descriptor to use.
   * @return a new instance of the class.
   * @throws Exception if any error occurs.
   */
  private Object newInstance(final ClassDescriptor cd) throws Exception {
    return SerializationReflectionHelper.create(cd.clazz);
  }

  /**
   * Read an array of boolean values.
   * @param len the length of the array to read.
   * @return a boolean[] of the specified length.
   * @throws Exception if any error occurs.
   */
  byte[] readByteArray(final int len) throws Exception {
    byte[] array = new byte[len];
    for (int count=0; count<len;) {
      int n = in.read(array, count, len-count);
      if (n < 0) throw new EOFException(String.format("could only read %d bytes out of %d", count, len));
      count += n;
    }
    return array;
  }

  /**
   * Read an array of boolean values.
   * @param len the length of the array to read.
   * @return a boolean[] of the specified length.
   * @throws Exception if any error occurs.
   */
  boolean[] readBooleanArray(final int len) throws Exception {
    boolean[] array = new boolean[len];
    for (int count=0; count<len;) {
      int n = Math.min(buf.length, len-count);
      readToBuf(0, n);
      for (int i=0; i<n; i++) array[count++] = buf[i] != 0;
    }
    return array;
  }

  /**
   * Read an array of char values.
   * @param len the length of the array to read.
   * @return a char[] of the specified length.
   * @throws Exception if any error occurs.
   */
  char[] readCharArray(final int len) throws Exception {
    char[] array = new char[len];
    for (int count=0; count<len;) {
      int n = Math.min(buf.length/2, len-count);
      readToBuf(0, 2*n);
      for (int i=0; i<2*n; i+=2) array[count++] = SerializationUtils.readChar(buf, i);
    }
    return array;
  }

  /**
   * Read an array of short values.
   * @param len the length of the array to read.
   * @return a short[] of the specified length.
   * @throws Exception if any error occurs.
   */
  short[] readShortArray(final int len) throws Exception {
    short[] array = new short[len];
    for (int count=0; count<len;) {
      int n = Math.min(buf.length/2, len-count);
      readToBuf(0, 2*n);
      for (int i=0; i<2*n; i+=2) array[count++] = SerializationUtils.readShort(buf, i);
    }
    return array;
  }

  /**
   * Read an array of int values.
   * @param len the length of the array to read.
   * @return a int[] of the specified length.
   * @throws Exception if any error occurs.
   */
  int[] readIntArray(final int len) throws Exception {
    int[] array = new int[len];
    for (int count=0; count<len;) {
      int n = Math.min(buf.length/4, len-count);
      readToBuf(0, 4*n);
      for (int i=0; i<4*n; i+=4) array[count++] = SerializationUtils.readInt(buf, i);
    }
    return array;
  }

  /**
   * Read an array of long values.
   * @param len the length of the array to read.
   * @return a long[] of the specified length.
   * @throws Exception if any error occurs.
   */
  long[] readLongArray(final int len) throws Exception {
    long[] array = new long[len];
    for (int count=0; count<len;) {
      int n = Math.min(buf.length/8, len-count);
      readToBuf(0, 8*n);
      for (int i=0; i<8*n; i+=8) array[count++] = SerializationUtils.readLong(buf, i);
    }
    return array;
  }

  /**
   * Read an array of float values.
   * @param len the length of the array to read.
   * @return a float[] of the specified length.
   * @throws Exception if any error occurs.
   */
  float[] readFloatArray(final int len) throws Exception {
    float[] array = new float[len];
    for (int count=0; count<len;) {
      int n = Math.min(buf.length/4, len-count);
      readToBuf(0, 4*n);
      for (int i=0; i<4*n; i+=4) array[count++] = Float.intBitsToFloat(SerializationUtils.readInt(buf, i));
    }
    return array;
  }

  /**
   * Read an array of double values.
   * @param len the length of the array to read.
   * @return a double[] of the specified length.
   * @throws Exception if any error occurs.
   */
  double[] readDoubleArray(final int len) throws Exception {
    double[] array = new double[len];
    for (int count=0; count<len;) {
      int n = Math.min(buf.length/8, len-count);
      readToBuf(0, 8*n);
      for (int i=0; i<8*n; i+=8) array[count++] = Double.longBitsToDouble(SerializationUtils.readLong(buf, i));
    }
    return array;
  }

  /**
   * Read a string from the underlying stream.
   * @return the string obtained from the stream.
   * @throws Exception if any error occurs.
   */
  String readString() throws Exception {
    StringLengthDesc result = SerializationUtils.readStringLength(in, buf);
    if (result.length == 0) return "";
    else if (result.ascii) return readAsciiString(result.length);
    else if (result.length <= 65535/3) return readUTFString(result.length);
    return readCharString(result.length);
  }

  /**
   * Read an ASCII string.
   * @param len the length of the string to read.
   * @return the string read form the stream.
   * @throws Exception if any error occurs.
   */
  private String readAsciiString(final int len) throws Exception {
    if (traceEnabled) log.trace("reading ASCII string for len={}", len);
    char[] chars = new char[len];
    for (int count=0; count<len;) {
      int n = Math.min(buf.length, len - count);
      readToBuf(0, n);
      //for (int i=0; i<n; i++) chars[count++] = (char) (buf[i] & 0x7F);
      for (int i=0; i<n; i++) chars[count++] = (char) buf[i];
    }
    return new String(chars);
  }

  /**
   * Read an UTF string.
   * @param len the length of the string to read.
   * @return the string read form the stream.
   * @throws Exception if any error occurs.
   */
  private String readUTFString(final int len) throws Exception {
    if (traceEnabled) log.trace("calling readUTF() for len={}", len);
    return in.readUTF(); // for writeUTF() : max bytes = 64k-1, max bytes per char = 3
  }

  /**
   * Read a string as an array of chars.
   * @param len the length of the string to read.
   * @return the string read form the stream.
   * @throws Exception if any error occurs.
   */
  private String readCharString(final int len) throws Exception {
    if (traceEnabled) log.trace("reading normal string for len={}", len);
    return new String(readCharArray(len));
  }

  /**
   * Read the specified number of bytes into the temp buffer.
   * @param offset the offset at which to start in the buffer.
   * @param len the number of bytes to read.
   * @throws IOException if any error occurs.
   */
  void readToBuf(final int offset, final int len) throws IOException {
    for (int pos=offset, count=0; count<len; ) {
      int n = in.read(buf, pos, len - count);
      if (n > 0) {
        pos += n;
        count += n;
      }
      else if (n < 0) throw new EOFException("could only read " + count + " bytes out of " + len);
    }
  }

  /**
   * Read the handle of the next serialized entity from the underlying stream.
   * @param header the header byte.
   * @return the handle as an int value.
   * @throws Exception if any error occurs.
   */
  private int readHandle(final byte header) throws Exception {
    int n = (header >>> 4) & 0x0F;
    readToBuf(0, n);
    int handle = 0;
    for (int i=8*(n-1), pos=0; i>=0; i-=8) handle += (buf[pos++] & 0xFF) << i;
    return handle;
  }

  /**
   * Read an int from the stream.
   * @return the read int value.
   * @throws Exception if any error occurs.
   */
  int readInt() throws Exception {
    int value = SerializationUtils.readVarInt(in, buf);
    return value;
  }

  /**
   * Read a long from the stream.
   * @return the read long value.
   * @throws Exception if any error occurs.
   */
  long readLong() throws Exception {
    long value = SerializationUtils.readVarLong(in, buf);
    return value;
  }

  /**
   * Read a float from the stream.
   * @return the read float value.
   * @throws Exception if any error occurs.
   */
  float readFloat() throws Exception {
    return Float.intBitsToFloat(readInt());
  }

  /**
   * Read a double from the stream.
   * @return the read double value.
   * @throws Exception if any error occurs.
   */
  double readDouble() throws Exception {
    return Double.longBitsToDouble(readLong());
  }
}
