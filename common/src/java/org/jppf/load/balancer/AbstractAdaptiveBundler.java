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

package org.jppf.load.balancer;

import org.jppf.management.JPPFSystemInformation;

/**
 * 
 * @author Laurent Cohen
 * @exclude
 */
public abstract class AbstractAdaptiveBundler extends AbstractBundler implements BundlerEx, NodeAwareness, ContextAwareness
{
  /**
   * The current bundle size.
   */
  protected int bundleSize = 1;
  /**
   * Holds information about the node's environment and configuration.
   */
  protected JPPFSystemInformation nodeConfiguration = null;
  /**
   * The number of processing threads in the node.
   */
  protected int nbThreads = 1;
  /**
   * Holds information about the execution context.
   */
  protected JPPFContext jppfContext = null;

  /**
   * Creates a new instance with the specified parameters profile.
   * @param profile the parameters of the load-balancing algorithm.
   */
  public AbstractAdaptiveBundler(final LoadBalancingProfile profile)
  {
    super(profile);
  }

  @Override
  public int getBundleSize()
  {
    return bundleSize;
  }

  @Override
  public JPPFSystemInformation getNodeConfiguration()
  {
    return nodeConfiguration;
  }

  @Override
  public void setNodeConfiguration(final JPPFSystemInformation nodeConfiguration)
  {
    this.nodeConfiguration = nodeConfiguration;
    nbThreads = nodeConfiguration.getJppf().getInt("jppf.processing.threads", 1);
  }

  @Override
  public void feedback(final int size, final double totalTime, final double accumulatedElapsed, final double overheadTime)
  {
    int n1 = size / nbThreads;
    int n2 = size % nbThreads;
    double mean = accumulatedElapsed / size;
    double t = 0d;
    if (n1 == 0) t = mean;
    else
    {
      t = n1 * mean;
      if (n2 > 0) t += mean * ((double) n2 / nbThreads);
    }
    t += overheadTime;
    feedback(size, t);
  }

  @Override
  public JPPFContext getJPPFContext()
  {
    return jppfContext;
  }

  @Override
  public void setJPPFContext(final JPPFContext context)
  {
    this.jppfContext = context;
  }
}
