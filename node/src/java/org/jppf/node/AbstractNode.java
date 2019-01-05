/*
 * JPPF.
 * Copyright (C) 2005-2018 JPPF Team.
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

package org.jppf.node;

import static org.jppf.management.JPPFManagementInfo.*;

import java.util.concurrent.atomic.AtomicBoolean;

import org.jppf.JPPFUnsupportedOperationException;
import org.jppf.classloader.AbstractJPPFClassLoader;
import org.jppf.management.*;
import org.jppf.node.event.LifeCycleEventHandler;
import org.jppf.serialization.*;
import org.jppf.server.node.NodeIO;
import org.jppf.utils.*;
import org.jppf.utils.concurrent.ThreadSynchronization;
import org.jppf.utils.configuration.JPPFProperties;
import org.slf4j.*;

/**
 * Abstract implementation of the {@link Node} interface.
 * @author Laurent Cohen
 */
public abstract class AbstractNode extends ThreadSynchronization implements NodeInternal {
  /**
   * Logger for this class.
   */
  private static final Logger log = LoggerFactory.getLogger(AbstractNode.class);
  /**
   * Utility for deserialization and serialization.
   * @exclude
   */
  protected SerializationHelper helper;
  /**
   * Utility for deserialization and serialization.
   * @exclude
   */
  protected ObjectSerializer serializer;
  /**
   * Total number of tasks executed.
   * @exclude
   */
  private int taskCount;
  /**
   * Used to synchronize access to the task count.
   */
  private final Object taskCountLock = new Object();
  /**
   * This node's universal identifier.
   * @exclude
   */
  protected final String uuid;
  /**
   * This node's system information.
   * @exclude
   */
  protected JPPFSystemInformation systemInformation;
  /**
   * Get the connection used by this node.
   * @exclude
   */
  protected NodeConnection<?> nodeConnection;
  /**
   * Determines whether this node is currently shutting down.
   */
  private final AtomicBoolean shuttingDown = new AtomicBoolean(false);
  /**
   * The main node class loader.
   */
  private AbstractJPPFClassLoader jppfClassLoader;
  /**
   * The object responsible for this node's I/O.
   * @exclude
   */
  protected NodeIO nodeIO;
  /**
   * The configuration of this node.
   * @exclude
   */
  protected final TypedProperties configuration;
  /**
   * Determines whether JMX management and monitoring is enabled for this node.
   * @exclude
   */
  protected boolean jmxEnabled;
  /**
   * Determines whether this node can execute .Net tasks.
   * @exclude
   */
  protected final boolean dotnetCapable;
  /**
   * Handles the firing of node life cycle events and the listeners that subscribe to these events.
   * @exclude
   */
  protected LifeCycleEventHandler lifeCycleEventHandler;
  /**
   * The jmx server that handles administration and monitoring functions for this node.
   * @exclude
   */
  protected JMXServer jmxServer;

  /**
   * Initialize this node.
   * @param uuid this node's uuid.
   * @param configuration the configuration of this node.
   */
  public AbstractNode(final String uuid, final TypedProperties configuration) {
    this.uuid = uuid;
    this.configuration = configuration;
    jmxEnabled = configuration.get(JPPFProperties.MANAGEMENT_ENABLED);
    dotnetCapable = configuration.get(JPPFProperties.DOTNET_BRIDGE_INITIALIZED);
  }

  /**
   * {@inheritDoc}
   * @exclude
   */
  @Override
  public NodeConnection<?> getNodeConnection() {
    return nodeConnection;
  }

  @Override
  public int getExecutedTaskCount() {
    synchronized (taskCountLock) {
      return taskCount;
    }
  }

  /**
   * Set the total number of tasks executed.
   * @param taskCount the number of tasks as an int.
   * @exclude
   */
  public void setExecutedTaskCount(final int taskCount) {
    synchronized (taskCountLock) {
      this.taskCount = taskCount;
    }
  }

