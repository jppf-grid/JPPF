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
import org.jppf.node.JPPFResourceWrapper;
import org.jppf.server.*;

/**
 * This class represents the state of waiting the response of a provider.
 * @author Domingos Creado
 */
class CReceivingResource extends ClassChannelState
{
	/**
	 * Log4j logger for this class.
	 */
	private static Logger log = Logger.getLogger(CReceivingResource.class);
	/**
	 * Initialize this state with a specified JPPFNIOServer.
	 * @param server the JPPFNIOServer this state relates to.
	 */
	CReceivingResource(ClassServer server)
	{
		super(server);
	}

	/**
	 * Read the response to a resource request from a resource provider.
	 * @param key the selector key the underlying socket channel is associated with.
	 * @param context object encapsulating the content of the request.
	 * @throws IOException if an error occurred while reading the response data.
	 * @see org.jppf.server.ChannelState#exec(java.nio.channels.SelectionKey,
	 *      org.jppf.server.ChannelContext)
	 */
	public void exec(SelectionKey key, ChannelContext context) throws IOException
	{
		SocketChannel channel = (SocketChannel) key.channel();
		List queue = (List) context.content;
		RemoteClassRequest request = (RemoteClassRequest) queue.get(0);
		Request out = request.getRequest();
		boolean requestFilled = false;
		try
		{
			requestFilled = server.fillRequest(channel, out);
		}
		catch(IOException e)
		{
			server.providerConnections.remove(context.uuid);
			ByteBuffer sendingBuffer = ByteBuffer.allocateDirect(4).putInt(0);
			SocketChannel destination = request.getChannel();
			SelectionKey destinationKey = destination.keyFor(server.getSelector());
			ChannelContext destinationContext = ((ChannelContext) destinationKey.attachment());
			destinationContext.content = sendingBuffer;
			destinationKey.interestOps(SelectionKey.OP_WRITE);
			throw e;
		}
		if (requestFilled)
		{
			// the request was totaly transfered from provider
			queue.remove(0);
			context.state = server.SendingRequest;
			if (queue.isEmpty()) key.interestOps(SelectionKey.OP_READ);
			else key.interestOps(SelectionKey.OP_WRITE | SelectionKey.OP_READ);
			// putting the definition in cache
			JPPFResourceWrapper resource = readResource(out.getOutput().toByteArray());
			CacheClassContent content = new CacheClassContent(resource.getDefinition());
			CacheClassKey cacheKey = new CacheClassKey(context.uuid, request.getResourceName());
			server.classCache.put(cacheKey, content);
			// fowarding it to channel that requested
			SocketChannel destination = request.getChannel();
			try
			{
				SelectionKey destinationKey = destination.keyFor(server.getSelector());
				resource.setState(JPPFResourceWrapper.State.NODE_RESPONSE);
				byte[] ser = writeResource(resource);
				ByteBuffer sendingBuffer = server.createByteBuffer(ser);
				destination.write(sendingBuffer);
				ChannelContext destinationContext = ((ChannelContext) destinationKey.attachment());
				destinationContext.state = server.SendingNodeData;
				destinationContext.content = sendingBuffer;
				destinationKey.interestOps(SelectionKey.OP_WRITE);
			}
			catch(IOException e)
			{
				log.error(e.getMessage(), e);
				closeChannel(destination);
			}
		}
	}
}