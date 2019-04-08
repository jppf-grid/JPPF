/*
 * JPPF.
 * Copyright (C) 2005-2019 JPPF Team.
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

package org.jppf.server.nio.classloader.node.async;

import java.util.*;
import java.util.concurrent.atomic.AtomicLong;

import org.jppf.classloader.*;
import org.jppf.nio.ClassLoaderNioMessage;
import org.jppf.server.JPPFDriver;
import org.jppf.server.nio.classloader.*;
import org.jppf.server.nio.classloader.client.async.*;
import org.jppf.server.nio.nodeserver.BaseNodeContext;
import org.jppf.utils.TraversalList;
import org.jppf.utils.configuration.JPPFProperties;
import org.jppf.utils.stats.JPPFStatisticsHelper;
import org.slf4j.*;

/**
 * Performs periodic heartbeat echanges with the nodes and handles heartbeat failures detection.
 * @author Laurent Cohen
 */
public class AsyncNodeClassMessageHandler {
  /**
   * Logger for this class.
   */
  private static final Logger log = LoggerFactory.getLogger(AsyncNodeClassMessageHandler.class);
  /**
   * Determines whether the debug level is enabled in the log configuration, without the cost of a method call.
   */
  private static final boolean debugEnabled = log.isDebugEnabled();
  /**
   * Reference to the singleton JPPF driver.
   */
  private final JPPFDriver driver;
  /**
   * Whether resources should be looked up in the file system if not found in the classpath.
   */
  private final boolean isFileLookup;
  /**
   * 
   */
  private final AtomicLong callableSeq = new AtomicLong(0L);
  /**
   * The class cache.
   */
  private final ClassCache classCache;
  /**
   * Generates the resources unique identifiers.
   */
  private final AtomicLong resourceSequence = new AtomicLong(0);

  /**
   * 
   * @param driver reference to the driver.
   */
  public AsyncNodeClassMessageHandler(final JPPFDriver driver) {
    this.driver = driver;
    this.classCache = driver.getInitializer().getClassCache();
    this.isFileLookup = driver.getConfiguration().get(JPPFProperties.CLASSLOADER_FILE_LOOKUP);
  }

  /**
   * Called when a handshake request is received from a node.
   * @param context represents the node connection.
   * @param resource encapsulates the request.
   * @throws Exception if any error occurs.
   */
  void handshakeRequest(final AsyncNodeClassContext context, final JPPFResourceWrapper resource) throws Exception {
    if (debugEnabled) log.debug("read initial request from node {}", context);
    context.setPeer((Boolean) resource.getData(ResourceIdentifier.PEER, Boolean.FALSE));
    if (debugEnabled) log.debug("initiating node {}", context);
    final String uuid = (String) resource.getData(ResourceIdentifier.NODE_UUID);
    if (debugEnabled) log.debug("received node init request for uuid = {}", uuid);
    if (uuid != null) {
      context.setUuid(uuid);
      context.getServer().addNodeConnection(uuid, context);
    }
    // send the uuid of this driver to the node or node peer.
    resource.setState(context.isPeer() ? JPPFResourceWrapper.State.NODE_INITIATION : JPPFResourceWrapper.State.NODE_RESPONSE);
    resource.setProviderUuid(driver.getUuid());
    if (debugEnabled) log.debug("sending handshake response {} to {}, providerUuid={}", resource, context, resource.getProviderUuid());
    if (context.isLocal()) {
      context.setLocalResponse(resource);
    } else {
      final ClassLoaderNioMessage message = context.serializeResource(resource);
      context.offerMessageToSend(message);
    }
  }

  /**
   * Called when a request to close the cpmmunication channel is received from a node.
   * @param context represents the node connection.
   * @param resource encapsulates the request.
   * @throws Exception if any error occurs.
   */
  void closeChannelRequest(final AsyncNodeClassContext context, final JPPFResourceWrapper resource) throws Exception {
    if (debugEnabled) log.debug("processing channel close request for node {}", context);
    context.getServer().closeConnection(context);
    if (context.isPeer()) {
      final String uuid = (String) resource.getData(ResourceIdentifier.NODE_UUID);
      final BaseNodeContext ctx = driver.getAsyncNodeNioServer().getConnection(uuid);
      if (ctx != null) ctx.handleException(null);
    }
  }

  /**
   * Called when a class loading request is received from a node.
   * @param context represents the node connection.
   * @param resource encapsulates the request.
   * @throws Exception if any error occurs.
   */
  void nodeRequest(final AsyncNodeClassContext context, final JPPFResourceWrapper resource) throws Exception {
    if (debugEnabled) log.debug("read resource request {} from node: {}", resource, context);
    resource.setRequestStartTime(System.nanoTime());
    final long id = resourceSequence.incrementAndGet();
    final String uuid = driver.getUuid();
    resource.setResourceId(uuid, id);
    if (resource instanceof CompositeResourceWrapper) {
      for (final JPPFResourceWrapper res: resource.getResources()) res.setResourceIds(resource.getResourceIds());
    }
    boolean allDefinitionsFound = true;
    context.addNodeRequest(resource);
    for (final JPPFResourceWrapper res: resource.getResources()) {
      final boolean b = (!res.isDynamic() || (res.getRequestUuid() == null)) ? processNonDynamic(context, res) : processDynamic(context, resource, res);
      allDefinitionsFound &= b;
    }
    if (allDefinitionsFound) {
      context.removeNodeRequest(resource);
      context.sendResponse(resource);
    }
    if (debugEnabled) log.debug("pending responses {} for node: {}", context.getNbPendingResponses(), context);
  }

