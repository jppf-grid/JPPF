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
import java.util.*;
import org.jppf.node.JPPFBootstrapException;
import org.jppf.server.JPPFNIOServer;
import org.jppf.utils.*;

/**
 * This class is a an important part of the remote class loading mechanism.
 * Its goal is to manage connections that request classes to be loaded by the nodes, as well as the client
 * connections that will serve those requests.
 * @author Laurent Cohen
 * @author Domingos Creado
 */
public class ClassServer extends JPPFNIOServer
{
	/**
	 * A mapping of the remote resource provider connections handled by this socket server, to their unique uuid.<br>
	 * Provider connections represent connections form the clients only. The mapping to a uuid is required to determine
	 * in which application classpath to look for the requested resources.
	 */
	protected Map<String, SocketChannel> providerConnections = new Hashtable<String, SocketChannel>();
	/**
	 * The cache of class definition, this is done to not flood the provider
	 * when it dispatch many tasks. it use WeakHashMap to minimize the
	 * OutOfMemory.
	 */
	private Map<CacheClassKey, CacheClassContent> classCache = new WeakHashMap<CacheClassKey, CacheClassContent>();

	/**
	 * Initialize this class server with the port it will listen to.
	 * @param port the prot number as an int value.
	 * @throws JPPFBootstrapException if this server could not be initialized.
	 */
	public ClassServer(int port) throws JPPFBootstrapException {
		super(port,"ClassServer Thread");
	}

	/**
	 * Close and remove all connections accepted by this server.
	 */
	public synchronized void removeAllConnections()
	{
		if (!stop) return;
		providerConnections.clear();
		super.removeAllConnections();
	}

	/**
	 * Start this class server from the command line.
	 * @param args not used.
	 */
	public static void main(String...args)
	{
		try
		{
			TypedProperties props = JPPFConfiguration.getProperties();
			new ClassServer(props.getInt("class.server.port", 11111)).start();
		}
		catch(Throwable t)
		{
			t.printStackTrace();
		}
	}

	/**
	 * Get the initial state of a connection to the class server.
	 * @return a <code>State</code> instance.
	 * @see org.jppf.server.JPPFNIOServer#getInitialState()
	 */
	protected State getInitialState() {
		return DefiningType;
	}
	
	/**
	 * Get the IO operations a class server connection is initially interested in.
	 * @return {@link java.nio.channels.SelectionKey.OP_READ SelectionKey.OP_READ}.
	 * @see org.jppf.server.JPPFNIOServer#getInitialInterest()
	 */
	protected int getInitialInterest(){
		return SelectionKey.OP_READ;
	}
	
	/**
	 * Get the initial content to send over the connection.
	 * @return a <code>Request</code> instance.
	 * @see org.jppf.server.JPPFNIOServer#getInitialContent()
	 */
	protected Object getInitialContent() {
		return new Request();
	}
	
	/**
	 * Called after a connection to the class server has been accepted by the server socket channel.
	 * This method does nothing.
	 * @param client the <code>SocketChannel</code> that was accepted.
	 * @see org.jppf.server.JPPFNIOServer#postAccept(java.nio.channels.SocketChannel)
	 */
	protected void postAccept(SocketChannel client) {
	}
	
	//====================================================
	// classes related to the state machine of channels
	//====================================================
	
	/**
	 * Initial state of all channel, where it's type is not yet defined, it can
	 * be a provider or a node channel.
	 */
	private State DefiningType = new CDefiningType();

	/**
	 * State of a channel to a node, it is waiting for request from node.
	 */
	private State WaitingRequest = new CWaitingRequest();

	/**
	 * State of a channel to a node where there is a class definition been send.
	 */
	private State SendingNodeData = new CSendingNodeData();

	/**
	 * State of channel with providers, where the request is been send to a provider.
	 */
	private State SendingRequest = new CSendingRequest();

	/**
	 * State of channel with providers, where the provider is sending the class
	 * definition.
	 */
	private State ReceivingResource = new CReceivingResource();

