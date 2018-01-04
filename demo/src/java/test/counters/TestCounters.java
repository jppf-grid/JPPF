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

package test.counters;

import java.lang.management.ManagementFactory;
import java.util.*;

/**
 * 
 * @author Laurent Cohen
 */
public class TestCounters {
  /**
   * 
   * @param args not used.
   */
  public static void main(final String[] args) {
    try {
      // do some warmup.
      final int warmupIterations = 100_000;
      System.out.println("starting warmup");
      test(new LongCounterSynchronized(), 1, warmupIterations);
      test(new LongCounterLock(), 1, warmupIterations);
      test(new LongCounterAtomic(), 1, warmupIterations);
      System.out.println("warmup done\n");
      final int max_iterations = 12_000_000;
      final int[] threadCounts = { 1, 2, 3, 4, 6, 8, 12, 16, 24, 32, 48, 64, 96, 128, 192, 256 };
      final Map<String, List<TimeAndCpu>> results = new LinkedHashMap<>();
      final int length = "LongCounter".length();
      for (int i=0; i<threadCounts.length; i++) {
        final LongCounter[] counters = { new LongCounterAtomic(), new LongCounterSynchronized(), new LongCounterLock() };
        final int nbThreads = threadCounts[i];
        final int iterations = max_iterations / nbThreads;
        System.out.printf("Testing  with %,d threads and %,d iterations per thread\n", nbThreads, iterations);
        for (final LongCounter counter: counters) {
          final String name = counter.getClass().getSimpleName().substring(length);
          final TimeAndCpu res = test(counter, nbThreads, iterations);
          System.out.printf("  %-12s done in %,d ms, avg cpu load = %.3f %%\n", name, res.time, 100d * res.cpuLoad);
          List<TimeAndCpu> list = results.get(name);
          if (list == null) {
            list = new ArrayList<>(threadCounts.length);
            results.put(name, list);
          }
          list.add(res);
        }
      }
      printResultsAsCSV(results, threadCounts);
    } catch (final Exception e) {
      e.printStackTrace();
    }
  }

  /**
   * 
   * @param counter the counter to test.
   * @param nbThreads the number of threads to use.
   * @param iterations the number of iterations to run for each thread.
   * @return the test elapsed time in nanoseconds.
   * @throws Exception if any error occurs.
   */
  private static TimeAndCpu test(final LongCounter counter, final int nbThreads, final int iterations) throws Exception {
    final TestThread[] threads = new TestThread[nbThreads];
    for (int i=0; i<nbThreads; i++) threads[i] = new TestThread(i+1, counter, iterations);
    long elapsed = System.nanoTime();
    for (int i=0; i<nbThreads; i++) threads[i].start();
    for (int i=0; i<nbThreads; i++) threads[i].join();
    elapsed = System.nanoTime() - elapsed;
    long totalCpu = 0L;
    for (int i=0; i<nbThreads; i++) totalCpu += threads[i].getCpu();
    final double load = (double) totalCpu/(double) (elapsed * Runtime.getRuntime().availableProcessors());
    return new TimeAndCpu(elapsed/1_000_000L, Math.min(1d, load));
  }

  /**
   * Print the results to the console as CSV.
   * @param results holds the test results.
   * @param threadCounts holds the number of threads used for each test.
   */
  private static void printResultsAsCSV(final Map<String, List<TimeAndCpu>> results, final int[] threadCounts) {
    System.out.println("\nCSV results:");
    final Set<String> nameSet = results.keySet();
    final StringBuilder sb = new StringBuilder();
    sb.append("\"threads\"");
    for (String name: nameSet) sb.append(String.format(",\"%1$s\",\"%1$s\"", name));
    sb.append('\n');
    for (int i=0; i<nameSet.size(); i++) sb.append(",\"time\",\"% cpu\"");
    sb.append('\n');
    for (int i=0; i<threadCounts.length; i++) {
      sb.append(threadCounts[i]);
      for (final String name: nameSet) {
        final TimeAndCpu tac = results.get(name).get(i);
        sb.append(',').append(tac.time);
        sb.append(',').append(tac.cpuLoad);
      }
      sb.append('\n');
    }
    System.out.println(sb.toString());
  }

  /**
   * Holds elapsed time and average cpu load for a single test.
   */
  private static class TimeAndCpu {
    /**
     * Elapsed time in nanoseconds.
     */
    public final long time;
    /**
     * Average cpu load.
     */
    public final double cpuLoad;

    /**
     * Initialize with the specified values.
     * @param time elapsed time.
     * @param cpuLoad average cpu load.
     */
    public TimeAndCpu(final long time, final double cpuLoad) {
      this.time = time;
      this.cpuLoad = cpuLoad;
    }
  }

  /**
   * Performs operations on a {@link LongCounter} shared with other threads,
   * for a given number of iterations.  
   */
  private static class TestThread extends Thread {
    /**
     * The counter object ot use.
     */
    public final LongCounter counter;
    /**
     * Number of iterations of the same (set of) operations to perform.
     */
    private final int iterations;
    /**
     * Measures the cpu time used by this thread.
     */
    private long cpu;

    /**
     * Initiialize this test thread.
     * @param idx the index of this thread, used solely to set the name of this thread.
     * @param counter the counter to test.
     * @param iterations the number of iterations to run.
     */
    public TestThread(final int idx, final LongCounter counter, final int iterations) {
      super("TestThread[" + idx + "]");
      this.counter = counter;
      this.iterations = iterations;
    }

    @Override
    public void run() {
      cpu = ManagementFactory.getThreadMXBean().getThreadCpuTime(this.getId());
      for (int i=0; i<iterations; i++) {
        counter.decrementAndGet();
        counter.incrementAndGet();
        counter.addAndGet(-1L);
        counter.set(counter.get() + 1);
      }
      cpu = ManagementFactory.getThreadMXBean().getThreadCpuTime(this.getId()) - cpu;
    }

    /**
     * Get the elapsed time.
     * @return the elapsed time in nanos.
     */
    public long getCpu() {
      return cpu;
    }
  }
}
