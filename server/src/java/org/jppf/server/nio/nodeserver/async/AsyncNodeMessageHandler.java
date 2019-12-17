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

package org.jppf.server.nio.nodeserver.async;

import static org.jppf.node.protocol.BundleParameter.*;
import static org.jppf.utils.StringUtils.build;

import java.net.*;
import java.nio.channels.SocketChannel;
import java.util.*;

import org.jppf.execute.ExecutorStatus;
import org.jppf.job.JobReturnReason;
import org.jppf.load.balancer.*;
import org.jppf.load.balancer.spi.JPPFBundlerFactory;
import org.jppf.management.*;
import org.jppf.node.protocol.*;
import org.jppf.scheduling.JPPFSchedule;
import org.jppf.server.*;
import org.jppf.server.nio.AbstractTaskBundleMessage;
import org.jppf.server.nio.nodeserver.*;
import org.jppf.server.protocol.*;
import org.jppf.utils.*;
import org.jppf.utils.configuration.JPPFProperties;
import org.jppf.utils.stats.*;
import org.slf4j.*;

/**
 * Handles messages received from and to send to the nodes.
 * @author Laurent Cohen
 */
public class AsyncNodeMessageHandler {
  /**
   * Logger for this class.
   */
  private static final Logger log = LoggerFactory.getLogger(AsyncNodeMessageHandler.class);
  /**
   * Determines whether the debug level is enabled in the log configuration, without the cost of a method call.
   */
  private static final boolean debugEnabled = log.isDebugEnabled();
  /**
   * Reference to the singleton JPPF driver.
   */
  private final JPPFDriver driver;
  /**
   * Whether to resolve the nodes' ip addresses into host names.
   */
  protected final boolean resolveIPs;

  /**
   * COnstruct this object.
   * @param driver reference to the driver.
   */
  public AsyncNodeMessageHandler(final JPPFDriver driver) {
    this.driver = driver;
    this.resolveIPs = driver.getConfiguration().get(JPPFProperties.RESOLVE_ADDRESSES);
  }

  /**
   * Send a hanshake bundle to a node.
   * @param context the channel sending the bundle.
   * @param bundle the task bundle to send.
   * @throws Exception if any error occurs.
   */
  public void sendHandshakeBundle(final AsyncNodeContext context, final ServerTaskBundleNode bundle) throws Exception {
    final AbstractTaskBundleMessage msg = context.serializeBundle(bundle);
    context.offerMessageToSend(bundle, msg);
  }

  /**
   * Called when a job was sent to a node.
   * @param context the channel that sent theb bundle.
   * @param nodeBundle the task bundle to send.
   * @throws Exception if any error occurs.
   */
  void bundleSent(final AsyncNodeContext context, final ServerTaskBundleNode nodeBundle)  throws Exception {
    if (nodeBundle != null) {
      final TaskBundle job = nodeBundle.getJob();
      if (job != null) {
        final JobSLA sla = job.getSLA();
        if (sla != null) {
          final JPPFSchedule schedule = sla.getDispatchExpirationSchedule();
          final AsyncNodeNioServer server = context.getServer();
          if (schedule != null) {
            final NodeDispatchTimeoutAction action = new NodeDispatchTimeoutAction(server.getOfflineNodeHandler(), nodeBundle, context.isOffline() ? null : context);
            server.getDispatchExpirationHandler().scheduleAction(ServerTaskBundleNode.makeKey(nodeBundle), schedule, action);
          }
        } else log.warn("null sla for {} of {} with {}", job, nodeBundle, context);
      } else log.warn("null job for {} with {}", nodeBundle, context);
    } else log.warn("null bundle for {}", context);
    context.setWriteMessage(null);
    if (context.isOffline()) processOfflineRequest(context, nodeBundle);
  }

