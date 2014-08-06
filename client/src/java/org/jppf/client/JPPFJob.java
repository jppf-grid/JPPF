/*
 * JPPF.
 * Copyright (C) 2005-2014 JPPF Team.
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
import org.jppf.client.event.*;
import org.jppf.client.persistence.JobPersistence;
import org.jppf.client.submission.*;
import org.jppf.client.taskwrapper.JPPFAnnotatedTask;
import org.jppf.execute.ExecutorChannel;
import org.jppf.node.protocol.*;
import org.jppf.task.storage.DataProvider;
import org.jppf.utils.*;
import org.slf4j.*;

/**
 * Instances of this class represent a JPPF submission and hold all the required elements:
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
   * Explicit serialVersionUID.
   */
  private static final long serialVersionUID = 1L;
  /**
   * The listener that receives notifications of completed tasks.
   */
  transient final JPPFResultCollector resultCollector;

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
    resultCollector = new JPPFResultCollector(this);
  }

  /**
   * Get the listener that receives notifications of completed tasks.
   * @return a <code>TaskCompletionListener</code> instance.
   * @exclude
   */
  public JPPFResultCollector getResultCollector() {
    return resultCollector;
  }

  @Override
  public String getUuid() {
    return uuid;
  }

  @Override
  public String getName() {
    return name;
  }

  /**
   * Set the user-defined display name for this job.
   * @param name the display name as a string.
   */
  public void setName(final String name) {
    this.name = name;
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
   * @return an instance of <code>JPPFTask</code> that is either the same as the input if the input is a subclass of <code>JPPFTask</code>,
   * or a wrapper around the input object in the other cases.
   * @throws JPPFException if one of the tasks is neither a <code>JPPFTask</code> or a JPPF-annotated class.
   */
  public Task<?> add(final Object taskObject, final Object...args) throws JPPFException {
    if (taskObject == null) throw new JPPFException("null tasks are not accepted");
    Task<?> jppfTask = null;
    if (taskObject instanceof Task) jppfTask = (Task) taskObject;
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
   * @return an instance of <code>JPPFTask</code> that is a wrapper around the input task object.
   * @throws JPPFException if one of the tasks is neither a <code>JPPFTask</code> or a JPPF-annotated class.
   */
  public Task<?> add(final String method, final Object taskObject, final Object...args) throws JPPFException {
    if (taskObject == null) throw new JPPFException("null tasks are not accepted");
    Task <?>jppfTask = new JPPFAnnotatedTask(taskObject, method, args);
    tasks.add(jppfTask);
    jppfTask.setPosition(tasks.size()-1);
    return jppfTask;
  }

  /**
   * Get the container for data shared between tasks.
   * @return a <code>DataProvider</code> instance.
   */
  public DataProvider getDataProvider() {
    return dataProvider;
  }

  /**
   * Set the container for data shared between tasks.
   * @param dataProvider a <code>DataProvider</code> instance.
   */
  public void setDataProvider(final DataProvider dataProvider) {
    this.dataProvider = dataProvider;
  }

  /**
   * Determine whether the execution of this job is blocking on the client side.
   * @return true if the execution is blocking, false otherwise.
   */
  public boolean isBlocking() {
    return blocking;
  }

  /**
   * Specify whether the execution of this job is blocking on the client side.
   * @param blocking true if the execution is blocking, false otherwise.
   */
  public void setBlocking(final boolean blocking) {
    this.blocking = blocking;
  }

  @Override
  public JobSLA getSLA() {
    return jobSLA;
  }

  /**
   * Get the job SLA for the client side.
   * @return an instance of <code>JobSLA</code>.
   */
  public JobClientSLA getClientSLA() {
    return jobClientSLA;
  }

  @Override
  public JobMetadata getMetadata() {
    return jobMetadata;
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
  public void fireJobEvent(final JobEvent.Type type, final ExecutorChannel channel, final List<Task<?>> tasks) {
    if (log.isDebugEnabled()) log.debug("firing {} event for {}", type, this);
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
   */
  public <T> void setPersistenceManager(final JobPersistence<T> persistenceManager) {
    this.persistenceManager = persistenceManager;
  }

  @Override
  public Iterator<Task<?>> iterator() {
    return tasks.iterator();
  }

  /**
   * Get the count of the tasks in this job that haven completed.
   * @return the number of executed tasks in this job.
   * @since 4.2
   */
  public int executedTaskCount() {
    return results.size();
  }

  /**
   * Get the count of the tasks in this job that haven't yet been executed.
   * @return the number of unexecuted tasks in this job.
   * @since 4.2
   */
  public int unexecutedTaskCount() {
    return tasks.size() - results.size();
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
    } catch (TimeoutException ignore) {
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
   * Get the execution status of this job.
   * @return a {@link SubmissionStatus} enum value, or {@code null} isd the status could not be determined.
   * @since 4.2
   */
  public SubmissionStatus getStatus() {
    if (resultCollector instanceof SubmissionStatusHandler) return ((SubmissionStatusHandler) resultCollector).getStatus();
    return null;
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
    if (mayInterruptIfRunning || (getStatus() != SubmissionStatus.EXECUTING)) {
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
}
