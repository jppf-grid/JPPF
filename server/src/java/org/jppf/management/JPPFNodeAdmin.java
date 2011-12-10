/*
 * JPPF.
 * Copyright (C) 2005-2011 JPPF Team.
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

package org.jppf.management;

import java.io.Serializable;
import java.util.Map;

import org.jppf.JPPFNodeReconnectionNotification;
import org.jppf.classloader.*;
import org.jppf.node.NodeRunner;
import org.jppf.server.node.JPPFNode;
import org.jppf.utils.*;
import org.slf4j.*;

/**
 * Management bean for a JPPF node.
 * @author Laurent Cohen
 */
public class JPPFNodeAdmin implements JPPFNodeAdminMBean
{
  /**
   * Explicit serialVersionUID.
   */
  private static final long serialVersionUID = 1L;
  /**
   * Logger for this class.
   */
  private static Logger log = LoggerFactory.getLogger(JPPFNodeAdmin.class);
  /**
   * Determines whether debug log statements are enabled.
   */
  private static boolean debugEnabled = log.isDebugEnabled();
  /**
   * The latest event that occurred within a task.
   */
  private JPPFNodeState nodeState = new JPPFNodeState();
  /**
   * The node whose state is monitored.
   */
  private transient JPPFNode node = null;
  /**
   * Unique id for this mbean.
   */
  private String uuid = new JPPFUuid(JPPFUuid.ALPHA_NUM, 24).toString();

  /**
   * Initialize this node management bean with the specified node.
   * @param node the node whose state is monitored.
   */
  public JPPFNodeAdmin(final JPPFNode node)
  {
    if (debugEnabled) log.debug("instantiating JPPNodeAdmin");
    this.node = node;
    node.setNodeAdmin(this);
    //node.addNodeListener(this);
    nodeState.setThreadPriority(node.getExecutionManager().getThreadsPriority());
    nodeState.setThreadPoolSize(node.getExecutionManager().getThreadPoolSize());
    node.getLifeCycleEventHandler().addNodeLifeCycleListener(new NodeStatusNotifier(this));
  }

  /**
   * Get the latest state information from the node.
   * @return a <code>JPPFNodeState</code> information.
   * @throws Exception if any error occurs.
   * @see org.jppf.management.JPPFNodeAdminMBean#state()
   */
  @Override
  public JPPFNodeState state() throws Exception
  {
    return nodeState.copy();
  }

  /**
   * This method always returns null.
   * @return null.
   * @throws Exception if any error occurs.
   * @see org.jppf.management.JPPFNodeAdminMBean#notification()
   * @deprecated see {@link org.jppf.server.protocol.JPPFTaskListener JPPFTaskListener} for a rationale.
   */
  @Override
  public Serializable notification() throws Exception
  {
    return null;
  }

  /**
   * Cancel the execution of the tasks with the specified id.
   * @param id the id of the tasks to cancel.
   * @throws Exception if any error occurs.
   * @see org.jppf.management.JPPFNodeAdminMBean#cancelTask(java.lang.String)
   * @deprecated the task cancel feature is inherently unsafe, as it depends on the task
   * having a unique id among all the tasks running in the grid, which cannot be guaranteed.
   * This feature has been removed from the management APIs, with no replacement.
   * Tasks can still be cancelled, but only as part of job cancel.
   */
  @Override
  public void cancelTask(final String id) throws Exception
  {
    node.getExecutionManager().cancelTask(id);
  }

  /**
   * Restart the execution of the tasks with the specified id.<br>
   * The task(s) will be restarted even if their execution has already completed.
   * @param id the id of the task or tasks to restart.
   * @throws Exception if any error occurs.
   * @see org.jppf.management.JPPFNodeAdminMBean#restartTask(java.lang.String)
   * @deprecated the task restart feature is inherently unsafe, as it depends on the task
   * having a unique id among all the tasks running in the grid, which cannot be guaranteed.
   * This feature has been removed from the management APIs, with no replacement. 
   */
  @Override
  public void restartTask(final String id) throws Exception
  {
    node.getExecutionManager().restartTask(id);
  }

  /**
   * Set the size of the node's thread pool.
   * @param size the size as an int.
   * @throws Exception if any error occurs.
   * @see org.jppf.management.JPPFNodeAdminMBean#updateThreadPoolSize(java.lang.Integer)
   */
  @Override
  public void updateThreadPoolSize(final Integer size) throws Exception
  {
    node.getExecutionManager().setThreadPoolSize(size);
    nodeState.setThreadPoolSize(size);
  }