  /**
   * Process a request to the driver's resource provider.
   * @param context represents the node connection.
   * @param resource the resource request description
   * @return {@code true} if the resource definition was found, {@code false} otherwise.
   * @throws Exception if any error occurs.
   */
  private boolean processNonDynamic(final AsyncNodeClassContext context, final JPPFResourceWrapper resource) throws Exception {
    byte[] b = null;
    final String name = resource.getName();
    final TraversalList<String> uuidPath = resource.getUuidPath();
    final AsyncNodeClassNioServer server = context.getServer();
    String uuid = (uuidPath.size() > 0) ? uuidPath.getCurrentElement() : null;
    if (((uuid == null) || uuid.equals(driver.getUuid())) && (resource.getCallable() == null)) {
      final boolean fileLookup = (Boolean) resource.getData(ResourceIdentifier.FILE_LOOKUP_ALLOWED, true) && isFileLookup;
      final ClassLoader cl = getClass().getClassLoader();
      if (resource.getData(ResourceIdentifier.MULTIPLE) != null) {
        final List<byte[]> list = server.getResourceProvider().getMultipleResourcesAsBytes(name, cl, fileLookup);
        if (debugEnabled) log.debug("multiple resources {}found [{}] in driver's classpath for node {}", list != null ? "" : "not ", name, context);
        if (list != null) resource.setData(ResourceIdentifier.RESOURCE_LIST, list);
      } else if (resource.getData(ResourceIdentifier.MULTIPLE_NAMES) != null) {
        final String[] names = (String[]) resource.getData(ResourceIdentifier.MULTIPLE_NAMES);
        final Map<String, List<byte[]>> map = server.getResourceProvider().getMultipleResourcesAsBytes(cl, fileLookup, names);
        resource.setData(ResourceIdentifier.RESOURCE_MAP, map);
      } else {
        if ((uuid == null) && !resource.isDynamic()) uuid = driver.getUuid();
        if (uuid != null) b = classCache.getCacheContent(uuid, name);
        final boolean alreadyInCache = (b != null);
        if (debugEnabled) log.debug("resource {}found [{}] in cache for node {}", alreadyInCache ? "" : "not ", name, context);
        if (!alreadyInCache) {
          b = server.getResourceProvider().getResource(name, cl, fileLookup);
          if (debugEnabled) log.debug("resource {}found [{}] in the driver's classpath for node {}", b == null ? "not " : "", name, context);
        }
        if ((b != null) || !resource.isDynamic()) {
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
   * @param context represents the node connection.
   * @param nodeRequest the initial request sent by the node, which includes the request to the provider.
   * @param resource the resource request description
   * @return {@code true} if the resource definition was found, {@code false} otherwise.
   * @throws Exception if any error occurs.
   */
  private boolean processDynamic(final AsyncNodeClassContext context, final JPPFResourceWrapper nodeRequest, final JPPFResourceWrapper resource) throws Exception {
    byte[] b = null;
    final String name = resource.getName();
    final TraversalList<String> uuidPath = resource.getUuidPath();
    if (resource.isSingleResource()) {
      b = classCache.getCacheContent(uuidPath.getFirst(), name);
      if (b != null) {
        resource.setDefinition(b);
        resource.setState(JPPFResourceWrapper.State.NODE_RESPONSE);
        return true;
      }
    } else if (resource.getCallable() != null) {
      resource.setData(ResourceIdentifier.DRIVER_CALLABLE_ID, callableSeq.incrementAndGet());
    }
    uuidPath.decPosition();
    final String uuid = resource.getUuidPath().getCurrentElement();
    final AsyncClientClassContext provider = findProviderConnection(uuid);
    if (provider != null) {
      if (debugEnabled) log.debug("requesting resource {} from client {} for node {}", resource, provider, context);
      final AsyncResourceRequest request = context.addPendingResponse(resource);
      provider.addRequest(request);
      return false;
    }
    if (debugEnabled) log.debug("no available provider for uuid={} : setting null response for node {}", uuid, context);
    resource.setDefinition(null);
    resource.setState(JPPFResourceWrapper.State.NODE_RESPONSE);
    return true;
  }

  /**
   * Find a provider connection for the specified provider uuid.
   * @param uuid the uuid for which to find a connection.
   * @return an {@link AsyncClientClassContext} instance.
   * @throws Exception if an error occurs while searching for a connection.
   */
  private AsyncClientClassContext findProviderConnection(final String uuid) throws Exception {
    AsyncClientClassContext result = null;
    final AsyncClientClassNioServer clientClassServer = driver.getAsyncClientClassServer();
    final List<AsyncClientClassContext> connections = clientClassServer.getProviderConnections(uuid);
    if (connections == null) return null;
    int minRequests = Integer.MAX_VALUE;
    for (final AsyncClientClassContext channel: connections) {
      final int size = channel.getNbPendingRequests();
      if (size < minRequests) {
        minRequests = size;
        result = channel;
      }
    }
    return result;
  }

  /**
   * Called when a repsonse has been sent to a node.
   * @param context represents the node channel.
   * @param resource the response that was sent.
   * @throws Exception if any error occurs.
   */
  void responseSent(final AsyncNodeClassContext context, final JPPFResourceWrapper resource) throws Exception {
    final long elapsed = (System.nanoTime() - resource.getRequestStartTime()) / 1_000_000L;
    driver.getStatistics().addValues(JPPFStatisticsHelper.NODE_CLASS_REQUESTS_TIME, elapsed, resource.getResources().length);
    if (debugEnabled) log.debug("node {} sent response {}", context, resource);
  }
}
