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

import org.jppf.comm.socket.SocketWrapper;
import org.jppf.management.*;
import org.jppf.node.event.LifeCycleEventHandler;

/**
 * Interface for a node that provides information about its activity.
 * @author Laurent Cohen
 */
public interface Node extends Runnable
{
  /**
   * Get the underlying socket used by this node.
   * @return a SocketWrapper instance.
   */
  SocketWrapper getSocketWrapper();

  /**
   * Set the underlying socket to be used by this node.
   * @param socketWrapper a SocketWrapper instance.
   */
  void setSocketWrapper(SocketWrapper socketWrapper);

  /**
   * Stop this node and release the resources it is using.
   */
  void stopNode();

  /**
   * Get the object that manages the node life cycle events.
   * @return a {@link LifeCycleEventHandler} instance.
   */
  LifeCycleEventHandler getLifeCycleEventHandler();

  /**
   * Get the JMX connecter server aossicated with the node.
   * @return a JMXServer instance.
   * @throws Exception if any error occurs.
   */
  JMXServer getJmxServer() throws Exception;

  /**
   * Get this node's UUID.
   * @return the uuid as a string.
   */
  String getUuid();

  /**
   * Get the system information for this node.
   * @return a {@link JPPFSystemInformation} instance.
   */
  JPPFSystemInformation getSystemInformation();
  /**
   * Determine whether this node is local to another component.
   * @return true if this node is local, false otherwise.
   */
  boolean isLocal();
}
