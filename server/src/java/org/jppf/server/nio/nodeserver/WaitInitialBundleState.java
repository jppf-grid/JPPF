/*
 * JPPF.
 * Copyright (C) 2005-2016 JPPF Team.
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

import static org.jppf.node.protocol.BundleParameter.*;
import static org.jppf.server.nio.nodeserver.NodeTransition.*;
import static org.jppf.utils.StringUtils.build;

import java.net.*;
import java.nio.channels.SocketChannel;

import org.jppf.load.balancer.*;
import org.jppf.management.*;
import org.jppf.nio.*;
import org.jppf.node.protocol.*;
import org.jppf.server.*;
import org.jppf.server.protocol.ServerTaskBundleNode;
import org.jppf.utils.*;
import org.jppf.utils.configuration.JPPFProperties;
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
  protected static final boolean debugEnabled = LoggingUtils.isDebugEnabled(log);
  /**
   * Whether to resolve the nodes' ip addresses into host names.
   */
  protected final boolean resolveIPs = JPPFConfiguration.get(JPPFProperties.RESOLVE_ADDRESSES);

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
   */
  @SuppressWarnings("deprecation")
  @Override
  public NodeTransition performTransition(final ChannelWrapper<?> channel) throws Exception  {
    AbstractNodeContext context = (AbstractNodeContext) channel.getContext();
    //if (debugEnabled) log.debug("exec() for " + channel);
    if (context.getMessage() == null) context.setMessage(context.newMessage());
    if (context.readMessage(channel)) {
      if (debugEnabled) log.debug("received handshake response for channel id = {}", context.getChannel().getId());
      BundleResults received = context.deserializeBundle();
      TaskBundle bundle = received.bundle();
      boolean offline =  bundle.getParameter(NODE_OFFLINE, false);
      if (offline) ((RemoteNodeContext) context).setOffline(true);
      else if (!bundle.isHandshake()) throw new IllegalStateException("handshake bundle expected.");
      if (debugEnabled) log.debug("read bundle for {}, bundle={}", channel, bundle);
      String uuid = bundle.getParameter(NODE_UUID_PARAM);
      context.setUuid(uuid);
      Bundler bundler = server.getBundlerFactory().newBundler();
      boolean isPeer = bundle.getParameter(IS_PEER, false);
      context.setPeer(isPeer);
      JPPFSystemInformation systemInfo = bundle.getParameter(SYSTEM_INFO_PARAM);
      if (systemInfo != null) {
        systemInfo.getJppf().setBoolean("jppf.peer.driver", isPeer);
        systemInfo.getJppf().set(JPPFProperties.NODE_IDLE, true);
        context.setNodeInfo(systemInfo, false);
        if (bundler instanceof ChannelAwareness) ((ChannelAwareness) bundler).setChannelConfiguration(systemInfo);
        else if (bundler instanceof NodeAwareness) ((NodeAwareness) bundler).setNodeConfiguration(systemInfo);
      } else if (debugEnabled) log.debug("no system info received for node {}", channel);

      if (bundler instanceof ContextAwareness) ((ContextAwareness) bundler).setJPPFContext(server.getJPPFContext());
      bundler.setup();
      context.setBundler(bundler);
      int port = bundle.getParameter(NODE_MANAGEMENT_PORT_PARAM, -1);
      boolean hasJmx = context.isSecure() ? JPPFConfiguration.get(JPPFProperties.MANAGEMENT_SSL_ENABLED) : JPPFConfiguration.get(JPPFProperties.MANAGEMENT_ENABLED);
      String host = getChannelHost(channel);
      HostIP hostIP = channel.isLocal() ? new HostIP(host, host) : resolveHost(channel);
      boolean sslEnabled = !channel.isLocal() && context.getSSLHandler() != null;
      int type = isPeer ? JPPFManagementInfo.PEER : JPPFManagementInfo.NODE;
      if (hasJmx && (uuid != null) && !offline && (port >= 0)) {
        if (channel.isLocal()) {
          type |= JPPFManagementInfo.LOCAL;
          DriverInitializer initializer = JPPFDriver.getInstance().getInitializer();
          JMXServer jmxServer = initializer.getJmxServer(sslEnabled);
          if (jmxServer != null) host = jmxServer.getManagementHost();
        }
        if (bundle.getParameter(NODE_PROVISIONING_MASTER, false)) type |= JPPFManagementInfo.MASTER;
        else if (bundle.getParameter(NODE_PROVISIONING_SLAVE, false)) type |= JPPFManagementInfo.SLAVE;
        if (bundle.getParameter(NODE_DOTNET_CAPABLE, false)) type |= JPPFManagementInfo.DOTNET;
        if ((systemInfo != null) && (systemInfo.getJppf().get(JPPFProperties.NODE_ANDROID))) type |= JPPFManagementInfo.ANDROID;
        JPPFManagementInfo info = new JPPFManagementInfo(hostIP, port, uuid, type, sslEnabled);
        if (debugEnabled) log.debug(String.format("configuring management for node %s", info));
        if (systemInfo != null) info.setSystemInfo(systemInfo);
        context.setManagementInfo(info);
      } else {
        if (offline || (port < 0)) {
          JPPFManagementInfo info = new JPPFManagementInfo(hostIP, -1, context.getUuid(), type, sslEnabled);
          if (systemInfo != null) info.setSystemInfo(systemInfo);
          context.setManagementInfo(info);
        }
        server.nodeConnected(context);
      }
      server.putConnection(context);
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
   * Resolve the host name for the specified channel.
   * @param channel the channel from which to get the host information.
   * @return a representation of the host name/ip address pair.
   * @throws Exception if any error occurs.
   * @since 5.0.2
   */
  private HostIP resolveHost(final ChannelWrapper<?> channel) throws Exception {
    String host = getChannelHost(channel);
    String ip = host;
    try {
      InetAddress addr = InetAddress.getByName(host);
      ip = addr.getHostAddress();
      if (host.equals(ip)) host = addr.getHostName();
      if (!host.equals(ip)) {
        if (log.isTraceEnabled()) log.trace("resolved host from reverse DNS lookup: host={}, ip={}", host, ip);
        return new HostIP(host, ip);
      }
    } catch (UnknownHostException ignore) {
    }
    // if host couldn't be resolved via reverse DNS lookup
    JPPFSystemInformation info = ((AbstractNodeContext) channel.getContext()).getSystemInformation();
    if (info != null) {
      for (HostIP hostIP: info.parseIPV4Addresses()) {
        if (host.equals(hostIP.hostName()) || host.equals(hostIP.ipAddress())) {
          if (log.isTraceEnabled()) log.trace("resolved host from system info: {}", hostIP);
          return hostIP;
        }
      }
      for (HostIP hostIP: info.parseIPV6Addresses()) {
        if (host.equals(hostIP.hostName()) || host.equals(hostIP.ipAddress())) {
          if (log.isTraceEnabled()) log.trace("resolved host from system info: {}", hostIP);
          return hostIP;
        }
      }
    }
    HostIP hostIP = resolveIPs ? NetworkUtils.getHostIP(host) : new HostIP(host, host);
    if (log.isTraceEnabled()) log.trace("{}: {}", (resolveIPs ? "resolved host from NetworkUtils.getHostIP()" : "unresolved host"), hostIP);
    return hostIP;
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
    NodeTransition transition = wrs.process(received, context);
    boolean closeCommand = bundle.getParameter(BundleParameter.CLOSE_COMMAND, false);
    if (closeCommand) {
      context.cleanup();
      return null;
    }
    return transition;
  }

  /**
   * Extract the remote host name from the specified channel.
   * @param channel the channel that carries the host information.
   * @return the remote host name as a string.
   * @throws Exception if any error occurs.
   */
  private String getChannelHost(final ChannelWrapper<?> channel) throws Exception {
    if (channel instanceof SelectionKeyWrapper) {
      SelectionKeyWrapper skw = (SelectionKeyWrapper) channel;
      SocketChannel ch = (SocketChannel) skw.getChannel().channel();
      return  ((InetSocketAddress) (ch.getRemoteAddress())).getHostString();
    }
    return null;
  }
}
