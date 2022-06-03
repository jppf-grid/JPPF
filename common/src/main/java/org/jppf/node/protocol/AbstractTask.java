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

package org.jppf.node.protocol;

import java.lang.reflect.*;

import org.jppf.JPPFException;
import org.jppf.node.Node;
import org.jppf.scheduling.JPPFSchedule;
import org.jppf.utils.JPPFCallable;

/**
 * Abstract superclass for all tasks submitted to the execution server.
 * This class provides the basic facilities to handle data shared among tasks, handling of task execution exception,
 * and handling of the execution results.<p>
 * JPPF clients have to extend this class and must implement the <code>run</code> method. In the
 * <code>run</code> method the task calculations are performed, and the result of the calculations
 * is set with the {@link #setResult(Object)} method:
 * <pre>
 * class MyTask extends AbstractTask&lt;Object&gt; {
 *   public void run() {
 *     // do the calculation ...
 *     setResult(myResult);
 *   }
 * }
 * </pre>
 * @param <T> the type of results returned by this task.
 * @author Laurent Cohen
 * @since 4.0
 */
public class AbstractTask<T> implements Task<T> {
  /**
   * Explicit serialVersionUID.
   */
  private static final long serialVersionUID = 1L;
  /**
   * The position of this task at the submission time.
   */
  private int position;
  /**
   * The result of the task execution.
   */
  private T result;
  /**
   * The {@code Throwable} that was raised by this task's execution.
   */
  private Throwable throwable;
  /**
   * The provider of shared data for this task.
   */
  private transient DataProvider dataProvider;
  /**
   * The task timeout schedule configuration.
   */
  private JPPFSchedule timeoutSchedule;
  /**
   * Determines whether this task is executing within a node, or locally on the client side.
   */
  private transient boolean inNode;
  /**
   * A user-assigned id for this task.
   */
  private String id;
  /**
   * Dispatches notifications from this task.
   */
  private transient TaskExecutionDispatcher executionDisptacher;
  /**
   * Whether this task should be resubmitted by the server.
   * @since 4.1
   */
  private transient boolean resubmit;
  /**
   * The max number of times a task can resubmit itself.
   */
  private transient int maxResubmits = -1;
  /**
   * The node in which this task is executing, if any.
   */
  private transient Node node;
  /**
   * The job this task is a part of.
   */
  private transient JPPFDistributedJob  job;

  /**
   *
   */
  public AbstractTask() {
  }

  @Override
  public T getResult() {
    return result;
  }

  @Override
  public Task<T> setResult(final T result) {
    this.result = result;
    return this;
  }

  @Override
  public Throwable getThrowable() {
    return throwable;
  }

  @Override
  public Task<T> setThrowable(final Throwable throwable) {
    this.throwable = throwable;
    return this;
  }

  @Override
  public DataProvider getDataProvider() {
    return dataProvider;
  }

  /**
   * {@inheritDoc}
   * @exclude
   */
  @Override
  public Task<T> setDataProvider(final DataProvider dataProvider) {
    this.dataProvider = dataProvider;
    return this;
  }

  @Override
  public String getId() {
    return id;
  }

  @Override
  public Task<T> setId(final String id) {
    this.id = id;
    return this;
  }

  @Override
  public void onCancel() {
  }

  @Override
  public void onTimeout() {
  }

  @Override
  public Object getTaskObject() {
    return this;
  }

  @Override
  public JPPFSchedule getTimeoutSchedule() {
    return timeoutSchedule;
  }

  @Override
  public Task<T> setTimeoutSchedule(final JPPFSchedule timeoutSchedule) {
    this.timeoutSchedule = timeoutSchedule;
    return this;
  }

  /**
   * {@inheritDoc}
   * @exclude
   */
  @Override
  public int getPosition() {
    return position;
  }

  /**
   * {@inheritDoc}
   * @exclude
   */
  @Override
  public Task<T> setPosition(final int position) {
    this.position = position;
    return this;
  }

  @Override
  public boolean isInNode() {
    return inNode;
  }

  /**
   * {@inheritDoc}
   * @exclude
   */
  @Override
  public Task<T> setInNode(final boolean inNode) {
    this.inNode = inNode;
    return this;
  }

  @Override
  @SuppressWarnings("unchecked")
  public <V> V compute(final JPPFCallable<V> callable) throws Exception {
    try {
      V result = null;
      if (isInNode()) {
        final ClassLoader cl = callable.getClass().getClassLoader();
        final Class<?> clClass = cl.getClass();
        Method m = null;
        try {
          m = clClass.getMethod("computeCallable", JPPFCallable.class);
        } catch(@SuppressWarnings("unused") final Exception e) {
          throw new JPPFException("the task class loader cannot send a computation request to the client, method 'computeCallable' is missing");
        }
        result = (V) m.invoke(cl, callable);
      }
      else result = callable.call();
      return result;
    } catch(final InvocationTargetException e) {
      final Throwable t = e.getCause();
      throw (t instanceof Exception) ? (Exception) t: e;
    }
  }

  @Override
  public Task<T> fireNotification(final Object userObject, final boolean sendViaJmx) {
    if (executionDisptacher != null) executionDisptacher.fireTaskNotification(this, userObject, sendViaJmx);
    return this;
  }

  /**
   * Set the task notification dispatcher onto this task.
   * @param executionDisptacher a {@link TaskExecutionDispatcher} instance.
   * @exclude
   */
  public void setExecutionDispatcher(final TaskExecutionDispatcher executionDisptacher) {
    this.executionDisptacher = executionDisptacher;
  }

  /**
   * {@inheritDoc}
   * @since 4.1
   */
  @Override
  public boolean isResubmit() {
    return resubmit;
  }

  /**
   * {@inheritDoc}
   * @since 4.1
   */
  @Override
  public Task<T> setResubmit(final boolean resubmit) {
    this.resubmit = resubmit;
    return this;
  }

  @Override
  public int getMaxResubmits() {
    return maxResubmits;
  }

  @Override
  public Task<T> setMaxResubmits(final int maxResubmits) {
    this.maxResubmits = maxResubmits;
    return this;
  }

  /**
   * {@inheritDoc}
   * @since 5.0
   * @exclude
   */
  @Override
  public ClassLoader getTaskClassLoader() {
    Object o = getTaskObject();
    if (o == null) o = this;
    return o.getClass().getClassLoader();
  }

  @Override
  public void run() {
  }

  @Override
  public boolean isInterruptible() {
    return true;
  }

  @Override
  public Node getNode() {
    return node;
  }

  /**
   * {@inheritDoc}
   * @exclude
   */
  @Override
  public Task<T> setNode(final Node node) {
    this.node = node;
    return this;
  }

  @Override
  public JPPFDistributedJob getJob() {
    return job;
  }

  /**
   * {@inheritDoc}
   * @exclude
   */
  @Override
  public Task<T> setJob(final JPPFDistributedJob job) {
    this.job = job;
    return this;
  }
}
