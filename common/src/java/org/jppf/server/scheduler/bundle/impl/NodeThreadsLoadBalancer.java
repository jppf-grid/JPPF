/*
 * JPPF.
 * Copyright (C) 2005-2012 JPPF Team.
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
package org.jppf.server.scheduler.bundle.impl;

import org.jppf.server.scheduler.bundle.*;
import org.jppf.server.scheduler.bundle.nodethreads.AbstractNodeThreadsLoadBalancer;
import org.slf4j.*;

/**
 * This implementation of a load-balancing algorithm illustrates the use of
 * the {@link NodeAwareness} APIs, by sending to each node at most <code>m * n</code> tasks,
 * where <i>n</i> is the number of processing threads in the node, and <i>m</i> is a
 * user-defined parameter which defaults to one.
 * @author Laurent Cohen
 */
public class NodeThreadsLoadBalancer extends AbstractNodeThreadsLoadBalancer implements ContextAwareness
{
  /**
   * Logger for this class.
   */
  private static Logger log = LoggerFactory.getLogger(NodeThreadsLoadBalancer.class);
  /**
   * Holds information about the execution context.
   */
  private JPPFContext jppfContext = null;

  /**
   * Creates a new instance with the specified parameters profile.
   * @param profile the parameters of the load-balancing algorithm.
   */
  public NodeThreadsLoadBalancer(final LoadBalancingProfile profile)
  {
    super(profile);
    if (log.isDebugEnabled()) log.debug("creating " + this.getClass().getSimpleName() + " #" + this.bundlerNumber);
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
    return new NodeThreadsLoadBalancer(profile.copy());
  }

  /**
   * Get the max bundle size that can be used for this bundler.
   * @return the bundle size as an int.
   * @see org.jppf.server.scheduler.bundle.AbstractBundler#maxSize()
   */
  @Override
  protected int maxSize()
  {
    return (jppfContext == null ||  jppfContext.getMaxBundleSize() <= 0) ? 300 : jppfContext.getMaxBundleSize();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public JPPFContext getJPPFContext()
  {
    return jppfContext;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void setJPPFContext(final JPPFContext context)
  {
    this.jppfContext = context;
  }
}
