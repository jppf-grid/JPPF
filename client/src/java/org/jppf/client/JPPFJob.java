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

package org.jppf.client;

import java.util.*;
import java.util.concurrent.*;

import org.jppf.JPPFException;
import org.jppf.client.balancer.ClientTaskBundle;
import org.jppf.client.event.*;
import org.jppf.client.event.JobEvent.Type;
import org.jppf.client.persistence.*;
import org.jppf.client.taskwrapper.JPPFAnnotatedTask;
import org.jppf.execute.ExecutorChannel;
import org.jppf.node.protocol.Task;
import org.jppf.utils.*;
import org.slf4j.*;

/**
 * Instances of this class represent a JPPF job and hold all the required elements:
 * tasks, execution policy, task listener, data provider, priority, blocking indicator.<br>
 * <p>This class also provides the API for handling JPPF-annotated tasks and POJO tasks.
 * <p>All jobs have a name. It can be specified by calling {@link #setName(java.lang.String) setName(String name)}.
 * If left unspecified, JPPF will automatically assign a uuid as its value.
 * @author Laurent Cohen
 */
public class JPPFJob extends AbstractJPPFJob implements Iterable<Task<?>>, Future<List<Task<?>>> {
  /**
   * Logger for this class.
   */
  private static Logger log = LoggerFactory.getLogger(JPPFJob.class);
  /**
   * Determines whether the debug level is enabled in the log configuration, without the cost of a method call.
   */
  private static boolean debugEnabled = log.isDebugEnabled();
  /**
   * Explicit serialVersionUID.
   */
  private static final long serialVersionUID = 1L;

  /**
   * Default constructor, creates a blocking job with no data provider, default SLA values and a priority of 0.
   * This constructor generates a pseudo-random id as a string of 32 hexadecimal characters.
   */
  public JPPFJob() {
    this(JPPFUuid.normalUUID());
  }

  /**
   * Default constructor, creates a blocking job with no data provider, default SLA values and a priority of 0.
   * This constructor generates a pseudo-random id as a string of 32 hexadecimal characters.
   * @param jobUuid the uuid to assign to this job.
   */
  public JPPFJob(final String jobUuid) {
    super(jobUuid);
  }

  /**
   * Get the list of tasks to execute.
   * @return a list of objects.
   */
  public List<Task<?>> getJobTasks() {
    return tasks;
  }

  /**
   * Add a task to this job. This method is for adding a task that is either an instance of {@link org.jppf.node.protocol.Task Task},
   * annotated with {@link org.jppf.node.protocol.JPPFRunnable JPPFRunnable}, or an instance of {@link java.lang.Runnable Runnable} or {@link java.util.concurrent.Callable Callable}.
   * @param taskObject the task to add to this job.
   * @param args arguments to use with a JPPF-annotated class.
   * @return an instance of <code>Task</code> that is either the same as the input if the input is a subclass of <code>JPPFTask</code>,
   * or a wrapper around the input object in the other cases.
   * @throws JPPFException if one of the tasks is neither a <code>Task</code> or a JPPF-annotated class.
   */
  public Task<?> add(final Object taskObject, final Object...args) throws JPPFException {
    if (taskObject == null) throw new JPPFException("null tasks are not accepted");
    Task<?> jppfTask = null;
    if (taskObject instanceof Task) jppfTask = (Task<?>) taskObject;
    else jppfTask = new JPPFAnnotatedTask(taskObject, args);
    tasks.add(jppfTask);
    jppfTask.setPosition(tasks.size()-1);
    return jppfTask;
  }

  /**
   * Add a POJO task to this job. The POJO task is identified as a method name associated with either an object for a non-static method,
   * or a class for a static method or for a constructor.
   * @param taskObject the task to add to this job.
   * @param method the name of the method to execute.
   * @param args arguments to use with a JPPF-annotated class.
   * @return an instance of <code>Task</code> that is a wrapper around the input task object.
   * @throws JPPFException if one of the tasks is neither a <code>Task</code> or a JPPF-annotated class.
   */
  public Task<?> add(final String method, final Object taskObject, final Object...args) throws JPPFException {
    if (taskObject == null) throw new JPPFException("null tasks are not accepted");
    Task <?>jppfTask = new JPPFAnnotatedTask(taskObject, method, args);
    tasks.add(jppfTask);
    jppfTask.setPosition(tasks.size()-1);
    return jppfTask;
  }

