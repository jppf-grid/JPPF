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

import javax.management.*;

import org.jppf.classloader.DelegationModel;
import org.jppf.management.NodeSelector;
import org.jppf.management.doc.*;
import org.jppf.utils.TypedProperties;

/**
 * MBean interface for forwarding node management requests and monitoring notfications via the driver.
 * @author Laurent Cohen
 */
@MBeanDescription("interface for forwarding node management requests and monitoring notfications through a driver")
@MBeanNotif(description = "notifications emitted by this MBean wrap actual notifications received from the nodes, " + 
  "then are forwarded to the JMX notification listeners whose node selector matches the emitting nodes", notifClass = JPPFNodeForwardingNotification.class)
public interface JPPFNodeForwardingMBean extends Serializable, NotificationEmitter {
  /**
   * Name of the driver's admin MBean.
   */
  String MBEAN_NAME = "org.jppf:name=nodeForwarding,type=driver";

  /**
   * Invoke a method on the specified MBean of the selected nodes attached to the driver.
   * @param selector a filter on the nodes attached tot he driver, determines the nodes to which this method applies.
   * @param name the name of the MBean.
   * @param methodName the name of the method to invoke.
   * @param params the method parameter values.
   * @param signature the types of the method parameters.
   * @return a mapping of node uuids to the result of invoking the MBean method on the corresponding node. Each result may be an exception.
   * <br/>Additionally, each result may be {@code null}, in particular if the invoked method has a {@code void} return type.
   * @throws Exception if the invocation failed.
   */
  @MBeanDescription("invoke a method on the specified MBean of the selected nodes")
  Map<String, Object> forwardInvoke(@MBeanParamName("nodeSelector") NodeSelector selector, @MBeanParamName("mbeanName") String name, @MBeanParamName("methodName") String methodName,
    @MBeanParamName("params") Object[] params, @MBeanParamName("signature") String[] signature) throws Exception;

  /**
   * Convenience method to invoke an MBean method that has no parameter.
   * <br/>This is equivalent to calling {@code forwardInvoke(selector, name, methodName, (Object[]) null, (String[]) null)}.
   * @param selector a filter on the nodes attached to the driver, determines the nodes to which this method applies.
   * @param name the name of the node MBean to invoke.
   * @param methodName the name of the method to invoke.
   * @return a mapping of node uuids to the result of invoking the MBean method on the corresponding node. Each result may be an exception.
   * <br/>Additionally, each result may be {@code null}, in particular if the invoked method has a {@code void} return type.
   * @throws Exception if the invocation failed.
   */
  @MBeanDescription("invoke an MBean method with no parameter on the selected nodes")
  Map<String, Object> forwardInvoke(@MBeanParamName("nodeSelector") NodeSelector selector, @MBeanParamName("mbeanName") String name, @MBeanParamName("methodName") String methodName) throws Exception;

  /**
   * Get the value of an attribute of the specified MBean for each specified node.
   * @param selector a filter on the nodes attached tot he driver, determines the nodes to which this method applies.
   * @param name the name of the MBean to invoke for each node.
   * @param attribute the name of the MBean attribute to read.
   * @return a mapping of node uuids to the result of getting the MBean attribute on the corresponding node. Each result may be an exception.
   * @throws Exception if the invocation failed.
   */
  @MBeanDescription("get the value of an attribute of the specified MBean for each selected node")
  Map<String, Object> forwardGetAttribute(@MBeanParamName("nodeSelector") NodeSelector selector, @MBeanParamName("mbeanName") String name, @MBeanParamName("attribute") String attribute) throws Exception;

  /**
   * Set the value of an attribute of the specified MBean on the specified nodes attached to the driver.
   * @param selector a filter on the nodes attached to the driver, determines the nodes to which this method applies.
   * @param name the name of the MBean to invoke for each node.
   * @param attribute the name of the MBean attribute to set.
   * @param value the value to set on the attribute.
   * @return a mapping of node uuids to an eventual exception resulting from setting the MBean attribute on the corresponding node.
   * This map may be empty if no exception was raised.
   * @throws Exception if the invocation failed.
   */
  @MBeanDescription("set the value of an attribute of the specified MBean on the selected nodes")
  Map<String, Object> forwardSetAttribute(@MBeanParamName("nodeSelector") NodeSelector selector, @MBeanParamName("mbeanName") String name, @MBeanParamName("attribute") String attribute,
    @MBeanParamName("value") Object value) throws Exception;

