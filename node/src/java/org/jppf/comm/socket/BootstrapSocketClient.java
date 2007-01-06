/*
 * Java Parallel Processing Framework.
 * Copyright (C) 2005-2007 JPPF Team.
 * http://www.jppf.org
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

import java.io.*;
import java.net.*;
import org.jppf.JPPFException;
import org.jppf.utils.*;

/**
 * This class provides a simple API to transfer objects over a TCP socket connection.
 * @author Laurent Cohen
 */
public class BootstrapSocketClient extends AbstractSocketWrapper
{
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
	public BootstrapSocketClient()
	{
		super();
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
		super(host, port, null);
	}

	/**
	 * Initialize this socket client with an already opened and connected socket.
	 * @param socket the underlying socket this socket client wraps around.
	 * @throws JPPFException if the socket connection fails.
	 */
	public BootstrapSocketClient(Socket socket) throws JPPFException
	{
		super(socket);
	}

	/**
	 * Send an object over a TCP socket connection.
	 * @param o the object to send.
	 * @throws IOException if the underlying output stream throws an exception.
	 */
	public void send(Object o) throws IOException
	{
		ByteArrayOutputStream baos = new ByteArrayOutputStream()
		{
			public synchronized byte[] toByteArray()
			{
				return buf;
			}
		};
		ObjectOutputStream oos = new ObjectOutputStream(baos);
		oos.writeObject(o);
		oos.flush();
		// Remove references kept by the stream, otherwise leads to OutOfMemory.
		JPPFBuffer buffer = new JPPFBuffer();
		buffer.setBuffer(baos.toByteArray());
		buffer.setLength(baos.size());
		oos.close();
		sendBytes(buffer);
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
			ByteArrayInputStream bais = new ByteArrayInputStream(buf.getBuffer(), 0, buf.getLength());
			ObjectInputStream ois = new ObjectInputStream(bais);
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
}
