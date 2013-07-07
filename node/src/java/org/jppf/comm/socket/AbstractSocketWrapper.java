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

import java.io.*;
import java.net.*;

import org.jppf.JPPFException;
import org.jppf.utils.*;
import org.slf4j.*;


/**
 * Common abstract superclass for all socket clients. This class is provided as a convenience and provides
 * as set of common methods to all classes implementing the
 * {@link org.jppf.comm.socket.SocketWrapper SocketWrapper} interface.
 * @author Laurent Cohen
 * @author Domingos Creado
 * @author Jeff Rosen
 */
public abstract class AbstractSocketWrapper implements SocketWrapper
{
  /**
   * Logger for this class.
   */
  private static Logger log = LoggerFactory.getLogger(AbstractSocketWrapper.class);
  /**
   * Determines whether debug-level logging is enabled.
   */
  private static boolean debugEnabled = log.isDebugEnabled();
  /**
   * Determines whether trace-level logging is enabled.
   */
  private static boolean traceEnabled = log.isTraceEnabled();
  /**
   * The underlying socket wrapped by this SocketClient.
   */
  protected Socket socket = null;
  /**
   * A timestamp that should reflect the system millisecond counter at the
   * last known good usage of the underlying socket.
   */
  private long socketTimestamp = 0L;
  /**
   * A reference to the underlying socket's output stream.
   */
  protected DataOutputStream dos = null;
  /**
   * A buffered stream built on top of to the underlying socket's input stream.
   */
  protected DataInputStream dis = null;
  /**
   * The host the socket connects to.
   */
  protected String host = null;
  /**
   * The port number on the host the socket connects to.
   */
  protected int port = -1;
  /**
   * Flag indicating the opened state of the underlying socket.
   */
  protected boolean opened = false;
  /**
   * Used to serialize or deserialize an object into or from an array of bytes.
   */
  protected ObjectSerializer serializer = null;

  /**
   * Default constructor is visible to subclasses only.
   */
  protected AbstractSocketWrapper()
  {
  }

  /**
   * Initialize this socket client and connect it to the specified host on the specified port.
   * @param host the remote host this socket client connects to.
   * @param port the remote port on the host this socket client connects to.
   * @param serializer the object serializer used by this socket client.
   * @throws ConnectException if the connection fails.
   * @throws IOException if there is an issue with the socket streams.
   */
  public AbstractSocketWrapper(final String host, final int port, final ObjectSerializer serializer)
  throws ConnectException, IOException
  {
    this.host = host;
    this.port = port;
    this.serializer = serializer;
    open();
  }

  /**
   * Initialize this socket client with an already opened and connected socket.
   * @param socket the underlying socket this socket client wraps around.
   * @throws JPPFException if the socket connection fails.
   */
  public AbstractSocketWrapper(final Socket socket) throws JPPFException
  {
    try
    {
      this.host = socket.getInetAddress().getHostName();
      this.port = socket.getPort();
      this.socket = socket;
      initStreams();
      opened = true;
    }
    catch(IOException ioe)
    {
      throw new JPPFException(ioe.getMessage(), ioe);
    }
  }

  /**
   * Send an array of bytes over a TCP socket connection.
   * @param buf the buffer container for the data to send.
   * @throws IOException if the underlying output stream throws an exception.
   * @see org.jppf.comm.socket.SocketWrapper#sendBytes(org.jppf.utils.JPPFBuffer)
   */
  @Override
  public void sendBytes(final JPPFBuffer buf) throws IOException
  {
    checkOpened();
    dos.writeInt(buf.getLength());
    dos.write(buf.getBuffer(), 0, buf.getLength());
    dos.flush();
  }

  /**
   * Send an array of bytes over a TCP socket connection.
   * @param data the data to send.
   * @param offset the position where to start reading data from the input array.
   * @param len the length of data to write.
   * @throws Exception if the underlying output stream throws an exception.
   * @see org.jppf.comm.socket.SocketWrapper#write(byte[], int, int)
   */
  @Override
  public void write(final byte[] data, final int offset, final int len) throws Exception
  {
    checkOpened();
    dos.write(data, offset, len);
    dos.flush();
  }

  /**
   * Write an int value over a socket connection.
   * @param n the value to write.
   * @throws Exception if the underlying output stream throws an exception.
   * @see org.jppf.comm.socket.SocketWrapper#writeInt(int)
   */
  @Override
  public void writeInt(final int n) throws Exception
  {
    checkOpened();
    dos.writeInt(n);
    dos.flush();
  }

