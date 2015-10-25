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

import java.io.Serializable;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

import org.jppf.JPPFException;
import org.jppf.client.event.*;
import org.jppf.client.persistence.JobPersistence;
import org.jppf.client.taskwrapper.JPPFAnnotatedTask;
import org.jppf.node.protocol.*;
import org.jppf.server.protocol.*;
import org.jppf.task.storage.DataProvider;
import org.jppf.utils.JPPFUuid;
import org.slf4j.*;

/**
 * Instances of this class represent a JPPF submission and hold all the required elements:
 * tasks, execution policy, task listener, data provider, priority, blocking indicator.<br>
 * @author Laurent Cohen
 */
public abstract class AbstractJPPFJob implements Serializable, JPPFDistributedJob {
  /**
   * Logger for this class.
   */
  private static Logger log = LoggerFactory.getLogger(AbstractJPPFJob.class);
  /**
   * Explicit serialVersionUID.
   */
  private static final long serialVersionUID = 1L;
  /**
   * The list of tasks to execute.
   */
  final List<Task<?>> tasks = new ArrayList<>();
  /**
   * The container for data shared between tasks.
   * The data provider should be considered read-only, i.e. no modification will be returned back to the client application.
   */
  DataProvider dataProvider = null;
  /**
   * The listener that receives notifications of completed tasks.
   */
  @SuppressWarnings("deprecation")
  transient TaskResultListener resultsListener;
  /**
   * Determines whether the execution of this job is blocking on the client side.
   */
  boolean blocking = true;
  /**
   * The user-defined display name for this job.
   */
  String name = null;
  /**
   * The universal unique id for this job.
   */
  final String uuid;
  /**
   * The service level agreement between the job and the server.
   */
  JobSLA jobSLA = new JPPFJobSLA();
  /**
   * The service level agreement on the client side.
   */
  JobClientSLA jobClientSLA = new JPPFJobClientSLA();
  /**
   * The user-defined metadata associated with this job.
   */
  JobMetadata jobMetadata = new JPPFJobMetadata();
  /**
   * The object that holds the results of executed tasks.
   */
  final JobResults results = new JobResults();
  /**
   * The list of listeners registered with this job.
   */
  transient List<JobListener> listeners = new CopyOnWriteArrayList<>();
  /**
   * The persistence manager that enables saving and restoring the state of this job.
   */
  transient JobPersistence<?> persistenceManager = null;
  /**
   * The client that submitted this job.
   */
  transient JPPFClient client;
  /**
   * Whether this job has been cancelled.
   */
  transient final AtomicBoolean cancelled = new AtomicBoolean(false);

  /**
   * Default constructor, creates a blocking job with no data provider, default SLA values and a priority of 0.
   * This constructor generates a pseudo-random id as a string of 32 hexadecimal characters.
   */
  public AbstractJPPFJob() {
    this(JPPFUuid.normalUUID());
  }

  /**
   * Default constructor, creates a blocking job with no data provider, default SLA values and a priority of 0.
   * This constructor generates a pseudo-random id as a string of 32 hexadecimal characters.
   * @param jobUuid the uuid to assign to this job.
   */
  public AbstractJPPFJob(final String jobUuid) {
    this.uuid = (jobUuid == null) ? JPPFUuid.normalUUID() : jobUuid;
    name = (jobUuid == null) ? this.uuid : jobUuid;
    results.job = this;
  }

  /**
   * Get the list of tasks to execute.
   * @return a list of objects.
   * @deprecated use {@link JPPFJob#getJobTasks()} instead.
   */
  @Deprecated
  public List<JPPFTask> getTasks() {
    List<JPPFTask> list = new ArrayList<>(tasks.size());
    for (Task<?> task: tasks) list.add((JPPFTask) task);
    return list;
  }