  /**
   * Get the utility for deserialization and serialization.
   * @return a <code>SerializationHelper</code> instance.
   * @exclude
   */
  public SerializationHelper getHelper() {
    return helper;
  }

  /**
   * {@inheritDoc}
   * <p>This implementation throws a <code>JPPFUnsupportedOperationException</code>. It is up to subclasses to implement it.
   * @exclude
   */
  @Override
  public JMXServer getJmxServer() throws Exception {
    throw new JPPFUnsupportedOperationException("getJmxServer() is not supported on this type of node");
  }

  @Override
  public String getUuid() {
    return uuid;
  }

  /**
   * Update the current system information.
   * @exclude
   */
  protected void updateSystemInformation() {
    this.systemInformation = new JPPFSystemInformation(getConfiguration(), uuid, isLocal(), true);
  }

  @Override
  public JPPFSystemInformation getSystemInformation() {
    return systemInformation;
  }

  /**
   * This implementation does nothing.
   * @param params not used.
   * @return {@code null}.
   */
  @Override
  public AbstractJPPFClassLoader resetTaskClassLoader(final Object...params) {
    return null;
  }

  @Override
  public boolean isAndroid() {
    return false;
  }

  /**
   * Determine whether this node is currently shutting down.
   * @return an {@link AtomicBoolean} instance whose value is {@code true</code> if the node is shutting down, <code>false} otherwise.
   * @exclude
   */
  public AtomicBoolean getShuttingDown() {
    return shuttingDown;
  }

  /**
   * @return the main node class loader.
   * @exclude
   */
  public AbstractJPPFClassLoader getJPPFClassLoader() {
    return jppfClassLoader;
  }

  /**
   * 
   * @param jppfClassLoader the main node class loader.
   * @exclude
   */
  public void setJPPFClassLoader(final AbstractJPPFClassLoader jppfClassLoader) {
    this.jppfClassLoader = jppfClassLoader;
  }

  @Override
  public JPPFManagementInfo getManagementInfo() {
    try {
      final JMXServer server = getJmxServer();
      final int type = NODE
        | (isMasterNode() ? MASTER : (isSlaveNode() ? SLAVE : 0))
        | (isAndroid() ? ANDROID : 0)
        | (isDotnetCapable() ? DOTNET : 0)
        | (isLocal() ? LOCAL :0);
      final String host = server.getManagementHost();
      final String ip = (getConfiguration().get(JPPFProperties.RESOLVE_ADDRESSES)) ? NetworkUtils.getHostIP(host).ipAddress() : host;
      return new JPPFManagementInfo(host, ip, server.getManagementPort(), getUuid(), type, getConfiguration().get(JPPFProperties.SSL_ENABLED), getMasterNodeUuid());
    } catch (final Exception e) {
      log.error(e.getMessage(), e);
    }
    return null;
  }

  @Override
  public TypedProperties getConfiguration() {
    return configuration;
  }

  /**
   * Determines whether JMX management and monitoring is enabled for this node.
   * @return true if JMX is enabled, false otherwise.
   * @exclude
   */
  protected boolean isJmxEnabled() {
    return jmxEnabled && !isOffline();
  }

  @Override
  public boolean isDotnetCapable() {
    return dotnetCapable;
  }

  /**
   * @exclude
   */
  @Override
  public LifeCycleEventHandler getLifeCycleEventHandler() {
    return lifeCycleEventHandler;
  }

  @Override
  public boolean isMasterNode() {
    return !isOffline() && (systemInformation != null) && systemInformation.getJppf().get(JPPFProperties.PROVISIONING_MASTER);
  }

  @Override
  public boolean isSlaveNode() {
    return (systemInformation != null) && systemInformation.getJppf().get(JPPFProperties.PROVISIONING_SLAVE);
  }

  @Override
  public String getMasterNodeUuid() {
    if (systemInformation == null) return null;
    return systemInformation.getJppf().get(JPPFProperties.PROVISIONING_MASTER_UUID);
  }
}
