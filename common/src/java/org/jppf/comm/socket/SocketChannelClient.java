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
package org.jppf.comm.socket;

import java.io.IOException;
import java.net.*;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;

import org.jppf.io.IO;
import org.jppf.serialization.ObjectSerializer;
import org.jppf.utils.JPPFBuffer;
import org.slf4j.*;

/**
 * This SocketWrapper implementation relies on an underlying SocketChannel, in order to allow
 * writing to, and reading from, at the same time from the same socket connection.
 * @author Laurent Cohen
 * @author Jeff Rosen
 */
public class SocketChannelClient implements SocketWrapper {
  /**
   * Logger for this class.
   */
  private static final Logger log = LoggerFactory.getLogger(SocketChannelClient.class);
  /**
   * Determines whether the debug level is enabled in the log configuration, without the cost of a method call.
   */
  private static final boolean debugEnabled = log.isDebugEnabled();
  /**
   * The channel associated with the underlying socket connection.
   */
  private SocketChannel channel = null;
  /**
   * The host to connect to.
   */
  private String host = null;
  /**
   * The port to listen on the host.
   */
  private int port = -1;
  /**
   * Used to serialize and deserialize the objects to send or receive over the connection.
   */
  private ObjectSerializer serializer = null;
  /**
   * Determines whether this client is opened or not.
   */
  private boolean opened = false;
  /**
   * Determines whether the socket channel must be in blocking or non-blocking mode.
   */
  private boolean blocking = false;

  /**
   * Initialize this socket channel client.
   * @param blocking true if the socket channel is in blocking mode, false otherwise.
   * @throws IOException if the socket channel could not be opened.
   */
  public SocketChannelClient(final boolean blocking) throws IOException {
    this.blocking = blocking;
  }

  /**
   * Initialize this socket channel client with a specified host and port.
   * @param host the host to connect to.
   * @param port the port to listen on the host.
   * @param blocking true if the socket channel is in blocking mode, false otherwise.
   * @throws IOException if the socket channel could not be opened.
   */
  public SocketChannelClient(final String host, final int port, final boolean blocking) throws IOException {
    this(blocking);
    this.host = host;
    this.port = port;
  }

  /**
   * Send an object over a TCP socket connection.
   * @param o the object to send.
   * @throws Exception if the underlying output stream throws an exception.
   */
  @Override
  public void send(final Object o) throws Exception {
    final JPPFBuffer buf = getSerializer().serialize(o);
    sendBytes(buf);
  }

  /**
   * Send an array of bytes over a TCP socket connection.
   * @param buf the buffer container for the data to send.
   * @throws Exception if the underlying output stream throws an exception.
   */
  @Override
  public void sendBytes(final JPPFBuffer buf) throws Exception {
    final int length = buf.getLength();
    writeInt(length);
    write(buf.getBuffer(), 0, length);
  }

  /**
   * Send an array of bytes over a TCP socket connection.
   * @param data the data to send.
   * @param offset the position where to start reading data from the input array.
   * @param len the length of data to write.
   * @throws Exception if the underlying output stream throws an exception.
   */
  @Override
  public void write(final byte[] data, final int offset, final int len) throws Exception {
    final ByteBuffer buffer = ByteBuffer.wrap(data, offset, len);
    for (int count = 0; count < len;) count += channel.write(buffer);
  }

  /**
   * Write an int value over a socket connection.
   * @param n the value to write.
   * @throws Exception if the underlying output stream throws an exception.
   */
  @Override
  public void writeInt(final int n) throws Exception {
    //ByteBuffer buffer = ByteBuffer.wrap(SerializationUtils.writeInt(n));
    final ByteBuffer buffer = ByteBuffer.allocate(4);
    buffer.putInt(n);
    buffer.flip();
    for (int count = 0; count < 4;) count += channel.write(buffer);
  }

  /**
   * This method does nothing, there is no flush on socket channels.
   * @throws IOException if an I/O error occurs.
   */
  @Override
  public void flush() throws IOException {
  }

