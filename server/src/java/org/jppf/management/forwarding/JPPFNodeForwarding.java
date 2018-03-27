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

package org.jppf.management.forwarding;

import static org.jppf.utils.collections.CollectionUtils.array;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;

import javax.management.*;

import org.jppf.classloader.DelegationModel;
import org.jppf.management.*;
import org.jppf.management.diagnostics.DiagnosticsMBean;
import org.jppf.node.provisioning.JPPFNodeProvisioningMBean;
import org.jppf.server.JPPFDriver;
import org.jppf.server.nio.nodeserver.AbstractNodeContext;
import org.jppf.utils.*;
import org.jppf.utils.concurrent.ConcurrentUtils;
import org.jppf.utils.configuration.JPPFProperties;
import org.slf4j.Logger;

/**
 * Implementation of the <code>JPPFNodeForwardingMBean</code> interface.
 * @author Laurent Cohen
 * @exclude
 */
public class JPPFNodeForwarding extends NotificationBroadcasterSupport implements JPPFNodeForwardingMBean {
  /**
   * Explicit serialVersionUID.
   */
  private static final long serialVersionUID = 1L;
  /**
   * Logger for this class.
   */
  private static final Logger log = LoggingUtils.getLogger(JPPFNodeForwarding.class, false);
  /**
   * Determines whether debug log statements are enabled.
   */
  private static final boolean debugEnabled = LoggingUtils.isDebugEnabled(log);
  /**
   * Flag to indicate a task which invokes an MBean method.
   */
  private static final int INVOKE_METHOD = 1;
  /**
   * Flag to indicate a task which gets an MBean attribute value.
   */
  private static final int GET_ATTRIBUTE = 2;
  /**
   * Flag to indicate a task which sets an MBean attribute value.
   */
  private static final int SET_ATTRIBUTE = 3;
  /**
   * Used to generate listener IDs.
   */
  private final AtomicLong listenerSequence = new AtomicLong(0L);
  /**
   * Reference to the JPPF driver.
   */
  final JPPFDriver driver = JPPFDriver.getInstance();
  /**
   * Manages the forwarding of node notifications to the registered clients.
   */
  private final ForwardingNotificationManager manager;
  /**
   * Provides an API for selecting nodes based on a {@link NodeSelector}.
   */
  private final NodeSelectionHelper selectionHelper;
  /**
   * Number of executor threads.
   */
  private static final int core = JPPFConfiguration.get(JPPFProperties.NODE_FORWARDING_POOL_SIZE);
  /**
   * Use to send management/monitoring requests in parallel with regards to the nodes.
   */
  private final ExecutorService executor;

  /**
   * Initialize this MBean implementation.
   */
  public JPPFNodeForwarding() {
    selectionHelper = new NodeSelectionHelper();
    NodeForwardingHelper.getInstance().setSelectionProvider(selectionHelper);
    manager = new ForwardingNotificationManager(this);
    //@SuppressWarnings("unused")
    executor = ConcurrentUtils.newFixedExecutor(core, "NodeForwarding");
    if (debugEnabled) log.debug("initialized JPPFNodeForwarding");
  }

  @Override
  public Map<String, Object> forwardInvoke(final NodeSelector selector, final String name, final String methodName, final Object[] params, final String[] signature) throws Exception {
    final Set<AbstractNodeContext> channels = selectionHelper.getChannels(selector);
    if (debugEnabled) log.debug("invoking {}() on mbean={} for selector={} ({} channels)", new Object[] {methodName, name, selector, channels.size()});
    return forward(INVOKE_METHOD, channels, name, methodName, params, signature);
  }

  @Override
  public Map<String, Object> forwardInvoke(final NodeSelector selector, final String name, final String methodName) throws Exception {
    return forwardInvoke(selector, name, methodName, (Object[]) null, (String[]) null);
  }

  @Override
  public Map<String, Object> forwardGetAttribute(final NodeSelector selector, final String name, final String attribute) throws Exception {
    final Set<AbstractNodeContext> channels = selectionHelper.getChannels(selector);
    return forward(GET_ATTRIBUTE, channels, name, attribute);
  }

