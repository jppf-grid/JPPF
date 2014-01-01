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
public class JPPFTaskBundle extends MetadataImpl implements Comparable<JPPFTaskBundle>, TaskBundle {
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

  @Override
  public TraversalList<String> getUuidPath() {
    return uuidPath;
  }

  @Override
  public void setUuidPath(final TraversalList<String> uuidPath) {
    this.uuidPath = uuidPath;
  }

  @Override
  public long getNodeExecutionTime() {
    return nodeExecutionTime;
  }

  @Override
  public void setNodeExecutionTime(final long nodeExecutionTime) {
    this.nodeExecutionTime = nodeExecutionTime;
  }

  @Override
  public int getTaskCount() {
    return taskCount;
  }

  @Override
  public void setTaskCount(final int taskCount) {
    this.taskCount = taskCount;
    if (initialTaskCount <= 0) initialTaskCount = taskCount;
    if (currentTaskCount <= 0) currentTaskCount = taskCount;
  }

  @Override
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

  @Override
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

  @Override
  public long getExecutionStartTime() {
    return executionStartTime;
  }

  @Override
  public void setExecutionStartTime(final long executionStartTime) {
    this.executionStartTime = executionStartTime;
  }

  @Override
  public int getInitialTaskCount() {
    return initialTaskCount;
  }

  @Override
  public JobSLA getSLA() {
    return jobSLA;
  }

  @Override
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

  @Override
  public void setName(final String name) {
    this.name = name;
  }

  @Override
  public JobMetadata getMetadata() {
    return jobMetadata;
  }

  @Override
  public void setMetadata(final JobMetadata jobMetadata) {
    this.jobMetadata = jobMetadata;
  }

  @Override
  public String getUuid() {
    return uuid;
  }

  @Override
  public void setUuid(final String jobUuid) {
    this.uuid = jobUuid;
  }

  @Override
  public int getCurrentTaskCount() {
    return currentTaskCount;
  }

  @Override
  public void setCurrentTaskCount(final int currentTaskCount) {
    this.currentTaskCount = currentTaskCount;
  }

  @Override
  public boolean isPending() {
    return getParameter(BundleParameter.JOB_PENDING, false);
  }

  @Override
  public boolean isRequeue() {
    return getParameter(BundleParameter.JOB_REQUEUE, false);
  }

  @Override
  public void setRequeue(final boolean requeue) {
    setParameter(BundleParameter.JOB_REQUEUE, requeue);
  }

  @Override
  public int getDriverQueueTaskCount() {
    return driverQueueTaskCount;
  }

  @Override
  public void setDriverQueueTaskCount(final int driverQueueTaskCount) {
    this.driverQueueTaskCount = driverQueueTaskCount;
  }

  @Override
  public boolean isHandshake() {
    return handshake;
  }

  @Override
  public void setHandshake(final boolean handshake) {
    this.handshake = handshake;
  }
}
