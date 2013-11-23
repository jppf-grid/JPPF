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

package org.jppf.node.protocol;

import org.jppf.utils.TraversalList;
import org.jppf.utils.collections.Metadata;

/**
 * Interface for job headers sent ver the network.
 * @author Laurent Cohen
 * @exclude
 */
public interface TaskBundle extends JPPFDistributedJob, Metadata
{
  /**
   * Get the uuid path of the applications (driver or client) in whose classpath the class definition may be found.
   * @return the uuid path as a list of string elements.
   */
  TraversalList<String> getUuidPath();

  /**
   * Set the uuid path of the applications (driver or client) in whose classpath the class definition may be found.
   * @param uuidPath the uuid path as a list of string elements.
   */
  void setUuidPath(TraversalList<String> uuidPath);

  /**
   * Get the time it took a node to execute this task.
   * @return the time in milliseconds as a long value.
   */
  long getNodeExecutionTime();

  /**
   * Set the time it took a node to execute this task.
   * @param nodeExecutionTime the time in milliseconds as a long value.
   */
  void setNodeExecutionTime(long nodeExecutionTime);

  /**
   * Get the number of tasks in this bundle.
   * @return the number of tasks as an int.
   */
  int getTaskCount();

  /**
   * Set the number of tasks in this bundle.
   * @param taskCount the number of tasks as an int.
   */
  void setTaskCount(int taskCount);

  /**
   * Set the initial number of tasks in this bundle.
   * @param initialTaskCount the number of tasks as an int.
   */
  void setInitialTaskCount(int initialTaskCount);

  /**
   * Make a copy of this bundle.
   * @return a new <code>JPPFTaskBundle</code> instance.
   */
  TaskBundle copy();

  /**
   * Get the time at which the bundle is taken out of the queue for sending to a node.
   * @return the time as a long value.
   */
  long getExecutionStartTime();

  /**
   * Set the time at which the bundle is taken out of the queue for sending to a node.
   * @param executionStartTime the time as a long value.
   */
  void setExecutionStartTime(long executionStartTime);

  /**
   * Get the initial task count of this bundle.
   * @return the task count as an int.
   */
  int getInitialTaskCount();

  /**
   * Get the service level agreement between the job and the server.
   * @param jobSLA an instance of {@link JobSLA}.
   */
  void setSLA(JobSLA jobSLA);

  /**
   * Set the user-defined display name for the job.
   * @param name the display name as a string.
   */
  void setName(String name);

  /**
   * Set this bundle's metadata.
   * @param jobMetadata a {@link JobMetadata} instance.
   */
  void setMetadata(JobMetadata jobMetadata);

  /**
   * Set the uuid of the initial job.
   * @param jobUuid the uuid as a string.
   */
  void setUuid(String jobUuid);

  /**
   * Get the current number of tasks in this bundle.
   * @return the current number of tasks as an int.
   */
  int getCurrentTaskCount();

  /**
   * Set the current number of tasks in this bundle.
   * @param currentTaskCount the current number of tasks as an int.
   */
  void setCurrentTaskCount(int currentTaskCount);

  /**
   * Get the job requeue flag.
   * @return job requeue flag.
   */
  boolean isPending();

  /**
   * Get the job requeue flag.
   * @return job requeue flag.
   */
  boolean isRequeue();

  /**
   * Set the job requeue flag.
   * @param requeue job requeue flag.
   */
  void setRequeue(boolean requeue);

  /**
   * Get the number of tasks in this bundle at the time it is received by a driver.
   * @return the number of tasks as an int.
   */
  int getDriverQueueTaskCount();

  /**
   * Set the number of tasks in this bundle at the time it is received by a driver.
   * @param driverQueueTaskCount the number of tasks as an int.
   */
  void setDriverQueueTaskCount(int driverQueueTaskCount);

  /**
   * Determine whether this object is used for handshake instead of execution.
   * @return <code>true</code> if this bundle is a handshake bundle, <code>false</code> otherwise.
   */
  boolean isHandshake();

  /**
   * Specify whether this object is used for handshake instead of execution.
   * @param handshake <code>true</code> if this bundle is a handshake bundle, <code>false</code> otherwise.
   */
  void setHandshake(boolean handshake);

}