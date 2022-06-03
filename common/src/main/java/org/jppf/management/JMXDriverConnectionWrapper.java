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

package org.jppf.management;

import java.lang.management.ManagementFactory;
import java.lang.reflect.Constructor;
import java.util.*;

import javax.management.*;

import org.jppf.job.JobInformation;
import org.jppf.job.persistence.PersistedJobsManagerMBean;
import org.jppf.load.balancer.LoadBalancingInformation;
import org.jppf.load.balancer.persistence.*;
import org.jppf.management.diagnostics.DiagnosticsMBean;
import org.jppf.management.forwarding.*;
import org.jppf.server.job.management.*;
import org.jppf.utils.stats.JPPFStatistics;
import org.slf4j.*;

/**
 * Driver-specific JMX connection wrapper, implementing a user-friendly interface for the monitoring
 * and management of a JPPF driver. Note that this class implements the interface {@link org.jppf.management.JPPFDriverAdminMBean JPPFDriverAdminMBean}.
 * @author Laurent Cohen
 */
public class JMXDriverConnectionWrapper extends JMXConnectionWrapper implements JPPFDriverAdminMBean {
  /**
   * Explicit serialVersionUID.
   */
  private static final long serialVersionUID = 1L;
  /**
   * Logger for this class.
   */
  private static final Logger log = LoggerFactory.getLogger(JMXDriverConnectionWrapper.class);
  /**
   * Signature of the method that registers a node forwarding listener.
   */
  private static final String[] FORWARDING_LISTENER_SIGNATURE = {NodeSelector.class.getName(), String.class.getName()};
  /**
   *
   */
  private static final Map<String, Map<String, ListenerWrapper>> forwardingListeners = new HashMap<>();

  /**
   * Initialize a local connection to the MBean server.
   */
  public JMXDriverConnectionWrapper() {
    this(ManagementFactory.getPlatformMBeanServer());
  }

  /**
   * Initialize a local connection to the MBean server.
   * @param mbeanServer a connection to the mbean server to use.
   */
  public JMXDriverConnectionWrapper(final MBeanServerConnection mbeanServer) {
    local = true;
    this.mbeanConnection.set(mbeanServer);
  }

  /**
   * Initialize a plain (non-secure) connection to the remote MBean server.
   * @param host the host the server is running on.
   * @param port the port used by the server.
   */
  public JMXDriverConnectionWrapper(final String host, final int port) {
    this(host, port, false);
  }

  /**
   * Initialize the connection to the remote MBean server.
   * @param host the host the server is running on.
   * @param port the port used by the server.
   * @param secure specifies whether the connection should be established over SSL/TLS.
   */
  public JMXDriverConnectionWrapper(final String host, final int port, final boolean secure) {
    super(host, port, secure);
    local = false;
  }

  @Override
  public Integer nbNodes() throws Exception {
    return (Integer) invoke(MBEAN_NAME, "nbNodes");
  }

  @Override
  public Integer nbNodes(final NodeSelector selector) throws Exception {
    return (Integer) invoke(MBEAN_NAME, "nbNodes", new Object[] { selector }, new String[] {NodeSelector.class.getName()});
  }

  @Override
  @SuppressWarnings("unchecked")
  public Collection<JPPFManagementInfo> nodesInformation() throws Exception {
    return (Collection<JPPFManagementInfo>) invoke(MBEAN_NAME, "nodesInformation");
  }

  @Override
  @SuppressWarnings("unchecked")
  public Collection<JPPFManagementInfo> nodesInformation(final NodeSelector selector) throws Exception {
    return (Collection<JPPFManagementInfo>) invoke(MBEAN_NAME, "nodesInformation", new Object[] { selector }, new String[] {NodeSelector.class.getName()});
  }

  @Override
  @SuppressWarnings("unchecked")
  public Collection<JPPFManagementInfo> nodesInformation(final NodeSelector selector, final boolean includePeers) throws Exception {
    return (Collection<JPPFManagementInfo>) invoke(MBEAN_NAME, "nodesInformation", new Object[] { selector, includePeers }, new String[] {NodeSelector.class.getName(), boolean.class.getName()});
  }

  @Override
  public JPPFStatistics statistics() throws Exception {
    return (JPPFStatistics) invoke(MBEAN_NAME, "statistics");
  }

  @Override
  public String restartShutdown(final Long shutdownDelay, final Long restartDelay) throws Exception {
    return (String) invoke(MBEAN_NAME, "restartShutdown", new Object[] {shutdownDelay, restartDelay}, new String[] {Long.class.getName(), Long.class.getName()});
  }

