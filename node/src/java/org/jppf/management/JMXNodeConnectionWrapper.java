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

package org.jppf.management;

import java.util.Map;

import org.jppf.classloader.DelegationModel;
import org.slf4j.*;

/**
 * Node-specific connection wrapper, implementing a user-friendly interface for the monitoring
 * and management of the node.
 * @author Laurent Cohen
 */
public class JMXNodeConnectionWrapper extends JMXConnectionWrapper implements JPPFNodeAdminMBean
{
  /**
   * Logger for this class.
   */
  private static Logger log = LoggerFactory.getLogger(JMXNodeConnectionWrapper.class);
  /**
   * Determines whether debug log statements are enabled.
   */
  private static boolean debugEnabled = log.isDebugEnabled();

  /**
   * Initialize a local connection to the MBean server.
   */
  public JMXNodeConnectionWrapper()
  {
    local = true;
  }

  /**
   * Initialize the connection to the remote MBean server.
   * @param host the host the server is running on.
   * @param port the RMI port used by the server.
   */
  public JMXNodeConnectionWrapper(final String host, final int port)
  {
    //super(host, port, JPPFAdminMBean.NODE_SUFFIX);
    this(host, port, false);
  }

  /**
   * Initialize the connection to the remote MBean server.
   * @param host the host the server is running on.
   * @param port the port used by the server.
   * @param secure specifies whether the connection should be established over SSL/TLS.
   */
  public JMXNodeConnectionWrapper(final String host, final int port, final boolean secure)
  {
    //super(host, port, JPPFAdminMBean.NODE_SUFFIX);
    super(host, port, secure);
    local = false;
  }

  /**
   * Get the latest state information from the node.
   * @return a <code>JPPFNodeState</code> information.
   * @throws Exception if an error occurs while invoking the Node MBean.
   * @see org.jppf.management.JPPFNodeAdminMBean#state()
   */
  @Override
  public JPPFNodeState state() throws Exception
  {
    return (JPPFNodeState) invoke(JPPFNodeAdminMBean.MBEAN_NAME,	"state", (Object[]) null, (String[]) null);
  }

  /**
   * Set the size of the node's thread pool.
   * @param size the size as an int.
   * @throws Exception if an error occurs while invoking the Node MBean.
   * @see org.jppf.management.JPPFNodeAdminMBean#updateThreadPoolSize(java.lang.Integer)
   */
  @Override
  public void updateThreadPoolSize(final Integer size) throws Exception
  {
    invoke(JPPFNodeAdminMBean.MBEAN_NAME, "updateThreadPoolSize", new Object[] { size }, new String[] { "java.lang.Integer" });
  }

  /**
   * Get detailed information about the node's JVM properties, environment variables
   * and runtime information such as memory usage and available processors.
   * @return a <code>JPPFSystemInformation</code> instance.
   * @throws Exception if an error occurs while invoking the Node MBean.
   * @see org.jppf.management.JPPFNodeAdminMBean#systemInformation()
   */
  @Override
  public JPPFSystemInformation systemInformation() throws Exception
  {
    return (JPPFSystemInformation) invoke(JPPFNodeAdminMBean.MBEAN_NAME,	"systemInformation", (Object[]) null, (String[]) null);
  }

  /**
   * Shutdown the node.
   * @throws Exception if an error is raised when invoking the node mbean.
   * @see org.jppf.management.JPPFNodeAdminMBean#shutdown()
   */
  @Override
  public void shutdown() throws Exception
  {
    if (debugEnabled) log.debug("node " + this + " shutdown requested");
    invoke(JPPFNodeAdminMBean.MBEAN_NAME, "shutdown", (Object[]) null, (String[]) null);
  }

  /**
   * Restart the node.
   * @throws Exception if an error is raised when invoking the node mbean.
   * @see org.jppf.management.JPPFNodeAdminMBean#restart()
   */
  @Override
  public void restart() throws Exception
  {
    if (debugEnabled) log.debug("node " + this + " restart requested");
    invoke(JPPFNodeAdminMBean.MBEAN_NAME, "restart", (Object[]) null, (String[]) null);
  }

  /**
   * Reset the node's executed tasks counter to zero.
   * @throws Exception if an error is raised when invoking the node mbean.
   * @see org.jppf.management.JPPFNodeAdminMBean#resetTaskCounter()
   */
  @Override
  public void resetTaskCounter() throws Exception
  {
    invoke(JPPFNodeAdminMBean.MBEAN_NAME, "resetTaskCounter", (Object[]) null, (String[]) null);
  }

  /**
   * Set the node's executed tasks counter to the specified value.
   * @param n - the new value of the task counter.
   * @throws Exception if an error is raised when invoking the node mbean.
   * @see org.jppf.management.JPPFNodeAdminMBean#setTaskCounter(java.lang.Integer)
   */
  @Override
  public void setTaskCounter(final Integer n) throws Exception
  {
    setAttribute(JPPFNodeAdminMBean.MBEAN_NAME, "TaskCounter", n);
  }

  /**
   * Update the priority of all execution threads.
   * @param newPriority the new priority to set.
   * @throws Exception if an error is raised when invoking the node mbean.
   * @see org.jppf.management.JPPFNodeAdminMBean#updateThreadsPriority(java.lang.Integer)
   */
  @Override
  public void updateThreadsPriority(final Integer newPriority) throws Exception
  {
    invoke(JPPFNodeAdminMBean.MBEAN_NAME, "updateThreadsPriority", new Object[] { newPriority }, new String[] { "java.lang.Integer" });
  }

  /**
   * Update the configuration properties of the node.
   * @param config the set of properties to update.
   * @param reconnect specifies whether the node should reconnect ot the driver after updating the properties.
   * @throws Exception if an error is raised when invoking the node mbean.
   * @see org.jppf.management.JPPFNodeAdminMBean#updateConfiguration(java.util.Map, java.lang.Boolean)
   */
  @Override
  public void updateConfiguration(final Map config, final Boolean reconnect) throws Exception
  {
    invoke(JPPFNodeAdminMBean.MBEAN_NAME, "updateConfiguration",
        new Object[] { config, reconnect }, new String[] { "java.util.Map", "java.lang.Boolean" });
  }

  /**
   * Cancel the job with the specified id.
   * @param jobUuid the id of the job to cancel.
   * @param requeue true if the job should be requeued on the server side, false otherwise.
   * @throws Exception if any error occurs.
   * @see org.jppf.management.JPPFNodeAdminMBean#cancelJob(java.lang.String,java.lang.Boolean)
   */
  @Override
  public void cancelJob(final String jobUuid, final Boolean requeue) throws Exception
  {
    invoke(JPPFNodeAdminMBean.MBEAN_NAME, "cancelJob", new Object[] { jobUuid, requeue }, new String[] { "java.lang.String", "java.lang.Boolean" });
  }

  @Override
  public DelegationModel getDelegationModel() throws Exception
  {
    return (DelegationModel) getAttribute(JPPFNodeAdminMBean.MBEAN_NAME, "DelegationModel");
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void setDelegationModel(final DelegationModel model) throws Exception
  {
    setAttribute(JPPFNodeAdminMBean.MBEAN_NAME, "DelegationModel", model);
  }
}
