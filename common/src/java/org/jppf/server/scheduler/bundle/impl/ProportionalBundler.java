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
import org.jppf.server.scheduler.bundle.proportional.AbstractProportionalBundler;
import org.slf4j.*;

/**
 * This bundler implementation computes bundle sizes proportional to the mean execution
 * time for each node to the power of n, where n is an integer value specified in the configuration file as "proportionality factor".<br>
 * The scope of this bundler is all nodes, which means that it computes the size for all nodes.<br>
 * The mean execution time is computed as a moving average over a number of tasks, specified in the bundling
 * algorithm profile configuration as &quot;minSamplesToAnalyse&quot;<br>
 * This algorithm is well suited for relatively small networks (a few dozen nodes at most). It generates an overhead
 * every time the performance data for a node is updated. In the case of a small network, this overhead is not
 * large enough to impact the overall performance significantly.
 * @author Laurent Cohen
 */
public class ProportionalBundler extends AbstractProportionalBundler implements ContextAwareness
{
  /**
   * Logger for this class.
   */
  private static Logger log = LoggerFactory.getLogger(ProportionalBundler.class);
  /**
   * Determines whether debugging level is set for logging.
   */
  private static boolean debugEnabled = log.isDebugEnabled();
  /**
   * Determines whether debugging level is set for logging.
   */
  private static boolean traceEnabled = log.isTraceEnabled();
  /**
   * Holds information about the execution context.
   */
  private JPPFContext jppfContext = null;

  /**
   * Creates a new instance with the initial size of bundle as the start size.
   * @param profile the parameters of the auto-tuning algorithm, grouped as a performance analysis profile.
   */
  public ProportionalBundler(final LoadBalancingProfile profile)
  {
    super(profile);
  }

  /**
   * Make a copy of this bundler
   * @return a <code>Bundler</code> instance.
   * @see org.jppf.server.scheduler.bundle.Bundler#copy()
   */
  @Override
  public Bundler copy()
  {
    return new ProportionalBundler(profile);
  }

  /**
   * Get the max bundle size that can be used for this bundler.
   * @return the bundle size as an int.
   * @see org.jppf.server.scheduler.bundle.AbstractBundler#maxSize()
   */
  @Override
  protected int maxSize()
  {
    if (traceEnabled) log.trace("bundler #" + this.bundlerNumber + ": jppfContext=" + jppfContext);
    if (jppfContext == null) return 300;
    int n = jppfContext.getMaxBundleSize();
    if (traceEnabled) log.trace("bundler #" + this.bundlerNumber + ": maxBundleSize=" + n);
    return n <= 0 ? 300 : n;
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
