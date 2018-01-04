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
  private static final Logger log = LoggingUtils.getLogger(JPPFThreadPool.class, false);
  /**
   * Determines whether the debug level is enabled in the log configuration, without the cost of a method call.
   */
  private static final boolean traceEnabled = log.isTraceEnabled();
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
   * The live workers.
   */
  private final Map<Worker, Boolean> workers = new HashMap<>();
  /**
   * Synchronizes access to the workers map.
   */
  private final Lock workersLock = new ReentrantLock();
  /**
   * Synchronizes access to the workers map.
   */
  private final Lock mainLock = new ReentrantLock();
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
   *
   */
  private final Stats stats = new Stats();
  /**
   * Current number of idle workers.
   */
  private int idleWorkers;
  /**
   * Current number of busy workers.
   */
  private int busyWorkers;

  /**
   * Create a fixed size thread pool with the specified number of threads, infinite thread time-to-live and a {@link Executors#defaultThreadFactory() default thread factory}.
   * @param coreThreads the number of threads in the pool.
   */
  public JPPFThreadPool(final int coreThreads) {
    this(coreThreads, coreThreads, Long.MAX_VALUE, Executors.defaultThreadFactory());
  }

  /**
   * Initialize with the specified number of core threads and thread factory.
   * @param coreThreads the number of core threads.
   * @param threadFactory the thread factory.
   */
  public JPPFThreadPool(final int coreThreads, final ThreadFactory threadFactory) {
    this(coreThreads, coreThreads, -1L, threadFactory);
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
  public JPPFThreadPool(final int coreThreads, final int maxThreads, final long ttl, final ThreadFactory threadFactory, final BlockingQueue<Runnable> queue) {
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
   * Execute the specified task some time in the future.
   * @param task the task to execute.
   */
  @Override
  public void execute(final Runnable task) {
    if (task == null) throw new NullPointerException("the task cannot be null");
    int count = 0;
    for (;;) {
      if (!queue.offer(task)) {
        if (addWorker() || (++count >= 10)) {
          new Worker(task);
          break;
        }
      } else {
        stats.queued.incrementAndGet();
        break;
      }
    }
    /*
    if (addWorker()) new Worker(task);
    else if (!queue.offer(task)) new Worker(task);
    else stats.queued.incrementAndGet();
    */
    stats.submitted.incrementAndGet();
    if (traceEnabled) log.trace("adding task {} to queue", task);
  }

  /**
   * @return whether to start a new worker.
   */
  private boolean addWorker() {
    final int idle, busy;
    synchronized(mainLock) {
      idle = idleWorkers;
      busy = busyWorkers;
    }
    return (idle + busy < coreThreads) || ((idle <= 0) && (busy < maxThreads));
  }

  @Override
  public void shutdown() {
    if (shutdown.compareAndSet(false, true)) {
      notifyWorkers(false);
      if (traceEnabled) log.trace(String.format("shutdown requested: queue size = %,d; worker count = %,d", queue.size(), idleWorkers + busyWorkers));
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
      for (final Map.Entry<Worker, Boolean> workerEntry: workers.entrySet()) {
        final Worker worker = workerEntry.getKey();
        worker.workerShutdown = true;
        if (interrupt || worker.idle) worker.interrupt();
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
    final int size;
    synchronized(workersLock) {
      size = workers.size();
    }
    final boolean b = (size <= 0) && queue.isEmpty();
    terminated.set(b);
    return b;
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
    private boolean idle;
    /**
     * First task assigned to this worker at construction time, if any.
     */
    private Runnable firstTask;
    /**
     * Whether this worker was notified of an executor shutdown.
     */
    volatile boolean workerShutdown;
    /**
     * 
     */
    private boolean hasBusy;

    /**
     * Initialize this worker with its first task to execute.
     * An associated thread is also created, using the thread pool's {@link ThreadFactory}.
     * @param firstTask .
     */
    private Worker(final Runnable firstTask) {
      id = workerIdSequence.incrementAndGet();
      this.thread = threadFactory.newThread(this);
      this.firstTask = firstTask;
      this.idle = (firstTask == null);
      final int size;
      synchronized(workersLock) {
        workers.put(this, Boolean.TRUE);
        size = workers.size();
      }
      peakThreadCount.compareAndSet(Operator.LESS_THAN, size);
      synchronized(mainLock) {
        if (idle) idleWorkers++;
        else busyWorkers++;
        hasBusy = busyWorkers > 0;
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
            setIdle(true);
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
          setIdle(false);
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
      synchronized(mainLock) {
        if (idle) idleWorkers--;
        else busyWorkers--;
        hasBusy = busyWorkers > 0;
      }
      if (traceEnabled) log.trace("terminating {}", this);
    }

    /**
     * @return whether this worker should stop processing tasks.
     */
    private boolean shouldStop() {
      return immediateShutdown || (this.workerShutdown && queue.isEmpty() && hasBusy);
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
     * @param idle the new state.
     */
    private void setIdle(final boolean idle) {
      if (this.idle == idle) return;
      if (traceEnabled) log.trace(String.format("%s transitioning from %s to %s", this, this.idle, idle));
      this.idle = idle;
      updateWorkerCounts(this.idle ? -1 : 1);
    }

    /**
     * Called when a worker changes state.
     * @param update .
     */
    private void updateWorkerCounts(final int update) {
      synchronized(mainLock) {
        idleWorkers -= update;
        busyWorkers += update;
        hasBusy = busyWorkers > 0;
      }
    }

    @Override
    public String toString() {
      return getClass().getSimpleName() + "[id=" + id + ']';
    }

    @Override
    public int compareTo(final Worker other) {
      //return id < other.id ? -1 : (id > other.id ? 1 : 0);
      if (idle) return other.idle ? compare(id, other.id) : -1;
      return other.idle ? 1 : compare(id, other.id); 
    }

    /**
     * 
     * @param i1 .
     * @param i2 .
     * @return .
     */
    private int compare(final int i1, final int i2) {
      return i1 < i2 ? -1 : (i1 > i2 ? 1 : 0);
    }
  }

  /**
   * Statistic for the executor.
   */
  private static class Stats {
    /**
     * 
     */
    private AtomicInteger submitted = new AtomicInteger(), queued = new AtomicInteger(), completed = new AtomicInteger();

    @Override
    public String toString() {
      return String.format("submitted: %,d, queued=%,d, completed: %,d", submitted.get(), queued.get(), completed.get());
    }
  }
}
