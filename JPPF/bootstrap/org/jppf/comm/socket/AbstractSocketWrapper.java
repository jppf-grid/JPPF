/*
 * Java Parallel Processing Framework.
 * Copyright (C) 2005-2006 Laurent Cohen.
 * lcohen@osp-chicago.com
 * 
 * This program is free software; you can redistribute it and/or modify it under the terms of
 * the GNU General Public License as published by the Free Software Foundation;
 * either version 2 of the License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License along with this program;
 * if not, write to the Free Software Foundation, Inc.,
 * 59 Temple Place, Suite 330,
 * Boston, MA 02111-1307 USA
 */
package org.jppf.comm.socket;

import java.io.*;
import java.net.*;
import java.net.Socket;
import org.jppf.JPPFException;
import org.jppf.utils.*;
import org.jppf.utils.JPPFBuffer;


/**
 * Common abstract superclass for all socket clients. This class is provided as a convenience and provides
 * as set of common methods to all classes implementing the
 * {@link org.jppf.comm.socket.SocketWrapper SocketWrapper} interface.
 * @author Laurent Cohen
 */
public abstract class AbstractSocketWrapper implements SocketWrapper
{
	/**
	 * The underlying socket wrapped by this SocketClient.
	 */
	protected Socket socket = null;
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
	public AbstractSocketWrapper(String host, int port, ObjectSerializer serializer)
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
	public AbstractSocketWrapper(Socket socket) throws JPPFException
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
	public void sendBytes(JPPFBuffer buf) throws IOException
	{
		try
		{
			checkOpened();
			dos.writeInt(buf.getLength());
			dos.write(buf.getBuffer(), 0, buf.getLength());
			dos.flush();
		}
		catch(IOException e)
		{
			throw e;
		}
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
	 * @return an array of bytes containing the serialized object to receive.
	 * @throws IOException if the underlying input stream throws an exception.
	 * @see org.jppf.comm.socket.SocketWrapper#receiveBytes(int)
	 */
	public JPPFBuffer receiveBytes(int timeout) throws IOException
	{
		checkOpened();
		JPPFBuffer buf = null;
		try
		{
			if (timeout > 0) socket.setSoTimeout(timeout);
			int len = dis.readInt();
			byte[] buffer = new byte[len];
			int count = 0;
			while (count < len)
			{
				int n = dis.read(buffer, count, len - count);
				if (n > 0) count += n;
				else break;
			}
			buf = new JPPFBuffer(buffer, len);
		}
		catch(IOException e)
		{
			throw e;
		}
		finally
		{
			// disable the timeout on subsequent read operations.
			if (timeout > 0) socket.setSoTimeout(0);
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
		if (!opened)
		{
			if ((host == null) || "".equals(host.trim()))
				throw new ConnectException("You must specify the host name");
			else if (port <= 0)
				throw new ConnectException("You must specify the port number");
			socket = new Socket();
			InetSocketAddress addr = new InetSocketAddress(host, port);
			int size = 32*1024;
			socket.setReceiveBufferSize(size);
			socket.connect(addr);
			initStreams();
			opened = true;
		}
		else throw new ConnectException("Client connection already opened");
	}
	
	/**
	 * Initialize all the stream used for receiving and sending objects through the
	 * underlying socket connection.
	 * @throws IOException if an error occurs during the streams initialization.
	 */
	protected void initStreams() throws IOException
	{
		OutputStream os = socket.getOutputStream();
		InputStream is = socket.getInputStream();
		BufferedOutputStream bos = new BufferedOutputStream(os, 32*1024);
		dos = new DataOutputStream(bos);
		dos.flush();
		BufferedInputStream bis = new BufferedInputStream(is);
		dis = new DataInputStream(bis); 
	}

	/**
	 * Close the underlying socket connection.
	 * @throws ConnectException if the socket connection is not opened.
	 * @throws IOException if the underlying input and output streams raise an error.
	 * @see org.jppf.comm.socket.SocketWrapper#close()
	 */
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
		if (!opened)
		{
			ConnectException e = new ConnectException("Client connection not opened");
			throw e;
		}
	}

	/**
	 * This implementation does nothing.
	 * @return null.
	 * @see org.jppf.comm.socket.SocketWrapper#getSerializer()
	 */
	public ObjectSerializer getSerializer()
	{
		return null;
	}

	/**
	 * This implementation does nothing.
	 * @param serializer not used.
	 * @see org.jppf.comm.socket.SocketWrapper#setSerializer(org.jppf.utils.ObjectSerializer)
	 */
	public void setSerializer(ObjectSerializer serializer)
	{
	}

	/**
	 * Get the remote host the underlying socket connects to.
	 * @return the host name or ip address as a string.
	 */
	public String getHost()
	{
		return host;
	}

	/**
	 * Set the remote host the underlying socket connects to.
	 * @param host the host name or ip address as a string.
	 */
	public void setHost(String host)
	{
		this.host = host;
	}

	/**
	 * Get the remote port the underlying socket connects to.
	 * @return the port number on the remote host.
	 */
	public int getPort()
	{
		return port;
	}

	/**
	 * Get the remote port the underlying socket connects to.
	 * @param port the port number on the remote host.
	 */
	public void setPort(int port)
	{
		this.port = port;
	}
}
