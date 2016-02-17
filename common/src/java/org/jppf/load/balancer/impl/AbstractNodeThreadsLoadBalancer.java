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
package org.jppf.load.balancer.impl;

import org.jppf.load.balancer.*;
import org.jppf.management.JPPFSystemInformation;
import org.jppf.utils.TypedProperties;
import org.jppf.utils.configuration.*;
import org.slf4j.*;

/**
 * This implementation of a load-balancing algorithm illustrates the use of
 * the {@link NodeAwareness} APIs, by sending to each node at most <code>m * n</code> tasks,
 * where <i>n</i> is the number of processing threads in the node, and <i>m</i> is a
 * user-defined parameter which defaults to one.
 * @author Laurent Cohen
 * @exclude
 */
public abstract class AbstractNodeThreadsLoadBalancer extends AbstractBundler implements NodeAwareness {
  /**
   * Logger for this class.
   */
  private static Logger log = LoggerFactory.getLogger(AbstractNodeThreadsLoadBalancer.class);
  /**
   * Holds information about the node's environment and configuration.
   */
  protected JPPFSystemInformation nodeConfiguration = null;
  /**
   * The current number of tasks to send to the node.
   */
  protected int bundleSize = 1;

  /**
   * Creates a new instance with the specified parameters profile.
   * @param profile the parameters of the load-balancing algorithm.
   */
  public AbstractNodeThreadsLoadBalancer(final LoadBalancingProfile profile) {
    super(profile);
  }

  /**
   * Get the current number of tasks to send to the node.
   * @return the bundle size as an int value.
   */
  @Override
  public int getBundleSize() {
    return bundleSize;
  }

  @Override
  public JPPFSystemInformation getNodeConfiguration() {
    return nodeConfiguration;
  }

  @Override
  public void setNodeConfiguration(final JPPFSystemInformation nodeConfiguration) {
    this.nodeConfiguration = nodeConfiguration;
    computeBundleSize();
    if (log.isDebugEnabled()) log.debug("setting node configuration on bundler #" + bundlerNumber + ": " + nodeConfiguration);
  }

  /**
   * Compute the number of tasks to send to the node. This is the actual algorithm implementation.
   */
  private void computeBundleSize() {
    JPPFSystemInformation nodeConfig = getNodeConfiguration();
    if (nodeConfig == null) bundleSize = 1;
    else {
      // get the number of processing threads in the node
      TypedProperties jppf = getNodeConfiguration().getJppf();
      boolean isPeer = jppf.getBoolean("jppf.peer.driver", false);
      JPPFProperty prop = isPeer ? JPPFProperties.PEER_PROCESSING_THREADS : JPPFProperties.PROCESSING_THREADS;
      int nbThreads = jppf.getInt(prop.getName(), -1);
      if (log.isDebugEnabled()) log.debug("bundler #" + this.bundlerNumber + " nb threads from config = " + nbThreads);
      // if number of threads is not defined, we assume it is the number of available processors
      if (nbThreads <= 0) nbThreads = getNodeConfiguration().getRuntime().getInt("availableProcessors");
      if (nbThreads <= 0) nbThreads = 1;
      int multiplicator = ((NodeThreadsProfile) profile).getMultiplicator();
      if (multiplicator <= 0) multiplicator = 1;
      bundleSize = nbThreads * multiplicator;
    }
    // log the new bundle size
    if (log.isDebugEnabled()) log.debug("bundler #" + this.bundlerNumber + " computed new bundle size = " + bundleSize);
  }

  /**
   * Release the resources used by this bundler.
   * @see org.jppf.load.balancer.AbstractBundler#dispose()
   */
  @Override
  public void dispose() {
    if (log.isDebugEnabled()) log.debug("disposing bundler #" + this.bundlerNumber);
    this.nodeConfiguration = null;
  }
}