  /**
   * Flush the data currently in the send buffer.
   * @throws IOException if an I/O error occurs.
   * @see org.jppf.comm.socket.SocketWrapper#flush()
   */
  @Override
  public void flush() throws IOException
  {
    updateSocketTimestamp();
    dos.flush();
  }

  /**
   * Read an object from a TCP socket connection.
   * This method blocks until an object is received.
   * @return the object that was read from the underlying input stream.
   * @throws Exception if the underlying input stream throws an exception.
   * @see org.jppf.comm.socket.SocketWrapper#receive()
   */
  @Override
  public Object receive() throws Exception
  {
    return receive(0);
  }

  /**
   * Read an object from a TCP socket connection.
   * This method blocks until an object is received or the specified timeout has expired, whichever happens first.
   * @param timeout timeout after which the operation is aborted. A timeout of zero is interpreted as an infinite timeout.
   * @return an array of bytes containing the serialized object to receive.
   * @throws IOException if the underlying input stream throws an exception.
   * @see org.jppf.comm.socket.SocketWrapper#receiveBytes(int)
   */
  @Override
  public JPPFBuffer receiveBytes(final int timeout) throws IOException
  {
    checkOpened();
    JPPFBuffer buf = null;
    try
    {
      if (timeout > 0) socket.setSoTimeout(timeout);
      int len = dis.readInt();
      byte[] buffer = new byte[len];
      read(buffer, 0, len);
      buf = new JPPFBuffer(buffer, len);
    }
    finally
    {
      // disable the timeout on subsequent read operations.
      if (timeout > 0) socket.setSoTimeout(0);
      updateSocketTimestamp();
    }
    return buf;
  }

  /**
   * Read <code>len</code> bytes from a TCP connection into a byte array, starting
   * at position <code>offset</code> in that array.
   * This method blocks until at least one byte of data is received.
   * @param data an array of bytes into which the data is stored.
   * @param offset the position where to start storing data read from the socket.
   * @param len the length of data to read.
   * @return the number of bytes actually read or -1 if the end of stream was reached.
   * @throws IOException if the underlying input stream throws an exception.
   * @see org.jppf.comm.socket.SocketWrapper#read(byte[], int, int)
   */
  @Override
  public int read(final byte[] data, final int offset, final int len) throws IOException
  {
    checkOpened();
    int count = 0;
    while (count < len)
    {
      int n = dis.read(data, count + offset, len - count);
      if (n < 0) break;
      else count += n;
    }
    updateSocketTimestamp();
    return count;
  }

  /**
   * Read an int value from a socket connection.
   * @return n the value to read from the socket, or -1 if end of stream was reached.
   * @throws Exception if the underlying input stream throws an exception.
   * @see org.jppf.comm.socket.SocketWrapper#readInt()
   */
  @Override
  public int readInt() throws Exception
  {
    int intRead = dis.readInt();
    updateSocketTimestamp();
    return intRead;
  }

  /**
   * Open the underlying socket connection.
   * @throws ConnectException if the socket fails to connect.
   * @throws IOException if the underlying input and output streams raise an error.
   * @see org.jppf.comm.socket.SocketWrapper#open()
   */
  @Override
  public final void open() throws ConnectException, IOException
  {
    if (!opened)
    {
      if ((host == null) || "".equals(host.trim()))
        throw new ConnectException("You must specify the host name");
      else if (port <= 0)
        throw new ConnectException("You must specify the port number");
      socket = new Socket();
      InetSocketAddress addr = new InetSocketAddress(host, port);
      socket.setReceiveBufferSize(SOCKET_RECEIVE_BUFFER_SIZE);
      socket.setSendBufferSize(SOCKET_RECEIVE_BUFFER_SIZE);
      socket.setTcpNoDelay(SOCKET_TCP_NO_DELAY);
      if (debugEnabled) log.debug("before connect()");
      socket.connect(addr);
      if (debugEnabled) log.debug("after connect()");
      initStreams();
      opened = true;
      if (debugEnabled) log.debug("getReceiveBufferSize() = " + socket.getReceiveBufferSize());
    }
    //else throw new ConnectException("Client connection already opened");
  }

