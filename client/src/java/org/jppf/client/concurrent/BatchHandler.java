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

package org.jppf.client.concurrent;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import java.util.concurrent.locks.*;

import org.jppf.JPPFException;
import org.jppf.client.JPPFJob;
import org.jppf.client.event.JobListener;
import org.jppf.client.taskwrapper.JPPFAnnotatedTask;
import org.jppf.node.protocol.Task;
import org.jppf.utils.*;
import org.slf4j.*;

/**
 * This class is a processor for tasks submitted via a {@link JPPFExecutorService}.
 * It handles both normal mode and batching mode, where the tasks throughput is streamlined
 * by specifying how many tasks should be sent to the grid, and a which intervals.
 * @author Laurent Cohen
 * @exclude
 */
public class BatchHandler extends ThreadSynchronization implements Runnable {
  /**
   * Logger for this class.
   */
  private static Logger log = LoggerFactory.getLogger(BatchHandler.class);
  /**
   * Determines whether debug-level logging is enabled.
   */
  private static boolean debugEnabled = LoggingUtils.isDebugEnabled(log);
  /**
   * Count of jobs created by this executor service.
   */
  private static final AtomicLong JOB_COUNT = new AtomicLong(0);
  /**
   * The minimum number of tasks that must be submitted before they are sent to the server.
   */
  private int batchSize;
  /**
   * The maximum time to wait before the next batch of tasks is to be sent for execution.
   */
  private long batchTimeout;
  /**
   * The JPPFExecutorService whose tasks are batched.
   */
  private final JPPFExecutorService executor;
  /**
   * The job to send for execution. If the reference is ont null, then the job is sent immediately.
   */
  private final AtomicReference<JPPFJob> currentJobRef = new AtomicReference<>(null);
  /**
   * The next job being prepared. It will be assigned to <code>currentJobRef</code> when it is ready for execution,
   * depending on the batching parameters.
   */
  private final AtomicReference<JPPFJob> nextJobRef = new AtomicReference<>(null);
  /**
   * The time at which we started to count for the the timeout.
   */
  private long start;
  /**
   * Time elapsed since the start.
   */
  private long elapsed;
  /**
   * Used to synchronize access to <code>currentJobRef</code> and <code>nextJobRef</code>.
   */
  private final ReentrantLock lock = new ReentrantLock(true);
  /**
   * Represents a condition to await for and corresponding to when <code>currentJobRef</code> is not null.
   */
  private final Condition jobReady = lock.newCondition();
  /**
   * Represents a condition to await for and corresponding to when <code>currentJobRef</code> is not null.
   */
  private final Condition submittingJob = lock.newCondition();
  /**Size
   * The configuration for this batch handler.
   */
  private ExecutorServiceConfiguration config = new ExecutorServiceConfigurationImpl();

  /**
   * Default constructor.
   * @param executor the JPPFExecutorService whose tasks are batched.
   */
  BatchHandler(final JPPFExecutorService executor) {
    this(executor, 0, 0L);
  }

  /**
   * Initialize with the specified executor service, batch size and batch tiemout.
   * @param executor the JPPFExecutorService whose tasks are batched.
   * @param batchSize the minimum number of tasks that must be submitted before they are sent to the server.
   * @param batchTimeout the maximum time to wait before the next batch of tasks is to be sent for execution.
   */
  BatchHandler(final JPPFExecutorService executor, final int batchSize, final long batchTimeout) {
    this.executor = executor;
    this.batchSize = batchSize;
    this.batchTimeout = batchTimeout;
    nextJobRef.set(createJob());
  }

  /**
   * Get the minimum number of tasks that must be submitted before they are sent to the server.
   * @return the batch size as an int.
   */
  int getBatchSize() {
    lock.lock();
    try {
      return batchSize;
    } finally {
      lock.unlock();
    }
  }

  /**
   * Set the minimum number of tasks that must be submitted before they are sent to the server.
   * @param batchSize the batch size as an int.
   */
  void setBatchSize(final int batchSize) {
    lock.lock();
    try {
      this.batchSize = batchSize;
      jobReady.signal();
    } finally {
      lock.unlock();
    }
  }

  /**
   * Get the maximum time to wait before the next batch of tasks is to be sent for execution.
   * @return the timeout as a long.
   */
  long getBatchTimeout() {
    lock.lock();
    try {
      return batchTimeout;
    } finally {
      lock.unlock();
    }
  }

