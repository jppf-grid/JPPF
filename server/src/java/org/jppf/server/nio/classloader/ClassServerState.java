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

package org.jppf.server.nio.classloader;

import static org.jppf.server.nio.classloader.ClassState.SENDING_NODE_RESPONSE;
import static org.jppf.utils.StringUtils.getRemoteHost;

import java.nio.channels.*;

import org.apache.commons.logging.*;
import org.jppf.server.JPPFDriver;
import org.jppf.server.nio.*;

/**
 * ABstract superclass for all possible states of a class server connection.
 * @author Laurent Cohen
 */
abstract class ClassServerState extends NioState<ClassTransition>
{
	/**
	 * Logger for this class.
	 */
	private static Log log = LogFactory.getLog(ClassServerState.class);
	/**
	 * Determines whether DEBUG logging level is enabled.
	 */
	private static boolean debugEnabled = log.isDebugEnabled();
	/**
	 * The server that handles this state.
	 */
	protected ClassNioServer server = null;
	/**
	 * Reference to the driver.
	 */
	protected JPPFDriver driver = JPPFDriver.getInstance();

	/**
	 * Initialize this state.
	 * @param server the server that handles this state.
	 */
	public ClassServerState(ClassNioServer server)
	{
		this.server = server;
	}

	/**
	 * Send a null response to a request node connection. This method is called for a provider
	 * that was disconnected but still has pending requests, so as not to block the requesting channels.
	 * @param request the selection key wrapping the requesting channel.
	 * @throws Exception if an error occurs while setting the new requester's state.
	 */
	protected void sendNullResponse(SelectionKey request) throws Exception
	{
		if (debugEnabled) log.debug("disconnected provider: sending null response to node " +
			getRemoteHost((SocketChannel) request.channel()));
		ClassContext requestContext = (ClassContext) request.attachment();
		requestContext.getResource().setDefinition(null);
		requestContext.serializeResource();
		requestContext.setState(SENDING_NODE_RESPONSE);
		server.getTransitionManager().setKeyOps(request, SelectionKey.OP_WRITE|SelectionKey.OP_READ);
	}
}
