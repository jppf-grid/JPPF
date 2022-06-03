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

package org.jppf.management.forwarding;

import java.io.Serializable;
import java.util.Map;

import javax.management.NotificationEmitter;

import org.jppf.classloader.DelegationModel;
import org.jppf.management.*;
import org.jppf.management.diagnostics.*;
import org.jppf.management.doc.*;
import org.jppf.utils.*;

/**
 * MBean interface for forwarding node management requests and monitoring notfications via the driver.
 * @author Laurent Cohen
 * @since 6.2
 */
@MBeanDescription("interface for forwarding node management requests and monitoring notfications through a driver")
@MBeanNotif(description = "notifications emitted by this MBean wrap actual notifications received from the nodes, " + 
  "then are forwarded to the JMX notification listeners whose node selector matches the emitting nodes", notifClass = JPPFNodeForwardingNotification.class)
public interface NodeForwardingMBean extends Serializable, NotificationEmitter, ForwardingNotficationEmitter {
  /**
   * Name of the driver's admin MBean.
   */
  String MBEAN_NAME = "org.jppf:name=nodeForwardingEx,type=driver";

  /**
   * Invoke a method on the specified MBean of the selected nodes attached to the driver.
   * @param <E> the type of results.
   * @param selector a filter on the nodes attached tot he driver, determines the nodes to which this method applies.
   * @param mbeanName the name of the MBean.
   * @param methodName the name of the method to invoke.
   * @param params the method parameter values.
   * @param signature the types of the method parameters.
   * @return a mapping of node uuids to the result of invoking the MBean method on the corresponding node. Each result may be an exception.
   * <br/>Additionally, each result may be {@code null}, in particular if the invoked method has a {@code void} return type.
   * @throws Exception if the invocation failed.
   */
  @MBeanDescription("invoke a method on the specified MBean of the selected nodes")
  @MBeanElementType(type = ResultsMap.class, parameters = { "java.lang.String", "E" })
  <E> ResultsMap<String, E> forwardInvoke(@MBeanParamName("nodeSelector") NodeSelector selector, @MBeanParamName("mbeanName") String mbeanName, @MBeanParamName("methodName") String methodName,
    @MBeanParamName("params") Object[] params, @MBeanParamName("signature") String[] signature) throws Exception;

  /**
   * Convenience method to invoke an MBean method that has no parameter.
   * <br/>This is equivalent to calling {@code forwardInvoke(selector, name, methodName, (Object[]) null, (String[]) null)}.
   * @param <E> the type of results.
   * @param selector a filter on the nodes attached to the driver, determines the nodes to which this method applies.
   * @param mbeanName the name of the node MBean to invoke.
   * @param methodName the name of the method to invoke.
   * @return a mapping of node uuids to the result of invoking the MBean method on the corresponding node. Each result may be an exception.
   * <br/>Additionally, each result may be {@code null}, in particular if the invoked method has a {@code void} return type.
   * @throws Exception if the invocation failed.
   */
  @MBeanDescription("invoke an MBean method with no parameter on the selected nodes")
  @MBeanElementType(type = ResultsMap.class, parameters = { "java.lang.String", "E" })
  <E> ResultsMap<String, E> forwardInvoke(@MBeanParamName("nodeSelector") NodeSelector selector, @MBeanParamName("mbeanName") String mbeanName,
    @MBeanParamName("methodName") String methodName) throws Exception;

  /**
   * Get the value of an attribute of the specified MBean for each specified node.
   * @param <E> the type of results.
   * @param selector a filter on the nodes attached tot he driver, determines the nodes to which this method applies.
   * @param mbeanName the name of the MBean to invoke for each node.
   * @param attribute the name of the MBean attribute to read.
   * @return a mapping of node uuids to the result of getting the MBean attribute on the corresponding node. Each result may be an exception.
   * @throws Exception if the invocation failed.
   */
  @MBeanDescription("get the value of an attribute of the specified MBean for each selected node")
  @MBeanElementType(type = ResultsMap.class, parameters = { "java.lang.String", "E" })
  <E> ResultsMap<String, E> forwardGetAttribute(@MBeanParamName("nodeSelector") NodeSelector selector, @MBeanParamName("mbeanName") String mbeanName,
    @MBeanParamName("attribute") String attribute) throws Exception;

