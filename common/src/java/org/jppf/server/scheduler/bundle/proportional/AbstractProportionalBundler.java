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

package org.jppf.server.scheduler.bundle.proportional;

import java.util.*;

import org.jppf.server.scheduler.bundle.*;
import org.slf4j.*;

/**
 * This bundler implementation computes bundle sizes proportional to the mean execution
 * time for each node to the power of n, where n is an integer value specified in the configuration file as "proportionality factor".<br>
 * The scope of this bundler is all nodes, which means that it computes the size for all nodes,
 * unless an override is specified by the nodes.<br>
 * The mean execution time is computed as a moving average over a number of tasks, specified in the bundling
 * algorithm profile configuration as &quot;performanceCacheSize&quot;<br>
 * This algorithm is well suited for relatively small networks (a few dozen nodes at most). It generates an overhead
 * every time the performance data for a node is updated. In the case of a small network, this overhead is not
 * large enough to impact the overall performance significantly.
 * @author Laurent Cohen
 */
public abstract class AbstractProportionalBundler extends AbstractAdaptiveBundler
{
  /**
   * Logger for this class.
   */
  private static Logger log = LoggerFactory.getLogger(AbstractProportionalBundler.class);
  /**
   * Determines whether debugging level is set for logging.
   */
  private static boolean debugEnabled = log.isDebugEnabled();
  /**
   * Determines whether debugging level is set for logging.
   */
  private static boolean traceEnabled = log.isTraceEnabled();
  /**
   * Mapping of individual bundler to corresponding performance data - global.
   */
  private static final Set<AbstractProportionalBundler> BUNDLERS = new HashSet<AbstractProportionalBundler>();
  /**
   * Mapping of individual bundler to corresponding performance data - local.
   */
  private final Set<AbstractProportionalBundler> bundlers;
  /**
   * Bounded memory of the past performance updates.
   */
  protected final BundleDataHolder dataHolder;

  /**
   * Creates a new instance with the initial size of bundle as the start size.
   * @param profile the parameters of the load-balancing algorithm,
   */
  public AbstractProportionalBundler(final LoadBalancingProfile profile)
  {
    this(profile, null);
  }

  /**
   * Creates a new instance with the initial size of bundle as the start size.
   * @param profile the parameters of the load-balancing algorithm,
   * @param bundlers mapping of individual bundler to corresponding performance data.
   */
  protected AbstractProportionalBundler(final LoadBalancingProfile profile, final Set<AbstractProportionalBundler> bundlers)
  {
    super(profile);

    if (bundlers == null)
      this.bundlers = BUNDLERS;
    else
      this.bundlers = bundlers;

    if (this.profile == null) this.profile = new ProportionalTuneProfile();
    ProportionalTuneProfile prof = (ProportionalTuneProfile) this.profile;
    dataHolder = new BundleDataHolder(prof.getPerformanceCacheSize(), prof.getInitialMeanTime());
    bundleSize = prof.getInitialSize();
    if (bundleSize < 1) bundleSize = 1;
    if (debugEnabled) log.debug("Bundler#" + bundlerNumber + ": Using proportional bundle size - the initial size is " + bundleSize + ", profile: " + profile);
  }

  /**
   * Get local mapping of individual bundler to corresponding performance data.
   * @return <code>Set<AbstractProportionalBundler></code>
   */
  protected final Set<AbstractProportionalBundler> getBundlers()
  {
    return bundlers;
  }

  /**
   * Set the current size of bundle.
   * @param size the bundle size as an int value.
   * @see org.jppf.server.scheduler.bundle.Bundler#getBundleSize()
   */
  public void setBundleSize(final int size)
  {
    bundleSize = (size <= 0) ? 1 : size;
  }