  /**
   * Get the latest state information from the node.
   * @param selector a filter on the nodes attached to the driver, determines the nodes to which this method applies.
   * @return a mapping of node uuids to the result of invoking the MBean method on the corresponding node.
   * Each result may be either a {@link org.jppf.management.JPPFNodeState} object, or an exception if the invocation failed for the corresponding node.
   * @throws Exception if any error occurs.
   */
  @MBeanDescription("get the latest state information from the selected nodes")
  Map<String, Object> state(@MBeanParamName("nodeSelector") NodeSelector selector) throws Exception;

  /**
   * Set the size of the specified nodes' thread pool.
   * @param selector a filter on the nodes attached to the driver, determines the nodes to which this method applies.
   * @param size the size as an int.
   * @return a mapping of node uuids to an eventual exception resulting from invoking this method on the corresponding node.
   * This map may be empty if no exception was raised.
   * @throws Exception if any error occurs.
   */
  @MBeanDescription("set the number of processing threads on the selected nodes")
  Map<String, Object> updateThreadPoolSize(@MBeanParamName("nodeSelector") NodeSelector selector, @MBeanParamName("size") Integer size) throws Exception;

  /**
   * Update the priority of all execution threads for the specified nodes.
   * @param selector a filter on the nodes attached to the driver, determines the nodes to which this method applies.
   * @param newPriority the new priority to set.
   * @return a mapping of node uuids to an eventual exception resulting from invoking this method on the corresponding node.
   * This map may be empty if no exception was raised.
   * @throws Exception if an error is raised when invoking the node mbean.
   */
  @MBeanDescription("set the priority of the processing threads on the selected nodes")
  Map<String, Object> updateThreadsPriority(@MBeanParamName("nodeSelector") NodeSelector selector, @MBeanParamName("newPriority") Integer newPriority) throws Exception;

  /**
   * Restart the specified nodes.
   * @param selector a filter on the nodes attached to the driver, determines the nodes to which this method applies.
   * @return a mapping of node uuids to an eventual exception resulting from invoking this method on the corresponding node.
   * This map may be empty if no exception was raised.
   * @throws Exception if any error occurs.
   */
  @MBeanDescription("restart the selected nodes")
  Map<String, Object> restart(@MBeanParamName("nodeSelector") NodeSelector selector) throws Exception;

  /**
   * Restart the specified nodes.
   * @param selector a filter on the nodes attached to the driver, determines the nodes to which this method applies.
   * @param interruptIfRunning whether to restart immediately or wait until each node is idle.
   * @return a mapping of node uuids to an eventual exception resulting from invoking this method on the corresponding node.
   * This map may be empty if no exception was raised.
   * @throws Exception if any error occurs.
   */
  @MBeanDescription("restart the selected nodes, specifying whther to wait until they are no longer executing jobs")
  Map<String, Object> restart(@MBeanParamName("nodeSelector") NodeSelector selector, @MBeanParamName("interruptIfRunning") Boolean interruptIfRunning) throws Exception;

  /**
   * Shutdown the specified nodes.
   * @param selector a filter on the nodes attached to the driver, determines the nodes to which this method applies.
   * @return a mapping of node uuids to an eventual exception resulting from invoking this method on the corresponding node.
   * This map may be empty if no exception was raised.
   * @throws Exception if any error occurs.
   */
  @MBeanDescription("shutdown the selected nodes")
  Map<String, Object> shutdown(@MBeanParamName("nodeSelector") NodeSelector selector) throws Exception;

  /**
   * Shutdown the specified nodes.
   * @param selector a filter on the nodes attached to the driver, determines the nodes to which this method applies.
   * @param interruptIfRunning whether to shutdown immediately or wait until each node is idle.
   * @return a mapping of node uuids to an eventual exception resulting from invoking this method on the corresponding node.
   * This map may be empty if no exception was raised.
   * @throws Exception if any error occurs.
   */
  @MBeanDescription("shutdown the selected nodes, specifying whther to wait until they are no longer executing jobs")
  Map<String, Object> shutdown(@MBeanParamName("nodeSelector") NodeSelector selector, @MBeanParamName("interruptIfRunning") Boolean interruptIfRunning) throws Exception;

