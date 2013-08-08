/*
 * JPPF.
 * Copyright (C) 2005-2013 JPPF Team.
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
import org.jppf.server.protocol.*;
import org.jppf.utils.*;
import org.slf4j.*;

/**
 * This class is a processor for tasks submitted via a {@link JPPFExecutorService}.
 * It handles both normal mode and batching mode, where the tasks throughput is streamlined
 * by specifying how many tasks should be sent to the grid, and a which intervals.
 * @author Laurent Cohen
 * @exclude
 */
public class BatchHandler extends ThreadSynchronization implements Runnable
{
  /**
   * Logger for this class.
   */
  private static Logger log = LoggerFactory.getLogger(BatchHandler.class);
  /**
   * Determines whether debug-level logging is enabled.
   */
  private static boolean debugEnabled = log.isDebugEnabled();
  /**
   * Count of jobs created by this executor service.
   */
  private static AtomicLong jobCount = new AtomicLong(0);
  /**
   * The default configuration all batch handlers.
   */
  private static final ExecutorServiceConfiguration DEFAULT_CONFIG = new ExecutorServiceConfigurationImpl();
  /**
   * The minimum number of tasks that must be submitted before they are sent to the server.
   */
  private int batchSize = 0;
  /**
   * The maximum time to wait before the next batch of tasks is to be sent for execution.
   */
  private long batchTimeout = 0L;
  /**
   * The JPPFExecutorService whose tasks are batched.
   */
  private JPPFExecutorService executor = null;
  /**
   * The job to send for execution. If the reference is ont null, then the job is sent immediately.
   */
  private AtomicReference<JPPFJob> currentJobRef = new AtomicReference<>(null);
  /**
   * The next job being prepared. It will be assigned to <code>currentJobRef</code> when it is ready for execution,
   * depending on the batching parameters.
   */
  private AtomicReference<JPPFJob> nextJobRef = new AtomicReference<>(null);
  /**
   * The time at which we started to count for the the timeout.
   */
  private long start = 0L;
  /**
   * Time elapsed since the start.
   */
  private long elapsed = 0L;
  /**
   * Used to synchronize access to <code>currentJobRef</code> and <code>nextJobRef</code>
   */
  private ReentrantLock lock = new ReentrantLock(true);
  /**
   * Represents a condition to await for and corresponding to when <code>currentJobRef</code> is not null.
   */
  private Condition jobReady = lock.newCondition();
  /**
   * Represents a condition to await for and corresponding to when <code>currentJobRef</code> is not null.
   */
  private Condition submittingJob = lock.newCondition();
  /**
   * The configuration for this batch handler.
   */
  private ExecutorServiceConfiguration config = new ExecutorServiceConfigurationImpl();

  /**
   * Default constructor.
   * @param executor the JPPFExecutorService whose tasks are batched.
   */
  BatchHandler(final JPPFExecutorService executor)
  {
    this.executor = executor;
    nextJobRef.set(createJob());
  }

  /**
   * Get the minimum number of tasks that must be submitted before they are sent to the server.
   * @return the batch size as an int.
   */
  synchronized int getBatchSize()
  {
    return batchSize;
  }

  /**
   * Set the minimum number of tasks that must be submitted before they are sent to the server.
   * @param batchSize the batch size as an int.
   */
  synchronized void setBatchSize(final int batchSize)
  {
    this.batchSize = batchSize;
  }

  /**
   * Get the maximum time to wait before the next batch of tasks is to be sent for execution.
   * @return the timeout as a long.
   */
  synchronized long getBatchTimeout()
  {
    return batchTimeout;
  }

