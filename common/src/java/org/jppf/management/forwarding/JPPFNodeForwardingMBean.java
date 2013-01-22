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

package org.jppf.management.forwarding;

import java.io.Serializable;
import java.util.Map;

import javax.management.*;

import org.jppf.classloader.DelegationModel;
import org.jppf.management.NodeSelector;

/**
 * MBean interface for forwarding node management requests and monitoring notfications via the driver.
 * @author Laurent Cohen
 */
public interface JPPFNodeForwardingMBean extends Serializable, NotificationEmitter
{
  /**
   * Name of the driver's admin MBean.
   */
  String MBEAN_NAME = "org.jppf:name=nodeForwarding,type=driver";

  /**
   * Invoke a method on the specified MBean of all nodes attached to the driver.
   * @param selector a filter on the nodes attached tot he driver, determines the nodes to which this method applies.
   * @param name the name of the MBean.
   * @param methodName the name of the method to invoke.
   * @param params the method parameter values.
   * @param signature the types of the method parameters.
   * @return a mapping of node uuids to the result of invoking the MBean method on the corresponding node. Each result may be an exception.
   * <br/>Additionally, each result may be <code>null</code>, in particular if the invoked method has a <code>void</code> return type.
   * @throws Exception if the invocation failed.
   */
  Map<String, Object> forwardInvoke(final NodeSelector selector, String name, String methodName, Object[] params, String[] signature) throws Exception;

  /**
   * Convenience method to invoke an MBean method that has no parameter.
   * <br/>This is equivalent to calling <code>forwardInvoke(selector, name, methodName, (Object[]) null, (String[]) null)</code>.
   * @param selector a filter on the nodes attached to the driver, determines the nodes to which this method applies.
   * @param name the name of the node MBean to invoke.
   * @param methodName the name of the method to invoke.
   * @return a mapping of node uuids to the result of invoking the MBean method on the corresponding node. Each result may be an exception.
   * <br/>Additionally, each result may be <code>null</code>, in particular if the invoked method has a <code>void</code> return type.
   * @throws Exception if the invocation failed.
   */
  Map<String, Object> forwardInvoke(final NodeSelector selector, final String name, final String methodName) throws Exception;

  /**
   * Get the value of an attribute of the specified MBean for each specified node.
   * @param selector a filter on the nodes attached tot he driver, determines the nodes to which this method applies.
   * @param name the name of the MBean to invoke for each node.
   * @param attribute the name of the MBean attribute to read.
   * @return a mapping of node uuids to the result of getting the MBean attribute on the corresponding node. Each result may be an exception.
   * @throws Exception if the invocation failed.
   */
  Map<String, Object> forwardGetAttribute(final NodeSelector selector, String name, String attribute) throws Exception;

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
  Map<String, Object> forwardSetAttribute(final NodeSelector selector, String name, String attribute, Object value) throws Exception;

  /**
   * Get the latest state information from the node.
   * @param selector a filter on the nodes attached to the driver, determines the nodes to which this method applies.
   * @return a mapping of node uuids to the result of invoking the MBean method on the corresponding node.
   * Each result may be either a {@link org.jppf.management.JPPFNodeState} object, or an exception if the invocation failed for the corresponding node.
   * @throws Exception if any error occurs.
   */
  Map<String, Object> state(NodeSelector selector) throws Exception;

  /**
   * Set the size of the specified nodes' thread pool.
   * @param selector a filter on the nodes attached to the driver, determines the nodes to which this method applies.
   * @param size the size as an int.
   * @return a mapping of node uuids to an eventual exception resulting from invoking this method on the corresponding node.
   * This map may be empty if no exception was raised.
   * @throws Exception if any error occurs.
   */
  Map<String, Object> updateThreadPoolSize(NodeSelector selector, Integer size) throws Exception;

  /**
   * Update the priority of all execution threads for the specified nodes.
   * @param selector a filter on the nodes attached to the driver, determines the nodes to which this method applies.
   * @param newPriority the new priority to set.
   * @return a mapping of node uuids to an eventual exception resulting from invoking this method on the corresponding node.
   * This map may be empty if no exception was raised.
   * @throws Exception if an error is raised when invoking the node mbean.
   */
  Map<String, Object> updateThreadsPriority(NodeSelector selector, Integer newPriority) throws Exception;

  /**
   * Restart the specified nodes.
   * @param selector a filter on the nodes attached to the driver, determines the nodes to which this method applies.
   * @return a mapping of node uuids to an eventual exception resulting from invoking this method on the corresponding node.
   * This map may be empty if no exception was raised.
   * @throws Exception if any error occurs.
   */
  Map<String, Object> restart(NodeSelector selector) throws Exception;