  @Override
  public Map<String, Object> forwardSetAttribute(final NodeSelector selector, final String name, final String attribute, final Object value) throws Exception {
    final Set<AbstractNodeContext> channels = selectionHelper.getChannels(selector);
    return forward(SET_ATTRIBUTE, channels, name, attribute, value);
  }

  @Override
  public Map<String, Object> state(final NodeSelector selector) throws Exception {
    return forwardInvoke(selector, JPPFNodeAdminMBean.MBEAN_NAME, "state");
  }

  @Override
  public Map<String, Object> updateThreadPoolSize(final NodeSelector selector, final Integer size) throws Exception {
    return forwardInvoke(selector, JPPFNodeAdminMBean.MBEAN_NAME, "updateThreadPoolSize", array(size), array("java.lang.Integer"));
  }

  @Override
  public Map<String, Object> updateThreadsPriority(final NodeSelector selector, final Integer newPriority) throws Exception {
    return forwardInvoke(selector, JPPFNodeAdminMBean.MBEAN_NAME, "updateThreadsPriority", array(newPriority), array("java.lang.Integer"));
  }

  @Override
  public Map<String, Object> restart(final NodeSelector selector) throws Exception {
    return forwardInvoke(selector, JPPFNodeAdminMBean.MBEAN_NAME, "restart");
  }

  @Override
  public Map<String, Object> restart(final NodeSelector selector, final Boolean interruptIfRunning) throws Exception {
    return forwardInvoke(selector, JPPFNodeAdminMBean.MBEAN_NAME, "restart", array(interruptIfRunning), array("java.lang.Boolean"));
  }

  @Override
  public Map<String, Object> shutdown(final NodeSelector selector) throws Exception {
    return forwardInvoke(selector, JPPFNodeAdminMBean.MBEAN_NAME, "shutdown");
  }

  @Override
  public Map<String, Object> shutdown(final NodeSelector selector, final Boolean interruptIfRunning) throws Exception {
    return forwardInvoke(selector, JPPFNodeAdminMBean.MBEAN_NAME, "shutdown", array(interruptIfRunning), array("java.lang.Boolean"));
  }

  @Override
  public Map<String, Object> resetTaskCounter(final NodeSelector selector) throws Exception {
    return forwardInvoke(selector, JPPFNodeAdminMBean.MBEAN_NAME, "resetTaskCounter");
  }

  @Override
  public Map<String, Object> setTaskCounter(final NodeSelector selector, final Integer n) throws Exception {
    return forwardSetAttribute(selector, JPPFNodeAdminMBean.MBEAN_NAME, "TaskCounter", n);
  }

  @Override
  public Map<String, Object> updateConfiguration(final NodeSelector selector, final Map<Object, Object> configOverrides, final Boolean restart, final Boolean interruptIfRunning) throws Exception {
    return forwardInvoke(selector, JPPFNodeAdminMBean.MBEAN_NAME, "updateConfiguration",
      new Object[] {configOverrides, restart, interruptIfRunning}, array("java.util.Map", "java.lang.Boolean", "java.lang.Boolean"));
  }

  @Override
  public Map<String, Object> updateConfiguration(final NodeSelector selector, final Map<Object, Object> configOverrides, final Boolean restart) throws Exception {
    return forwardInvoke(selector, JPPFNodeAdminMBean.MBEAN_NAME, "updateConfiguration", new Object[] {configOverrides, restart}, array("java.util.Map", "java.lang.Boolean"));
  }

  @Override
  public Map<String, Object> cancelJob(final NodeSelector selector, final String jobUuid, final Boolean requeue) throws Exception {
    return forwardInvoke(selector, JPPFNodeAdminMBean.MBEAN_NAME, "cancelJob", new Object[] {jobUuid, requeue}, array("java.lang.String", "java.lang.Boolean"));
  }

  @Override
  public Map<String, Object> getDelegationModel(final NodeSelector selector) throws Exception {
    return forwardGetAttribute(selector, JPPFNodeAdminMBean.MBEAN_NAME, "DelegationModel");
  }

  @Override
  public Map<String, Object> setDelegationModel(final NodeSelector selector, final DelegationModel model) throws Exception {
    return forwardSetAttribute(selector, JPPFNodeAdminMBean.MBEAN_NAME, "DelegationModel", model);
  }