  /**
   * Set the value of an attribute of the specified MBean on the specified nodes attached to the driver.
   * @param selector a filter on the nodes attached to the driver, determines the nodes to which this method applies.
   * @param mbeanName the name of the MBean to invoke for each node.
   * @param attribute the name of the MBean attribute to set.
   * @param value the value to set on the attribute.
   * @return a mapping of node uuids to an eventual exception resulting from setting the MBean attribute on the corresponding node.
   * @throws Exception if the invocation failed.
   */
  @MBeanDescription("set the value of an attribute of the specified MBean on the selected nodes")
  @MBeanElementType(type = ResultsMap.class, parameters = { "java.lang.String", "java.lang.Void" })
  ResultsMap<String, Void> forwardSetAttribute(@MBeanParamName("nodeSelector") NodeSelector selector, @MBeanParamName("mbeanName") String mbeanName, @MBeanParamName("attribute") String attribute,
    @MBeanParamName("value") Object value) throws Exception;

  /**
   * Get the latest state information from the node.
   * @param selector a filter on the nodes attached to the driver, determines the nodes to which this method applies.
   * @return a mapping of node uuids to the result of invoking the MBean method on the corresponding node.
   * Each result may be either a {@link org.jppf.management.JPPFNodeState} object, or an exception if the invocation failed for the corresponding node.
   * @throws Exception if any error occurs.
   * @see org.jppf.management.JPPFNodeAdminMBean#state()
   */
  @MBeanDescription("get the latest state information from the selected nodes")
  @MBeanElementType(type = ResultsMap.class, parameters = { "java.lang.String", "org.jppf.management.JPPFNodeState" })
  ResultsMap<String, JPPFNodeState> state(@MBeanParamName("nodeSelector") NodeSelector selector) throws Exception;

  /**
   * Set the size of the specified nodes' thread pool.
   * @param selector a filter on the nodes attached to the driver, determines the nodes to which this method applies.
   * @param size the size as an int.
   * @return a mapping of node uuids to an eventual exception resulting from invoking this method on the corresponding node.
   * @throws Exception if any error occurs.
   * @see org.jppf.management.JPPFNodeAdminMBean#updateThreadPoolSize(Integer)
   */
  @MBeanDescription("set the number of processing threads on the selected nodes")
  @MBeanElementType(type = ResultsMap.class, parameters = { "java.lang.String", "java.lang.Void" })
  ResultsMap<String, Void> updateThreadPoolSize(@MBeanParamName("nodeSelector") NodeSelector selector, @MBeanParamName("size") Integer size) throws Exception;

  /**
   * Update the priority of all execution threads for the specified nodes.
   * @param selector a filter on the nodes attached to the driver, determines the nodes to which this method applies.
   * @param newPriority the new priority to set.
   * @return a mapping of node uuids to an eventual exception resulting from invoking this method on the corresponding node.
   * @throws Exception if an error is raised when invoking the node mbean.
   * @see org.jppf.management.JPPFNodeAdminMBean#updateThreadsPriority(Integer)
   */
  @MBeanDescription("set the priority of the processing threads on the selected nodes")
  @MBeanElementType(type = ResultsMap.class, parameters = { "java.lang.String", "java.lang.Void" })
  ResultsMap<String, Void> updateThreadsPriority(@MBeanParamName("nodeSelector") NodeSelector selector, @MBeanParamName("newPriority") Integer newPriority) throws Exception;

