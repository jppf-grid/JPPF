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

/**
 * Implementation of {@link ObjectOutputStream} that writes objects without regards to whether
 * they implement {@link Serializable} or not. This allows using non-serializable classes in
 * JPPF tasks, especially when their source code is not available.
 * <p>The rest of the {@link ObjectOutputStream} specification is respected:
 * <ul>
 * <li>transient fields are not serialized</li>
 * <li><code>private void writeObject(ObjectOutputStream)</code> is used whenever implemented</li>
 * <li>the {@link Externalizable} interface is respected</li>
 * </ul>
 * @author Laurent Cohen
 */
public class JPPFObjectOutputStream extends ObjectOutputStream {
  /**
   * The stream serialized data is written to.
   */
  private DataOutputStream out;
  /**
   * Determines whether the stream is already writing an object graph.
   */
  private boolean writingObject = false;
  /**
   * The object graph serializer.
   */
  private Serializer serializer = null;
  /**
   * Temporary buffer to write primitive types.
   */
  private final byte[] buf = new byte[16];
  /**
   * The latest generated PutField instance.
   */
  private PutField currentPutField = null;

  /**
   * Initialize this object stream.
   * @param out the stream to write objects to.
   * @throws IOException if any error occurs.
   */
  public JPPFObjectOutputStream(final OutputStream out) throws IOException {
    super();
    this.out = (out instanceof DataOutputStream) ? (DataOutputStream) out : new DataOutputStream(out);
    serializer = new Serializer(this);
    write(Serializer.HEADER);
  }

  /**
   * Initialize this object stream.
   * @param out the stream to write objects to.
   * @param serializer the serializer to use.
   * @throws IOException if any error occurs.
   */
  public JPPFObjectOutputStream(final OutputStream out, final Serializer serializer) throws IOException {
    super();
    this.out = (out instanceof DataOutputStream) ? (DataOutputStream) out : new DataOutputStream(out);
    this.serializer = serializer;
    serializer.out = this;
    write(Serializer.HEADER);
  }

  @Override
  protected final void writeObjectOverride(final Object obj) throws IOException {
    boolean alreadyWriting = writingObject;
    try {
      if (!alreadyWriting) writingObject = true;
      serializer.writeObject(obj);
    } catch (Exception e) {
      throw (e instanceof IOException) ? (IOException) e : new IOException(e.getMessage(), e);
    } finally {
      if (!alreadyWriting) {
        writingObject = false;
        flush();
      }
    }
  }

  @Override
  public void write(final int val) throws IOException {
    out.write(val);
  }

  @Override
  public void write(final byte[] buf) throws IOException {
    out.write(buf);
  }

  @Override
  public void write(final byte[] buf, final int off, final int len) throws IOException {
    out.write(buf, off, len);
  }

  @Override
  public void writeBoolean(final boolean val) throws IOException {
    out.writeBoolean(val);
  }

  @Override
  public void writeByte(final int val) throws IOException {
    out.writeByte(val);
  }

  @Override
  public void writeShort(final int val) throws IOException {
    SerializationUtils.writeShort((short) val, buf, 0);
    out.write(buf, 0, 2);
  }

  @Override
  public void writeChar(final int val) throws IOException {
    SerializationUtils.writeChar((char) val, buf, 0);
    out.write(buf, 0, 2);
  }

  @Override
  public void writeInt(final int val) throws IOException {
    SerializationUtils.writeInt(val, buf, 0);
    out.write(buf, 0, 4);
    //SerializationUtils.writeVarInt(out, val, buf);
  }

  @Override
  public void writeLong(final long val) throws IOException {
    SerializationUtils.writeLong(val, buf, 0);
    out.write(buf, 0, 8);
  }

  @Override
  public void writeFloat(final float val) throws IOException {
    writeInt(Float.floatToIntBits(val));
  }

  @Override
  public void writeDouble(final double val) throws IOException {
    writeLong(Double.doubleToLongBits(val));
  }

  @Override
  public void writeBytes(final String str) throws IOException {
    out.writeBytes(str);
  }

  @Override
  public void writeChars(final String str) throws IOException {
    out.writeChars(str);
  }

  @Override
  public void writeUTF(final String str) throws IOException {
    out.writeUTF(str);
  }

  @Override
  public void defaultWriteObject() throws IOException {
    try {
      serializer.writeDeclaredFields(serializer.currentObject, serializer.currentClassDescriptor);
    } catch (Exception e) {
      if (e instanceof IOException) throw (IOException) e;
      else throw new IOException(e.getMessage(), e);
    }
  }

  @Override
  public void flush() throws IOException {
    out.flush();
  }

  @Override
  public void close() throws IOException {
    out.close();
  }

  @Override
  public PutField putFields() throws IOException {
    if (currentPutField == null) currentPutField = new JPPFPutField(this);
    return currentPutField;
  }

  @Override
  public void writeFields() throws IOException {
    try {
      JPPFPutField f = (JPPFPutField) currentPutField;
      writeFields0(f.primitiveFields);
      writeFields0(f.objectFields);
    } catch (Exception e) {
      if (e instanceof IOException) throw (IOException) e;
      else throw new IOException(e);
    }
  }

  /**
   * Write the primitive or object fields of the current PutField.
   * @param map the map containing fields names and values.
   * @throws Exception if any error occurs.
   */
  private void writeFields0(final Map<String, Object> map) throws Exception {
    writeInt(map.size());
    for (Map.Entry<String, Object> entry : map.entrySet()) {
      serializer.writeObject(entry.getKey());
      serializer.writeObject(entry.getValue());
    }
  }
}