  @Override
  public Map<String, Object> systemInformation(final NodeSelector selector) throws Exception {
    return forwardInvoke(selector, JPPFNodeAdminMBean.MBEAN_NAME, "systemInformation");
  }

  /**
   * Register a listener with the specified node selector and MBean.
   * @param selector the node slector to apply to the listener.
   * @param mBeanName the name of the node mbeans to receive notifications from.
   * @return a unique id for the listener.
   * @throws IllegalArgumentException if <code>selector</code> or <code>mBeanName</code> is null.
   */
  @Override
  public String registerForwardingNotificationListener(final NodeSelector selector, final String mBeanName) throws IllegalArgumentException {
    if (debugEnabled) log.debug("before registering listener with selector=" + selector + ", mbean=" + mBeanName);
    if (selector == null) throw new IllegalArgumentException("selector cannot be null");
    if (mBeanName == null) throw new IllegalArgumentException("mBeanName cannot be null");
    final String id = StringUtils.build(driver.getUuid(), ':', listenerSequence.incrementAndGet());
    this.manager.addNotificationListener(id, selector, mBeanName);
    if (debugEnabled) log.debug("registered listener id=" + id);
    return id;
  }

  /**
   * Unregister the specified listener.
   * @param listenerID the ID of the listener to unregister.
   * @throws ListenerNotFoundException if the listener could not be found.
   */
  @Override
  public void unregisterForwardingNotificationListener(final String listenerID) throws ListenerNotFoundException {
    if (debugEnabled) log.debug("before unregistering listener id={}", listenerID);
    manager.removeNotificationListener(listenerID);
    if (debugEnabled) log.debug("unregistered listener id={}", listenerID);
  }

  @Override
  public void sendNotification(final Notification notification) {
    if (debugEnabled) log.debug("sending notif: " + notification);
    super.sendNotification(notification);
  }

  /**
   * Get the object that provides an API for selecting nodes based on a {@link NodeSelector}.
   * @return a {@link NodeSelectionHelper} instance.
   */
  NodeSelectionHelper getSelectionHelper() {
    return selectionHelper;
  }

  @Override
  public Map<String, Object> healthSnapshot(final NodeSelector selector) throws Exception {
    return forwardInvoke(selector, DiagnosticsMBean.MBEAN_NAME_NODE, "healthSnapshot");
  }

  @Override
  public Map<String, Object> threadDump(final NodeSelector selector) throws Exception {
    return forwardInvoke(selector, DiagnosticsMBean.MBEAN_NAME_NODE, "threadDump");
  }

  @Override
  public Map<String, Object> gc(final NodeSelector selector) throws Exception {
    return forwardInvoke(selector, DiagnosticsMBean.MBEAN_NAME_NODE, "gc");
  }

  @Override
  public Map<String, Object> heapDump(final NodeSelector selector) throws Exception {
    return forwardInvoke(selector, DiagnosticsMBean.MBEAN_NAME_NODE, "heapDump");
  }

  @Override
  public Map<String, Object> getNbSlaves(final NodeSelector selector) throws Exception {
    return forwardGetAttribute(selector, JPPFNodeProvisioningMBean.MBEAN_NAME, "NbSlaves");
  }

  @Override
  public Map<String, Object> provisionSlaveNodes(final NodeSelector selector, final int nbNodes) throws Exception {
    return forwardInvoke(selector, JPPFNodeProvisioningMBean.MBEAN_NAME, "provisionSlaveNodes", new Object[] { nbNodes }, new String[] { "int" });
  }

  @Override
  public Map<String, Object> provisionSlaveNodes(final NodeSelector selector, final int nbNodes, final boolean interruptIfRunning) throws Exception {
    return forwardInvoke(selector, JPPFNodeProvisioningMBean.MBEAN_NAME, "provisionSlaveNodes", new Object[] { nbNodes, interruptIfRunning }, new String[] { "int", "boolean" });
  }

  @Override
  public Map<String, Object> provisionSlaveNodes(final NodeSelector selector, final int nbNodes, final TypedProperties configOverrides) throws Exception {
    return forwardInvoke(selector, JPPFNodeProvisioningMBean.MBEAN_NAME, "provisionSlaveNodes",
      new Object[] { nbNodes, configOverrides }, new String[] { "int", TypedProperties.class.getName() });
  }

