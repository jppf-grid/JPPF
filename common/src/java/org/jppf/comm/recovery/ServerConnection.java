/*
 * JPPF.
 * Copyright (C) 2005-2010 JPPF Team.
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

package org.jppf.comm.recovery;

import java.net.*;

import org.jppf.comm.socket.*;
import org.jppf.utils.*;
import org.slf4j.*;

/**
 * Instances of this class represent connections to a server from a remote peer.
 * <p>They are used to send, at regular intervals, a request to the distant peer and check
 * if the expected response is received. If the response is incorrect or not received at all,
 * then the connection is considered broken and closed on the server side.
 * <p>The main goal is to detect network connections broken due to hardware failures on
 * tthe remote peer side, which cannot be detected otherwise.
 * @author Laurent Cohen
 */
public class ServerConnection implements Runnable
{
	/**
	 * Logger for this class.
	 */
	private static Logger log = LoggerFactory.getLogger(ServerConnection.class);
	/**
	 * Determines whether the debug level is enabled in the log configuration, without the cost of a method call.
	 */
	private boolean debugEnabled = log.isDebugEnabled();
	/**
	 * Connection to a client.
	 */
	private SocketWrapper socketWrapper = null;
	/**
	 * Maximum number of failed write/read attempts on a connection before the remote peer is considered dead.
	 */
	private int maxRetries = 3;
	/**
	 * Maximum wait time on a response from the remote peer.
	 */
	private int socketReadTimeout = 6000;
	/**
	 * Determines whether this connection is ok after is has been checked.
	 */
	private boolean ok = true;
	/**
	 * Determines whether the initial handshake has been performed.
	 */
	private boolean initialized = false;
	/**
	 * The uuid for the remote peer, sent by the peer during handshake.
	 * It is used to correlate this server connection with a corresponding channel
	 * handled by the {@link org.jppf.server.nio.nodeserver.NodeNioServer}, so the node connection can be closed when this
	 * connection is considered broken.
	 */
	private String uuid = null;

	/**
	 * Initialize this connection with the specified socket.
	 * @param socket the socket connected to a client.
	 * @param maxRetries the maximum number of failed write/read attempts on a connection before the remote peer is considered dead.
	 * @param socketReadTimeout the maximum wait time on a response from the remote peer.
	 * @throws Exception if any error occurs while initializing the socket connection.
	 */
	public ServerConnection(Socket socket, int maxRetries, int socketReadTimeout) throws Exception
	{
		this.maxRetries = maxRetries;
		this.socketReadTimeout = socketReadTimeout;
		socketWrapper = new SocketClient(socket);
	}

	/**
	 * {@inheritDoc}
	 */
	public void run()
	{
		if (!initialized) performHandshake();
		else performCheck();
	}

	/**
	 * Perform the initial handshake with the remote peer.
	 */
	private void performHandshake()
	{
		String response = doRequestResponse("handshake");
		if (!isOk()) return;
		int idx = response.indexOf(';');
		if (idx < 0)
		{
			setOk(false);
			return;
		}
		uuid = response.substring(idx + 1);
	}

	/**
	 * Perform the initial handshake with the remote peer.
	 */
	private void performCheck()
	{
		String response = doRequestResponse("check");
	}

	/**
	 * Send a string to the remote peer and receive a string back.
	 * If any exception occurs while sending or receiving, the connection is considered broken.
	 * While receiving the response, this method also waits for {@link #socketReadTimeout} specified
	 * in the constructor. If the timeout expires {@link #maxRetries} times in a row, the connection
	 * is also considered broken.
	 * @param message the string message to send to the remote peer.
	 * @return the response as a string.
	 */
	private String doRequestResponse(String message)
	{
		String response = null;
		try
		{
			JPPFBuffer buffer = new JPPFBuffer(message);
			socketWrapper.sendBytes(buffer);
			if (debugEnabled) log.debug("sent '" + message + "'");
			int retries = 0;
			boolean success = false;
			while ((retries < maxRetries) && !success)
			{
				try
				{
					buffer = socketWrapper.receiveBytes(socketReadTimeout);
					success = true;
					response = buffer.asString();
					if (debugEnabled) log.debug("received '" + response + "'");
				}
				catch (SocketTimeoutException e)
				{
					retries++;
					if (debugEnabled) log.debug("retry #" + retries + " failed! (" + this + ")");
				}
			}
			if (!success) setOk(false);
		}
		catch (Exception e)
		{
			setOk(false);
			if (debugEnabled) log.debug("error closing the recovery server socket", e);
		}
		return response;
	}

	/**
	 * Close this server connection and release the resources it is using.
	 */
	public void close()
	{
		try
		{
			socketWrapper.close();
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
	}

	/**
	 * Determine whether this connection is ok after is has been checked.
	 * @return true if the connection is ok, false otherwise.
	 */
	public synchronized boolean isOk()
	{
		return ok;
	}

	/**
	 * Specifiy whether this connection is ok after is has been checked.
	 * @param ok true if the connection is ok, false otherwise.
	 */
	public synchronized void setOk(boolean ok)
	{
		this.ok = ok;
	}

	/**
	 * Determine whether the initial handshake has been performed.
	 * @return <code>true</code> if the initial handshake was done, <code>false</code> otherwise.
	 */
	public synchronized boolean isInitialized()
	{
		return initialized;
	}

	/**
	 * Specify whether the initial handshake has been performed.
	 * @param initialized <code>true</code> if the initial handshake was done, <code>false</code> otherwise.
	 */
	public synchronized void setInitialized(boolean initialized)
	{
		this.initialized = initialized;
	}

	/**
	 * Get the uuid of the remote peer.
	 * @return the uuid as a string.
	 */
	public String getUuid()
	{
		return uuid;
	}

	/**
	 * {@inheritDoc}
	 */
	public String toString()
	{
		return StringUtils.buildString("ServerConnection[socketWrapper=", socketWrapper, ", ok=", ok, ", initialized=", initialized, ", uuid=", uuid, "]");
	}
}
