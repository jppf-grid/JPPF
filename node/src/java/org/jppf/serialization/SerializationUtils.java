/*
 * JPPF.
 * Copyright (C) 2005-2014 JPPF Team.
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
import java.nio.ByteBuffer;
import java.nio.channels.*;

/**
 * Utility methods for serializing and deserializing data
 * @author Laurent Cohen
 * @exclude
 */
public final class SerializationUtils
{
  /**
   * Instantiation of this class is not permitted.
   */
  private SerializationUtils()
  {
  }

  /**
   * Serialize an int value into an array of bytes.
   * @param value the int value to serialize.
   * @return an array of bytes filled with the value's representation.
   */
  public static byte[] writeInt(final int value)
  {
    return writeInt(value, new byte[4], 0);
  }

  /**
   * Serialize an int value into an array of bytes.
   * @param value the int value to serialize.
   * @param data the array of bytes into which to serialize the value.
   * @param offset the position in the array of byte at which the serialization should start.
   * @return an array of bytes filled with the value's representation, starting at the specified offset.
   */
  public static byte[] writeBoolean(final boolean value, final byte[] data, final int offset)
  {
    data[offset] = value ? (byte) 1 : 0;
    return data;
  }

  /**
   * Serialize an int value into an array of bytes.
   * @param value the int value to serialize.
   * @param data the array of bytes into which to serialize the value.
   * @param offset the position in the array of byte at which the serialization should start.
   * @return an array of bytes filled with the value's representation, starting at the specified offset.
   */
  public static byte[] writeChar(final char value, final byte[] data, final int offset)
  {
    int pos = offset;
    data[pos++] = (byte) ((value >>>  8) & 0xFF);
    data[pos++] = (byte) ((value       ) & 0xFF);
    return data;
  }

  /**
   * Serialize an int value into an array of bytes.
   * @param value the int value to serialize.
   * @param data the array of bytes into which to serialize the value.
   * @param offset the position in the array of byte at which the serialization should start.
   * @return an array of bytes filled with the value's representation, starting at the specified offset.
   */
  public static byte[] writeShort(final short value, final byte[] data, final int offset)
  {
    int pos = offset;
    data[pos++] = (byte) ((value >>>  8) & 0xFF);
    data[pos++] = (byte) ((value       ) & 0xFF);
    return data;
  }

  /**
   * Serialize an int value into an array of bytes.
   * @param value the int value to serialize.
   * @param data the array of bytes into which to serialize the value.
   * @param offset the position in the array of byte at which the serialization should start.
   * @return an array of bytes filled with the value's representation, starting at the specified offset.
   */
  public static byte[] writeInt(final int value, final byte[] data, final int offset)
  {
    int pos = offset;
    data[pos++] = (byte) ((value >>> 24) & 0xFF);
    data[pos++] = (byte) ((value >>> 16) & 0xFF);
    data[pos++] = (byte) ((value >>>  8) & 0xFF);
    data[pos++] = (byte) ((value       ) & 0xFF);
    return data;
  }

  /**
   * Serialize a long value into an array of bytes.
   * @param value the int value to serialize.
   * @param data the array of bytes into which to serialize the value.
   * @param offset the position in the array of byte at which the serialization should start.
   * @return an array of bytes filled with the value's representation, starting at the specified offset.
   */
  public static byte[] writeLong(final long value, final byte[] data, final int offset)
  {
    int pos = offset;
    data[pos++] = (byte) ((value >>> 56) & 0xFF);
    data[pos++] = (byte) ((value >>> 48) & 0xFF);
    data[pos++] = (byte) ((value >>> 40) & 0xFF);
    data[pos++] = (byte) ((value >>> 32) & 0xFF);
    data[pos++] = (byte) ((value >>> 24) & 0xFF);
    data[pos++] = (byte) ((value >>> 16) & 0xFF);
    data[pos++] = (byte) ((value >>>  8) & 0xFF);
    data[pos++] = (byte) ((value       ) & 0xFF);
    return data;
  }

  /**
   * Serialize an int value to a stream.
   * @param value the int value to serialize.
   * @param os the stream to write to.
   * @throws IOException if an error occurs while writing the data.
   */
  public static void writeInt(final int value, final OutputStream os) throws IOException
  {
    os.write((byte) ((value >>> 24) & 0xFF));
    os.write((byte) ((value >>> 16) & 0xFF));
    os.write((byte) ((value >>>  8) & 0xFF));
    os.write((byte) ((value       ) & 0xFF));
  }

