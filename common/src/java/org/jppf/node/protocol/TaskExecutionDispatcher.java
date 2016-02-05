/*
 * JPPF.
 * Copyright (C) 2005-2015 JPPF Team.
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

import java.util.*;
import java.util.concurrent.*;

import org.jppf.node.event.*;
import org.jppf.utils.ServiceFinder;

/**
 * 
 * @author Laurent Cohen
 * @exclude
 * @since 4.0
 */
public class TaskExecutionDispatcher {
  /**
   * List of listeners to task execution events.
   */
  private final List<TaskExecutionListener> taskExecutionListeners = new CopyOnWriteArrayList<>();
  /**
   * Executes the dispatching of notifications asynchronously.
   */
  private final ExecutorService executor = Executors.newSingleThreadExecutor();
  /**
   * The bundle whose tasks are currently being executed.
   */
  private TaskBundle bundle = null;
  /**
   * Class loader used to discover listeners via SPI.
   */
  private ClassLoader loader = null;

  /**
   * Intialize this dispatcher with the specified class loader.
   * @param loader the class loader used to discover listeners via SPI.
   */
  public TaskExecutionDispatcher(final ClassLoader loader) {
    this.loader = loader;
    loadListeners();
  }

  /**
   * Add a task execution listener to the list of task execution listeners.
   * @param listener the listener to add.
   */
  public void addTaskExecutionListener(final TaskExecutionListener listener) {
    taskExecutionListeners.add(listener);
  }

  /**
   * Remove a task execution listener from the list of task execution listeners.
   * @param listener the listener to remove.
   */
  public void removeTaskExecutionListener(final TaskExecutionListener listener) {
    taskExecutionListeners.remove(listener);
  }

  /**
   * Fire an event to notify that a task has ended its execution.
   * @param task the JPPF task from which the event originates.
   * @param jobId the id of the job this task belongs to.
   * @param jobName the name of the job this task belongs to.
   * @param cpuTime the cpu time taken by the task.
   * @param elapsedTime the wall clock time taken by the task.
   * @param error determines whether the task had an exception.
   */
  public void fireTaskEnded(final Task<?> task, final String jobId, final String jobName, final long cpuTime, final long elapsedTime, final boolean error) {
    TaskExecutionEvent event = new TaskExecutionEvent(task, jobId, jobName, cpuTime, elapsedTime, error);
    executor.submit(new NotificationTask(event));
  }

  /**
   * Fire an event from a task.
   * @param task the JPPF task from which the event originates.
   * @param userObject a user-defined object to send as part of the notification.
   * @param sendViaJmx if <code>true</code> then also send this notification via the JMX MBean, otherwise only send to local listeners.
   */
  public void fireTaskNotification(final Task<?> task, final Object userObject, final boolean sendViaJmx) {
    TaskExecutionEvent event = null;
    if (bundle == null) event = new TaskExecutionEvent(task, null, null, userObject, sendViaJmx);
    else event = new TaskExecutionEvent(task, bundle.getUuid(), bundle.getName(), userObject, sendViaJmx);
    executor.submit(new NotificationTask(event));
  }

  /**
   * Release the resources used by this dispatcher.
   */
  public void close() {
    executor.shutdownNow();
  }

  /**
   * 
   */
  private class NotificationTask implements Runnable {
    /**
     * The event to dispatch.
     */
    private final TaskExecutionEvent event;

    /**
     * Initiailize this task with the specified event.
     * @param event the event to dispatch.
     */
    public NotificationTask(final TaskExecutionEvent event) {
      this.event = event;
    }

    @Override
    public void run() {
      for (TaskExecutionListener listener : taskExecutionListeners) {
        //if ((listener instanceof JPPFNodeTaskMonitorMBean) && !event.isSendViaJmx()) continue;
        if (event.isTaskCompletion()) listener.taskExecuted(event);
        else listener.taskNotification(event);
      }
    }
  }

  /**
   * Set the bundle whose tasks are currently being executed.
   * @param bundle a {@link TaskBundle} instance.
   */
  public void setBundle(final TaskBundle bundle) {
    this.bundle = bundle;
  }

  /**
   * Register all listeners discovered via SPI.
   */
  private void loadListeners() {
    Iterator<TaskExecutionListener> it = ServiceFinder.lookupProviders(TaskExecutionListener.class, loader);
    while (it.hasNext()) addTaskExecutionListener(it.next());
  }
}