  /**
   * Read an object from a TCP socket connection.
   * This method blocks until an object is received.
   * @return the object that was read from the underlying input stream.
   * @throws Exception if the underlying input stream throws an exception.
   */
  @Override
  public Object receive() throws Exception {
    return receive(0);
  }

  /**
   * Read an object from a TCP socket connection.
   * This method blocks until an object is received or the specified timeout has expired, whichever happens first.
   * @param timeout timeout after which the operation is aborted. A timeout of zero is interpreted as an infinite timeout.
   * @return the object that was read from the underlying input stream or null if the operation timed out.
   * @throws Exception if the underlying input stream throws an exception.
   */
  @Override
  public Object receive(final int timeout) throws Exception {
    Object o = null;
    try {
      if (timeout > 0) channel.socket().setSoTimeout(timeout);
      final JPPFBuffer buf = receiveBytes(timeout);
      o = getSerializer().deserialize(buf);
    } finally {
      // disable the timeout on subsequent read operations.
      if (timeout > 0) channel.socket().setSoTimeout(0);
    }
    return o;
  }

  /**
   * Read an object from a TCP socket connection.
   * This method blocks until an object is received or the specified timeout has expired, whichever happens first.
   * @param timeout timeout after which the operation is aborted. A timeout of zero is interpreted as an infinite timeout.
   * @return an array of bytes containing the serialized object to receive.
   * @throws Exception if the underlying input stream throws an exception.
   */
  @Override
  public JPPFBuffer receiveBytes(final int timeout) throws Exception {
    final int length = readInt();
    final byte[] data = new byte[length];
    read(data, 0, length);
    return new JPPFBuffer(data, length);
  }

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
  @Override
  public int read(final byte[] data, final int offset, final int len) throws Exception {
    final ByteBuffer byteBuffer = ByteBuffer.wrap(data, offset, len);
    int count = 0;
    while (count < len) count += channel.read(byteBuffer);
    return count;
  }

  @Override
  public int readPartial(final byte[] data, final int offset, final int len) throws Exception {
    final ByteBuffer byteBuffer = ByteBuffer.wrap(data, offset, len);
    return channel.read(byteBuffer);
  }

  /**
   * Read an int value from a socket connection.
   * @return n the value to read from the socket, or -1 if end of stream was reached.
   * @throws Exception if the underlying input stream throws an exception.
   */
  @Override
  public int readInt() throws Exception {
    final ByteBuffer buf = ByteBuffer.allocate(4);
    int count = 0;
    while (count < 4) count += channel.read(buf);
    buf.flip();
    return buf.getInt();
  }

  /**
   * Open the underlying socket connection.
   * @throws ConnectException if the socket fails to connect.
   * @throws IOException if the underlying input and output streams raise an error.
   */
  @Override
  public void open() throws ConnectException, IOException {
    channel = SocketChannel.open();
    channel.setOption(StandardSocketOptions.SO_RCVBUF, IO.SOCKET_BUFFER_SIZE);
    channel.setOption(StandardSocketOptions.SO_SNDBUF, IO.SOCKET_BUFFER_SIZE);
    channel.setOption(StandardSocketOptions.TCP_NODELAY, IO.SOCKET_TCP_NODELAY);
    channel.setOption(StandardSocketOptions.SO_KEEPALIVE, IO.SOCKET_KEEPALIVE);
    final InetSocketAddress address = new InetSocketAddress(host, port);
    channel.connect(address);
    if (!blocking) {
      while (!channel.finishConnect()) {
        try {
          Thread.sleep(1L);
        } catch (@SuppressWarnings("unused") final InterruptedException e) {
        }
      }
    }
    opened = true;
    if (debugEnabled) log.debug("getReceiveBufferSize() = {}", channel.getOption(StandardSocketOptions.SO_RCVBUF));
  }

