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

import static org.jppf.server.nio.classloader.ChannelTransition.*;
import static org.jppf.utils.StringUtils.getRemostHost;

import java.nio.channels.*;

import org.apache.log4j.Logger;
import org.jppf.node.JPPFResourceWrapper;
import org.jppf.server.JPPFDriver;
import org.jppf.utils.TraversalList;

/**
 * This class represents the state of waiting for a request from a node.
 * @author Laurent Cohen
 */
public class WaitingNodeRequestState extends ClassServerState
{
	/**
	 * Log4j logger for this class.
	 */
	private static Logger log = Logger.getLogger(WaitingNodeRequestState.class);
	/**
	 * Determines whether DEBUG logging level is enabled.
	 */
	private static boolean debugEnabled = log.isDebugEnabled();

	/**
	 * Initialize this state with a specified NioServer.
	 * @param server the JPPFNIOServer this state relates to.
	 */
	public WaitingNodeRequestState(ClassNioServer server)
	{
		super(server);
	}

	/**
	 * Execute the action associated with this channel state.
	 * @param key the selection key corresponding to the channel and selector for this state.
	 * @return a state transition as an <code>NioTransition</code> instance.
	 * @throws Exception if an error occurs while transitioning to another state.
	 * @see org.jppf.server.nio.NioState#performTransition(java.nio.channels.SelectionKey)
	 */
	public ChannelTransition performTransition(SelectionKey key) throws Exception
	{
		SocketChannel channel = (SocketChannel) key.channel();
		ClassContext context = (ClassContext) key.attachment();
		if (context.readMessage(channel))
		{
			if (debugEnabled) log.debug("read resource request from node: " + getRemostHost(channel));
			JPPFResourceWrapper resource = context.deserializeResource();
			TraversalList<String> uuidPath = resource.getUuidPath();
			boolean dynamic = resource.isDynamic();
			String name = resource.getName();
			String uuid = (uuidPath.size() > 0) ? uuidPath.getCurrentElement() : null; 
			byte[] b = null;
			if ((uuid == null) || uuid.equals(JPPFDriver.getInstance().getUuid()))
			{
				b = server.getResourceProvider().getResourceAsBytes(name);
				if ((b != null) || !dynamic)
				{
					if (debugEnabled)
					{
						log.debug("resource " + (b == null ? " not " : "") + "found [" + name +
							"] in the driver's classpath for node: " + getRemostHost(channel));
					}
					resource.setDefinition(b);
					context.serializeResource();
					return TO_SENDING_NODE_RESPONSE;
				}
			}
			if ((b == null) && dynamic)
			{
				CacheClassContent content = server.classCache.get(new CacheClassKey(uuidPath.getFirst(), name));
				if (content != null)
				{
					if (debugEnabled) log.debug("found cached resource [" + name + "] for node: " + getRemostHost(channel));
					resource.setDefinition(content.getContent());
					context.serializeResource();
					return TO_SENDING_NODE_RESPONSE;
				}
				else
				{
					uuidPath.decPosition();
					uuid = uuidPath.getCurrentElement();
					SocketChannel provider = server.providerConnections.get(uuid);
					if (provider != null)
					{
						if (debugEnabled) log.debug("request resource [" + name + "] from client: " +
								getRemostHost(provider) + " for node: " + getRemostHost(channel));
						SelectionKey providerKey = provider.keyFor(server.getSelector());
						ClassContext providerContext = (ClassContext) providerKey.attachment();
						providerContext.addRequest(key);
						if (providerContext.getCurrentRequest() == null)
							providerKey.interestOps(SelectionKey.OP_READ|SelectionKey.OP_WRITE);
						return TO_IDLE_NODE;
					}
				}
			}
			if (debugEnabled) log.debug("resource [" + name + "] not found for node: " + getRemostHost(channel));
			return TO_SENDING_NODE_RESPONSE;
		}
		return TO_WAITING_NODE_REQUEST;
	}
}
