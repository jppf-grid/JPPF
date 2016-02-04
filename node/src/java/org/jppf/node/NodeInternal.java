/*
 * JPPF.
 * Copyright (C) 2005-2016 JPPF Team.
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

import org.jppf.execute.ExecutionManager;
import org.jppf.management.JMXServer;
import org.jppf.node.event.LifeCycleEventHandler;

/**
 * Internal interface for methods of a node that shoudln't be exposed in the public API.
 * @author Laurent Cohen
 * @exclude
 */
public interface NodeInternal extends Node
{
  /**
   * Get the connection used by this node.
   * @return a {@link NodeConnection} instance.
   */
  NodeConnection getNodeConnection();

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
   * Get the task execution manager for this node.
   * @return a <code>NodeExecutionManager</code> instance.
   */
  ExecutionManager getExecutionManager();

    /**
   * Get the JMX connector server associated with the node.
   * @return a JMXServer instance.
   * @throws Exception if any error occurs.
   */
  JMXServer getJmxServer() throws Exception;
}
