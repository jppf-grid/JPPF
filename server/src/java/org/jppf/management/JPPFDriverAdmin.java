/*
 * JPPF.
 * Copyright (C) 2005-2015 JPPF Team.
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

package org.jppf.management;

import static org.jppf.utils.stats.JPPFStatisticsHelper.*;

import java.util.*;

import org.jppf.load.balancer.*;
import org.jppf.load.balancer.spi.JPPFBundlerFactory;
import org.jppf.node.policy.ExecutionPolicy;
import org.jppf.server.*;
import org.jppf.server.nio.nodeserver.*;
import org.jppf.utils.*;
import org.jppf.utils.configuration.JPPFProperties;
import org.jppf.utils.stats.*;
import org.slf4j.*;

/**
 * Instances of this class encapsulate the administration functionalities for a JPPF driver.
 * @author Laurent Cohen
 * @exclude
 */
public class JPPFDriverAdmin implements JPPFDriverAdminMBean {
  /**
   * Explicit serialVersionUID.
   */
  private static final long serialVersionUID = 1L;
  /**
   * Logger for this class.
   */
  private static final Logger log = LoggerFactory.getLogger(JPPFDriverAdmin.class);
  /**
   * Determines whether debug log statements are enabled.
   */
  private static boolean debugEnabled = LoggingUtils.isDebugEnabled(log);
  /**
   * Base name used for localization lookups.
   */
  private static final String I18N_BASE = "org.jppf.server.i18n.server_messages";
  /**
   * Reference to the driver.
   */
  private final JPPFDriver driver = JPPFDriver.getInstance();
  /**
   * The latest load-balancing information set via JMX.
   */
  private LoadBalancingInformation currentLoadBalancingInformation;
  /**
   * Synchronization lock.
   */
  private final Object loadBalancingInformationLock = new Object();
  /**
   *
   */
  private final NodeSelectionHelper selectionHelper = new NodeSelectionHelper();

  @Override
  public Integer nbNodes() throws Exception {
    return nbNodes(null);
  }

  @Override
  public Integer nbNodes(final NodeSelector selector) throws Exception {
    Collection<JPPFManagementInfo> nodes = nodesInformation(selector);
    return nodes == null ? -1 : nodes.size();
  }

  @Override
  public Collection<JPPFManagementInfo> nodesInformation() {
    return nodesInformation(null);
  }

  @Override
  public Collection<JPPFManagementInfo> nodesInformation(final NodeSelector selector) {
    return nodesInformation(selector, false);
  }

  @Override
  public Collection<JPPFManagementInfo> nodesInformation(final NodeSelector selector, final boolean includePeers) {
    try {
      Set<AbstractNodeContext> nodes = selectionHelper.getChannels(selector == null ? NodeSelector.ALL_NODES : selector, includePeers);
      List<JPPFManagementInfo> list = new ArrayList<>(nodes.size());
      for (AbstractNodeContext context : nodes) {
        JPPFManagementInfo info = context.getManagementInfo();
        if (info != null) list.add(info);
      }
      return list;
    } catch(Exception e) {
      log.error(e.getMessage(), e);
      return null;
    }
  }

  @Override
  public Integer nbIdleNodes() throws Exception {
    return nbIdleNodes(null);
  }

  @Override
  public Integer nbIdleNodes(final NodeSelector selector) throws Exception {
    Collection<JPPFManagementInfo> nodes = idleNodesInformation(selector);
    return nodes == null ? -1 : nodes.size();
  }

  @Override
  public Collection<JPPFManagementInfo> idleNodesInformation() throws Exception {
    return idleNodesInformation(null);
  }

  @Override
  public Collection<JPPFManagementInfo> idleNodesInformation(final NodeSelector selector) {
    try {
      Set<AbstractNodeContext> nodes = selectionHelper.getChannels(selector == null ? NodeSelector.ALL_NODES : selector);
      List<JPPFManagementInfo> list = new ArrayList<>(nodes.size());
      for (AbstractNodeContext context : nodes) {
        if (getNodeNioServer().isIdle(context.getChannel())) {
          JPPFManagementInfo info = context.getManagementInfo();
          if (info != null) list.add(info);
        }
      }
      return list;
    } catch(Exception e) {
      log.error(e.getMessage(), e);
      return null;
    }
  }

  @Override
  public JPPFStatistics statistics() throws Exception {
    try {
      JPPFStatistics  stats = driver.getStatistics().copy();
      if (log.isTraceEnabled()) log.trace("stats request = " + stats);
      return stats;
    } catch(Throwable e) {
      log.error(e.getMessage(), e);
      return null;
    }
  }

  @Override
  public String changeLoadBalancerSettings(final String algorithm, final Map<Object, Object> parameters) throws Exception {
    try {
      if (algorithm == null) return "Error: no algorithm specified (null value)";
      NodeNioServer server = getNodeNioServer();
      JPPFBundlerFactory factory = server.getBundlerFactory();
      if (!factory.getBundlerProviderNames().contains(algorithm)) return "Error: unknown algorithm '" + algorithm + '\'';
      TypedProperties props = new TypedProperties(parameters);
      synchronized(loadBalancingInformationLock) {
        currentLoadBalancingInformation = new LoadBalancingInformation(algorithm, props, loadBalancerInformation().getAlgorithmNames());
      }
      Bundler bundler = factory.createBundler(algorithm, props);
      server.setBundler(bundler);
      return localize("load.balancing.updated") + " (" + algorithm + ")";
    } catch(Exception e) {
      log.error(e.getMessage(), e);
      return "Error : " + e.getMessage();
    }
  }

