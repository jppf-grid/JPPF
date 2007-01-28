/*
 * Java Parallel Processing Framework.
 * Copyright (C) 2005-2007 JPPF Team.
 * http://www.jppf.org
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

package org.jppf.server.nio.classloader;

import static org.jppf.server.nio.classloader.ClassState.SENDING_NODE_RESPONSE;
import static org.jppf.utils.StringUtils.getRemoteHost;

import java.nio.channels.*;

import org.apache.log4j.Logger;
import org.jppf.server.nio.*;

/**
 * 
 * @author Laurent Cohen
 */
public abstract class ClassServerState extends NioState<ClassTransition>
{
	/**
	 * Log4j logger for this class.
	 */
	private static Logger log = Logger.getLogger(ClassServerState.class);
	/**
	 * Determines whether DEBUG logging level is enabled.
	 */
	private static boolean debugEnabled = log.isDebugEnabled();
	/**
	 * The server that handles this state.
	 */
	protected ClassNioServer server = null;

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
		server.setKeyOps(request, SelectionKey.OP_WRITE|SelectionKey.OP_READ);
	}
}