	/**
	 * This class represents the state of a new class server connection,
	 * whose type is yet undetermined.
	 */
	private class CDefiningType implements State {
		/**
		 * Get the initialization data sent over the connenction, and describing
		 * the type of the connection.
		 * @param key the selector key the underlying socket channel is associated with.
		 * @param context object encapsualting the content sent over the connection.
		 * @throws IOException if an error occurred while reading the data.
		 * @see org.jppf.server.JPPFNIOServer.State#exec(java.nio.channels.SelectionKey, org.jppf.server.JPPFNIOServer.Context)
		 */
		public void exec(SelectionKey key, Context context) throws IOException {
			// we don't know yet which whom we are talking, is it a node or a provider?
			SocketChannel channel = (SocketChannel) key.channel();
			Request out = (Request) context.content;

			if (fillRequest(channel, out)) {
				String name = new String(out.getOutput().toByteArray());
				if (name.startsWith("provider|")) {
					String uuid = name.substring("provider|".length(), name.length());
					// it is a provider
					providerConnections.put(uuid, channel);
					context.uuid = uuid;
					context.state = SendingRequest;
					// create the queue of requests to this provider
					context.content = new LinkedList<RemoteClassRequest>();
					key.interestOps(SelectionKey.OP_READ);
				} else if (name.equalsIgnoreCase("node")) {
					// it is a provider
					context.content = new Request();
					// we will wait for a request
					context.state = WaitingRequest;
					key.interestOps(SelectionKey.OP_READ);
				}
			}
		}
	}

	/**
	 * This class represents the state of sending a answer to nodes.
	 */
	private class CSendingNodeData implements State {
		/**
		 * Send resource data to a node.
		 * @param key the selector key the underlying socket channel is associated with.
		 * @param context object encapsulating the content to send over the connection.
		 * @throws IOException if an error occurred while sending the data.
		 * @see org.jppf.server.JPPFNIOServer.State#exec(java.nio.channels.SelectionKey, org.jppf.server.JPPFNIOServer.Context)
		 */
		public void exec(SelectionKey key, Context context) throws IOException {
			SocketChannel channel = (SocketChannel) key.channel();
			if (context.content == null) return;
			ByteBuffer confirm = (ByteBuffer) context.content;
			channel.write(confirm);
			if (!confirm.hasRemaining()) {
				context.content = new Request();
				context.state = WaitingRequest;
				key.interestOps(SelectionKey.OP_READ);
			}
		}
	}

	/**
	 * This class represents the state of being waiting for a request from Nodes
	 */
	private class CWaitingRequest implements State {
		/**
		 * Get a resource request from a node and execute it.
		 * @param key the selector key the underlying socket channel is associated with.
		 * @param context object encapsulating the content of the request.
		 * @throws IOException if an error occurred while getting the request data.
		 * @see org.jppf.server.JPPFNIOServer.State#exec(java.nio.channels.SelectionKey, org.jppf.server.JPPFNIOServer.Context)
		 */
		@SuppressWarnings("unchecked")
		public void exec(SelectionKey key, Context context) throws IOException {
			SocketChannel channel = (SocketChannel) key.channel();
			Request out = (Request) context.content;
			if (fillRequest(channel, out)) {
				String name = new String(out.getOutput().toByteArray());
				boolean dynamic = false;
				if (name.startsWith(":")) {
					dynamic = true;
					name = name.substring(1);
				}
				byte[] b = null;
				String uuid = null;
				int idx = name.indexOf("|");
				if (idx >= 0) {
					uuid = name.substring(0, idx);
					name = name.substring(idx + 1);
				}
				if (uuid == null) {
					b = resourceProvider.getResourceAsBytes(name);
					// Sending b back to node
					returnOrSchedule(key, context, b);
				}
				if ((b == null) && dynamic) {
					CacheClassContent content = classCache.get(new CacheClassKey(uuid, name));
					if (content != null) {
						returnOrSchedule(key, context, content.getContent());
					} else {
						SocketChannel provider = providerConnections.get(uuid);
						if (provider != null) {
							SelectionKey providerKey = provider.keyFor(selector);
							List<RemoteClassRequest> queue = 
								(List<RemoteClassRequest>) ((Context) providerKey.attachment()).content;
							byte[] nameArray = name.getBytes();
							ByteBuffer sending = createByteBuffer(nameArray);
							if (queue.isEmpty()) {
								try {
									provider.write(sending);
								} catch (IOException e) {
									providerConnections.remove(uuid);
									try {
										provider.close();
									} catch (Exception ignored) {
									}
									returnOrSchedule(key, context, new byte[0]);
								}
								if (!sending.hasRemaining()) {
									((Context) providerKey.attachment()).state = ReceivingResource;
									providerKey.interestOps(SelectionKey.OP_READ);
									context.state = SendingNodeData;
									context.content = null;
								} else {
									providerKey.interestOps(SelectionKey.OP_WRITE);
								}
							}
							queue.add(new RemoteClassRequest(name, sending, channel));
							// hangs until the response from provider be fulfilled
							key.interestOps(0);
						}
					}
				}
			}
		}

