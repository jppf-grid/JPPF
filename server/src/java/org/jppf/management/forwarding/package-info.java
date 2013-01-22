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

/**
 * Support for forwarding JMX requests to the nodes, along with receiving notifications from them,
 * via the JPPF driver's JMX server. Which nodes are impacted is determined by a user-provided {@link org.jppf.management.NodeSelector node selector}.
 * <p>This provides two major benefits:
 * <ul>
 * <li>this allows managing and monitoring the nodes in situations where the nodes are not reachable from the client,
 * for instance when the client and nodes are on different networks or subnets</li>
 * <li>the requests and notification forwarding mechanism automatically adapts to node connection and disconnection events,
 * which means that if new nodes are started in the grid, they will be automatically enrolled in the forwarding mechanism, provided they match the node selector</li>
 * </ul>
 * 
 * <h2>Forwarding management requests</h2>
 * <p>The request forwarding mechanism is based on a built-in driver MBean: {@link JPPFNodeForwardingMBean}, which provides methods to invoke methods, or get or set attributes on remote node MBeans.
 * Each of its methods requires a {@link NodeSelector} argument and an MBean name, to determine to which nodes, and which MBean in these nodes, the request will be performed.
 * The return value is always a map of node UUIDs to the corresponding value returned by the request (if any) to the corresponding node.
 * If an exception is raised when performing the request on a specific node, then that exception is returned in the map.
 * 
 * Here is an example:
 * <pre>
 * JPPFClient client = ...;
 * AbstractJPPFClientConnection conn = (AbstractJPPFClientConnection) client.getClientConnection();
 * JMXDriverConnectionWrapper driverJmx = conn.getJmxConnection();
 * 
 * JPPFNodeForwardingMBean proxy =
 *   driverJmx.getProxy(JPPFNodeForwardingMBean.MBEAN_NAME, JPPFNodeForwardingMBean.class);
 * 
 * // this selector selects all nodes attached to the driver
 * NodeSelector selector = new NodeSelector.AllNodes();
 * // this selector selects all nodes that have more than 2 processors
 * ExecutionPolicy policy = new MoreThan("availableProcessors ", 2);
 * NodeSelector selector2 = new NodeSelector.ExecutionPolicySelector(policy);
 * 
 * // invoke the state() method on the remote 'JPPFNodeAdminMBean' node MBeans.
 * // note that the MBean name does not need to be stated explicitely.
 * Map&lt;String, Object&gt; results = proxy.state(selector);
 * // this is an exact equivalent, explicitely stating the target MBean on the remote nodes:
 * String targetMBeanName = JPPFNodeAdminMBean.MBEAN_NAME;
 * Map&lt;String, Object&gt; results2 = proxy.forwardInvoke(selector, targetMBeanName, "state");
 * 
 * // handling the results
 * for (Map.Entry&lt;String, Object&gt; entry: results) {
 *   if (entry.getValue() instanceof Exception) {
 *     // handle the exception ...
 *   } else {
 *     JPPFNodeState state = (JPPFNodeState) entry.getValue();
 *     // handle the result ...
 *   }
 * }
 * </pre>
 * 
 * <h2>Forwarding JMX notifications</h2>
 * <p>JPPF provides a way to subscribe to notifications from a set of selected nodes, which differs from the one specified in the JMX API.
 * This is due to the fact that the server-side mechanism for the registration of notification listeners is unspecified and thus provides
 * no reliable way of overriding it.
 * <p>To circumvent this, the notification listener registration is performed via the JMX client wrapper {@link org.jppf.management.JMXDriverConnectionWrapper}:
 * <ul>
 * <li>to add a notification listener, use {@link org.jppf.management.JMXDriverConnectionWrapper#registerForwardingNotificationListener(NodeSelector,String,NotificationListener,NotificationFilter,Object) registerForwardingNotificationListener(NodeSelector selector, String mBeanName, NotificationListener listener, NotificationFilter filter, Object handback)}.
 * This will register a notification listener for the specified MBean on each of the selected nodes.
 * This method returns a <i>listener ID</i> which will be used to remove the notfication listener later on.
 * Thus, the application must be careful to keep track of all registered listener IDs.
 * </li>
 * <li>to remove a notification listener, use {@link org.jppf.management.JMXDriverConnectionWrapper#unregisterForwardingNotificationListener(String) unregisterForwardingNotificationListener(String listenerID)}.</li>
 * </ul>
 * <p>The notifications forwarded from the nodes are all wrapped into instances of {@link JPPFNodeForwardingNotification}.
 * This class, which inherits from {@link javax.management.Notification}, provides additional APIs to identify from which node and which MBean the notification was emitted.
 * <p>The following code sample puts it all together:
 * <pre>
 * JPPFClient client = ...;
 * AbstractJPPFClientConnection conn = (AbstractJPPFClientConnection) client.getClientConnection();
 * JMXDriverConnectionWrapper driverJmx = conn.getJmxConnection();
 * // this selector selects all nodes attached to the driver
 * NodeSelector selector = new NodeSelector.AllNodes();
 * 
 * // create a JMX notification listener
 * NotificationListener myListener = new NotificationListener() {
 *   &#64;Override
 *   public void handleNotification(Notification notification, Object handback) {
 *     JPPFNodeForwardingNotification notif = (JPPFNodeForwardingNotification) notification;
 *     System.out.println("received notification from nodeUuid=" + notif.getNodeUuid()
 *       + ", mBeanName=" + notif.getMBeanName());
 *     // get the actual notification sent by the node
 *     TaskExecutionNotification actualNotif = (TaskExecutionNotification) notif.getNotification();
 *     // handle the notification data ...
 *   }
 * }
 * 
 * // register the notification listener with the JPPFNodeTaskMonitorMBean on the selected nodes
 * String listenerID = driverJmx.registerForwardingNotificationListener(
 *   selector, JPPFNodeTaskMonitorMBean.MBEAN_NAME, listener, null, null);
 * 
 * // ... submit a JPPF job ...
 * 
 * // once the job has completed, unregister the notification listener
 * driverJmx.unregisterForwardingNotificationListener(listenerID);
 * </pre>
 */
package org.jppf.management.forwarding;

