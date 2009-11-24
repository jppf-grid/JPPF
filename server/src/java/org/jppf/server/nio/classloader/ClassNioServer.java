/*
 * Java Parallel Processing Framework.
 *  Copyright (C) 2005-2009 JPPF Team. 
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

import static org.jppf.server.nio.classloader.ClassState.DEFINING_TYPE;

import java.nio.channels.*;
import java.util.*;

import org.apache.commons.logging.*;
import org.jppf.JPPFException;
import org.jppf.classloader.*;
import org.jppf.server.JPPFDriver;
import org.jppf.server.nio.*;

/**
 * Instances of this class serve class loading requests from the JPPF nodes.
 * @author Laurent Cohen
 */
public class ClassNioServer extends NioServer<ClassState, ClassTransition>
{
	/**
	 * Logger for this class.
	 */
	private static Log log = LogFactory.getLog(ClassNioServer.class);
	/**
	 * A mapping of the remote resource provider connections handled by this socket server, to their unique uuid.<br>
	 * Provider connections represent connections form the clients only. The mapping to a uuid is required to determine in
	 * which application classpath to look for the requested resources.
	 */
	//protected Map<String, SocketChannel> providerConnections = new Hashtable<String, SocketChannel>();
	protected Map<String, List<SelectableChannel>> providerConnections = new Hashtable<String, List<SelectableChannel>>();
	/**
	 * The cache of class definition, this is done to not flood the provider when it dispatch many tasks. it use
	 * WeakHashMap to minimize the OutOfMemory.
	 */
	Map<CacheClassKey, CacheClassContent> classCache = new WeakHashMap<CacheClassKey, CacheClassContent>();

	/**
	 * Initialize this class server with the port it will listen to.
	 * @param port the port number as an int value.
	 * @throws JPPFException if this server could not be initialized.
	 */
	public ClassNioServer(int port) throws JPPFException
	{
		this(new int[] { port });
	}

	/**
	 * Initialize this class server with a specified list of port numbers.
	 * @param ports the list of port this server accepts connections from.
	 * @throws JPPFException if the underlying server socket can't be opened.
	 */
	public ClassNioServer(final int[] ports) throws JPPFException
	{
		super(ports, "ClassServer Thread", false);
		//selectTimeout = 1L;
	}

	/**
	 * Create the factory holding all the states and transition mappings.
	 * @return an <code>NioServerFactory</code> instance.
	 * @see org.jppf.server.nio.NioServer#createFactory()
	 */
	protected NioServerFactory<ClassState, ClassTransition> createFactory()
	{
		return new ClassServerFactory(this);
	}

	/**
	 * Determine whether a stop condition external to this server has been reached.
	 * @return true if the driver is shutting down, false otherwise.
	 * @see org.jppf.server.nio.NioServer#externalStopCondition()
	 */
	protected boolean externalStopCondition()
	{
		return JPPFDriver.getInstance().isShuttingDown();
	}

	/**
	 * Define a context for a newly created channel.
	 * @return an <code>NioContext</code> instance.
	 * @see org.jppf.server.nio.NioServer#createNioContext()
	 */
	public NioContext createNioContext()
	{
		return new ClassContext();
	}

	/**
	 * Get the IO operations a connection is initially interested in.
	 * @return a bit-wise combination of the interests, taken from {@link java.nio.channels.SelectionKey SelectionKey}
	 * constants definitions.
	 * @see org.jppf.server.nio.NioServer#getInitialInterest()
	 */
	public int getInitialInterest()
	{
		return SelectionKey.OP_READ;
	}

	/**
	 * Process a channel that was accepted by the server socket channel.
	 * @param key the selection key for the socket channel to process.
	 * @see org.jppf.server.nio.NioServer#postAccept(java.nio.channels.SelectionKey)
	 */
	public void postAccept(SelectionKey key)
	{
		((ClassContext) key.attachment()).setState(DEFINING_TYPE);
	}

	/**
	 * Close and remove all connections accepted by this server.
	 */
	public synchronized void removeAllConnections()
	{
		if (!isStopped()) return;
		providerConnections.clear();
		super.removeAllConnections();
	}

	/**
	 * Get the resource provider for this server.
	 * @return a ResourceProvider instance.
	 */
	public ResourceProvider getResourceProvider()
	{
		return resourceProvider;
	}

	/**
	 * Add a provider connection to the map of existing available providers.
	 * @param uuid the provider uuid as a string.
	 * @param channel the provider's communication channel.
	 */
	public void addProviderConnection(String uuid, SelectableChannel channel)
	{
		List<SelectableChannel> list = providerConnections.get(uuid);
		if (list == null)
		{
			list = new ArrayList<SelectableChannel>();
			providerConnections.put(uuid, list);
		}
		list.add(channel);
	}

	/**
	 * Add a provider connection to the map of existing available providers.
	 * @param uuid the provider uuid as a string.
	 * @param channel the provider's communication channel.
	 */
	public void removeProviderConnection(String uuid, SelectableChannel channel)
	{
		List<SelectableChannel> list = providerConnections.get(uuid);
		if (list == null) return;
		list.remove(channel);
	}

	/**
	 * Add a resource content to the class cache.
	 * @param uuid uuid of the resource provider.
	 * @param name name of the resource.
	 * @param content content of the resource.
	 */
	public void setCacheContent(String uuid, String name, byte[] content)
	{
		CacheClassContent cacheContent = new CacheClassContent(content);
		CacheClassKey cacheKey = new CacheClassKey(uuid, name);
		classCache.put(cacheKey, cacheContent);
	}

	/**
	 * Get a resource definition from the resource cache.
	 * @param uuid uuid of the reosurce provider.
	 * @param name name of the resource.
	 * @return the content of the resource as an array of bytes.
	 */
	public byte[] getCacheContent(String uuid, String name)
	{
		CacheClassContent content = classCache.get(new CacheClassKey(uuid, name));
		return content != null ? content.getContent() : null;
	}
}
