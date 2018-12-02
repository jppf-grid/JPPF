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

import java.util.concurrent.atomic.AtomicBoolean;

import org.jppf.JPPFUnsupportedOperationException;
import org.jppf.classloader.AbstractJPPFClassLoader;
import org.jppf.management.*;
import org.jppf.node.event.LifeCycleEventHandler;
import org.jppf.serialization.*;
import org.jppf.utils.concurrent.ThreadSynchronization;

/**
 * Abstract implementation of the {@link Node} interface.
 * @author Laurent Cohen
 */
public abstract class AbstractNode extends ThreadSynchronization implements NodeInternal {
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
   * Initialize this node.
   * @param uuid this node's uuid.
   */
  public AbstractNode(final String uuid) {
    this.uuid = uuid;
  }

  /**
   * {@inheritDoc}
   * @exclude
   */
  @Override
  public NodeConnection<?> getNodeConnection() {
    return nodeConnection;
  }

  /**
   * Get the total number of tasks executed.
   * @return the number of tasks as an int.
   */
  public int getTaskCount() {
    synchronized (taskCountLock) {
      return taskCount;
    }
  }

  /**
   * Set the total number of tasks executed.
   * @param taskCount the number of tasks as an int.
   * @exclude
   */
  public void setTaskCount(final int taskCount) {
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
   * Default implementation
   * @return this method always returns null.
   * @see org.jppf.node.NodeInternal#getLifeCycleEventHandler()
   * @exclude
   */
  @Override
  public LifeCycleEventHandler getLifeCycleEventHandler() {
    return null;
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
   * @exclude
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
}
