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

package org.jppf.client.balancer.utils;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.FutureTask;

/**
 * Future task that notifies transition to done state.
 * @param <V> The result type returned by <code>get</code> method.
 * @author Martin JANDA
 */
public class JPPFFutureTask<V> extends FutureTask<V> implements JPPFFuture<V>
{
  /**
   * List of listeners for this task.
   */
  private final List<Listener> listenerList = new ArrayList<Listener>();

  /**
   * Creates a <tt>FutureTask</tt> that will, upon running, execute the
   * given <tt>Callable</tt>.
   *
   * @param callable the callable task.
   * @throws NullPointerException if callable is null.
   */
  public JPPFFutureTask(final Callable<V> callable)
  {
    super(callable);
  }

  /**
   *
   * @param runnable the runnable task.
   * @param result the result returned on successful completion.
   * @throws NullPointerException if runnable is null.
   */
  public JPPFFutureTask(final Runnable runnable, final V result)
  {
    super(runnable, result);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  protected void done()
  {
    Listener[] listeners;
    synchronized (listenerList) {
      listeners = listenerList.toArray(new Listener[listenerList.size()]);
    }
    for (Listener listener : listeners)
    {
      listener.onDone(this);
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void addListener(final Listener listener)
  {
    if (listener == null) throw new IllegalArgumentException("listener is null");

    if (isDone())
    {
      listener.onDone(this);
    }
    else
    {
      synchronized (listenerList)
      {
        listenerList.add(listener);
      }
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void removeListener(final Listener listener)
  {
    if (listener == null) throw new IllegalArgumentException("listener is null");

    synchronized (listenerList)
    {
      listenerList.remove(listener);
    }
  }
}
