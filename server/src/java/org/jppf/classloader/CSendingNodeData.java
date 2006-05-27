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
import java.nio.ByteBuffer;
import java.nio.channels.*;
import org.jppf.server.*;

/**
 * This class represents the state of sending a answer to nodes.
 * @author Domingos Creado
 */
class CSendingNodeData implements ChannelState
{
	/**
	 * The JPPFNIOServer this state relates to.
	 */
	private ClassServer server;

	/**
	 * Initialize this state with a specified JPPFNIOServer.
	 * @param server the JPPFNIOServer this state relates to.
	 */
	CSendingNodeData(ClassServer server)
	{
		this.server = server;
	}

	/**
	 * Send resource data to a node.
	 * @param key the selector key the underlying socket channel is associated with.
	 * @param context object encapsulating the content to send over the connection.
	 * @throws IOException if an error occurred while sending the data.
	 * @see org.jppf.server.ChannelState#exec(java.nio.channels.SelectionKey,
	 *      org.jppf.server.ChannelContext)
	 */
	public void exec(SelectionKey key, ChannelContext context) throws IOException
	{
		SocketChannel channel = (SocketChannel) key.channel();
		if (context.content == null) return;
		ByteBuffer confirm = (ByteBuffer) context.content;
		channel.write(confirm);
		if (!confirm.hasRemaining())
		{
			context.content = new Request();
			context.state = server.WaitingRequest;
			key.interestOps(SelectionKey.OP_READ);
		}
	}
}