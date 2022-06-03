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

import org.jppf.management.*;
import org.jppf.utils.TypedProperties;

/**
 * Interface for a node that provides information about its activity.
 * @author Laurent Cohen
 */
public interface Node extends Runnable {
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
   * @return {@code true} if this node is local, {@code false} otherwise.
   */
  boolean isLocal();

  /**
   * Reset the current task class loader if any is present (i.e. if a job is being executed), without reconnecting to the server.
   * @param params a (possibly empty) set of arbitrary parameters to propagate to the class loader.
   * @return the newly created class loader, or {@code null} if none could be created at this time.
   * This class loader can be safely cast to an {@link org.jppf.classloader.AbstractJPPFClassLoader AbstractJPPFClassLoader}.
   */
  ClassLoader resetTaskClassLoader(final Object...params);

  /**
   * Determine whether this node is running in offline mode.
   * @return {@code true} if this node is offline, {@code false} otherwise.
   */
  boolean isOffline();

  /**
   * Determine whether this node is a 'master' node for the provisioning features.
   * @return {@code true} if this node is a master, {@code false} otherwise.
   */
  boolean isMasterNode();

  /**
   * Determine whether this node is a 'slave' node for the provisioning features.
   * @return {@code true} if this node is a slave, {@code false} otherwise.
   */
  boolean isSlaveNode();

  /**
   * Get the uuid of the node who this node is a slave of.
   * @return the uuid of this node's master node, or {@code null} if this node is not a slave.
   * @since 6.0
   */
  public String getMasterNodeUuid();

  /**
   * Determine whether this node can execute .Net tasks.
   * @return {@code true} if this node can execute .Net tasks, {@code false} otherwise.
   * @deprecated the .Net port is no longer part of JPPF, there is no replacement.
   */
  @Deprecated
  default boolean isDotnetCapable() {
    return false;
  }

  /**
   * Determine whether this node is an Android node.
   * @return {@code true} if this node runs on Android, {@code false} otherwise.
   * @deprecated the Android port is no longer part of JPPF, there is no replacement.
   */
  @Deprecated
  default boolean isAndroid() {
    return false;
  }

  /**
   * Get the configuration of this node.
   * @return the node configuration as a {@link TypedProperties} instance.
   */
  TypedProperties getConfiguration();

  /**
   * Get the management information for this node.
   * @return a {@link JPPFManagementInfo} object.
   */
  JPPFManagementInfo getManagementInfo();

  /**
   * Get the total number of tasks executed.
   * @return the number of tasks as an int.
   */
  int getExecutedTaskCount();
}
