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

package org.jppf.example.loadbalancer.server;

import org.jppf.load.balancer.*;
import org.jppf.management.JPPFSystemInformation;
import org.jppf.node.protocol.JPPFDistributedJob;
import org.jppf.utils.TypedProperties;
import org.jppf.utils.configuration.JPPFProperties;
import org.slf4j.*;

/**
 * This implementation of a load-balancing algorithm illustrates the use of
 * the {@link NodeAwareness} and {@link JobAwareness} APIs, allowing the algorithm to work
 * based on known information about the nodes and the jobs.
 * <p>In this implementation, we assume each job provides the following metadata:
 * <ul>
 * <li>"task.memory": the maximum memory footprint of each task in bytes</li>
 * <li>"task.time": the maximum duration of a task in milliseconds</li>
 * <li>"allowed.time": the maximum allowed time for execution of a single set of tasks on a node, in milliseconds</li>
 * <li>"id": the id of the current job being executed, used for debugging and logging</li>
 * </ul>
 * @author Laurent Cohen
 */
public class CustomLoadBalancer extends AbstractBundler implements ChannelAwareness, JobAwarenessEx {
  /**
   * Logger for this class.
   */
  private static Logger log = LoggerFactory.getLogger(CustomLoadBalancer.class);
  /**
   * Holds information about the node's environment and configuration.
   */
  private JPPFSystemInformation nodeConfiguration = null;
  /**
   * Holds information about the current job being dispatched.
   */
  private JPPFDistributedJob jobInformation = null;
  /**
   * The current number of tasks to send to the node.
   */
  private int bundleSize = 1;
  /**
   * Holds information about the execution context.
   */
  private JPPFContext jppfContext = null;

  /**
   * Creates a new instance with the specified parameters profile.
   * @param profile the parameters of the load-balancing algorithm.
   */
  public CustomLoadBalancer(final LoadBalancingProfile profile) {
    super(profile);
    if (log.isDebugEnabled()) log.debug("creating CustomLoadBalancer #" + this.bundlerNumber);
  }

  /**
   * Get the current number of tasks to send to the node.
   * @return the bundle size as an int value.
   */
  @Override
  public int getBundleSize() {
    return bundleSize;
  }

  /**
   * Get the corresponding node's system information.
   * @return a {@link JPPFSystemInformation} instance.
   */
  @Override
  public JPPFSystemInformation getChannelConfiguration() {
    return nodeConfiguration;
  }

  /**
   * Set the corresponding node's system information.
   * @param nodeConfiguration a {@link JPPFSystemInformation} instance.
   */
  @Override
  public void setChannelConfiguration(final JPPFSystemInformation nodeConfiguration) {
    this.nodeConfiguration = nodeConfiguration;
    if (log.isDebugEnabled()) log.debug("setting node configuration on bundler #" + bundlerNumber + ": " + nodeConfiguration);
  }

  /**
   * Get the current job's inforamtion.
   * @return a {@link JPPFDistributedJob} instance.
   */
  @Override
  public JPPFDistributedJob getJob() {
    return jobInformation;
  }

  /**
   * Set the current job's information.
   * @param jobInformation a {@link JPPFDistributedJob} instance.
   */
  @Override
  public void setJob(final JPPFDistributedJob jobInformation) {
    this.jobInformation = jobInformation;
    // compute the number of tasks to send to the node,
    // based on the new job metadata
    computeBundleSize();
  }

  /**
   * Compute the number of tasks to send to the node. This is the actual algorithm implementation.
   */
  private void computeBundleSize() {
    if (log.isDebugEnabled()) log.debug("computing bundle size for bundler #" + this.bundlerNumber);
    // Get the job metadata in an easy to use format
    TypedProperties props = new TypedProperties(getJob().getMetadata().getAll());
    // the maximum memory footprint of each task in bytes
    long taskMemory = props.getLong("task.memory", 10 * 1024);
    // fetch the length of a task in milliseconds
    long taskTime = props.getLong("task.time", 10);
    // fetch the maximum allowed time for execution of a single set of tasks on a node
    long allowedTime = props.getLong("allowed.time", -1);
    // if allowed time is not defined we assume no time limit
    if (allowedTime <= 0) allowedTime = Long.MAX_VALUE;
    // get the number of processing threads in the node
    int nbThreads = getChannelConfiguration().getJppf().get(JPPFProperties.PROCESSING_THREADS);
    // max node heap size of the node in bytes
    long nodeMemory = getChannelConfiguration().getRuntime().getLong("maxMemory");
    // we assume 20 MB of the node's memory is taken by JPPF code and add-ons
    nodeMemory -= 20 * 1024 * 1024;
    if (nodeMemory < 0) nodeMemory = 0;
    // max number of tasks that can fit in the node's heap
    // we count 2*taskMemory because it will take approximately twice the memory footprint
    // when each task is serialized or deserialized in the node (serialized data + the object itself)
    int maxTasks = (int) (nodeMemory / (2 * taskMemory));
    // the maximum time needed to execute maxTasks tasks on nbThreads parallel threads
    long maxTime = taskTime * maxTasks / nbThreads;
    // if maxTime is not a multiple of nbThreads, make the adjustment
    if ((maxTasks % nbThreads) != 0) maxTime += taskTime;
    // if max time is longer than the allowed time, reduce the number of tasks by the appropriate amount
    if (maxTime > allowedTime) {
      maxTasks = (int) ((maxTasks * allowedTime) / maxTime);
    }
    // finally, store the computation result, ensuring that 1 <= size <= maxSize
    bundleSize = Math.max(1, Math.min(maxTasks, maxSize()));
    // for debugging and logging purposes
    String id = props.getString("id", "unknown id");
    // log the new bundle size
    if (log.isDebugEnabled()) log.debug("bundler #" + this.bundlerNumber + " computed new bundle size = " + bundleSize + " for job id = " + id);
  }

  /**
   * Release the resources used by this bundler.
   */
  @Override
  public void dispose() {
    if (log.isDebugEnabled()) log.debug("disposing bundler #" + this.bundlerNumber);
    this.jobInformation = null;
    this.nodeConfiguration = null;
  }

  @Override
  public JPPFContext getJPPFContext() {
    return jppfContext;
  }

  @Override
  public void setJPPFContext(final JPPFContext context) {
    this.jppfContext = context;
  }
}
