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

package org.jppf.management;

import java.util.Map;

import org.jppf.classloader.DelegationModel;
import org.jppf.management.diagnostics.DiagnosticsMBean;
import org.jppf.node.provisioning.JPPFNodeProvisioningMBean;
import org.jppf.utils.*;
import org.slf4j.*;

/**
 * Node-specific JMX connection wrapper, implementing a user-friendly interface for the monitoring and management of a node.
 *  Note that this class implements the interface {@link org.jppf.management.JPPFNodeAdminMBean JPPFNodeAdminMBean}.
 * @author Laurent Cohen
 */
public class JMXNodeConnectionWrapper extends JMXConnectionWrapper implements JPPFNodeAdminMBean {
  /**
   * Logger for this class.
   */
  private static Logger log = LoggerFactory.getLogger(JMXNodeConnectionWrapper.class);
  /**
   * Determines whether debug log statements are enabled.
   */
  private static boolean debugEnabled = LoggingUtils.isDebugEnabled(log);

  /**
   * Initialize a local connection to the MBean server.
   */
  public JMXNodeConnectionWrapper() {
    local = true;
  }

  /**
   * Initialize the connection to the remote MBean server.
   * @param host the host the server is running on.
   * @param port the RMI port used by the server.
   */
  public JMXNodeConnectionWrapper(final String host, final int port) {
    this(host, port, false);
  }

  /**
   * Initialize the connection to the remote MBean server.
   * @param host the host the server is running on.
   * @param port the port used by the server.
   * @param secure specifies whether the connection should be established over SSL/TLS.
   */
  public JMXNodeConnectionWrapper(final String host, final int port, final boolean secure) {
    super(host, port, secure);
    local = false;
  }

  /**
   * Get the latest state information from the node.
   * @return a <code>JPPFNodeState</code> information.
   * @throws Exception if an error occurs while invoking the Node MBean.
   */
  @Override
  public JPPFNodeState state() throws Exception {
    return (JPPFNodeState) invoke(JPPFNodeAdminMBean.MBEAN_NAME,	"state", (Object[]) null, (String[]) null);
  }

  /**
   * Set the size of the node's thread pool.
   * @param size the size as an int.
   * @throws Exception if an error occurs while invoking the Node MBean.
   */
  @Override
  public void updateThreadPoolSize(final Integer size) throws Exception {
    invoke(JPPFNodeAdminMBean.MBEAN_NAME, "updateThreadPoolSize", new Object[] { size }, new String[] { "java.lang.Integer" });
  }

  /**
   * Get detailed information about the node's JVM properties, environment variables
   * and runtime information such as memory usage and available processors.
   * @return a <code>JPPFSystemInformation</code> instance.
   * @throws Exception if an error occurs while invoking the Node MBean.
   */
  @Override
  public JPPFSystemInformation systemInformation() throws Exception {
    return (JPPFSystemInformation) invoke(JPPFNodeAdminMBean.MBEAN_NAME,	"systemInformation", (Object[]) null, (String[]) null);
  }

  /**
   * Shutdown the node.
   * @throws Exception if an error is raised when invoking the node mbean.
   */
  @Override
  public void shutdown() throws Exception {
    if (debugEnabled) log.debug("node " + this + " shutdown requested");
    invoke(JPPFNodeAdminMBean.MBEAN_NAME, "shutdown", (Object[]) null, (String[]) null);
  }

  /**
   * {@inheritDoc}
   * @since 5.0
   */
  @Override
  public void shutdown(final Boolean interruptIfRunning) throws Exception {
    if (debugEnabled) log.debug("node {} shutdown requested with interruptIfRunning = {}", this, interruptIfRunning);
    invoke(JPPFNodeAdminMBean.MBEAN_NAME, "shutdown", new Object[] {interruptIfRunning}, new String[] {Boolean.class.getName()});
  }

  /**
   * Restart the node.
   * @throws Exception if an error is raised when invoking the node mbean.
   */
  @Override
  public void restart() throws Exception {
    if (debugEnabled) log.debug("node " + this + " restart requested");
    invoke(JPPFNodeAdminMBean.MBEAN_NAME, "restart", (Object[]) null, (String[]) null);
  }

  /**
   * {@inheritDoc}
   * @since 5.0
   */
  @Override
  public void restart(final Boolean interruptIfRunning) throws Exception {
    if (debugEnabled) log.debug("node {} restart requested with interruptIfRunning = {}", this, interruptIfRunning);
    invoke(JPPFNodeAdminMBean.MBEAN_NAME, "restart", new Object[] {interruptIfRunning}, new String[] {Boolean.class.getName()});
  }

  /**
   * Reset the node's executed tasks counter to zero.
   * @throws Exception if an error is raised when invoking the node mbean.
   */
  @Override
  public void resetTaskCounter() throws Exception {
    invoke(JPPFNodeAdminMBean.MBEAN_NAME, "resetTaskCounter", (Object[]) null, (String[]) null);
  }

  /**
   * Set the node's executed tasks counter to the specified value.
   * @param n the new value of the task counter.
   * @throws Exception if an error is raised when invoking the node mbean.
   */
  @Override
  public void setTaskCounter(final Integer n) throws Exception {
    setAttribute(JPPFNodeAdminMBean.MBEAN_NAME, "TaskCounter", n);
  }

