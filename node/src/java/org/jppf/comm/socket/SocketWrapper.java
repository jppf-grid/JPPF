/*
 * Java Parallel Processing Framework.
 * Copyright (C) 2005-2008 JPPF Team.
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

import java.net.Socket;

import org.jppf.utils.*;

/**
 * This interface is common to all classes wrapping an underlying socket connections.<br>
 * The underlying socket API can be either based on a socket channel (blocking or non-blocking),
 * or just based on a plain socket.
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
	 * @throws Exception if the underlying output stream throws an exception.
	 */
	void send(Object o) throws Exception;

	/**
	 * Send an array of bytes over a TCP socket connection.
	 * @param buf the buffer container for the data to send.
	 * @throws Exception if the underlying output stream throws an exception.
	 */
	void sendBytes(JPPFBuffer buf) throws Exception;

	/**
	 * Send an array of bytes over a TCP socket connection.
	 * @param data the data to send.
	 * @throws Exception if the underlying output stream throws an exception.
	 */
	void write(byte[] data) throws Exception;

	/**
	 * Read an object from a TCP socket connection.
	 * This method blocks until an object is received.
	 * @return the object that was read from the underlying input stream.
	 * @throws Exception if the underlying input stream throws an exception.
	 */
	Object receive() throws Exception;

	/**
	 * Read an object from a TCP socket connection.
	 * This method blocks until an object is received or the specified timeout has expired, whichever happens first.
	 * @param timeout timeout after which the operation is aborted. A timeout of zero is interpreted as an infinite timeout.
	 * @return the object that was read from the underlying input stream or null if the operation timed out.
	 * @throws Exception if the underlying input stream throws an exception.
	 */
	Object receive(int timeout) throws Exception;

	/**
	 * Read an object from a TCP socket connection.
	 * This method blocks until an object is received or the specified timeout has expired, whichever happens first.
	 * @param timeout timeout after which the operation is aborted. A timeout of zero is interpreted as an infinite timeout.
	 * @return an array of bytes containing the serialized object to receive.
	 * @throws Exception if the underlying input stream throws an exception.
	 */
	JPPFBuffer receiveBytes(int timeout) throws Exception;
	/**
	 * Skip <code>n</code> bytes of data from the sokcet of channel input stream.
	 * @param n the number of bytes to skip.
	 * @return the actual number of bytes skipped.
	 * @throws Exception if an IO error occurs.
	 */
	int skip(int n) throws Exception;

	/**
	 * Open the underlying socket connection.
	 * @throws Exception if the underlying input and output streams raise an error.
	 */
	void open() throws Exception;

	/**
	 * Close the underlying socket connection.
	 * @throws Exception if the underlying input and output streams raise an error.
	 */
	void close() throws Exception;

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
	
	/**
	 * Get the underlying socket used by this socket wrapper.
	 * @return a Socket instance.
	 */
	Socket getSocket();
	
	/**
	 * Set the underlying socket to be used by this socket wrapper.
	 * @param socket a Socket instance.
	 */
	void setSocket(Socket socket);
}
