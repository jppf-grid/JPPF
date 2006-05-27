/*
 * Java Parallel Processing Framework.
 * Copyright (C) 2005-2006 Laurent Cohen.
 * lcohen@osp-chicago.com
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
package org.jppf.classloader;

import java.io.IOException;
import java.nio.channels.*;
import java.util.LinkedList;
import org.jppf.server.*;

/**
 * This class represents the state of a new class server connection, whose type is yet undetermined.
 * @author Domingos Creado
 */
class CDefiningType implements ChannelState
{
	/**
	 * The JPPFNIOServer this state relates to.
	 */
	private ClassServer server;

	/**
	 * Initialize this state with a specified JPPFNIOServer.
	 * @param server the JPPFNIOServer this state relates to.
	 */
	CDefiningType(ClassServer server)
	{
		this.server = server;
	}

	/**
	 * Get the initialization data sent over the connenction, and describing the type of the connection.
	 * @param key the selector key the underlying socket channel is associated with.
	 * @param context object encapsualting the content sent over the connection.
	 * @throws IOException if an error occurred while reading the data.
	 * @see org.jppf.server.ChannelState#exec(java.nio.channels.SelectionKey,
	 *      org.jppf.server.ChannelContext)
	 */
	public void exec(SelectionKey key, ChannelContext context) throws IOException
	{
		// we don't know yet which whom we are talking, is it a node or a provider?
		SocketChannel channel = (SocketChannel) key.channel();
		Request out = (Request) context.content;
		if (server.fillRequest(channel, out))
		{
			String name = new String(out.getOutput().toByteArray());
			if (name.startsWith("provider|"))
			{
				String uuid = name.substring("provider|".length(), name.length());
				// it is a provider
				this.server.providerConnections.put(uuid, channel);
				context.uuid = uuid;
				context.state = this.server.SendingRequest;
				// create the queue of requests to this provider
				context.content = new LinkedList<RemoteClassRequest>();
				key.interestOps(SelectionKey.OP_READ);
			}
			else if (name.equalsIgnoreCase("node"))
			{
				// it is a provider
				context.content = new Request();
				// we will wait for a request
				context.state = this.server.WaitingRequest;
				key.interestOps(SelectionKey.OP_READ);
			}
		}
	}
}