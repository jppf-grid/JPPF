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
import java.util.Map;

import org.jppf.utils.SerializationUtils;

/**
 * Implementation of {@link ObjectOutputStream} that writes objects without regards to whether
 * they implement {@link Serializable} or not. This allows using non-serializable classes in
 * JPPF tasks, especially when their source code is not available.
 * <p>The rest of the {@link ObjectOutputStream} specification is respected:
 * <ul>
 * <li>transient fields are not serialized</li>
 * <li><code>private void writeObject(ObjectOutputStream)</code> is used whenever implemented</li>
 * <li>the {@link Externalized} interface is respected</li>
 * </ul>
 * @author Laurent Cohen
 */
public class JPPFObjectOutputStream extends ObjectOutputStream
{
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
  private final byte[] buf = new byte[8];
  /**
   * The latest generated PutField instance.
   */
  private PutField currentPutField = null;

  /**
   * Initialize this object stream.
   * @param out the stream to write objects to.
   * @throws IOException if any error occurs.
   */
  public JPPFObjectOutputStream(final OutputStream out) throws IOException
  {
    super();
    this.out = new DataOutputStream(out);
    serializer = new Serializer(this);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  protected final void writeObjectOverride(final Object obj) throws IOException
  {
    boolean alreadyWriting = writingObject;
    try
    {
      if (!alreadyWriting)
      {
        writingObject = true;
        serializer.writeObject(obj);
      }
      else
      {
        serializer.writeObject(obj);
      }
    }
    catch (Exception e)
    {
      throw (e instanceof IOException) ? (IOException) e : new IOException(e.getMessage(), e);
    }
    finally
    {
      if (!alreadyWriting)
      {
        writingObject = false;
        flush();
      }
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void write(final int val) throws IOException
  {
    out.write(val);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void write(final byte[] buf) throws IOException
  {
    out.write(buf);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void write(final byte[] buf, final int off, final int len) throws IOException
  {
    out.write(buf, off, len);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void writeBoolean(final boolean val) throws IOException
  {
    out.writeBoolean(val);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void writeByte(final int val) throws IOException
  {
    out.writeByte(val);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void writeShort(final int val) throws IOException
  {
    out.writeShort(val);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void writeChar(final int val) throws IOException
  {
    out.writeChar(val);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void writeInt(final int val) throws IOException
  {
    SerializationUtils.writeInt(val, buf, 0);
    out.write(buf, 0, 4);
    //out.writeInt(val);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void writeLong(final long val) throws IOException
  {
    out.writeLong(val);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void writeFloat(final float val) throws IOException
  {
    out.writeFloat(val);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void writeDouble(final double val) throws IOException
  {
    //out.writeDouble(val);
    out.writeLong(Double.doubleToLongBits(val));
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void writeBytes(final String str) throws IOException
  {
    out.writeBytes(str);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void writeChars(final String str) throws IOException
  {
    out.writeChars(str);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void writeUTF(final String str) throws IOException
  {
    out.writeUTF(str);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void defaultWriteObject() throws IOException
  {
    try
    {
      serializer.writeFields(serializer.currentObject, serializer.currentClassDescriptor);
    }
    catch(Exception e)
    {
      if (e instanceof IOException) throw (IOException) e;
      else throw new IOException(e.getMessage(), e);
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void flush() throws IOException
  {
    out.flush();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void close() throws IOException
  {
    out.close();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public PutField putFields() throws IOException
  {
    //return super.putFields();
    if (currentPutField == null) currentPutField = new JPPFPutField(this);
    return currentPutField;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void writeFields() throws IOException
  {
    //super.writeFields();
    try
    {
      JPPFPutField f = (JPPFPutField) currentPutField;
      writeFields0(f.primitiveFields);
      writeFields0(f.objectFields);
    }
    catch (Exception e)
    {
      if (e instanceof IOException) throw (IOException) e;
      else throw new IOException(e);
    }
  }

  /**
   * Write the primitive or object fields of the current PutField.
   * @param map the map containing fields names and values.
   * @throws Exception if any error occurs.
   */
  private void writeFields0(final Map<String, Object> map) throws Exception
  {
    int n = map.size();
    String[] names = new String[n];
    Object[] values = new Object[n];
    int count = 0;
    for (Map.Entry<String, Object> entry: map.entrySet())
    {
      names[count] = entry.getKey();
      values[count] = entry.getValue();
      count++;
    }
    serializer.writeObject(names);
    serializer.writeObject(values);
  }
}
