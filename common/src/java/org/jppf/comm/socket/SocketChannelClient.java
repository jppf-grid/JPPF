/*
 * Java Parallel Processing Framework.
 * Copyright (C) 2005-2006 Laurent Cohen.
 * lcohen@osp-chicago.com
 *
 * This library is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as published by the
 * Free Software Foundation; either version 2.1 of the License, or (at your
 * option) any later version.
 *
 * This library is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this library; if not, write to the Free Software Foundation,
 * Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA
 */
package org.jppf.comm.socket;

import java.io.IOException;
import java.net.*;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import org.apache.log4j.Logger;
import org.jppf.utils.*;

/**
 * This SocketWrapper implementation relies on an underlying SocketChannel, in order to allow
 * writing to, and reading from, at the same time from the same socket connection. 
 * @author Laurent Cohen
 */
public class SocketChannelClient implements SocketWrapper
{
	/**
	 * Log4j logger for this class.
	 */
	private static Logger log = Logger.getLogger(SocketChannelClient.class);
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
	 * Determiens whether this client is opened or not.
	 */
	private boolean opened = false;

	/**
	 * Initialize this socket channel client.
	 * @throws IOException if the socket channel could not be opened.
	 */
	public SocketChannelClient() throws IOException
	{
		channel = SocketChannel.open();
		channel.configureBlocking(true);
	}

	/**
	 * Initialize this socket channel client with a specified host and port.
	 * @param host the host to connect to.
	 * @param port the port to listen on the host.
	 * @throws IOException if the socket channel could not be opened.
	 */
	public SocketChannelClient(String host, int port) throws IOException
	{
		this();
		this.host = host;
		this.port = port;
	}

	/**
	 * Send an object over a TCP socket connection.
	 * @param o the object to send.
	 * @throws IOException if the underlying output stream throws an exception.
	 * @see org.jppf.comm.socket.SocketWrapper#send(java.lang.Object)
	 */
	public void send(Object o) throws IOException
	{
		JPPFBuffer buf = getSerializer().serialize(o);
		sendBytes(buf);
	}

	/**
	 * Send an array of bytes over a TCP socket connection.
	 * @param buf the buffer container for the data to send.
	 * @throws IOException if the underlying output stream throws an exception.
	 * @see org.jppf.comm.socket.SocketWrapper#sendBytes(org.jppf.utils.JPPFBuffer)
	 */
	public void sendBytes(JPPFBuffer buf) throws IOException
	{
		ByteBuffer buffer = ByteBuffer.allocate(4);
		int length = buf.getLength();
		buffer.putInt(length);
		buffer.rewind();
		int count = 0;
		while (count < 4) count += channel.write(buffer);
		count = 0;
		buffer = ByteBuffer.wrap(buf.getBuffer(), 0, length);
		while (count < length) count += channel.write(buffer);
	}

	/**
	 * Read an object from a TCP socket connection.
	 * This method blocks until an object is received.
	 * @return the object that was read from the underlying input stream.
	 * @throws ClassNotFoundException if the socket connection is closed.
	 * @throws IOException if the underlying input stream throws an exception.
	 * @see org.jppf.comm.socket.SocketWrapper#receive()
	 */
	public Object receive() throws ClassNotFoundException, IOException
	{
		return receive(0);
	}

	/**
	 * Read an object from a TCP socket connection.
	 * This method blocks until an object is received or the specified timeout has expired, whichever happens first.
	 * @param timeout timeout after which the operation is aborted. A timeout of zero is interpreted as an infinite timeout.
	 * @return the object that was read from the underlying input stream or null if the operation timed out.
	 * @throws ClassNotFoundException if the socket connection is closed.
	 * @throws IOException if the underlying input stream throws an exception.
	 * @see org.jppf.comm.socket.SocketWrapper#receive(int)
	 */
	public Object receive(int timeout) throws ClassNotFoundException, IOException
	{
		Object o = null;
		try
		{
			if (timeout >= 0) channel.socket().setSoTimeout(timeout);
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
		finally
		{
			// disable the timeout on subsequent read operations.
			channel.socket().setSoTimeout(0);
		}
		return o;
	}

	/**
	 * Read an object from a TCP socket connection.
	 * This method blocks until an object is received or the specified timeout has expired, whichever happens first.
	 * @param timeout timeout after which the operation is aborted. A timeout of zero is interpreted as an infinite timeout.
	 * @return an array of bytes containing the serialized object to receive.
	 * @throws IOException if the underlying input stream throws an exception.
	 * @see org.jppf.comm.socket.SocketWrapper#receiveBytes(int)
	 */
	public JPPFBuffer receiveBytes(int timeout) throws IOException
	{
		ByteBuffer byteBuffer = ByteBuffer.allocate(4);
		int count = 0;
		while (count < 4)
		{
			count += channel.read(byteBuffer);
		}
		byteBuffer.rewind();
		int length = byteBuffer.getInt();
		JPPFBuffer buf = new JPPFBuffer(new byte[length], length);
		byteBuffer = ByteBuffer.wrap(buf.getBuffer());
		count = 0;
		while (count < length)
		{
			int n = channel.read(byteBuffer);
			count += n;
		}
		return buf;
	}

	/**
	 * Open the underlying socket connection.
	 * @throws ConnectException if the socket fails to connect.
	 * @throws IOException if the underlying input and output streams raise an error.
	 * @see org.jppf.comm.socket.SocketWrapper#open()
	 */
	public void open() throws ConnectException, IOException
	{
		InetSocketAddress address = new InetSocketAddress(host, port);
		channel.connect(address);
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
			channel.close();
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
		return host;
	}

	/**
	 * Set the remote host the underlying socket connects to.
	 * @param host the host name or ip address as a string.
	 * @see org.jppf.comm.socket.SocketWrapper#setHost(java.lang.String)
	 */
	public void setHost(String host)
	{
		this.host = host;
	}

	/**
	 * Get the remote port the underlying socket connects to.
	 * @return the port number on the remote host.
	 * @see org.jppf.comm.socket.SocketWrapper#getPort()
	 */
	public int getPort()
	{
		return port;
	}

	/**
	 * Get the remote port the underlying socket connects to.
	 * @param port the port number on the remote host.
	 * @see org.jppf.comm.socket.SocketWrapper#setPort(int)
	 */
	public void setPort(int port)
	{
		this.port = port;
	}
}
