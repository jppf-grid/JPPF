/*
 * JPPF.
 * Copyright (C) 2005-2013 JPPF Team.
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

package org.jppf.server.nio.classloader.node;

import static org.jppf.server.nio.classloader.ClassTransition.*;
import static org.jppf.utils.StringUtils.build;

import java.util.*;

import org.jppf.classloader.*;
import org.jppf.server.nio.ChannelWrapper;
import org.jppf.server.nio.classloader.*;
import org.jppf.server.nio.classloader.client.ClientClassNioServer;
import org.jppf.server.nio.nodeserver.AbstractNodeContext;
import org.jppf.utils.TraversalList;
import org.slf4j.*;

/**
 * This class represents the state of waiting for a request from a node.
 * @author Laurent Cohen
 */
class WaitingNodeRequestState extends ClassServerState
{
  /**
   * Logger for this class.
   */
  private static final Logger log = LoggerFactory.getLogger(WaitingNodeRequestState.class);
  /**
   * Determines whether DEBUG logging level is enabled.
   */
  private static final boolean debugEnabled = log.isDebugEnabled();
  /**
   * The class cache.
   */
  private final ClassCache classCache = driver.getInitializer().getClassCache();

  /**
   * Initialize this state with a specified NioServer.
   * @param server the JPPFNIOServer this state relates to.
   */
  public WaitingNodeRequestState(final ClassNioServer server)
  {
    super(server);
  }

  /**
   * Execute the action associated with this channel state.
   * @param channel the selection key corresponding to the channel and selector for this state.
   * @return a state transition as an <code>NioTransition</code> instance.
   * @throws Exception if an error occurs while transitioning to another state.
   */
  @Override
  public ClassTransition performTransition(final ChannelWrapper<?> channel) throws Exception
  {
    ClassContext context = (ClassContext) channel.getContext();
    if (context.readMessage(channel))
    {
      JPPFResourceWrapper res = context.deserializeResource();
      if (debugEnabled) log.debug(build("read resource request ", res, " from node: ", channel));
      if (res.getState() == JPPFResourceWrapper.State.CLOSE_CHANNEL)
      {
        NodeClassNioServer.closeConnection(channel);
        String uuid = (String) res.getData(ResourceIdentifier.NODE_UUID);
        AbstractNodeContext ctx = driver.getNodeNioServer().getConnection(uuid);
        if (ctx != null) ctx.closeChannel();
        return null;
      }
      boolean allDefinitionsFound = true;
      for (JPPFResourceWrapper resource: res.getResources()) allDefinitionsFound &= processResource(channel, resource);
      if (allDefinitionsFound) return sendResponse(context);
      if (debugEnabled) log.debug(build("pending responses ", context.getNbPendingResponses(), " for node: ", channel));
      return TO_IDLE_NODE;
    }
    return TO_WAITING_NODE_REQUEST;
  }

  /**
   * Process a resource request.
   * @param channel encapsulates the context and channel.
   * @param resource the resource request description
   * @return <code>true</code> if the resource definition was found, <code>false</code> otherwise.
   * @throws Exception if any error occurs.
   */
  private boolean processResource(final ChannelWrapper<?> channel, final JPPFResourceWrapper resource) throws Exception
  {
    TraversalList<String> uuidPath = resource.getUuidPath();
    boolean dynamic = resource.isDynamic();
    if (!dynamic || (resource.getRequestUuid() == null)) return processNonDynamic(channel, resource);
    return processDynamic(channel, resource);
  }