  /**
   * Add a task to this job. This method is for adding a task that is either an instance of {@link org.jppf.server.protocol.JPPFTask JPPFTask},
   * annotated with {@link org.jppf.server.protocol.JPPFRunnable JPPFRunnable}, or an instance of {@link java.lang.Runnable Runnable} or {@link java.util.concurrent.Callable Callable}.
   * @param taskObject the task to add to this job.
   * @param args arguments to use with a JPPF-annotated class.
   * @return an instance of <code>JPPFTask</code> that is either the same as the input if the input is a subclass of <code>JPPFTask</code>,
   * or a wrapper around the input object in the other cases.
   * @throws JPPFException if one of the tasks is neither a <code>JPPFTask</code> or a JPPF-annotated class.
   * @deprecated use {@link JPPFJob#add(Object, Object...)} instead.
   */
  @Deprecated
  public JPPFTask addTask(final Object taskObject, final Object...args) throws JPPFException {
    JPPFTask jppfTask = null;
    if (taskObject == null) throw new JPPFException("null tasks are not accepted");
    if (taskObject instanceof JPPFTask) jppfTask = (JPPFTask) taskObject;
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
   * @deprecated use {@link JPPFJob#add(String, Object, Object...)} instead.
   */
  @Deprecated
  public JPPFTask addTask(final String method, final Object taskObject, final Object...args) throws JPPFException {
    if (taskObject == null) throw new JPPFException("null tasks are not accepted");
    JPPFTask jppfTask = new JPPFAnnotatedTask(taskObject, method, args);
    tasks.add(jppfTask);
    jppfTask.setPosition(tasks.size()-1);
    return jppfTask;
  }

  /**
   * Get the listener that receives notifications of completed tasks.
   * @return a <code>TaskCompletionListener</code> instance.
   * @deprecated {@code TaskResultListener} and its implementations are no longer exposed as public APIs.
   * {@link JobListener} should be used instead, with the {@link JPPFJob#addJobListener(JobListener)} and {@link JPPFJob#removeJobListener(JobListener)} methods.
   */
  public TaskResultListener getResultListener() {
    return resultsListener;
  }

  /**
   * Set the listener that receives notifications of completed tasks.
   * @param resultsListener a <code>TaskCompletionListener</code> instance.
   * @deprecated {@code TaskResultListener} and its implementations are no longer exposed as public APIs.
   * {@link JobListener} should be used instead, with the {@link JPPFJob#addJobListener(JobListener)} and {@link JPPFJob#removeJobListener(JobListener)} methods.
   */
  public void setResultListener(final TaskResultListener resultsListener) {
    this.resultsListener = resultsListener;
  }

  @Override
  public int hashCode() {
    return 31 + (uuid == null ? 0 : uuid.hashCode());
  }

  @Override
  public boolean equals(final Object obj) {
    if (this == obj) return true;
    if (!(obj instanceof AbstractJPPFJob)) return false;
    AbstractJPPFJob other = (AbstractJPPFJob) obj;
    return (uuid == null) ? other.uuid == null : uuid.equals(other.uuid);
  }

  /**
   * Get the object that holds the results of executed tasks.
   * @return a {@link JobResults} instance.
   */
  public JobResults getResults() {
    return results;
  }

  /**
   * Resolve this instance after deserialization.
   * @return an instance of {@link Object}.
   */
  protected Object readResolve() {
    listeners = new LinkedList<>();
    return this;
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append(getClass().getSimpleName()).append('[');
    sb.append("name=").append(name);
    sb.append(", uuid=").append(uuid);
    sb.append(", blocking=").append(blocking);
    sb.append(", nbTasks=").append(tasks.size());
    sb.append(", nbResults=").append(results.size());
    sb.append(", jobSLA=").append(jobSLA);
    sb.append(']');
    return sb.toString();
  }

  /**
   * Wait until the job is complete or the timeout expires, whichever happens first.
   * @param timeout the maximum time to wait for the job completion.
   * @param raiseTimeoutException whether to raise a {@link TimeoutException} when the timeout expires.
   * @throws TimeoutException if the tiemout expired and {@code raiseTimeoutException == true}.
   */
  void await(final long timeout, final boolean raiseTimeoutException) throws TimeoutException {
    long millis = timeout > 0L ? timeout : Long.MAX_VALUE;
    long elapsed = 0L;
    long start = System.nanoTime();
    while ((results.size() < tasks.size()) && ((elapsed = (System.nanoTime() - start) / 1_000_000L) < millis)) results.goToSleep(1L);
    if ((elapsed >= millis) && raiseTimeoutException) throw new TimeoutException("timeout expired");
  }

  /**
   * Get the flag that determines whether this job has been cancelled.
   * @return an {@code AtomicBoolean} instance.
   * @exclude
   */
  public AtomicBoolean getCancelledFlag() {
    return cancelled;
  }
}
