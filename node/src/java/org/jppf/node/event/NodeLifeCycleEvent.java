/*
 * JPPF.
 * Copyright (C) 2005-2014 JPPF Team.
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

import java.util.*;

import org.jppf.classloader.AbstractJPPFClassLoader;
import org.jppf.node.Node;
import org.jppf.node.protocol.*;

/**
 * Instances of this class represent node life cycle events.
 * @author Laurent Cohen
 */
public class NodeLifeCycleEvent extends EventObject
{
  /**
   * The class loader used to load the tasks and the classes they need from the client.
   */
  private AbstractJPPFClassLoader cl = null;
  /**
   * The tasks currently being executed.
   */
  private List<Task<?>> tasks = null;
  /**
   * The data provider for the current job, if any.
   */
  private DataProvider dataProvider = null;
  /**
   * The job that is about to be or has been executed.
   */
  private JPPFDistributedJob job = null;
  /**
   * The type of this event.
   */
  private final NodeLifeCycleEventType type;

  /**
   * Initialize this event with the specified execution manager.
   * <br>This constructor is used for <code>nodeStarting()</code> and <code>nodeEnding()</code> notifications only.
   * @param node an object representing the JPPF node. If the {@link NodeLifeCycleListener} was deployed in the server's classpath,
   * then it can be safely cast to a <code>org.jppf.server.node.JPPFNode</code> instance.
   * @param type the type of this event.
   */
  public NodeLifeCycleEvent(final Node node, final NodeLifeCycleEventType type)
  {
    super(node);
    this.type = type;
  }

  /**
   * Initialize this event with the specified job, task class loader and tasks.
   * <br>This constructor is used for <code>jobHeaderLoaded()</code> notifications only.
   * @param node an object representing the JPPF node.
   * @param type the type of this event.
   * @param job the job that is about to be or has been executed.
   * @param cl the class loader used to load the tasks and the classes they need from the client.
   */
  public NodeLifeCycleEvent(final Node node, final NodeLifeCycleEventType type, final JPPFDistributedJob job, final AbstractJPPFClassLoader cl)
  {
    this(node, type);
    this.job = job;
    this.cl = cl;
  }

  /**
   * Initialize this event with the specified job, task class loader and tasks.
   * <br>This constructor is used for <code>jobStarting()</code> and <code>jobEnding()</code> notifications only.
   * @param node an object representing the JPPF node.
   * @param type the type of this event.
   * @param job the job that is about to be or has been executed.
   * @param cl the class loader used to load the tasks and the classes they need from the client.
   * @param tasks the tasks about to be or which have been executed.
   * @param dataProvider the data provider for the current job, if any.
   */
  public NodeLifeCycleEvent(final Node node, final NodeLifeCycleEventType type, final JPPFDistributedJob job, final AbstractJPPFClassLoader cl, final List<Task<?>> tasks, final DataProvider dataProvider)
  {
    this(node, type);
    this.job = job;
    this.cl = cl;
    this.tasks = tasks;
    this.dataProvider = dataProvider;
  }

  /**
   * Get the object representing the current JPPF node.
   * <br>The node is available for all notifications.
   * @return a {@link Node} instance, or null if this event isn't part of a <code>nodeStarting()</code> or <code>nodeEnding()</code> notification.
   * If the {@link NodeLifeCycleListener} was deployed in the server's classpath,
   * then this return value can be safely cast to a <code>org.jppf.server.node.JPPFNode</code> instance.
   */
  public Node getNode()
  {
    return (Node) getSource();
  }

  /**
   * Get the job currently being executed.
   * <br>The job is available within <code>jobHeaderLoaded()</code>, <code>jobStarting()</code> and <code>jobEnding()</code> notifications only.
   * It will be <code>null</code> in all other cases.
   * @return a {@link JPPFDistributedJob} instance, or null if no job is being executed.
   */
  public JPPFDistributedJob getJob()
  {
    return job;
  }

  /**
   * Get the tasks currently being executed.
   * <br>The tasks are available within <code>jobStarting()</code> and <code>jobEnding()</code> notifications only.
   * This method will return <code>null</code> in all other cases.
   * @return a list of {@link Task} instances, or null if the node is idle.
   */
  public List<Task<?>> getTasks()
  {
    return tasks;
  }

  /**
   * Get the class loader used to load the tasks and the classes they need from the client.
   * <br>The task class loader is available within <code>jobHeaderLoaded()</code>, <code>jobStarting()</code> and <code>jobEnding()</code> notifications only.
   * It will be <code>null</code> in all other cases.
   * @return an instance of <code>AbstractJPPFClassLoader</code>.
   */
  public AbstractJPPFClassLoader getTaskClassLoader()
  {
    return cl;
  }

  /**
   * Get the data provider for the current job, if any.
   * <br>The data provider is available within <code>jobStarting()</code> and <code>jobEnding()</code> notifications only.
   * It will be <code>null</code> in all other cases.
   * @return a {@link DataProvider} instance.
   */
  public DataProvider getDataProvider()
  {
    return dataProvider;
  }

  @Override
  public String toString()
  {
    StringBuilder sb = new StringBuilder();
    sb.append(getClass().getSimpleName()).append('[');
    sb.append("type=").append(getType());
    sb.append(", node=").append(getNode());
    sb.append(", job=").append(getJob());
    sb.append(", dataProvider=").append(getDataProvider());
    sb.append(", taskClassLoader=").append(getTaskClassLoader());
    sb.append(", tasks=").append(getTasks());
    sb.append(']');
    return sb.toString();
  }

  /**
   * Get the type of this event.
   * @return the event type as an instance of the typesafe enum {@link NodeLifeCycleEventType}.
   */
  public NodeLifeCycleEventType getType()
  {
    return type;
  }
}
