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

package org.jppf.example.loadbalancer.server;

import org.jppf.management.JPPFSystemInformation;
import org.jppf.node.protocol.JobMetadata;
import org.jppf.server.scheduler.bundle.*;
import org.jppf.utils.TypedProperties;
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
public class CustomLoadBalancer extends AbstractBundler implements NodeAwareness, JobAwareness, ContextAwareness
{
  /**
   * Logger for this class.
   */
  private static Logger log = LoggerFactory.getLogger(CustomLoadBalancer.class);
  /**
   * Holds information about the node's environment and configuration.
   */
  private JPPFSystemInformation nodeConfiguration = null;
  /**
   * Holds metadata about the current job being dispatched.
   */
  private JobMetadata jobMetadata = null;
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
  public CustomLoadBalancer(final LoadBalancingProfile profile)
  {
    super(profile);
    if (log.isDebugEnabled()) log.debug("creating CustomLoadBalancer #" + this.bundlerNumber);
  }

  /**
   * Make a copy of this bundler.
   * Which parts are actually copied depends on the implementation.
   * @return a new <code>Bundler</code> instance.
   * @see org.jppf.server.scheduler.bundle.Bundler#copy()
   */
  @Override
  public Bundler copy()
  {
    return new CustomLoadBalancer(null);
  }

  /**
   * Get the current number of tasks to send to the node.
   * @return  the bundle size as an int value.
   * @see org.jppf.server.scheduler.bundle.Bundler#getBundleSize()
   */
  @Override
  public int getBundleSize()
  {
    return bundleSize;
  }

  /**
   * Get the corresponding node's system information.
   * @return a {@link JPPFSystemInformation} instance.
   * @see org.jppf.server.scheduler.bundle.NodeAwareness#getNodeConfiguration()
   */
  @Override
  public JPPFSystemInformation getNodeConfiguration()
  {
    return nodeConfiguration;
  }

  /**
   * Set the corresponding node's system information.
   * @param nodeConfiguration a {@link JPPFSystemInformation} instance.
   * @see org.jppf.server.scheduler.bundle.NodeAwareness#setNodeConfiguration(org.jppf.management.JPPFSystemInformation)
   */
  @Override
  public void setNodeConfiguration(final JPPFSystemInformation nodeConfiguration)
  {
    this.nodeConfiguration = nodeConfiguration;
    if (log.isDebugEnabled()) log.debug("setting node configuration on bundler #" + bundlerNumber + ": " + nodeConfiguration);
  }

  /**
   * Get the max bundle size that can be used for this bundler.
   * @return the bundle size as an int.
   * @see org.jppf.server.scheduler.bundle.AbstractBundler#maxSize()
   */
  @Override
  protected int maxSize()
  {
    return jppfContext == null ? 300 : jppfContext.getMaxBundleSize();
  }

  /**
   * Get the current job's metadata.
   * @return a {@link JobMetadata} instance.
   * @see org.jppf.server.scheduler.bundle.JobAwareness#getJobMetadata()
   */
  @Override
  public JobMetadata getJobMetadata()
  {
    return jobMetadata;
  }

  /**
   * Set the current job's metadata.
   * @param metadata a {@link JobMetadata} instance.
   * @see org.jppf.server.scheduler.bundle.JobAwareness#setJobMetadata(JobMetadata)
   */
  @Override
  public void setJobMetadata(final JobMetadata metadata)
  {
    this.jobMetadata = metadata;
    // compute the number of tasks to send to the node,
    // based on the new job metadata
    computeBundleSize();
  }

  /**
   * Compute the number of tasks to send to the node. This is the actual algorithm implementation.
   */
  private void computeBundleSize()
  {
    if (log.isDebugEnabled()) log.debug("computing bundle size for bundler #" + this.bundlerNumber);
    // Get the job metadata in an easy to use format
    TypedProperties props = new TypedProperties(getJobMetadata().getAll());
    // the maximum memory footprint of each task in bytes
    long taskMemory = props.getLong("task.memory", 10*1024);
    // fetch the length of a task in milliseconds
    long taskTime = props.getLong("task.time", 10);
    // fetch the maximum allowed time for execution of a single set of tasks on a node
    long allowedTime = props.getLong("allowed.time", -1);
    // if allowed time is not defined we assume no time limit
    if (allowedTime <= 0) allowedTime = Long.MAX_VALUE;
    // get the number of processing threads in the node
    int nbThreads = getNodeConfiguration().getJppf().getInt("jppf.processing.threads", -1);
    // if number of threads is not defined, we assume it is the number of available processors
    if (nbThreads <= 0) nbThreads = getNodeConfiguration().getRuntime().getInt("availableProcessors");
    // max node heap size of the node in bytes
    long nodeMemory = getNodeConfiguration().getRuntime().getLong("maxMemory");
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
    if (maxTime > allowedTime)
    {
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
   * @see org.jppf.server.scheduler.bundle.AbstractBundler#dispose()
   */
  @Override
  public void dispose()
  {
    if (log.isDebugEnabled()) log.debug("disposing bundler #" + this.bundlerNumber);
    this.jobMetadata = null;
    this.nodeConfiguration = null;
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
