/*
 * JPPF.
 * Copyright (C) 2005-2011 JPPF Team.
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
package org.jppf.jca.work;

import static org.jppf.client.JPPFClientConnectionStatus.DISCONNECTED;

import org.jppf.client.JPPFClientConnection;
import org.slf4j.*;

/**
 * Wrapper class for the initialization of a client connection.
 */
public class ConnectionInitializerTask implements Runnable
{
	/**
	 * Logger for this class.
	 */
	private static Logger log = LoggerFactory.getLogger(ConnectionInitializerTask.class);
	/**
	 * The client connection to initialize.
	 */
	private JPPFClientConnection c = null;
	/**
	 * Instantiate this connection initializer with the specified client connection.
	 * @param c the client connection to initialize.
	 */
	public ConnectionInitializerTask(JPPFClientConnection c)
	{
		this.c = c;
	}

	/**
	 * Perform the initialization of a client connection.
	 * @see java.lang.Runnable#run()
	 */
	@Override
    public void run()
	{
		//if (debugEnabled) log.debug("initializing driver connection '"+c+"'");
		if (c.getStatus().equals(DISCONNECTED)) c.init();
	}
}