  /**
   * Add a {@link Task} to this job.
   * @param task the task to add to this job.
   * @return an instance of <code>Task</code> that is either the same as the input if the input is a subclass of <code>JPPFTask</code>,
   * or a wrapper around the input object in the other cases.
   * @throws JPPFException if one of the tasks is neither a <code>Task</code> or a JPPF-annotated class.
   * @since 5.0
   */
  public Task<?> add(final Task<?> task) throws JPPFException {
    return add(task, (Object[]) null);
  }

  /**
   * Add a {@link Runnable} task to this job.
   * @param runnable the runnable task to add to this job.
   * @return an instance of <code>Task</code> that is either the same as the input if the input is a subclass of <code>JPPFTask</code>,
   * or a wrapper around the input object in the other cases.
   * @throws JPPFException if one of the tasks is neither a <code>Task</code> or a JPPF-annotated class.
   * @since 5.0
   */
  public Task<?> add(final Runnable runnable) throws JPPFException {
    return add(runnable, (Object[]) null);
  }

  /**
   * Add a {@link Callable} task to this job.
   * @param callable the callable task to add to this job.
   * @return an instance of <code>Task</code> that is either the same as the input if the input is a subclass of <code>JPPFTask</code>,
   * or a wrapper around the input object in the other cases.
   * @throws JPPFException if one of the tasks is neither a <code>Task</code> or a JPPF-annotated class.
   * @since 5.0
   */
  public Task<?> add(final Callable<?> callable) throws JPPFException {
    return add(callable, (Object[]) null);
  }

  /**
   * Add a listener to the list of job listeners.
   * @param listener a {@link JobListener} instance.
   */
  public void addJobListener(final JobListener listener) {
    listeners.add(listener);
  }

  /**
   * Remove a listener from the list of job listeners.
   * @param listener a {@link JobListener} instance.
   */
  public void removeJobListener(final JobListener listener) {
    listeners.remove(listener);
  }

  /**
   * Notify all listeners of the specified event type.
   * @param type the type of the event.
   * @param channel the channel to which a job is dispatched or from which it is returned.
   * @param tasks the tasks that were dispatched or returned.
   * @exclude
   */
  public void fireJobEvent(final JobEvent.Type type, final ExecutorChannel<ClientTaskBundle> channel, final List<Task<?>> tasks) {
    if (log.isDebugEnabled()) log.debug(String.format("firing %s event with %d tasks for %s", type, (tasks == null ? 0 : tasks.size()), this));
    JobEvent event = new JobEvent(this, channel, tasks);
    switch(type) {
      case JOB_START: for (JobListener listener: listeners) listener.jobStarted(event);
      break;
      case JOB_END: for (JobListener listener: listeners) listener.jobEnded(event);
      break;
      case JOB_DISPATCH: for (JobListener listener: listeners) listener.jobDispatched(event);
      break;
      case JOB_RETURN: for (JobListener listener: listeners) listener.jobReturned(event);
      break;
    }
  }

  /**
   * Get the persistence manager that enables saving and restoring the state of this job.
   * @return a {@link JobPersistence} instance.
   * @param <T> the type of the keys used by the persistence manager.
   */
  @SuppressWarnings("unchecked")
  public <T> JobPersistence<T> getPersistenceManager() {
    return (JobPersistence<T>) persistenceManager;
  }

  /**
   * Set the persistence manager that enables saving and restoring the state of this job.
   * @param persistenceManager a {@link JobPersistence} instance.
   * @param <T> the type of the keys used by the persistence manager.
   * @return this job, for method chaining.
   */
  public <T> JPPFJob setPersistenceManager(final JobPersistence<T> persistenceManager) {
    this.persistenceManager = persistenceManager;
    return this;
  }

  @Override
  public Iterator<Task<?>> iterator() {
    return tasks.iterator();
  }

  /**
   * Wait until all execution results of the tasks in this job have been collected.
   * This method is equivalent to {@code get()}, except that it doesn't raise an exception.
   * @return the list of resulting tasks.
   * @since 4.2
   */
  public List<Task<?>> awaitResults() {
    return awaitResults(Long.MAX_VALUE);
  }

  /**
   * Wait until all execution results of the tasks in this job have been collected, or the timeout expires, whichever happens first.
   * This method is equivalent to {@code get(timeout, TimeUnit.MILLISECONDS)}, except that it doesn't raise an exception.
   * @param timeout the maximum time to wait in milliseconds, zero or less meaning an infinite wait.
   * @return the list of resulting tasks, or {@code null} if the timeout expired before all results were received.
   * @since 4.2
   */
  public List<Task<?>> awaitResults(final long timeout) {
    try {
      await(timeout, false);
    } catch (@SuppressWarnings("unused") TimeoutException ignore) {
    }
    return results.getResultsList();
  }

