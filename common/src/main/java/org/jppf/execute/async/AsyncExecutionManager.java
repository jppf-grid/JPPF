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

package org.jppf.execute.async;

import java.util.concurrent.ExecutorService;

import org.jppf.execute.*;
import org.jppf.node.protocol.*;

/**
 * Instances of this interface manage the execution of JPPF tasks by a node.
 * @author Laurent Cohen
 * @exclude
 */
public interface AsyncExecutionManager {
  /**
   * Execute the specified tasks of the specified tasks bundle.
   * @param bundleWithTakss the bundle to execute with its associated tasks.
   * @throws Exception if the execution failed.
   */
  void execute(BundleWithTasks bundleWithTakss) throws Exception;

  /**
   * Cancel all executing or pending tasks.
   * @param callOnCancel determines whether the onCancel() callback method of each task should be invoked.
   * @param requeue true if the job should be requeued on the server side, false otherwise.
   */
  void cancelAllTasks(boolean callOnCancel, boolean requeue);

  /**
   * Cancel all executing or pending tasks for the specified job.
   * @param jobUuid the uuid of the job to cancel.
   * @param callOnCancel determines whether the onCancel() callback method of each task should be invoked.
   * @param requeue true if the job should be requeued on the server side, false otherwise.
   */
  void cancelJob(String jobUuid, boolean callOnCancel, boolean requeue);

  /**
   * Shutdown this execution manager.
   */
  void shutdown();

  /**
   * Get the executor used by this execution manager.
   * @return an {@code ExecutorService} instance.
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
   * Get the number of bundles with the specified job uuid that are pending or being processed.
   * @param jobUuid the uuid of the job for which to count the bundles.
   * @return number of bundles.
   */
  int getNbBundles(final String jobUuid);

  /**
   * Register a listener with this execution manager.
   * @param listener the listener to register.
   */
  void addExecutionManagerListener(final ExecutionManagerListener listener);

  /**
   * Remove a listener from the registered listeners.
   * @param listener the listener to remove.
   */
  void removeExecutionManagerListener(final ExecutionManagerListener listener);

  /**
   * Notification that a task has finished executing.
   * @param taskWrapper the task that finished its execution.
   */
  void taskEnded(NodeTaskWrapper taskWrapper);

  /**
   * Called when a task bundle s being received by the node, and before it is submitted to this execution manager.
   * @param bundle the bundle to process.
   */
  void addPendingJobEntry(TaskBundle bundle);
}
