/*
 * JPPF.
 * Copyright (C) 2005-2013 JPPF Team.
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
public class JPPFObjectInputStream extends ObjectInputStream
{
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
   * Temporary buffer to write primitive types.
   */
  private final byte[] buf = new byte[8];

  /**
   * Initialize this object input stream with the specified stream.
   * @param in the stream to read data from.
   * @throws IOException if an I/O error occurs.
   */
  public JPPFObjectInputStream(final InputStream in) throws IOException
  {
    super();
    this.in = (in instanceof DataInputStream) ? (DataInputStream) in : new DataInputStream(in);
    deserializer = new Deserializer(this);
  }

  @Override
  protected Object readObjectOverride() throws IOException, ClassNotFoundException
  {
    Object o = null;
    boolean alreadyReading = readingObject;
    try
    {
      if (!alreadyReading) readingObject = true;
       o = deserializer.readObject();
    }
    catch(Exception e)
    {
      if (e instanceof ClassNotFoundException) throw (ClassNotFoundException) e;
      else if (e instanceof IOException) throw (IOException) e;
      else throw new IOException(e.getMessage(), e);
    }
    finally
    {
      if (!alreadyReading) readingObject = false;
    }
    return o;
  }

  @Override
  public int read() throws IOException
  {
    return in.read();
  }

  @Override
  public int read(final byte[] buf, final int off, final int len) throws IOException
  {
    return in.read(buf, off, len);
  }

  @Override
  public boolean readBoolean() throws IOException
  {
    return in.readBoolean();
  }

  @Override
  public byte readByte() throws IOException
  {
    return in.readByte();
  }

  @Override
  public char readChar() throws IOException
  {
    return in.readChar();
  }

  @Override
  public short readShort() throws IOException
  {
    return in.readShort();
  }

  @Override
  public int readInt() throws IOException
  {
    return in.readInt();
  }

  @Override
  public long readLong() throws IOException
  {
    return in.readLong();
  }

  @Override
  public float readFloat() throws IOException
  {
    return in.readFloat();
  }

  @Override
  public double readDouble() throws IOException
  {
    return in.readDouble();
  }

  @Override
  public int skipBytes(final int len) throws IOException
  {
    return in.skipBytes(len);
  }

  @Override
  public String readUTF() throws IOException
  {
    return in.readUTF();
  }

  @Override
  public void close() throws IOException
  {
    in.close();
  }

  @Override
  public int readUnsignedByte() throws IOException
  {
    return in.readUnsignedByte();
  }

  @Override
  public int readUnsignedShort() throws IOException
  {
    return in.readUnsignedShort();
  }

  @Override
  public void readFully(final byte[] buf) throws IOException
  {
    in.readFully(buf);
  }

  @Override
  public void readFully(final byte[] buf, final int off, final int len) throws IOException
  {
    in.readFully(buf, off, len);
  }

  @Override
  @SuppressWarnings("deprecation")
  public String readLine() throws IOException
  {
    return in.readLine();
  }

  @Override
  public void defaultReadObject() throws IOException, ClassNotFoundException
  {
    try
    {
      deserializer.readDeclaredFields(deserializer.currentClassDescriptor, deserializer.currentObject);
    }
    catch(Exception e)
    {
      if (e instanceof ClassNotFoundException) throw (ClassNotFoundException) e;
      else if (e instanceof IOException) throw (IOException) e;
      else throw new IOException(e.getMessage(), e);
    }
  }

  @Override
  public GetField readFields() throws IOException, ClassNotFoundException
  {
    try
    {
      JPPFGetField getFields = new JPPFGetField();
      readFields0(getFields.primitiveFields);
      readFields0(getFields.objectFields);
      return getFields;
    }
    catch (Exception e)
    {
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
  private void readFields0(final Map<String, Object> map) throws Exception
  {
    String[] names = (String[]) deserializer.readObject();
    Object[] values = (Object[]) deserializer.readObject();
    for (int i=0; i<names.length; i++) map.put(names[i], values[i]);
  }
}
