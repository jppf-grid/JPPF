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

package org.jppf.load.balancer;

import org.jppf.management.JPPFSystemInformation;
import org.jppf.node.protocol.JPPFDistributedJob;
import org.jppf.utils.TypedProperties;
import org.jppf.utils.configuration.*;

/**
 * 
 * @param <T> the type of parameters profile used by this bundler.
 * @author Laurent Cohen
 */
public abstract class AbstractAdaptiveBundler<T extends LoadBalancingProfile> extends AbstractBundler<T> implements BundlerEx<T>, ChannelAwareness, JobAwareness {
  /**
   * The current bundle size.
   */
  protected int bundleSize = 1;
  /**
   * Holds information about the node's environment and configuration.
   */
  protected JPPFSystemInformation channelConfiguration;
  /**
   * The number of processing threads in the node.
   */
  protected int nbThreads = 1;
  /**
   * Holds information about the current job being dispatched.
   */
  protected JPPFDistributedJob job;

  /**
   * Creates a new instance with the specified parameters profile.
   * @param profile the parameters of the load-balancing algorithm.
   */
  public AbstractAdaptiveBundler(final T profile) {
    super(profile);
  }

  @Override
  public int getBundleSize() {
    return bundleSize;
  }

  @Override
  public JPPFSystemInformation getChannelConfiguration() {
    return channelConfiguration;
  }

  @Override
  public void setChannelConfiguration(final JPPFSystemInformation nodeConfiguration) {
    this.channelConfiguration = nodeConfiguration;
    TypedProperties jppf = nodeConfiguration.getJppf();
    boolean isPeer = jppf.getBoolean("jppf.peer.driver", false);
    JPPFProperty<Integer> prop = isPeer ? JPPFProperties.PEER_PROCESSING_THREADS : JPPFProperties.PROCESSING_THREADS;
    int nbThreads = jppf.getInt(prop.getName(), -1);
    // if number of threads is not defined, we assume it is the number of available processors
    if (nbThreads <= 0) nbThreads = getChannelConfiguration().getRuntime().getInt("availableProcessors");
    if (nbThreads <= 0) nbThreads = 1;
  }

  @Override
  public void feedback(final int size, final double totalTime, final double accumulatedElapsed, final double overheadTime) {
    int n1 = size / nbThreads;
    int n2 = size % nbThreads;
    double mean = accumulatedElapsed / size;
    double t = 0d;
    if (n1 == 0) t = mean;
    else {
      t = n1 * mean;
      if (n2 > 0) t += mean * ((double) n2 / nbThreads);
    }
    t += overheadTime;
    feedback(size, t);
  }

  @Override
  public JPPFDistributedJob getJob() {
    return job;
  }

  @Override
  public void setJob(final JPPFDistributedJob job) {
    this.job = job;
  }

  @Override
  public void dispose() {
    super.dispose();
    channelConfiguration = null;
    job = null;
  }

  /**
   * Get the max bundle size that can be used for this bundler.
   * @return the bundle size as an int.
   */
  @Override
  public int maxSize() {
    return (job != null)  ? job.getTaskCount() : super.maxSize();
  }
}
