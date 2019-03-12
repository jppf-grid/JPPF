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

package org.jppf.node.event;

import static org.jppf.node.event.NodeLifeCycleEventType.*;

import java.util.List;

import org.jppf.classloader.AbstractJPPFClassLoader;
import org.jppf.node.Node;
import org.jppf.node.protocol.*;
import org.jppf.server.node.JPPFNode;
import org.jppf.utils.*;
import org.slf4j.*;

/**
 * This class handles the firing of node life cycle events and the listeners that subscribe to these events.
 * @author Laurent Cohen
 * @exclude
 */
public class LifeCycleEventHandler extends ServiceProviderHandler<NodeLifeCycleListener> {
  /**
   * Logger for this class.
   */
  private static Logger log = LoggerFactory.getLogger(LifeCycleEventHandler.class);
  /**
   * The default node life cycle error handler, used when the life cycle listener implementation does not implement {@link NodeLifeCycleErrorHandler}.
   */
  private static final NodeLifeCycleErrorHandler DEFAULT_ERROR_HANDLER = new DefaultLifeCycleErrorHandler();

  /**
   * The object that manages the job executions for the node.
   */
  private final Node node;

  /**
   * Initialize this event handler with the specified execution manager.
   * @param node an object representing the JPPF node.
   */
  public LifeCycleEventHandler(final Node node) {
    super(NodeLifeCycleListener.class, ((JPPFNode) node).getClassLoader());
    this.node = node;
  }

  /**
   * Notify all listeners that the node is starting.
   */
  public void fireNodeStarting() {
    final NodeLifeCycleEvent event = new NodeLifeCycleEvent(node, NODE_STARTING);
    for (final NodeLifeCycleListener listener : providers) {
      try {
        listener.nodeStarting(event);
      } catch(final Throwable t) {
        handleError(listener, event, t);
      }
    }
  }

  /**
   * Notify all listeners that the node is terminating.
   */
  public void fireNodeEnding() {
    final NodeLifeCycleEvent event = new NodeLifeCycleEvent(node, NODE_ENDING);
    for (final NodeLifeCycleListener listener : providers) {
      try {
        listener.nodeEnding(event);
      } catch(final Throwable t) {
        handleError(listener, event, t);
      }
    }
  }

  /**
   * Notify all listeners that the node has loaded a job header and before the <code>DataProvider</code> or any of the tasks has been loaded..
   * @param job the job that is about to be or has been executed.
   * @param cl the class loader used to load the tasks and the classes they need from the client.
   */
  public void fireJobHeaderLoaded(final JPPFDistributedJob job, final AbstractJPPFClassLoader cl) {
    final NodeLifeCycleEvent event = new NodeLifeCycleEvent(node, JOB_HEADER_LOADED, job, cl);
    for (final NodeLifeCycleListener listener : providers) {
      try {
        listener.jobHeaderLoaded(event);
      } catch(final Throwable t) {
        handleError(listener, event, t);
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
  public void fireJobStarting(final JPPFDistributedJob job, final AbstractJPPFClassLoader cl, final List<Task<?>> tasks, final DataProvider dataProvider) {
    final NodeLifeCycleEvent event = new NodeLifeCycleEvent(node, JOB_STARTING, job, cl, tasks, dataProvider);
    for (final NodeLifeCycleListener listener : providers) {
      try {
        listener.jobStarting(event);
      } catch(final Throwable t) {
        handleError(listener, event, t);
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
  public void fireJobEnding(final JPPFDistributedJob job, final AbstractJPPFClassLoader cl, final List<Task<?>> tasks, final DataProvider dataProvider) {
    final NodeLifeCycleEvent event = new NodeLifeCycleEvent(node, JOB_ENDING, job, cl, tasks, dataProvider);
    for (final NodeLifeCycleListener listener : providers) {
      try {
        listener.jobEnding(event);
      } catch(final Throwable t) {
        handleError(listener, event, t);
      }
    }
  }

  /**
   * Notify all listeners that the node is between jobs.
   */
  public void fireBeforeNextJob() {
    final NodeLifeCycleEvent event = new NodeLifeCycleEvent(node, BEFORE_NEXT_JOB, null, null, null, null);
    for (final NodeLifeCycleListener listener : providers) {
      try {
        listener.beforeNextJob(event);
      } catch(final Throwable t) {
        handleError(listener, event, t);
      }
    }
  }

  /**
   * Handle an uncaught throwable raised in one of the methods of a listener.
   * @param listener the listener whose method was being executed when the thowable was raised.
   * @param event the event notification for which an error was raised.
   * @param t the uncaught throwable that was raised during the notification. 
   */
  private static void handleError(final NodeLifeCycleListener listener, final NodeLifeCycleEvent event, final Throwable t) {
    final NodeLifeCycleErrorHandler handler = (listener instanceof NodeLifeCycleErrorHandler) ? (NodeLifeCycleErrorHandler) listener : DEFAULT_ERROR_HANDLER;
    try {
      handler.handleError(listener, event, t);
    } catch (final Exception e) {
      final String msg = "exception occurred while invoking error handler "  + handler + " :\n" + ExceptionUtils.getStackTrace(e);
      log.error(msg);
    }
  }
}
