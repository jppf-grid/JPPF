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
package org.jppf.server.protocol;

import java.util.concurrent.atomic.AtomicLong;

import org.jppf.node.protocol.*;
import org.jppf.utils.*;
import org.jppf.utils.collections.*;

/**
 * Instances of this class group tasks from the same client together, so they are sent to the same node,
 * avoiding unnecessary transport overhead.<br>
 * The goal is to provide a performance enhancement through an adaptive bundling of tasks originating from the same client.
 * The bundle size is computed dynamically, depending on the number of nodes connected to the server, and other factors.
 * @author Laurent Cohen
 * @exclude
 */
public class JPPFTaskBundle extends MetadataImpl implements Comparable<JPPFTaskBundle>, JPPFDistributedJob {
  /**
   * Explicit serialVersionUID.
   */
  private static final long serialVersionUID = 1L;
  /**
   * Flag indicating whether collection of debug information is available via JMX.
   */
  private static final boolean JPPF_DEBUG = JPPFConfiguration.getProperties().getBoolean("jppf.debug.enabled", false);
  /**
   * The unique identifier for the request (the job) this task bundle is a part of.
   */
  private String uuid = null;
  /**
   * The user-defined display name for this job.
   */
  private String name = null;
  /**
   * The unique identifier for the submitting application.
   */
  private TraversalList<String> uuidPath;
  /**
   * The number of tasks in this bundle at the time it is received by a driver.
   */
  protected int driverQueueTaskCount = 0;
  /**
   * The number of tasks in this bundle.
   */
  protected int taskCount = 0;
  /**
   * The current number of tasks in this bundle.
   */
  protected int currentTaskCount = 0;
  /**
   * The initial number of tasks in this bundle.
   */
  protected int initialTaskCount = 0;
  /**
   * The time it took a node to execute this task.
   */
  private long nodeExecutionTime = 0L;
  /**
   * The time at which the bundle is taken out of the queue for sending to a node.
   */
  private long executionStartTime = 0L;
  /**
   * Indicates whether this object is used for handshake instead of execution.
   */
  private boolean handshake = false;
  /**
   * The service level agreement between the job and the server.
   */
  private JobSLA jobSLA;
  /**
   * The user-defined metadata associated with this job.
   */
  private JobMetadata jobMetadata;
  /**
   * Count of bundles copied.
   */
  private static AtomicLong copyCount = new AtomicLong(0L);

  /**
   * Initialize this task bundle and set its build number.
   */
  public JPPFTaskBundle() {
    uuidPath = new TraversalList<>();
    jobSLA = new JPPFJobSLA();
    jobMetadata = new JPPFJobMetadata();
  }

  /**
   * Get the uuid path of the applications (driver or client) in whose classpath the class definition may be found.
   * @return the uuid path as a list of string elements.
   */
  public TraversalList<String> getUuidPath() {
    return uuidPath;
  }

  /**
   * Set the uuid path of the applications (driver or client) in whose classpath the class definition may be found.
   * @param uuidPath the uuid path as a list of string elements.
   */
  public void setUuidPath(final TraversalList<String> uuidPath) {
    this.uuidPath = uuidPath;
  }

  /**
   * Get the time it took a node to execute this task.
   * @return the time in milliseconds as a long value.
   */
  public long getNodeExecutionTime() {
    return nodeExecutionTime;
  }

  /**
   * Set the time it took a node to execute this task.
   * @param nodeExecutionTime the time in milliseconds as a long value.
   */
  public void setNodeExecutionTime(final long nodeExecutionTime) {
    this.nodeExecutionTime = nodeExecutionTime;
  }

  /**
   * Get the number of tasks in this bundle.
   * @return the number of tasks as an int.
   */
  public int getTaskCount() {
    return taskCount;
  }

  /**
   * Set the number of tasks in this bundle.
   * @param taskCount the number of tasks as an int.
   */
  public void setTaskCount(final int taskCount) {
    this.taskCount = taskCount;
    if (initialTaskCount <= 0) initialTaskCount = taskCount;
    if (currentTaskCount <= 0) currentTaskCount = taskCount;
  }

  /**
   * Set the initial number of tasks in this bundle.
   * @param initialTaskCount the number of tasks as an int.
   */
  public void setInitialTaskCount(final int initialTaskCount) {
    this.initialTaskCount = initialTaskCount;
  }

  /**
   * Compare two task bundles, based on their respective priorities.<br>
   * <b>Note:</b> <i>this class has a natural ordering that is inconsistent with equals.</i>
   * @param bundle the bundle compare this one to.
   * @return a positive int if this bundle is greater, 0 if both are equal,
   * or a negative int if this bundle is less than the other.
   * @see java.lang.Comparable#compareTo(java.lang.Object)
   */
  @Override
  public int compareTo(final JPPFTaskBundle bundle) {
    if (bundle == null) return 1;
    int otherPriority = bundle.getSLA().getPriority();
    if (jobSLA.getPriority() < otherPriority) return -1;
    if (jobSLA.getPriority() > otherPriority) return 1;
    return 0;
  }

