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
 * Client-side connection for the recovery mechanism.
 * @author Laurent Cohen
 */
public class ClientConnection extends AbstractRecoveryConnection
{
	/**
	 * Logger for this class.
	 */
	private static Logger log = LoggerFactory.getLogger(ClientConnection.class);
	/**
	 * Determines whether the debug level is enabled in the log configuration, without the cost of a method call.
	 */
	private static boolean debugEnabled = log.isDebugEnabled();
	/**
	 * Used to synchronize access to the underlying socket from multiple threads.
	 */
	private SocketInitializer socketInitializer;

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
			configure();
			if (debugEnabled) log.debug("initializing recovery client connection " + socketWrapper);
			socketInitializer = new SocketInitializerImpl();
			socketInitializer.initializeSocket(socketWrapper);
			if (!socketInitializer.isSuccessfull())
			{
				log.error("Could not initialize recovery client connection " + socketWrapper);
				close();
				return;
			}
			while (!isStopped())
			{
				String message = receiveMessage(1, 0);
				String response = "checked;" + uuid;
				sendMessage(response);
				//message = receiveMessage();
			}
		}
		catch (Exception e)
		{
			log.error(e.getMessage(), e);
			close();
		}
		if (debugEnabled) log.debug(Thread.currentThread().getName() + " stopping");
	}

	/**
	 * Configure this client connection from the JPPF properties.
	 */
	private void configure()
	{
		if (debugEnabled) log.debug("configuring connection");
		TypedProperties config = JPPFConfiguration.getProperties();
		String host = config.getString("jppf.server.host", "localhost");
		int port = config.getInt("jppf.recovery.server.port", 22222);
		maxRetries = config.getInt("jppf.recovery.max.retries", 3);
		socketReadTimeout = config.getInt("jppf.recovery.read.timeout", 6000);
		socketWrapper = new SocketClient();
		socketWrapper.setHost(host);
		socketWrapper.setPort(port);
	}

	/**
	 * Close this client and release any resources it is using.
	 */
	public void close()
	{
		setStopped(true);
		try
		{
			if (debugEnabled) log.debug("closing connection");
			SocketWrapper tmp = socketWrapper;
			socketWrapper = null;
			if (tmp != null) tmp.close();
			if (socketInitializer != null) socketInitializer.close();
			socketInitializer = null;
		}
		catch (Exception e)
		{
			log.error(e.getMessage(), e);
		}
	}
}