  /**
   * Set the maximum time to wait before the next batch of tasks is to be sent for execution.
   * @param batchTimeout the timeout as a long.
   */
  synchronized void setBatchTimeout(final long batchTimeout)
  {
    this.batchTimeout = batchTimeout;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void run()
  {
    start = System.currentTimeMillis();
    while (!isStopped())
    {
      try
      {
        lock.lock();
        try
        {
          while (!isStopped() && (currentJobRef.get() == null))
          //while (!isStopped() && pendingJobs.isEmpty())
          {
            if (batchTimeout > 0)
            {
              long n = batchTimeout - elapsed;
              if (n > 0) jobReady.await(n, TimeUnit.MILLISECONDS);
            }
            else jobReady.await();
            updateNextJob(false);
          }
          if (isStopped()) break;
          JPPFJob job = currentJobRef.get();
          if (debugEnabled) log.debug("submitting job " + job.getName() + " with " + job.getTasks().size() + " tasks");
          FutureResultCollector collector = (FutureResultCollector) job.getResultListener();
          configureJob(job);
          executor.submitJob(job);
          currentJobRef.set(null);
          elapsed = System.currentTimeMillis() - start;
          submittingJob.signal();
          //updateNextJob(false);
        }
        finally
        {
          lock.unlock();
        }
      }
      catch (Exception e)
      {
        log.error(e.getMessage(), e);
      }
    }
  }

  /**
   * Update the next job to submit if one is ready.
   * @param sendSignal true if signal is to be sent, false otherwise.
   */
  private void updateNextJob(final boolean sendSignal)
  {
    JPPFJob job = nextJobRef.get();
    int size = job.getTasks().size();
    if (batchTimeout > 0L) elapsed = System.currentTimeMillis() - start;
    if (size == 0)
    {
      if ((batchTimeout > 0L) && (elapsed >= batchTimeout)) resetTimeout();
      return;
    }
    if (((batchTimeout > 0L) && (elapsed >= batchTimeout)) ||
        ((batchSize > 0) && (size >= batchSize)) ||
        ((batchSize <= 0) && (batchTimeout <= 0L)))
    {
      currentJobRef.set(job);
      nextJobRef.set(createJob());
      resetTimeout();
      if (sendSignal)
      {
        jobReady.signal();
        try
        {
          submittingJob.await();
        }
        catch (InterruptedException e)
        {
          throw new RejectedExecutionException(e);
        }
      }
    }
  }

  /**
   * Reset the timeout counter.
   */
  private void resetTimeout()
  {
    start = System.currentTimeMillis();
    elapsed = 0L;
  }

  /**
   * Submit a {@link JPPFTask} that returns the specified type of result.
   * @param <T> the type of result returned by the task.
   * @param task the task to submit.
   * @param result this parameter is only here for type inference (I know, it's ugly).
   * @return a {@link Future} representing pending completion of the task.
   */
  <T> Future<T> addTask(final JPPFTask task, final T result)
  {
    lock.lock();
    try
    {
      if (debugEnabled) log.debug("submitting one JPPFTask");
      Future<T> future = null;
      JPPFJob job = nextJobRef.get();
      try
      {
        FutureResultCollector collector = (FutureResultCollector) job.getResultListener();
        job.addTask(task);
        future = new JPPFTaskFuture<>(collector, task.getPosition());
      }
      catch (JPPFException e)
      {
        log.error(e.getMessage(), e);
        throw new RejectedExecutionException(e);
      }
      updateNextJob(true);
      return future;
    }
    finally
    {
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
  <T> Future<T> addTask(final Runnable task, final T result)
  {
    lock.lock();
    try
    {
      if (debugEnabled) log.debug("submitting one Runnable task with result");
      Future<T> future = null;
      JPPFJob job = nextJobRef.get();
      try
      {
        FutureResultCollector collector = (FutureResultCollector) job.getResultListener();
        JPPFAnnotatedTask t = (JPPFAnnotatedTask) job.addTask(task);
        t.setResult(result);
        configureTask(t);
        future = new JPPFTaskFuture<>(collector, t.getPosition());
      }
      catch (JPPFException e)
      {
        log.error(e.getMessage(), e);
        throw new RejectedExecutionException(e);
      }
      updateNextJob(true);
      return future;
    }
    finally
    {
      lock.unlock();
    }
  }

  /**
   * Submit a task for execution.
   * @param <T> the type of results.
   * @param task the task to submit.
   * @return a {@link Future} representing pending completion of the task.
   */
  <T> Future<T> addTask(final Callable<T> task)
  {
    lock.lock();
    try
    {
      if (debugEnabled) log.debug("submitting one Callable Task");
      Future<T> future = null;
      JPPFJob job = nextJobRef.get();
      try
      {
        FutureResultCollector collector = (FutureResultCollector) job.getResultListener();
        JPPFAnnotatedTask jppfTask = (JPPFAnnotatedTask) job.addTask(task);
        configureTask(jppfTask);
        future = new JPPFTaskFuture<>(collector, jppfTask.getPosition());
      }
      catch (JPPFException e)
      {
        log.error(e.getMessage(), e);
        throw new RejectedExecutionException(e);
      }
      updateNextJob(true);
      return future;
    }
    finally
    {
      lock.unlock();
    }
  }

  /**
   * Submit a list of tasks for execution.
   * @param <T> the type of the results.
   * @param tasks the tasks to submit.
   * @return a pair representing the result collector used in the current job, along with the position of the first task.
   */
  @SuppressWarnings("unchecked")
  <T> Pair<FutureResultCollector, Integer> addTasks(final Collection<? extends Callable<T>> tasks)
  {
    lock.lock();
    try
    {
      if (debugEnabled) log.debug("submitting " + tasks.size() + " Callable Tasks");
      Pair<FutureResultCollector, Integer> pair = null;
      JPPFJob job = nextJobRef.get();
      FutureResultCollector collector = (FutureResultCollector) job.getResultListener();
      int start = 0;
      try
      {
        List<JPPFTask> jobTasks = job.getTasks();
        start = jobTasks.size();
        for (Callable<?> task: tasks)
        {
          JPPFTask t = job.addTask(task);
          configureTask((JPPFAnnotatedTask) t);
        }
      }
      catch (JPPFException e)
      {
        log.error(e.getMessage(), e);
        throw new RejectedExecutionException(e);
      }
      pair = new Pair(collector, start);
      updateNextJob(true);
      return pair;
    }
    finally
    {
      lock.unlock();
    }
  }

  /**
   * Create a new job with a FutureResultCollector a results listener.
   * @return a {@link JPPFJob} instance.
   */
  private JPPFJob createJob()
  {
    JPPFJob job = new JPPFJob();
    job.setName(getClass().getSimpleName() + " job " + jobCount.incrementAndGet());
    FutureResultCollector collector = new FutureResultCollector(job);
    job.setResultListener(collector);
    job.setBlocking(false);
    collector.addListener(executor);
    if (debugEnabled) log.debug("created job " + job);
    //configureJob(job);
    return job;
  }

  /**
   * Configure the specified job using the current configuration.
   * @param job the job to configure.
   */
  private synchronized void configureJob(final JPPFJob job)
  {
    if (config != null)
    {
      JobConfiguration jc = config.getJobConfiguration();
      job.setSLA(jc.getSLA());
      job.setClientSLA(jc.getClientSLA());
      job.setMetadata(jc.getMetadata());
      job.setPersistenceManager(jc.getPersistenceManager());
      job.setDataProvider(jc.getDataProvider());
      for (JobListener listener: jc.getAllJobListeners()) job.addJobListener(listener);
    }
  }

  /**
   * Configure the specified job using the current configuration.
   * @param task the task to configure.
   */
  private synchronized void configureTask(final JPPFAnnotatedTask task)
  {
    if (config != null)
    {
      TaskConfiguration tc = config.getTaskConfiguration();
      task.setCancelCallback(tc.getOnCancelCallback());
      task.setTimeoutCallback(tc.getOnTimeoutCallback());
      task.setTimeoutSchedule(tc.getTimeoutSchedule());
    }
  }

  /**
   * Close this batch handler.
   */
  void close()
  {
    setStopped(true);
    lock.lock();
    try
    {
      jobReady.signalAll();
    }
    finally
    {
      lock.unlock();
    }
  }

  /**
   * Get the configuration for this batch handler.
   * @return an {@link ExecutorServiceConfiguration} instance.
   */
  synchronized ExecutorServiceConfiguration getConfig()
  {
    return config;
  }

  /**
   * Set the configuration for this batch handler.
   * @param config an {@link ExecutorServiceConfiguration} instance.
   * @throws IllegalArgumentException if the new configuration is null.
   */
  synchronized void setConfig(final ExecutorServiceConfiguration config) throws IllegalArgumentException
  {
    if (config == null) throw new IllegalArgumentException("configuration cannot be null");
    this.config = config;
  }

  /**
   * Get the configuration for this batch handler.
   * @return an {@link ExecutorServiceConfiguration} instance.
   */
  synchronized ExecutorServiceConfiguration resetConfig()
  {
    config = new ExecutorServiceConfigurationImpl();
    return config;
  }
}
