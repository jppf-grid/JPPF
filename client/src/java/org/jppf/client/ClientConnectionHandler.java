/*
 * JPPF.
 * Copyright (C) 2005-2009 JPPF Team.
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

package org.jppf.client;

import org.jppf.client.event.*;
import org.jppf.comm.socket.SocketWrapper;

/**
 * 
 * @author Laurent Cohen
 */
public interface ClientConnectionHandler extends ClientConnectionStatusHandler
{
	/**
	 * Initialize the connection.
	 * @throws Exception if an error is raised while initializing the connection.
	 */
	void init() throws Exception;
	/**
	 * Initialize the underlying socket connection of this connection handler.
	 * @throws Exception if an error is raised during initialization.
	 */
	void initSocketClient() throws Exception;
	/**
	 * Get the socket client uses to communicate over a socket connection.
	 * @return a <code>SocketWrapper</code> instance.
	 */
	SocketWrapper getSocketClient();
	/**
	 * Close and cleanup this connection handler.
	 */
	void close();
}
