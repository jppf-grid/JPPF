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

package org.jppf.management;

import static org.jppf.utils.stats.JPPFStatisticsHelper.*;

import java.util.*;

import org.jppf.load.balancer.LoadBalancingInformation;
import org.jppf.load.balancer.spi.JPPFBundlerFactory;
import org.jppf.server.*;
import org.jppf.server.nio.nodeserver.BaseNodeContext;
import org.jppf.utils.*;
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
  private static boolean debugEnabled = log.isDebugEnabled();
  /**
   * Base name used for localization lookups.
   */
  private static final String I18N_BASE = "org.jppf.server.i18n.server_messages";
  /**
   * Reference to the driver.
   */
  private final JPPFDriver driver;
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
  private final NodeSelectionHelper selectionHelper;

  /**
   * 
   * @param driver the jppf driver that holds this mbean.
   */
  public JPPFDriverAdmin(final JPPFDriver driver) {
    this.driver = driver;
    selectionHelper = new NodeSelectionHelper(driver);
  }

  @Override
  public Integer nbNodes() throws Exception {
    return nbNodes(null);
  }

  @Override
  public Integer nbNodes(final NodeSelector selector) throws Exception {
    return selectionHelper.getNbChannels(selector == null ? NodeSelector.ALL_NODES : selector, false, false);
  }

  @Override
  public Collection<JPPFManagementInfo> nodesInformation() {
    return nodesInformation(null, false);
  }

  @Override
  public Collection<JPPFManagementInfo> nodesInformation(final NodeSelector selector) {
    return nodesInformation(selector, false);
  }

  @Override
  public Collection<JPPFManagementInfo> nodesInformation(final NodeSelector selector, final boolean includePeers) {
    try {
      final Set<BaseNodeContext> nodes = selectionHelper.getChannels(selector == null ? NodeSelector.ALL_NODES : selector, includePeers, false);
      final List<JPPFManagementInfo> list = new ArrayList<>(nodes.size());
      for (final BaseNodeContext context : nodes) {
        final JPPFManagementInfo info = context.getManagementInfo();
        if (info != null) list.add(info);
      }
      return list;
    } catch(final Exception e) {
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
    return nbIdleNodes(selector, false);
  }

  @Override
  public Integer nbIdleNodes(final NodeSelector selector, final boolean includePeers) throws Exception {
    final Set<BaseNodeContext> nodes = selectionHelper.getChannels(selector == null ? NodeSelector.ALL_NODES : selector, includePeers, false);
    if (nodes == null) return -1;
    int result = 0;
    for (final BaseNodeContext node: nodes) {
      final boolean idle = node.getIdle().get();
      if (idle) result++;
    }
    if (debugEnabled) log.debug("there are {} nodes, including {} idle nodes", nodes.size(), result);
    return result;
  }

  @Override
  public Collection<JPPFManagementInfo> idleNodesInformation() throws Exception {
    return idleNodesInformation(null);
  }

  @Override
  public Collection<JPPFManagementInfo> idleNodesInformation(final NodeSelector selector) {
    try {
      final Set<BaseNodeContext> nodes = selectionHelper.getChannels(selector == null ? NodeSelector.ALL_NODES : selector, false, false);
      final List<JPPFManagementInfo> list = new ArrayList<>(nodes.size());
      for (final BaseNodeContext node : nodes) {
        if (node.getIdle().get()) {
          final JPPFManagementInfo info = node.getManagementInfo();
          if (info != null) list.add(info);
        }
      }
      return list;
    } catch(final Exception e) {
      log.error(e.getMessage(), e);
      return null;
    }
  }

  @Override
  public JPPFStatistics statistics() throws Exception {
    try {
      final JPPFStatistics stats = driver.getStatistics();
      if (log.isTraceEnabled()) log.trace("stats request = " + stats);
      return stats;
    } catch(final Throwable e) {
      log.error(e.getMessage(), e);
      return null;
    }
  }

  @Override
  public String changeLoadBalancerSettings(final String algorithm, final Map<Object, Object> parameters) throws Exception {
    try {
      if (algorithm == null) return "Error: no algorithm specified (null value)";
      final JPPFBundlerFactory factory = getBundlerFactory();
      if (!factory.getBundlerProviderNames().contains(algorithm)) return "Error: unknown algorithm '" + algorithm + '\'';
      final TypedProperties props = new TypedProperties(parameters);
      synchronized(loadBalancingInformationLock) {
        currentLoadBalancingInformation = new LoadBalancingInformation(algorithm, props, loadBalancerInformation().getAlgorithmNames());
        factory.setAndGetCurrentInfo(currentLoadBalancingInformation);
      }
      return "load.balancing.updated";
    } catch(final Exception e) {
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
    final JPPFBundlerFactory factory = getBundlerFactory();
    final LoadBalancingInformation info = factory.getCurrentInfo();
    final List<String> algorithmsList = factory.getBundlerProviderNames();
    return new LoadBalancingInformation(info.getAlgorithm(), info.getParameters(), algorithmsList);
  }

  @Override
  public String restartShutdown(final Long shutdownDelay, final Long restartDelay) throws Exception {
    try {
      if (debugEnabled) log.debug("request to restart/shutdown this driver, shutdownDelay=" + shutdownDelay + ", restartDelay=" + restartDelay);
      final boolean restart = restartDelay >= 0L;
      driver.initiateShutdownRestart(shutdownDelay, restart, restartDelay);
      return localize("request.acknowledged");
    } catch(final Exception e) {
      log.error(e.getMessage(), e);
      return "Error : " + e.getMessage();
    }
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
    final JPPFStatistics stats = driver.getStatistics();
    final JPPFSnapshot.LabelExcludingFilter filter = new JPPFSnapshot.LabelExcludingFilter(NODES, IDLE_NODES, CLIENTS, JOB_COUNT, TASK_QUEUE_COUNT);
    stats.reset(filter);
    for (final String s: new String[] {JOB_COUNT, TASK_QUEUE_COUNT}) {
      final JPPFSnapshot snapshot = stats.getSnapshot(s);
      if (snapshot instanceof AbstractJPPFSnapshot) ((AbstractJPPFSnapshot) snapshot).assignLatestToMax();
    }
  }

  @Override
  public JPPFSystemInformation systemInformation() throws Exception {
    return driver.getSystemInformation();
  }

  /**
   * Get the JPPF load-balancer factory.
   * @return a {@link JPPFBundlerFactory} instance.
   * @exclude
   */
  private JPPFBundlerFactory getBundlerFactory() {
    return driver.getAsyncNodeNioServer().getBundlerFactory();
  }

  @Override
  public void toggleActiveState(final NodeSelector selector) throws Exception {
    final Set<BaseNodeContext> nodes = selectionHelper.getChannels(selector == null ? NodeSelector.ALL_NODES : selector);
    for (final BaseNodeContext node: nodes) {
      driver.getAsyncNodeNioServer().activateNode(node.getUuid(), !node.isActive()); 
    }
  }

  @Override
  public Map<String, Boolean> getActiveState(final NodeSelector selector) throws Exception {
    final Set<BaseNodeContext> nodes = selectionHelper.getChannels(selector == null ? NodeSelector.ALL_NODES : selector);
    final Map<String, Boolean> result = new HashMap<>(nodes.size());
    for (final BaseNodeContext node: nodes) result.put(node.getUuid(), node.isActive());
    return result;
  }

  @Override
  public void setActiveState(final NodeSelector selector, final boolean active) throws Exception {
    final Set<BaseNodeContext> nodes = selectionHelper.getChannels(selector == null ? NodeSelector.ALL_NODES : selector);
    for (final BaseNodeContext node: nodes) {
      driver.getAsyncNodeNioServer().activateNode(node.getUuid(), active); 
    }
  }

  @Override
  public void setBroadcasting(final boolean broadcasting) throws Exception {
    final DriverInitializer di = driver.getInitializer();
    final boolean b = di.isBroadcasting();
    if (b == broadcasting) return;
    if (b) di.initBroadcaster();
    else di.stopBroadcaster();
  }

  @Override
  public boolean getBroadcasting() throws Exception {
    return driver.getInitializer().isBroadcasting();
  }
}
