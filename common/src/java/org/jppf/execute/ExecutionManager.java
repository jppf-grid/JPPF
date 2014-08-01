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

package org.jppf.execute;

import java.util.List;
import java.util.concurrent.ExecutorService;

import org.jppf.node.protocol.*;

/**
 * Instances of this interface manage the execution of JPPF tasks by a node.
 * @author Laurent Cohen
 * @exclude
 */
public interface ExecutionManager {
  /**
   * Execute the specified tasks of the specified tasks bundle.
   * @param bundle the bundle to which the tasks are associated.
   * @param taskList the list of tasks to execute.
   * @throws Exception if the execution failed.
   */
  void execute(TaskBundle bundle, List<Task<?>> taskList) throws Exception;

  /**
   * Cancel all executing or pending tasks.
   * @param callOnCancel determines whether the onCancel() callback method of each task should be invoked.
   * @param requeue true if the job should be requeued on the server side, false otherwise.
   */
  void cancelAllTasks(boolean callOnCancel, boolean requeue);

  /**
   * Shutdown this execution manager.
   */
  void shutdown();

  /**
   * Get the id of the job currently being executed.
   * @return the job id as a string, or null if no job is being executed.
   */
  String getCurrentJobId();

  /**
   * Get the executor used by this execution manager.
   * @return an <code>ExecutorService</code> instance.
   */
  ExecutorService getExecutor();

  /**
   * Set the size of the node's thread pool.
   * @param size the size as an int.
   */
  void setThreadPoolSize(int size);

  /**
   * Get the size of the node's thread pool.
   * @return the size as an int.
   */
  int getThreadPoolSize();

  /**
   * Get the priority assigned to the execution threads.
   * @return the priority as an int value.
   */
  int getThreadsPriority();

  /**
   * Update the priority of all execution threads.
   * @param newPriority the new priority to set.
   */
  void updateThreadsPriority(int newPriority);

  /**
   * Get the thread manager for this node.
   * @return a {@link ThreadManager} instance.
   */
  ThreadManager getThreadManager();

  /**
   * Determine whether the current job has been cancelled, including before starting its execution.
   * @return <code>true</code> if the job has been cancelled, <code>false</code> otherwise.
   */
  boolean isJobCancelled();

  /**
   * Specify whether the current job has been cancelled, including before starting its execution.
   * @param jobCancelled <code>true</code> if the job has been cancelled, <code>false</code> otherwise.
   */
  void setJobCancelled(boolean jobCancelled);

  /**
   * Get the object which dispatches tasks notifications to registered listeners.
   * @return a {@link TaskExecutionDispatcher} instance.
   */
  TaskExecutionDispatcher getTaskNotificationDispatcher();

  /**
   * Determines whether the configuration has changed and resets the flag if it has.
   * @return true if the config was changed, false otherwise.
   */
  boolean checkConfigChanged();

  /**
   * Trigger the configuration changed flag.
   */
  void triggerConfigChanged();

  /**
   * Get the bundle whose tasks are currently being executed.
   * @return a {@link TaskBundle} instance.
   */
  TaskBundle getBundle();

  /**
   * Set the bundle whose tasks are currently being executed.
   * @param bundle a {@link TaskBundle} instance.
   */
  void setBundle(TaskBundle bundle);
}
