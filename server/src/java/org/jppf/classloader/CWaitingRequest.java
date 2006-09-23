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
 * This class represents the state of waiting for a request from Nodes
 * @author Domingos Creado
 */
class CWaitingRequest extends ClassChannelState
{
	/**
	 * Log4j logger for this class.
	 */
	private static Logger log = Logger.getLogger(CWaitingRequest.class);

	/**
	 * Initialize this state with a specified JPPFNIOServer.
	 * @param server the JPPFNIOServer this state relates to.
	 */
	CWaitingRequest(ClassServer server)
	{
		super(server);
	}

	/**
	 * Get a resource request from a node and execute it.
	 * @param key the selector key the underlying socket channel is associated with.
	 * @param context object encapsulating the content of the request.
	 * @throws IOException if an error occurred while getting the request data.
	 * @see org.jppf.server.ChannelState#exec(java.nio.channels.SelectionKey, org.jppf.server.ChannelContext)
	 */
	@SuppressWarnings("unchecked")
	public void exec(SelectionKey key, ChannelContext context) throws IOException
	{
		SocketChannel channel = (SocketChannel) key.channel();
		Request out = (Request) context.content;
		if (server.fillRequest(channel, out))
		{
			JPPFResourceWrapper resource = readResource(out.getOutput().toByteArray());
			boolean dynamic = resource.isDynamic();
			String name = resource.getName();
			String uuid = resource.getAppUuid();
			byte[] b = null;
			if (uuid == null)
			{
				b = server.getResourceProvider().getResourceAsBytes(name);
				// Sending b back to node
				resource.setDefinition(b);
				returnOrSchedule(key, context, resource);
			}
			if ((b == null) && dynamic)
			{
				CacheClassContent content = server.classCache.get(new CacheClassKey(uuid, name));
				if (content != null)
				{
					resource.setDefinition(content.getContent());
					returnOrSchedule(key, context, resource);
				}
				else
				{
					SocketChannel provider = server.providerConnections.get(uuid);
					if (provider != null)
					{
						SelectionKey providerKey = provider.keyFor(server.getSelector());
						ChannelContext providerContext = (ChannelContext) providerKey.attachment();
						List<RemoteClassRequest> queue = (List<RemoteClassRequest>) providerContext.content;
						byte[] nameArray = writeResource(resource);
						ByteBuffer sending = server.createByteBuffer(nameArray);
						if (queue.isEmpty())
						{
							try
							{
								provider.write(sending);
							}
							catch(IOException e)
							{
								log.error(e.getMessage(), e);
								server.providerConnections.remove(uuid);
								try
								{
									provider.close();
								}
								catch(Exception ignored)
								{
									log.error(ignored.getMessage(), ignored);
								}
								returnOrSchedule(key, context, resource);
							}
							if (!sending.hasRemaining())
							{
								providerContext.state = server.ReceivingResource;
								providerKey.interestOps(SelectionKey.OP_READ);
								context.state = server.SendingNodeData;
								context.content = null;
							}
							else providerKey.interestOps(SelectionKey.OP_WRITE);
						}
						queue.add(new RemoteClassRequest(name, sending, channel));
						// hangs until the response from provider is fulfilled
						key.interestOps(0);
					}
				}
			}
		}
	}
	/*
	public void exec(SelectionKey key, ChannelContext context) throws IOException
	{
		SocketChannel channel = (SocketChannel) key.channel();
		Request out = (Request) context.content;
		if (server.fillRequest(channel, out))
		{
			String name = new String(out.getOutput().toByteArray());
			boolean dynamic = false;
			if (name.startsWith(":"))
			{
				dynamic = true;
				name = name.substring(1);
			}
			byte[] b = null;
			String uuid = null;
			int idx = name.indexOf("|");
			if (idx >= 0)
			{
				uuid = name.substring(0, idx);
				name = name.substring(idx + 1);
			}
			if (uuid == null)
			{
				b = server.getResourceProvider().getResourceAsBytes(name);
				// Sending b back to node
				returnOrSchedule(key, context, b);
			}
			if ((b == null) && dynamic)
			{
				CacheClassContent content = server.classCache.get(new CacheClassKey(uuid, name));
				if (content != null)
				{
					returnOrSchedule(key, context, content.getContent());
				}
				else
				{
					SocketChannel provider = server.providerConnections.get(uuid);
					if (provider != null)
					{
						SelectionKey providerKey = provider.keyFor(server.getSelector());
						ChannelContext providerContext = (ChannelContext) providerKey.attachment();
						List<RemoteClassRequest> queue = (List<RemoteClassRequest>) providerContext.content;
						byte[] nameArray = name.getBytes();
						ByteBuffer sending = server.createByteBuffer(nameArray);
						if (queue.isEmpty())
						{
							try
							{
								provider.write(sending);
							}
							catch(IOException e)
							{
								log.error(e.getMessage(), e);
								server.providerConnections.remove(uuid);
								try
								{
									provider.close();
								}
								catch(Exception ignored)
								{
									log.error(ignored.getMessage(), ignored);
								}
								returnOrSchedule(key, context, new byte[0]);
							}
							if (!sending.hasRemaining())
							{
								providerContext.state = server.ReceivingResource;
								providerKey.interestOps(SelectionKey.OP_READ);
								context.state = server.SendingNodeData;
								context.content = null;
							}
							else providerKey.interestOps(SelectionKey.OP_WRITE);
						}
						queue.add(new RemoteClassRequest(name, sending, channel));
						// hangs until the response from provider is fulfilled
						key.interestOps(0);
					}
				}
			}
		}
	}
	*/

	/**
	 * This method tries to replay a request to a node, but as the channel is in non-blocking mode, the packet can be
	 * splitted. If the whole data is transfer to OS to transmission, the channel stays in its current state. If data is
	 * splitted, the channel goes to SendingNodeData state.
	 * @param key the key of node channel.
	 * @param context the context of the request.
	 * @param data data be send.
	 * @throws IOException if an IO error occurs while sending the data.
	 */
	void returnOrSchedule(SelectionKey key, ChannelContext context, JPPFResourceWrapper data) throws IOException
	{
		SocketChannel channel = (SocketChannel) key.channel();
		if (data == null)
		{
			data = new JPPFResourceWrapper();
		}
		data.setState(JPPFResourceWrapper.State.PROVIDER_RESPONSE);
		ByteBuffer sendingBuffer = server.createByteBuffer(writeResource(data));
		channel.write(sendingBuffer);
		if (sendingBuffer.hasRemaining())
		{
			context.content = sendingBuffer;
			context.state = server.SendingNodeData;
			key.interestOps(SelectionKey.OP_WRITE);
		}
		else context.content = new Request();
	}
	/*
	void returnOrSchedule(SelectionKey key, ChannelContext context, byte[] data) throws IOException
	{
		SocketChannel channel = (SocketChannel) key.channel();
		if (data == null) data = new byte[0];
		ByteBuffer sendingBuffer = server.createByteBuffer(data);
		channel.write(sendingBuffer);
		if (sendingBuffer.hasRemaining())
		{
			context.content = sendingBuffer;
			context.state = server.SendingNodeData;
			key.interestOps(SelectionKey.OP_WRITE);
		}
		else context.content = new Request();
	}
	*/
}