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
package org.jppf.comm.socket;

import java.io.IOException;
import java.net.Socket;

import org.jppf.utils.*;

/**
 * This interface is common to all classes wrapping an underlying socket connections.<br>
 * The underlying socket API can be either based on a socket channel (blocking or non-blocking),
 * or just based on a plain socket.
 * @author Laurent Cohen
 * @author Jeff Rosen
 */
public interface SocketWrapper
{
  /**
   * Send an object over a TCP socket connection.
   * @param o the object to send.
   * @throws Exception if the underlying output stream throws an exception.
   */
  void send(Object o) throws Exception;

  /**
   * Send an array of bytes over a TCP socket connection.
   * @param buf the buffer container for the data to send.
   * @throws Exception if the underlying output stream throws an exception.
   */
  void sendBytes(JPPFBuffer buf) throws Exception;

  /**
   * Send an array of bytes over a TCP socket connection.
   * @param data the data to send.
   * @param offset the position where to start reading data from the input array.
   * @param len the length of data to write.
   * @throws Exception if the underlying output stream throws an exception.
   */
  void write(byte[] data, int offset, int len) throws Exception;

  /**
   * Write an int value over a socket connection.
   * @param n the value to write.
   * @throws Exception if the underlying output stream throws an exception.
   */
  void writeInt(int n) throws Exception;

  /**
   * Flush the data currently in the send buffer.
   * @throws IOException if an I/O error occurs.
   */
  void flush() throws IOException;

  /**
   * Read an object from a TCP socket connection.
   * This method blocks until an object is received.
   * @return the object that was read from the underlying input stream.
   * @throws Exception if the underlying input stream throws an exception.
   */
  Object receive() throws Exception;

  /**
   * Read an object from a TCP socket connection.
   * This method blocks until an object is received or the specified timeout has expired, whichever happens first.
   * @param timeout timeout after which the operation is aborted. A timeout of zero is interpreted as an infinite timeout.
   * @return the object that was read from the underlying input stream or null if the operation timed out.
   * @throws Exception if the underlying input stream throws an exception.
   */
  Object receive(int timeout) throws Exception;

  /**
   * Read <code>len</code> bytes from a TCP connection into a byte array, starting
   * at position <code>offset</code> in that array.
   * This method blocks until at least one byte of data is received.
   * @param data an array of bytes into which the data is stored.
   * @param offset the position where to start storing data read from the socket.
   * @param len the length of data to read.
   * @return the number of bytes actually read or -1 if the end of stream was reached.
   * @throws Exception if the underlying input stream throws an exception.
   */
  int read(byte[] data, int offset, int len) throws Exception;

  /**
   * Read an int value from a socket connection.
   * @return n the value to read from the socket, or -1 if end of stream was reached.
   * @throws Exception if the underlying input stream throws an exception.
   */
  int readInt() throws Exception;

  /**
   * Read an array of bytes from a TCP socket connection.
   * The data read is prefixed by an int header whose value is the actual length of data to read.
   * This method blocks until data is received or the specified timeout has expired, whichever happens first.
   * @param timeout timeout after which the operation is aborted. A timeout of zero is interpreted as an infinite timeout.
   * @return a buffer holding the length of data and the data itself.
   * @throws Exception if the underlying input stream throws an exception.
   */
  JPPFBuffer receiveBytes(int timeout) throws Exception;

  /**
   * Skip <code>n</code> bytes of data from the socket or channel input stream.
   * @param n the number of bytes to skip.
   * @return the actual number of bytes skipped.
   * @throws Exception if an IO error occurs.
   */
  int skip(int n) throws Exception;

  /**
   * Open the underlying socket connection.
   * @throws Exception if the underlying input and output streams raise an error.
   */
  void open() throws Exception;

  /**
   * Close the underlying socket connection.
   * @throws Exception if the underlying input and output streams raise an error.
   */
  void close() throws Exception;

  /**
   * Determine whether this socket client is opened or not.
   * @return true if this client is opened, false otherwise.
   */
  boolean isOpened();

  /**
   * Get an object serializer / deserializer to convert an object to or from an array of bytes.
   * @return an <code>ObjectSerializer</code> instance.
   */
  ObjectSerializer getSerializer();

  /**
   * Set the object serializer / deserializer to convert an object to or from an array of bytes.
   * @param serializer an <code>ObjectSerializer</code> instance.
   */
  void setSerializer(ObjectSerializer serializer);

  /**
   * Get the remote host the underlying socket connects to.
   * @return the host name or ip address as a string.
   */
  String getHost();

  /**
   * Set the remote host the underlying socket connects to.
   * @param host the host name or ip address as a string.
   */
  void setHost(String host);

  /**
   * Get the remote port the underlying socket connects to.
   * @return the port number on the remote host.
   */
  int getPort();

  /**
   * Get the remote port the underlying socket connects to.
   * @param port the port number on the remote host.
   */
  void setPort(int port);

  /**
   * Get the underlying socket used by this socket wrapper.
   * @return a Socket instance.
   */
  Socket getSocket();

  /**
   * Set the underlying socket to be used by this socket wrapper.
   * @param socket a Socket instance.
   */
  void setSocket(Socket socket);

  /**
   * Returns a timestamp that should reflect the system millisecond counter at the
   * last known good usage of the underlying socket.
   * @return the socket usage timestamp
   */
  long getSocketTimestamp();
}
