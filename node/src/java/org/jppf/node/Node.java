/*
 * JPPF.
 * Copyright (C) 2005-2014 JPPF Team.
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

import org.jppf.classloader.AbstractJPPFClassLoader;
import org.jppf.management.JPPFSystemInformation;

/**
 * Interface for a node that provides information about its activity.
 * @author Laurent Cohen
 */
public interface Node extends Runnable
{
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
   * @return <code>true</code> if this node is local, <code>false</code> otherwise.
   */
  boolean isLocal();

  /**
   * Reset the current task class loader if any is present (i.e. if a job is being executed), without reconnecting to the server.
   * @return the newly created class loader, or <code>null</code> if none could be created at this time. 
   */
  AbstractJPPFClassLoader resetTaskClassLoader();

  /**
   * Determine whether this node is running in offline mode.
   * @return <code>true</code> if this node is offline, <code>false</code> otherwise.
   */
  boolean isOffline();

  /**
   * Determine whether this node is a 'master' node for the provisioning features.
   * @return <code>true</code> if this node is a master, <code>false</code> otherwise.
   */
  boolean isMasterNode();

  /**
   * Determine whether this node is a 'slave' node for the provisioning features.
   * @return <code>true</code> if this node is a slave, <code>false</code> otherwise.
   */
  boolean isSlaveNode();
}
