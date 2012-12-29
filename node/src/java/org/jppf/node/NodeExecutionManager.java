/*
 * JPPF.
 * Copyright (C) 2005-2012 JPPF Team.
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

package org.jppf.node;

import java.util.List;

import org.jppf.JPPFNodeReconnectionNotification;
import org.jppf.node.protocol.*;

/**
 * Instances of this interface manage the execution of JPPF tasks by a node.
 * @author Laurent Cohen
 */
public interface NodeExecutionManager
{
  /**
   * Get the job currently being executed.
   * @return a {@link JPPFDistributedJob} instance, or null if no job is being executed.
   */
  JPPFDistributedJob getCurrentJob();

  /**
   * Get the list of tasks currently being executed.
   * @return a list of {@link Task} instances, or null if the node is idle.
   */
  List<Task> getTasks();

  /**
   * Get the id of the job currently being executed.
   * @return the job id as a string, or null if no job is being executed.
   */
  String getCurrentJobId();

  /**
   * Get the node holding this execution manager.
   * @return a {@link Node} instance.
   */
  Node getNode();

  /**
   * Get the thread manager for this node.
   * @return a {@link ThreadManager} instance.
   */
  ThreadManager getThreadManager();

  /**
   * Notification sent by a node task wrapper when a task is complete.
   * @param task the task that just ended.
   * @param taskNumber identifier for the task future.
   * @param info the cpu time and wall clock time taken by the task.
   * @param elapsedTime the wall clock time taken by the task
   * @exclude
   */
  void taskEnded(Task task, long taskNumber, NodeExecutionInfo info, long elapsedTime);

  /**
   * Get the notification that the node must reconnect to the driver.
   * @return a {@link JPPFNodeReconnectionNotification} instance.
   * @exclude
   */
  JPPFNodeReconnectionNotification getReconnectionNotification();

  /**
   * Set the notification that the node must reconnect to the driver.
   * @param reconnectionNotification a {@link JPPFNodeReconnectionNotification} instance.
   * @exclude
   */
  void setReconnectionNotification(JPPFNodeReconnectionNotification reconnectionNotification);
}