  @Override
  public Map<String, Object> provisionSlaveNodes(final NodeSelector selector, final int nbNodes, final boolean interruptIfRunning, final TypedProperties configOverrides) throws Exception {
    return forwardInvoke(selector, JPPFNodeProvisioningMBean.MBEAN_NAME, "provisionSlaveNodes",
      new Object[] { nbNodes, interruptIfRunning, configOverrides }, new String[] { "int", "boolean", TypedProperties.class.getName() });
  }

  /**
   * Forward the specified operation to the specified nodes.
   * @param type the type of operation to forward.
   * @param nodes the nodes to forward to.
   * @param mbeanName the name of the node MBean to which the request is sent.
   * @param memberName the name of the method to invoke, or of the attribute to get or set.
   * @param params additional params to send with the request.
   * @return a mapping of node uuids to the result of invoking the MBean operation on the corresponding node. Each result may be an exception.
   * Additionally, each result may be {@code null}, in particular if the invoked method has a {@code void} return type.
   * @throws Exception if the invocation failed.
   */
  Map<String, Object> forward(final int type, final Set<AbstractNodeContext> nodes, final String mbeanName, final String memberName, final Object...params) throws Exception {
    try {
      final int size = nodes.size();
      if (size <= 0) return Collections.<String, Object>emptyMap();
      final ForwardCallback callback = new ForwardCallback(size);
      AbstractForwardingTask task;
      for (final AbstractNodeContext node: nodes) {
        switch(type) {
          case INVOKE_METHOD:
            task = new InvokeMethodTask(node, callback, mbeanName, memberName, (Object[]) params[0], (String[]) params[1]);
            break;
          case GET_ATTRIBUTE:
            task = new GetAttributeTask(node, callback, mbeanName, memberName);
            break;
          case SET_ATTRIBUTE:
            task = new SetAttributeTask(node, callback, mbeanName, memberName, params[0]);
            break;
          default:
            throw new IllegalArgumentException(
              String.format("unknown type of operation %d for mbean=%s, memeber=%s, param=%s, node=%s", type, mbeanName, memberName, Arrays.deepToString(params), node));
        }
        if (debugEnabled) log.debug(String.format("about to forward with type=%d, mbean=%s, member=%s, params=%s, node=%s", type, mbeanName, memberName, Arrays.deepToString(params), node));
        executor.execute(task);
      }
      return callback.await();
    } catch (final Exception e) {
      if (debugEnabled) {
        log.debug(String.format("error forwarding with type=%d, nb nodes=%d, mbeanNaem=%s, memberName=%s, params=%s%n%s",
          type, nodes.size(), mbeanName, memberName, Arrays.asList(params), ExceptionUtils.getStackTrace(e)));
      }
      throw e;
    }
  }

  /**
   * A callback invoked by each submitted forwarding task to notify that results have arrived from a node.
   */
  static class ForwardCallback {
    /**
     * The map holding the results from all nodes.
     */
    private final Map<String, Object> resultMap;
    /**
     * The expected total number of results.
     */
    private final int expectedCount;
    /**
     * The current count of received results.
     */
    private int count;

    /**
     * Initialize with the specified expected total number of results.
     * @param expectedCount the expected total number of results.
     */
    ForwardCallback(final int expectedCount) {
      this.resultMap = new ConcurrentHashMap<>(expectedCount, 0.75f, core);
      this.expectedCount = expectedCount;
    }

    /**
     * Called when a result is received from a node.
     * @param uuid the uuid of the node.
     * @param result the result of exception returned by the JMX call.
     */
    void gotResult(final String uuid, final Object result) {
      resultMap.put(uuid, result);
      synchronized(this) {
        if (++count == expectedCount) notify();
      }
    }

    /**
     * Wait until the number of results reaches the expected count.
     * @return the results map;
     * @throws Exception if any error occurs.
     */
    Map<String, Object> await() throws Exception {
      synchronized(this) {
        while (count < expectedCount) wait();
      }
      return resultMap;
    }
  }
}
