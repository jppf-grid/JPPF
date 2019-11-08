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

package org.jppf.management.forwarding.generated;

import org.jppf.management.JMXDriverConnectionWrapper;
import org.jppf.management.JPPFNodeTaskMonitorMBean;
import org.jppf.management.NodeSelector;
import org.jppf.management.TaskExecutionNotification;
import org.jppf.management.forwarding.AbstractNodeForwardingProxy;
import org.jppf.utils.ResultsMap;

/**
 * Forwarding proxy for the {@link JPPFNodeTaskMonitorMBean} MBean.
 * MBean description: monitoring of the tasks processing in a node.
 * <p>This Mbean emits notification of type {@link TaskExecutionNotification}:
 * <br>- notifications sent after or during the execution of each task.
 * <br>- user data: any user-defined data that is sent along with the notification.
 * <br>- user data type: any type.
 * @since 6.2
 */
public class JPPFNodeTaskMonitorMBeanForwarder extends AbstractNodeForwardingProxy {
  /**
   * Initialize this proxy.
   * @param jmx a {@link JMXDriverConnectionWrapper} instance.
   * @throws Exception if any error occurs..
   */
  public JPPFNodeTaskMonitorMBeanForwarder(final JMXDriverConnectionWrapper jmx) throws Exception {
    super(jmx, "org.jppf:name=task.monitor,type=node");
  }

  /**
   * Get the value of the {@code TotalTasksExecuted} attribute for all selected nodes (the total number of tasks executed by the node).
   * @param selector a {@link NodeSelector} instance.
   * @return a mapping of node uuids to {@link Integer} instances.
   * @throws Exception if any error occurs.
   */
  public ResultsMap<String, Integer> getTotalTasksExecuted(final NodeSelector selector) throws Exception {
    return getAttribute(selector, "TotalTasksExecuted");
  }

  /**
   * Get the value of the {@code TotalTasksInError} attribute for all selected nodes (the total number of tasks that ended in error).
   * @param selector a {@link NodeSelector} instance.
   * @return a mapping of node uuids to {@link Integer} instances.
   * @throws Exception if any error occurs.
   */
  public ResultsMap<String, Integer> getTotalTasksInError(final NodeSelector selector) throws Exception {
    return getAttribute(selector, "TotalTasksInError");
  }

  /**
   * Get the value of the {@code TotalTaskElapsedTime} attribute for all selected nodes (the total elapsed time used by the tasks in milliseconds).
   * @param selector a {@link NodeSelector} instance.
   * @return a mapping of node uuids to {@link Long} instances.
   * @throws Exception if any error occurs.
   */
  public ResultsMap<String, Long> getTotalTaskElapsedTime(final NodeSelector selector) throws Exception {
    return getAttribute(selector, "TotalTaskElapsedTime");
  }

  /**
   * Get the value of the {@code TotalTasksSucessfull} attribute for all selected nodes (the total number of tasks that executed successfully).
   * @param selector a {@link NodeSelector} instance.
   * @return a mapping of node uuids to {@link Integer} instances.
   * @throws Exception if any error occurs.
   */
  public ResultsMap<String, Integer> getTotalTasksSucessfull(final NodeSelector selector) throws Exception {
    return getAttribute(selector, "TotalTasksSucessfull");
  }

  /**
   * Get the value of the {@code TotalTaskCpuTime} attribute for all selected nodes (the total cpu time used by the tasks in milliseconds).
   * @param selector a {@link NodeSelector} instance.
   * @return a mapping of node uuids to {@link Long} instances.
   * @throws Exception if any error occurs.
   */
  public ResultsMap<String, Long> getTotalTaskCpuTime(final NodeSelector selector) throws Exception {
    return getAttribute(selector, "TotalTaskCpuTime");
  }

  /**
   * Invoke the {@code reset} operation for all selected nodes (reset the statistics maintained by this MBean).
   * @param selector a {@link NodeSelector} instance.
   * @return a mapping of node uuids to objects that wrap either {@code null} or an exeption.
   * @throws Exception if any error occurs.
   */
  public ResultsMap<String, Void> reset(final NodeSelector selector) throws Exception {
    return invoke(selector, "reset");
  }
}
