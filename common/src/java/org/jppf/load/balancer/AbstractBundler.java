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

/**
 * Abstract implementation of the {@link Bundler} interface. In general, it will be more convenient to extend this class than
 * to implement {@link Bundler} directly.
 * <p>It provides working implementations of the {@link #getTimestamp() getTimestamp()}, {@link #getProfile() getProfile()},
 * {@link #setup() setup()} and {@link #dispose() dispose()} methods, along with an empty implementation of {@link #feedback(int, double) feedback(int, double)}.
 * <p>It also adds the {@link #maxSize() maxSize()} method which uses an internal context object to compute a usable cap for the value returned by {@code getBundleSize()}.
 * @param <T> the type of parameters profile used by this bundler.
 * @author Laurent Cohen
 */
public abstract class AbstractBundler<T extends LoadBalancingProfile> implements Bundler<T>, ContextAwareness {
  /**
   * The bundler number for this bundler.
   * @exclude
   */
  protected final int bundlerNumber = BUNDLER_COUNT.incrementAndGet();
  /**
   * The creation timestamp for this bundler.
   */
  protected final long timestamp = System.currentTimeMillis();
  /**
   * The parameters of this load-balancing algorithm.
   */
  protected T profile;
  /**
   * Holds information about the execution context.
   */
  private JPPFContext jppfContext = null;

  /**
   * Creates a new instance with the specified parameters profile.
   * @param profile the parameters of the load-balancing algorithm,
   */
  public AbstractBundler(final T profile) {
    this.profile = profile;
  }

  /**
   * Get the max bundle size that can be used for this bundler.
   * @return the bundle size as an int.
   */
  public int maxSize() {
    return (jppfContext == null || jppfContext.getMaxBundleSize() <= 0) ? 300 : jppfContext.getMaxBundleSize();
  }


  /**
   * This implementation does nothing and should be overridden in subclasses that compute the bundle size based on the feedback from the nodes.
   * @param bundleSize the number of tasks retruning from execution on a node.
   * @param totalTime the total round-trip time of the tasks, from the server to the node and back.
   */
  @Override
  public void feedback(final int bundleSize, final double totalTime) {
  }

  /**
   * Get the timestamp at which this bundler was created.
   * This is used to enable node channels to know when the bundler settings have changed.
   * @return the timestamp as a long value.
   */
  @Override
  public long getTimestamp() {
    return timestamp;
  }

  /**
   * Get this bundler's number (a unique Bundle instance count). Used for logging and debugging.
   * @return the bundler number as an int.
   * @exclude
   */
  public int getBundlerNumber() {
    return bundlerNumber;
  }

  /**
   * Perform context-independent initializations.
   */
  @Override
  public void setup() {
  }

  /**
   * Release the resources used by this bundler.
   */
  @Override
  public void dispose() {
    jppfContext = null;
  }

  /**
   * Get the parameters of this load-balancing algorithm.
   * @return an instance of an implementation of {@link LoadBalancingProfile}.
   */
  @Override
  public T getProfile() {
    return profile;
  }

  /**
   * @return the JPPF context for this bundler.
   * @exclude
   */
  @Override
  public JPPFContext getJPPFContext() {
    return jppfContext;
  }

  /**
   * @param context the JPPF context for this bundler.
   * @exclude
   */
  @Override
  public void setJPPFContext(final JPPFContext context) {
    this.jppfContext = context;
  }

  /**
   * @return {@code null}.
   * @deprecated this method is not needed anymore, all bundler and profile instantiations are done via the declared {@link org.jppf.load.balancer.spi.JPPFBundlerProvider JPPFBundlerProvider}s.
   */
  @Override
  public Bundler copy() {
    return null;
  }
}
