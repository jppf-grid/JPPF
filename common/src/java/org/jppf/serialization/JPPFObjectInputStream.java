/*
 * JPPF.
 * Copyright (C) 2005-2017 JPPF Team.
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
import java.util.Map;

import org.jppf.utils.StringUtils;

/**
 * Implementation of {@link ObjectInputStream} that reads objects without regards to whether
 * they implement {@link Serializable} or not. This allows using non-serializable classes in
 * JPPF tasks, especially when their source code is not available.
 * <p>The rest of the {@link ObjectInputStream} specification is respected:
 * <ul>
 * <li>transient fields are not deserialized</li>
 * <li><code>private void readObject(ObjectInputStream)</code> is used whenever implemented</li>
 * <li>the {@link java.io.Externalizable Externalizable} interface is respected</li>
 * </ul>
 * @author Laurent Cohen
 */
public class JPPFObjectInputStream extends ObjectInputStream {
  /**
   * The stream to read data from.
   */
  private DataInputStream in;
  /**
   * The deserializer.
   */
  private Deserializer deserializer;
  /**
   * Determines whether the stream is already reading an object graph.
   */
  private boolean readingObject = false;
  /**
   * Temporary buffer.
   */
  private final byte[] buf = new byte[16];

  /**
   * Initialize this object input stream with the specified stream.
   * @param in the stream to read data from.
   * @throws IOException if an I/O error occurs.
   */
  public JPPFObjectInputStream(final InputStream in) throws IOException {
    super();
    this.in = (in instanceof DataInputStream) ? (DataInputStream) in : new DataInputStream(in);
    deserializer = new Deserializer(this);
    readToBuf(4);
    if ( (buf[0] != Serializer.HEADER[0]) || (buf[1] != Serializer.HEADER[1]) || (buf[2] != Serializer.HEADER[2]) || (buf[3] != Serializer.HEADER[3]))
      throw new IOException("bad header: " + StringUtils.toHexString(buf, 0, 4, " "));
  }

  /**
   * Initialize this object input stream with the specified stream.
   * @param in the stream to read data from.
   * @param deserializer the deserializer to use.
   * @throws IOException if an I/O error occurs.
   */
  public JPPFObjectInputStream(final InputStream in, final Deserializer deserializer) throws IOException {
    super();
    this.in = (in instanceof DataInputStream) ? (DataInputStream) in : new DataInputStream(in);
    this.deserializer = deserializer;
    deserializer.in = this;
    readToBuf(4);
    if ( (buf[0] != Serializer.HEADER[0]) || (buf[1] != Serializer.HEADER[1]) || (buf[2] != Serializer.HEADER[2]) || (buf[3] != Serializer.HEADER[3]))
      throw new IOException("bad header: " + StringUtils.toHexString(buf, 0, 4, " "));
  }

  @Override
  protected Object readObjectOverride() throws IOException, ClassNotFoundException {
    Object o = null;
    final boolean alreadyReading = readingObject;
    try {
      if (!alreadyReading) readingObject = true;
      o = deserializer.readObject();
    } catch (final Exception e) {
      if (e instanceof ClassNotFoundException) throw (ClassNotFoundException) e;
      else if (e instanceof IOException) throw (IOException) e;
      else throw new IOException(e.getMessage(), e);
    } finally {
      if (!alreadyReading) readingObject = false;
    }
    return o;
  }

  @Override
  public int read() throws IOException {
    return in.read();
  }

  @Override
  public int read(final byte[] buf, final int off, final int len) throws IOException {
    return in.read(buf, off, len);
  }

  @Override
  public boolean readBoolean() throws IOException {
    return in.readBoolean();
  }

  @Override
  public byte readByte() throws IOException {
    return in.readByte();
  }

  @Override
  public char readChar() throws IOException {
    readToBuf(2);
    return SerializationUtils.readChar(buf, 0);
  }

  @Override
  public short readShort() throws IOException {
    readToBuf(2);
    return SerializationUtils.readShort(buf, 0);
  }

  @Override
  public int readInt() throws IOException {
    readToBuf(4);
    return SerializationUtils.readInt(buf, 0);
  }

  @Override
  public long readLong() throws IOException {
    readToBuf(8);
    return SerializationUtils.readLong(buf, 0);
  }

  @Override
  public float readFloat() throws IOException {
    return Float.intBitsToFloat(readInt());
  }

  @Override
  public double readDouble() throws IOException {
    return Double.longBitsToDouble(readLong());
  }

  @Override
  public int skipBytes(final int len) throws IOException {
    return in.skipBytes(len);
  }

  @Override
  public String readUTF() throws IOException {
    return in.readUTF();
  }

  @Override
  public void close() throws IOException {
    in.close();
  }

  @Override
  public int readUnsignedByte() throws IOException {
    return in.readUnsignedByte();
  }

  @Override
  public int readUnsignedShort() throws IOException {
    return in.readUnsignedShort();
  }

  @Override
  public void readFully(final byte[] buf) throws IOException {
    in.readFully(buf);
  }

  @Override
  public void readFully(final byte[] buf, final int off, final int len) throws IOException {
    in.readFully(buf, off, len);
  }

  @Override
  @SuppressWarnings("deprecation")
  public String readLine() throws IOException {
    return in.readLine();
  }

  @Override
  public void defaultReadObject() throws IOException, ClassNotFoundException {
    try {
      deserializer.readDeclaredFields(deserializer.currentClassDescriptor, deserializer.currentObject);
    } catch (final Exception e) {
      if (e instanceof ClassNotFoundException) throw (ClassNotFoundException) e;
      else if (e instanceof IOException) throw (IOException) e;
      else throw new IOException(e.getMessage(), e);
    }
  }

  @Override
  public GetField readFields() throws IOException, ClassNotFoundException {
    try {
      final JPPFGetField getFields = new JPPFGetField();
      readFields0(getFields.primitiveFields);
      readFields0(getFields.objectFields);
      return getFields;
    } catch (final Exception e) {
      if (e instanceof ClassNotFoundException) throw (ClassNotFoundException) e;
      else if (e instanceof IOException) throw (IOException) e;
      else throw new IOException(e.getMessage(), e);
    }
  }

  /**
   * Read the primitive or object fields of the current GetField.
   * @param map a map that receives the names and values of the fields.
   * @throws Exception if any error occurs.
   */
  private void readFields0(final Map<String, Object> map) throws Exception {
    final int n = readInt();
    for (int i=0; i<n; i++) {
      final Object o = deserializer.readObject();
      final String name = (String) o;
      final Object value = deserializer.readObject();
      map.put(name, value);
    }
  }

  /**
   * Read the specified number of bytes into the temp buyffer.
   * @param len the number of bytes to read.
   * @throws IOException if any error occurs.
   */
  private void readToBuf(final int len) throws IOException {
    int pos = 0;
    while (pos < len) {
      final int n = in.read(buf, pos, len - pos);
      if (n > 0) pos += n;
      else if (n < 0) throw new EOFException("could only read " + pos + " bytes out of " + len);
    }
  }
}
