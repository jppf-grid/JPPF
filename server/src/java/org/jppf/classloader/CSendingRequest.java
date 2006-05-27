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
import java.util.List;
import org.apache.log4j.Logger;
import org.jppf.server.*;

/**
 * This class represents the state of sending a request to a provider.
 * @author Domingos Creado
 */
class CSendingRequest implements ChannelState
{
	/**
	 * Log4j logger for this class.
	 */
	private static Logger log = Logger.getLogger(CSendingRequest.class);
	/**
	 * The JPPFNIOServer this state relates to.
	 */
	private ClassServer server;

	/**
	 * Initialize this state with a specified JPPFNIOServer.
	 * @param server the JPPFNIOServer this state relates to.
	 */
	CSendingRequest(ClassServer server)
	{
		this.server = server;
	}

	/**
	 * Forward a resource request to a resource provider.
	 * @param key the selector key the underlying socket channel is associated with.
	 * @param context object encapsulating the content of the request.
	 * @throws IOException if an error occurred while sending the request data.
	 * @see org.jppf.server.ChannelState#exec(java.nio.channels.SelectionKey,
	 *      org.jppf.server.ChannelContext)
	 */
	public void exec(SelectionKey key, ChannelContext context) throws IOException
	{
		SocketChannel channel = (SocketChannel) key.channel();
		if (key.isReadable())
		{
			// the provider has closed the connection
			this.server.providerConnections.remove(context.uuid);
			channel.close();
			return;
		}
		List queue = (List) context.content;
		if (!queue.isEmpty())
		{
			RemoteClassRequest request = (RemoteClassRequest) queue.get(0);
			try
			{
				channel.write(request.getResource());
			}
			catch(IOException e)
			{
				server.providerConnections.remove(context.uuid);
				// sending a response to node
				SocketChannel destination = request.getChannel();
				try
				{
					SelectionKey destinationKey = destination.keyFor(server.getSelector());
					ByteBuffer sendingBuffer = server.createByteBuffer(new byte[0]);
					destination.write(sendingBuffer);
					ChannelContext destinationContext = ((ChannelContext) destinationKey.attachment());
					destinationContext.content = sendingBuffer;
					destinationKey.interestOps(SelectionKey.OP_WRITE);
				}
				catch(IOException e2)
				{
					log.error(e2.getMessage(), e2);
					try
					{
						destination.close();
					}
					catch(IOException ignored)
					{
						log.error(ignored.getMessage(), ignored);
					}
				}
				// let the main loop close the channel
				throw e;
			}
			if (!request.getResource().hasRemaining())
			{
				context.state = this.server.ReceivingResource;
				key.interestOps(SelectionKey.OP_READ);
			}
		}
	}
}