  @Override
  public String restartShutdown(final Long shutdownDelay, final Long restartDelay) throws Exception {
    try {
      if (debugEnabled) log.debug("request to restart/shutdown this driver, shutdownDelay=" + shutdownDelay + ", restartDelay=" + restartDelay);
      boolean restart = restartDelay >= 0L;
      driver.initiateShutdownRestart(shutdownDelay, restart, restartDelay);
      return localize("request.acknowledged");
    } catch(Exception e) {
      log.error(e.getMessage(), e);
      return "Error : " + e.getMessage();
    }
  }

  @Override
  public LoadBalancingInformation loadBalancerInformation() throws Exception {
    synchronized(loadBalancingInformationLock) {
      if (currentLoadBalancingInformation == null) currentLoadBalancingInformation = computeCurrentLoadBalancingInformation();
      return currentLoadBalancingInformation;
    }
  }

  /**
   * Compute the current load balancing parameters form the JPPF configuration.
   * @return a {@link LoadBalancingInformation} instance.
   */
  private LoadBalancingInformation computeCurrentLoadBalancingInformation() {
    TypedProperties props = JPPFConfiguration.getProperties();
    String algorithm = props.get(JPPFProperties.LOAD_BALANCING_ALGORITHM);
    String profileName = props.get(JPPFProperties.LOAD_BALANCING_PROFILE);
    JPPFBundlerFactory factory = getNodeNioServer().getBundlerFactory();
    TypedProperties params = factory.convertJPPFConfiguration(profileName, props);
    List<String> algorithmsList = null;
    try {
      algorithmsList = factory.getBundlerProviderNames();
    } catch (Exception e) {
      algorithmsList = new ArrayList<>();
      log.error(e.getMessage(), e);
    }
    return new LoadBalancingInformation(algorithm, params, algorithmsList);
  }

  /**
   * Get a localized message given its unique name and the current locale.
   * @param message the unique name of the localized message.
   * @return a message in the current locale, or the default locale
   * if the localization for the current locale is not found.
   */
  private static String localize(final String message) {
    return LocalizationUtils.getLocalized(I18N_BASE, message);
  }

  @Override
  public void resetStatistics() throws Exception {
    if (debugEnabled) log.debug("statistics reset requested");
    JPPFStatistics stats = driver.getStatistics();
    JPPFSnapshot.LabelExcludingFilter filter = new JPPFSnapshot.LabelExcludingFilter(NODES, IDLE_NODES, CLIENTS, JOB_COUNT, TASK_QUEUE_COUNT);
    stats.reset(filter);
    for (String s: new String[] {JOB_COUNT, TASK_QUEUE_COUNT}) {
      JPPFSnapshot snapshot = stats.getSnapshot(s);
      if (snapshot instanceof AbstractJPPFSnapshot) ((AbstractJPPFSnapshot) snapshot).assignLatestToMax();
    }
  }

  @Override
  public JPPFSystemInformation systemInformation() throws Exception {
    return driver.getSystemInformation();
  }

  /**
   * {@inheritDoc}
   * @deprecated use {@link #nbNodes(NodeSelector)} with an {@link ExecutionPolicySelector} instead.
   */
  @Override
  public Integer matchingNodes(final ExecutionPolicy policy) throws Exception {
    return nbNodes(new ExecutionPolicySelector(policy));
  }

  /**
   * Get the JPPF nodes server.
   * @return a <code>NodeNioServer</code> instance.
   * @exclude
   */
  private NodeNioServer getNodeNioServer() {
    return driver.getNodeNioServer();
  }

  @Override
  public void toggleActiveState(final NodeSelector selector) throws Exception {
    Set<AbstractNodeContext> nodes = selectionHelper.getChannels(selector == null ? NodeSelector.ALL_NODES : selector);
    for (AbstractNodeContext node: nodes) getNodeNioServer().activateNode(node.getUuid(), !node.isActive());
  }

  @Override
  public Map<String, Boolean> getActiveState(final NodeSelector selector) throws Exception {
    Set<AbstractNodeContext> nodes = selectionHelper.getChannels(selector == null ? NodeSelector.ALL_NODES : selector);
    Map<String, Boolean> result = new HashMap<>(nodes.size());
    for (AbstractNodeContext node: nodes) result.put(node.getUuid(), node.isActive());
    return result;
  }

  @Override
  public void setActiveState(final NodeSelector selector, final boolean active) throws Exception {
    Set<AbstractNodeContext> nodes = selectionHelper.getChannels(selector == null ? NodeSelector.ALL_NODES : selector);
    for (AbstractNodeContext node: nodes) getNodeNioServer().activateNode(node.getUuid(), active);
  }

  @Override
  public void setBroadcasting(final boolean broadcasting) throws Exception {
    DriverInitializer di = driver.getInitializer();
    boolean b = di.isBroadcasting();
    if (b == broadcasting) return;
    if (b) di.initBroadcaster();
    else di.stopBroadcaster();
  }

  @Override
  public boolean getBroadcasting() throws Exception {
    return driver.getInitializer().isBroadcasting();
  }
}
