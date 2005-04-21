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
import org.apache.log4j.Logger;
import org.jppf.utils.PropertyManager;

/**
 * This class provides a simple API to transfer objects over a TCP socket connection.
 * @author Laurent Cohen
 */
public class SocketClient
{
	private static Logger log = Logger.getLogger(SocketClient.class);

	/**
	 * The underlying socket wrapped by this SocketClient.
	 */
	private Socket socket = null;
	/**
	 * Object stream used to send objects. 
	 */
	private ObjectOutputStream oos = null;
	private OutputStream os = null;
	private BufferedOutputStream bos = null;
	/**
	 * Object stream used to receive objects. 
	 */
	private ObjectInputStream ois = null;
	private InputStream is = null;
	private BufferedInputStream bis = null;

	/**
	 * The host the socket connects to.
	 */
	private String host = null;
	/**
	 * The port number on the host the socket connects to.
	 */
	private int port = -1;
	/**
	 * Flag indicating the opened state of the underlying socket.
	 */
	private boolean opened = false;

	/**
	 * Default constructor is invisible to other classes.
	 * See {@link org.jppf.comm.socket.SocketClient#SocketClient(java.lang.String, int) SocketClient(String, int)} and
	 * {@link org.jppf.comm.socket.SocketClient#SocketClient(java.net.Socket) SocketClient(Socket)} for ways to
	 * instanciate a SocketClient. 
	 */
	private SocketClient()
	{
	}
	
	/**
	 * Initialize this socket client and connect it to the specified host on the specified port.
	 * @param host the remote host this socket client connects to.
	 * @param port the remote port on the host this socket client connects to.
	 * @throws ConnectException if the connection fails.
	 * @throws IOException if there is an issue with the socket streams..
	 */
	public SocketClient(String host, int port) throws ConnectException, IOException
	{
		this.host = host;
		this.port = port;
		open();
		//socket.setTcpNoDelay(true);
	}
	
	/**
	 * Initialize this socket client with an already opened and connected socket.
	 * @param socket the underlying socket this socket client wraps around.
	 * @throws Exception if the socket connection fails.
	 */
	public SocketClient(Socket socket) throws Exception
	{
		this.host = socket.getInetAddress().getHostName();
		this.port = socket.getPort();
		this.socket = socket;
		//socket.setTcpNoDelay(true);
		initStreams();
		opened = true;
	}
	
	/**
	 * Send an object over a TCP socket connection.
	 * @param o the object to send.
	 * @throws ConnectException is the socket connection is not opened.
	 * @throws IOException if the underlying output stream throws an exception.
	 */
	public void send(Object o) throws ConnectException, IOException
	{
		checkOpened();
		oos.writeObject(o);
		oos.flush();
		// Remove references kept by the stream, otherwise leads to OutOfMemory.
		oos.reset();
	}
	
	/**
	 * Read an object from a TCP socket connection.
	 * This method blocks until an object is received.
	 * @return the object that was read from the underlying input stream.
	 * @throws ConnectException if the socket connection is closed.
	 * @throws IOException if the underlying input stream throws an exception.
	 * @throws ClassNotFoundException if the class of the object that was read cannot be loaded.
	 */
	public Object receive() throws ConnectException, IOException, ClassNotFoundException
	{
		return receive(0);
	}

	/**
	 * Read an object from a TCP socket connection.
	 * This method blocks until an object is received or the specified timeout has expired, whichever happens first.
	 * @param timeout timeout after which the operation is aborted. A timeout of zero is interpreted as an infinite timeout.
	 * @return the object that was read from the underlying input stream or null if the operation timed out.
	 * @throws ConnectException if the socket connection is closed.
	 * @throws IOException if the underlying input stream throws an exception.
	 * @throws ClassNotFoundException if the class of the object that was read cannot be loaded.
	 */
	public Object receive(int timeout) throws ConnectException, IOException, ClassNotFoundException
	{
		checkOpened(); 
		Object o = null;
		try
		{
			if (timeout >= 0) socket.setSoTimeout(timeout);
			o = ois.readObject();
		}
		catch(Exception e)
		{
			log.error(e.getMessage(), e);
			throw new IOException(e.getMessage());
		}
		finally
		{
			// disable the timeout on subsequent read operations.
			socket.setSoTimeout(0);
		}
		return o;
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
			int size = PropertyManager.getInt("test", "receive.buffer.size", 1024*1024);
			socket.setReceiveBufferSize(size);
			socket.connect(addr);
			//socket = new Socket(host, port);
			initStreams();
			opened = true;
		}
		else throw new ConnectException("Client connection already opened");
	}
	
	private void initStreams() throws IOException
	{
		os = socket.getOutputStream();
		is = socket.getInputStream();
		int size = 32*1024;
		bos = new BufferedOutputStream(os, size);
		oos = new ObjectOutputStream(bos);
		oos.flush();
		bis = new BufferedInputStream(is);
		ois = new ObjectInputStream(bis); 
	}

	/**
	 * Close the underlying socket connection.
	 * @throws ConnectException if the socket connection is not opened.
	 * @throws IOException if the underlying input and output streams raise an error.
	 */
	public void close() throws ConnectException, IOException
	{
		checkOpened();
		socket.close();
		opened = false;
	}
	
	/**
	 * Check whether the underlying socket is opened or not.
	 * @throws ConnectException if the connection is not opened.
	 */
	private void checkOpened() throws ConnectException
	{
		if (!opened)
			throw new ConnectException("Client connection not opened"); 
	}
}
