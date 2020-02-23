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

import java.util.Map;
import org.jppf.classloader.DelegationModel;
import org.jppf.management.JMXDriverConnectionWrapper;
import org.jppf.management.JPPFNodeAdminMBean;
import org.jppf.management.JPPFNodeState;
import org.jppf.management.JPPFSystemInformation;
import org.jppf.management.NodePendingAction;
import org.jppf.management.NodeSelector;
import org.jppf.management.forwarding.AbstractMBeanForwarder;
import org.jppf.utils.ResultsMap;

/**
 * Forwarding proxy for the {@link JPPFNodeAdminMBean} MBean.
 * MBean description: management and monitoring of a JPPF node.
 * @since 6.2
 */
public class JPPFNodeAdminMBeanForwarder extends AbstractMBeanForwarder {
  /**
   * Initialize this proxy.
   * @param jmx a {@link JMXDriverConnectionWrapper} instance.
   * @throws Exception if any error occurs..
   */
  public JPPFNodeAdminMBeanForwarder(final JMXDriverConnectionWrapper jmx) throws Exception {
    super(jmx, "org.jppf:name=admin,type=node");
  }

  /**
   * Get the value of the {@code DelegationModel} attribute for all selected nodes (the current class loader delegation model for the node).
   * @param selector a {@link NodeSelector} instance.
   * @return a mapping of node uuids to {@link DelegationModel} instances.
   * @throws Exception if any error occurs.
   */
  public ResultsMap<String, DelegationModel> getDelegationModel(final NodeSelector selector) throws Exception {
    return getAttribute(selector, "DelegationModel");
  }

  /**
   * Set the value of the {@code DelegationModel} attribute on all selected nodes (the current class loader delegation model for the node).
   * @param selector a {@link NodeSelector} instance.
   * @param value the value to set, a {@link DelegationModel} instance.
   * @return a mapping of node uuids to invocation results which may either be null or an exception.
   * @throws Exception if any error occurs.
   */
  public ResultsMap<String, Void> setDelegationModel(final NodeSelector selector, final DelegationModel value) throws Exception {
    return setAttribute(selector, "DelegationModel", value);
  }

  /**
   * Set the value of the {@code TaskCounter} attribute on all selected nodes (reset the node's executed tasks counter to the specified value).
   * @param selector a {@link NodeSelector} instance.
   * @param value the value to set, a {@link Integer} instance.
   * @return a mapping of node uuids to invocation results which may either be null or an exception.
   * @throws Exception if any error occurs.
   */
  public ResultsMap<String, Void> setTaskCounter(final NodeSelector selector, final Integer value) throws Exception {
    return setAttribute(selector, "TaskCounter", value);
  }

  /**
   * Invoke the {@code shutdown} operation for all selected nodes (shutdown the node unconditionally).
   * @param selector a {@link NodeSelector} instance.
   * @return a mapping of node uuids to objects that wrap either {@code null} or an exeption.
   * @throws Exception if any error occurs.
   */
  public ResultsMap<String, Void> shutdown(final NodeSelector selector) throws Exception {
    return invoke(selector, "shutdown");
  }

  /**
   * Invoke the {@code shutdown} operation for all selected nodes (shutdown the node, specifying whether to wait for executing tasks to complete).
   * @param selector a {@link NodeSelector} instance.
   * @param interruptIfRunning a {@link Boolean} instance.
   * @return a mapping of node uuids to objects that wrap either {@code null} or an exeption.
   * @throws Exception if any error occurs.
   */
  public ResultsMap<String, Void> shutdown(final NodeSelector selector, final Boolean interruptIfRunning) throws Exception {
    return invoke(selector, "shutdown", new Object[] { interruptIfRunning }, new String[] { Boolean.class.getName() });
  }

  /**
   * Invoke the {@code state} operation for all selected nodes (get the latest state information from the node).
   * @param selector a {@link NodeSelector} instance.
   * @return a mapping of node uuids to objects that wrap either a [@link JPPFNodeState} or an exeption.
   * @throws Exception if any error occurs.
   */
  public ResultsMap<String, JPPFNodeState> state(final NodeSelector selector) throws Exception {
    return invoke(selector, "state");
  }

  /**
   * Invoke the {@code pendingAction} operation for all selected nodes (determine wether a deffered shutdwon or restartd was requested and not yet performed for the node).
   * @param selector a {@link NodeSelector} instance.
   * @return a mapping of node uuids to objects that wrap either a [@link NodePendingAction} or an exeption.
   * @throws Exception if any error occurs.
   */
  public ResultsMap<String, NodePendingAction> pendingAction(final NodeSelector selector) throws Exception {
    return invoke(selector, "pendingAction");
  }

  /**
   * Invoke the {@code restart} operation for all selected nodes (restart the node, specifying whether to wait for executing tasks to complete).
   * @param selector a {@link NodeSelector} instance.
   * @param interruptIfRunning a {@link Boolean} instance.
   * @return a mapping of node uuids to objects that wrap either {@code null} or an exeption.
   * @throws Exception if any error occurs.
   */
  public ResultsMap<String, Void> restart(final NodeSelector selector, final Boolean interruptIfRunning) throws Exception {
    return invoke(selector, "restart", new Object[] { interruptIfRunning }, new String[] { Boolean.class.getName() });
  }

  /**
   * Invoke the {@code restart} operation for all selected nodes (restart the node unconditionally).
   * @param selector a {@link NodeSelector} instance.
   * @return a mapping of node uuids to objects that wrap either {@code null} or an exeption.
   * @throws Exception if any error occurs.
   */
  public ResultsMap<String, Void> restart(final NodeSelector selector) throws Exception {
    return invoke(selector, "restart");
  }