  /**
   * Write an integer value to a channel.
   * @param channel the channel to write to.
   * @param value the value to write.
   * @throws IOException if an error occurs while writing the data.
   */
  public static void writeInt(final WritableByteChannel channel, final int value) throws IOException
  {
    ByteBuffer buf = ByteBuffer.allocate(4);
    buf.putInt(value);
    buf.flip();
    int count = 0;
    while (count < 4)
    {
      int n = 0;
      while (n == 0) n = channel.write(buf);
      if (n < 0) throw new ClosedChannelException();
      count += n;
    }
  }

  /**
   * Read an integer value from a channel.
   * @param channel the channel to read from.
   * @return the value read from the channel.
   * @throws IOException if an error occurs while reading the data.
   */
  public static int readInt(final ReadableByteChannel channel) throws IOException
  {
    ByteBuffer buf = ByteBuffer.allocate(4);
    int count = 0;
    while (count < 4)
    {
      int n = 0;
      while (n == 0) n = channel.read(buf);
      if (n < 0) throw new ClosedChannelException();
      count += n;
    }
    buf.flip();
    return buf.getInt();
  }

  /**
   * Deserialize an int value from an array of bytes.
   * @param data the array of bytes into which to serialize the value.
   * @param offset the position in the array of byte at which the serialization should start.
   * @return the int value read from the array of bytes
   */
  public static boolean readBoolean(final byte[] data, final int offset)
  {
    return data[offset] != 0;
  }

  /**
   * Deserialize an int value from an array of bytes.
   * @param data the array of bytes into which to serialize the value.
   * @param offset the position in the array of byte at which the serialization should start.
   * @return the int value read from the array of bytes
   */
  public static char readChar(final byte[] data, final int offset)
  {
    int pos = offset;
    int result = (data[pos++] & 0xFF) << 8;
    result    += (data[pos++] & 0xFF);
    return (char) result;
  }

  /**
   * Deserialize an int value from an array of bytes.
   * @param data the array of bytes into which to serialize the value.
   * @param offset the position in the array of byte at which the serialization should start.
   * @return the int value read from the array of bytes
   */
  public static short readShort(final byte[] data, final int offset)
  {
    int pos = offset;
    int result = (data[pos++] & 0xFF) << 8;
    result    += (data[pos++] & 0xFF);
    return (short) result;
  }

  /**
   * Deserialize an int value from an array of bytes.
   * @param data the array of bytes into which to serialize the value.
   * @param offset the position in the array of byte at which the serialization should start.
   * @return the int value read from the array of bytes
   */
  public static int readInt(final byte[] data, final int offset)
  {
    int pos = offset;
    int result = (data[pos++] & 0xFF) << 24;
    result    += (data[pos++] & 0xFF) << 16;
    result    += (data[pos++] & 0xFF) <<  8;
    result    += (data[pos++] & 0xFF);
    return result;
  }

  /**
   * Deserialize an int value from an array of bytes.
   * @param data the array of bytes into which to serialize the value.
   * @param offset the position in the array of byte at which the serialization should start.
   * @return the int value read from the array of bytes
   */
  public static long readLong(final byte[] data, final int offset)
  {
    int pos = offset;
    long result = (long) (data[pos++] & 0xFF) << 56;
    result     += (long) (data[pos++] & 0xFF) << 48;
    result     += (long) (data[pos++] & 0xFF) << 40;
    result     += (long) (data[pos++] & 0xFF) << 32;
    result     += (long) (data[pos++] & 0xFF) << 24;
    result     += (long) (data[pos++] & 0xFF) << 16;
    result     += (long) (data[pos++] & 0xFF) <<  8;
    result     += (data[pos++] & 0xFF);
    return result;
  }

  /**
   * Deserialize an int value from a stream.
   * @param is the stream to read from.
   * @return the int value read from the stream.
   * @throws IOException if an error occurs while reading the data.
   */
  public static int readInt(final InputStream is) throws IOException
  {
    int result = (is.read() & 0xFF) << 24;
    result    += (is.read() & 0xFF) << 16;
    result    += (is.read() & 0xFF) <<  8;
    result    += (is.read() & 0xFF);
    return result;
  }

  /**
   * Deserialize an int value from a stream.
   * @param is the stream to read from.
   * @return the int value read from the stream.
   * @throws IOException if an error occurs while reading the data.
   */
  public static long readLong(final InputStream is) throws IOException
  {
    long result = (long) (is.read() & 0xFF) << 56;
    result     += (long) (is.read() & 0xFF) << 48;
    result     += (long) (is.read() & 0xFF) << 40;
    result     += (long) (is.read() & 0xFF) << 32;
    result     += (long) (is.read() & 0xFF) << 24;
    result     += (long) (is.read() & 0xFF) << 16;
    result     += (long) (is.read() & 0xFF) <<  8;
    result     += (is.read() & 0xFF);
    return result;
  }
}
