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

package org.jppf.server.queue;

import java.util.*;
import java.util.concurrent.locks.ReentrantLock;

import org.jppf.server.protocol.*;
import org.slf4j.*;

/**
 * Abstract superclass for all JPPFQueue implementations.
 * @author Laurent Cohen
 */
public abstract class AbstractJPPFQueue implements JPPFQueue
{
  /**
   * Logger for this class.
   */
  private static Logger log = LoggerFactory.getLogger(AbstractJPPFQueue.class);
  /**
   * Determines whether the debug level is enabled in the logging configuration, without the cost of a method call.
   */
  private static boolean debugEnabled = log.isDebugEnabled();
  /**
   * Used for synchronized access to the queue.
   */
  protected ReentrantLock lock = new ReentrantLock();
  /**
   * An ordered map of bundle sizes, mapping to a list of bundles of this size.
   */
  protected TreeMap<Integer, List<ServerJob>> sizeMap = new TreeMap<Integer, List<ServerJob>>();
  /**
   * 
   */
  protected int latestMaxSize = 0;
  /**
   * The list of registered listeners.
   */
  private final List<QueueListener> queueListeners = new ArrayList<QueueListener>();

  /**
   * Add a listener to the list of listeners.
   * @param listener the listener to add to the list.
   */
  public void addQueueListener(final QueueListener listener)
  {
    synchronized(queueListeners)
    {
      queueListeners.add(listener);
    }
  }

  /**
   * Remove a listener from the list of listeners.
   * @param listener the listener to remove from the list.
   */
  public void removeQueueListener(final QueueListener listener)
  {
    synchronized(queueListeners)
    {
      queueListeners.remove(listener);
    }
  }

  /**
   * Get the bundle size to use for bundle size tuning.
   * @param bundleWrapper the bundle to get the size from.
   * @return the bundle size as an int.
   */
  protected int getSize(final ServerJob bundleWrapper)
  {
    //return bundle.getTaskCount();
    return ((JPPFTaskBundle) bundleWrapper.getJob()).getInitialTaskCount();
  }

  /**
   * Notify all queue listeners of an event.
   * @param event - the event to notify of.
   */
  protected void fireQueueEvent(final QueueEvent event)
  {
    if (!event.isRequeued()) event.getBundleWrapper().fireJobQueued();
    else event.getBundleWrapper().fireJobUpdated();

    synchronized(queueListeners)
    {
      for (QueueListener listener : queueListeners) listener.newBundle(event);
    }
  }

  /**
   * Get the lock used for synchronized access to the queue.
   * @return a <code>ReentrantLock</code> instance.
   */
  public ReentrantLock getLock()
  {
    return lock;
  }
}
