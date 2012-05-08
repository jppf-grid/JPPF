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

import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.FutureTask;

/**
 * Created with IntelliJ IDEA.
 * User: martin
 * Date: 4/29/12
 * Time: 10:37 PM
 * To change this template use File | Settings | File Templates.
 */
public class JPPFFutureTask<V> extends FutureTask<V> implements JPPFFuture<V>
{

  private final List<Listener> listeners = new CopyOnWriteArrayList<Listener>();

  public JPPFFutureTask(final Callable<V> callable)
  {
    super(callable);
  }

  public JPPFFutureTask(final Runnable runnable, final V result)
  {
    super(runnable, result);
  }

  @Override
  protected void done()
  {
    for (Listener listener : listeners)
    {
      listener.onDone(this);
    }
  }

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
      listeners.add(listener);
    }
  }

  @Override
  public void removeListener(final Listener listener)
  {
    if (listener == null) throw new IllegalArgumentException("listener is null");

    listeners.remove(listener);
  }
}
