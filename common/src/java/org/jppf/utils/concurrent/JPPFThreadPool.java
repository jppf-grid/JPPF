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

package org.jppf.utils.concurrent;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.*;

import org.jppf.client.Operator;
import org.jppf.utils.*;
import org.slf4j.Logger;

/**
 * A thread pool based on an ubounded queue which supports a core and maximum number of threads, along with a TTL for non-core threads.
 * <p>Core threads are always live and are always prefered when available for new tasks.
 * Non-core threads are created up to the maximum number of threads, after which tasks are simpy put into the queue.
 * @author Laurent Cohen
 */
public class JPPFThreadPool extends AbstractExecutorService {
  /**
   * Logger for this class.
   */
  private static Logger log = LoggingUtils.getLogger(JPPFThreadPool.class, false);
  /**
   * Determines whether the debug level is enabled in the log configuration, without the cost of a method call.
   */
  private static boolean traceEnabled = log.isTraceEnabled();
  /**
   * Possible worker states.
   */
  private final static int NEW = 0, BUSY = 1, IDLE = 2, TERMINATED = 3;
  /**
   * Performance monitor capacity.
   */
  //private final static int PERFMON_CAPACITY = 5000;
  /**
   * The tasks queue.
   */
  private final BlockingQueue<Runnable> queue = new LinkedBlockingQueue<>();
  /**
   * The thread factory.
   */
  private final ThreadFactory threadFactory;
  /**
   * The number of core threads.
   */
  private final int coreThreads;
  /**
   * The maximum number of threads.
   */
  private SynchronizedInteger maxThreads = new SynchronizedInteger(Integer.MAX_VALUE);
  /**
   * The thread time-to-live.
   */
  private AtomicLong ttl = new AtomicLong(Long.MAX_VALUE);
  /**
   * Used to synchronize access to the maps.
   */
  private final Lock mainLock = new ReentrantLock();
  /**
   * The live workers.
   */
  private final Set<Worker> workers = new HashSet<>();
  /**
   * Generates sequential worker ids.
   */
  private final SynchronizedInteger workerIdSequence = new SynchronizedInteger(0);
  /**
   * Peak count of threads.
   */
  private final SynchronizedInteger peakThreadCount = new SynchronizedInteger(0);
  /**
   * Whether this executor has been shutdown.
   */
  private final SynchronizedBoolean shutdown = new SynchronizedBoolean(false);
  /**
   * Whether this executor has been shutdown immediately.
   */
  private final SynchronizedBoolean immediateShutdown = new SynchronizedBoolean(false);
  /**
   * Whether this executor is terminated.
   */
  private final SynchronizedBoolean terminated = new SynchronizedBoolean(false);
  /**
   * Maintains a count of the workers in each possible state.
   */
  private final int[] stateCount = { 0, 0, 0, 0 };
  /**
   *
   */
  private final Stats stats = new Stats();

  /**
   * Create a fixed size thread pool with the specified number of threads, infinite thread time-to-live and a {@link Executors#defaultThreadFactory() default thread factory}.
   * @param coreThreads the number of threads in the pool.
   */
  public JPPFThreadPool(final int coreThreads) {
    this(coreThreads, coreThreads, Long.MAX_VALUE, Executors.defaultThreadFactory());
  }

  /**
   * Initialize with the specified number of core threads, maximum number of threads, thread ttl and a {@link Executors#defaultThreadFactory() default thread factory}.
   * @param coreThreads the number of core threads.
   * @param maxThreads the maximum number of threads.
   * @param ttl the thread time-to-live in milliseconds.
   */
  public JPPFThreadPool(final int coreThreads, final int maxThreads, final long ttl) {
    this(coreThreads, maxThreads, ttl, Executors.defaultThreadFactory());
  }

