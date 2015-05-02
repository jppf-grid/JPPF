/*
 * JPPF.
 * Copyright (C) 2005-2015 JPPF Team.
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
import org.jppf.server.JPPFDriver;
import org.jppf.server.nio.nodeserver.*;
import org.jppf.utils.*;
import org.slf4j.*;

/**
 * Implementation of the <code>JPPFNodeForwardingMBean</code> interface.
 * @author Laurent Cohen
 * @exclude
 */
public class JPPFNodeForwarding extends NotificationBroadcasterSupport implements JPPFNodeForwardingMBean {
  /**
   * Logger for this class.
   */
  private static final Logger log = LoggerFactory.getLogger(JPPFNodeForwarding.class);
  /**
   * Determines whether debug log statements are enabled.
   */
  private static boolean debugEnabled = LoggingUtils.isDebugEnabled(log);
  /**
   * Base name used for localization lookups.
   */
  private static final String I18N_BASE = "org.jppf.server.i18n.server_messages";
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
  final ForwardingNotificationManager manager;
  /**
   * Provides an API for selecting nodes based on a {@link NodeSelector}.
   */
  final NodeSelectionHelper selectionHelper;
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
    int nbThreads = JPPFConfiguration.getProperties().getInt("jppf.node.forwarding.pool.size", Runtime.getRuntime().availableProcessors());
    ThreadFactory factory = new JPPFThreadFactory("NodeForwarding");
    executor = Executors.newFixedThreadPool(nbThreads, factory);
    //executor = new ThreadPoolExecutor(0, Integer.MAX_VALUE, 60L, TimeUnit.SECONDS, new SynchronousQueue<Runnable>(), factory);
    //executor = Executors.newCachedThreadPool(new JPPFThreadFactory("NodeForwarding"));
    if (debugEnabled) log.debug("initialized JPPFNodeForwarding");
  }

  @Override
  public Map<String, Object> forwardInvoke(final NodeSelector selector, final String name, final String methodName, final Object[] params, final String[] signature) throws Exception {
    Set<AbstractNodeContext> channels = selectionHelper.getChannels(selector);
    if (debugEnabled) log.debug("invoking {}() on mbean={} for selector={} ({} channels)", new Object[] {methodName, name, selector, channels.size()});
    return forward(INVOKE_METHOD, channels, name, methodName, params, signature);
  }

  @Override
  public Map<String, Object> forwardInvoke(final NodeSelector selector, final String name, final String methodName) throws Exception {
    return forwardInvoke(selector, name, methodName, (Object[]) null, (String[]) null);
  }

  @Override
  public Map<String, Object> forwardGetAttribute(final NodeSelector selector, final String name, final String attribute) throws Exception {
    Set<AbstractNodeContext> channels = selectionHelper.getChannels(selector);
    return forward(GET_ATTRIBUTE, channels, name, attribute);
  }

  @Override
  public Map<String, Object> forwardSetAttribute(final NodeSelector selector, final String name, final String attribute, final Object value) throws Exception {
    Set<AbstractNodeContext> channels = selectionHelper.getChannels(selector);
    return forward(SET_ATTRIBUTE, channels, name, attribute, value);
  }

  /**
   * Get the JPPF nodes server.
   * @return a <code>NodeNioServer</code> instance.
   * @exclude
   */
  private NodeNioServer getNodeNioServer() {
    return driver.getNodeNioServer();
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
  public Map<String, Object> updateConfiguration(final NodeSelector selector, final Map<Object, Object> config, final Boolean reconnect) throws Exception {
    return forwardInvoke(selector, JPPFNodeAdminMBean.MBEAN_NAME, "updateConfiguration", new Object[] {config, reconnect}, array("java.util.Map", "java.lang.Boolean"));
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
    String id = StringUtils.build(driver.getUuid(), ':', listenerSequence.incrementAndGet());
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
    if (debugEnabled) log.debug("before unregistering listener id=" + listenerID);
    manager.removeNotificationListener(listenerID);
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

  /**
   * Forward the specified operation to the specified nodes.
   * @param type the type of operation to forward.
   * @param nodes the nodes to forward to.
   * @param mbeanName the name of the node MBean to which the request is sent.
   * @param memberName the name of the method to invoke, or of the attribute to get or set.
   * @param otherParams additional params to send with the request.
   * @return a mapping of node uuids to the result of invoking the MBean operation on the corresponding node. Each result may be an exception.
   * <br>Additionally, each result may be <code>null</code>, in particular if the invoked method has a <code>void</code> return type.
   * @throws Exception if the invocation failed.
   */
  private Map<String, Object> forward(final int type, final Set<AbstractNodeContext> nodes, final String mbeanName, final String memberName, final Object...otherParams) throws Exception {
    List<Future<Pair<String, Object>>> futures = new ArrayList<>(nodes.size());
    final Map<String, Object> map = new HashMap<>();
    for (AbstractNodeContext node: nodes) {
      ForwardingTask task = null;
      switch(type) {
        case INVOKE_METHOD:
          task = new InvokeMethodTask(node, mbeanName, memberName, (Object[]) otherParams[0], (String[]) otherParams[1]);
          break;
        case GET_ATTRIBUTE:
          task = new GetAttributeTask(node, mbeanName, memberName);
          break;
        case SET_ATTRIBUTE:
          task = new SetAttributeTask(node, mbeanName, memberName, otherParams[0]);
          break;
        default:
          continue;
      }
      if (task != null) futures.add(executor.submit(task));
    }
    for (Future<Pair<String, Object>> f: futures) {
      Pair<String, Object> result = f.get();
      if (result != null) map.put(result.first(), result.second());
    }
    return map;
  }

  /**
   * This task invokes an MBean method on a remote node.
   */
  private static class InvokeMethodTask extends ForwardingTask {
    /**
     * The method parameter values.
     */
    private final Object[] params;
    /**
     * The types of the method parameters.
     */
    private final String[] signature;

    /**
     * Initialize this task.
     * @param context represents the node to which a request is sent.
     * @param mbeanName the name of the node MBean to which the request is sent.
     * @param methodName the name of the method to invoke, or the attribute to get or set.
     * @param params the method parameter values.
     * @param signature the types of the method parameters.
     */
    protected InvokeMethodTask(final AbstractNodeContext context, final String mbeanName, final String methodName, final Object[] params, final String[] signature) {
      super(context, mbeanName, methodName);
      this.params = params;
      this.signature = signature;
    }

    @Override
    protected Pair<String, Object> execute() throws Exception {
      String uuid = context.getUuid();
      JMXNodeConnectionWrapper wrapper = context.getJmxConnection();
      if (debugEnabled) log.debug("invoking {}() on mbean={} for node={} with jmx=", new Object[] {memberName, mbeanName, uuid, wrapper});
      Object o = wrapper.invoke(mbeanName, memberName, params, signature);
      return new Pair<>(uuid, o);
    }
  }

  /**
   * This task sets an MBean attribute value on a remote node.
   */
  private static class SetAttributeTask extends ForwardingTask {
    /**
     * The method parameter values.
     */
    private final Object value;

    /**
     * Initialize this task.
     * @param context represents the node to which a request is sent.
     * @param mbeanName the name of the node MBean to which the request is sent.
     * @param attribute the name of the attribute to set.
     * @param value the value to set on the attribute.
     */
    protected SetAttributeTask(final AbstractNodeContext context, final String mbeanName, final String attribute, final Object value) {
      super(context, mbeanName, attribute);
      this.value = value;
    }

    @Override
    protected Pair<String, Object> execute() throws Exception {
      String uuid = context.getUuid();
      JMXNodeConnectionWrapper wrapper = context.getJmxConnection();
      wrapper.setAttribute(mbeanName, memberName, value);
      if (debugEnabled) log.debug("set attribute '" + memberName + "' on node " + uuid);
      return null;
    }
  }

  /**
   * This task gets an MBean attribute value from a remote node.
   */
  private static class GetAttributeTask extends ForwardingTask {
    /**
     * Initialize this task.
     * @param context represents the node to which a request is sent.
     * @param mbeanName the name of the node MBean to which the request is sent.
     * @param attribute the name of the attribute to get.
     */
    protected GetAttributeTask(final AbstractNodeContext context, final String mbeanName, final String attribute) {
      super(context, mbeanName, attribute);
    }

    @Override
    protected Pair<String, Object> execute() throws Exception {
      String uuid = context.getUuid();
      JMXNodeConnectionWrapper wrapper = context.getJmxConnection();
      Object o = wrapper.getAttribute(mbeanName, memberName);
      if (debugEnabled) {
        log.debug(String.format("get attribute '%s' = %s on node %s", memberName, o, uuid));
      }
      return new Pair<>(uuid, o);
    }
  }

  /**
   * Common super class for all forwrding tasks.
   */
  private abstract static class ForwardingTask implements Callable<Pair<String, Object>> {
    /**
     * Represents the node to which a request is sent.
     */
    protected final AbstractNodeContext context;
    /**
     * The name of the node MBean to which the request is sent.
     */
    protected final String mbeanName;
    /**
     * The name of the method to invoke, or the attribute to get or set.
     */
    protected final String memberName;

    /**
     * Initialize this task.
     * @param context represents the node to which a request is sent.
     * @param mbeanName the name of the node MBean to which the request is sent.
     * @param memberName the name of the method to invoke, or the attribute to get or set.
     */
    protected ForwardingTask(final AbstractNodeContext context, final String mbeanName, final String memberName) {
      this.context = context;
      this.mbeanName = mbeanName;
      this.memberName = memberName;
    }

    @Override
    public Pair<String, Object> call() {
      try {
        return execute();
      } catch (Exception e) {
       return new Pair<>(context.getUuid(), (Object) e);
      }
    }

    /**
     * Executes the request.
     * @return a pair made of the node uuid and either the request result or an exception that was raised.
     * @throws Exception if any error occurs.
     */
    protected abstract Pair<String, Object> execute() throws Exception;
  }
}
