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

package test.loadbalancer;

import java.util.concurrent.atomic.AtomicInteger;

import org.jppf.client.JPPFClient;
import org.jppf.client.monitoring.topology.TopologyManager;
import org.jppf.load.balancer.*;
import org.jppf.management.JPPFSystemInformation;
import org.jppf.node.protocol.JPPFDistributedJob;

/**
 * A custom client-side only load-balancer which distributes tasks between the client-local channel and a remote driver channel.
 * <p>It keeps track of the number of remote nodes using a {@code TopologyListener} registered with a {@code TopologyManager},
 * in order to compute the number of tasks to dispatch to  the remote driver.
 * <p>example use in the configuration:
 * <pre>
 * jppf.load.balancing.algorithm = MyLoadBalancer
 * jppf.load.balancing.profile = MyProfile
 * jppf.load.balancing.profile.MyProfile.sizePerExecutor = 10
 * </pre>
 */
public class MyLoadBalancer extends AbstractBundler implements NodeAwareness, ContextAwareness, JobAwarenessEx {
  /** The default task bundle size */
  static final int DEFAULT_SIZE = 10;
  /** Count of instances of this class */
  private static final AtomicInteger instanceCount = new AtomicInteger(0);
  /** */
  private static final LBLogger lbLogger = new LBLogger();
  /** the configuration for the executor associated with this load-balancer */
  private JPPFSystemInformation nodeConfig;
  /** used to keep track of the number of remote nodes see {@link #setClient(JPPFClient)} */
  private static TopologyManager manager;
  /** whether the associated executor is client-local or a remote node */
  private boolean localExecutor;
  /** the JPPF context */
  private JPPFContext context;
  /** instance number */
  private final int instanceNumber = instanceCount.incrementAndGet();
  /** */
  private JPPFDistributedJob job;

  /**
   * Initialize this load-balancer with the specified parameters profile.
   * @param profile the profile to use.
   */
  public MyLoadBalancer(final LoadBalancingProfile profile) {
    super(profile);
  }

  @Override
  public int getBundleSize() {
    // number of tasks dispatched to each executor, whether client-local or remote node
    int nbTasks = ((MyLoadBalancingProfile) getProfile()).getSizePerExecutor();
    // if local client channel then dispatch n tasks, otherwise dispatch n * nbNodes tasks
    int size = localExecutor ? nbTasks : manager.getNodeCount() * nbTasks;
    // this method should never return a task bundle size <= 0
    if (size <= 0) size = DEFAULT_SIZE;
    lbLogger.log("#%d disptaching %d tasks to %s", instanceNumber, size, (localExecutor ? "client" : "server"));
    return size;
  }

  @Override
  public Bundler copy() {
    return new MyLoadBalancer(getProfile().copy());
  }

  @Override
  protected int maxSize() {
    return context.getMaxBundleSize();
  }

  /* NodeAwareness interface */

  @Override
  public JPPFSystemInformation getNodeConfiguration() {
    return nodeConfig;
  }

  @Override
  public void setNodeConfiguration(final JPPFSystemInformation nodeConfiguration) {
    // despite its name, this method set the config for the current client or a remote driver
    this.nodeConfig = nodeConfiguration;
    localExecutor = nodeConfig.getJppf().getBoolean("jppf.channel.local", false);
  }

  /* ContextAwareness interface */

  @Override
  public JPPFContext getJPPFContext() {
    return this.context;
  }

  @Override
  public void setJPPFContext(final JPPFContext context) {
    this.context = context;
  }

  /* JobAwarenessEx interface */

  @Override
  public JPPFDistributedJob getJob() {
    return job;
  }

  @Override
  public void setJob(final JPPFDistributedJob job) {
    this.job = job;
  }

  /**
   * Set the JPPF client for all instances of this client-side load-balancer.
   * @param client the JPPF client to use.
   */
  public static synchronized void setClient(final JPPFClient client) {
    if (manager == null) {
      // register a topology listener that will keep track of the number of remote nodes
      manager = new TopologyManager(client);
    }
  }
}
