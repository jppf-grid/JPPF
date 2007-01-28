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

import static org.jppf.server.nio.classloader.ClassState.DEFINING_TYPE;

import java.nio.channels.*;
import java.util.*;

import org.apache.log4j.Logger;
import org.jppf.JPPFException;
import org.jppf.classloader.*;
import org.jppf.server.nio.*;

/**
 * 
 * @author Laurent Cohen
 */
public class ClassNioServer extends NioServer<ClassState, ClassTransition, ClassNioServer>
{
	/**
	 * Log4j logger for this class.
	 */
	private static Logger log = Logger.getLogger(ClassNioServer.class);
	/**
	 * A mapping of the remote resource provider connections handled by this socket server, to their unique uuid.<br>
	 * Provider connections represent connections form the clients only. The mapping to a uuid is required to determine in
	 * which application classpath to look for the requested resources.
	 */
	protected Map<String, SocketChannel> providerConnections = new Hashtable<String, SocketChannel>();
	/**
	 * The cache of class definition, this is done to not flood the provider when it dispatch many tasks. it use
	 * WeakHashMap to minimize the OutOfMemory.
	 */
	Map<CacheClassKey, CacheClassContent> classCache = new WeakHashMap<CacheClassKey, CacheClassContent>();
	/**
	 * Reads resource files from the classpath.
	 */
	protected ResourceProvider resourceProvider = new ResourceProvider();

	/**
	 * Initialize this class server with the port it will listen to.
	 * @param port the port number as an int value.
	 * @throws JPPFException if this server could not be initialized.
	 */
	public ClassNioServer(int port) throws JPPFException
	{
		super(port, "ClassServer Thread");
		selectTimeout = 1L;
	}

	/**
	 * Create the factory holding all the states and transition mappings.
	 * @return an <code>NioServerFactory</code> instance.
	 * @see org.jppf.server.nio.NioServer#createFactory()
	 */
	protected NioServerFactory<ClassState, ClassTransition, ClassNioServer> createFactory()
	{
		return new ClassServerFactory(this);
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
		if (!stop) return;
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
	public void addProviderConnection(String uuid, SocketChannel channel)
	{
		providerConnections.put(uuid, channel);
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
