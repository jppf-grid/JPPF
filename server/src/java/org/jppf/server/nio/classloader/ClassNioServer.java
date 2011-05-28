/*
 * JPPF.
 * Copyright (C) 2005-2011 JPPF Team.
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

import java.nio.channels.SelectionKey;
import java.util.*;

import org.jppf.JPPFException;
import org.jppf.classloader.ResourceProvider;
import org.jppf.comm.recovery.*;
import org.jppf.server.JPPFDriver;
import org.jppf.server.nio.*;
import org.jppf.utils.*;
import org.slf4j.*;

/**
 * Instances of this class serve class loading requests from the JPPF nodes.
 * @author Laurent Cohen
 */
public class ClassNioServer extends NioServer<ClassState, ClassTransition> implements ReaperListener
{
	/**
	 * Logger for this class.
	 */
	private static Logger log = LoggerFactory.getLogger(ClassNioServer.class);
	/**
	 * Determines whether DEBUG logging level is enabled.
	 */
	private static boolean debugEnabled = log.isDebugEnabled();
	/**
	 * Determines whether TRACE logging level is enabled.
	 */
	private static boolean traceEnabled = log.isTraceEnabled();
	/**
	 * A mapping of the remote resource provider connections handled by this socket server, to their unique uuid.<br>
	 * Provider connections represent connections form the clients only. The mapping to a uuid is required to determine in
	 * which application classpath to look for the requested resources.
	 */
	protected Map<String, List<ChannelWrapper<?>>> providerConnections = new Hashtable<String, List<ChannelWrapper<?>>>();
	/**
	 * The cache of class definition, this is done to not flood the provider when it dispatch many tasks. it use
	 * WeakHashMap to minimize the OutOfMemory.
	 */
	//Map<CacheClassKey, CacheClassContent> classCache = new WeakHashMap<CacheClassKey, CacheClassContent>();
	Map<CacheClassKey, CacheClassContent> classCache = new SoftReferenceValuesMap<CacheClassKey, CacheClassContent>();
	/**
	 * The thread polling the local channel.
	 */
	private ChannelSelectorThread selectorThread = null;
	/**
	 * The local channel, if any.
	 */
	private ChannelWrapper<?> localChannel = null;
	/**
	 * Mapping of channels to their uuid.
	 */
	private Map<String, ChannelWrapper<?>> nodeConnections = new HashMap<String, ChannelWrapper<?>>();
	/**
	 * Reference to the driver.
	 */
	private static JPPFDriver driver = JPPFDriver.getInstance();

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
		super(ports, "ClassServer", false);
		RecoveryServer recoveryServer = driver.getInitializer().getRecoveryServer();
		if (recoveryServer != null) recoveryServer.getReaper().addReaperListener(this);
		selectTimeout = 1L;
	}

	/**
	 * Initialize the local channel connection.
	 * @param localChannel the local channel to use.
	 */
	public void initLocalChannel(ChannelWrapper<?> localChannel)
	{
		if (JPPFConfiguration.getProperties().getBoolean("jppf.local.node.enabled", false))
		{
			this.localChannel = localChannel;
			ChannelSelector channelSelector = new LocalChannelSelector(localChannel);
			localChannel.setSelector(channelSelector);
			selectorThread = new ChannelSelectorThread(channelSelector, this);
			localChannel.setKeyOps(getInitialInterest());
			new Thread(selectorThread, "ClassChannelSelector").start();
			postAccept(localChannel);
		}
	}
	
	/**
	 * {@inheritDoc}
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
		return driver.isShuttingDown();
	}

	/**
	 * {@inheritDoc}
	 */
	public NioContext createNioContext()
	{
		return new ClassContext();
	}

	/**
	 * {@inheritDoc}
	 */
	public int getInitialInterest()
	{
		return SelectionKey.OP_READ;
	}

	/**
	 * {@inheritDoc}
	 */
	public void postAccept(ChannelWrapper wrapper)
	{
		((ClassContext) wrapper.getContext()).setState(DEFINING_TYPE);
	}

	/**
	 * Close and remove all connections accepted by this server.
	 * @see org.jppf.server.nio.NioServer#removeAllConnections()
	 */
	public synchronized void removeAllConnections()
	{
		if (!isStopped()) return;
		synchronized(providerConnections)
		{
			providerConnections.clear();
		}
		synchronized(nodeConnections)
		{
			nodeConnections.clear();
		}
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
	public void addProviderConnection(String uuid, ChannelWrapper<?> channel)
	{
		if (debugEnabled) log.debug("adding provider connection: uuid=" + uuid + ", channel=" + channel);
		synchronized(providerConnections)
		{
			List<ChannelWrapper<?>> list = providerConnections.get(uuid);
			if (list == null)
			{
				list = new ArrayList<ChannelWrapper<?>>();
				providerConnections.put(uuid, list);
			}
			list.add(channel);
		}
	}

	/**
	 * Add a provider connection to the map of existing available providers.
	 * @param uuid the provider uuid as a string.
	 * @param channel the provider's communication channel.
	 */
	public void removeProviderConnection(String uuid, ChannelWrapper channel)
	{
		if (debugEnabled) log.debug("removing provider connection: uuid=" + uuid + ", channel=" + channel);
		synchronized(providerConnections)
		{
			List<ChannelWrapper<?>> list = providerConnections.get(uuid);
			if (list == null) return;
			list.remove(channel);
		}
	}

	/**
	 * Get all the provider connections for the specified client uuid.
	 * @param uuid the uuid of the client for which to get connections.
	 * @return a list of connection channels.
	 */
	public List<ChannelWrapper<?>> getProviderConnections(String uuid)
	{
		synchronized(providerConnections)
		{
			List<ChannelWrapper<?>> list = providerConnections.get(uuid);
			if (list == null) return null;
			return Collections.unmodifiableList(list);
		}
	}

	/**
	 * Add a resource content to the class cache.
	 * @param uuid uuid of the resource provider.
	 * @param name name of the resource.
	 * @param content content of the resource.
	 */
	public void setCacheContent(String uuid, String name, byte[] content)
	{
		if (traceEnabled) log.trace("adding cache entry with key=[" + uuid + ", " + name + "]");
		CacheClassContent cacheContent = new CacheClassContent(content);
		CacheClassKey cacheKey = new CacheClassKey(uuid, name);
		synchronized(classCache)
		{
			classCache.put(cacheKey, cacheContent);
		}
	}

	/**
	 * Get a resource definition from the resource cache.
	 * @param uuid uuid of the reosurce provider.
	 * @param name name of the resource.
	 * @return the content of the resource as an array of bytes.
	 */
	public byte[] getCacheContent(String uuid, String name)
	{
		if (traceEnabled) log.trace("looking up key=[" + uuid + ", " + name + "]");
		CacheClassContent content;
		synchronized(classCache)
		{
			content = classCache.get(new CacheClassKey(uuid, name));
		}
		return content != null ? content.getContent() : null;
	}

	/**
	 * Get a channel from its uuid.
	 * @param uuid the uuid key to look up in the the map.
	 * @return channel the corresponding channel.
	 */
	ChannelWrapper<?> getNodeConnection(String uuid)
	{
		synchronized(nodeConnections)
		{
			return nodeConnections.get(uuid);
		}
	}

	/**
	 * Put the specified uuid / channel pair into the uuid map.
	 * @param uuid the uuid key to add to the map.
	 * @param channel the corresponding channel.
	 */
	void addNodeConnection(String uuid, ChannelWrapper<?> channel)
	{
		if (debugEnabled) log.debug("adding node connection: uuid=" + uuid + ", channel=" + channel);
		synchronized(nodeConnections)
		{
			nodeConnections.put(uuid, channel);
		}
	}

	/**
	 * Remove the specified uuid entry from the uuid map.
	 * @param uuid the uuid key to remove from the map.
	 * @return channel the corresponding channel.
	 */
	ChannelWrapper<?> removeNodeConnection(String uuid)
	{
		if (debugEnabled) log.debug("removing node connection: uuid=" + uuid);
		synchronized(nodeConnections)
		{
			return nodeConnections.remove(uuid);
		}
	}

	/**
	 * Close the specified connection.
	 * @param channel the channel representing the connection.
	 */
	static void closeConnection(ChannelWrapper<?> channel)
	{
		if (channel == null)
		{
			log.warn("attempt to close null channel - skipping this step");
			return;
		}
		ClassNioServer server = JPPFDriver.getInstance().getClassServer();
		ClassContext context = (ClassContext) channel.getContext();
		String uuid = context.getUuid();
		if (uuid != null)
		{
			if (context.isProvider()) server.removeProviderConnection(uuid, channel);
			else server.removeNodeConnection(uuid);
		}
		try
		{
			channel.close();
		}
		catch(Exception e)
		{
			if (debugEnabled) log.debug(e.getMessage(), e);
			else log.warn(e.getMessage());
		}
	}

	/**
	 * {@inheritDoc}
	 */
	public void connectionFailed(ReaperEvent event)
	{
		ServerConnection c = event.getConnection();
		if (!c.isOk())
		{
			String uuid = c.getUuid();
			ChannelWrapper<?> channel = getNodeConnection(uuid);
			if (debugEnabled) log.debug("about to close channel = " + channel + " with uuid = " + uuid);
			closeConnection(channel);
		}
	}
}
