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

package org.jppf.node;

import org.jppf.JPPFUnsupportedOperationException;
import org.jppf.classloader.AbstractJPPFClassLoader;
import org.jppf.management.*;
import org.jppf.node.event.LifeCycleEventHandler;
import org.jppf.serialization.*;
import org.jppf.utils.ThreadSynchronization;

/**
 * Abstract implementation of the {@link Node} interface.
 * @author Laurent Cohen
 */
public abstract class AbstractNode extends ThreadSynchronization implements NodeInternal {
  /**
   * Utility for deserialization and serialization.
   * @exclude
   */
  protected SerializationHelper helper = null;
  /**
   * Utility for deserialization and serialization.
   * @exclude
   */
  protected ObjectSerializer serializer = null;
  /**
   * Total number of tasks executed.
   * @exclude
   */
  private int taskCount = 0;
  /**
   * Used to synchronize access to the task count.
   */
  private final Object taskCountLock = new Object();
  /**
   * This node's universal identifier.
   * @exclude
   */
  protected String uuid = null;
  /**
   * This node's system information.
   * @exclude
   */
  protected JPPFSystemInformation systemInformation = null;
  /**
   * Get the connection used by this node.
   * @exclude
   */
  protected NodeConnection<?> nodeConnection = null;

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
    this.systemInformation = new JPPFSystemInformation(uuid, isLocal(), true);
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
}