  /**
   * Set the maximum time to wait before the next batch of tasks is to be sent for execution.
   * @param batchTimeout the timeout as a long.
   */
  void setBatchTimeout(final long batchTimeout) {
    lock.lock();
    try {
      this.batchTimeout = batchTimeout;
      jobReady.signal();
    } finally {
      lock.unlock();
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void run() {
    start = System.nanoTime();
    while (!isStopped()) {
      try {
        lock.lock();
        try {
          while (!isStopped() && (currentJobRef.get() == null)) {
            long batchTimeout = getBatchTimeout();
            if (batchTimeout > 0) {
              long n = batchTimeout - elapsed;
              if (n > 0) jobReady.await(n, TimeUnit.MILLISECONDS);
            }
            else jobReady.await();
            updateNextJob(false);
          }
          if (isStopped()) break;
          JPPFJob job = currentJobRef.get();
          if (debugEnabled) log.debug("submitting job " + job.getName() + " with " + job.getJobTasks().size() + " tasks");
          configureJob(job);
          executor.submitJob(job);
          currentJobRef.set(null);
          elapsed = (System.nanoTime() - start) / 1_000_000L;
          submittingJob.signal();
        } finally {
          lock.unlock();
        }
      } catch (Exception e) {
        log.error(e.getMessage(), e);
      }
    }
  }

  /**
   * Update the next job to submit if one is ready.
   * @param sendSignal true if signal is to be sent, false otherwise.
   */
  private void updateNextJob(final boolean sendSignal) {
    JPPFJob job = nextJobRef.get();
    int size = job.getJobTasks().size();
    long batchTimeout = getBatchTimeout();
    int batchSize = getBatchSize();
    if (batchTimeout > 0L) elapsed = (System.nanoTime() - start) / 1_000_000L;
    if (size == 0) {
      if ((batchTimeout > 0L) && (elapsed >= batchTimeout)) resetTimeout();
      return;
    }
    if (((batchTimeout > 0L) && (elapsed >= batchTimeout)) ||
        ((batchSize > 0) && (size >= batchSize)) ||
        ((batchSize <= 0) && (batchTimeout <= 0L))) {
      currentJobRef.set(job);
      nextJobRef.set(createJob());
      resetTimeout();
      if (sendSignal) {
        jobReady.signal();
        try {
          submittingJob.await();
        } catch (InterruptedException e) {
          throw new RejectedExecutionException(e);
        }
      }
    }
  }

  /**
   * Reset the timeout counter.
   */
  private void resetTimeout() {
    start = System.nanoTime();
    elapsed = 0L;
  }

  /**
   * Submit a {@link Task} that returns the specified type of result.
   * @param <T> the type of result returned by the task.
   * @param task the task to submit.
   * @param result this parameter is only here for type inference (I know, it's ugly).
   * @return a {@link Future} representing pending completion of the task.
   */
  <T> Future<T> addTask(final Task<?> task, final T result) {
    lock.lock();
    try {
      if (debugEnabled) log.debug("submitting one JPPFTask");
      Future<T> future = null;
      JPPFJob job = nextJobRef.get();
      try {
        job.add(task);
        future = new JPPFTaskFuture<>(job, task.getPosition());
      } catch (JPPFException e) {
        log.error(e.getMessage(), e);
        throw new RejectedExecutionException(e);
      }
      updateNextJob(true);
      return future;
    } finally {
      lock.unlock();
    }
  }

  /**
   * Submit a {@link Runnable} that returns the specified type of result.
   * @param <T> the type of result returned by the task.
   * @param task the task to submit.
   * @param result the result for the task.
   * @return a {@link Future} representing pending completion of the task.
   */
  <T> Future<T> addTask(final Runnable task, final T result) {
    lock.lock();
    try {
      if (debugEnabled) log.debug("submitting one Runnable task with result");
      Future<T> future = null;
      JPPFJob job = nextJobRef.get();
      try {
        JPPFAnnotatedTask t = (JPPFAnnotatedTask) job.add(task);
        t.setResult(result);
        configureTask(t);
        future = new JPPFTaskFuture<>(job, t.getPosition());
      } catch (JPPFException e) {
        log.error(e.getMessage(), e);
        throw new RejectedExecutionException(e);
      }
      updateNextJob(true);
      return future;
    } finally {
      lock.unlock();
    }
  }

  /**
   * Submit a task for execution.
   * @param <T> the type of results.
   * @param task the task to submit.
   * @return a {@link Future} representing pending completion of the task.
   */
  <T> Future<T> addTask(final Callable<T> task) {
    lock.lock();
    try {
      if (debugEnabled) log.debug("submitting one Callable Task");
      Future<T> future = null;
      JPPFJob job = nextJobRef.get();
      try {
        JPPFAnnotatedTask jppfTask = (JPPFAnnotatedTask) job.add(task);
        configureTask(jppfTask);
        future = new JPPFTaskFuture<>(job, jppfTask.getPosition());
      } catch (JPPFException e) {
        log.error(e.getMessage(), e);
        throw new RejectedExecutionException(e);
      }
      updateNextJob(true);
      return future;
    } finally {
      lock.unlock();
    }
  }

  /**
   * Submit a list of tasks for execution.
   * @param <T> the type of the results.
   * @param tasks the tasks to submit.
   * @return a pair representing the result collector used in the current job, along with the position of the first task.
   */
  <T> Pair<JPPFJob, Integer> addTasks(final Collection<? extends Callable<T>> tasks) {
    lock.lock();
    try {
      if (debugEnabled) log.debug("submitting " + tasks.size() + " Callable Tasks");
      Pair<JPPFJob, Integer> pair = null;
      JPPFJob job = nextJobRef.get();
      int start = 0;
      try {
        List<Task<?>> jobTasks = job.getJobTasks();
        start = jobTasks.size();
        for (Callable<?> task: tasks) {
          Task<?> t = job.add(task);
          configureTask((JPPFAnnotatedTask) t);
        }
      } catch (JPPFException e) {
        log.error(e.getMessage(), e);
        throw new RejectedExecutionException(e);
      }
      pair = new Pair<>(job, start);
      updateNextJob(true);
      return pair;
    } finally {
      lock.unlock();
    }
  }

  /**
   * Create a new job with a FutureResultCollector a results listener.
   * @return a {@link JPPFJob} instance.
   */
  private JPPFJob createJob() {
    JPPFJob job = new JPPFJob();
    job.setName(getClass().getSimpleName() + " job " + JOB_COUNT.incrementAndGet());
    job.setBlocking(false);
    job.addJobListener(executor);
    if (debugEnabled) log.debug("created job " + job);
    //configureJob(job);
    return job;
  }

  /**
   * Configure the specified job using the current configuration.
   * @param job the job to configure.
   */
  private synchronized void configureJob(final JPPFJob job) {
    if (config != null) {
      JobConfiguration jc = config.getJobConfiguration();
      job.setSLA(jc.getSLA());
      job.setClientSLA(jc.getClientSLA());
      job.setMetadata(jc.getMetadata());
      job.setPersistenceManager(jc.getPersistenceManager());
      job.setDataProvider(jc.getDataProvider());
      for (JobListener listener: jc.getAllJobListeners()) job.addJobListener(listener);
      for (ClassLoader cl: jc.getClassLoaders()) executor.client.registerClassLoader(cl, job.getUuid());
    }
  }

  /**
   * Configure the specified job using the current configuration.
   * @param task the task to configure.
   */
  private synchronized void configureTask(final JPPFAnnotatedTask task) {
    if (config != null) {
      TaskConfiguration tc = config.getTaskConfiguration();
      task.setCancelCallback(tc.getOnCancelCallback());
      task.setTimeoutCallback(tc.getOnTimeoutCallback());
      task.setTimeoutSchedule(tc.getTimeoutSchedule());
    }
  }

  /**
   * Close this batch handler.
   */
  void close() {
    setStopped(true);
    lock.lock();
    try {
      jobReady.signalAll();
    } finally {
      lock.unlock();
    }
  }

  /**
   * Get the configuration for this batch handler.
   * @return an {@link ExecutorServiceConfiguration} instance.
   */
  synchronized ExecutorServiceConfiguration getConfig() {
    return config;
  }

  /**
   * Set the configuration for this batch handler.
   * @param config an {@link ExecutorServiceConfiguration} instance.
   * @throws IllegalArgumentException if the new configuration is null.
   */
  synchronized void setConfig(final ExecutorServiceConfiguration config) throws IllegalArgumentException {
    if (config == null) throw new IllegalArgumentException("configuration cannot be null");
    this.config = config;
  }

  /**
   * Get the configuration for this batch handler.
   * @return an {@link ExecutorServiceConfiguration} instance.
   */
  synchronized ExecutorServiceConfiguration resetConfig() {
    config = new ExecutorServiceConfigurationImpl();
    return config;
  }
}
