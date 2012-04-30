package org.jppf.client.balancer.utils;

import java.util.concurrent.Future;

/**
 * Created with IntelliJ IDEA.
 * User: martin
 * Date: 4/29/12
 * Time: 10:34 PM
 * To change this template use File | Settings | File Templates.
 */
public interface JPPFFuture<V> extends Future<V>
{

  public void addListener(final Listener listener);

  public void removeListener(final Listener listener);

  public static interface Listener
  {
    public void onDone(final JPPFFuture<?> future);
  }
}
