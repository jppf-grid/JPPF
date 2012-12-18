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

package org.jppf.management;

import java.io.Serializable;

import org.jppf.utils.LocalizationUtils;

/**
 * Instances of this class represent the state of a node.
 * They are used as the result of node JMX monitoring request.
 * @author Laurent Cohen
 */
public class JPPFNodeState implements Serializable
{
  /**
   * Base name for the localizationr esource bundles.
   */
  private static final String I18N = "org.jppf.server.i18n.messages";

  /**
   * Enumeration of connection states.
   */
  public enum ConnectionState
  {
    /**
     * The state is not yet known.
     */
    UNKNOWN("unknown"),
    /**
     * The node is connected.
     */
    CONNECTED("node.connected"),
    /**
     * The node is disconnected.
     */
    DISCONNECTED("node.disconnected");

    /**
     * The name to display.
     */
    private final String displayName;
    /**
     * Initialize this enum element witht he specified localized display name.
     * @param msg the display name ot localize.
     */
    private ConnectionState(final String msg)
    {
      displayName = LocalizationUtils.getLocalized(I18N, msg);
    }

    @Override
    public String toString()
    {
      return displayName;
    }
  }

  /**
   * Enumeration of execution states.
   */
  public enum ExecutionState
  {
    /**
     * The state is not yet known.
     */
    UNKNOWN("unknown"),
    /**
     * The node is connected.
     */
    IDLE("node.idle"),
    /**
     * The node is disconnected.
     */
    EXECUTING("node.executing");

    /**
     * The name to display.
     */
    private final String displayName;
    /**
     * Initialize this enum element witht he specified localized display name.
     * @param msg the display name ot localize.
     */
    private ExecutionState(final String msg)
    {
      displayName = LocalizationUtils.getLocalized(I18N, msg);
    }

    @Override
    public String toString()
    {
      return displayName;
    }
  }

  /**
   * Explicit serialVersionUID.
   */
  private static final long serialVersionUID = 1L;
  /**
   * Status of the connection between the node and the server.
   */
  private ConnectionState connectionStatus = ConnectionState.UNKNOWN;
  /**
   * Latest execution status of the node.
   */
  private ExecutionState executionStatus = ExecutionState.IDLE;
  /**
   * The number of tasks executed by the node.
   */
  private int nbTasksExecuted = 0;
  /**
   * The total cpu time used by the task processing threads.
   */
  private long cpuTime = 0L;
  /**
   * Size of the node's thread pool.
   */
  private int threadPoolSize = -1;
  /**
   * Priority of the threads in the pool.
   */
  private int threadPriority = -1;
  //private int threadPriority = Thread.NORM_PRIORITY;

  /**
   * This method returns <code>null</code>.
   * @return <code>null</code>.
   * @deprecated see {@link org.jppf.server.protocol.JPPFTaskListener JPPFTaskListener} for a rationale.
   */
  public synchronized Serializable getTaskNotification()
  {
    return null;
  }

  /**
   * This method does nothing.
   * @param taskEvent the event as an object.
   * @deprecated see {@link org.jppf.server.protocol.JPPFTaskListener} for a rationale.
   */
  public synchronized void setTaskEvent(final Serializable taskEvent)
  {
  }

  /**
   * Get the number of tasks executed by the node.
   * @return the number of tasks as an int.
   */
  public synchronized int getNbTasksExecuted()
  {
    return nbTasksExecuted;
  }

  /**
   * Set the number of tasks executed by the node.
   * @param nbTasksExecuted the number of tasks as an int.
   */
  public synchronized void setNbTasksExecuted(final int nbTasksExecuted)
  {
    this.nbTasksExecuted = nbTasksExecuted;
  }

  /**
   * Get the status of the connection between the node and the server.
   * @return the connection status.
   */
  public synchronized ConnectionState getConnectionStatus()
  {
    return connectionStatus;
  }

  /**
   * Set the status of the connection between the node and the server.
   * @param connectionStatus the connection status.
   */
  public synchronized void setConnectionStatus(final ConnectionState connectionStatus)
  {
    this.connectionStatus = connectionStatus;
  }

  /**
   * Get the latest execution status of the node.
   * @return the execution status.
   */
  public synchronized ExecutionState getExecutionStatus()
  {
    return executionStatus;
  }

  /**
   * Get the latest execution status of the node.
   * @param executionStatus the execution status.
   */
  public synchronized void setExecutionStatus(final ExecutionState executionStatus)
  {
    this.executionStatus = executionStatus;
  }

  /**
   * Get the size of the node's thread pool.
   * @return the size as an int.
   */
  public int getThreadPoolSize()
  {
    return threadPoolSize;
  }

  /**
   * Set the size of the node's thread pool.
   * @param threadPoolSize the size as an int.
   */
  public void setThreadPoolSize(final int threadPoolSize)
  {
    this.threadPoolSize = threadPoolSize;
  }

  /**
   * Get the total cpu time used by the task processing threads.
   * @return the cpu time in milliseconds.
   */
  public synchronized long getCpuTime()
  {
    return cpuTime;
  }

  /**
   * Set the total cpu time used by the task processing threads.
   * @param cpuTime the cpu time in milliseconds.
   */
  public synchronized void setCpuTime(final long cpuTime)
  {
    this.cpuTime = cpuTime;
  }

  /**
   * Get the priority of the threads in the pool.
   * @return the priority as an int value.
   */
  public int getThreadPriority()
  {
    return threadPriority;
  }

  /**
   * Set the priority of the threads in the pool.
   * @param threadPriority the priority as an int value.
   */
  public void setThreadPriority(final int threadPriority)
  {
    this.threadPriority = threadPriority;
  }

  /**
   * Make a copy of this node state.
   * @return a <code>JPPFNodeState</code> instance.
   */
  public JPPFNodeState copy()
  {
    JPPFNodeState s = new JPPFNodeState();
    s.setNbTasksExecuted(getNbTasksExecuted());
    s.setConnectionStatus(getConnectionStatus());
    s.setExecutionStatus(getExecutionStatus());
    s.setThreadPoolSize(getThreadPoolSize());
    s.setThreadPriority(getThreadPriority());
    s.setCpuTime(getCpuTime());
    return s;
  }
}