  /**
   * Invoke the {@code cancelJob} operation for all selected nodes (ancel the job with the specified uuid).
   * @param selector a {@link NodeSelector} instance.
   * @param jobUuid a {@link String} instance.
   * @param requeue a {@link Boolean} instance.
   * @return a mapping of node uuids to objects that wrap either {@code null} or an exeption.
   * @throws Exception if any error occurs.
   */
  public ResultsMap<String, Void> cancelJob(final NodeSelector selector, final String jobUuid, final Boolean requeue) throws Exception {
    return invoke(selector, "cancelJob", new Object[] { jobUuid, requeue }, new String[] { String.class.getName(), Boolean.class.getName() });
  }

  /**
   * Invoke the {@code cancelPendingAction} operation for all selected nodes (cancel a previous deferred shutdown or restart request, if any).
   * @param selector a {@link NodeSelector} instance.
   * @return a mapping of node uuids to objects that wrap either a [@link Boolean} or an exeption.
   * @throws Exception if any error occurs.
   */
  public ResultsMap<String, Boolean> cancelPendingAction(final NodeSelector selector) throws Exception {
    return invoke(selector, "cancelPendingAction");
  }

  /**
   * Invoke the {@code updateThreadsPriority} operation for all selected nodes (update the priority of all processing threads).
   * @param selector a {@link NodeSelector} instance.
   * @param newPriority a {@link Integer} instance.
   * @return a mapping of node uuids to objects that wrap either {@code null} or an exeption.
   * @throws Exception if any error occurs.
   */
  public ResultsMap<String, Void> updateThreadsPriority(final NodeSelector selector, final Integer newPriority) throws Exception {
    return invoke(selector, "updateThreadsPriority", new Object[] { newPriority }, new String[] { Integer.class.getName() });
  }

  /**
   * Invoke the {@code reconnect} operation for all selected nodes (force the node to reconnect without restarting, specifying whether to wait for executing tasks to complete).
   * @param selector a {@link NodeSelector} instance.
   * @param interruptIfRunning a {@link Boolean} instance.
   * @return a mapping of node uuids to objects that wrap either {@code null} or an exeption.
   * @throws Exception if any error occurs.
   */
  public ResultsMap<String, Void> reconnect(final NodeSelector selector, final Boolean interruptIfRunning) throws Exception {
    return invoke(selector, "reconnect", new Object[] { interruptIfRunning }, new String[] { Boolean.class.getName() });
  }

  /**
   * Invoke the {@code resetTaskCounter} operation for all selected nodes (reset the node's executed tasks counter to zero).
   * @param selector a {@link NodeSelector} instance.
   * @return a mapping of node uuids to objects that wrap either {@code null} or an exeption.
   * @throws Exception if any error occurs.
   */
  public ResultsMap<String, Void> resetTaskCounter(final NodeSelector selector) throws Exception {
    return invoke(selector, "resetTaskCounter");
  }

  /**
   * Invoke the {@code updateThreadPoolSize} operation for all selected nodes (set the size of the node's thread pool).
   * @param selector a {@link NodeSelector} instance.
   * @param poolSize a {@link Integer} instance.
   * @return a mapping of node uuids to objects that wrap either {@code null} or an exeption.
   * @throws Exception if any error occurs.
   */
  public ResultsMap<String, Void> updateThreadPoolSize(final NodeSelector selector, final Integer poolSize) throws Exception {
    return invoke(selector, "updateThreadPoolSize", new Object[] { poolSize }, new String[] { Integer.class.getName() });
  }

  /**
   * Invoke the {@code updateConfiguration} operation for all selected nodes (update the configuration properties of the node).
   * @param selector a {@link NodeSelector} instance.
   * @param cfgUpdates a {@link Map} instance.
   * @param restartNode a {@link Boolean} instance.
   * @param interruptIfRunning a {@link Boolean} instance.
   * @return a mapping of node uuids to objects that wrap either {@code null} or an exeption.
   * @throws Exception if any error occurs.
   */
  public ResultsMap<String, Void> updateConfiguration(final NodeSelector selector, final Map<Object, Object> cfgUpdates, final Boolean restartNode, final Boolean interruptIfRunning) throws Exception {
    return invoke(selector, "updateConfiguration", new Object[] { cfgUpdates, restartNode, interruptIfRunning }, new String[] { Map.class.getName(), Boolean.class.getName(), Boolean.class.getName() });
  }

  /**
   * Invoke the {@code updateConfiguration} operation for all selected nodes (update the configuration properties of the node).
   * @param selector a {@link NodeSelector} instance.
   * @param configUpdates a {@link Map} instance.
   * @param restartNode a {@link Boolean} instance.
   * @return a mapping of node uuids to objects that wrap either {@code null} or an exeption.
   * @throws Exception if any error occurs.
   */
  public ResultsMap<String, Void> updateConfiguration(final NodeSelector selector, final Map<Object, Object> configUpdates, final Boolean restartNode) throws Exception {
    return invoke(selector, "updateConfiguration", new Object[] { configUpdates, restartNode }, new String[] { Map.class.getName(), Boolean.class.getName() });
  }

  /**
   * Invoke the {@code systemInformation} operation for all selected nodes (get detailed information on the system where the JPPF server or node is runnning: environement variables, JVM system properties, JPPF configuration, runtime information, storage details, network interfaces, statistics).
   * @param selector a {@link NodeSelector} instance.
   * @return a mapping of node uuids to objects that wrap either a [@link JPPFSystemInformation} or an exeption.
   * @throws Exception if any error occurs.
   */
  public ResultsMap<String, JPPFSystemInformation> systemInformation(final NodeSelector selector) throws Exception {
    return invoke(selector, "systemInformation");
  }
}
