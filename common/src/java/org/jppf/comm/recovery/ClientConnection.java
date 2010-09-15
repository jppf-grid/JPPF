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

import org.jppf.comm.socket.*;
import org.jppf.utils.*;
import org.slf4j.*;

/**
 * 
 * @author Laurent Cohen
 */
public class ClientConnection extends ThreadSynchronization implements Runnable
{
	/**
	 * Logger for this class.
	 */
	private static Logger log = LoggerFactory.getLogger(ClientConnection.class);
	/**
	 * Determines whether the debug level is enabled in the log configuration, without the cost of a method call.
	 */
	private boolean debugEnabled = log.isDebugEnabled();
	/**
	 * Connection to a client.
	 */
	private SocketWrapper socketWrapper = null;
	/**
	 * Used to synchronize access to the underlying socket from multiple threads.
	 */
	private SocketInitializer socketInitializer = new SocketInitializerImpl();
	/**
	 * The JPPF node or client uuid.
	 */
	private String uuid = null;

	/**
	 * Initialize this cliet connection with the specified uuid.
	 * @param uuid the JPPF node or client uuid.
	 */
	public ClientConnection(String uuid)
	{
		this.uuid = uuid;
	}

	/**
	 * {@inheritDoc}
	 */
	public void run()
	{
		try
		{
			TypedProperties config = JPPFConfiguration.getProperties();
			String host = config.getString("jppf.server.host", "localhost");
			int port = config.getInt("jppf.recovery.server.port", 22222);
			//socketWrapper = new SocketClient(host, port);
			socketWrapper = new SocketClient();
			socketWrapper.setHost(host);
			socketWrapper.setPort(port);
			if (debugEnabled) log.debug("initializing " + socketWrapper);
			socketInitializer.initializeSocket(socketWrapper);
			if (!socketInitializer.isSuccessfull())
			{
				log.error("Could not initialize reaper connection " + socketWrapper);
				socketInitializer.close();
				return;
			}
			while (!isStopped())
			{
				JPPFBuffer buffer = socketWrapper.receiveBytes(0);
				String message = new String(buffer.buffer);
				if (debugEnabled) log.debug("received '" + message + "'");
				String response = "checked;" + uuid;
				buffer = new JPPFBuffer(response);
				socketWrapper.sendBytes(buffer);
				if (debugEnabled) log.debug("sent '" + response + "'");
			}
		}
		catch (Exception e)
		{
			log.error(e.getMessage(), e);
		}
	}

	/**
	 * Close this client and release any resources it is using.
	 */
	public void close()
	{
		try
		{
			setStopped(true);
			socketWrapper.close();
		}
		catch (Exception e)
		{
			log.error(e.getMessage(), e);
		}
	}

	/**
	 * Main entry point.
	 * @param args not used.
	 */
	public static void main(String[] args)
	{
		try
		{
			new ClientConnection("jppf").run();
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
	}
}
