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
import java.util.concurrent.atomic.*;

import org.jppf.utils.*;
import org.slf4j.Logger;

/**
 * A thread pool based on an ubounded queue which supports a core and maximum number of threads, along with a TTL for non-core threads.
 * <p>Core threads are always live and are always prefered when available for new tasks.
 * Non-core threads are created up to the maximum number of threads, after which tasks are simpy put into the queue.
 * @author Laurent Cohen
 */
public class JPPFThreadPool2 extends AbstractExecutorService {
  /**
   * Logger for this class.
   */
  private static final Logger log = LoggingUtils.getLogger(JPPFThreadPool2.class, false);
  /**
   * Determines whether the debug level is enabled in the log configuration, without the cost of a method call.
   */
  private static final boolean traceEnabled = log.isTraceEnabled();
  /**
   * Possible worker states.
   */
  private final static int BUSY = 0, IDLE = 1, TERMINATED = 2;
  /**
   * The tasks queue.
   */
  private final BlockingQueue<Runnable> queue;
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
  private volatile int maxThreads = Integer.MAX_VALUE;
  /**
   * The thread time-to-live.
   */
  private volatile long ttl = Long.MAX_VALUE;
  /**
   * Used to synchronize access to the maps.
   */
  private final Object mainLock = new Object();
  /**
   * The live workers.
   */
  private final Map<Worker, Boolean> workers = new HashMap<>();
  /**
   * Synchronizes access to the workers map.
   */
  private final Object workersLock = new Object();
  /**
   * Generates sequential worker ids.
   */
  private final AtomicInteger workerIdSequence = new AtomicInteger(0);
  /**
   * Peak count of threads.
   */
  private final SynchronizedInteger peakThreadCount = new SynchronizedInteger(0);
  /**
   * Whether this executor has been shutdown.
   */
  private final AtomicBoolean shutdown = new AtomicBoolean(false);
  /**
   * Whether this executor has been shutdown immediately.
   */
  private volatile boolean immediateShutdown;
  /**
   * Whether this executor is terminated.
   */
  private final AtomicBoolean terminated = new AtomicBoolean(false);
  /**
   * Maintains a count of the workers in each possible state.
   */
  private final int[] stateCount = { 0, 0, 0 };
  /**
   *
   */
  private final Stats stats = new Stats();
  /**
   * Whether this pool has any busy worker.
   */
  private volatile boolean hasBusy;

  /**
   * Create a fixed size thread pool with the specified number of threads, infinite thread time-to-live and a {@link Executors#defaultThreadFactory() default thread factory}.
   * @param coreThreads the number of threads in the pool.
   */
  public JPPFThreadPool2(final int coreThreads) {
    this(coreThreads, coreThreads, Long.MAX_VALUE, Executors.defaultThreadFactory());
  }

  /**
   * Initialize with the specified number of core threads and thread factory.
   * @param coreThreads the number of core threads.
   * @param threadFactory the thread factory.
   */
  public JPPFThreadPool2(final int coreThreads, final ThreadFactory threadFactory) {
    this(coreThreads, coreThreads, -1L, threadFactory);
  }

  /**
   * Initialize with the specified number of core threads, maximum number of threads, thread ttl and a {@link Executors#defaultThreadFactory() default thread factory}.
   * @param coreThreads the number of core threads.
   * @param maxThreads the maximum number of threads.
   * @param ttl the thread time-to-live in milliseconds.
   */
  public JPPFThreadPool2(final int coreThreads, final int maxThreads, final long ttl) {
    this(coreThreads, maxThreads, ttl, Executors.defaultThreadFactory());
  }

  /**
   * Initialize with the specified number of core threads, maximum number of threads, ttl and thread factory.
   * @param coreThreads the number of core threads.
   * @param maxThreads the maximum number of threads.
   * @param ttl the thread time-to-live in milliseconds.
   * @param threadFactory the thread factory.
   */
  public JPPFThreadPool2(final int coreThreads, final int maxThreads, final long ttl, final ThreadFactory threadFactory) {
    this(coreThreads, maxThreads, ttl, threadFactory, new LinkedBlockingQueue<Runnable>());
  }

  /**
   * Initialize with the specified number of core threads, maximum number of threads, ttl and thread factory.
   * @param coreThreads the number of core threads.
   * @param maxThreads the maximum number of threads.
   * @param ttl the thread time-to-live in milliseconds.
   * @param threadFactory the thread factory.
   * @param queue the queue to use.
   */
  public JPPFThreadPool2(final int coreThreads, final int maxThreads, final long ttl, final ThreadFactory threadFactory, final BlockingQueue<Runnable> queue) {
    this.queue = queue;
    this.coreThreads = coreThreads;
    this.maxThreads = maxThreads;
    this.ttl = ((ttl <= 0L) || (ttl == Long.MAX_VALUE) ? -1L : ttl);
    this.threadFactory = threadFactory;
    //for (int i=0; i<coreThreads; i++) new Worker(null);
  }

  /**
   * @return the maximum number of threads.
   */
  public int getMaxThreads() {
    return maxThreads;
  }

  /**
   * Set the maximum number of threads.
   * @param maxThreads the new maximum number of threads.
   */
  public void setMaxThreads(final int maxThreads) {
    this.maxThreads = maxThreads;
  }

  /**
   * @return the non-threads' time-to-live.
   */
  public long getTtl() {
    return ttl;
  }

