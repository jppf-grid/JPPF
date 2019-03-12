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

package org.jppf.node;

import org.jppf.JPPFUnsupportedOperationException;
import org.jppf.execute.ExecutionManager;
import org.jppf.execute.async.AsyncExecutionManager;
import org.jppf.management.*;
import org.jppf.node.event.LifeCycleEventHandler;
import org.jppf.server.node.AbstractClassLoaderManager;

/**
 * Internal interface for methods of a node that shoudln't be exposed in the public API.
 * @author Laurent Cohen
 */
public interface NodeInternal extends Node {
  /**
   * Get the connection used by this node.
   * @return a {@link NodeConnection} instance.
   * @exclude
   */
  NodeConnection<?> getNodeConnection();

  /**
   * Stop this node and release the resources it is using.
   * @exclude
   */
  void stopNode();

  /**
   * Get the object that manages the node life cycle events.
   * @return a {@link LifeCycleEventHandler} instance.
   * @exclude
   */
  LifeCycleEventHandler getLifeCycleEventHandler();

  /**
   * Get the task execution manager for this node.
   * @return a {@link ExecutionManager} instance.
   * @exclude
   */
  AsyncExecutionManager getExecutionManager();

  /**
   * Get the JMX connector server associated with the node.
   * <p>The default implementation throws a {@link JPPFUnsupportedOperationException}. It is up to concrete implementations to override it.
   * @return a JMXServer instance.
   * @throws Exception if any error occurs.
   * @exclude
   */
  default JMXServer getJmxServer() throws Exception {
    throw new JPPFUnsupportedOperationException("getJmxServer() is not supported on this type of node");
  }

  /**
   * Initialize this node's data channel.
   * @throws Exception if an error is raised during initialization.
   * @exclude
   */
  void initDataChannel() throws Exception;

  /**
   * Initialize this node's data channel.
   * @throws Exception if an error is raised during initialization.
   * @exclude
   */
  void closeDataChannel() throws Exception;

  /**
   * Get the service that manages the class loaders and how they are used.
   * @return an {@link AbstractClassLoaderManager} instance.
   * @exclude
   */
  AbstractClassLoaderManager<?> getClassLoaderManager();

  /**
   * @return the mbean which sends notifications of configuration changes.
   * @exclude
   */
  NodeConfigNotifier getNodeConfigNotifier();
}
