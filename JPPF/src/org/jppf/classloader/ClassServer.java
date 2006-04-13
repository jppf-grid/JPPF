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
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.util.Hashtable;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

import org.jppf.node.JPPFBootstrapException;
import org.jppf.server.JPPFNIOServer;
import org.jppf.utils.JPPFConfiguration;
import org.jppf.utils.TypedProperties;

/**
 * This class is a wrapper around a server socket, listenening to incoming connections to and from nodes and client
 * applications. 
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
	
	
	public ClassServer(int port) throws JPPFBootstrapException {
		super(port,"ClassServer Thread");
	}

	/**
	 * Close and remove all connections accepted by this server.
	 */
	@Override
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


	@Override
	protected State getInitialState() {
		return DefiningType;
	}
	
	@Override
	protected int getInitialInterest(){
		return SelectionKey.OP_READ;
	}
	
	@Override
	protected Object getInitialContent() {
		return new Request();
	}
	
	@Override
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
	 * State of channel with providers, where the request is been send to
	 * provider.
	 */
	private State SendingRequest = new CSendingRequest();

	/**
	 * State of channel with providers, where the provider is sending the class
	 * definition.
	 */
	private State ReceivingResource = new CReceivingResource();

	private class CDefiningType implements State {
		public void exec(SelectionKey key, Context context) throws IOException {
			// we don't know yet which whom we are talking,
			// is it a node or a provider?

			SocketChannel channel = (SocketChannel) key.channel();
			Request out = (Request) context.content;

			if (fillRequest(channel, out)) {
				String name = new String(out.getOutput().toByteArray());
				if (name.startsWith("provider|")) {
					String uuid = name.substring("provider|".length(), name
							.length());
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
	 * 
	 */
	private class CSendingNodeData implements State {

		public void exec(SelectionKey key, Context context) throws IOException {
			SocketChannel channel = (SocketChannel) key.channel();
			if (context.content == null)
				return;
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
	 * 
	 */
	private class CWaitingRequest implements State {

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

					CacheClassContent content = classCache
							.get(new CacheClassKey(uuid, name));
					if (content != null) {
						returnOrSchedule(key, context, content.getContent());
					} else {
						SocketChannel provider = providerConnections.get(uuid);
						if (provider != null) {

							SelectionKey providerKey = provider
									.keyFor(selector);

							List<RemoteClassRequest> queue = 
								(List<RemoteClassRequest>) ((Context) providerKey
									.attachment()).content;
							byte[] nameArray = name.getBytes();
							ByteBuffer sending = ByteBuffer
									.allocateDirect(nameArray.length + 4);
							sending.putInt(nameArray.length);
							sending.put(nameArray);
							sending.flip();
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
									providerKey
											.interestOps(SelectionKey.OP_READ);
									context.state = SendingNodeData;
									context.content = null;
								} else {
									providerKey
											.interestOps(SelectionKey.OP_WRITE);
								}
							}
							queue.add(new RemoteClassRequest(name, sending,
									channel));

							// hangs until the response from provider be
							// fulfilled
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
		 * @param key
		 *            the key of node channel
		 * @param context
		 *            the context of the request
		 * @param data
		 *            data be send
		 * @throws IOException
		 */
		private void returnOrSchedule(SelectionKey key, Context context,
				byte[] data) throws IOException {
			
			SocketChannel channel = (SocketChannel) key.channel();
			if(data == null){
				data = new byte[0];
			}
			ByteBuffer sending = ByteBuffer.allocateDirect(data.length + 4);
			sending.putInt(data.length);
			sending.put(data);
			sending.flip();
			channel.write(sending);

			if (sending.hasRemaining()) {
				context.content = sending;
				context.state = SendingNodeData;
				key.interestOps(SelectionKey.OP_WRITE);
			} else {
				context.content = new Request();
			}
		}
	}

	/**
	 * This class represents the state of sending a request to a provider.
	 * 
	 */
	private class CSendingRequest implements State {

		public void exec(SelectionKey key, Context context) throws IOException {
			
			SocketChannel channel = (SocketChannel) key.channel();
			
			if(key.isReadable()){
				//the provider has closed the connection
				providerConnections.remove(context.uuid);
				channel.close();
				return;
			}
			List<RemoteClassRequest> queue = (List<RemoteClassRequest>) context.content;
			if (!queue.isEmpty()) {
				RemoteClassRequest request = queue.get(0);
				try {
					channel.write(request.getResource());
				} catch (IOException e) {
					providerConnections.remove(context.uuid);

					// sending a response to node
					SocketChannel destination = request.getChannel();
					try {
						SelectionKey destinationKey = destination
								.keyFor(selector);
						byte[] classDef = new byte[0];

						ByteBuffer sending = ByteBuffer
								.allocateDirect(classDef.length + 4);
						sending.putInt(classDef.length);
						sending.put(classDef);
						sending.flip();
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
	 * 
	 */
	private class CReceivingResource implements State {

		public void exec(SelectionKey key, Context context) throws IOException {
			SocketChannel channel = (SocketChannel) key.channel();
			List<RemoteClassRequest> queue = (List<RemoteClassRequest>) context.content;
			RemoteClassRequest request = queue.get(0);
			Request out = request.getRequest();
			
			boolean requestFilled = false;
			try {
				requestFilled = fillRequest(channel, out);
			} catch (IOException e) {
				providerConnections.remove(context.uuid);
				ByteBuffer sending = ByteBuffer
				.allocateDirect(4).putInt(0);
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
				if (queue.isEmpty()) {
					key.interestOps(SelectionKey.OP_READ);
				} else {
					key.interestOps(SelectionKey.OP_WRITE | SelectionKey.OP_READ);
				}

				// putting the definition in cache
				CacheClassContent content = new CacheClassContent(out
						.getOutput().toByteArray());
				CacheClassKey cacheKey = new CacheClassKey(context.uuid,
						request.getResourceName());
				classCache.put(cacheKey, content);

				// fowarding it to channel that requested
				SocketChannel destination = request.getChannel();
				try {
					SelectionKey destinationKey = destination.keyFor(selector);
					byte[] classDef = out.getOutput().toByteArray();

					ByteBuffer sending = ByteBuffer
							.allocateDirect(classDef.length + 4);
					sending.putInt(classDef.length);
					sending.put(classDef);
					sending.flip();
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

	private class RemoteClassRequest {
		private ByteBuffer resource;

		private Request request = new Request();

		private SocketChannel channel;;

		private String res;

		public RemoteClassRequest(String res, ByteBuffer resource,
				SocketChannel channel) {
			super();
 
			this.res = res;
			this.resource = resource;
			this.channel = channel;
		}

		public SocketChannel getChannel() {
			return channel;
		}

		public ByteBuffer getResource() {
			return resource;
		}

		public Request getRequest() {
			return request;
		}

		public String getResourceName() {
			return res;
		}
	}
	
	//====================================================
	// classes related to class definition cache
	//====================================================
	private class CacheClassKey {
		private String uuid;

		private String res;

		public CacheClassKey(String uuid, String res) {
			this.uuid = uuid;
			this.res = res;
		}

		@Override
		public boolean equals(Object obj) {
			if (obj instanceof CacheClassKey) {
				CacheClassKey other = (CacheClassKey) obj;
				return this.uuid.equals(other.uuid)
						&& this.res.equals(other.res);
			}
			return false;

		}

		@Override
		public int hashCode() {
			return uuid.hashCode() + res.hashCode();
		}
	}

	private class CacheClassContent {
		private byte[] content;

		public CacheClassContent(byte[] content) {
			super();
			this.content = content;
		}

		public byte[] getContent() {
			return content;
		}
	}
}
