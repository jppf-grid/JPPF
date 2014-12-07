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

package org.jppf.node.protocol;

import java.io.Serializable;

import org.jppf.scheduling.JPPFSchedule;
import org.jppf.utils.JPPFCallable;

/**
 * Interface for all tasks submitted to the execution server.
 * @param <T> the type of results produced by the task.
 * @author Laurent Cohen
 */
public interface Task<T> extends Runnable, Serializable {
  /**
   * Get the result of the task execution.
   * @return the task result.
   */
  T getResult();

  /**
   * Set the result of the task execution.
   * @param  result the result of this task's execution.
   */
  void setResult(T result);

  /**
   * Get the Throwable that was raised by this task's execution.
   * @return a <code>Throwable</code> instance, or {@code null} if no exception was raised.
   * @since 4.0
   */
  Throwable getThrowable();

  /**
   * Get the {@code Throwable} that was raised by this task's execution, wrapped in an Exception.
   * @return a <code>Exception</code> instance, or {@code null} if no exception was raised.
   * @since 5.0
   */
  Exception getException();

  /**
   * Sets the Throwable that was raised by this task's execution in the <code>run()</code> method.
   * The Throwable is normally set by the JPPF framework.
   * @param throwable a <code>Throwable</code> instance.
   * @since 4.0
   */
  void setThrowable(Throwable throwable);

  /**
   * Get the provider of shared data for this task.
   * @return a <code>DataProvider</code> instance.
   */
  DataProvider getDataProvider();

  /**
   * Set the provider of shared data for this task.
   * @param dataProvider a <code>DataProvider</code> instance.
   * @exclude
   */
  void setDataProvider(DataProvider dataProvider);

  /**
   * Get the user-assigned id for this task.
   * @return the id as a string.
   */
  String getId();

  /**
   * Set the user-assigned id for this task.
   * @param id the id as a string.
   */
  void setId(String id);

  /**
   * Callback invoked when this task is cancelled.
   * The default implementation does nothing and should be overriden by
   * subclasses that desire to implement a specific behaviour on cancellation.
   */
  void onCancel();

  /**
   * Callback invoked when this task times out.
   * The default implementation does nothing and should be overriden by
   * subclasses that desire to implement a specific behaviour on timeout.
   */
  void onTimeout();

  /**
   * Get the <code>JPPFRunnable</code>-annotated object or POJO wrapped by this task.
   * @return an object or class that is JPPF-annotated.
   */
  Object getTaskObject();

  /**
   * Get the task timeout schedule configuration.
   * @return a <code>JPPFSchedule</code> instance.
   */
  JPPFSchedule getTimeoutSchedule();

  /**
   * Get the task timeout schedule configuration.
   * @param timeoutSchedule a <code>JPPFSchedule</code> instance.
   */
  void setTimeoutSchedule(JPPFSchedule timeoutSchedule);

  /**
   * Returns the position of this task in the job in which it was submitted.
   * @return the position of this task as an <code>int</code>.
   * @exclude
   */
  int getPosition();

  /**
   * Set the position of this task in the job in which it was submitted.
   * @param position the position of this task as an <code>int</code>.
   * @exclude
   */
  void setPosition(int position);

  /**
   * Determine whether this task is executing within a node, or locally on the client side.
   * @return <code>true</code> if this task is executing in a node, <code>false</code> if it is on the client side.
   */
  boolean isInNode();

  /**
   * Determine whether this task is executing within a node, or locally on the client side.
   * @param inNode <code>true</code> if this task is executing in a node, <code>false</code> if it is on the client side.
   * @exclude
   */
  void setInNode(boolean inNode);

  /**
   * Compute a value on the client-side, as the result of the execution of a {@link JPPFCallable}.
   * <p>Any {@link Throwable} raised in the callable's <code>call()</code> method will be thrown as the result of this method.
   * If the Throwable is an instance of <code>Exception</code> or one of its subclasses, it is thrown as such, otherwise it is wrapped
   * into a {@link org.jppf.JPPFException}.
   * @param <V> the type of results returned by the callable.
   * @param callable the callable to execute on the client side.
   * @return the value computed on the client, or null if the value could not be computed.
   * @throws Exception if the execution of the callable in the client resulted in a {@link Throwable} being raised.
   */
  <V> V compute(JPPFCallable<V> callable) throws Exception;

  /**
   * Causes the task to send a notification to all listeners.
   * This method can be called at any time during the execution of the task,
   * i.e. while in the execution scope of the {@link #run()}, {@link #onTimeout()} or {@link #onCancel()} methods.
   * <p>If the parameter <code>sendViaJmx</code> is {@code true}, then a notfication will also be sent
   * by the {@link org.jppf.management.JPPFNodeTaskMonitorMBean JPPFNodeTaskMonitorMBean} mbean, otherwise only local listeners will be notified.
   * @param userObject a user-defined object to send as part of the notification.
   * @param sendViaJmx if <code>true</code> then also send the notification via the JMX MBean, otherwise only send to local listeners.
   * If the task is executing within a client local executor, this parameter has no effect. 
   * @since 4.0
   */
  void fireNotification(Object userObject, boolean sendViaJmx);

  /**
   * Determine whether this task should be resubmitted by the server.
   * @return {@code true} to indicate this task will be resubmitted, {@code false} otherwise.
   * @since 4.2
   */
  boolean isResubmit();

  /**
   * Specify whether this task should be resubmitted by the server.
   * @param resubmit {@code true} to indicate this task will be resubmitted, {@code false} otherwise.
   * @since 4.2
   */
  void setResubmit(final boolean resubmit);

  /**
   * Get the maximum number of times a task can resubmit itself.
   * @return a positive integer, or -1 if this attribute was never set,
   * in which case the value set in the job SLA will be used.
   */
  int getMaxResubmits();

  /**
   * Set the maximum number of times a task can resubmit itself.
   * If the specified value is greater than or equal to zero, it will override
   * the job-wide value provided by {@link JobSLA#getMaxTaskResubmits()}.
   * @param maxResubmits the maximum number of resubmits.
   */
  void setMaxResubmits(int maxResubmits);

  /**
   * Get the class loader used to load this task, or the object it wraps if any.
   * @return a {@link ClassLoader} instance.
   * @since 5.0
   * @exclude
   */
  ClassLoader getTaskClassLoader();
}