  /**
   * Initialize with the specified number of core threads, maximum number of threads, ttl and thread factory.
   * @param coreThreads the number of core threads.
   * @param maxThreads the maximum number of threads.
   * @param ttl the thread time-to-live in milliseconds.
   * @param threadFactory the thread factory.
   */
  public JPPFThreadPool(final int coreThreads, final int maxThreads, final long ttl, final ThreadFactory threadFactory) {
    this.coreThreads = coreThreads;
    this.maxThreads.set(maxThreads);
    this.ttl.set(ttl);
    this.threadFactory = threadFactory;
    for (int i=0; i<coreThreads; i++) new Worker(null);
  }

  /**
   * @return the maximum number of threads.
   */
  public int getMaxThreads() {
    return maxThreads.get();
  }

  /**
   * Set the maximum number of threads.
   * @param maxThreads the new maximum number of threads.
   */
  public void setMaxThreads(final int maxThreads) {
    this.maxThreads.set(maxThreads);
  }

  /**
   * @return the non-threads' time-to-live.
   */
  public long getTtl() {
    return ttl.get();
  }

  /**
   * Set the non-core threads' time-to-live.
   * @param ttl the ttl in millis.
   */
  public void setTtl(final long ttl) {
    this.ttl.set(ttl);
  }

  /**
   * @return the peak thread count for this thread pool.
   */
  public int getPeakThreadCount() {
    return peakThreadCount.get();
  }

  /**
   * @return the number of live workers.
   */
  private int workerCount() {
    synchronized(mainLock) {
      return stateCount[IDLE] + stateCount[BUSY];
    }
  }

  /**
   * Called when a worker changes state.
   * @param worker the worker whose state is changing.
   * @param oldState the worker's old state.
   * @param newState the worker's new state.
   */
  private void stateTransition(final Worker worker, final int oldState, final int newState) {
    synchronized(mainLock) {
      if (newState == NEW) {
        peakThreadCount.compareAndSet(Operator.LESS_THAN, stateCount[BUSY] + stateCount[IDLE] + 1);
        workers.add(worker);
      } else if (newState == TERMINATED) {
        workers.remove(worker);
      }
      if (oldState >= 0) stateCount[oldState]--;
      if (newState >= 0) stateCount[newState]++;
    }
  }

  /**
   * Execute the specified task some time in the future.
   * @param task the task to execute.
   */
  @Override
  public void execute(final Runnable task) {
    if (task == null) throw new NullPointerException("the task cannot be null");
    if (shutdown.get()) throw new RejectedExecutionException("this executor has been shut down");
    if (addWorker()) new Worker(task);
    else queue.offer(task);
    stats.queued.incrementAndGet();
    if (traceEnabled) log.trace("adding task {} to queue", task);
  }

  /**
   * @return whether to start a new worker.
   */
  private boolean addWorker() {
    synchronized(mainLock) {
      int idle = stateCount[IDLE];
      return (idle <= 0) && (idle + stateCount[BUSY] < getMaxThreads());
    }
  }

  @Override
  public void shutdown() {
    if (shutdown.compareAndSet(false, true)) {
      if (traceEnabled) log.trace(String.format("shutdown requested: queue size = %,d; worker count = %,d", queue.size(), workerCount()));
    }
  }

  @Override
  public List<Runnable> shutdownNow() {
    if (shutdown.compareAndSet(false, true)) {
      immediateShutdown.set(true);
      if (traceEnabled) log.trace("immediate shutdown requested");
      List<Runnable> remainingTasks = new ArrayList<>(queue.size());
      queue.drainTo(remainingTasks);
      interruptWorkers();
      return remainingTasks;
    }
    return null;
  }

  /**
   * Interrupt the remaining live workers.
   */
  private void interruptWorkers() {
    synchronized(mainLock) {
      for (Worker worker: workers) worker.interrupt();
    }
  }

  @Override
  public boolean isShutdown() {
    return shutdown.get();
  }

  @Override
  public boolean isTerminated() {
    if (!shutdown.get()) return false;
    if (terminated.get()) return true;
    synchronized(mainLock) {
      terminated.set((workerCount() <= 0) && queue.isEmpty());
    }
    return terminated.get();
  }