		/**
		 * This method tries to replay a request to a node, but as the channel
		 * is in non-blocking mode, the packet can be splitted. If the whole
		 * data is transfer to OS to transmission, the channel stays in its
		 * current state. If data is splitted, the channel goes to
		 * SendingNodeData state.
		 * 
		 * @param key the key of node channel.
		 * @param context the context of the request.
		 * @param data data be send.
		 * @throws IOException if an IO error occurs while sending the data.
		 */
		private void returnOrSchedule(SelectionKey key, Context context, byte[] data) throws IOException {
			SocketChannel channel = (SocketChannel) key.channel();
			if (data == null) data = new byte[0];
			ByteBuffer sending = createByteBuffer(data);
			channel.write(sending);
			if (sending.hasRemaining()) {
				context.content = sending;
				context.state = SendingNodeData;
				key.interestOps(SelectionKey.OP_WRITE);
			}
			else context.content = new Request();
		}
	}

	/**
	 * This class represents the state of sending a request to a provider.
	 */
	private class CSendingRequest implements State {
		/**
		 * Forward a resource request to a resource provider.
		 * @param key the selector key the underlying socket channel is associated with.
		 * @param context object encapsulating the content of the request.
		 * @throws IOException if an error occurred while sending the request data.
		 * @see org.jppf.server.JPPFNIOServer.State#exec(java.nio.channels.SelectionKey, org.jppf.server.JPPFNIOServer.Context)
		 */
		public void exec(SelectionKey key, Context context) throws IOException {
			SocketChannel channel = (SocketChannel) key.channel();
			if (key.isReadable()){
				//the provider has closed the connection
				providerConnections.remove(context.uuid);
				channel.close();
				return;
			}
			List queue = (List) context.content;
			if (!queue.isEmpty()) {
				RemoteClassRequest request = (RemoteClassRequest) queue.get(0);
				try {
					channel.write(request.getResource());
				} catch (IOException e) {
					providerConnections.remove(context.uuid);

					// sending a response to node
					SocketChannel destination = request.getChannel();
					try {
						SelectionKey destinationKey = destination.keyFor(selector);
						ByteBuffer sending = createByteBuffer(new byte[0]);
						destination.write(sending);
						((Context) destinationKey.attachment()).content = sending;
						destinationKey.interestOps(SelectionKey.OP_WRITE);
					} catch (IOException e2) {
						try {
							destination.close();
						} catch (IOException ignored) {	}
					}
					// let the main loop close the channel
					throw e;
				}
				if (!request.getResource().hasRemaining()) {
					context.state = ReceivingResource;
					key.interestOps(SelectionKey.OP_READ);
				}
			}
		}
	}

	/**
	 * This class represents the state of waiting the response of a provider.
	 */
	private class CReceivingResource implements State {
		/**
		 * Read the response to a resource request from a resource provider.
		 * @param key the selector key the underlying socket channel is associated with.
		 * @param context object encapsulating the content of the request.
		 * @throws IOException if an error occurred while reading the response data.
		 * @see org.jppf.server.JPPFNIOServer.State#exec(java.nio.channels.SelectionKey, org.jppf.server.JPPFNIOServer.Context)
		 */
		public void exec(SelectionKey key, Context context) throws IOException {
			SocketChannel channel = (SocketChannel) key.channel();
			List queue = (List) context.content;
			RemoteClassRequest request = (RemoteClassRequest) queue.get(0);
			Request out = request.getRequest();
			boolean requestFilled = false;
			try {
				requestFilled = fillRequest(channel, out);
			} catch (IOException e) {
				providerConnections.remove(context.uuid);
				ByteBuffer sending = ByteBuffer.allocateDirect(4).putInt(0);
				SocketChannel destination = request.getChannel();
				SelectionKey destinationKey = destination.keyFor(selector);
				((Context) destinationKey.attachment()).content = sending;
				destinationKey.interestOps(SelectionKey.OP_WRITE);
				throw e;
			}
			if (requestFilled) {
				// the request was totaly transfered from provider
				queue.remove(0);
				context.state = SendingRequest;
				if (queue.isEmpty()) key.interestOps(SelectionKey.OP_READ);
				else key.interestOps(SelectionKey.OP_WRITE | SelectionKey.OP_READ);
				// putting the definition in cache
				CacheClassContent content =
					new CacheClassContent(out.getOutput().toByteArray());
				CacheClassKey cacheKey =
					new CacheClassKey(context.uuid, request.getResourceName());
				classCache.put(cacheKey, content);
				// fowarding it to channel that requested
				SocketChannel destination = request.getChannel();
				try {
					SelectionKey destinationKey = destination.keyFor(selector);
					byte[] classDef = out.getOutput().toByteArray();
					ByteBuffer sending = createByteBuffer(classDef);
					destination.write(sending);
					((Context) destinationKey.attachment()).content = sending;
					destinationKey.interestOps(SelectionKey.OP_WRITE);
				} catch (IOException e) {
					try {
						destination.close();
					} catch (IOException ignored) {	}
				}
			}
			return;
		}
	}

