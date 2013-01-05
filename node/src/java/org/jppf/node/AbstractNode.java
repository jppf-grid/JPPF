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

package org.jppf.node;

import org.jppf.JPPFUnsupportedOperationException;
import org.jppf.comm.socket.*;
import org.jppf.management.*;
import org.jppf.node.event.LifeCycleEventHandler;
import org.jppf.utils.*;

/**
 * Abstract implementation of the {@link Node} interface.
 * @author Laurent Cohen
 */
public abstract class AbstractNode extends ThreadSynchronization implements Node, Runnable
{
  /**
   * Utility for deserialization and serialization.
   */
  protected SerializationHelper helper = null;
  /**
   * Utility for deserialization and serialization.
   */
  protected ObjectSerializer serializer = null;
  /**
   * Wrapper around the underlying server connection.
   */
  protected SocketWrapper socketClient = null;
  /**
   * Used to synchronize access to the underlying socket from multiple threads.
   */
  protected SocketInitializer socketInitializer = new SocketInitializerImpl();
  /**
   * Total number of tasks executed.
   */
  private int taskCount = 0;
  /**
   * This node's universal identifier.
   */
  protected String uuid = null;
  /**
   * This node's system information.
   */
  protected JPPFSystemInformation systemInformation = null;

  /**
   * Get the underlying socket wrapper used by this node.
   * @return a <code>SocketWrapper</code> instance.
   */
  @Override
  public SocketWrapper getSocketWrapper()
  {
    return socketClient;
  }

  /**
   * Get the underlying socket wrapper used by this node.
   * @param wrapper a <code>SocketWrapper</code> instance.
   */
  @Override
  public void setSocketWrapper(final SocketWrapper wrapper)
  {
    this.socketClient = wrapper;
  }

  /**
   * Stop this node and release the resources it is using.
   * @see org.jppf.node.Node#stopNode()
   */
  @Override
  public abstract void stopNode();

  /**
   * Get the total number of tasks executed.
   * @return the number of tasks as an int.
   */
  public synchronized int getTaskCount()
  {
    return taskCount;
  }

  /**
   * Set the total number of tasks executed.
   * @param taskCount the number of tasks as an int.
   */
  public synchronized void setTaskCount(final int taskCount)
  {
    this.taskCount = taskCount;
  }

  /**
   * Get the utility for deserialization and serialization.
   * @return a <code>SerializationHelper</code> instance.
   */
  public SerializationHelper getHelper()
  {
    return helper;
  }

  /**
   * Default implementation
   * @return this method always returns null.
   * @see org.jppf.node.Node#getLifeCycleEventHandler()
   */
  @Override
  public LifeCycleEventHandler getLifeCycleEventHandler()
  {
    return null;
  }

  /**
   * {@inheritDoc}
   * <p>This implemenatation throws a <code>JPPFUnsupportedOperationException</code>.
   * It is up to subclasses to implement it.
   */
  @Override
  public JMXServer getJmxServer() throws Exception
  {
    throw new JPPFUnsupportedOperationException("getJmxServer() is not supported on this type of node");
  }

  @Override
  public String getUuid()
  {
    return uuid;
  }

  @Override
  public JPPFSystemInformation getSystemInformation()
  {
    return systemInformation;
  }

  /**
   * Update the current system information.
   */
  protected void updateSystemInformation()
  {
    this.systemInformation = new JPPFSystemInformation(uuid, isLocal(), true);
  }
}