  /**
   * Called when a hanshake response is received from a node.
   * @param context the channel that received the repsonse.
   * @param message the handshake response.
   * @throws Exception if any error occurs.
   */
  @SuppressWarnings("deprecation")
  public void handshakeReceived(final AsyncNodeContext context, final AbstractTaskBundleMessage message)  throws Exception {
    final NodeBundleResults received = context.deserializeBundle(message);
    if (debugEnabled) log.debug("received handshake response for channel {} : {}", context, received);
    final TaskBundle bundle = received.bundle();
    final boolean offline =  bundle.getParameter(NODE_OFFLINE, false);
    if (offline) {
      context.setOffline(true);
      context.setMaxJobs(1);
    } else {
      updateMaxJobs(context, bundle);
      final Boolean acceptsNewJobs = bundle.getParameter(NODE_ACCEPTS_NEW_JOBS);
      if (acceptsNewJobs != null) context.setAcceptingNewJobs(acceptsNewJobs);
      if (!bundle.isHandshake()) throw new IllegalStateException("handshake bundle expected.");
    }
    if (debugEnabled) log.debug("read bundle for {}, bundle={}", context, bundle);
    final String uuid = bundle.getParameter(NODE_UUID_PARAM);
    context.setUuid(uuid);
    final JPPFSystemInformation systemInfo = bundle.getParameter(SYSTEM_INFO_PARAM);
    context.setNodeIdentifier(getNodeIdentifier(context.getServer().getBundlerFactory(), context, systemInfo));
    if (debugEnabled) log.debug("nodeID = {} for node = {}", context.getNodeIdentifier(), context);
    final boolean isPeer = bundle.getParameter(IS_PEER, false);
    context.setPeer(isPeer);
    if (systemInfo != null) {
      systemInfo.getJppf().setBoolean("jppf.peer.driver", isPeer);
      systemInfo.getJppf().set(JPPFProperties.NODE_IDLE, true);
      context.setNodeInfo(systemInfo, false);
      if (log.isTraceEnabled()) log.trace("node network info:\nipv4: {}\nipv6: {}", systemInfo.getNetwork().getString("ipv4.addresses"), systemInfo.getNetwork().getString("ipv6.addresses"));
    } else if (debugEnabled) log.debug("no system info received for node {}", context);
    final int port = bundle.getParameter(NODE_MANAGEMENT_PORT_PARAM, -1);
    if (debugEnabled) log.debug("management port = {} for node = {}", port, context);
    String host = bundle.getParameter(NODE_MANAGEMENT_HOST_PARAM, null);
    HostIP hostIP = null;
    if (host == null) {
      host = getChannelHost(context);
      hostIP = context.isLocal() ? new HostIP(host, host) : resolveHost(context);
    } else hostIP = NetworkUtils.getHostIP(host);
    final boolean sslEnabled = !context.isLocal() && context.getSSLHandler() != null;
    final boolean hasJmx = driver.getConfiguration().get(JPPFProperties.MANAGEMENT_ENABLED);
    final String masterUuid = bundle.getParameter(NODE_PROVISIONING_MASTER_UUID);
    int type = isPeer ? JPPFManagementInfo.PEER : JPPFManagementInfo.NODE;
    if (hasJmx && (uuid != null) && !offline && (port >= 0)) {
      if (context.isLocal()) {
        type |= JPPFManagementInfo.LOCAL;
        final DriverInitializer initializer = driver.getInitializer();
        final JMXServer jmxServer = initializer.getJmxServer(sslEnabled);
        if (jmxServer != null) host = jmxServer.getManagementHost();
      }
      if (bundle.getParameter(NODE_PROVISIONING_MASTER, false)) type |= JPPFManagementInfo.MASTER;
      else if (bundle.getParameter(NODE_PROVISIONING_SLAVE, false)) type |= JPPFManagementInfo.SLAVE;
      if (bundle.getParameter(NODE_DOTNET_CAPABLE, false)) type |= JPPFManagementInfo.DOTNET;
      if ((systemInfo != null) && (systemInfo.getJppf().get(JPPFProperties.NODE_ANDROID))) type |= JPPFManagementInfo.ANDROID;
      final JPPFManagementInfo info = new JPPFManagementInfo(hostIP.hostName(), hostIP.ipAddress(), port, uuid, type, sslEnabled, masterUuid);
      if (debugEnabled) log.debug("configuring management for hostIP={} : {}", hostIP, info);
      if (systemInfo != null) {
        info.setSystemInfo(systemInfo);
        if (debugEnabled) log.debug("node has following configuration:\n{}", systemInfo.getJppf());
      }
      context.setManagementInfo(info);
    } else {
      if (offline || (port < 0)) {
        final JPPFManagementInfo info = new JPPFManagementInfo(hostIP.hostName(), hostIP.ipAddress(), -1, context.getUuid(), type, sslEnabled, masterUuid);
        if (systemInfo != null) info.setSystemInfo(systemInfo);
        context.setManagementInfo(info);
      }
      context.getServer().nodeConnected(context);
    }
    context.getServer().putConnection(context);
    if (bundle.getParameter(NODE_OFFLINE_OPEN_REQUEST, false)) processOfflineReopen(received, context);
  }