  /**
   * This method delegates the bundle size calculation to the singleton instance of <code>SimpleBundler</code>.
   * @param size the number of tasks executed.
   * @param time the time in nanoseconds it took to execute the tasks.
   * @see org.jppf.server.scheduler.bundle.AbstractBundler#feedback(int, double)
   */
  @Override
  public void feedback(final int size, final double time)
  {
    if (traceEnabled) log.trace("Bundler#" + bundlerNumber + ": new performance sample [size=" + size + ", time=" + (long) time + ']');
    if (size <= 0) return;
    BundlePerformanceSample sample = new BundlePerformanceSample(time / size, size);
    synchronized(bundlers)
    {
      dataHolder.addSample(sample);
      computeBundleSizes();
    }
  }

  /**
   * Perform context-independent initializations.
   * @see org.jppf.server.scheduler.bundle.AbstractBundler#setup()
   */
  @Override
  public void setup()
  {
    synchronized(bundlers)
    {
      bundlers.add(this);
      if (debugEnabled) log.debug("added " + this + ", bundlers set = " + bundlers);
    }
  }

  /**
   * Release the resources used by this bundler.
   * @see org.jppf.server.scheduler.bundle.AbstractBundler#dispose()
   */
  @Override
  public void dispose()
  {
    synchronized(bundlers)
    {
      bundlers.remove(this);
      if (debugEnabled) log.debug("removed " + this + ", bundlers set = " + bundlers);
    }
  }

  /**
   * Get the bounded memory of the past performance updates.
   * @return a BundleDataHolder instance.
   */
  public BundleDataHolder getDataHolder()
  {
    return dataHolder;
  }

  /**
   * Update the bundler sizes.
   */
  private void computeBundleSizes()
  {
    synchronized(bundlers)
    {
      double maxMean = Double.NEGATIVE_INFINITY;
      double minMean = Double.POSITIVE_INFINITY;
      AbstractProportionalBundler minBundler = null;
      double meanSum = 0.0d;
      for (AbstractProportionalBundler b: bundlers)
      {
        BundleDataHolder h = b.getDataHolder();
        double m = h.getMean();
        if (m > maxMean) maxMean = m;
        if (m < minMean)
        {
          minMean = m;
          minBundler = b;
        }
      }
      for (AbstractProportionalBundler b: bundlers)
      {
        BundleDataHolder h = b.getDataHolder();
        meanSum += normalize(h.getMean());
      }
      int max = maxSize();
      int sum = 0;
      for (AbstractProportionalBundler b: bundlers)
      {
        BundleDataHolder h = b.getDataHolder();
        double p = normalize(h.getMean()) / meanSum;
        int size = Math.max(1, (int) (p * max));
        if (size >= max) size = max-1;
        b.setBundleSize(size);
        sum += size;
      }
      if ((sum < max) && (minBundler != null))
      {
        int size = minBundler.getBundleSize();
        minBundler.setBundleSize(size + (max - sum));
      }
      if (traceEnabled)
      {
        StringBuilder sb = new StringBuilder();
        sb.append("bundler info:\n");
        sb.append("  minMean=").append(minMean).append(", maxMean=").append(maxMean).append(", maxSize=").append(max).append('\n');
        for (AbstractProportionalBundler b: bundlers)
        {
          sb.append("  bundler #").append(b.getBundlerNumber()).append(" : bundleSize=").append(b.getBundleSize()).append(", ");
          sb.append(b.getDataHolder()).append('\n');
        }
        log.trace(sb.toString());
      }
    }
  }

  /**
   * 
   * @param x .
   * @return .
   */
  public double normalize(final double x)
  {
    //return 1d / (1d + (x <= 0d ? 0d : Math.log(1d + ((ProportionalTuneProfile) profile).getProportionalityFactor() * x)));
    //return Math.exp(-((ProportionalTuneProfile) profile).getProportionalityFactor() * x);
    double r = 1.0d;
    for (int i=0; i<((ProportionalTuneProfile) profile).getProportionalityFactor(); i++) r *= x;
    return 1.0d /r;
    /*
     */
  }

  @Override
  public Bundler copy()
  {
    return null;
  }

  @Override
  protected int maxSize()
  {
    return 0;
  }
}