  /**
   * Process a request to the driver's resource provider.
   * @param channel encapsulates the context and channel.
   * @param resource the resource request description
   * @return <code>true</code> if the resource definition was found, <code>false</code> otherwise.
   * @throws Exception if any error occurs.
   */
  private boolean processNonDynamic(final ChannelWrapper<?> channel, final JPPFResourceWrapper resource) throws Exception
  {
    byte[] b = null;
    String name = resource.getName();
    ClassContext context = (ClassContext) channel.getContext();
    TraversalList<String> uuidPath = resource.getUuidPath();

    String uuid = (uuidPath.size() > 0) ? uuidPath.getCurrentElement() : null;
    if (((uuid == null) || uuid.equals(driver.getUuid())) && (resource.getCallable() == null))
    {
      if (resource.getData(ResourceIdentifier.MULTIPLE) != null)
      {
        List<byte[]> list = server.getResourceProvider().getMultipleResourcesAsBytes(name, null);
        if (debugEnabled) log.debug(build("multiple resources ", list != null ? "" : "not ", "found [", name, "] in driver's classpath for node: ", channel));
        if (list != null) resource.setData(ResourceIdentifier.RESOURCE_LIST, list);
      }
      else if (resource.getData(ResourceIdentifier.MULTIPLE_NAMES) != null)
      {
        String[] names = (String[]) resource.getData(ResourceIdentifier.MULTIPLE_NAMES);
        Map<String, List<byte[]>> map = server.getResourceProvider().getMultipleResourcesAsBytes(null, names);
        resource.setData(ResourceIdentifier.RESOURCE_MAP, map);
      }
      else
      {
        if ((uuid == null) && !resource.isDynamic()) uuid = driver.getUuid();
        if (uuid != null) b = classCache.getCacheContent(uuid, name);
        boolean alreadyInCache = (b != null);
        if (debugEnabled) log.debug(build("resource ", alreadyInCache ? "" : "not ", "found [", name, "] in cache for node: ", channel));
        if (!alreadyInCache)
        {
          b = server.getResourceProvider().getResource(name);
          if (debugEnabled) log.debug(build("resource ", b == null ? "not " : "", "found [", name, "] in the driver's classpath for node: ", channel));
        }
        if ((b != null) || !resource.isDynamic())
        {
          if ((b != null) && !alreadyInCache) classCache.setCacheContent(driver.getUuid(), name, b);
          resource.setDefinition(b);
        }
      }
    }
    resource.setState(JPPFResourceWrapper.State.NODE_RESPONSE);
    return true;
  }

  /**
   * Process a request to the client's resource provider.
   * @param channel encapsulates the context and channel.
   * @param resource the resource request description
   * @return <code>true</code> if the resource definition was found in the cache, <code>false</code> otherwise.
   * @throws Exception if any error occurs.
   */
  private boolean processDynamic(final ChannelWrapper<?> channel, final JPPFResourceWrapper resource) throws Exception
  {
    byte[] b = null;
    String name = resource.getName();
    TraversalList<String> uuidPath = resource.getUuidPath();
    ClassContext context = (ClassContext) channel.getContext();
    if (resource.getCallable() == null) b = classCache.getCacheContent(uuidPath.getFirst(), name);
    if ((b != null) && (resource.getData(ResourceIdentifier.MULTIPLE) == null) && !(resource.getData(ResourceIdentifier.MULTIPLE_NAMES) == null))
    {
      resource.setDefinition(b);
      resource.setState(JPPFResourceWrapper.State.NODE_RESPONSE);
      return true;
    }
    uuidPath.decPosition();
    String uuid = resource.getUuidPath().getCurrentElement();
    ChannelWrapper<?> provider = findProviderConnection(uuid);
    if (provider != null)
    {
      if (debugEnabled) log.debug(build("requesting resource " + resource + " from client: ", provider, " for node: ", channel));
      ClassContext providerContext = (ClassContext) provider.getContext();
      ResourceRequest request = new ResourceRequest(channel, resource);
      resource.setState(JPPFResourceWrapper.State.PROVIDER_REQUEST);
      context.addPendingResponse(resource, request);
      providerContext.addRequest(request);
      return false;
    }
    if (debugEnabled) log.debug(build("no available provider for uuid=", uuid, " : setting null response for node ", channel));
    resource.setDefinition(null);
    resource.setState(JPPFResourceWrapper.State.NODE_RESPONSE);
    return true;
  }

  /**
   * Find a provider connection for the specified provider uuid.
   * @param uuid the uuid for which to find a connection.
   * @return a <code>SelectableChannel</code> instance.
   * @throws Exception if an error occurs while searching for a connection.
   */
  private ChannelWrapper<?> findProviderConnection(final String uuid) throws Exception
  {
    ChannelWrapper<?> result = null;
    ClientClassNioServer clientClassServer = (ClientClassNioServer) driver.getClientClassServer();
    List<ChannelWrapper<?>> connections = clientClassServer.getProviderConnections(uuid);
    if (connections == null) return null;
    int minRequests = Integer.MAX_VALUE;
    for (ChannelWrapper<?> channel: connections)
    {
      ClassContext ctx = (ClassContext) channel.getContext();
      int size = ctx.getNbPendingRequests();
      if (size < minRequests)
      {
        minRequests = size;
        result = channel;
      }
    }
    return result;
  }
}
