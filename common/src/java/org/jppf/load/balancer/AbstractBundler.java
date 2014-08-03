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

import java.util.concurrent.atomic.AtomicInteger;

/**
 * Abstract implementation of the bundler interface.
 * @author Laurent Cohen
 * @exclude
 */
public abstract class AbstractBundler implements Bundler
{
  /**
   * Count of the bundlers used to generate a readable unique id.
   */
  private static AtomicInteger bundlerCount = new AtomicInteger(0);
  /**
   * The bundler number for this bundler.
   */
  protected int bundlerNumber = incBundlerCount();
  /**
   * The creation timestamp for this bundler.
   */
  protected long timestamp = System.currentTimeMillis();
  /**
   * Parameters of the algorithm, grouped as a performance analysis profile.
   */
  protected LoadBalancingProfile profile;

  /**
   * Default constructor.
   */
  private AbstractBundler()
  {
  }

  /**
   * Creates a new instance with the specified parameters profile.
   * @param profile the parameters of the load-balancing algorithm,
   */
  public AbstractBundler(final LoadBalancingProfile profile)
  {
    this.profile = profile;
  }

  /**
   * Increment the bundlers count by one.
   * @return the new count as an int value.
   */
  private static int incBundlerCount()
  {
    return bundlerCount.incrementAndGet();
  }

  /**
   * Get the max bundle size that can be used for this bundler.
   * @return the bundle size as an int.
   */
  protected abstract int maxSize();

  /**
   * This method does nothing and should be overridden in subclasses.
   * @param bundleSize not used.
   * @param totalTime in nanoseconds - not used.
   * @see org.jppf.load.balancer.Bundler#feedback(int, double)
   */
  @Override
  public void feedback(final int bundleSize, final double totalTime)
  {
  }

  /**
   * Get the timestamp at which this bundler was created.
   * This is used to enable node channels to know when the bundler settings have changed.
   * @return the timestamp as a long value.
   * @see org.jppf.load.balancer.Bundler#getTimestamp()
   */
  @Override
  public long getTimestamp()
  {
    return timestamp;
  }

  /**
   * Get the bundler number for this bundler.
   * @return the bundler number as an int.
   */
  public int getBundlerNumber()
  {
    return bundlerNumber;
  }

  /**
   * Perform context-independent initializations.
   * @see org.jppf.load.balancer.Bundler#setup()
   */
  @Override
  public void setup()
  {
  }

  /**
   * Release the resources used by this bundler.
   * @see org.jppf.load.balancer.Bundler#dispose()
   */
  @Override
  public void dispose()
  {
  }

  /**
   * Get the parameters of the algorithm, grouped as a performance analysis profile.
   * @return an instance of <code>LoadBalancingProfile</code>.
   * @see org.jppf.load.balancer.Bundler#getProfile()
   */
  @Override
  public LoadBalancingProfile getProfile()
  {
    return profile;
  }
}
