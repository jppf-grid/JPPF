/*
 * JPPF.
 * Copyright (C) 2005-2017 JPPF Team.
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

package org.jppf.load.balancer.impl;

import java.util.concurrent.locks.ReentrantLock;

import org.jppf.load.balancer.*;
import org.slf4j.*;

/**
 * Instances of this bundler delegate their operations to a singleton instance of a
 * {@link org.jppf.load.balancer.impl.AutoTunedBundler AutoTunedBundler}.
 * @author Laurent Cohen
 */
public class AutotunedDelegatingBundler extends AbstractBundler<AnnealingTuneProfile> {
  /**
   * Logger for this class.
   */
  private static Logger log = LoggerFactory.getLogger(AutotunedDelegatingBundler.class);
  /**
   * The global bundler to which bundle size calculations are delegated.
   */
  private static AutoTunedBundler simpleBundler = null;
  /**
   * Used to synchronize multiple threads when creating the simple bundler.
   */
  private final static ReentrantLock lock = new ReentrantLock();

  /**
   * Creates a new instance with the initial size of bundle as the start size.
   * @param profile the parameters of the auto-tuning algorithm grouped as a performance analysis profile.
   */
  public AutotunedDelegatingBundler(final AnnealingTuneProfile profile) {
    super(profile);
    log.info("Bundler#" + bundlerNumber + ": Using Auto-Tuned bundle size");
    //log.info("Bundler#" + bundlerNumber + ": The initial size is " + bundleSize);
    lock.lock();
    try {
      if (simpleBundler == null) simpleBundler = new AutoTunedBundler(profile);
    } finally {
      lock.unlock();
    }
  }

  /**
   * Get the current size of bundle.
   * @return the bundle size as an int value.
   */
  @Override
  public int getBundleSize() {
    return simpleBundler.getBundleSize();
  }

  /**
   * This method delegates the bundle size calculation to the singleton instance of <code>SimpleBundler</code>.
   * @param bundleSize the number of tasks executed.
   * @param totalTime the time in nanoseconds it took to execute the tasks.
   */
  @Override
  public void feedback(final int bundleSize, final double totalTime) {
    simpleBundler.feedback(bundleSize, totalTime);
  }

  /**
   * Get the max bundle size that can be used for this bundler.
   * @return the bundle size as an int.
   */
  @Override
  public int maxSize() {
    int max = 0;
    synchronized (simpleBundler) {
      max = simpleBundler.maxSize();
    }
    return max;
  }
}
