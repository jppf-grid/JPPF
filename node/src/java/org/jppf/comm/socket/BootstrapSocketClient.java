/*
 * Java Parallel Processing Framework.
 *  Copyright (C) 2005-2009 JPPF Team. 
 * http://www.jppf.org
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * 	 http://www.apache.org/licenses/LICENSE-2.0
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
import org.jppf.serialization.JPPFObjectStreamFactory;
import org.jppf.utils.JPPFBuffer;

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
	 * @throws Exception if the underlying output stream throws an exception.
	 */
	public void send(Object o) throws Exception
	{
		ByteArrayOutputStream baos = new ByteArrayOutputStream()
		{
			public synchronized byte[] toByteArray()
			{
				return buf;
			}
		};
		ObjectOutputStream oos = JPPFObjectStreamFactory.newObjectOutputStream(baos);
		oos.writeObject(o);
		oos.flush();
		oos.close();
		// Remove references kept by the stream, otherwise leads to OutOfMemory.
		JPPFBuffer buffer = new JPPFBuffer();
		buffer.setBuffer(baos.toByteArray());
		buffer.setLength(baos.size());
		sendBytes(buffer);
	}

	/**
	 * Read an object from a TCP socket connection.
	 * This method blocks until an object is received or the specified timeout has expired, whichever happens first.
	 * @param timeout timeout after which the operation is aborted. A timeout of zero is interpreted as an infinite timeout.
	 * @return the object that was read from the underlying input stream or null if the operation timed out.
	 * @throws Exception if the underlying input stream throws an exception.
	 */
	public Object receive(int timeout) throws Exception
	{
		checkOpened();
		Object o = null;
		try
		{
			if (timeout >= 0) socket.setSoTimeout(timeout);
			JPPFBuffer buf = receiveBytes(timeout);
			ByteArrayInputStream bais = new ByteArrayInputStream(buf.getBuffer(), 0, buf.getLength());
			ObjectInputStream ois = JPPFObjectStreamFactory.newObjectInputStream(bais);
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
