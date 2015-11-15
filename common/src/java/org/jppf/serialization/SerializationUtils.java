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
import java.nio.ByteBuffer;
import java.nio.channels.*;

import org.jppf.io.IO;
import org.jppf.utils.Pair;

/**
 * Utility methods for serializing and deserializing data
 * @author Laurent Cohen
 * @exclude
 */
public final class SerializationUtils {
  /**
   * Bit mask indicating a negative value.
   */
  private static final byte SIGN_BIT = 0x10;
  /**
   * Bit mask for a value of zero.
   */
  private static final byte ZERO_BIT = 0x20;
  /**
   * Bit mask for a pure ASCII string.
   */
  private static final byte ASCII_BIT = 0x40;
  /**
   * .
   */
  static final int[] INT_MAX_VALUES = { 128, 128 << 8, 128 << 16 };
  /**
   * .
   */
  private static final long[] LONG_MAX_VALUES = { 128L, 128L << 8, 128L << 16, 128L << 24, 128L << 32, 128L << 40, 128L << 48 };
  /**
   * 
   */
  public static int TEMP_BUFFER_SIZE = 2048;

  /**
   * Instantiation of this class is not permitted.
   */
  private SerializationUtils() {
  }

  /**
   * Serialize an int value into an array of bytes.
   * @param value the int value to serialize.
   * @return an array of bytes filled with the value's representation.
   */
  public static byte[] writeInt(final int value) {
    byte[] bytes = IO.LENGTH_BUFFER_POOL.get();
    writeInt(value, bytes, 0);
    return bytes;
  }

  /**
   * Serialize an int value into an array of bytes.
   * @param value the int value to serialize.
   * @param data the array of bytes into which to serialize the value.
   * @param offset the position in the array of byte at which the serialization should start.
   */
  public static void writeBoolean(final boolean value, final byte[] data, final int offset) {
    data[offset] = value ? (byte) 1 : 0;
  }

  /**
   * Serialize an int value into an array of bytes.
   * @param value the int value to serialize.
   * @param data the array of bytes into which to serialize the value.
   * @param offset the position in the array of byte at which the serialization should start.
   */
  public static void writeChar(final char value, final byte[] data, final int offset) {
    data[offset] = (byte) ((value >>> 8) & 0xFF);
    data[offset+1] = (byte) ((value) & 0xFF);
  }

  /**
   * Serialize an int value into an array of bytes.
   * @param value the int value to serialize.
   * @param data the array of bytes into which to serialize the value.
   * @param offset the position in the array of byte at which the serialization should start.
   */
  public static void writeShort(final short value, final byte[] data, final int offset) {
    data[offset] = (byte) ((value >>> 8) & 0xFF);
    data[offset+1] = (byte) ((value) & 0xFF);
  }

  /**
   * Serialize an int value into an array of bytes.
   * @param value the int value to serialize.
   * @param data the array of bytes into which to serialize the value.
   * @param offset the position in the array of byte at which the serialization should start.
   */
  public static void writeInt(final int value, final byte[] data, final int offset) {
    for (int i=24, pos=offset; i>=0; i-=8) data[pos++] = (byte) ((value >>> i) & 0xFF);
  }

  /**
   * Serialize a long value into an array of bytes.
   * @param value the int value to serialize.
   * @param data the array of bytes into which to serialize the value.
   * @param offset the position in the array of byte at which the serialization should start.
   */
  public static void writeLong(final long value, final byte[] data, final int offset) {
    for (int i=56, pos=offset; i>=0; i-=8) data[pos++] = (byte) ((value >>> i) & 0xFF);
  }

  /**
   * Serialize an int value to a stream.
   * @param value the int value to serialize.
   * @param os the stream to write to.
   * @throws IOException if an error occurs while writing the data.
   */
  public static void writeChar(final char value, final OutputStream os) throws IOException {
    for (int i=8; i>=0; i-=8) os.write((byte) ((value >>> i) & 0xFF));
  }

  /**
   * Serialize an int value to a stream.
   * @param value the int value to serialize.
   * @param os the stream to write to.
   * @throws IOException if an error occurs while writing the data.
   */
  public static void writeInt(final int value, final OutputStream os) throws IOException {
    for (int i=24; i>=0; i-=8) os.write((byte) ((value >>> i) & 0xFF));
  }