  @Override
  public String changeLoadBalancerSettings(final String algorithm, final Map<Object, Object> parameters) throws Exception {
    return (String) invoke(MBEAN_NAME, "changeLoadBalancerSettings", new Object[] {algorithm, parameters}, new String[] {String.class.getName(), Map.class.getName()});
  }

  @Override
  public LoadBalancingInformation loadBalancerInformation() throws Exception {
    return (LoadBalancingInformation) invoke(MBEAN_NAME, "loadBalancerInformation");
  }

  /**
   * Cancel the job with the specified id.
   * @param jobId the id of the job to cancel.
   * @throws Exception if any error occurs.
   */
  public void cancelJob(final String jobId) throws Exception {
    invoke(DriverJobManagementMBean.MBEAN_NAME, "cancelJob", new Object[] { jobId }, new String[] { "java.lang.String" });
  }

  /**
   * Suspend the job with the specified id.
   * @param jobId the id of the job to suspend.
   * @param requeue true if the sub-jobs running on each node should be canceled and requeued,
   * false if they should be left to execute until completion.
   * @throws Exception if any error occurs.
   */
  public void suspendJob(final String jobId, final Boolean requeue) throws Exception {
    invoke(DriverJobManagementMBean.MBEAN_NAME, "suspendJob", new Object[] { jobId, requeue }, new String[] { "java.lang.String", "java.lang.Boolean" });
  }

  /**
   * Resume the job with the specified id.
   * @param jobId the id of the job to resume.
   * @throws Exception if any error occurs.
   */
  public void resumeJob(final String jobId) throws Exception {
    invoke(DriverJobManagementMBean.MBEAN_NAME, "resumeJob", new Object[] { jobId }, new String[] { "java.lang.String" });
  }

  /**
   * Update the maximum number of nodes a node can run on.
   * @param jobId the id of the job to update.
   * @param maxNodes the new maximum number of nodes for the job.
   * @throws Exception if any error occurs.
   */
  public void updateMaxNodes(final String jobId, final Integer maxNodes) throws Exception {
    invoke(DriverJobManagementMBean.MBEAN_NAME, "updateMaxNodes", new Object[] { jobId, maxNodes }, new String[] { "java.lang.String", "java.lang.Integer" });
  }

  /**
   * Update the priority of a job.
   * @param jobId the id of the job to update.
   * @param newPriority the new priority of the job.
   * @throws Exception if any error occurs.
   */
  public void updateJobPriority(final String jobId, final Integer newPriority) throws Exception {
    invoke(DriverJobManagementMBean.MBEAN_NAME, "updatePriority", new Object[] { jobId, newPriority }, new String[] { "java.lang.String", "java.lang.Integer" });
  }

  /**
   * Get an object describing the job with the specified id.
   * @param jobId the id of the job to get information about.
   * @return an instance of <code>JobInformation</code>.
   * @throws Exception if any error occurs.
   */
  public JobInformation getJobInformation(final String jobId) throws Exception {
    return (JobInformation) invoke(DriverJobManagementMBean.MBEAN_NAME, "getJobInformation", new Object[] { jobId }, new String[] { "java.lang.String" });
  }

  /**
   * Get a list of objects describing the nodes to which the whole or part of a job was dispatched.
   * @param jobId the id of the job for which to find node information.
   * @return an array of <code>NodeManagementInfo</code>, <code>JobInformation</code> instances.
   * @throws Exception if any error occurs.
   */
  public NodeJobInformation[] getNodeInformation(final String jobId) throws Exception {
    return (NodeJobInformation[]) invoke(DriverJobManagementMBean.MBEAN_NAME, "getNodeInformation", new Object[] { jobId }, new String[] { "java.lang.String" });
  }

  @Override
  public void resetStatistics() throws Exception {
    invoke(MBEAN_NAME, "resetStatistics");
  }

  @Override
  public JPPFSystemInformation systemInformation() throws Exception {
    return (JPPFSystemInformation) invoke(MBEAN_NAME, "systemInformation");
  }

  @Override
  public Integer nbIdleNodes() throws Exception {
    return (Integer) invoke(MBEAN_NAME, "nbIdleNodes");
  }

  @Override
  public Integer nbIdleNodes(final NodeSelector selector) throws Exception {
    return (Integer) invoke(MBEAN_NAME, "nbIdleNodes", new Object[] { selector }, new String[] {NodeSelector.class.getName()});
  }

  @Override
  public Integer nbIdleNodes(final NodeSelector selector, final boolean includePeers) throws Exception {
    return (Integer) invoke(MBEAN_NAME, "nbIdleNodes", new Object[] { selector, includePeers }, new String[] { NodeSelector.class.getName(), boolean.class.getName() });
  }