	/**
	 * Create a <code>ByteBuffer</code> filled with the specified data.
	 * Before being returned, the buffer's position is set to 0.
	 * @param data the data used to fill the buffer.
	 * @return a <code>ByteBuffer</code> instance.
	 */
	private ByteBuffer createByteBuffer(byte[] data)
	{
		ByteBuffer buffer = ByteBuffer.allocateDirect(data.length + 4);
		buffer.putInt(data.length);
		buffer.put(data);
		buffer.flip();
		return buffer;
	}

	/**
	 * This class encapsulate a resource request to a resource provider.
	 */
	private class RemoteClassRequest {
		/**
		 * Buffer used to read the data from a socket channel.
		 */
		private ByteBuffer resource;
		/**
		 * This object encapsulates the request to send to the provider.
		 */
		private Request request = new Request();
		/**
		 * The socket channel encapsulating a non-blocking socket connection.
		 */
		private SocketChannel channel;
		/**
		 * String containing the provider uuid.
		 */
		private String res;

		/**
		 * Initialize this remote request with a specified request string,
		 * data buffer and socket channel.
		 * @param res string containing the provider uuid.
		 * @param resource buffer used to read the data from a socket channel.
		 * @param channel the socket channel encapsulating a non-blocking socket connection.
		 */
		public RemoteClassRequest(String res, ByteBuffer resource, SocketChannel channel) {
			super();
 			this.res = res;
			this.resource = resource;
			this.channel = channel;
		}

		/**
		 * Get the socket channel encapsulating a non-blocking socket connection.
		 * @return a <code>SocketChannel</code> instance.
		 */
		public SocketChannel getChannel() {
			return channel;
		}

		/**
		 * Get the buffer used to read the data from a socket channel.
		 * @return a <code>ByteBuffer</code> instance.
		 */
		public ByteBuffer getResource() {
			return resource;
		}

		/**
		 * Get the object that encapsulates the request to send to the provider.
		 * @return a <code>Request</code> instance.
		 */
		public Request getRequest() {
			return request;
		}

		/**
		 * Get the string containing the provider uuid.
		 * @return the uuid as a string.
		 */
		public String getResourceName() {
			return res;
		}
	}
	
	//====================================================
	// classes related to class definition cache
	//====================================================
	/**
	 * This class represents the key used in the class cache.
	 */
	private class CacheClassKey {
		/**
		 * The provider uuid.
		 */
		private String uuid;
		/**
		 * String describing the cached resource.
		 */
		private String res;

		/**
		 * Initialize this key with a specified provider uuid and resource string.
		 * @param uuid the provider uuid.
		 * @param res string describing the cached resource.
		 */
		public CacheClassKey(String uuid, String res) {
			this.uuid = uuid;
			this.res = res;
		}

		/**
		 * Determine whether this key is equal to another one.
		 * @param obj the other key to compre with.
		 * @return true if the 2 keys a re equal, false otherwise.
		 * @see java.lang.Object#equals(java.lang.Object)
		 */
		public boolean equals(Object obj) {
			if (obj instanceof CacheClassKey) {
				CacheClassKey other = (CacheClassKey) obj;
				return this.uuid.equals(other.uuid) && this.res.equals(other.res);
			}
			return false;
		}

		/**
		 * Calculate the ahsh code of this key.
		 * @return the hashcode as an int value.
		 * @see java.lang.Object#hashCode()
		 */
		public int hashCode() {
			return uuid.hashCode() + res.hashCode();
		}
	}

	/**
	 * This class encapsulates the content oif a class cache entry.
	 */
	private class CacheClassContent {
		/**
		 * The actual content of this element.
		 */
		private byte[] content;

		/**
		 * Initialize this content with the specified data.
		 * @param content the data as an array of bytes.
		 */
		public CacheClassContent(byte[] content) {
			super();
			this.content = content;
		}

		/**
		 * Get the actual content of this element.
		 * @return the data as an array of bytes.
		 */
		public byte[] getContent() {
			return content;
		}
	}
}
