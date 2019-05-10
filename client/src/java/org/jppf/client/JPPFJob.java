/*
 * JPPF.
 * Copyright (C) 2005-2019 JPPF Team.
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
import org.jppf.node.protocol.graph.TaskNode;
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
public class JPPFJob extends AbstractJPPFJob<JPPFJob> implements Iterable<Task<?>>, Future<List<Task<?>>> {
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
    results.setJobName(name);
  }

  /**
   * Get the list of tasks to execute.
   * @return a list of objects.
   */
  public List<Task<?>> getJobTasks() {
    return tasks;
  }

  /**
   * Add the specified tasks to this job in a bulk operation.
   * @param tasks the list of tasks to add.
   * @throws JPPFException if any error occurs.
   * @since 6.0
   */
  public void addAll(final List<Task<?>> tasks)  throws JPPFException {
    int pos = this.tasks.size();
    for (final Task<?> task: tasks) task.setPosition(pos++);
    this.tasks.addAll(tasks);
  }

  /**
   * Add a task to this job. This method is for adding a task that is either an instance of {@link Task},
   * annotated with {@link org.jppf.node.protocol.JPPFRunnable JPPFRunnable}, or an instance of {@link java.lang.Runnable Runnable} or {@link java.util.concurrent.Callable Callable}.
   * @param taskObject the task to add to this job.
   * @param args arguments to use with a JPPF-annotated class.
   * @return an instance of {@code Task} that is either the same as the input if the input is a subclass of {@code AbstractTask},
   * or a wrapper around the input object in the other cases.
   * @throws JPPFException if one of the tasks is neither a {@code Task} or a JPPF-annotated class.
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
   * @param method the name of the method to execute. For a constructor, this should be identical to the simple name of the class as per {@code Class.getSimpleName()}.
   * @param taskObject the task to add to this job.
   * @param args arguments to use with a JPPF-annotated class.
   * @return an instance of {@code Task} that is a wrapper around the input task object.
   * @throws JPPFException if one of the tasks is neither a {@code Task} or a JPPF-annotated class.
   */
  public Task<?> add(final String method, final Object taskObject, final Object...args) throws JPPFException {
    if (taskObject == null) throw new JPPFException("null tasks are not accepted");
    final Task <?>jppfTask = new JPPFAnnotatedTask(taskObject, method, args);
    tasks.add(jppfTask);
    jppfTask.setPosition(tasks.size()-1);
    return jppfTask;
  }

  /**
   * Add a {@link Task} to this job. When the task is an instance of {@link TaskNode}, the entire dependency graph rooted at this task is added as well.
   * @param task the task to add to this job.
   * @return an instance of {@code Task} that is either the same as the input if the input is a subclass of {@code AbstractTask},
   * or a wrapper around the input object in the other cases.
   * @throws JPPFException if one of the tasks is neither a {@code Task} or a JPPF-annotated class.
   * @since 5.0
   */
  public Task<?> add(final Task<?> task) throws JPPFException {
    if (tasks.contains(task)) return task;
    return (task instanceof TaskNode) ? addWithDependencies((TaskNode<?>) task) : add(task, (Object[]) null);
  }

  /**
   * Add a {@link TaskNode} to this job. The entire dependency graph rooted at this task is added as well.
   * <br>If the task is already present in this job, nothing is added.
   * @param task the task to add to this job.
   * @return an instance of {@code Task} that is either the same as the input if the input is a subclass of {@code AbstractTask},
   * or a wrapper around the input object in the other cases.
   * @throws JPPFException if one of the tasks is neither a {@code Task} or a JPPF-annotated class.
   * @since 6.2
   */
  private Task<?> addWithDependencies(final TaskNode<?> task) throws JPPFException {
    if (tasks.contains(task)) return task;
    taskGraph = true;
    final Task<?> result = add(task, (Object[]) null);
    if (task.hasDependency()) {
      for (final TaskNode<?> dep: task.getDependencies()) addWithDependencies(dep);
    }
    return result;
  }

  /**
   * Add a {@link Runnable} task to this job.
   * <p><b>Note:</b> it is recommended to use {@link #add(JPPFRunnableTask)} whenever possible instead. This ensures that the provided {@link Runnable} is {@link java.io.Serializable serializable}.
   * @param runnable the runnable task to add to this job.
   * @return an instance of {@link Task} that is either the same as the input if the input is a subclass of {@link org.jppf.node.protocol.AbstractTask AbstractTask}, or a wrapper around the input object in the other cases.
   * @throws JPPFException if one of the tasks is neither a {@code Task} or a JPPF-annotated class.
   * @since 5.0
   */
  public Task<?> add(final Runnable runnable) throws JPPFException {
    return add(runnable, (Object[]) null);
  }

  /**
   * Add a {@link JPPFRunnableTask} task to this job.
   * @param runnable the runnable task to add to this job.
   * @return an instance of {@link Task} that is either the same as the input if the input is a subclass of {@link org.jppf.node.protocol.AbstractTask AbstractTask}, or a wrapper around the input object in the other cases.
   * @throws JPPFException if one of the tasks is neither a {@code Task} or a JPPF-annotated class.
   * @since 6.1
   */
  public Task<?> add(final JPPFRunnableTask runnable) throws JPPFException {
    return add(runnable, (Object[]) null);
  }

  /**
   * Add a {@link Callable} task to this job.
   * <p><b>Note:</b> it is recommended to use {@link #add(JPPFCallable)} whenever possible instead. This ensures that the provided {@link Callable} is {@link java.io.Serializable serializable}.
   * @param callable the callable task to add to this job.
   * @return an instance of {@link Task} that is either the same as the input if the input is a subclass of {@link org.jppf.node.protocol.AbstractTask AbstractTask}, or a wrapper around the input object in the other cases.
   * @throws JPPFException if one of the tasks is neither a {@code Task} or a JPPF-annotated class.
   * @since 5.0
   */
  public Task<?> add(final Callable<?> callable) throws JPPFException {
    return add(callable, (Object[]) null);
  }

  /**
   * Add a {@link Callable} task to this job.
   * @param callable the callable task to add to this job.
   * @return an instance of {@link Task} that is either the same as the input if the input is a subclass of {@link org.jppf.node.protocol.AbstractTask AbstractTask}, or a wrapper around the input object in the other cases.
   * @throws JPPFException if one of the tasks is neither a {@code Task} or a JPPF-annotated class.
   * @since 6.1
   */
  public Task<?> add(final JPPFCallable<?> callable) throws JPPFException {
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
    if (log.isDebugEnabled()) log.debug("firing {} event with {} tasks for {}, connection = {}", type, (tasks == null ? 0 : tasks.size()), this, channel);
    final JobEvent event = new JobEvent(this, channel, tasks);
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
    } catch (@SuppressWarnings("unused") final TimeoutException|InterruptedException ignore) {
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
    if (getCancellingFlag().compareAndSet(false, true)) {
      try {
        if (mayInterruptIfRunning || (getStatus() != JobStatus.EXECUTING)) {
          try {
            if (client != null) return client.cancelJob(uuid);
          } catch(final Exception e) {
            log.error("error cancelling job {} : {}", this, ExceptionUtils.getStackTrace(e));
          }
        }
      } finally {
        getCancellingFlag().set(false);
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
    try {
      await(Long.MAX_VALUE, false);
    } catch (@SuppressWarnings("unused") final TimeoutException ignore) {
    }
    return results.getResultsList();
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
   * @throws TimeoutException if the timeout expired and {@code raiseTimeoutException == true}.
   * @throws InterruptedException if the current thread is interrupted while wating for the results.
   */
  void await(final long timeout, final boolean raiseTimeoutException) throws TimeoutException, InterruptedException {
    final long start = System.nanoTime();
    long elapsed;
    final int nbTasks = tasks.size();
    try {
      synchronized(results) {
        while (((elapsed = (System.nanoTime() - start) / 1_000_000L) < timeout) && ((results.size() < nbTasks) || !getStatus().isDone())) {
          results.wait(timeout - elapsed);
        }
        if (!getStatus().isDone() && raiseTimeoutException) throw new TimeoutException("timeout expired");
      }
    } catch (final TimeoutException|InterruptedException e) {
      throw e;
    } catch (final Exception e) {
      log.error(e.getMessage(), e);
    }
  }

  /**
   * Called to notify that the results of a number of tasks have been received from the server.
   * @param tasks the list of tasks whose results have been received from the server.
   * @param throwable the throwable that was raised while receiving the results.
   * @param sendJobEvent whether to emit a {@link org.jppf.client.event.JobEvent JobEvent} notification.
   * @exclude
   */
  public void resultsReceived(final List<Task<?>> tasks, final Throwable throwable, final boolean sendJobEvent) {
    synchronized(results) {
      int unexecutedTaskCount = 0;
      if (tasks != null) {
        results.addResults(tasks);
        unexecutedTaskCount = this.unexecutedTaskCount();
        if (debugEnabled) log.debug("Received results for {} tasks, pendingCount={}, count={}, jobResults={}", tasks.size(), unexecutedTaskCount, tasks.size(), results);
        if (persistenceManager != null) {
          try {
            @SuppressWarnings("unchecked")
            final JobPersistence<Object> pm = (JobPersistence<Object>) persistenceManager;
            pm.storeJob(pm.computeKey(this), this, tasks);
          } catch (final JobPersistenceException e) {
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
    }
    results.wakeUp();
  }

  /**
   * Determine whether the tasks in this job form a dependencies graph.
   * @return {@code true} if the tasks form a graph, {@code false} if all the tasks are independant from each other.
   * @since 6.2
   */
  public boolean hasTaskGraph() {
    return taskGraph;
  }
}
