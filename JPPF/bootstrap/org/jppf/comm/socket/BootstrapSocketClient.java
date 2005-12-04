/*
 * Java Parallel Processing Framework.
 * Copyright (C) 2005 Laurent Cohen.
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
import org.jppf.classloader.JPPFBootstrapException;
import org.jppf.utils.JPPFBuffer;

/**
 * This class provides a simple API to transfer objects over a TCP socket connection.
 * @author Laurent Cohen
 */
public class BootstrapSocketClient
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
	 * Buffer used to receive data from the socket connection.
	 */
	protected JPPFBuffer jppfBuffer = new JPPFBuffer();

	/**
	 * Default constructor is invisible to other classes.
	 * See {@link org.jppf.comm.socket.BootstrapSocketClient#BootstrapSocketClient(java.lang.String, int) BootstrapSocketClient(String, int)} and
	 * {@link org.jppf.comm.socket.BootstrapSocketClient#BootstrapSocketClient(java.net.Socket) BootstrapSocketClient(Socket)} for ways to
	 * instanciate a SocketClient. 
	 */
	protected BootstrapSocketClient()
	{
	}
	
	/**
	 * Initialize this socket client and connect it to the specified host on the specified port.
	 * @param host the remote host this socket client connects to.
	 * @param port the remote port on the host this socket client connects to.
	 * @throws ConnectException if the connection fails.
	 * @throws IOException if there is an issue with the socket streams.
	 */
	public BootstrapSocketClient(String host, int port) throws ConnectException, IOException
	{
		this.host = host;
		this.port = port;
		open();
	}
	
	/**
	 * Initialize this socket client with an already opened and connected socket.
	 * @param socket the underlying socket this socket client wraps around.
	 * @throws JPPFBootstrapException if the socket connection fails.
	 */
	public BootstrapSocketClient(Socket socket) throws JPPFBootstrapException
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
			throw new JPPFBootstrapException(ioe.getMessage(), ioe);
		}
	}
	
	/**
	 * Send an object over a TCP socket connection.
	 * @param o the object to send.
	 * @throws IOException if the underlying output stream throws an exception.
	 */
	public void send(Object o) throws IOException
	{
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		ObjectOutputStream oos = new ObjectOutputStream(baos);
		oos.writeObject(o);
		oos.flush();
		// Remove references kept by the stream, otherwise leads to OutOfMemory.
		JPPFBuffer buf = new JPPFBuffer();
		buf.setBuffer(baos.toByteArray());
		buf.setLength(baos.size());
		oos.close();
		sendBytes(buf);
	}
	
	/**
	 * Send an object over a TCP socket connection.
	 * @param buf the buffer container for the data to send.
	 * @throws IOException if the underlying output stream throws an exception.
	 */
	public void sendBytes(JPPFBuffer buf) throws IOException
	{
		checkOpened();
		dos.writeInt(buf.getLength());
		dos.write(buf.getBuffer(), 0, buf.getLength());
		dos.flush();
	}
	
	/**
	 * Read an object from a TCP socket connection.
	 * This method blocks until an object is received.
	 * @return the object that was read from the underlying input stream.
	 * @throws ClassNotFoundException if the socket connection is closed.
	 * @throws IOException if the underlying input stream throws an exception.
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
	 */
	public Object receive(int timeout) throws ClassNotFoundException, IOException
	{
		checkOpened(); 
		Object o = null;
		try
		{
			if (timeout >= 0) socket.setSoTimeout(timeout);
			JPPFBuffer buf = receiveBytes(timeout);
			ObjectInputStream ois = new ObjectInputStream(new ByteArrayInputStream(buf.getBuffer(), 0, buf.getLength()));
			o = ois.readObject();
			ois.close();
		}
		finally
		{
			// disable the timeout on subsequent read operations.
			socket.setSoTimeout(0);
		}
		return o;
	}

	/**
	 * Read an object from a TCP socket connection.
	 * This method blocks until an object is received or the specified timeout has expired, whichever happens first.
	 * @param timeout timeout after which the operation is aborted. A timeout of zero is interpreted as an infinite timeout.
	 * @return an array of bytes containing the serialized object to receive.
	 * @throws IOException if the underlying input stream throws an exception.
	 */
	public JPPFBuffer receiveBytes(int timeout) throws IOException
	{
		checkOpened();
		try
		{
			if (timeout >= 0) socket.setSoTimeout(timeout);
			jppfBuffer.setLength(dis.readInt());
			if (jppfBuffer.getLength() > jppfBuffer.getBuffer().length) jppfBuffer.setBuffer(new byte[jppfBuffer.getLength()]);
			int count = 0;
			while (count < jppfBuffer.getLength())
			{
				int n = dis.read(jppfBuffer.getBuffer(), count, jppfBuffer.getLength() - count);
				if (n >= 0) count += n;
				else break;
			}
		}
		finally
		{
			// disable the timeout on subsequent read operations.
			socket.setSoTimeout(0);
		}
		return jppfBuffer;
	}

	/**
	 * Open the underlying socket connection.
	 * @throws ConnectException if the socket fails to connect.
	 * @throws IOException if the underlying input and output streams raise an error.
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
	 */
	public void close() throws ConnectException, IOException
	{
		opened = false;
		socket.close();
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
	 * Determine whether this socket client is opened or not.
	 * @return true if this client is opened, false otherwise.
	 */
	public boolean isOpened()
	{
		return opened;
	}
}
