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

package org.jppf.management;

import static org.jppf.utils.stats.JPPFStatisticsHelper.*;

import java.util.*;

import org.jppf.node.policy.ExecutionPolicy;
import org.jppf.server.*;
import org.jppf.server.nio.nodeserver.*;
import org.jppf.server.scheduler.bundle.*;
import org.jppf.server.scheduler.bundle.spi.JPPFBundlerFactory;
import org.jppf.utils.*;
import org.jppf.utils.stats.*;
import org.slf4j.*;

/**
 * Instances of this class encapsulate the administration functionalities for a JPPF driver.
 * @author Laurent Cohen
 * @exclude
 */
public class JPPFDriverAdmin implements JPPFDriverAdminMBean
{
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
  public Integer nbNodes() throws Exception
  {
    return getNodeNioServer().getNbNodes();
  }

  @Override
  public Collection<JPPFManagementInfo> nodesInformation()
  {
    try
    {
      List<AbstractNodeContext> allChannels = getNodeNioServer().getAllChannels();
      List<JPPFManagementInfo> list = new ArrayList<>(allChannels.size());
      for (AbstractNodeContext context : allChannels)
      {
        JPPFManagementInfo info = context.getManagementInfo();
        if (info != null) list.add(info);
      }
      return list;
    }
    catch(Exception e)
    {
      log.error(e.getMessage(), e);
      return null;
    }
  }

  @Override
  public JPPFStatistics  statistics() throws Exception
  {
    try
    {
      JPPFStatistics  stats = driver.getStatistics().copy();
      if (debugEnabled) log.debug("stats request = " + stats);
      return stats;
    }
    catch(Throwable e)
    {
      log.error(e.getMessage(), e);
      return null;
    }
  }

  @Override
  public String changeLoadBalancerSettings(final String algorithm, final Map<Object, Object> parameters) throws Exception
  {
    try
    {
      if (algorithm == null) return "Error: no algorithm specified (null value)";
      NodeNioServer server = getNodeNioServer();
      JPPFBundlerFactory factory = server.getBundlerFactory();
      if (!factory.getBundlerProviderNames().contains(algorithm)) return "Error: unknown algorithm '" + algorithm + '\'';
      TypedProperties props = new TypedProperties(parameters);
      synchronized(loadBalancingInformationLock)
      {
        currentLoadBalancingInformation = new LoadBalancingInformation(algorithm, props, loadBalancerInformation().getAlgorithmNames());
      }
      Bundler bundler = factory.createBundler(algorithm, props);
      server.setBundler(bundler);
      return localize("load.balancing.updated");
    }
    catch(Exception e)
    {
      log.error(e.getMessage(), e);
      return "Error : " + e.getMessage();
    }
  }

  @Override
  public String restartShutdown(final Long shutdownDelay, final Long restartDelay) throws Exception
  {
    try
    {
      if (debugEnabled) log.debug("request to restart/shutdown this driver, shutdownDelay=" + shutdownDelay + ", restartDelay=" + restartDelay);
      boolean restart = restartDelay >= 0L;
      driver.initiateShutdownRestart(shutdownDelay, restart, restartDelay);
      return localize("request.acknowledged");
    }
    catch(Exception e)
    {
      log.error(e.getMessage(), e);
      return "Error : " + e.getMessage();
    }
  }

  @Override
  public LoadBalancingInformation loadBalancerInformation() throws Exception
  {
    synchronized(loadBalancingInformationLock)
    {
      if (currentLoadBalancingInformation == null) currentLoadBalancingInformation = computeCurrentLoadBalancingInformation();
      return currentLoadBalancingInformation;
    }
  }

  /**
   * Compute the current load balancing parameters form the JPPF configuration.
   * @return a {@link LoadBalancingInformation} instance.
   */
  private LoadBalancingInformation computeCurrentLoadBalancingInformation()
  {
    TypedProperties props = JPPFConfiguration.getProperties();
    String algorithm = props.getString("jppf.load.balancing.algorithm", "proportional");
    String profileName = props.getString("jppf.load.balancing.profile", null);
    if (profileName == null) profileName = props.getString("jppf.load.balancing.strategy", "jppf");
    JPPFBundlerFactory factory = getNodeNioServer().getBundlerFactory();
    TypedProperties params = factory.convertJPPFConfiguration(profileName, props);
    List<String> algorithmsList = null;
    try
    {
      algorithmsList = factory.getBundlerProviderNames();
    }
    catch (Exception e)
    {
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
  private static String localize(final String message)
  {
    return LocalizationUtils.getLocalized(I18N_BASE, message);
  }

  @Override
  public void resetStatistics() throws Exception
  {
    JPPFSnapshot.LabelExcludingFilter filter = new JPPFSnapshot.LabelExcludingFilter(NODES, IDLE_NODES, CLIENTS);
    driver.getStatistics().reset(filter);
  }

  @Override
  public JPPFSystemInformation systemInformation() throws Exception
  {
    return driver.getSystemInformation();
  }

  @Override
  public Integer matchingNodes(final ExecutionPolicy policy) throws Exception
  {
    List<AbstractNodeContext> allChannels = getNodeNioServer().getAllChannels();

    if (debugEnabled) log.debug("Testing policy against " + allChannels.size() + " nodes:\n" + policy );
    if (policy == null) return allChannels.size();

    int count = 0;
    TaskQueueChecker.preparePolicy(policy, null, driver.getStatistics(), 0);
    for (AbstractNodeContext context : allChannels) {
      JPPFManagementInfo mgtInfo = context.getManagementInfo();
      boolean match = false;
      if (mgtInfo == null) match = true;
      else
      {
        JPPFSystemInformation info = context.getSystemInformation();
        try
        {
          match = policy.accepts(info);
        }
        catch(Exception e)
        {
          String msg = "An error occurred while checking node " + mgtInfo + " against execution policy " + policy;
          if (debugEnabled) log.debug(msg, e);
          else log.warn(msg);
        }
        if (match) count++;
        if (debugEnabled) log.debug("testing against " + mgtInfo + " returns " + match);
      }
    }
    if (debugEnabled) log.debug("matching nodes = " + count);
    return count;
  }

  @Override
  public Integer nbIdleNodes() throws Exception
  {
    int n = getNodeNioServer().getNbIdleChannels();
    if (debugEnabled) log.debug("found " + n + " idle channels");
    return n;
  }

  @Override
  public Collection<JPPFManagementInfo> idleNodesInformation() throws Exception
  {
    List<AbstractNodeContext> idleChannels = getNodeNioServer().getIdleChannels();
    int size = idleChannels.size();
    if (debugEnabled) log.debug("found " + size + " idle channels");
    List<JPPFManagementInfo> list = new ArrayList<>(size);
    for (AbstractNodeContext context: idleChannels)
    {
      JPPFManagementInfo info = context.getManagementInfo();
      if (info != null) list.add(info);
      else if (debugEnabled) log.debug("no management info for channel " + context);
    }
    return list;
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
  public void toggleActiveState(final NodeSelector selector) throws Exception
  {
    Set<AbstractNodeContext> nodes = selectionHelper.getChannels(selector);
    for (AbstractNodeContext node: nodes) getNodeNioServer().activateNode(node.getUuid(), !node.isActive());
  }
}