  /**
   * Get the list of currently available task execution results.
   * This method is a shortcut for {@code getResults().getResultsList()}.
   * @return a list of {@link Task} instances, possibly empty.
   * @since 4.2
   */
  public List<Task<?>> getAllResults() {
    return results.getResultsList();
  }

  /**
   * Cancel this job unconditionally.
   * This method is equivalent to calling {@code cancel(true)}.
   * @return {@code false} if the job could not be cancelled, typically because it has already completed normally; {@code true} otherwise.
   * @since 4.2
   */
  public boolean cancel() {
    return cancel(true);
  }

  /**
   * {@inheritDoc}
   * @since 4.2
   */
  @Override
  public boolean cancel(final boolean mayInterruptIfRunning) {
    if (log.isDebugEnabled()) log.debug("request to cancel {}, client={}", this, client);
    if (mayInterruptIfRunning || (getStatus() != JobStatus.EXECUTING)) {
      try {
        if (client != null) return client.cancelJob(uuid);
      } catch(Exception e) {
        log.error("error cancelling job {} : {}", this, ExceptionUtils.getStackTrace(e));
      }
    }
    return false;
  }

  /**
   * {@inheritDoc}
   * @since 4.2
   */
  @Override
  public boolean isCancelled() {
    return cancelled.get();
  }

  /**
   * {@inheritDoc}
   * @since 4.2
   */
  @Override
  public boolean isDone() {
    return cancelled.get() || (unexecutedTaskCount() <= 0);
  }

  /**
   * {@inheritDoc}
   * @since 4.2
   */
  @Override
  public List<Task<?>> get() throws InterruptedException, ExecutionException {
    return awaitResults(Long.MAX_VALUE);
  }

  /**
   * {@inheritDoc}
   * @since 4.2
   */
  @Override
  public List<Task<?>> get(final long timeout, final TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
    await(DateTimeUtils.toMillis(timeout, unit), true);
    return results.getResultsList();
  }

  /**
   * Wait until the job is complete or the timeout expires, whichever happens first.
   * @param timeout the maximum time to wait for the job completion.
   * @param raiseTimeoutException whether to raise a {@link TimeoutException} when the timeout expires.
   * @throws TimeoutException if the tiemout expired and {@code raiseTimeoutException == true}.
   */
  void await(final long timeout, final boolean raiseTimeoutException) throws TimeoutException {
    boolean fullfilled = ConcurrentUtils.awaitCondition(results, new ConcurrentUtils.Condition() {
      @Override public boolean evaluate() {
        JobStatus status = getStatus();
        return (results.size() >= tasks.size()) && ((status == JobStatus.FAILED) || (status == JobStatus.COMPLETE));
      }
    }, timeout);
    if (!fullfilled && raiseTimeoutException) throw new TimeoutException("timeout expired");
  }

  /**
   * Called to notify that the results of a number of tasks have been received from the server.
   * @param tasks the list of tasks whose results have been received from the server.
   * @param throwable the throwable that was raised while receiving the results.
   * @param sendJobEvent whether to emit a {@link org.jppf.client.event.JobEvent JobEvent} notification.
   * @excluded
   */
  public void resultsReceived(final List<Task<?>> tasks, final Throwable throwable, final boolean sendJobEvent) {
    synchronized(getResultsReceivedLock()) {
      int unexecutedTaskCount = 0;
      if (tasks != null) {
        results.addResults(tasks);
        unexecutedTaskCount = this.unexecutedTaskCount();
        if (debugEnabled) log.debug(String.format("Received results for %d tasks, pendingCount=%d, count=%d, jobResults=%s", tasks.size(), unexecutedTaskCount, tasks.size(), results));
        if (persistenceManager != null) {
          try {
            @SuppressWarnings("unchecked")
            JobPersistence<Object> pm = (JobPersistence<Object>) persistenceManager;
            pm.storeJob(pm.computeKey(this), this, tasks);
          } catch (JobPersistenceException e) {
            log.error(e.getMessage(), e);
          }
        }
      } else {
        if (debugEnabled) log.debug("received throwable '{}'", ExceptionUtils.getMessage(throwable));
      }
      if (sendJobEvent) {
        fireJobEvent(JobEvent.Type.JOB_RETURN, null, tasks);
        if (unexecutedTaskCount <= 0) fireJobEvent(Type.JOB_END, null, tasks);
      }
      client.unregisterClassLoaders(uuid);
      results.wakeUp();
      notifyAll();
    }
  }
}