  /**
   * Called when job results are received from a node.
   * @param context the channel that received the response.
   * @param message the received results.
   * @throws Exception if any error occurs.
   */
  public void resultsReceived(final AsyncNodeContext context, final AbstractTaskBundleMessage message)  throws Exception {
    if (debugEnabled) log.debug("node {} received {}", context, message);
    final NodeBundleResults received = context.deserializeBundle(message);
    process(received, context);
  }

  /**
   * Resolve the host name for the specified channel.
   * @param context the channel from which to get the host information.
   * @return a representation of the host name/ip address pair.
   * @throws Exception if any error occurs.
   */
  private HostIP resolveHost(final AsyncNodeContext context) throws Exception {
    String host = getChannelHost(context);
    String ip = host;
    final boolean resolveFromSysInfo = driver.getConfiguration().getBoolean("jppf.resolve.node.host.from.sysinfo", false);
    if (!resolveFromSysInfo) {
      try {
        final InetAddress addr = InetAddress.getByName(host);
        ip = addr.getHostAddress();
        if (host.equals(ip)) host = addr.getHostName();
        if (!host.equals(ip)) {
          if (log.isTraceEnabled()) log.trace("resolved host from reverse DNS lookup: host={}, ip={}", host, ip);
          return new HostIP(host, ip);
        }
      } catch (@SuppressWarnings("unused") final UnknownHostException ignore) {
      }
    }
    // if host couldn't be resolved via reverse DNS lookup
    final JPPFSystemInformation info = context.getSystemInformation();
    if (info != null) {
      for (final HostIP hostIP: info.parseIPV4Addresses()) {
        if (resolveFromSysInfo || (host.equals(hostIP.hostName()) || host.equals(hostIP.ipAddress()))) {
          if (log.isTraceEnabled()) log.trace("resolved host from system info: {}", hostIP);
          return hostIP;
        }
      }
      for (final HostIP hostIP: info.parseIPV6Addresses()) {
        if (resolveFromSysInfo || (host.equals(hostIP.hostName()) || host.equals(hostIP.ipAddress()))) {
          if (log.isTraceEnabled()) log.trace("resolved host from system info: {}", hostIP);
          return hostIP;
        }
      }
    }
    final HostIP hostIP = resolveIPs ? NetworkUtils.getHostIP(host) : new HostIP(host, host);
    if (log.isTraceEnabled()) log.trace("{}: {}", (resolveIPs ? "resolved host from NetworkUtils.getHostIP()" : "unresolved host"), hostIP);
    return hostIP;
  }

  /**
   * Process an offline request from the node.
   * @param context the current context associated with the channel.
   * @param nodeBundle the task bundle sent.
   * @throws Exception if any error occurs.
   */
  private static void processOfflineRequest(final AsyncNodeContext context, final ServerTaskBundleNode nodeBundle) throws Exception {
    if (debugEnabled) log.debug("processing offline request, nodeBundle={} for node={}", nodeBundle, context);
    context.getServer().getOfflineNodeHandler().addNodeBundle(nodeBundle);
    context.cleanup();
    context.getServer().closeConnection(context);
  }