  /**
   * Reset the specified nodes' executed tasks counter to zero.
   * @param selector a filter on the nodes attached to the driver, determines the nodes to which this method applies.
   * @return a mapping of node uuids to an eventual exception resulting from invoking this method on the corresponding node.
   * This map may be empty if no exception was raised.
   * @throws Exception if any error occurs.
   */
  @MBeanDescription("reset the executed tasks count on the selected nodes to zero")
  Map<String, Object> resetTaskCounter(@MBeanParamName("nodeSelector") NodeSelector selector) throws Exception;

  /**
   * Reset the specified nodes' executed tasks counter to the specified value.
   * @param selector a filter on the nodes attached to the driver, determines the nodes to which this method applies.
   * @param n the number to set the task counter to.
   * @return a mapping of node uuids to an eventual exception resulting from invoking this method on the corresponding node.
   * This map may be empty if no exception was raised.
   * @throws Exception if any error occurs.
   */
  @MBeanDescription("set the executed tasks count on the selected nodes to a specified value")
  Map<String, Object> setTaskCounter(@MBeanParamName("nodeSelector") NodeSelector selector, @MBeanParamName("taskCount") Integer n) throws Exception;

  /**
   * Update the configuration properties of the specified nodes.
   * @param selector a filter on the nodes attached to the driver, determines the nodes to which this method applies.
   * @param configOverrides the set of properties to update.
   * @param restart specifies whether the node should be restarted after updating the properties.
   * @param interruptIfRunning when {@code true}, then restart the node even if it is executing tasks, when {@code false}, then only shutdown the node when it is no longer executing.
   * This parameter only applies when the {@code restart} parameter is {@code true}.
   * @return a mapping of node uuids to an eventual exception resulting from invoking this method on the corresponding node.
   * This map may be empty if no exception was raised.
   * @throws Exception if any error occurs.
   * @since 5.2
   */
  @MBeanDescription("update the configuration properties of the selected nodes")
  Map<String, Object> updateConfiguration(@MBeanParamName("nodeSelector") NodeSelector selector, @MBeanParamName("configOverrides") Map<Object, Object> configOverrides,
    @MBeanParamName("restart") Boolean restart, @MBeanParamName("interruptIfRunning") Boolean interruptIfRunning) throws Exception;

  /**
   * Update the configuration properties of the specified nodes.
   * @param selector a filter on the nodes attached to the driver, determines the nodes to which this method applies.
   * @param configOverrides the set of properties to update.
   * @param restart specifies whether the node should be restarted after updating the properties.
   * @return a mapping of node uuids to an eventual exception resulting from invoking this method on the corresponding node.
   * This map may be empty if no exception was raised.
   * @throws Exception if any error occurs.
   */
  @MBeanDescription("update the configuration properties of the selected nodes")
  Map<String, Object> updateConfiguration(@MBeanParamName("nodeSelector") NodeSelector selector, @MBeanParamName("configOverrides") Map<Object, Object> configOverrides,
    @MBeanParamName("restart") Boolean restart) throws Exception;

  /**
   * Cancel the job with the specified id in the specified nodes.
   * @param selector a filter on the nodes attached to the driver, determines the nodes to which this method applies.
   * @param jobId the id of the job to cancel.
   * @param requeue true if the job should be requeued on the server side, false otherwise.
   * @return a mapping of node uuids to an eventual exception resulting invoking this method on the corresponding node.
   * This map may be empty if no exception was raised.
   * @throws Exception if any error occurs.
   */
  @MBeanDescription("cancel the job with the specified uuid in the selected nodes")
  Map<String, Object> cancelJob(@MBeanParamName("nodeSelector") NodeSelector selector, @MBeanParamName("jobUuid") String jobId, @MBeanParamName("requeue") Boolean requeue) throws Exception;

  /**
   * Get the current class loader delegation model for the specified nodes.
   * @param selector a filter on the nodes attached to the driver, determines the nodes to which this method applies.
   * @return a mapping of node uuids to the result of invoking the MBean method on the corresponding node.
   * Each result may be either a {@link DelegationModel} enum value, or an exception if the invocation failed for the corresponding node.
   * @throws Exception if any error occurs.
   * @see org.jppf.classloader.AbstractJPPFClassLoader#getDelegationModel()
   */
  @MBeanDescription("get the current class loader delegation model for the selected nodes")
  Map<String, Object> getDelegationModel(@MBeanParamName("nodeSelector") NodeSelector selector) throws Exception;