  /**
   * Close the underlying socket connection.
   * @throws ConnectException if the socket connection is not opened.
   * @throws IOException if the underlying input and output streams raise an error.
   */
  @Override
  public void close() throws ConnectException, IOException {
    if (opened) {
      opened = false;
      channel.close();
    }
  }

  /**
   * Determine whether this socket client is opened or not.
   * @return true if this client is opened, false otherwise.
   */
  @Override
  public boolean isOpened() {
    return opened;
  }

  /**
   * Get an object serializer / deserializer to convert an object to or from an array of bytes.
   * @return an <code>ObjectSerializer</code> instance.
   */
  @Override
  public ObjectSerializer getSerializer() {
    if (serializer == null) {
      //serializer = new ObjectSerializerImpl();
      final String name = "org.jppf.utils.ObjectSerializerImpl";
      try {
        serializer = (ObjectSerializer) Class.forName(name).newInstance();
      } catch (InstantiationException|IllegalAccessException|ClassNotFoundException e) {
        log.error(e.getMessage(), e);
      }
    }
    return serializer;
  }

  /**
   * Set the object serializer / deserializer to convert an object to or from an array of bytes.
   * @param serializer an <code>ObjectSerializer</code> instance.
   */
  @Override
  public void setSerializer(final ObjectSerializer serializer) {
    this.serializer = serializer;
  }

  /**
   * Get the remote host the underlying socket connects to.
   * @return the host name or ip address as a string.
   */
  @Override
  public String getHost() {
    return host;
  }

  /**
   * Set the remote host the underlying socket connects to.
   * @param host the host name or ip address as a string.
   */
  @Override
  public void setHost(final String host) {
    this.host = host;
  }

  /**
   * Get the remote port the underlying socket connects to.
   * @return the port number on the remote host.
   */
  @Override
  public int getPort() {
    return port;
  }

  /**
   * Get the remote port the underlying socket connects to.
   * @param port the port number on the remote host.
   */
  @Override
  public void setPort(final int port) {
    this.port = port;
  }

  /**
   * Get the underlying socket used by this socket wrapper.
   * @return a Socket instance.
   */
  @Override
  public Socket getSocket() {
    return channel.socket();
  }

  /**
   * Set the underlying socket to be used by this socket wrapper.
   * @param socket a Socket instance.
   */
  @Override
  public void setSocket(final Socket socket) {
  }

  /**
   * Get the underlying socket channel  used by this socket wrapper.
   * @return a SocketChannel instance.
   */
  public SocketChannel getChannel() {
    return channel;
  }

  /**
   * Set the underlying socket to be used by this socket wrapper.
   * @param channel a SocketChannel instance.
   */
  public void setChannel(final SocketChannel channel) {
    this.channel = channel;
  }

  /**
   * Skip <code>n</code> bytes of data from the socket of channel input stream.
   * @param n the number of bytes to skip.
   * @return the actual number of bytes skipped, or -1 if the end of stream is reached.
   * @throws Exception if an IO error occurs.
   */
  @Override
  public int skip(final int n) throws Exception {
    if (n < 0) throw new IllegalArgumentException("number of bytes to skip must be >= 0");
    else if (n == 0) return 0;
    final ByteBuffer buf = ByteBuffer.allocate(n);
    while (buf.hasRemaining()) {
      final int r = channel.read(buf);
      if ((r == 0) && blocking) break;
      else if (r < 0) break;
    }
    return buf.position();
  }

  /**
   * Send an array of bytes over a TCP socket connection.
   * @param data the data to send.
   * @throws Exception if the underlying output stream throws an exception.
   */
  public void write(final byte[] data) throws Exception {
    write(data, 0, data.length);
  }

  /**
   * Returns a timestamp that should reflect the system millisecond counter at the
   * last known good usage of the underlying socket.
   * 
   * NOTE: In this case we assume that the channel is "known good" and just return
   * the current system tick.
   * 
   * @return the socket usage timestamp
   */
  @Override
  public long getSocketTimestamp() {
    return System.nanoTime();
  }
}