  /**
   * Shutdown the specified nodes.
   * @param selector a filter on the nodes attached to the driver, determines the nodes to which this method applies.
   * @return a mapping of node uuids to an eventual exception resulting from invoking this method on the corresponding node.
   * This map may be empty if no exception was raised.
   * @throws Exception if any error occurs.
   */
  Map<String, Object> shutdown(NodeSelector selector) throws Exception;

  /**
   * Reset the specified nodes' executed tasks counter to zero.
   * @param selector a filter on the nodes attached to the driver, determines the nodes to which this method applies.
   * @return a mapping of node uuids to an eventual exception resulting from invoking this method on the corresponding node.
   * This map may be empty if no exception was raised.
   * @throws Exception if any error occurs.
   */
  Map<String, Object> resetTaskCounter(NodeSelector selector) throws Exception;

  /**
   * Reset the specified nodes' executed tasks counter to the specified value.
   * @param selector a filter on the nodes attached to the driver, determines the nodes to which this method applies.
   * @param n the number to set the task counter to.
   * @return a mapping of node uuids to an eventual exception resulting from invoking this method on the corresponding node.
   * This map may be empty if no exception was raised.
   * @throws Exception if any error occurs.
   */
  Map<String, Object> setTaskCounter(NodeSelector selector, Integer n) throws Exception;

  /**
   * Update the configuration properties of the specified nodes.
   * @param selector a filter on the nodes attached to the driver, determines the nodes to which this method applies.
   * @param config the set of properties to update.
   * @param reconnect specifies whether the node should reconnect ot the driver after updating the properties.
   * @return a mapping of node uuids to an eventual exception resulting from invoking this method on the corresponding node.
   * This map may be empty if no exception was raised.
   * @throws Exception if any error occurs.
   */
  Map<String, Object> updateConfiguration(NodeSelector selector, Map<Object, Object> config, Boolean reconnect) throws Exception;

  /**
   * Cancel the job with the specified id in the specified nodes.
   * @param selector a filter on the nodes attached to the driver, determines the nodes to which this method applies.
   * @param jobId the id of the job to cancel.
   * @param requeue true if the job should be requeued on the server side, false otherwise.
   * @return a mapping of node uuids to an eventual exception resulting invoking this method on the corresponding node.
   * This map may be empty if no exception was raised.
   * @throws Exception if any error occurs.
   */
  Map<String, Object> cancelJob(NodeSelector selector, String jobId, Boolean requeue) throws Exception;

  /**
   * Get the current class loader delegation model for the specified nodes.
   * @param selector a filter on the nodes attached to the driver, determines the nodes to which this method applies.
   * @return a mapping of node uuids to the result of invoking the MBean method on the corresponding node.
   * Each result may be either a {@link DelegationModel} enum value, or an exception if the invocation failed for the corresponding node.
   * @throws Exception if any error occurs.
   * @see org.jppf.classloader.AbstractJPPFClassLoader#getDelegationModel()
   */
  Map<String, Object> getDelegationModel(NodeSelector selector) throws Exception;

  /**
   * Set the current class loader delegation model for the specified nodes.
   * @param selector a filter on the nodes attached to the driver, determines the nodes to which this method applies.
   * @param model either either {@link org.jppf.classloader.DelegationModel#PARENT_FIRST PARENT_FIRST} or {@link org.jppf.classloader.DelegationModel#URL_FIRST URL_FIRST}.
   * If any other value is specified then this method has no effect.
   * @return a mapping of node uuids to an eventual exception resulting from invoking this method on the corresponding node.
   * This map may be empty if no exception was raised.
   * @throws Exception if any error occurs.
   * @see org.jppf.classloader.AbstractJPPFClassLoader#setDelegationModel(org.jppf.classloader.DelegationModel)
   */
  Map<String, Object> setDelegationModel(NodeSelector selector, DelegationModel model) throws Exception;

  /**
   * Get the system information for the specified nodes.
   * @param selector a filter on the nodes attached to the driver, determines the nodes to which this method applies.
   * @return a mapping of node uuids to an eventual exception resulting from invoking this method on the corresponding node.
   * This map may be empty if no exception was raised.
   * @throws Exception if any error occurs.
   */
  Map<String, Object> systemInformation(NodeSelector selector) throws Exception;

  /**
   * Register a listener with the specified node selector and MBean.
   * @param selector the node slector to apply to the listener.
   * @param mBeanName the name of the node mbeans to receive notifications from.
   * @return a unique id for the listener.
   * @throws IllegalArgumentException if <code>selector</code> or <code>mBeanName</code> is null.
   * @exclude
   */
  String registerForwardingNotificationListener(final NodeSelector selector, final String mBeanName) throws IllegalArgumentException;

  /**
   * Unregister the specified listener.
   * @param listenerID the ID of the listener to unregister.
   * @throws ListenerNotFoundException if the listener could not be found.
   * @exclude
   */
  void unregisterForwardingNotificationListener(final String listenerID) throws ListenerNotFoundException;
}