  /**
   * Set the current class loader delegation model for the specified nodes.
   * @param selector a filter on the nodes attached to the driver, determines the nodes to which this method applies.
   * @param model either either {@link DelegationModel#PARENT_FIRST PARENT_FIRST} or {@link DelegationModel#URL_FIRST URL_FIRST}.
   * If any other value is specified then this method has no effect.
   * @return a mapping of node uuids to an eventual exception resulting from invoking this method on the corresponding node.
   * This map may be empty if no exception was raised.
   * @throws Exception if any error occurs.
   * @see org.jppf.classloader.AbstractJPPFClassLoader#setDelegationModel(org.jppf.classloader.DelegationModel)
   */
  @MBeanDescription("set the class loader delegation model onr the selected nodes")
  Map<String, Object> setDelegationModel(@MBeanParamName("nodeSelector") NodeSelector selector, @MBeanParamName("delegationModel") DelegationModel model) throws Exception;

  /**
   * Get the system information for the specified nodes.
   * @param selector a filter on the nodes attached to the driver, determines the nodes to which this method applies.
   * @return a mapping of node uuids to an eventual exception resulting from invoking this method on the corresponding node.
   * This map may be empty if no exception was raised.
   * @throws Exception if any error occurs.
   */
  @MBeanDescription("get the system information for the selected nodes")
  Map<String, Object> systemInformation(@MBeanParamName("nodeSelector") NodeSelector selector) throws Exception;

  /**
   * Get the JVM health snapshot for the specified nodes.
   * @param selector a filter on the nodes attached to the driver, determines the nodes to which this method applies.
   * @return a mapping of node uuids to an eventual exception resulting from invoking this method on the corresponding node.
   * This map may be empty if no exception was raised.
   * @throws Exception if any error occurs.
   */
  @MBeanDescription("get a JVM health snapshot for the selected nodes")
  Map<String, Object> healthSnapshot(@MBeanParamName("nodeSelector") NodeSelector selector) throws Exception;

  /**
   * Invoke {@code System.gc()} on the specified nodes.
   * @param selector a filter on the nodes attached to the driver, determines the nodes to which this method applies.
   * @return a mapping of node uuids to an eventual exception resulting from invoking this method on the corresponding node. This map may be empty if no exception was raised.
   * @throws Exception if any error occurs.
   */
  @MBeanDescription("invoke System.gc(} on the selected nodes")
  Map<String, Object> gc(@MBeanParamName("nodeSelector") NodeSelector selector) throws Exception;

  /**
   * Trigger a heap dump on the specified nodes.
   * @param selector a filter on the nodes attached to the driver, determines the nodes to which this method applies.
   * @return a mapping of node uuids to an eventual exception resulting from invoking this method on the corresponding node. This map may be empty if no exception was raised.
   * @throws Exception if any error occurs.
   */
  @MBeanDescription("trigger a heap dump on the selected nodes")
  Map<String, Object> heapDump(@MBeanParamName("nodeSelector") NodeSelector selector) throws Exception;

  /**
   * Get a JVM thread dump for the specified nodes.
   * @param selector a filter on the nodes attached to the driver, determines the nodes to which this method applies.
   * @return a mapping of node uuids to an eventual exception resulting from invoking this method on the corresponding node. This map may be empty if no exception was raised.
   * @throws Exception if any error occurs.
   */
  @MBeanDescription("get a thread dump for the selected nodes")
  Map<String, Object> threadDump(@MBeanParamName("nodeSelector") NodeSelector selector) throws Exception;

  /**
   * Get the number of provisioned slave nodes for the selected nodes.
   * @param selector a filter on the nodes attached to the driver, determines the nodes to which this method applies.
   * @return a mapping of node uuids to either an {@code int} corresponding tot he number of slaves provisioned by the node,
   * or an eventual exception resulting from invoking this method on the corresponding node. This map may be empty if no exception was raised.
   * @throws Exception if any error occurs.
   */
  @MBeanDescription("get the number of provisioned slave nodes for the selected nodes")
  Map<String, Object> getNbSlaves(@MBeanParamName("nodeSelector") NodeSelector selector) throws Exception;