  /**
   * Restart the specified nodes.
   * @param selector a filter on the nodes attached to the driver, determines the nodes to which this method applies.
   * @return a mapping of node uuids to an eventual exception resulting from invoking this method on the corresponding node.
   * @throws Exception if any error occurs.
   * @see org.jppf.management.JPPFNodeAdminMBean#restart()
   */
  @MBeanDescription("restart the selected nodes")
  @MBeanElementType(type = ResultsMap.class, parameters = { "java.lang.String", "java.lang.Void" })
  ResultsMap<String, Void> restart(@MBeanParamName("nodeSelector") NodeSelector selector) throws Exception;

  /**
   * Restart the specified nodes.
   * @param selector a filter on the nodes attached to the driver, determines the nodes to which this method applies.
   * @param interruptIfRunning whether to restart immediately or wait until each node is idle.
   * @return a mapping of node uuids to an eventual exception resulting from invoking this method on the corresponding node.
   * @throws Exception if any error occurs.
   * @see org.jppf.management.JPPFNodeAdminMBean#restart(Boolean)
   */
  @MBeanDescription("restart the selected nodes, specifying whther to wait until they are no longer executing jobs")
  @MBeanElementType(type = ResultsMap.class, parameters = { "java.lang.String", "java.lang.Void" })
  ResultsMap<String, Void> restart(@MBeanParamName("nodeSelector") NodeSelector selector, @MBeanParamName("interruptIfRunning") Boolean interruptIfRunning) throws Exception;

  /**
   * Shutdown the specified nodes.
   * @param selector a filter on the nodes attached to the driver, determines the nodes to which this method applies.
   * @return a mapping of node uuids to an eventual exception resulting from invoking this method on the corresponding node.
   * @throws Exception if any error occurs.
   * @see org.jppf.management.JPPFNodeAdminMBean#shutdown()
   */
  @MBeanDescription("shutdown the selected nodes")
  @MBeanElementType(type = ResultsMap.class, parameters = { "java.lang.String", "java.lang.Void" })
  ResultsMap<String, Void> shutdown(@MBeanParamName("nodeSelector") NodeSelector selector) throws Exception;

  /**
   * Shutdown the specified nodes.
   * @param selector a filter on the nodes attached to the driver, determines the nodes to which this method applies.
   * @param interruptIfRunning whether to shutdown immediately or wait until each node is idle.
   * @return a mapping of node uuids to an eventual exception resulting from invoking this method on the corresponding node.
   * @throws Exception if any error occurs.
   * @see org.jppf.management.JPPFNodeAdminMBean#shutdown(Boolean)
   */
  @MBeanDescription("shutdown the selected nodes, specifying whther to wait until they are no longer executing jobs")
  @MBeanElementType(type = ResultsMap.class, parameters = { "java.lang.String", "java.lang.Void" })
  ResultsMap<String, Void> shutdown(@MBeanParamName("nodeSelector") NodeSelector selector, @MBeanParamName("interruptIfRunning") Boolean interruptIfRunning) throws Exception;

  /**
   * Force the specified nodes to reconnect.
   * @param selector a filter on the nodes attached to the driver, determines the nodes to which this method applies.
   * @param interruptIfRunning whether to reconnect immediately or wait until each node is idle.
   * @return a mapping of node uuids to an eventual exception resulting from invoking this method on the corresponding node.
   * @throws Exception if any error occurs.
   * @see org.jppf.management.JPPFNodeAdminMBean#reconnect(Boolean)
   */
  @MBeanDescription("shutdown the selected nodes, specifying whther to wait until they are no longer executing jobs")
  @MBeanElementType(type = ResultsMap.class, parameters = { "java.lang.String", "java.lang.Void" })
  ResultsMap<String, Void> reconnect(@MBeanParamName("nodeSelector") NodeSelector selector, @MBeanParamName("interruptIfRunning") Boolean interruptIfRunning) throws Exception;