  /**
   * Initialize all the stream used for receiving and sending objects through the
   * underlying socket connection.
   * @throws IOException if an error occurs during the streams initialization.
   */
  protected final void initStreams() throws IOException
  {
    OutputStream os = socket.getOutputStream();
    InputStream is = socket.getInputStream();
    //BufferedOutputStream bos = new BufferedOutputStream(os, SOCKET_RECEIVE_BUFFER_SIZE);
    BufferedOutputStream bos = new BufferedOutputStream(os);
    dos = new DataOutputStream(bos);
    //dos.flush();
    BufferedInputStream bis = new BufferedInputStream(is);
    dis = new DataInputStream(bis);
  }

  /**
   * Close the underlying socket connection.
   * @throws ConnectException if the socket connection is not opened.
   * @throws IOException if the underlying input and output streams raise an error.
   * @see org.jppf.comm.socket.SocketWrapper#close()
   */
  @Override
  public void close() throws ConnectException, IOException
  {
    opened = false;
    if (socket != null) socket.close();
  }

  /**
   * Determine whether this socket client is opened or not.
   * @return true if this client is opened, false otherwise.
   * @see org.jppf.comm.socket.SocketWrapper#isOpened()
   */
  @Override
  public boolean isOpened()
  {
    return opened;
  }

  /**
   * Check whether the underlying socket is opened or not.
   * @throws ConnectException if the connection is not opened.
   */
  protected void checkOpened() throws ConnectException
  {
    if (!opened) throw new ConnectException("Client connection not opened");
  }

  /**
   * This implementation does nothing.
   * @return null.
   * @see org.jppf.comm.socket.SocketWrapper#getSerializer()
   */
  @Override
  public ObjectSerializer getSerializer()
  {
    return null;
  }

  /**
   * This implementation does nothing.
   * @param serializer not used.
   * @see org.jppf.comm.socket.SocketWrapper#setSerializer(org.jppf.utils.ObjectSerializer)
   */
  @Override
  public void setSerializer(final ObjectSerializer serializer)
  {
  }

  /**
   * Get the remote host the underlying socket connects to.
   * @return the host name or ip address as a string.
   */
  @Override
  public String getHost()
  {
    return host;
  }

  /**
   * Set the remote host the underlying socket connects to.
   * @param host the host name or ip address as a string.
   */
  @Override
  public void setHost(final String host)
  {
    this.host = host;
  }

  /**
   * Get the remote port the underlying socket connects to.
   * @return the port number on the remote host.
   */
  @Override
  public int getPort()
  {
    return port;
  }

  /**
   * Get the remote port the underlying socket connects to.
   * @param port the port number on the remote host.
   */
  @Override
  public void setPort(final int port)
  {
    this.port = port;
  }

  /**
   * Get the underlying socket used by this socket wrapper.
   * @return a Socket instance.
   * @see org.jppf.comm.socket.SocketWrapper#getSocket()
   */
  @Override
  public Socket getSocket()
  {
    return socket;
  }

  /**
   * Set the underlying socket to be used by this socket wrapper.
   * @param socket a Socket instance.
   * @see org.jppf.comm.socket.SocketWrapper#setSocket(java.net.Socket)
   */
  @Override
  public void setSocket(final Socket socket)
  {
    this.socket = socket;
  }

  /**
   * Skip <code>n</code> bytes of data from the socket or channel input stream.
   * @param n the number of bytes to skip.
   * @return the actual number of bytes skipped, or -1 if the end of file is reached..
   * @throws Exception if an IO error occurs.
   * @see org.jppf.comm.socket.SocketWrapper#skip(int)
   */
  @Override
  public int skip(final int n) throws Exception
  {
    if (n < 0) throw new IllegalArgumentException("number of bytes to skip must be >= 0");
    else if (n == 0) return 0;
    int count = 0;
    while (count < n)
    {
      long p = dis.skip(n-count);
      //if (p < 0) break;
      if (p <= 0) break;
      else count += p;
    }
    updateSocketTimestamp();
    return count;
  }

  /**
   * Returns a timestamp that should reflect the system millisecond counter at the
   * last known good usage of the underlying socket.
   * @return the socket usage timestamp
   */
  @Override
  public long getSocketTimestamp() {
    return socketTimestamp;
  }

  /**
   * Marks the socket "known good" usage timestamp with the current value of the
   * system millisecond counter.
   */
  protected void updateSocketTimestamp() {
    socketTimestamp = System.currentTimeMillis();
  }
}