  @Override
  public boolean awaitTermination(final long timeout, final TimeUnit unit) throws InterruptedException {
    if (isTerminated()) return true;
    long millis = unit.toMillis(timeout);
    ConcurrentUtils.Condition condition = new ConcurrentUtils.Condition() {
      @Override
      public boolean evaluate() {
        return isTerminated();
      }
    };
    return ConcurrentUtils.awaitCondition(new ThreadSynchronization(), condition, millis, 10L);
  }

  @Override
  public String toString() {
    return new StringBuilder(getClass().getSimpleName()).append('[')
      .append("coreThreads=").append(coreThreads)
      .append(", maxThreads=").append(maxThreads.get())
      .append(", ttl=").append(ttl.get())
      .append(", peakThreads=").append(peakThreadCount.get())
      .append(", stats={").append(stats).append('}')
      .append(']').toString();
  }

  /**
   * Instances of this class represent the worker threads in the poool.
   */
  private final class Worker implements Runnable {
    /**
     * The associated thread.
     */
    private final Thread thread;
    /**
     * This worker's id.
     */
    private final Integer id;
    /**
     * The state of this worker.
     */
    private int state = -1;
    /**
     * 
     */
    private Runnable firstTask;

    /**
     * Initialize this worker with its first task to execute.
     * An associated thread is also created, using the thread pool's {@link ThreadFactory}.
     * @param firstTask .
     */
    private Worker(final Runnable firstTask) {
      id = Integer.valueOf(workerIdSequence.incrementAndGet());
      this.thread = threadFactory.newThread(this);
      this.firstTask = firstTask;
      setState(NEW);
      thread.start();
    }

    @Override
    public void run() {
      while (!shouldStop()) {
        Runnable task = firstTask;
        if (firstTask != null) firstTask = null;
        if (traceEnabled) log.trace("{} entering IDLE state", this);
        try {
          setState(IDLE);
          long ttl = getTtl();
          long start = System.currentTimeMillis();
          while ((task == null) && (System.currentTimeMillis() - start < ttl) && !shouldStop()) {
            task = queue.poll(1L, TimeUnit.MILLISECONDS);
          }
        } catch (InterruptedException e) {
          if (traceEnabled) log.trace("terminating {} due to interrupt: {}", this, ExceptionUtils.getStackTrace(e));
          break;
        }
        if (task != null) {
          if (traceEnabled) log.trace("{} executing task {}", this, task);
          setState(BUSY);
          try {
            task.run();
          } catch (Exception e) {
            if (traceEnabled) log.trace(String.format("%s caught exception while executing task %s:%n%s", this, task, ExceptionUtils.getStackTrace(e)));
          } finally {
            stats.completed.incrementAndGet();
          }
        } else {
          if (id > coreThreads) {
            if (traceEnabled) log.trace("terminating {}", this);
            break;
          }
        }
      }
      setState(TERMINATED);
    }

    /**
     * @return whether this worker should stop processing tasks.
     */
    private boolean shouldStop() {
      return immediateShutdown.get() || (shutdown.get() && queue.isEmpty());
    }

    /**
     * Interrupt this worker.
     */
    private void interrupt() {
      if ((thread != null) && thread.isAlive() && !thread.isInterrupted()) thread.interrupt();
    }

    /**
     * Set the state of this worker.
     * @param newState the new state.
     */
    private void setState(final int newState) {
      if (this.state == newState) return;
      int oldState = state;
      this.state = newState;
      if (traceEnabled) log.trace(String.format("%s transitioning from %s to %s", this, this.state, newState));
      stateTransition(this, oldState, newState);
    }

    @Override
    public String toString() {
      return getClass().getSimpleName() + "[id=" + id + ']';
    }
  }

  /**
   * Statistic for the executor.
   */
  private static class Stats {
    /**
     * 
     */
    private SynchronizedInteger queued = new SynchronizedInteger(), completed = new SynchronizedInteger();

    @Override
    public String toString() {
      return String.format("queued: %,d, completed: %,d", queued.get(), completed.get());
    }
  }
}