  /**
   * Reset the specified nodes' executed tasks counter to zero.
   * @param selector a filter on the nodes attached to the driver, determines the nodes to which this method applies.
   * @return a mapping of node uuids to an eventual exception resulting from invoking this method on the corresponding node.
   * @throws Exception if any error occurs.
   * @see org.jppf.management.JPPFNodeAdminMBean#resetTaskCounter()
   */
  @MBeanDescription("reset the executed tasks count on the selected nodes to zero")
  @MBeanElementType(type = ResultsMap.class, parameters = { "java.lang.String", "java.lang.Void" })
  ResultsMap<String, Void> resetTaskCounter(@MBeanParamName("nodeSelector") NodeSelector selector) throws Exception;

  /**
   * Reset the specified nodes' executed tasks counter to the specified value.
   * @param selector a filter on the nodes attached to the driver, determines the nodes to which this method applies.
   * @param n the number to set the task counter to.
   * @return a mapping of node uuids to an eventual exception resulting from invoking this method on the corresponding node.
   * @throws Exception if any error occurs.
   * @see org.jppf.management.JPPFNodeAdminMBean#setTaskCounter(Integer)
   */
  @MBeanDescription("set the executed tasks count on the selected nodes to a specified value")
  @MBeanElementType(type = ResultsMap.class, parameters = { "java.lang.String", "java.lang.Void" })
  ResultsMap<String, Void> setTaskCounter(@MBeanParamName("nodeSelector") NodeSelector selector, @MBeanParamName("taskCount") Integer n) throws Exception;

  /**
   * Update the configuration properties of the specified nodes.
   * @param selector a filter on the nodes attached to the driver, determines the nodes to which this method applies.
   * @param configOverrides the set of properties to update.
   * @param restart specifies whether the node should be restarted after updating the properties.
   * @param interruptIfRunning when {@code true}, then restart the node even if it is executing tasks, when {@code false}, then only shutdown the node when it is no longer executing.
   * This parameter only applies when the {@code restart} parameter is {@code true}.
   * @return a mapping of node uuids to an eventual exception resulting from invoking this method on the corresponding node.
   * @throws Exception if any error occurs.
   * @see org.jppf.management.JPPFNodeAdminMBean#updateConfiguration(Map, Boolean, Boolean)
   */
  @MBeanDescription("update the configuration properties of the selected nodes")
  @MBeanElementType(type = ResultsMap.class, parameters = { "java.lang.String", "java.lang.Void" })
  ResultsMap<String, Void> updateConfiguration(@MBeanParamName("nodeSelector") NodeSelector selector, @MBeanParamName("configOverrides") Map<Object, Object> configOverrides,
    @MBeanParamName("restart") Boolean restart, @MBeanParamName("interruptIfRunning") Boolean interruptIfRunning) throws Exception;

  /**
   * Update the configuration properties of the specified nodes.
   * @param selector a filter on the nodes attached to the driver, determines the nodes to which this method applies.
   * @param configOverrides the set of properties to update.
   * @param restart specifies whether the node should be restarted after updating the properties.
   * @return a mapping of node uuids to an eventual exception resulting from invoking this method on the corresponding node.
   * @throws Exception if any error occurs.
   * @see org.jppf.management.JPPFNodeAdminMBean#updateConfiguration(Map, Boolean)
   */
  @MBeanDescription("update the configuration properties of the selected nodes")
  @MBeanElementType(type = ResultsMap.class, parameters = { "java.lang.String", "java.lang.Void" })
  ResultsMap<String, Void> updateConfiguration(@MBeanParamName("nodeSelector") NodeSelector selector, @MBeanParamName("configOverrides") Map<Object, Object> configOverrides,
    @MBeanParamName("restart") Boolean restart) throws Exception;

