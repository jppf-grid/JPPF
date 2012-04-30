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
