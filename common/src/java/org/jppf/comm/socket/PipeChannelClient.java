/*
 * Java Parallel Processing Framework.
 * Copyright (C) 2005-2009 JPPF Team.
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
import java.nio.channels.*;
import org.apache.commons.logging.*;
import org.jppf.utils.*;

/**
 * This SocketWrapper implementation relies on an underlying SocketChannel, in order to allow
 * writing to, and reading from, at the same time from the same socket connection. 
 * @author Laurent Cohen
 */
public class PipeChannelClient implements SocketWrapper
{
	/**
	 * Logger for this class.
	 */
	private static Log log = LogFactory.getLog(PipeChannelClient.class);
	/**
	 * Used to serialize and deserialize the objects to send or receive over the connection.
	 */
	private ObjectSerializer serializer = null;
	/**
	 * Determines whether this client is opened or not.
	 */
	private boolean opened = false;
	/**
	 * Determines whther the socket channel must be in blocking or non-blocking mode.
	 */
	private boolean blocking = true;
	/**
	 * Inbound nio pipe.
	 */
	private Pipe inPipe = null;
	/**
	 * Outbound nio pipe.
	 */
	private Pipe outPipe = null;

	/**
	 * Initialize this piped channel client.
	 * @param inPipe - inbound nio pipe.
	 * @param outPipe - outbound nio pipe.
	 * @throws IOException if the socket channel could not be opened.
	 */
	public PipeChannelClient(Pipe inPipe, Pipe outPipe) throws IOException
	{
		this.inPipe = inPipe;
		this.outPipe = outPipe;
	}

	/**
	 * Send an object over a TCP socket connection.
	 * @param o the object to send.
	 * @throws Exception if the underlying output stream throws an exception.
	 * @see org.jppf.comm.socket.SocketWrapper#send(java.lang.Object)
	 */
	public void send(Object o) throws Exception
	{
		JPPFBuffer buf = getSerializer().serialize(o);
		sendBytes(buf);
	}

	/**
	 * Send an array of bytes over a TCP socket connection.
	 * @param buf the buffer container for the data to send.
	 * @throws Exception if the underlying output stream throws an exception.
	 * @see org.jppf.comm.socket.SocketWrapper#sendBytes(org.jppf.utils.JPPFBuffer)
	 */
	public void sendBytes(JPPFBuffer buf) throws Exception
	{
		int length = buf.getLength();
		writeInt(length);
		write(buf.getBuffer(), 0, length);
	}

	/**
	 * Send an array of bytes over a TCP socket connection.
	 * @param data the data to send.
	 * @param offset the position where to start reading data from the input array.
	 * @param len the length of data to write.
	 * @throws Exception if the underlying output stream throws an exception.
	 * @see org.jppf.comm.socket.SocketWrapper#write(byte[], int, int)
	 */
	public void write(byte[] data, int offset, int len) throws Exception
	{
		ByteBuffer buffer = ByteBuffer.wrap(data, offset, len);
		for (int count=0; count < len;) count += outPipe.sink().write(buffer);
	}

	/**
	 * Write an int value over a socket connection.
	 * @param n the value to write.
	 * @throws Exception if the underlying output stream throws an exception.
	 * @see org.jppf.comm.socket.SocketWrapper#writeInt(int)
	 */
	public void writeInt(int n) throws Exception
	{
		ByteBuffer buffer = ByteBuffer.wrap(SerializationUtils.writeInt(n));
		for (int count=0; count < 4;) count += outPipe.sink().write(buffer);
	}

	/**
	 * This method does nothing, there is no flush on socket channels.
	 * @throws IOException if an I/O error occurs.
	 * @see org.jppf.comm.socket.SocketWrapper#flush()
	 */
	public void flush() throws IOException
	{
	}

	/**
	 * Read an object from a TCP socket connection.
	 * This method blocks until an object is received.
	 * @return the object that was read from the underlying input stream.
	 * @throws Exception if the underlying input stream throws an exception.
	 * @see org.jppf.comm.socket.SocketWrapper#receive()
	 */
	public Object receive() throws Exception
	{
		return receive(0);
	}

	/**
	 * Read an object from a TCP socket connection.
	 * This method blocks until an object is received or the specified timeout has expired, whichever happens first.
	 * @param timeout timeout after which the operation is aborted. A timeout of zero is interpreted as an infinite timeout.
	 * @return the object that was read from the underlying input stream or null if the operation timed out.
	 * @throws Exception if the underlying input stream throws an exception.
	 * @see org.jppf.comm.socket.SocketWrapper#receive(int)
	 */
	public Object receive(int timeout) throws Exception
	{
		Object o = null;
		try
		{
			JPPFBuffer buf = receiveBytes(timeout);
			o = getSerializer().deserialize(buf);
		}
		catch(ClassNotFoundException e)
		{
			throw e;
		}
		catch(IOException e)
		{
			throw e;
		}
		return o;
	}