  /**
   * Cancel the job with the specified id in the specified nodes.
   * @param selector a filter on the nodes attached to the driver, determines the nodes to which this method applies.
   * @param jobId the id of the job to cancel.
   * @param requeue true if the job should be requeued on the server side, false otherwise.
   * @return a mapping of node uuids to an eventual exception resulting invoking this method on the corresponding node.
   * @throws Exception if any error occurs.
   * @see org.jppf.management.JPPFNodeAdminMBean#cancelJob(String, Boolean)
   */
  @MBeanDescription("cancel the job with the specified uuid in the selected nodes")
  @MBeanElementType(type = ResultsMap.class, parameters = { "java.lang.String", "java.lang.Void" })
  ResultsMap<String, Void> cancelJob(@MBeanParamName("nodeSelector") NodeSelector selector, @MBeanParamName("jobUuid") String jobId, @MBeanParamName("requeue") Boolean requeue) throws Exception;

  /**
   * Get the current class loader delegation model for the specified nodes.
   * @param selector a filter on the nodes attached to the driver, determines the nodes to which this method applies.
   * @return a mapping of node uuids to the result of invoking the MBean method on the corresponding node.
   * Each result may be either a {@link DelegationModel} enum value, or an exception if the invocation failed for the corresponding node.
   * @throws Exception if any error occurs.
   * @see org.jppf.classloader.AbstractJPPFClassLoader#getDelegationModel()
   * @see org.jppf.management.JPPFNodeAdminMBean#getDelegationModel()
   */
  @MBeanDescription("get the current class loader delegation model for the selected nodes")
  @MBeanElementType(type = ResultsMap.class, parameters = { "java.lang.String", "org.jppf.classloader.DelegationModel" })
  ResultsMap<String, DelegationModel> getDelegationModel(@MBeanParamName("nodeSelector") NodeSelector selector) throws Exception;

  /**
   * Set the current class loader delegation model for the specified nodes.
   * @param selector a filter on the nodes attached to the driver, determines the nodes to which this method applies.
   * @param model either either {@link DelegationModel#PARENT_FIRST PARENT_FIRST} or {@link DelegationModel#URL_FIRST URL_FIRST}.
   * If any other value is specified then this method has no effect.
   * @return a mapping of node uuids to an eventual exception resulting from invoking this method on the corresponding node.
   * @throws Exception if any error occurs.
   * @see org.jppf.classloader.AbstractJPPFClassLoader#setDelegationModel(org.jppf.classloader.DelegationModel)
   * @see org.jppf.management.JPPFNodeAdminMBean#setDelegationModel(DelegationModel)
   */
  @MBeanDescription("set the class loader delegation model onr the selected nodes")
  @MBeanElementType(type = ResultsMap.class, parameters = { "java.lang.String", "java.lang.Void" })
  ResultsMap<String, Void> setDelegationModel(@MBeanParamName("nodeSelector") NodeSelector selector, @MBeanParamName("delegationModel") DelegationModel model) throws Exception;

  /**
   * Get the system information for the specified nodes.
   * @param selector a filter on the nodes attached to the driver, determines the nodes to which this method applies.
   * @return a mapping of node uuids to an eventual exception resulting from invoking this method on the corresponding node.
   * @throws Exception if any error occurs.
   * @see org.jppf.management.JPPFNodeAdminMBean#systemInformation()
   */
  @MBeanDescription("get the system information for the selected nodes")
  @MBeanElementType(type = ResultsMap.class, parameters = { "java.lang.String", "org.jppf.management.JPPFSystemInformation" })
  ResultsMap<String, JPPFSystemInformation> systemInformation(@MBeanParamName("nodeSelector") NodeSelector selector) throws Exception;

  /**
   * Get the JVM health snapshot for the specified nodes.
   * @param selector a filter on the nodes attached to the driver, determines the nodes to which this method applies.
   * @return a mapping of node uuids to an eventual exception resulting from invoking this method on the corresponding node.
   * @throws Exception if any error occurs.
   * @see org.jppf.management.diagnostics.DiagnosticsMBean#healthSnapshot()
   */
  @MBeanDescription("get a JVM health snapshot for the selected nodes")
  @MBeanElementType(type = ResultsMap.class, parameters = { "java.lang.String", "org.jppf.management.diagnostics.HealthSnapshot" })
  ResultsMap<String, HealthSnapshot> healthSnapshot(@MBeanParamName("nodeSelector") NodeSelector selector) throws Exception;