  /**
   * Process a request from the node to send the results of a job executed offline.
   * @param received holds the received bundle along with the tasks.
   * @param context the context associated with the node channel.
   * @throws Exception if any error occurs.
   */
  private void processOfflineReopen(final NodeBundleResults received, final AsyncNodeContext context) throws Exception {
    final TaskBundle bundle = received.bundle();
    final String jobUuid = bundle.getParameter(JOB_UUID);
    final long id = bundle.getBundleId();
    final ServerTaskBundleNode nodeBundle = context.getServer().getOfflineNodeHandler().removeNodeBundle(jobUuid, id);
    if (nodeBundle == null) return;
    if (debugEnabled) log.debug(build("processing offline reopen with jobUuid=", jobUuid, ", id=", id, ", nodeBundle=", nodeBundle, ", node=", context));
    context.addJobEntry(nodeBundle);
    process(received, context);
    if (bundle.getParameter(CLOSE_COMMAND, false)) {
      context.cleanup();
      context.getServer().closeConnection(context);
    }
  }

  /**
   * Process the bundle that was just read.
   * @param received holds the received bundle along with the tasks.
   * @param context the channel from which the bundle was read.
   * @throws Exception if any error occurs.
   */
  private void process(final NodeBundleResults received, final AsyncNodeContext context) throws Exception {
    final TaskBundle bundle = received.first();
    final ServerTaskBundleNode nodeBundle = context.removeJobEntry(bundle.getUuid(), bundle.getBundleId());
    final ServerJob job = nodeBundle.getServerJob();
    boolean mustProcess = true;
    job.getLock().lock();
    try {
      if (job.isCancelled()) mustProcess = false;
      else {
        for (final ServerTask task: nodeBundle.getTaskList()) task.setReturnedFromNode(true);
      }
    } finally {
      job.getLock().unlock();
    }
    context.getServer().getDispatchExpirationHandler().cancelAction(ServerTaskBundleNode.makeKey(nodeBundle), false);

    if (mustProcess) {
      boolean requeue = false;
      try {
        final TaskBundle newBundle = received.bundle();
        if (debugEnabled) log.debug("read bundle " + newBundle + " from node " + context);
        requeue = processResults(context, received, nodeBundle);
      } catch (final Throwable t) {
        log.error(t.getMessage(), t);
        nodeBundle.setJobReturnReason(JobReturnReason.DRIVER_PROCESSING_ERROR);
        nodeBundle.resultsReceived(t);
      }
      if (requeue) nodeBundle.resubmit();
    }
    if (!context.isOffline()) updateMaxJobs(context, bundle);
    if (context.getCurrentNbJobs() < context.getMaxJobs()) {
      if (debugEnabled) log.debug("updating execution status to ACTIVE for {}", context);
      context.setExecutionStatus(ExecutorStatus.ACTIVE);
    }
  }