  /**
   * Make a copy of this bundle.
   * @return a new <code>JPPFTaskBundle</code> instance.
   */
  public synchronized JPPFTaskBundle copy() {
    JPPFTaskBundle bundle = new JPPFTaskBundle();
    bundle.setUuidPath(uuidPath);
    bundle.setUuid(uuid);
    bundle.setName(name);
    bundle.setTaskCount(taskCount);
    bundle.setCurrentTaskCount(currentTaskCount);
    bundle.initialTaskCount = initialTaskCount;
    bundle.getAll().putAll(this.getAll());
    bundle.setSLA(jobSLA);
    bundle.setMetadata(jobMetadata);
    bundle.setHandshake(handshake);
    if (JPPF_DEBUG) bundle.setParameter("bundle.uuid", uuidPath.getLast() + '-' + copyCount.incrementAndGet());
    return bundle;
  }

  /**
   * Make a copy of this bundle containing only the first nbTasks tasks it contains.
   * @param nbTasks the number of tasks to include in the copy.
   * @return a new <code>JPPFTaskBundle</code> instance.
   */
  public JPPFTaskBundle copy(final int nbTasks) {
    JPPFTaskBundle bundle = copy();
    synchronized(this) {
      bundle.setTaskCount(nbTasks);
      taskCount -= nbTasks;
    }
    return bundle;
  }

  /**
   * Get the time at which the bundle is taken out of the queue for sending to a node.
   * @return the time as a long value.
   */
  public long getExecutionStartTime() {
    return executionStartTime;
  }

  /**
   * Set the time at which the bundle is taken out of the queue for sending to a node.
   * @param executionStartTime the time as a long value.
   */
  public void setExecutionStartTime(final long executionStartTime) {
    this.executionStartTime = executionStartTime;
  }

  /**
   * Get the initial task count of this bundle.
   * @return the task count as an int.
   */
  public int getInitialTaskCount() {
    return initialTaskCount;
  }

  @Override
  public JobSLA getSLA() {
    return jobSLA;
  }

  /**
   * Get the service level agreement between the job and the server.
   * @param jobSLA an instance of <code>JPPFJobSLA</code>.
   */
  public void setSLA(final JobSLA jobSLA) {
    this.jobSLA = jobSLA;
  }

  @Override
  public String toString()
  {
    StringBuilder sb = new StringBuilder();
    sb.append(getClass().getSimpleName()).append('[');
    sb.append("name=").append(name);
    sb.append(", uuid=").append(uuid);
    sb.append(", initialTaskCount=").append(initialTaskCount);
    sb.append(", taskCount=").append(taskCount);
    sb.append(", bundleUuid=").append(getParameter("bundle.uuid"));
    sb.append(", uuidPath=").append(uuidPath);
    if (JPPF_DEBUG) sb.append(", nodeBundleId=").append(getParameter("node.bundle.id"));
    sb.append(']');
    return sb.toString();
  }

  @Override
  public String getName() {
    return name;
  }

  /**
   * Set the user-defined display name for the job.
   * @param name the display name as a string.
   */
  public void setName(final String name) {
    this.name = name;
  }

  @Override
  public JobMetadata getMetadata() {
    return jobMetadata;
  }

  /**
   * Set this bundle's metadata.
   * @param jobMetadata a {@link JPPFJobMetadata} instance.
   */
  public void setMetadata(final JobMetadata jobMetadata) {
    this.jobMetadata = jobMetadata;
  }

  @Override
  public String getUuid() {
    return uuid;
  }

  /**
   * Set the uuid of the initial job.
   * @param jobUuid the uuid as a string.
   */
  public void setUuid(final String jobUuid) {
    this.uuid = jobUuid;
  }

  /**
   * Get the current number of tasks in this bundle.
   * @return the current number of tasks as an int.
   */
  public int getCurrentTaskCount() {
    return currentTaskCount;
  }

  /**
   * Set the current number of tasks in this bundle.
   * @param currentTaskCount the current number of tasks as an int.
   */
  public void setCurrentTaskCount(final int currentTaskCount) {
    this.currentTaskCount = currentTaskCount;
  }

  /**
   * Get the job requeue flag.
   * @return job requeue flag.
   */
  public boolean isPending() {
    return getParameter(BundleParameter.JOB_PENDING, false);
  }

  /**
   * Get the job requeue flag.
   * @return job requeue flag.
   */
  public boolean isRequeue() {
    return getParameter(BundleParameter.JOB_REQUEUE, false);
  }

  /**
   * Set the job requeue flag.
   * @param requeue job requeue flag.
   */
  public void setRequeue(final boolean requeue) {
    setParameter(BundleParameter.JOB_REQUEUE, requeue);
  }

  /**
   * Get the number of tasks in this bundle at the time it is received by a driver.
   * @return the number of tasks as an int.
   */
  public int getDriverQueueTaskCount() {
    return driverQueueTaskCount;
  }

  /**
   * Set the number of tasks in this bundle at the time it is received by a driver.
   * @param driverQueueTaskCount the number of tasks as an int.
   */
  public void setDriverQueueTaskCount(final int driverQueueTaskCount) {
    this.driverQueueTaskCount = driverQueueTaskCount;
  }

  /**
   * Determine whether this object is used for handshake instead of execution.
   * @return <code>true</code> if this bundle is a handshake bundle, <code>false</code> otherwise.
   */
  public boolean isHandshake() {
    return handshake;
  }

  /**
   * Specify whether this object is used for handshake instead of execution.
   * @param handshake <code>true</code> if this bundle is a handshake bundle, <code>false</code> otherwise.
   */
  public void setHandshake(final boolean handshake) {
    this.handshake = handshake;
  }
}
