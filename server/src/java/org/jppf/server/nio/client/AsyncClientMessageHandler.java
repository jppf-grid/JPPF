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

package org.jppf.server.nio.client;

import java.util.*;

import org.jppf.io.*;
import org.jppf.management.*;
import org.jppf.node.protocol.*;
import org.jppf.server.JPPFDriver;
import org.jppf.server.nio.classloader.client.*;
import org.jppf.server.nio.nodeserver.PeerAttributesHandler;
import org.jppf.server.protocol.ServerTaskBundleClient;
import org.jppf.utils.*;
import org.jppf.utils.configuration.JPPFProperties;
import org.slf4j.*;

/**
 * Performs periodic heartbeat echanges with the nodes and handles heartbeat failures detection.
 * @author Laurent Cohen
 */
public class AsyncClientMessageHandler {
  /**
   * Logger for this class.
   */
  private static final Logger log = LoggerFactory.getLogger(AsyncClientMessageHandler.class);
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
   * @param driver reference to the driver.
   */
  public AsyncClientMessageHandler(final JPPFDriver driver) {
    this.driver = driver;
  }

  /**
   * Called when a job is received from a client.
   * @param context the client connection context.
   * @param message the message that was received.
   * @throws Exception if any error occurs.
   */
  void jobReceived(final AsyncClientContext context, final ClientMessage message) throws Exception {
    if (debugEnabled) log.debug("received job {} for {}", message, context);
    final ServerTaskBundleClient clientBundle = context.deserializeBundle(message);
    final TaskBundle header = clientBundle.getJob();
    final boolean closeCommand = header.getParameter(BundleParameter.CLOSE_COMMAND, false);
    if (closeCommand) {
      if (debugEnabled) log.debug("processing CLOSE_COMMAND for {}", context);
      context.handleException(null);
      return;
    }
    final int count = header.getTaskCount();
    if (debugEnabled) log.debug("read bundle {}" + " from client {}" + " done: received {} tasks", clientBundle, context, count);
    if (clientBundle.getJobReceivedTime() == 0L) clientBundle.setJobReceivedTime(System.currentTimeMillis());
    header.getUuidPath().incPosition();
    header.getUuidPath().add(driver.getUuid());
    if (debugEnabled) log.debug("uuid path=" + header.getUuidPath());
    clientBundle.addCompletionListener(new CompletionListener(context));
    clientBundle.handleNullTasks();
    if (clientBundle.isDone()) {
      if (debugEnabled) log.debug("client bundle done: {}", clientBundle);
      clientBundle.bundleEnded();
    } else {
      context.addEntry(clientBundle);
      context.driver.getQueue().addBundle(clientBundle);
    }
  }

  /**
   * Send the specified job results.
   * @param context the client connection context.
   * @param bundle contains the results to send.
   * @throws Exception if any error occurs.
   */
  void sendJobResults(final AsyncClientContext context, final ServerTaskBundleClient bundle) throws Exception {
    if (log.isTraceEnabled()) {
      log.trace("sending job results with originalId={}, bundle={} for {}\ntasks positions = {}",
        bundle.getOriginalBundleId(), bundle, context, Arrays.toString(bundle.getTasksPositions()));
    }
    else if (debugEnabled) log.debug("sending job results with originalId={}, bundle={} for {}", bundle.getOriginalBundleId(), bundle, context);
    final ClientMessage message = context.serializeBundle(bundle);
    context.offerMessageToSend(bundle, message);
  }

  /**
   * Call when a set of job results has been sent.
   * @param context the client connection context.
   * @param bundle contains the results to send.
   * @throws Exception if any error occurs.
   */
  void jobResultsSent(final AsyncClientContext context, final ServerTaskBundleClient bundle) throws Exception {
    final long bundleId = bundle.getOriginalBundleId();
    if (debugEnabled) log.debug("job results sent bundleId={}, bundle={} for {}", bundleId, bundle, context);
    final JobEntry entry = context.getJobEntry(bundle.getUuid() + bundleId);
    if (entry != null) {
      entry.nbTasksToSend -= bundle.getTaskCount();
      if (debugEnabled) log.debug("job entry = {}", entry);
      if (entry.nbTasksToSend <= 0) {
        if (debugEnabled) log.debug("*** client bundle ended {}", entry.getBundle());
        entry.jobEnded();
        context.removeJobEntry(bundle.getUuid(), bundleId);
      }
    } else {
      if (log.isTraceEnabled()) log.trace("job entry not found for uuid={}, bundleId={}, call stack:\n{}", bundle.getUuid(), bundleId, ExceptionUtils.getCallStack());
      else if (debugEnabled) log.debug("job entry not found for uuid={}, bundleId={}", bundle.getUuid(), bundleId);
    }
  }

