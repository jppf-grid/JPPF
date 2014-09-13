/*
 * JPPF.
 * Copyright (C) 2005-2014 JPPF Team.
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

package org.jppf.server.nio.nodeserver;

import static org.jppf.server.nio.nodeserver.NodeTransition.*;
import static org.jppf.server.protocol.BundleParameter.*;
import static org.jppf.utils.StringUtils.build;

import org.jppf.management.*;
import org.jppf.nio.ChannelWrapper;
import org.jppf.node.protocol.TaskBundle;
import org.jppf.server.*;
import org.jppf.server.protocol.ServerTaskBundleNode;
import org.jppf.server.scheduler.bundle.*;
import org.jppf.utils.JPPFConfiguration;
import org.slf4j.*;

/**
 * This class implements the state of receiving a task bundle from the node as a
 * response to sending the initial bundle.
 * @author Laurent Cohen
 */
class WaitInitialBundleState extends NodeServerState {
  /**
   * Logger for this class.
   */
  protected static final Logger log = LoggerFactory.getLogger(WaitInitialBundleState.class);
  /**
   * Determines whether DEBUG logging level is enabled.
   */
  protected static final boolean debugEnabled = log.isDebugEnabled();

  /**
   * Initialize this state.
   * @param server the server that handles this state.
   */
  public WaitInitialBundleState(final NodeNioServer server) {
    super(server);
  }

  /**
   * Execute the action associated with this channel state.
   * @param channel the selection key corresponding to the channel and selector for this state.
   * @return a state transition as an <code>NioTransition</code> instance.
   * @throws Exception if an error occurs while transitioning to another state.
   * @see org.jppf.nio.NioState#performTransition(java.nio.channels.SelectionKey)
   */
  @Override
  public NodeTransition performTransition(final ChannelWrapper<?> channel) throws Exception  {
    AbstractNodeContext context = (AbstractNodeContext) channel.getContext();
    //if (debugEnabled) log.debug("exec() for " + channel);
    if (context.getMessage() == null) context.setMessage(context.newMessage());
    if (context.readMessage(channel)) {
      BundleResults received = context.deserializeBundle();
      TaskBundle bundle = received.bundle();
      boolean offline =  (bundle.getParameter(NODE_OFFLINE, false));
      if (offline) ((RemoteNodeContext) context).setOffline(true);
      else if (!bundle.isHandshake()) throw new IllegalStateException("handshake bundle expected.");
      if (debugEnabled) log.debug("read bundle for {], bundle={}", channel, bundle);
      String uuid = bundle.getParameter(NODE_UUID_PARAM);
      context.setUuid(uuid);
      Bundler bundler = server.getBundler().copy();
      JPPFSystemInformation systemInfo = bundle.getParameter(SYSTEM_INFO_PARAM);
      if (systemInfo != null) {
        context.setNodeInfo(systemInfo);
        if (bundler instanceof NodeAwareness) ((NodeAwareness) bundler).setNodeConfiguration(systemInfo);
      } else if (debugEnabled) log.debug("no system info received for node {}", channel);

      if (bundler instanceof ContextAwareness) ((ContextAwareness) bundler).setJPPFContext(server.getJPPFContext());
      bundler.setup();
      context.setBundler(bundler);
      boolean isPeer = bundle.getParameter(IS_PEER, false);
      context.setPeer(isPeer);
      if (JPPFConfiguration.getProperties().getBoolean("jppf.management.enabled", true) && (uuid != null) && !bundle.getParameter(NODE_OFFLINE, false)) {
        String host = getChannelHost(channel);
        int port = bundle.getParameter(NODE_MANAGEMENT_PORT_PARAM, -1);
        boolean sslEnabled = !channel.isLocal() && context.getSSLHandler() != null;
        if (debugEnabled) log.debug(String.format("configuring management for node @ {}:{}, secure="));
        byte type = isPeer ? JPPFManagementInfo.PEER : JPPFManagementInfo.NODE;
        if (channel.isLocal()) {
          type |= JPPFManagementInfo.LOCAL;
          DriverInitializer initializer = JPPFDriver.getInstance().getInitializer();
          JMXServer jmxServer = initializer.getJmxServer(sslEnabled);
          if (jmxServer != null) host = jmxServer.getManagementHost();
        }
        if (bundle.getParameter(NODE_PROVISIONING_MASTER, false)) type |= JPPFManagementInfo.MASTER;
        else if (bundle.getParameter(NODE_PROVISIONING_SLAVE, false)) type |= JPPFManagementInfo.SLAVE;
        JPPFManagementInfo info = new JPPFManagementInfo(host, port, uuid, type, sslEnabled);
        if (systemInfo != null) info.setSystemInfo(systemInfo);
        context.setManagementInfo(info);
      } else server.nodeConnected(context);
      if (bundle.getParameter(NODE_OFFLINE_OPEN_REQUEST, false)) return processOfflineReopen(received, context);
      return finalizeTransition(context);
    }
    return TO_WAIT_INITIAL;
  }

  /**
   * Finalize the state transition processing and return the traznsition to the next state.
   * @param context the context associated with the node channel.
   * @return the next transition to process.
   * @throws Exception if any error occurs.
   */
  private NodeTransition finalizeTransition(final AbstractNodeContext context) throws Exception {
    context.setMessage(null);
    context.setBundle(null);
    return context.isPeer() ? TO_IDLE_PEER : TO_IDLE;
  }

  /**
   * Process a request from the node to send the results of a job executed offline.
   * @param received holds the received bundle along with the tasks.
   * @param context the context associated with the node channel.
   * @return the next transition to process.
   * @throws Exception if any error occurs.
   */
  private NodeTransition processOfflineReopen(final BundleResults received, final AbstractNodeContext context) throws Exception {
    TaskBundle bundle = received.bundle();
    String jobUuid = bundle.getParameter(JOB_UUID);
    long id = bundle.getParameter(NODE_BUNDLE_ID);
    ServerTaskBundleNode nodeBundle = server.getOfflineNodeHandler().removeNodeBundle(jobUuid, id);
    // if the driver was restarted, we discard the results
    if (nodeBundle == null) return finalizeTransition(context);
    if (debugEnabled) log.debug(build("processing offline reopen with jobUuid=", jobUuid, ", id=", id, ", nodeBundle=", nodeBundle, ", node=", context.getChannel()));
    context.setBundle(nodeBundle);
    WaitingResultsState wrs = (WaitingResultsState) server.getFactory().getState(NodeState.WAITING_RESULTS);
    return wrs.process(received, context);
  }
}