  @Override
  @SuppressWarnings("unchecked")
  public Collection<JPPFManagementInfo> idleNodesInformation() throws Exception {
    return (Collection<JPPFManagementInfo>) invoke(MBEAN_NAME, "idleNodesInformation");
  }

  @Override
  @SuppressWarnings("unchecked")
  public Collection<JPPFManagementInfo> idleNodesInformation(final NodeSelector selector) throws Exception {
    return (Collection<JPPFManagementInfo>) invoke(MBEAN_NAME, "idleNodesInformation", new Object[] { selector }, new String[] {NodeSelector.class.getName()});
  }

  @Override
  public void toggleActiveState(final NodeSelector selector) throws Exception {
    invoke(MBEAN_NAME, "toggleActiveState", new Object[] {selector}, new String[] {NodeSelector.class.getName()});
  }

  @SuppressWarnings("unchecked")
  @Override
  public Map<String, Boolean> getActiveState(final NodeSelector selector) throws Exception {
    return (Map<String, Boolean>) invoke(MBEAN_NAME, "getActiveState", new Object[] {selector}, new String[] {NodeSelector.class.getName()});
  }

  @Override
  public void setActiveState(final NodeSelector selector, final boolean active) throws Exception {
    invoke(MBEAN_NAME, "setActiveState", new Object[] {selector, active}, new String[] {NodeSelector.class.getName(), boolean.class.getName()});
  }

  @Override
  public void setBroadcasting(final boolean broadcasting) throws Exception {
    setAttribute(MBEAN_NAME, "Broadcasting", broadcasting);
  }

  @Override
  public boolean getBroadcasting() throws Exception {
    return (boolean) getAttribute(MBEAN_NAME, "Broadcasting");
  }

  /**
   * Register a notification listener which will receive notifications from the specified MBean on the selected nodes.
   * @param selector determines which nodes will be selected.
   * @param mBeanName the name of the MBean from which to receive notificaztions from the selected nodes.
   * @param listener the listener to register.
   * @param filter the notification filter.
   * @param handback the handback object.
   * @return the id of the registered listener, to use with {@link #unregisterForwardingNotificationListener(String)}.
   * @throws Exception if any error occurs.
   */
  public String registerForwardingNotificationListener(final NodeSelector selector, final String mBeanName,
      final NotificationListener listener, final NotificationFilter filter, final Object handback) throws Exception {
    final String listenerID = (String) invoke(NodeForwardingMBean.MBEAN_NAME, "registerForwardingNotificationListener", new Object[] {selector, mBeanName}, FORWARDING_LISTENER_SIGNATURE);
    final InternalNotificationFilter internalFilter = new InternalNotificationFilter(listenerID, filter);
    addNotificationListener(NodeForwardingMBean.MBEAN_NAME, listener, internalFilter, handback);
    synchronized(forwardingListeners) {
      Map<String, ListenerWrapper> map = forwardingListeners.get(getId());
      if (map == null) {
        map = new HashMap<>();
        forwardingListeners.put(getId(), map);
      }
      map.put(listenerID, new ListenerWrapper(listener, internalFilter, handback));
    }
    return listenerID;
  }

  /**
   * Unregister a previously registered forwarding notification listeners.
   * @param listenerID the id of a listener previously registered with {@link #registerForwardingNotificationListener(NodeSelector,String,NotificationListener,NotificationFilter,Object)}.
   * @throws Exception if the listener with this id was not found or if any other error occurss.
   */
  public void unregisterForwardingNotificationListener(final String listenerID) throws Exception {
    synchronized(forwardingListeners) {
      final Map<String, ListenerWrapper> map = forwardingListeners.get(getId());
      if (map != null) {
        final ListenerWrapper wrapper = map.get(listenerID);
        if (wrapper != null) {
          map.remove(listenerID);
          if (map.isEmpty()) forwardingListeners.remove(getId());
          try {
            removeNotificationListener(NodeForwardingMBean.MBEAN_NAME, wrapper.getListener(), wrapper.getFilter(), wrapper.getHandback());
          } catch (final Exception e) {
            log.error(e.getMessage(), e);
          }
        }
      }
    }
    invoke(NodeForwardingMBean.MBEAN_NAME, "unregisterForwardingNotificationListener", new Object[] {listenerID}, new String[] {String.class.getName()});
  }