  /**
   * Invoke {@code System.gc()} on the specified nodes.
   * @param selector a filter on the nodes attached to the driver, determines the nodes to which this method applies.
   * @return a mapping of node uuids to an eventual exception resulting from invoking this method on the corresponding node.
   * @throws Exception if any error occurs.
   * @see org.jppf.management.diagnostics.DiagnosticsMBean#gc()
   */
  @MBeanDescription("invoke System.gc(} on the selected nodes")
  @MBeanElementType(type = ResultsMap.class, parameters = { "java.lang.String", "java.lang.Void" })
  ResultsMap<String, Void> gc(@MBeanParamName("nodeSelector") NodeSelector selector) throws Exception;

  /**
   * Trigger a heap dump on the specified nodes.
   * @param selector a filter on the nodes attached to the driver, determines the nodes to which this method applies.
   * @return a mapping of node uuids to a string holding the path to the heap dump in each node's file system.
   * @throws Exception if any error occurs.
   * @see org.jppf.management.diagnostics.DiagnosticsMBean#heapDump()
   */
  @MBeanDescription("trigger a heap dump on the selected nodes")
  @MBeanElementType(type = ResultsMap.class, parameters = { "java.lang.String", "java.lang.String" })
  ResultsMap<String, String> heapDump(@MBeanParamName("nodeSelector") NodeSelector selector) throws Exception;

  /**
   * Get a JVM thread dump for the specified nodes.
   * @param selector a filter on the nodes attached to the driver, determines the nodes to which this method applies.
   * @return a mapping of node uuids to an eventual exception resulting from invoking this method on the corresponding node.
   * @throws Exception if any error occurs.
   * @see org.jppf.management.diagnostics.DiagnosticsMBean#threadDump()
   */
  @MBeanDescription("get a thread dump for the selected nodes")
  @MBeanElementType(type = ResultsMap.class, parameters = { "java.lang.String", "org.jppf.management.diagnostics.ThreadDump" })
  ResultsMap<String, ThreadDump> threadDump(@MBeanParamName("nodeSelector") NodeSelector selector) throws Exception;

  /**
   * Get the number of provisioned slave nodes for the selected nodes.
   * @param selector a filter on the nodes attached to the driver, determines the nodes to which this method applies.
   * @return a mapping of node uuids to either an {@code int} corresponding to the number of slaves provisioned by the node,
   * or an eventual exception resulting from invoking this method on the corresponding node.
   * @throws Exception if any error occurs.
   * @see org.jppf.node.provisioning.JPPFNodeProvisioningMBean#getNbSlaves()
   */
  @MBeanDescription("get the number of provisioned slave nodes for the selected nodes")
  @MBeanElementType(type = ResultsMap.class, parameters = { "java.lang.String", "java.lang.Integer" })
  ResultsMap<String, Integer> getNbSlaves(@MBeanParamName("nodeSelector") NodeSelector selector) throws Exception;

  /**
   * Start or stop the required number of slaves to reach the specified number on the selected nodes.
   * This is equivalent to calling {@code provisionSlaveNodes(nbNodes, null)}.
   * @param selector a filter on the nodes attached to the driver, determines the nodes to which this method applies.
   * @param nbNodes the number of slave nodes to reach.
   * @return a mapping of node uuids to an eventual exception resulting from invoking this method on the corresponding node.
   * @throws Exception if any error occurs.
   * @see org.jppf.node.provisioning.JPPFNodeProvisioningMBean#provisionSlaveNodes(int)
   */
  @MBeanDescription("make a slave node provisioning request to the selected nodes")
  @MBeanElementType(type = ResultsMap.class, parameters = { "java.lang.String", "java.lang.Void" })
  ResultsMap<String, Void> provisionSlaveNodes(@MBeanParamName("nodeSelector") NodeSelector selector, @MBeanParamName("nbSlaves") int nbNodes) throws Exception;