  /**
   * Set the non-core threads' time-to-live.
   * @param ttl the ttl in millis.
   */
  public void setTtl(final long ttl) {
    this.ttl = ttl;
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
   * @param oldState the worker's old state.
   * @param newState the worker's new state.
   */
  private void stateTransition(final int oldState, final int newState) {
    synchronized(mainLock) {
      stateCount[oldState]--;
      stateCount[newState]++;
      hasBusy = stateCount[BUSY] > 0;
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
    else if (!queue.offer(task)) new Worker(task);
    stats.queued.incrementAndGet();
    if (traceEnabled) log.trace("adding task {} to queue", task);
  }

  /**
   * @return whether to start a new worker.
   */
  private boolean addWorker() {
    synchronized(mainLock) {
      final int idle = stateCount[IDLE], busy = stateCount[BUSY];
      return (idle + busy < coreThreads) || ((idle <= 0) && (busy < maxThreads));
    }
  }

  @Override
  public void shutdown() {
    if (shutdown.compareAndSet(false, true)) {
      notifyWorkers(false);
      if (traceEnabled) log.trace(String.format("shutdown requested: queue size = %,d; worker count = %,d", queue.size(), workerCount()));
    }
  }

  @Override
  public List<Runnable> shutdownNow() {
    if (shutdown.compareAndSet(false, true)) {
      immediateShutdown = true;
      notifyWorkers(true);
      if (traceEnabled) log.trace("immediate shutdown requested");
      final List<Runnable> remainingTasks = new ArrayList<>(queue.size());
      queue.drainTo(remainingTasks);
      return remainingTasks;
    }
    return null;
  }

  /**
   * Interrupt the remaining live workers.
   * @param interrupt whether to interrupt the workers.
   */
  private void notifyWorkers(final boolean interrupt) {
    synchronized(workersLock) {
      for (Map.Entry<Worker, Boolean> workerEntry: workers.entrySet()) {
        final Worker worker = workerEntry.getKey();
        worker.workerShutdown = true;
        if (interrupt || (worker.state == IDLE)) worker.interrupt();
      }
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
    final long millis = unit.toMillis(timeout);
    final ConcurrentUtils.Condition condition = new ConcurrentUtils.Condition() {
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
      .append(", maxThreads=").append(maxThreads)
      .append(", ttl=").append(ttl)
      .append(", peakThreads=").append(peakThreadCount.get())
      .append(", stats={").append(stats).append('}')
      .append(']').toString();
  }

  /**
   * Instances of this class represent the worker threads in the pool.
   */
  private final class Worker implements Runnable, Comparable<Worker> {
    /**
     * The associated thread.
     */
    private final Thread thread;
    /**
     * This worker's id.
     */
    private final int id;
    /**
     * The state of this worker.
     */
    private int state;
    /**
     * First task assigned to this worker at construction time, if any.
     */
    private Runnable firstTask;
    /**
     * Whether this worker was notified of an executor shutdown.
     */
    volatile boolean workerShutdown;

    /**
     * Initialize this worker with its first task to execute.
     * An associated thread is also created, using the thread pool's {@link ThreadFactory}.
     * @param firstTask .
     */
    private Worker(final Runnable firstTask) {
      id = workerIdSequence.incrementAndGet();
      this.thread = threadFactory.newThread(this);
      this.firstTask = firstTask;
      this.state = (firstTask == null) ? IDLE: BUSY;
      peakThreadCount.compareAndSet(Operator.LESS_THAN, stateCount[BUSY] + stateCount[IDLE] + 1);
      synchronized(workersLock) {
        workers.put(this, Boolean.TRUE);
      }
      synchronized(mainLock) {
        stateCount[this.state]++;
        hasBusy = stateCount[BUSY] > 0;
      }
      thread.start();
    }

    @Override
    public void run() {
      Runnable task = firstTask;
      firstTask = null;
      while (!shouldStop()) {
        if (task == null) {
          try {
            setState(IDLE);
            if (traceEnabled) log.trace("{} entering IDLE state", this);
            final long timeout = ttl;
            task = (timeout <= 0L) ? queue.take() : queue.poll(timeout, TimeUnit.MILLISECONDS);
            if ((task == null) && (id > coreThreads)) break;
          } catch (final InterruptedException e) {
            if (traceEnabled) log.trace("terminating {} due to interrupt: {}", this, ExceptionUtils.getStackTrace(e));
            break;
          }
        }
        if (task != null) {
          setState(BUSY);
          if (traceEnabled) log.trace("{} executing task {}", this, task);
          try {
            task.run();
          } catch (final Exception e) {
            if (traceEnabled) log.trace(String.format("%s caught exception while executing task %s:%n%s", this, task, ExceptionUtils.getStackTrace(e)));
          } finally {
            task = null;
            stats.completed.incrementAndGet();
          }
        }
      }
      synchronized(workersLock) {
        workers.remove(this);
      }
      setState(TERMINATED);
      if (traceEnabled) log.trace("terminating {}", this);
    }

    /**
     * @return whether this worker should stop processing tasks.
     */
    private boolean shouldStop() {
      return immediateShutdown || (this.workerShutdown && queue.isEmpty() && !hasBusy);
      //return this.workerShutdown && (immediateShutdown || (queue.isEmpty() && !hasBusy));
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
      if (state == newState) return;
      final int oldState = state;
      state = newState;
      if (traceEnabled) log.trace(String.format("%s transitioning from %s to %s", this, this.state, newState));
      stateTransition(oldState, newState);
    }

    @Override
    public String toString() {
      return getClass().getSimpleName() + "[id=" + id + ']';
    }

    @Override
    public int compareTo(final Worker other) {
      return id < other.id ? -1 : (id > other.id ? 1 : 0);
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