	/**
	 * Read an object from a TCP socket connection.
	 * This method blocks until an object is received or the specified timeout has expired, whichever happens first.
	 * @param timeout timeout after which the operation is aborted. A timeout of zero is interpreted as an infinite timeout.
	 * @return an array of bytes containing the serialized object to receive.
	 * @throws Exception if the underlying input stream throws an exception.
	 * @see org.jppf.comm.socket.SocketWrapper#receiveBytes(int)
	 */
	public JPPFBuffer receiveBytes(int timeout) throws Exception
	{
		int length = readInt();
		byte[] data = new byte[length];
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
	 * @see org.jppf.comm.socket.SocketWrapper#read(byte[], int, int)
	 */
	public int read(byte[] data, int offset, int len) throws Exception
	{
		ByteBuffer byteBuffer = ByteBuffer.wrap(data, offset, len);
		int count = 0;
		while (count < len) count += inPipe.source().read(byteBuffer);
		return count;
	}

	/**
	 * Read an int value from a socket connection.
	 * @return n the value to read from the socket, or -1 if end of stream was reached.
	 * @throws Exception if the underlying input stream throws an exception.
	 * @see org.jppf.comm.socket.SocketWrapper#readInt()
	 */
	public int readInt() throws Exception
	{
		ByteBuffer buf = ByteBuffer.allocateDirect(4);
		int count = 0;
		while (count < 4) count += inPipe.source().read(buf);
		buf.flip();
		return buf.getInt();
	}

	/**
	 * Open the underlying socket connection.
	 * @throws ConnectException if the socket fails to connect.
	 * @throws IOException if the underlying input and output streams raise an error.
	 * @see org.jppf.comm.socket.SocketWrapper#open()
	 */
	public void open() throws ConnectException, IOException
	{
		opened = true;
	}

	/**
	 * Close the underlying socket connection.
	 * @throws ConnectException if the socket connection is not opened.
	 * @throws IOException if the underlying input and output streams raise an error.
	 * @see org.jppf.comm.socket.SocketWrapper#close()
	 */
	public void close() throws ConnectException, IOException
	{
		if (opened)
		{
			opened = false;
		}
	}

	/**
	 * Determine whether this socket client is opened or not.
	 * @return true if this client is opened, false otherwise.
	 * @see org.jppf.comm.socket.SocketWrapper#isOpened()
	 */
	public boolean isOpened()
	{
		return opened;
	}

	/**
	 * Get an object serializer / deserializer to convert an object to or from an array of bytes.
	 * @return an <code>ObjectSerializer</code> instance.
	 * @see org.jppf.comm.socket.SocketWrapper#getSerializer()
	 */
	public ObjectSerializer getSerializer()
	{
		if (serializer == null)
		{
			//serializer = new ObjectSerializerImpl();
			String name = "org.jppf.utils.ObjectSerializerImpl";
			try
			{
				serializer = (ObjectSerializer) Class.forName(name).newInstance();
			}
			catch(InstantiationException e)
			{
				log.fatal(e.getMessage(), e);
			}
			catch(IllegalAccessException e)
			{
				log.fatal(e.getMessage(), e);
			}
			catch(ClassNotFoundException e)
			{
				log.fatal(e.getMessage(), e);
			}
		}
		return serializer;
	}

	/**
	 * Set the object serializer / deserializer to convert an object to or from an array of bytes.
	 * @param serializer an <code>ObjectSerializer</code> instance.
	 * @see org.jppf.comm.socket.SocketWrapper#setSerializer(org.jppf.utils.ObjectSerializer)
	 */
	public void setSerializer(ObjectSerializer serializer)
	{
		this.serializer = serializer;
	}

	/**
	 * Get the remote host the underlying socket connects to.
	 * @return the host name or ip address as a string.
	 * @see org.jppf.comm.socket.SocketWrapper#getHost()
	 */
	public String getHost()
	{
		return null;
	}

	/**
	 * Set the remote host the underlying socket connects to.
	 * @param host the host name or ip address as a string.
	 * @see org.jppf.comm.socket.SocketWrapper#setHost(java.lang.String)
	 */
	public void setHost(String host)
	{
	}

	/**
	 * Get the remote port the underlying socket connects to.
	 * @return the port number on the remote host.
	 * @see org.jppf.comm.socket.SocketWrapper#getPort()
	 */
	public int getPort()
	{
		return -1;
	}

	/**
	 * Get the remote port the underlying socket connects to.
	 * @param port the port number on the remote host.
	 * @see org.jppf.comm.socket.SocketWrapper#setPort(int)
	 */
	public void setPort(int port)
	{
	}

	/**
	 * Get the underlying socket used by this socket wrapper.
	 * @return a Socket instance.
	 * @see org.jppf.comm.socket.SocketWrapper#getSocket()
	 */
	public Socket getSocket()
	{
		return null;
	}

	/**
	 * Set the underlying socket to be used by this socket wrapper.
	 * @param socket a Socket instance.
	 * @see org.jppf.comm.socket.SocketWrapper#setSocket(java.net.Socket)
	 */
	public void setSocket(Socket socket)
	{
	}

	/**
	 * Get the underlying socket used by this socket wrapper.
	 * @return a SocketChannel instance.
	 */
	public SocketChannel getChannel()
	{
		return null;
	}

	/**
	 * Set the underlying socket to be used by this socket wrapper.
	 * @param channel a SocketChannel instance.
	 */
	public void setChannel(SocketChannel channel)
	{
	}

	/**
	 * Skip <code>n</code> bytes of data from the sokcet of channel input stream.
	 * @param n the number of bytes to skip.
	 * @return the actual number of bytes skipped, or -1 if the end of sream is reached.
	 * @throws Exception if an IO error occurs.
	 * @see org.jppf.comm.socket.SocketWrapper#skip(int)
	 */
	public int skip(int n) throws Exception
	{
		if (n < 0) throw new IllegalArgumentException("number of bytes to skip must be >= 0");
		else if (n == 0) return 0;
		ByteBuffer buf = ByteBuffer.allocateDirect(n);
		while (buf.hasRemaining())
		{
			int r = inPipe.source().read(buf);
			if ((r == 0) && blocking) break;
			else if (r < 0) break;
		}
		return buf.position();
	}

	/**
	 * Send an array of bytes over a TCP socket connection.
	 * @param data the data to send.
	 * @throws Exception if the underlying output stream throws an exception.
	 * @see PipeChannelClient#write(byte[],int,int)
	 */
	public void write(byte[] data) throws Exception
	{
		write(data, 0, data.length);
	}
}
