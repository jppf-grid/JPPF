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

package org.jppf.management.diagnostics;

import java.lang.management.*;
import java.util.concurrent.atomic.AtomicLong;

import org.jppf.utils.*;
import org.jppf.utils.concurrent.ThreadSynchronization;
import org.jppf.utils.configuration.JPPFProperties;
import org.slf4j.*;

/**
 * This class computes, at regular intervals, the approximative value of the CPU load
 * for the current JVM. 
 * <p>The computed value is equal to <code>sum<sub>i</sub>(thread_used_cpu<sub>i</sub>) / interval</code>, for all the
 * live threads of the JVM at the time of the computation.
 * <p>Thus, errors may occur, since many threads may have been created then died between two computations.
 * However, in most cases this is a reasonable approximation, whose computation does not tax the CPU too heavily.
 * @author Laurent Cohen
 */
public class CPUTimeCollector extends ThreadSynchronization implements Runnable {
  /**
   * Logger for this class.
   */
  private static Logger log = LoggerFactory.getLogger(CPUTimeCollector.class);
  /**
   * The interval between two computations in milliseconds.
   * Taken from the value of configuration property "jppf.cpu.load.compuation.interval".
   * It defaults to 1000 (1 second) if the property is unspecified.
   */
  protected static long INTERVAL = JPPFConfiguration.get(JPPFProperties.CPU_LOAD_COMPUTATION_INTERVAL);
  /**
   * The total CPU time in milliseconds.
   */
  private AtomicLong totalCpuTime = new AtomicLong(0L);
  /**
   * The average load over the interval.
   */
  private AtomicLong load = new AtomicLong(0L);
  /**
   * Reference to the platform's thread MXBean.
   */
  ThreadMXBean threadMXBean = ManagementFactory.getThreadMXBean();
  /**
   * Reference to the platform's operating system MXBean.
   */
  OperatingSystemMXBean systemMXBean = ManagementFactory.getOperatingSystemMXBean();

  @Override
  public void run() {
    try {
      while (!isStopped()) {
        final long oldValue = totalCpuTime.get();
        final long start = System.nanoTime();
        final long[] ids = threadMXBean.getAllThreadIds();
        long time = 0L;
        for (final long id: ids) {
          final long l = threadMXBean.getThreadCpuTime(id);
          if (l >= 0L) time += l;
        }
        final long cpuTime = time / 1000000L;
        totalCpuTime.set(cpuTime);

        final double d = (double) (cpuTime - oldValue) / (double) (INTERVAL * systemMXBean.getAvailableProcessors());
        load.set(Double.doubleToLongBits(d));
        final long sleepTime = INTERVAL - ((System.nanoTime() - start) / 1_000_000L);
        //log.info("computed difference ms = " + (cpuTime - oldValue) + ", sleep time = " + sleepTime);
        goToSleep(sleepTime <= 0L ? INTERVAL : sleepTime);
      }
    } catch (final Exception e) {
      log.error(e.getMessage(), e);
    }
  }

  /**
   * Get the CPU load as the ratio of <code>totalCpuTime / computationInterval</code>.
   * @return the CPU load as a double value in the range <code>[0, 1]</code>.
   */
  public double getLoad() {
    double d = Double.longBitsToDouble(load.get());
    if (d > 1d) d = 1d;
    return d;
  }
}