  /**
   * Process the results received from the node.
   * @param context the context associated witht he node channel.
   * @param received groups the job header and resuls of the tasks.
   * @param nodeBundle the initial node bundle that was sent.
   * @return A boolean requeue indicator.
   * @throws Exception if any error occurs.
   */
  private boolean processResults(final AsyncNodeContext context, final NodeBundleResults received, final ServerTaskBundleNode nodeBundle) throws Exception {
    final TaskBundle newBundle = received.bundle();
    Bundler<?> bundler = context.getBundler();
    final ServerJob job = nodeBundle.getClientJob();
    final Throwable t = newBundle.getParameter(NODE_EXCEPTION_PARAM);
    if (t != null) {
      if (debugEnabled) log.debug("node {} returned exception parameter in the header for bundle {} : {}", context, newBundle, ExceptionUtils.getMessage(t));
      nodeBundle.setJobReturnReason(JobReturnReason.NODE_PROCESSING_ERROR);
      nodeBundle.resultsReceived(t);
    } else if (job.isCancelled()) {
      if (debugEnabled) log.debug("received bundle with {} tasks for already cancelled job: {}", received.second().size(), received.bundle());
      if (!nodeBundle.isCancelled()) {
        if (debugEnabled) log.debug("node bundle was not cancelled: {}", nodeBundle);
        job.cancelDispatch(nodeBundle);
      }
    } else {
      if (debugEnabled) log.debug("received bundle with {} tasks, taskCount={}: {}", received.second().size(), newBundle.getTaskCount(), received.bundle());
      if (nodeBundle.getJobReturnReason() == null) nodeBundle.setJobReturnReason(JobReturnReason.RESULTS_RECEIVED);
      if (!nodeBundle.isExpired()) {
        Set<Integer> resubmitSet = null;
        final int[] resubmitPositions = newBundle.getParameter(RESUBMIT_TASK_POSITIONS, null);
        if (debugEnabled) log.debug("resubmitPositions = {} for {}", resubmitPositions, newBundle);
        if (resubmitPositions != null) {
          resubmitSet = new HashSet<>();
          for (int n: resubmitPositions) resubmitSet.add(n);
          if (debugEnabled) log.debug("resubmitSet = {} for {}", resubmitSet, newBundle);
        }
        int count = 0;
        for (final ServerTask task: nodeBundle.getTaskList()) {
          if ((resubmitSet != null) && resubmitSet.contains(task.getPosition())) {
            if (task.incResubmitCount() <= task.getMaxResubmits()) {
              task.resubmit();
              count++;
            }
          }
        }
        if (count > 0) context.updateStatsUponTaskResubmit(count);
      } else if (debugEnabled) log.debug("bundle has expired: {}", nodeBundle);
      if (debugEnabled) log.debug("nodeBundle={}", nodeBundle);
      bundler = updateBundlerAndStats(context, bundler, nodeBundle, newBundle);
      nodeBundle.resultsReceived(received.data());
      if (debugEnabled) log.debug("updated stats for {}", context);
    }
    final JPPFSystemInformation systemInfo = newBundle.getParameter(SYSTEM_INFO_PARAM);
    if (systemInfo != null) {
      context.setNodeInfo(systemInfo, true);
      if (bundler instanceof ChannelAwareness) ((ChannelAwareness) bundler).setChannelConfiguration(systemInfo);
    }
    return newBundle.isRequeue();
  }

  /**
   * Called when a node sends a notification or alert.
   * @param context the channel that sent the notification.
   * @param notification the notification to process.
   * @throws Exception if any error occurs.
   */
  void notificationReceived(final AsyncNodeContext context, final NotificationBundle notification)  throws Exception {
    switch(notification.getNotificationType()) {
      case THROTTLING:
        final Boolean accepting = notification.getParameter(NODE_ACCEPTS_NEW_JOBS, true);
        if (debugEnabled) log.debug("received notification that the node {} new jobs, node {}", accepting ? "accepts" : "does not accept", context);
        context.setAcceptingNewJobs(accepting);
        break;
    }
  }

  /**
   * 
   * @param context the context for which to update the bundler
   * @param currentBundler the current bundler for this context.
   * @param nodeBundle the bundle that was dispatched to the node.
   * @param newBundle the header of the bundle received from the node.
   * @return the updated bundler;
   */
  private Bundler<?> updateBundlerAndStats(final AsyncNodeContext context, final Bundler<?> currentBundler, final ServerTaskBundleNode nodeBundle, final TaskBundle newBundle) {
    final long elapsed = System.nanoTime() - nodeBundle.getJob().getExecutionStartTime();
    final Bundler<?> bundler = (currentBundler == null) ? context.checkBundler(context.getServer().getBundlerFactory(), context.getServer().getJPPFContext()) : currentBundler;
    if (bundler instanceof BundlerEx) {
      final long accumulatedTime = newBundle.getParameter(NODE_BUNDLE_ELAPSED_PARAM, -1L);
      BundlerHelper.updateBundler((BundlerEx<?>) bundler, newBundle.getTaskCount(), elapsed, accumulatedTime, elapsed - newBundle.getNodeExecutionTime());
    } else BundlerHelper.updateBundler(bundler, newBundle.getTaskCount(), elapsed);
    if (debugEnabled) log.debug("updated bundler for {}", context);
    context.getServer().getBundlerHandler().storeBundler(context.getNodeIdentifier(), bundler, context.getBundlerAlgorithm());
    updateStats(newBundle.getTaskCount(), elapsed / 1_000_000L, newBundle.getNodeExecutionTime() / 1_000_000L);
    return bundler;
  }

