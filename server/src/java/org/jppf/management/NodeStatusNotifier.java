/*
 * JPPF.
 * Copyright (C) 2005-2016 JPPF Team.
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

import org.jppf.node.event.*;

/**
 * This NodeLifeCycleListener implementations is used to update the node state
 * maintained by the node management MBean.
 * @author Laurent Cohen
 * @exclude
 */
public class NodeStatusNotifier extends DefaultLifeCycleErrorHandler implements NodeLifeCycleListener {
  /**
   * The mbean that provides information on the node's state.
   */
  private final JPPFNodeAdmin nodeAdmin;

  /**
   * Initialize this notifier with the specified node admin mbean.
   * @param nodeAdmin the mbean that provides information on the node's state.
   */
  public NodeStatusNotifier(final JPPFNodeAdmin nodeAdmin) {
    this.nodeAdmin = nodeAdmin;
  }

  @Override
  public void nodeStarting(final NodeLifeCycleEvent event) {
    synchronized (nodeAdmin) {
      nodeAdmin.getNodeState().setConnectionStatus(JPPFNodeState.ConnectionState.CONNECTED);
    }
  }

  @Override
  public void nodeEnding(final NodeLifeCycleEvent event) {
    synchronized (nodeAdmin) {
      nodeAdmin.getNodeState().setConnectionStatus(JPPFNodeState.ConnectionState.DISCONNECTED);
    }
  }

  @Override
  public void jobStarting(final NodeLifeCycleEvent event) {
    synchronized (nodeAdmin) {
      nodeAdmin.getNodeState().setExecutionStatus(JPPFNodeState.ExecutionState.EXECUTING);
    }
  }

  @Override
  public void jobEnding(final NodeLifeCycleEvent event) {
    synchronized (nodeAdmin) {
      nodeAdmin.getNodeState().setExecutionStatus(JPPFNodeState.ExecutionState.IDLE);
      int n = event.getTasks().size();
      n += nodeAdmin.getNodeState().getNbTasksExecuted();
      try {
        nodeAdmin.setTaskCounter(n);
      } catch (Exception e) {
        e.printStackTrace();
      }
    }
  }

  @Override
  public void jobHeaderLoaded(final NodeLifeCycleEvent event) {
  }

  @Override
  public void beforeNextJob(final NodeLifeCycleEvent event) {
  }
}