  /**
   * Update the priority of all execution threads.
   * @param newPriority the new priority to set.
   * @throws Exception if an error is raised when invoking the node mbean.
   */
  @Override
  public void updateThreadsPriority(final Integer newPriority) throws Exception {
    invoke(JPPFNodeAdminMBean.MBEAN_NAME, "updateThreadsPriority", new Object[] { newPriority }, new String[] { "java.lang.Integer" });
  }

  /**
   * Update the configuration properties of the node.
   * @param configOverrides the set of properties to update.
   * @param restart specifies whether the node should be restarted after updating the properties.
   * @throws Exception if an error is raised when invoking the node mbean.
   */
  @Override
  public void updateConfiguration(final Map<Object, Object> configOverrides, final Boolean restart) throws Exception {
    updateConfiguration(configOverrides, restart, true);
  }

  /**
   * Update the configuration properties of the node.
   * @param configOverrides the set of properties to update.
   * @param restart specifies whether the node should be restarted after updating the properties.
   * @param interruptIfRunning when {@code true}, then restart the node even if it is executing tasks, when {@code false}, then only shutdown the node when it is no longer executing.
   * This parameter only applies when the {@code restart} parameter is {@code true}.
   * @throws Exception if an error is raised when invoking the node mbean.
   * @since 5.2
   */
  @Override
  public void updateConfiguration(final Map<Object, Object> configOverrides, final Boolean restart, final Boolean interruptIfRunning) throws Exception {
    invoke(JPPFNodeAdminMBean.MBEAN_NAME, "updateConfiguration",
        new Object[] { configOverrides, restart, interruptIfRunning }, new String[] { "java.util.Map", "java.lang.Boolean", "java.lang.Boolean" });
  }

  /**
   * Cancel the job with the specified id.
   * @param jobUuid the id of the job to cancel.
   * @param requeue true if the job should be requeued on the server side, false otherwise.
   * @throws Exception if any error occurs.
   */
  @Override
  public void cancelJob(final String jobUuid, final Boolean requeue) throws Exception {
    invoke(JPPFNodeAdminMBean.MBEAN_NAME, "cancelJob", new Object[] { jobUuid, requeue }, new String[] { "java.lang.String", "java.lang.Boolean" });
  }

  @Override
  public DelegationModel getDelegationModel() throws Exception {
    return (DelegationModel) getAttribute(JPPFNodeAdminMBean.MBEAN_NAME, "DelegationModel");
  }

  @Override
  public void setDelegationModel(final DelegationModel model) throws Exception {
    setAttribute(JPPFNodeAdminMBean.MBEAN_NAME, "DelegationModel", model);
  }

  @Override
  public DiagnosticsMBean getDiagnosticsProxy() throws Exception {
    return getProxy(DiagnosticsMBean.MBEAN_NAME_NODE, DiagnosticsMBean.class);
  }

  @Override
  public NodePendingAction pendingAction() {
    try {
      return (NodePendingAction) invoke(JPPFNodeAdminMBean.MBEAN_NAME,  "pendingAction");
    } catch (Exception e) {
      if (debugEnabled) log.debug(String.format("error invoking %s on MBean %s: %s", ReflectionUtils.getCurrentMethodName(), JPPFNodeAdminMBean.MBEAN_NAME, ExceptionUtils.getStackTrace(e)));
    }
    return null;
  }

  @Override
  public boolean cancelPendingAction() {
    try {
      return (Boolean) invoke(JPPFNodeAdminMBean.MBEAN_NAME,  "hasPendingAction");
    } catch (Exception e) {
      if (debugEnabled) log.debug(String.format("error invoking %s on MBean %s: %s", ReflectionUtils.getCurrentMethodName(), JPPFNodeAdminMBean.MBEAN_NAME, ExceptionUtils.getStackTrace(e)));
    }
    return false;
  }

  /**
   * A shortcut method for {@code getProxy(JPPFNodeMaintenanceMBean.MBEAN_NAME, JPPFNodeMaintenanceMBean.class)}.
   * @return a dynamic proxy implementing the {@link JPPFNodeMaintenanceMBean} interface.
   * @throws Exception if any error occurs.
   * @since 5.2
   */
  public JPPFNodeMaintenanceMBean getJPPFNodeMaintenanceMProxy() throws Exception {
    return getProxy(JPPFNodeMaintenanceMBean.MBEAN_NAME, JPPFNodeMaintenanceMBean.class);
  }

  /**
   * A shortcut method for {@code getProxy(JPPFNodeTaskMonitorMBean.MBEAN_NAME, JPPFNodeTaskMonitorMBean.class)}.
   * @return a dynamic proxy implementing the {@link JPPFNodeTaskMonitorMBean} interface.
   * @throws Exception if any error occurs.
   * @since 5.2
   */
  public JPPFNodeTaskMonitorMBean getJPPFNodeTaskMonitorProxy() throws Exception {
    return getProxy(JPPFNodeTaskMonitorMBean.MBEAN_NAME, JPPFNodeTaskMonitorMBean.class);
  }

  /**
   * A shortcut method for {@code getProxy(JPPFNodeProvisioningMBean.MBEAN_NAME, JPPFNodeProvisioningMBean.class)}.
   * @return a dynamic proxy implementing the {@link JPPFNodeProvisioningMBean} interface.
   * @throws Exception if any error occurs.
   * @since 5.2
   */
  public JPPFNodeProvisioningMBean getJPPFNodeProvisioningProxy() throws Exception {
    return getProxy(JPPFNodeProvisioningMBean.MBEAN_NAME, JPPFNodeProvisioningMBean.class);
  }
}