  /**
   * Update the statistcis from the received results.
   * @param nbTasks number of tasks received.
   * @param elapsed server/node round trip time.
   * @param elapsedInNode time spent in the node.
   */
  private void updateStats(final int nbTasks, final long elapsed, final long elapsedInNode) {
    final JPPFStatistics stats = driver.getStatistics();
    stats.addValue(JPPFStatisticsHelper.TASK_DISPATCH, nbTasks);
    stats.addValues(JPPFStatisticsHelper.EXECUTION, elapsed, nbTasks);
    stats.addValues(JPPFStatisticsHelper.NODE_EXECUTION, elapsedInNode, nbTasks);
    stats.addValues(JPPFStatisticsHelper.TRANSPORT_TIME, elapsed - elapsedInNode, nbTasks);
  }

  /**
   * 
   * @param context the context for which to update the max number of jobs.
   * @param bundle the header of the bundle received from the node.
   */
  private void updateMaxJobs(final AsyncNodeContext context, final TaskBundle bundle) {
    final int n = context.getMaxJobs();
    final Integer newMaxJobs = bundle.getParameter(NODE_MAX_JOBS);
    final int maxJobs = (newMaxJobs == null) ? driver.getConfiguration().get(JPPFProperties.NODE_MAX_JOBS) : newMaxJobs;
    if (debugEnabled) log.debug("n={}, newMaxJobs={}, computed maxJobs={}, context={}", n, newMaxJobs, maxJobs, context);
    if (maxJobs > 0) context.setMaxJobs(maxJobs);
  }

  /**
   * Compute a repeatable unique identifier for a node, which can be reused over node restarts.
   * @param factory bundler (load-balancer) factory.
   * @param channel the channel that carries the host information.
   * @param info the system information for the node.
   * @return a pair of string representing the clear string (keft side) and resulting unique string identifier for the node (right side).
   * @throws Exception if any error occurs.
   */
  private static Pair<String, String> getNodeIdentifier(final JPPFBundlerFactory factory, final BaseNodeContext channel, final JPPFSystemInformation info) throws Exception {
    if (factory.getPersistence() == null) return null;
    final StringBuilder sb = new StringBuilder();
    final String ip = NetworkUtils.getNonLocalHostAddress();
    sb.append('[').append(ip == null ? "localhost" : ip);
    if (channel.getSocketChannel() != null) {
      final SocketChannel ch = channel.getSocketChannel();
      sb.append(':').append(ch.socket().getLocalPort()).append(']');
      final InetSocketAddress isa = (InetSocketAddress) ch.getRemoteAddress();
      sb.append(isa.getAddress().getHostAddress());
    } else if (channel.isLocal()) {
      sb.append( "local_channel").append(']');
    }
    final TypedProperties jppf = info.getJppf();
    final boolean master = jppf.get(JPPFProperties.PROVISIONING_MASTER);
    final boolean slave = jppf.get(JPPFProperties.PROVISIONING_SLAVE);
    if (master || slave) {
      sb.append(master ? "master" : "slave");
      sb.append(jppf.get(JPPFProperties.PROVISIONING_SLAVE_PATH_PREFIX));
      if (slave) sb.append(jppf.get(JPPFProperties.PROVISIONING_SLAVE_ID));
    }
    final String s = sb.toString();
    return new Pair<>(s, CryptoUtils.computeHash(s, factory.getHashAlgorithm()));
  }

  /**
   * Extract the remote host name from the specified channel.
   * @param context the channel.
   * @return the remote host name as a string.
   * @throws Exception if any error occurs.
   */
  private static String getChannelHost(final BaseNodeContext context) throws Exception {
    if (!context.isLocal()) {
      final SocketChannel ch = context.getSocketChannel();
      return  ((InetSocketAddress) (ch.getRemoteAddress())).getHostString();
    }
    else  return "localhost";
  }
}