  /**
   * Start or stop the required number of slaves to reach the specified number on the selected nodes.
   * @param selector a filter on the nodes attached to the driver, determines the nodes to which this method applies.
   * @param nbNodes the number of slave nodes to reach.
   * @param interruptIfRunning if true then nodes can only be stopped once they are idle. 
   * @return a mapping of node uuids to an eventual exception resulting from invoking this method on the corresponding node.
   * @throws Exception if any error occurs.
   * @see org.jppf.node.provisioning.JPPFNodeProvisioningMBean#provisionSlaveNodes(int, boolean)
   */
  @MBeanDescription("make a slave node provisioning request to the selected nodes, specifying whether to wait for slave nodes to be idle before stopping them")
  @MBeanElementType(type = ResultsMap.class, parameters = { "java.lang.String", "java.lang.Void" })
  ResultsMap<String, Void> provisionSlaveNodes(@MBeanParamName("nodeSelector") NodeSelector selector, @MBeanParamName("nbSlaves") int nbNodes,
    @MBeanParamName("interruptIfRunning") boolean interruptIfRunning) throws Exception;

  /**
   * Start or stop the required number of slaves to reach the specified number, using the specified config overrides, on the selected nodes.
   * <p>If {@code configOverrides} is null, then previous overrides are applied, and already running slave nodes do not need to be stopped.
   * @param selector a filter on the nodes attached to the driver, determines the nodes to which this method applies.
   * @param nbNodes the number of slave nodes to reach.
   * @param configOverrides the configuration overrides to apply.
   * @return a mapping of node uuids to an eventual exception resulting from invoking this method on the corresponding node.
   * @throws Exception if any error occurs.
   * @see org.jppf.node.provisioning.JPPFNodeProvisioningMBean#provisionSlaveNodes(int, TypedProperties)
   */
  @MBeanDescription("make a slave node provisioning request to the selected nodes, specifying configuration overrides")
  @MBeanElementType(type = ResultsMap.class, parameters = { "java.lang.String", "java.lang.Void" })
  ResultsMap<String, Void> provisionSlaveNodes(@MBeanParamName("nodeSelector") NodeSelector selector, @MBeanParamName("slaves") int nbNodes,
    @MBeanParamName("cfgOverrides") TypedProperties configOverrides) throws Exception;

  /**
   * Start or stop the required number of slaves to reach the specified number, using the specified config overrides, on the selected nodes.
   * <p>If {@code configOverrides} is null, then previous overrides are applied, and already running slave nodes do not need to be stopped.
   * @param selector a filter on the nodes attached to the driver, determines the nodes to which this method applies.
   * @param nbNodes the number of slave nodes to reach.
   * @param interruptIfRunning if true then nodes can only be stopped once they are idle. 
   * @param configOverrides the configuration overrides to apply.
   * @return a mapping of node uuids to an eventual exception resulting from invoking this method on the corresponding node.
   * @throws Exception if any error occurs.
   * @see org.jppf.node.provisioning.JPPFNodeProvisioningMBean#provisionSlaveNodes(int, boolean, TypedProperties)
   */
  @MBeanDescription("make a slave node provisioning request to the selected nodes, specifying configuration overrides")
  @MBeanElementType(type = ResultsMap.class, parameters = { "java.lang.String", "java.lang.Void" })
  ResultsMap<String, Void> provisionSlaveNodes(@MBeanParamName("nodeSelector") NodeSelector selector, @MBeanParamName("nbSlaves") int nbNodes,
    @MBeanParamName("interruptIfRunning") boolean interruptIfRunning, @MBeanParamName("configOverrides") TypedProperties configOverrides) throws Exception;
}