  /**
   * Start or stop the required number of slaves to reach the specified number on the selected nodes.
   * This is equivalent to calling {@code provisionSlaveNodes(nbNodes, null)}.
   * @param selector a filter on the nodes attached to the driver, determines the nodes to which this method applies.
   * @param nbNodes the number of slave nodes to reach.
   * @return a mapping of node uuids to an eventual exception resulting from invoking this method on the corresponding node. This map may be empty if no exception was raised.
   * @throws Exception if any error occurs.
   */
  @MBeanDescription("make a slave node provisioning request to the selected nodes")
  Map<String, Object> provisionSlaveNodes(@MBeanParamName("nodeSelector") NodeSelector selector, @MBeanParamName("nbSlaves") int nbNodes) throws Exception;

  /**
   * Start or stop the required number of slaves to reach the specified number on the selected nodes.
   * @param selector a filter on the nodes attached to the driver, determines the nodes to which this method applies.
   * @param nbNodes the number of slave nodes to reach.
   * @param interruptIfRunning if true then nodes can only be stopped once they are idle. 
   * @return a mapping of node uuids to an eventual exception resulting from invoking this method on the corresponding node. This map may be empty if no exception was raised.
   * @throws Exception if any error occurs.
   */
  @MBeanDescription("make a slave node provisioning request to the selected nodes, specifying whether to wait for slave nodes to be idle before stopping them")
  Map<String, Object> provisionSlaveNodes(@MBeanParamName("nodeSelector") NodeSelector selector, @MBeanParamName("nbSlaves") int nbNodes,
    @MBeanParamName("interruptIfRunning") boolean interruptIfRunning) throws Exception;

  /**
   * Start or stop the required number of slaves to reach the specified number, using the specified config overrides, on the selected nodes.
   * <p>If {@code configOverrides} is null, then previous overrides are applied, and already running slave nodes do not need to be stopped.
   * @param selector a filter on the nodes attached to the driver, determines the nodes to which this method applies.
   * @param nbNodes the number of slave nodes to reach.
   * @param configOverrides the configuration overrides to apply.
   * @return a mapping of node uuids to an eventual exception resulting from invoking this method on the corresponding node. This map may be empty if no exception was raised.
   * @throws Exception if any error occurs.
   */
  @MBeanDescription("make a slave node provisioning request to the selected nodes, specifying configuration overrides")
  Map<String, Object> provisionSlaveNodes(@MBeanParamName("nodeSelector") NodeSelector selector, @MBeanParamName("slaves") int nbNodes,
    @MBeanParamName("cfgOverrides") TypedProperties configOverrides) throws Exception;

  /**
   * Start or stop the required number of slaves to reach the specified number, using the specified config overrides, on the selected nodes.
   * <p>If {@code configOverrides} is null, then previous overrides are applied, and already running slave nodes do not need to be stopped.
   * @param selector a filter on the nodes attached to the driver, determines the nodes to which this method applies.
   * @param nbNodes the number of slave nodes to reach.
   * @param interruptIfRunning if true then nodes can only be stopped once they are idle. 
   * @param configOverrides the configuration overrides to apply.
   * @return a mapping of node uuids to an eventual exception resulting from invoking this method on the corresponding node. This map may be empty if no exception was raised.
   * @throws Exception if any error occurs.
   */
  @MBeanDescription("make a slave node provisioning request to the selected nodes, specifying configuration overrides")
  Map<String, Object> provisionSlaveNodes(@MBeanParamName("nodeSelector") NodeSelector selector, @MBeanParamName("nbSlaves") int nbNodes,
    @MBeanParamName("interruptIfRunning") boolean interruptIfRunning, @MBeanParamName("configOverrides") TypedProperties configOverrides) throws Exception;

  /**
   * Register a listener with the specified node selector and MBean.
   * @param selector the node slector to apply to the listener.
   * @param mBeanName the name of the node mbeans to receive notifications from.
   * @return a unique id for the listener.
   * @throws IllegalArgumentException if {@code selector} or {@code mBeanName} is null.
   * @exclude
   */
  @MBeanExclude
  String registerForwardingNotificationListener(@MBeanParamName("nodeSelector") NodeSelector selector, @MBeanParamName("mbeanName") final String mBeanName) throws IllegalArgumentException;

  /**
   * Unregister the specified listener.
   * @param listenerID the ID of the listener to unregister.
   * @throws ListenerNotFoundException if the listener could not be found.
   * @exclude
   */
  @MBeanExclude
  void unregisterForwardingNotificationListener(@MBeanParamName("listenerId") String listenerID) throws ListenerNotFoundException;
}
