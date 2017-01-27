/*
 * JPPF.
 * Copyright (C) 2005-2017 JPPF Team.
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

/**
 * Instances of this class represent the state of a node.
 * They are used as the result of node JMX monitoring request.
 * @author Laurent Cohen
 */
public class JPPFNodeState implements Serializable {
  /**
   * Enumeration of connection states.
   */
  public enum ConnectionState {
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
     * Initialize this enum element with the specified localized display name.
     * @param msg the display name or localize.
     */
    private ConnectionState(final String msg) {
      displayName = msg;
    }

    /**
     * Get the localizable display name.
     * @return the key of a message ot localize.
     */
    public String getDisplayName() {
      return displayName;
    }
  }

  /**
   * Enumeration of execution states.
   */
  public enum ExecutionState {
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
     * Initialize this enum element with the specified localized display name.
     * @param msg the display name ot localize.
     */
    private ExecutionState(final String msg) {
      displayName = msg;
    }

    /**
     * Get the localizable display name.
     * @return the key of a message ot localize.
     */
    public String getDisplayName() {
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
  /**
   * The current pending action for the node.
   */
  private NodePendingAction pendingAction = NodePendingAction.NONE;

  /**
   * Get the number of tasks executed by the node.
   * @return the number of tasks as an int.
   */
  public synchronized int getNbTasksExecuted() {
    return nbTasksExecuted;
  }

  /**
   * Set the number of tasks executed by the node.
   * @param nbTasksExecuted the number of tasks as an int.
   */
  public synchronized void setNbTasksExecuted(final int nbTasksExecuted) {
    this.nbTasksExecuted = nbTasksExecuted;
  }

  /**
   * Get the status of the connection between the node and the server.
   * @return the connection status.
   */
  public synchronized ConnectionState getConnectionStatus() {
    return connectionStatus;
  }

  /**
   * Set the status of the connection between the node and the server.
   * @param connectionStatus the connection status.
   */
  public synchronized void setConnectionStatus(final ConnectionState connectionStatus) {
    this.connectionStatus = connectionStatus;
  }

  /**
   * Get the latest execution status of the node.
   * @return the execution status.
   */
  public synchronized ExecutionState getExecutionStatus() {
    return executionStatus;
  }

  /**
   * Get the latest execution status of the node.
   * @param executionStatus the execution status.
   */
  public synchronized void setExecutionStatus(final ExecutionState executionStatus) {
    this.executionStatus = executionStatus;
  }

  /**
   * Get the size of the node's thread pool.
   * @return the size as an int.
   */
  public int getThreadPoolSize() {
    return threadPoolSize;
  }

  /**
   * Set the size of the node's thread pool.
   * @param threadPoolSize the size as an int.
   */
  public void setThreadPoolSize(final int threadPoolSize) {
    this.threadPoolSize = threadPoolSize;
  }

  /**
   * Get the total cpu time used by the task processing threads.
   * @return the cpu time in milliseconds.
   */
  public synchronized long getCpuTime() {
    return cpuTime;
  }

  /**
   * Set the total cpu time used by the task processing threads.
   * @param cpuTime the cpu time in milliseconds.
   */
  public synchronized void setCpuTime(final long cpuTime) {
    this.cpuTime = cpuTime;
  }

  /**
   * Get the priority of the threads in the pool.
   * @return the priority as an int value.
   */
  public int getThreadPriority() {
    return threadPriority;
  }

  /**
   * Set the priority of the threads in the pool.
   * @param threadPriority the priority as an int value.
   */
  public void setThreadPriority(final int threadPriority) {
    this.threadPriority = threadPriority;
  }


  /**
   * Get the current pending action for the node.
   * @return a {@link NodePendingAction} enum element.
   */
  public synchronized NodePendingAction getPendingAction() {
    return pendingAction;
  }

  /**
   * Set the current pending action for the node.
   * @param pendingAction a {@link NodePendingAction} enum element.
   */
  public synchronized void setPendingAction(final NodePendingAction pendingAction) {
    this.pendingAction = pendingAction != null ? pendingAction : NodePendingAction.NONE;
  }

  /**
   * Make a copy of this node state.
   * @return a <code>JPPFNodeState</code> instance.
   */
  public JPPFNodeState copy() {
    JPPFNodeState s = new JPPFNodeState();
    s.setNbTasksExecuted(getNbTasksExecuted());
    s.setConnectionStatus(getConnectionStatus());
    s.setExecutionStatus(getExecutionStatus());
    s.setThreadPoolSize(getThreadPoolSize());
    s.setThreadPriority(getThreadPriority());
    s.setCpuTime(getCpuTime());
    s.setPendingAction(getPendingAction());
    return s;
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder(getClass().getSimpleName()).append('[');
    sb.append("threadPoolSize=").append(threadPoolSize);
    sb.append(", threadPriority=").append(threadPriority);
    sb.append(", nbTasksExecuted=").append(nbTasksExecuted);
    sb.append(", executionStatus=").append(executionStatus);
    sb.append(", connectionStatus=").append(connectionStatus);
    sb.append(", cpuTime=").append(cpuTime);
    sb.append(", pendingAction=").append(pendingAction);
    sb.append(']');
    return sb.toString();
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + nbTasksExecuted;
    //result = prime * result + (int) (cpuTime ^ (cpuTime >>> 32));
    result = prime * result + ((executionStatus == null) ? 0 : executionStatus.hashCode());
    result = prime * result + ((connectionStatus == null) ? 0 : connectionStatus.hashCode());
    result = prime * result + threadPoolSize;
    result = prime * result + threadPriority;
    result = prime * result + ((pendingAction == null) ? 0 : pendingAction.hashCode());
    return result;
  }

  @Override
  public boolean equals(final Object obj) {
    if (this == obj) return true;
    if (obj == null) return false;
    if (getClass() != obj.getClass()) return false;
    JPPFNodeState other = (JPPFNodeState) obj;
    if (nbTasksExecuted != other.nbTasksExecuted) return false;
    //if (cpuTime != other.cpuTime) return false;
    if (executionStatus != other.executionStatus) return false;
    if (connectionStatus != other.connectionStatus) return false;
    if (threadPoolSize != other.threadPoolSize) return false;
    if (threadPriority != other.threadPriority) return false;
    if (pendingAction != other.pendingAction) return false;
    return true;
  }
}
