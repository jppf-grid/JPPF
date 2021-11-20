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

package org.jppf.server.nio.classloader.client;

import org.jppf.classloader.*;
import org.jppf.server.JPPFDriver;
import org.jppf.server.nio.classloader.AsyncResourceRequest;
import org.jppf.utils.stats.JPPFStatisticsHelper;
import org.slf4j.*;

/**
 * Performs periodic heartbeat echanges with the nodes and handles heartbeat failures detection.
 * @author Laurent Cohen
 */
public class AsyncClientClassMessageHandler {
  /**
   * Logger for this class.
   */
  private static final Logger log = LoggerFactory.getLogger(AsyncClientClassMessageHandler.class);
  /**
   * Determines whether the debug level is enabled in the log configuration, without the cost of a method call.
   */
  private static final boolean debugEnabled = log.isDebugEnabled();
  /**
   * Reference to the singleton JPPF driver.
   */
  private final JPPFDriver driver;
  /**
   * 
   */
  private final AsyncClientClassNioServer server;

  /**
   * 
   * @param server the nio server.
   */
  public AsyncClientClassMessageHandler(final AsyncClientClassNioServer server) {
    this.server = server;
    this.driver = server.getDriver();
  }

  /**
   * Called when a handshake request is received from a client.
   * @param context represents the connection with the client.
   * @param resource the resource sent by the client.
   * @throws Exception if any error occurs.
   */
  void peerHandshakeResponse(final AsyncClientClassContext context, final JPPFResourceWrapper resource) throws Exception {
    final String uuid = resource.getProviderUuid();
    context.setPeer(true);
    context.setUuid(uuid);
    if (debugEnabled) log.debug("read initial response from peer {}, providerUuid={}", context, uuid);
    server.addProviderConnection(uuid, context);
  }

  /**
   * Called when a handshake request is received from a client.
   * @param context represents the connection with the client.
   * @param resource the resource sent by the client.
   * @throws Exception if any error occurs.
   */
  void providerHandshakeRequest(final AsyncClientClassContext context, final JPPFResourceWrapper resource) throws Exception {
    if (debugEnabled) log.debug("read initial request from provider {}", context);
    final String uuid = resource.getUuidPath().getFirst();
    if (debugEnabled) log.debug("initializing provider with uuid={}", uuid);
    context.setUuid(uuid);
    context.setConnectionUuid((String) resource.getData(ResourceIdentifier.CONNECTION_UUID));
    server.addProviderConnection(uuid, context);
    context.offerMessageToSend(context.serializeResource(resource));
    if (debugEnabled) log.debug("initialized provider {}", context);
  }

  /**
   * Called when a response is received from a client.
   * @param context represents the connection with the client.
   * @param resource the resource sent by the client.
   * @throws Exception if any error occurs.
   */
  void providerResponse(final AsyncClientClassContext context, final JPPFResourceWrapper resource) throws Exception {
    final AsyncResourceRequest request = context.removeRequest(resource);
    if (debugEnabled) log.debug("read response from provider {}, sending to node {}, resource = {}", context, request.getContext(), resource.getName());
    final double elapsed = (System.nanoTime() - request.getRequestStartTime()) / 1_000_000d;
    if ((resource.getDefinition() != null) && resource.isSingleResource()) server.getClassCache().setCacheContent(resource.getUuidPath().getFirst(), resource.getName(), resource.getDefinition());
    resource.setState(JPPFResourceWrapper.State.NODE_RESPONSE);
    request.setResource(resource);
    request.getContext().handleProviderResponse(request);
    driver.getStatistics().addValue(JPPFStatisticsHelper.CLIENT_CLASS_REQUESTS_TIME, elapsed);
  }
}
