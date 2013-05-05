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

package org.jppf.node.event;

import static org.jppf.node.event.NodeLifeCycleEventType.*;

import java.util.*;

import org.jppf.classloader.AbstractJPPFClassLoader;
import org.jppf.node.Node;
import org.jppf.node.protocol.*;
import org.jppf.task.storage.DataProvider;
import org.jppf.utils.*;
import org.slf4j.*;

/**
 * This class handles the firing of node life cycle events and the listeners that subscribe to these events.
 * @author Laurent Cohen
 * @exclude
 */
public class LifeCycleEventHandler
{
  /**
   * Logger for this class.
   */
  private static Logger log = LoggerFactory.getLogger(LifeCycleEventHandler.class);
  /**
   * Determines whether debug-level logging is enabled.
   */
  private static boolean debugEnabled = log.isDebugEnabled();
  /**
   * If <code>true</code> (the default), then {@link Error}s caught in the listeners methods will be propagated, otherwise they will be just logged.
   */
  private static final boolean propagateErrors = JPPFConfiguration.getProperties().getBoolean("jppf.node.listener.errors.propagate", true);
  /**
   * The default node life cycle error handler, used when the life cycle listener implementation does not implement {@link NodeLifeCycleErrorHandler}.
   */
  private static final NodeLifeCycleErrorHandler DEFAULT_ERROR_HANDLER = new DefaultLifeCycleErrorHandler();
  /**
   * The list of listeners to this object's events.
   */
  private final List<NodeLifeCycleListener> listeners = new ArrayList<NodeLifeCycleListener>();

  /**
   * The object that manages the job executions for the node.
   */
  private final Node node;

  /**
   * Initialize this event handler with the specified execution manager.
   * @param node an object representing the JPPF node.
   */
  public LifeCycleEventHandler(final Node node)
  {
    this.node = node;
  }

  /**
   * Add a listener to the list of listeners.
   * @param listener the listener to add.
   */
  public void addNodeLifeCycleListener(final NodeLifeCycleListener listener)
  {
    if (listener == null) return;
    synchronized (listeners)
    {
      listeners.add(listener);
    }
  }

  /**
   * Remove a listener from the list of listeners.
   * @param listener the listener to remove.
   */
  public void removeNodeLifeCycleListener(final NodeLifeCycleListener listener)
  {
    if (listener == null) return;
    synchronized (listeners)
    {
      listeners.remove(listener);
    }
  }

  /**
   * Remove all listeners from the list of listeners.
   */
  public void removeAllListeners()
  {
    synchronized (listeners)
    {
      listeners.clear();
    }
  }

  /**
   * Notify all listeners that the node is starting.
   */
  public void fireNodeStarting()
  {
    NodeLifeCycleEvent event = new NodeLifeCycleEvent(node, NODE_STARTING);
    synchronized (listeners)
    {
      for (NodeLifeCycleListener listener : listeners)
      {
        try
        {
          listener.nodeStarting(event);
        }
        catch(Throwable t)
        {
          handleError(listener, event, t);
        }
      }
    }
  }

  /**
   * Notify all listeners that the node is terminating.
   */
  public void fireNodeEnding()
  {
    NodeLifeCycleEvent event = new NodeLifeCycleEvent(node, NODE_ENDING);
    synchronized (listeners)
    {
      for (NodeLifeCycleListener listener : listeners)
      {
        try
        {
          listener.nodeEnding(event);
        }
        catch(Throwable t)
        {
          handleError(listener, event, t);
        }
      }
    }
  }

  /**
   * Notify all listeners that the node has loaded a job header and before the <code>DataProvider</code> or any of the tasks has been loaded..
   * @param job the job that is about to be or has been executed.
   * @param cl the class loader used to load the tasks and the classes they need from the client.
   */
  public void fireJobHeaderLoaded(final JPPFDistributedJob job, final AbstractJPPFClassLoader cl)
  {
    NodeLifeCycleEvent event = new NodeLifeCycleEvent(node, JOB_HEADER_LOADED, job, cl);
    synchronized (listeners)
    {
      for (NodeLifeCycleListener listener : listeners)
      {
        try
        {
          listener.jobHeaderLoaded(event);
        }
        catch(Throwable t)
        {
          handleError(listener, event, t);
        }
      }
    }
  }

  /**
   * Notify all listeners that the node is starting a job.
   * @param job the job that is about to be or has been executed.
   * @param cl the class loader used to load the tasks and the classes they need from the client.
   * @param tasks the tasks about to be or which have been executed.
   * @param dataProvider the data provider for the current job.
   */
  public void fireJobStarting(final JPPFDistributedJob job, final AbstractJPPFClassLoader cl, final List<Task> tasks, final DataProvider dataProvider)
  {
    NodeLifeCycleEvent event = new NodeLifeCycleEvent(node, JOB_STARTING, job, cl, tasks, dataProvider);
    synchronized (listeners)
    {
      for (NodeLifeCycleListener listener : listeners)
      {
        try
        {
          listener.jobStarting(event);
        }
        catch(Throwable t)
        {
          handleError(listener, event, t);
        }
      }
    }
  }

  /**
   * Notify all listeners that the node is completing a job.
   * @param job the job that is about to be or has been executed.
   * @param cl the class loader used to load the tasks and the classes they need from the client.
   * @param tasks the tasks about to be or which have been executed.
   * @param dataProvider the data provider for the current job.
   */
  public void fireJobEnding(final JPPFDistributedJob job, final AbstractJPPFClassLoader cl, final List<Task> tasks, final DataProvider dataProvider)
  {
    NodeLifeCycleEvent event = new NodeLifeCycleEvent(node, JOB_ENDING, job, cl, tasks, dataProvider);
    synchronized (listeners)
    {
      for (NodeLifeCycleListener listener : listeners)
      {
        try
        {
          listener.jobEnding(event);
        }
        catch(Throwable t)
        {
          handleError(listener, event, t);
        }
      }
    }
  }

  /**
   * Load all listener instances found in the class path via a service definition.
   */
  public void loadListeners()
  {
    Iterator<NodeLifeCycleListener> it = ServiceFinder.lookupProviders(NodeLifeCycleListener.class);
    while (it.hasNext())
    {
      try
      {
        NodeLifeCycleListener listener = it.next();
        addNodeLifeCycleListener(listener);
        if (debugEnabled) log.debug("successfully added node life cycle listener " + listener.getClass().getName());
      }
      catch(Error e)
      {
        log.error(e.getMessage(), e);
      }
    }
  }

  /**
   * Handle an uncaught throwable raised in one of the methods of a listener.
   * @param listener the listener whose method was being executed when the thowable was raised.
   * @param event the event notification for which an error was raised.
   * @param t the uncaught throwable that was raised during the notification. 
   */
  private void handleError(final NodeLifeCycleListener listener, final NodeLifeCycleEvent event, final Throwable t)
  {
    NodeLifeCycleErrorHandler handler = (listener instanceof NodeLifeCycleErrorHandler) ? (NodeLifeCycleErrorHandler) listener : DEFAULT_ERROR_HANDLER;
    try
    {
      handler.handleError(listener, event, t);
    }
    catch (Exception e)
    {
      String msg = "exception occurred while invoking error handler "  + handler + " : ";
      if (debugEnabled) log.debug(msg, e);
      else log.error(msg + ExceptionUtils.getMessage(e));
    }
  }
}