  /**
   * Called when a handshake request from a client is received.
   * @param context the client connection context.
   * @param message the message that was received.
   * @throws Exception if any error occurs.
   */
  void handshakeReceived(final AsyncClientContext context, final ClientMessage message) throws Exception {
    final ServerTaskBundleClient bundle = context.deserializeBundle(message);
    final TaskBundle header = bundle.getJob();
    if (debugEnabled) log.debug("read handshake bundle {} from client {}", header, context);
    context.setConnectionUuid((String) header.getParameter(BundleParameter.CONNECTION_UUID));
    header.getUuidPath().incPosition();
    final String uuid = header.getUuidPath().getCurrentElement();
    context.setUuid(uuid);
    awaitClassProvider(uuid);
    header.getUuidPath().add(driver.getUuid());
    if (debugEnabled) log.debug("uuid path=" + header.getUuidPath());
    header.clear();
    header.setParameter(BundleParameter.SYSTEM_INFO_PARAM, driver.getSystemInformation());
    header.setParameter(BundleParameter.DRIVER_UUID_PARAM, driver.getUuid());
    JMXServer jmxServer = driver.getInitializer().getJmxServer(false);
    header.setParameter(BundleParameter.DRIVER_MANAGEMENT_PORT, jmxServer != null ? jmxServer.getManagementPort() : -1);
    jmxServer = driver.getInitializer().getJmxServer(true);
    header.setParameter(BundleParameter.DRIVER_MANAGEMENT_PORT_SSL, jmxServer != null ? jmxServer.getManagementPort() : -1);
    final ClientMessage response = context.serializeBundle(bundle);
    context.offerMessageToSend(bundle, response);
  }

  /**
   * Called when a peer connection is established.
   * @param context the client connection context.
   * @throws Exception if any error occurs.
   */
  public void sendPeerHandshake(final AsyncClientContext context) throws Exception {
    final TaskBundle header = new JPPFTaskBundle();
    final TraversalList<String> uuidPath = new TraversalList<>();
    uuidPath.add(driver.getUuid());
    header.setUuidPath(uuidPath);
    if (debugEnabled) log.debug("sending handshake job, uuidPath={}", uuidPath);
    header.setUuid(JPPFUuid.normalUUID());
    header.setName("handshake job");
    header.setHandshake(true);
    header.setUuid(header.getName());
    header.setParameter(BundleParameter.CONNECTION_UUID, context.getConnectionUuid());
    header.setParameter(BundleParameter.IS_PEER, true);
    header.setParameter(BundleParameter.NODE_UUID_PARAM, driver.getUuid());
    final TypedProperties config = driver.getConfiguration();
    if (config.containsProperty(JPPFProperties.NODE_MAX_JOBS)) header.setParameter(BundleParameter.NODE_MAX_JOBS, config.get(JPPFProperties.NODE_MAX_JOBS));
    final JMXServer jmxServer = driver.getInitializer().getJmxServer(context.isSecure());
    header.setParameter(BundleParameter.NODE_MANAGEMENT_PORT_PARAM, jmxServer.getManagementPort());
    final PeerAttributesHandler peerHandler = driver.getAsyncNodeNioServer().getPeerHandler();
    final JPPFSystemInformation systemInformation = driver.getSystemInformation();
    systemInformation.getJppf().setInt(PeerAttributesHandler.PEER_TOTAL_THREADS, peerHandler.getTotalThreads());
    systemInformation.getJppf().setInt(PeerAttributesHandler.PEER_TOTAL_NODES, peerHandler.getTotalNodes());
    header.setParameter(BundleParameter.SYSTEM_INFO_PARAM, systemInformation);
    header.setSLA(null);
    header.setMetadata(null);
    final DataLocation dataProvider = IOHelper.serializeData(null);
    final ServerTaskBundleClient bundle = new ServerTaskBundleClient(header, dataProvider, Collections.<DataLocation>emptyList(), true);
    final ClientMessage request = context.serializeBundle(bundle);
    context.offerMessageToSend(bundle, request);
  }

  /**
   * Called when a handshake request from a client is received.
   * @param context the client connection context.
   * @param message the message that was received.
   * @throws Exception if any error occurs.
   */
  void peerHandshakeResponseReceived(final AsyncClientContext context, final ClientMessage message) throws Exception {
    final ServerTaskBundleClient bundleWrapper = context.deserializeBundle(message);
    final TaskBundle header = bundleWrapper.getJob();
    if (debugEnabled) log.debug("read peer handshake bundle {} from client {}", header, context);
    header.getUuidPath().incPosition();
    final String uuid = header.getUuidPath().getCurrentElement();
    context.setUuid(uuid);
    awaitClassProvider(uuid);
    final String driverUUID = driver.getUuid();
    header.getUuidPath().add(driverUUID);
    if (debugEnabled) log.debug("uuid path=" + header.getUuidPath());
    header.clear();
  }

  /**
   * Await for a t least one class server connection to the client with the specified uuid o be established. 
   * @param uuid the uuid of the client.
   * @throws Exception if any error occurs.
   */
  private void awaitClassProvider(final String uuid) throws Exception {
    final AsyncClientClassNioServer classServer = driver.getAsyncClientClassServer();
    List<AsyncClientClassContext> list = classServer.getProviderConnections(uuid);
    while ((list == null) || list.isEmpty()) {
      Thread.sleep(1L);
      list = classServer.getProviderConnections(uuid);
    }
  }
}