  /**
   * Write an integer value to a channel.
   * @param channel the channel to write to.
   * @param value the value to write.
   * @throws IOException if an error occurs while writing the data.
   */
  public static void writeInt(final WritableByteChannel channel, final int value) throws IOException {
    ByteBuffer buf = ByteBuffer.allocate(4);
    buf.putInt(value);
    buf.flip();
    int count = 0;
    while (count < 4) {
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
  public static int readInt(final ReadableByteChannel channel) throws IOException {
    ByteBuffer buf = ByteBuffer.allocate(4);
    int count = 0;
    while (count < 4) {
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
  public static boolean readBoolean(final byte[] data, final int offset) {
    return data[offset] != 0;
  }

  /**
   * Deserialize an int value from an array of bytes.
   * @param data the array of bytes into which to serialize the value.
   * @param offset the position in the array of byte at which the serialization should start.
   * @return the int value read from the array of bytes
   */
  public static char readChar(final byte[] data, final int offset) {
    int pos = offset;
    int result = (data[pos++] & 0xFF) << 8;
    result += (data[pos++] & 0xFF);
    return (char) result;
  }

  /**
   * Deserialize an int value from an array of bytes.
   * @param data the array of bytes into which to serialize the value.
   * @param offset the position in the array of byte at which the serialization should start.
   * @return the int value read from the array of bytes
   */
  public static short readShort(final byte[] data, final int offset) {
    int pos = offset;
    int result = (data[pos++] & 0xFF) << 8;
    result += (data[pos++] & 0xFF);
    return (short) result;
  }

  /**
   * Deserialize an int value from an array of bytes.
   * @param data the array of bytes into which to serialize the value.
   * @param offset the position in the array of byte at which the serialization should start.
   * @return the int value read from the array of bytes
   */
  public static int readInt(final byte[] data, final int offset) {
    int result = 0;
    for (int i=24, pos=offset; i>=0; i-=8) result += (long) (data[pos++] & 0xFF) << i;
    return result;
  }

  /**
   * Deserialize an int value from an array of bytes.
   * @param data the array of bytes into which to serialize the value.
   * @param offset the position in the array of byte at which the serialization should start.
   * @return the int value read from the array of bytes
   */
  public static long readLong(final byte[] data, final int offset) {
    long result = 0;
    for (int i=56, pos=offset; i>=0; i-=8) result += (long) (data[pos++] & 0xFF) << i;
    return result;
  }

  /**
   * Deserialize a char value from a stream.
   * @param is the stream to read from.
   * @return the char value read from the stream.
   * @throws IOException if an error occurs while reading the data.
   */
  public static char readChar(final InputStream is) throws IOException {
    int result = 0;
    for (int i=8; i>=0; i-=8) result += (is.read() & 0xFF) << i;
    return (char) result;
  }

  /**
   * Deserialize an int value from a stream.
   * @param is the stream to read from.
   * @return the int value read from the stream.
   * @throws IOException if an error occurs while reading the data.
   */
  public static int readInt(final InputStream is) throws IOException {
    int result = 0;
    for (int i=24; i>=0; i-=8) result += (is.read() & 0xFF) << i;
    return result;
  }

  /**
   * Deserialize an int value from a stream.
   * @param is the stream to read from.
   * @return the int value read from the stream.
   * @throws IOException if an error occurs while reading the data.
   */
  public static long readLong(final InputStream is) throws IOException {
    long result = 0L;
    for (int i=56; i>=0; i-=8) result += (long) (is.read() & 0xFF) << i;
    return result;
  }

  /**
   * Serialize an int value into an array of bytes.
   * @param os the stream to write to.
   * @param value the int value to serialize.
   * @param data the array of bytes into which to serialize the value.
   * @throws IOException if an error occurs while writing the data.
   */
  public static void writeVarInt(final OutputStream os, final int value, final byte[] data) throws IOException {
    if (value == 0) {
      os.write(ZERO_BIT);
      return;
    }
    int absValue = (value > 0) ? value : -value;
    byte n = 4;
    for (int i=0; i<INT_MAX_VALUES.length; i++) {
      if (absValue < INT_MAX_VALUES[i]) {
        n = (byte) (i + 1);
        break;
      }
    }
    byte b = n;
    if (value < 0) b |= SIGN_BIT;
    data[0] = b;
    for (int i=8*(n-1), pos=1; i>=0; i-=8) data[pos++] = (byte) ((absValue >>> i) & 0xFF);
    os.write(data, 0, n+1);
  }

  /**
   * Serialize a long value into an array of bytes.
   * @param os the stream to write to.
   * @param value the long value to serialize.
   * @param data the array of bytes into which to serialize the value.
   * @throws IOException if an error occurs while writing the data.
   */
  public static void writeVarLong(final OutputStream os, final long value, final byte[] data) throws IOException {
    if (value == 0) {
      os.write(ZERO_BIT);
      return;
    }
    long absValue = (value > 0L) ? value : -value;
    byte n = 8;
    for (int i=0; i<LONG_MAX_VALUES.length; i++) {
      if (absValue < LONG_MAX_VALUES[i]) {
        n = (byte) (i + 1);
        break;
      }
    }
    byte b = n;
    if (value < 0) b |= SIGN_BIT;
    data[0] = b;
    for (int i=8*(n-1), pos=1; i>=0; i-=8) data[pos++] = (byte) ((absValue >>> i) & 0xFF);
    os.write(data, 0, n+1);
  }

  /**
   * Deserialize an int value from a stream.
   * @param is the stream to read from.
   * @param buf a temporary buffer.
   * @return the int value read from the stream.
   * @throws IOException if an error occurs while reading the data.
   */
  public static int readVarInt(final InputStream is, final byte[] buf) throws IOException {
    byte b = (byte) is.read();
    if (b == ZERO_BIT) return 0;
    byte n = (byte) (b & 0x0F);
    int result = 0;
    if (n == 1) result = is.read();
    else {
      is.read(buf, 0, n);
      for (int i=8*(n-1), pos=0; i>=0; i-=8) result += (buf[pos++] & 0xFF) << i;
    }
    if ((b & SIGN_BIT) != 0) result = -result;
    return result;
  }

  /**
   * Deserialize a long value from a stream.
   * @param is the stream to read from.
   * @param buf a temporary buffer.
   * @return the long value read from the stream.
   * @throws IOException if an error occurs while reading the data.
   */
  public static long readVarLong(final InputStream is, final byte[] buf) throws IOException {
    byte b = (byte) is.read();
    if (b == ZERO_BIT) return 0L;
    byte n = (byte) (b & 0x0F);
    long result = 0L;
    if (n == 1) result = is.read();
    else {
      is.read(buf, 0, n);
      for (int i=8*(n-1), pos=0; i>=0; i-=8) result += (long) (buf[pos++] & 0xFF) << i;
    }
    if ((b & SIGN_BIT) != 0) result = -result;
    return result;
  }

  /**
   * Determine whether a character sequence is only made of ACII characters.
   * @param s the char sequence to check.
   * @return {@code true} if the sequence only contains ACII characters, {@code false} otherwise.
   * @throws Exception if any error occurs.
   */
  public static boolean isASCII(final String s) throws Exception {
    for (char c: s.toCharArray()) {
      if (c > 127) return false;
    }
    return true;
  }

  /**
   * Serialize an int value into an array of bytes.
   * @param os the stream to write to.
   * @param isAscii whether the string is pure ASCII.
   * @param value the string length to serialize.
   * @param data the array of bytes into which to serialize the value.
   * @throws IOException if an error occurs while writing the data.
   */
  public static void writeStringLength(final OutputStream os, final boolean isAscii, final int value, final byte[] data) throws IOException {
    if (value == 0) {
      os.write(ZERO_BIT);
      return;
    }
    int absValue = (value > 0) ? value : -value;
    byte n = 4;
    for (int i=0; i<INT_MAX_VALUES.length; i++) {
      if (absValue < INT_MAX_VALUES[i]) {
        n = (byte) (i + 1);
        break;
      }
    }
    byte b = n;
    if (isAscii) b |= ASCII_BIT;
    if (value < 0) b |= SIGN_BIT;
    data[0] = b;
    for (int i=8*(n-1), pos=1; i>=0; i-=8) data[pos++] = (byte) ((absValue >>> i) & 0xFF);
    os.write(data, 0, n+1);
  }

  /**
   * Deserialize an int value from a stream.
   * @param is the stream to read from.
   * @param buf a temporary buffer.
   * @return the int value read from the stream.
   * @throws IOException if an error occurs while reading the data.
   */
  public static Pair<Integer, Boolean> readStringLength(final InputStream is, final byte[] buf) throws IOException {
    byte b = (byte) is.read();
    if (b == ZERO_BIT) return new Pair<>(0, false);
    byte n = (byte) (b & 0x0F);
    int result = 0;
    if (n == 1) result = is.read();
    else {
      is.read(buf, 0, n);
      for (int i=8*(n-1), pos=0; i>=0; i-=8) result += (buf[pos++] & 0xFF) << i;
    }
    if ((b & SIGN_BIT) != 0) result = -result;
    return new Pair<>(result, (b & ASCII_BIT) != 0);
  }
}