  /**
   * Get detailed information about the node's JVM properties, environment variables
   * and runtime information such as memory usage and available processors.
   * @return a <code>JPPFSystemInformation</code> instance.
   * @throws Exception if any error occurs.
   * @see org.jppf.management.JPPFNodeAdminMBean#systemInformation()
   */
  @Override
  public JPPFSystemInformation systemInformation() throws Exception
  {
    JPPFSystemInformation info = new JPPFSystemInformation(NodeRunner.getUuid());
    info.populate();
    info.getRuntime().setProperty("cpuTime", Long.toString(node.getExecutionManager().getCpuTime()));
    return info;
  }

  /**
   * Restart the node.
   * @throws Exception if any error occurs.
   * @see org.jppf.management.JPPFNodeAdminMBean#restart()
   */
  @Override
  public void restart() throws Exception
  {
    node.shutdown(true);
  }

  /**
   * Shutdown the node.
   * @throws Exception if any error occurs.
   * @see org.jppf.management.JPPFNodeAdminMBean#shutdown()
   */
  @Override
  public void shutdown() throws Exception
  {
    node.shutdown(false);
  }

  /**
   * Reset the node's executed tasks counter to zero.
   * @throws Exception if any error occurs.
   * @see org.jppf.management.JPPFNodeAdminMBean#resetTaskCounter()
   */
  @Override
  public void resetTaskCounter() throws Exception
  {
    setTaskCounter(0);
  }

  /**
   * Set the node's executed tasks counter to the specified value.
   * @param n the new value of the task counter.
   * @throws Exception if any error occurs.
   * @see org.jppf.management.JPPFNodeAdminMBean#setTaskCounter(java.lang.Integer)
   */
  @Override
  public synchronized void setTaskCounter(final Integer n) throws Exception
  {
    node.setTaskCount(n);
    nodeState.setNbTasksExecuted(n);
  }

  /**
   * Update the priority of all execution threads.
   * @param newPriority the new priority to set.
   * @throws Exception if any error occurs.
   * @see org.jppf.management.JPPFNodeAdminMBean#updateThreadsPriority(java.lang.Integer)
   */
  @Override
  public void updateThreadsPriority(final Integer newPriority) throws Exception
  {
    node.getExecutionManager().updateThreadsPriority(newPriority);
    nodeState.setThreadPriority(newPriority);
  }

  /**
   * Update the configuration properties of the node.
   * @param config the set of properties to update.
   * @param reconnect - specifies whether the node should reconnect ot the driver after updating the properties.
   * @throws Exception if any error occurs.
   * @see org.jppf.management.JPPFNodeAdminMBean#updateConfiguration(java.util.Map, java.lang.Boolean)
   */
  @Override
  public void updateConfiguration(final Map<Object, Object> config, final Boolean reconnect) throws Exception
  {
    if (config == null) return;
    JPPFConfiguration.getProperties().putAll(config);
    if (reconnect) triggerReconnect();
  }

  /**
   * Trigger a deconnection/reconnection of this node.
   * @throws Exception if any error occurs.
   */
  private void triggerReconnect() throws Exception
  {
    try
    {
      // we close the socket connection in case the node is waiting for data from the server.
      node.getSocketWrapper().close();
    }
    catch(Exception e)
    {
      log.error(e.getMessage(), e);
    }
    node.setExitAction(new Runnable()
    {
      @Override
      public void run()
      {
        throw new JPPFNodeReconnectionNotification("Reconnecting this node due to configuration changes");
      }
    });
    node.setStopped(true);
  }

  /**
   * Cancel the job with the specified id.
   * @param jobId the id of the job to cancel.
   * @param requeue true if the job should be requeued on the server side, false otherwise.
   * @throws Exception if any error occurs.
   * @see org.jppf.management.JPPFNodeAdminMBean#cancelJob(java.lang.String,java.lang.Boolean)
   */
  @Override
  public void cancelJob(final String jobId, final Boolean requeue) throws Exception
  {
    if (debugEnabled) log.debug("Request to cancel jobId = '" + jobId + "', requeue = " + requeue);
    if (jobId == null) return;
    if (jobId.equals(node.getExecutionManager().getCurrentJobId()))
    {
      node.getExecutionManager().cancelAllTasks(true, requeue);
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public DelegationModel getDelegationModel() throws Exception
  {
    return AbstractJPPFClassLoader.getDelegationModel();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void setDelegationModel(final DelegationModel model) throws Exception
  {
    if (model != null) AbstractJPPFClassLoader.setDelegationModel(model);
  }

  /**
   * Get the current state of the node
   * @return a {@link JPPFNodeState} instance.
   */
  synchronized JPPFNodeState getNodeState()
  {
    return nodeState;
  }
}
