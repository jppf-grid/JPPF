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

package org.jppf.server.nio.multiplexer;

import org.jppf.comm.socket.SocketChannelClient;
import org.jppf.server.nio.*;

/**
 * Instances of this class act as wrapper for a multiplexer connection.<br>
 * They handle (re)connection services wen needed.
 * @author Laurent Cohen
 */
public class MultiplexerChannelHandler extends AbstractSocketChannelHandler
{
	/**
	 * Initialize the channel with the specified host and port.
	 * @param server the NioServer to which the channel is registred.
	 * @param host the remote host to connect to.
	 * @param port the port to connect to on the remote host.
	 */
	public MultiplexerChannelHandler(NioServer server, String host, int port)
	{
		super(server, host, port);
	}

	/**
	 * Initialize the channel client.
	 * @return a non-connected <code>SocketChannelClient</code> instance.
	 * @throws Exception if an error is raised during initialization.
	 * @see org.jppf.server.nio.AbstractSocketChannelHandler#initSocketChannel()
	 */
	protected SocketChannelClient initSocketChannel() throws Exception
	{
		return new SocketChannelClient(host, port, false);
	}

	/**
	 * This method is called after the channel is successfully conntected.
	 * @throws Exception if an error is raised while performing the initialization.
	 * @see org.jppf.server.nio.AbstractSocketChannelHandler#postInit()
	 */
	protected void postInit() throws Exception
	{
	}
}
