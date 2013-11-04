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

import org.jppf.JPPFException;
import org.jppf.classloader.AbstractJPPFClassLoader;
import org.jppf.scheduling.JPPFSchedule;
import org.jppf.task.storage.DataProvider;
import org.jppf.utils.JPPFCallable;

/**
 * Abstract superclass for all tasks submitted to the execution server.
 * This class provides the basic facilities to handle data shared among tasks, handling of task execution exception,
 * and handling of the execution results.<p>
 * JPPF clients have to extend this class and must implement the <code>run</code> method. In the
 * <code>run</code> method the task calculations are performed, and the result of the calculations
 * is set with the {@link #setResult(Object)} method:
 * <pre>
 * class MyTask extends JPPFTask {
 *   public void run() {
 *     // do the calculation ...
 *     setResult(myResult);
 *   }
 * }
 * </pre>
 * @param <T> the type of results retrun by this task.
 * @author Laurent Cohen
 */
public abstract class AbstractTask<T> implements Task<T>
{
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
  private T result = null;
  /**
   * The <code>Throwable</code> that was raised by this task's execution.
   */
  private Throwable throwable = null;
  /**
   * The provider of shared data for this task.
   */
  private transient DataProvider dataProvider = null;
  /**
   * The task timeout schedule configuration.
   */
  private JPPFSchedule timeoutSchedule = null;
  /**
   * Determines whether this task is executing within a node, or locally on the client side.
   */
  private boolean inNode = false;
  /**
   * A user-assigned id for this task.
   */
  private String id = null;

  @Override
  public T getResult()
  {
    return result;
  }

  @Override
  public void setResult(final T result)
  {
    this.result = result;
  }

  @Override
  @Deprecated
  public Exception getException()
  {
    if (throwable == null) return null;
    return throwable instanceof Exception ? (Exception) throwable : new JPPFException(throwable);
  }

  /**
   * {@inheritDoc}
   * @deprecated {@link #setThrowable(java.lang.Throwable)} should be used instead.
   */
  @Override
  @Deprecated
  public void setException(final Exception exception)
  {
    this.throwable = exception;
  }

  @Override
  public Throwable getThrowable()
  {
    return throwable;
  }

  @Override
  public void setThrowable(final Throwable throwable)
  {
    this.throwable = throwable;
  }

  @Override
  public DataProvider getDataProvider()
  {
    return dataProvider;
  }

  /**
   * {@inheritDoc}
   * @exclude
   */
  @Override
  public void setDataProvider(final DataProvider dataProvider)
  {
    this.dataProvider = dataProvider;
  }

  @Override
  public String getId()
  {
    return id;
  }

  @Override
  public void setId(final String id)
  {
    this.id = id;
  }

  @Override
  public void onCancel()
  {
  }

  @Override
  public void onTimeout()
  {
  }

  @Override
  public Object getTaskObject()
  {
    return this;
  }

  @Override
  public JPPFSchedule getTimeoutSchedule()
  {
    return timeoutSchedule;
  }

  @Override
  public void setTimeoutSchedule(final JPPFSchedule timeoutSchedule)
  {
    this.timeoutSchedule = timeoutSchedule;
  }

  /**
   * {@inheritDoc}
   * @exclude
   */
  @Override
  public int getPosition()
  {
    return position;
  }

  /**
   * {@inheritDoc}
   * @exclude
   */
  @Override
  public void setPosition(final int position)
  {
    this.position = position;
  }

  @Override
  public boolean isInNode()
  {
    return inNode;
  }

  /**
   * {@inheritDoc}
   * @exclude
   */
  @Override
  public void setInNode(final boolean inNode)
  {
    this.inNode = inNode;
  }

  @Override
  @SuppressWarnings("unchecked")
  public <V> V compute(final JPPFCallable<V> callable) throws Exception
  {
    V result = null;
    if (isInNode())
    {
      ClassLoader cl = callable.getClass().getClassLoader();
      if (cl instanceof AbstractJPPFClassLoader)
      {
        AbstractJPPFClassLoader loader = (AbstractJPPFClassLoader) cl;
        result = loader.computeCallable(callable);
      }
    }
    else result = callable.call();
    return result;
  }
}
