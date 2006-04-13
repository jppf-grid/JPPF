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
import java.net.ConnectException;
import org.jppf.utils.*;

/**
 * Common interface for socket clients.
 * @author Laurent Cohen
 */
public interface SocketWrapper
{
	/**
	 * Size of receive buffer size for socket connections.
	 */
	int SOCKET_RECEIVE_BUFFER_SIZE = 64*1024;
	/**
	 * Send an object over a TCP socket connection.
	 * @param o the object to send.
	 * @throws IOException if the underlying output stream throws an exception.
	 */
	void send(Object o) throws IOException;

	/**
	 * Send an array of bytes over a TCP socket connection.
	 * @param buf the buffer container for the data to send.
	 * @throws IOException if the underlying output stream throws an exception.
	 */
	void sendBytes(JPPFBuffer buf) throws IOException;

	/**
	 * Read an object from a TCP socket connection.
	 * This method blocks until an object is received.
	 * @return the object that was read from the underlying input stream.
	 * @throws ClassNotFoundException if the socket connection is closed.
	 * @throws IOException if the underlying input stream throws an exception.
	 */
	Object receive() throws ClassNotFoundException, IOException;

	/**
	 * Read an object from a TCP socket connection.
	 * This method blocks until an object is received or the specified timeout has expired, whichever happens first.
	 * @param timeout timeout after which the operation is aborted. A timeout of zero is interpreted as an infinite timeout.
	 * @return the object that was read from the underlying input stream or null if the operation timed out.
	 * @throws ClassNotFoundException if the socket connection is closed.
	 * @throws IOException if the underlying input stream throws an exception.
	 */
	Object receive(int timeout) throws ClassNotFoundException, IOException;

	/**
	 * Read an object from a TCP socket connection.
	 * This method blocks until an object is received or the specified timeout has expired, whichever happens first.
	 * @param timeout timeout after which the operation is aborted. A timeout of zero is interpreted as an infinite timeout.
	 * @return an array of bytes containing the serialized object to receive.
	 * @throws IOException if the underlying input stream throws an exception.
	 */
	JPPFBuffer receiveBytes(int timeout) throws IOException;

	/**
	 * Open the underlying socket connection.
	 * @throws ConnectException if the socket fails to connect.
	 * @throws IOException if the underlying input and output streams raise an error.
	 */
	void open() throws ConnectException, IOException;

	/**
	 * Close the underlying socket connection.
	 * @throws ConnectException if the socket connection is not opened.
	 * @throws IOException if the underlying input and output streams raise an error.
	 */
	void close() throws ConnectException, IOException;

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
}