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

package org.jppf.node.protocol;

import java.io.Serializable;

import org.jppf.scheduling.JPPFSchedule;
import org.jppf.task.storage.DataProvider;
import org.jppf.utils.JPPFCallable;

/**
 * Interface for all tasks submitted to the execution server.
 * @param <T> the type of results produced by the task.
 * @author Laurent Cohen
 */
public interface Task<T> extends Runnable, Serializable
{
  /**
   * Get the result of the task execution.
   * @return the result as an array of bytes.
   */
  T getResult();

  /**
   * Set the result of the task execution.
   * @param  result the result of this task's execution.
   */
  void setResult(T result);

  /**
   * Get the exception that was raised by this task's execution. If the task raised a
   * {@link Throwable}, the exception is embedded into a {@link org.jppf.JPPFException}.
   * @return a <code>Exception</code> instance, or null if no exception was raised.
   */
  Exception getException();

  /**
   * Sets the exception that was raised by this task's execution in the <code>run</code> method.
   * The exception is set by the JPPF framework.
   * @param exception a <code>ClientApplicationException</code> instance.
   */
  void setException(Exception exception);

  /**
   * Get the provider of shared data for this task.
   * @return a <code>DataProvider</code> instance.
   */
  DataProvider getDataProvider();

  /**
   * Set the provider of shared data for this task.
   * @param dataProvider a <code>DataProvider</code> instance.
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
   * Callback invoked when this task is restarted.
   * The default implementation does nothing and should be overriden by
   * subclasses that desire to implement a specific behaviour on restart.
   * @deprecated the task restart feature is inherently unsafe, as it depends on the task
   * having a unique id among all the tasks running in the grid, which cannot be guaranteed.
   * This feature has been removed from the management APIs, with no replacement. 
   */
  void onRestart();

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
   */
  int getPosition();

  /**
   * Set the position of this task in the job in which it was submitted.
   * @param position the position of this task as an <code>int</code>.
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
   * Compute a value on the client-side, as the result of the execution of a {@link org.jppf.utils.JPPFCallable JPPFCallable}.
   * @param <V> the type of results returned by the callable.
   * @param callable the key from which to get the value.
   * @return the looked-up value, or null if the value could not be found.
   * @see org.jppf.utils.JPPFCallable
   */
  <V> V compute(JPPFCallable<V> callable);
}
