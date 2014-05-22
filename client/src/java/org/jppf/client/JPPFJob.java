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
import java.util.concurrent.CopyOnWriteArrayList;

import org.jppf.JPPFException;
import org.jppf.client.event.*;
import org.jppf.client.persistence.JobPersistence;
import org.jppf.client.taskwrapper.JPPFAnnotatedTask;
import org.jppf.execute.ExecutorChannel;
import org.jppf.node.protocol.*;
import org.jppf.server.protocol.*;
import org.jppf.task.storage.DataProvider;
import org.jppf.utils.JPPFUuid;

/**
 * Instances of this class represent a JPPF submission and hold all the required elements:
 * tasks, execution policy, task listener, data provider, priority, blocking indicator.<br>
 * <p>This class also provides the API for handling JPPF-annotated tasks and POJO tasks.
 * <p>All jobs have a name. It can be specified by calling {@link #setName(java.lang.String) setName(String name)}.
 * If left unspecified, JPPF will automatically assign a uuid as its value.
 * @author Laurent Cohen
 */
public class JPPFJob implements Serializable, JPPFDistributedJob, Iterable<Task<?>> {
  /**
   * Explicit serialVersionUID.
   */
  private static final long serialVersionUID = 1L;
  /**
   * The list of tasks to execute.
   */
  private final List<Task<?>> tasks = new ArrayList<>();
  /**
   * The container for data shared between tasks.
   * The data provider should be considered read-only, i.e. no modification will be returned back to the client application.
   */
  private DataProvider dataProvider = null;
  /**
   * The listener that receives notifications of completed tasks.
   */
  private transient TaskResultListener resultsListener = null;
  /**
   * Determines whether the execution of this job is blocking on the client side.
   */
  private boolean blocking = true;
  /**
   * The user-defined display name for this job.
   */
  private String name = null;
  /**
   * The universal unique id for this job.
   */
  private final String uuid;
  /**
   * The service level agreement between the job and the server.
   */
  private JobSLA jobSLA = new JPPFJobSLA();
  /**
   * The service level agreement on the client side.
   */
  private JobClientSLA jobClientSLA = new JPPFJobClientSLA();
  /**
   * The user-defined metadata associated with this job.
   */
  private JobMetadata jobMetadata = new JPPFJobMetadata();
  /**
   * The object that holds the results of executed tasks.
   */
  private final JobResults results = new JobResults();
  /**
   * The list of listeners registered with this job.
   */
  private transient List<JobListener> listeners = new CopyOnWriteArrayList<>();
  /**
   * The persistence manager that enables saving and restoring the state of this job.
   */
  private transient JobPersistence<?> persistenceManager = null;

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
    this.uuid = (jobUuid == null) ? JPPFUuid.normalUUID() : jobUuid;
    name = (jobUuid == null) ? this.uuid : jobUuid;
  }

  /**
   * Initialize a blocking job with the specified parameters.
   * @param dataProvider the container for data shared between tasks.
   */
  public JPPFJob(final DataProvider dataProvider) {
    this(dataProvider, null, true, null);
  }

  /**
   * Initialize a blocking job with the specified parameters.
   * @param dataProvider the container for data shared between tasks.
   * @param jobSLA service level agreement between job and server.
   */
  public JPPFJob(final DataProvider dataProvider, final JobSLA jobSLA) {
    this(dataProvider, jobSLA, true, null);
  }

  /**
   * Initialize a non-blocking job with the specified parameters.
   * @param resultsListener the listener that receives notifications of completed tasks.
   */
  public JPPFJob(final TaskResultListener resultsListener) {
    this(null, null, false, resultsListener);
  }

  /**
   * Initialize a non-blocking job with the specified parameters.
   * @param dataProvider the container for data shared between tasks.
   * @param resultsListener the listener that receives notifications of completed tasks.
   */
  public JPPFJob(final DataProvider dataProvider, final TaskResultListener resultsListener) {
    this(dataProvider, null, false, resultsListener);
  }

  /**
   * Initialize a non-blocking job with the specified parameters.
   * @param dataProvider the container for data shared between tasks.
   * @param jobSLA service level agreement between job and server.
   * @param resultsListener the listener that receives notifications of completed tasks.
   */
  public JPPFJob(final DataProvider dataProvider, final JobSLA jobSLA, final TaskResultListener resultsListener) {
    this(dataProvider, jobSLA, false, resultsListener);
  }

  /**
   * Initialize a job with the specified parameters.
   * @param dataProvider the container for data shared between tasks.
   * @param jobSLA service level agreement between job and server.
   * @param blocking determines whether this job is blocking.
   * @param resultsListener the listener that receives notifications of completed tasks.
   */
  public JPPFJob(final DataProvider dataProvider, final JobSLA jobSLA, final boolean blocking, final TaskResultListener resultsListener) {
    this(dataProvider, jobSLA, null, blocking, resultsListener);
  }

  /**
   * Initialize a job with the specified parameters.
   * @param dataProvider the container for data shared between tasks.
   * @param jobSLA service level agreement between job and server.
   * @param jobMetadata the user-defined job metadata.
   * @param blocking determines whether this job is blocking.
   * @param resultsListener the listener that receives notifications of completed tasks.
   */
  public JPPFJob(final DataProvider dataProvider, final JobSLA jobSLA, final JPPFJobMetadata jobMetadata, final boolean blocking, final TaskResultListener resultsListener) {
    this();
    this.dataProvider = dataProvider;
    if (jobSLA != null) this.jobSLA = jobSLA;
    if (jobMetadata != null) this.jobMetadata = jobMetadata;
    this.resultsListener = resultsListener;
    this.blocking = blocking;
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
   * @deprecated use {@link #getJobTasks()} instead.
   */
  @Deprecated
  public List<JPPFTask> getTasks() {
    List<JPPFTask> list = new ArrayList<>(tasks.size());
    for (Task<?> task: tasks) list.add((JPPFTask) task);
    return list;
  }

  /**
   * Get the list of tasks to execute.
   * @return a list of objects.
   */
  public List<Task<?>> getJobTasks() {
    return tasks;
  }

  /**
   * Add a task to this job. This method is for adding a task that is either an instance of {@link org.jppf.server.protocol.JPPFTask JPPFTask},
   * annotated with {@link org.jppf.server.protocol.JPPFRunnable JPPFRunnable}, or an instance of {@link java.lang.Runnable Runnable} or {@link java.util.concurrent.Callable Callable}.
   * @param taskObject the task to add to this job.
   * @param args arguments to use with a JPPF-annotated class.
   * @return an instance of <code>JPPFTask</code> that is either the same as the input if the input is a subclass of <code>JPPFTask</code>,
   * or a wrapper around the input object in the other cases.
   * @throws JPPFException if one of the tasks is neither a <code>JPPFTask</code> or a JPPF-annotated class.
   * @deprecated use {@link #add(Object, Object...)} instead.
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
   * @deprecated use {@link #add(String, Object, Object...)} instead.
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
   * Add a task to this job. This method is for adding a task that is either an instance of {@link org.jppf.server.protocol.JPPFTask JPPFTask},
   * annotated with {@link org.jppf.server.protocol.JPPFRunnable JPPFRunnable}, or an instance of {@link java.lang.Runnable Runnable} or {@link java.util.concurrent.Callable Callable}.
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
   * Get the listener that receives notifications of completed tasks.
   * @return a <code>TaskCompletionListener</code> instance.
   */
  public TaskResultListener getResultListener() {
    return resultsListener;
  }

  /**
   * Set the listener that receives notifications of completed tasks.
   * @param resultsListener a <code>TaskCompletionListener</code> instance.
   */
  public void setResultListener(final TaskResultListener resultsListener) {
    this.resultsListener = resultsListener;
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
   * Get the service level agreement between the job and the server.
   * @param jobSLA an instance of <code>JobSLA</code>.
   */
  public void setSLA(final JobSLA jobSLA) {
    this.jobSLA = jobSLA;
  }

  /**
   * Get the job SLA for the client side.
   * @return an instance of <code>JobSLA</code>.
   */
  public JobClientSLA getClientSLA() {
    return jobClientSLA;
  }

  /**
   * Get the service level agreement between the job and the server.
   * @param jobClientSLA an instance of <code>JobSLA</code>.
   */
  public void setClientSLA(final JobClientSLA jobClientSLA) {
    this.jobClientSLA = jobClientSLA;
  }

  @Override
  public JobMetadata getMetadata() {
    return jobMetadata;
  }

  /**
   * Set this job's metadata.
   * @param jobMetadata a {@link JPPFJobMetadata} instance.
   */
  public void setMetadata(final JobMetadata jobMetadata) {
    this.jobMetadata = jobMetadata;
  }

  @Override
  public int hashCode() {
    return 31 + (uuid == null ? 0 : uuid.hashCode());
  }

  @Override
  public boolean equals(final Object obj) {
    if (this == obj) return true;
    if (!(obj instanceof JPPFJob)) return false;
    JPPFJob other = (JPPFJob) obj;
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

  @Override
  public Iterator<Task<?>> iterator() {
    return tasks.iterator();
  }

  /**
   * Get the count of the tasks in this job that haven't yet been executed.
   * @return the number of unexecuted tasks in this job.
   * @since 4.2
   */
  public int unexecutedTaskCount() {
    return tasks.size() - results.size();
  }
}
