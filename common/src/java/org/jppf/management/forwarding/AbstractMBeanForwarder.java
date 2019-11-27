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

import java.util.*;

import javax.management.*;

import org.jppf.management.*;
import org.jppf.utils.ResultsMap;

/**
 * Abstrat super class for generated node MBean forwarding proxies.
 * @author Laurent Cohen
 * @since 6.2
 */
public class AbstractMBeanForwarder {
  /**
   * A wrapper for the JMX connection to the driver.
   */
  private final JMXDriverConnectionWrapper jmx;
  /**
   * Proxy to the node forwarding MBean in the driver.
   */
  private final NodeForwardingMBean forwarder;
  /**
   * The mbean name of the MBean to which the methods apply.
   */
  private final String mbeanName;
  /**
   * The set of lisnterIds for the notification listners registered with this proxy instance. 
   */
  private final Set<String> listenerIDs = new HashSet<>();

  /**
   * Initialize this forwarding proxy with the specified driver jmx connection and mbean name. 
   * @param jmx a wrapper for the JMX connection to the driver.
   * @param mbeanName the name of the mbean to proxy for.
   * @throws Exception if any error occurs.
   */
  public AbstractMBeanForwarder(final JMXDriverConnectionWrapper jmx, final String mbeanName) throws Exception {
    this.jmx = jmx;
    this.forwarder = jmx.getForwarder();
    this.mbeanName = mbeanName;
  }

  /**
   * Invoke a method on the specified MBean of the selected nodes attached to the driver.
   * @param <E> the type of results.
   * @param selector a filter on the nodes attached to the driver, determines the nodes this method applies to.
   * @param methodName the name of the method to invoke.
   * @param params the method parameter values.
   * @param signature the types of the method parameters.
   * @return a mapping of node uuids to the result of invoking the MBean method on the corresponding node. Each result may be an exception.
   * <br/>Additionally, each result may be {@code null}, in particular if the invoked method has a {@code void} return type.
   * @throws Exception if the invocation failed.
   */
  public <E> ResultsMap<String, E> invoke(final NodeSelector selector, final String methodName, final Object[] params, final String[] signature) throws Exception {
    return forwarder.forwardInvoke(selector, mbeanName, methodName, params, signature);
  }

  /**
   * Convenience method to invoke an MBean method that has no parameter.
   * <br/>This is equivalent to calling {@code forwardInvoke(selector, name, methodName, (Object[]) null, (String[]) null)}.
   * @param <E> the type of results.
   * @param selector a filter on the nodes attached to the driver, determines the nodes this method applies to.
   * @param methodName the name of the method to invoke.
   * @return a mapping of node uuids to the result of invoking the MBean method on the corresponding node. Each result may be an exception.
   * <br/>Additionally, each result may be {@code null}, in particular if the invoked method has a {@code void} return type.
   * @throws Exception if the invocation failed.
   */
  public <E> ResultsMap<String, E> invoke(final NodeSelector selector, final String methodName) throws Exception {
    return forwarder.forwardInvoke(selector, mbeanName, methodName);
  }

  /**
   * Get the value of an attribute of the specified MBean for each specified node.
   * @param <E> the type of results.
   * @param selector a filter on the nodes attached to the driver, determines the nodes this method applies to.
   * @param attribute the name of the MBean attribute to read.
   * @return a mapping of node uuids to the result of getting the MBean attribute on the corresponding node. Each result may be an exception.
   * @throws Exception if the invocation failed.
   */
  public <E> ResultsMap<String, E> getAttribute(final NodeSelector selector, final String attribute) throws Exception {
    return forwarder.forwardGetAttribute(selector, mbeanName, attribute);
  }

  /**
   * Set the value of an attribute of the specified MBean on the specified nodes attached to the driver.
   * @param <E> the type of the value to set.
   * @param selector a filter on the nodes attached to the driver, determines the nodes this method applies to.
   * @param attribute the name of the MBean attribute to set.
   * @param value the value to set on the attribute.
   * @return a mapping of node uuids to an eventual exception resulting from setting the MBean attribute on the corresponding node.
   * @throws Exception if the invocation failed.
   */
  public <E> ResultsMap<String, Void> setAttribute(final NodeSelector selector, final String attribute, final E value) throws Exception {
    return forwarder.forwardSetAttribute(selector, mbeanName, attribute, value);
  }

  /**
   * Add a listener to the MBean for all selected nodes, without filter or handback object.
   * @param selector a filter on the nodes attached to the driver, determines the nodes this method applies to.
   * @param listener The listener object which will handle the notifications emitted by the broadcaster.
   * @return a string which uniquely identifies the registered notification listener.
   * @throws Exception if any error occurs.
   */
  public String addNotificationListener(final NodeSelector selector, final NotificationListener listener) throws Exception {
    return addNotificationListener(selector, listener, null, null);
  }

  /**
   * Add a listener to the MBean for all selected nodes.
   * @param selector a filter on the nodes attached to the driver, determines the nodes this method applies to.
   * @param listener The listener object which will handle the notifications emitted by the broadcaster.
   * @param filter The filter object. If filter is null, no filtering occurs.
   * @param handback An opaque object to be sent back to the listener when a notification is emitted
   * @return a string which uniquely identifies the registered notification listener.
   * @throws Exception if any error occurs.
   */
  public String addNotificationListener(final NodeSelector selector, final NotificationListener listener, final NotificationFilter filter, final Object handback) throws Exception {
    final String listenerID = jmx.registerForwardingNotificationListener(selector, mbeanName, listener, filter, handback);
    listenerIDs.add(listenerID);
    return listenerID;
  }

  /**
   * Remove a listener from this MBean forwarder. 
   * @param listenerID a string which uniquely identifies the notification listener to remove.
   * @throws Exception if any error occurs.
   */
  public void removeNotificationListener(final String listenerID) throws Exception {
    jmx.unregisterForwardingNotificationListener(listenerID);
    listenerIDs.remove(listenerID);
  }

  /**
   * Remove all listeners registered from this MBean forwarder. 
   * @throws Exception if any error occurs.
   */
  public void removeAllNotificationListeners() throws Exception {
    for (final String listenerID: listenerIDs) jmx.unregisterForwardingNotificationListener(listenerID);
    listenerIDs.clear();
  }
}