  /**
   * Uneregister all previously registered forwarding notification listener.
   * @return a list of the ids of the listeners that were unregistered, if any. This list may be empty but never {@code null}. 
   * @throws Exception if any error occurs.
   */
  public List<String> unregisterAllForwardingNotificationListeners() throws Exception {
    final List<String> result = new ArrayList<>();
    synchronized(forwardingListeners) {
      final Map<String, ListenerWrapper> map = forwardingListeners.remove(getId());
      if (map != null) {
        for (final Map.Entry<String, ListenerWrapper> entry: map.entrySet()) {
          final String listenerID = entry.getKey();
          result.add(listenerID);
          final ListenerWrapper wrapper = entry.getValue();
          try {
            removeNotificationListener(NodeForwardingMBean.MBEAN_NAME, wrapper.getListener(), wrapper.getFilter(), wrapper.getHandback());
          } catch (final Exception e) {
            log.error(e.getMessage(), e);
          }
          invoke(NodeForwardingMBean.MBEAN_NAME, "unregisterForwardingNotificationListener", new Object[] {listenerID}, new String[] {String.class.getName()});
        }
      }
    }
    return result;
  }

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
   * @deprecated use {@link #getForwarder()}{@link NodeForwardingMBean#forwardInvoke(NodeSelector, String, String, Object[], String[]) .forwardInvoke(selector, name, methodName, params, signature)} instead.
   * Thsis method always returns {@code null}.
   */
  public Map<String, Object> forwardInvoke(final NodeSelector selector, final String name, final String methodName, final Object[] params, final String[] signature) throws Exception {
    return null;
  }

  /**
   * Convenience method to invoke an MBean method that has no parameter.
   * <br/>This is equivalent to calling <code>forwardInvoke(selector, name, methodName, (Object[]) null, (String[]) null)</code>.
   * @param selector a filter on the nodes attached to the driver, determines the nodes to which this method applies.
   * @param name the name of the node MBean to invoke.
   * @param methodName the name of the method to invoke.
   * @return a mapping of node uuids to the result of invoking the MBean method on the corresponding node. Each result may be an exception.
   * <br/>Additionally, each result may be <code>null</code>, in particular if the invoked method has a <code>void</code> return type.
   * @throws Exception if the invocation failed.
   * @deprecated use {@link #getForwarder()}{@link NodeForwardingMBean#forwardInvoke(NodeSelector, String, String) .forwardInvoke(selector, name, methodName)} instead.
   * Thsis method always returns {@code null}.
   */
  public Map<String, Object> forwardInvoke(final NodeSelector selector, final String name, final String methodName) throws Exception {
    return null;
  }

  /**
   * Get the value of an attribute of the specified MBean for each specified node.
   * @param selector a filter on the nodes attached tot he driver, determines the nodes to which this method applies.
   * @param name the name of the MBean to invoke for each node.
   * @param attribute the name of the MBean attribute to read.
   * @return a mapping of node uuids to the result of getting the MBean attribute on the corresponding node. Each result may be an exception.
   * @throws Exception if the invocation failed.
   * @deprecated use {@link #getForwarder()}{@link NodeForwardingMBean#forwardGetAttribute(NodeSelector, String, String) .forwardGetAttribute(selector, name, attribute)} instead.
   * Thsis method always returns {@code null}.
   */
  public Map<String, Object> forwardGetAttribute(final NodeSelector selector, final String name, final String attribute) throws Exception {
    return null;
  }

  /**
   * Set the value of an attribute of the specified MBean on the specified nodes attached to the driver.
   * @param selector a filter on the nodes attached to the driver, determines the nodes to which this method applies.
   * @param name the name of the MBean to invoke for each node.
   * @param attribute the name of the MBean attribute to set.
   * @param value the value to set on the attribute.
   * @return a mapping of node uuids to an eventual exception resulting from setting the MBean attribute on the corresponding node.
   * This map may be empty if no exception was raised.
   * @throws Exception if the invocation failed.
   * @deprecated use {@link #getForwarder()}{@link NodeForwardingMBean#forwardSetAttribute(NodeSelector, String, String, Object) .forwardSetAttribute(selector, name, attribute, value)} instead.
   * Thsis method always returns {@code null}.
   */
  public Map<String, Object> forwardSetAttribute(final NodeSelector selector, final String name, final String attribute, final Object value) throws Exception {
    return null;
  }

  /**
   * This convenience method creates a proxy to the driver's mbean which forwards requests to its nodes.
   * It is equivalent to calling the more cumbersome {@code getProxy(NodeForwardingMBean.MBEAN_NAME, NodeForwardingMBean.class)}.
   * @return an instance of {@link NodeForwardingMBean}.
   * @throws Exception if a proxy could not be created for any reason.
   * @since 4.2
   */
  public NodeForwardingMBean getForwarder() throws Exception {
    return getProxy(NodeForwardingMBean.MBEAN_NAME, NodeForwardingMBean.class);
  }

  /**
   * This convenience method creates a proxy to the driver's mbean which forwards requests to its nodes.
   * It is equivalent to calling the more cumbersome {@code getProxy(JPPFNodeForwardingMBean.MBEAN_NAME, JPPFNodeForwardingMBean.class)}.
   * @return an instance of {@link JPPFNodeForwardingMBean}.
   * @throws Exception if a proxy could not be created for any reason.
   * @since 4.2
   * @deprecated use {@link #getForwarder()} instead. Thsis method always returns {@code null}.
   */
  public JPPFNodeForwardingMBean getNodeForwarder() throws Exception {
    return null;
  }

  /**
   * This convenience method creates a proxy to the driver's mbean which manages and monitors jobs.
   * It is equivalent to calling the more cumbersome {@code getProxy(DriverJobManagementMBean.MBEAN_NAME, DriverJobManagementMBean.class)}.
   * @return an instance of {@link DriverJobManagementMBean}.
   * @throws Exception if a proxy could not be created for any reason.
   * @since 4.2
   */
  public DriverJobManagementMBean getJobManager() throws Exception {
    return getProxy(DriverJobManagementMBean.MBEAN_NAME, DriverJobManagementMBean.class);
  }

  /**
   * This convenience method creates a proxy to the driver's mbean which manages persisted jobs.
   * It is equivalent to calling the more cumbersome {@code getProxy(PersistedJobsManagerMBean.MBEAN_NAME, PersistedJobsManagerMBean.class)}.
   * @return an instance of {@link PersistedJobsManagerMBean}.
   * @throws Exception if a proxy could not be created for any reason.
   * @exclude
   */
  public PersistedJobsManagerMBean getPersistedJobsManager() throws Exception {
    return getProxy(PersistedJobsManagerMBean.MBEAN_NAME, PersistedJobsManagerMBean.class);
  }

  /**
   * This convenience method creates a proxy to the driver's mbean that manages the load-balancers persisted states.
   * It is equivalent to calling the more cumbersome {@code getProxy(LoadBalancerPersistenceManagerMBean.MBEAN_NAME, LoadBalancerPersistenceManagerMBean.class)}.
   * @return an instance of {@link PersistedJobsManagerMBean}.
   * @throws Exception if a proxy could not be created for any reason.
   * @since 6.0
   */
  public LoadBalancerPersistenceManagement getLoadBalancerPersistenceManagement() throws Exception {
    return getProxy(LoadBalancerPersistenceManagerMBean.MBEAN_NAME, LoadBalancerPersistenceManagerMBean.class);
  }

  @Override
  public DiagnosticsMBean getDiagnosticsProxy() throws Exception {
    return getProxy(DiagnosticsMBean.MBEAN_NAME_DRIVER, DiagnosticsMBean.class);
  }

  /**
   * Get a proxy to the dependency manager MBean in the driver.
   * This is a shortcut method for {@link JMXConnectionWrapper#getProxy(String, Class) getProxy(JobDependencyManagerMBean.MBEAN_NAME, JobDependencyManagerMBean.class)}.
   * @return an instance of an implementation of the {@link JobDependencyManagerMBean} interface.
   * @throws Exception if any error occurs.
   * @since 6.2
   */
  public JobDependencyManagerMBean getJobDependencyManager() throws Exception {
    return getProxy(JobDependencyManagerMBean.MBEAN_NAME, JobDependencyManagerMBean.class);
  }

  /**
   * Create a forwarding proxy for the specified node MBean.
   * @param <E> the type of mbean interface.
   * @param mbeanInterface the class of the node MBean nterface.
   * @return a forwarding proxy to the specified MBean.
   * @throws Exception if any error occurs.
   * @since 6.2
   */
  @SuppressWarnings("unchecked")
  public <E extends AbstractMBeanForwarder> E getMBeanForwarder(final Class<?> mbeanInterface) throws Exception {
    final String proxyClassName = "org.jppf.management.forwarding.generated." + mbeanInterface.getSimpleName() + "Forwarder";
    Class<?> clazz = null;
    try {
      clazz = Class.forName(proxyClassName);
    } catch (final ClassNotFoundException e) {
      throw new ClassNotFoundException("could not find a class named " + proxyClassName, e);
    }
    final Constructor<?> c = clazz.getConstructor(getClass());
    return (E) c.newInstance(this);
  }
}
