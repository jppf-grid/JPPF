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

import java.io.*;
import java.util.*;
import java.util.concurrent.*;

import org.jppf.JPPFException;
import org.jppf.client.balancer.ClientTaskBundle;
import org.jppf.client.event.*;
import org.jppf.client.persistence.JobPersistence;
import org.jppf.client.taskwrapper.JPPFAnnotatedTask;
import org.jppf.execute.ExecutorChannel;
import org.jppf.node.protocol.*;
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
   * Explicit serialVersionUID.
   */
  private static final long serialVersionUID = 1L;
  /**
   * The listener that receives notifications of completed tasks.
   */
  transient JPPFResultCollector resultCollector;

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
   * @return this job, for method chaining.
   */
  public JPPFJob setName(final String name) {
    this.name = name;
    return this;
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
   * Get the container for data shared between tasks.
   * @return a <code>DataProvider</code> instance.
   */
  public DataProvider getDataProvider() {
    return dataProvider;
  }

  /**
   * Set the container for data shared between tasks.
   * @param dataProvider a <code>DataProvider</code> instance.
   * @return this job, for method chaining.
   */
  public JPPFJob setDataProvider(final DataProvider dataProvider) {
    this.dataProvider = dataProvider;
    return this;
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
   * @return this job, for method chaining.
   */
  public JPPFJob setBlocking(final boolean blocking) {
    this.blocking = blocking;
    return this;
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
   * Get the execution status of this job.
   * @return a {@link JobStatus} enum value, or {@code null} isd the status could not be determined.
   * @since 4.2
   */
  public JobStatus getStatus() {
    return resultCollector.getStatus();
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
   * Save the state of the {@code JPPFJob} instance to a stream (i.e.,serialize it).
   * @param out the output stream to which to write the job. 
   * @throws IOException if any I/O error occurs.
   * @since 5.0
   */
  private void writeObject(final ObjectOutputStream out) throws IOException {
    out.defaultWriteObject();
  }

  /**
   * Reconstitute the {@code JPPFJob} instance from a stream (i.e., deserialize it).
   * @param in the input stream from which to read the job. 
   * @throws IOException if any I/O error occurs.
   * @throws ClassNotFoundException if the class of an object in the object graph can not be found.
   * @since 5.0
   */
  private void readObject(final ObjectInputStream in) throws IOException, ClassNotFoundException {
    in.defaultReadObject();
    resultCollector = new JPPFResultCollector(this);
  }

  @Override
  public int getTaskCount() {
    return tasks.size();
  }
}
