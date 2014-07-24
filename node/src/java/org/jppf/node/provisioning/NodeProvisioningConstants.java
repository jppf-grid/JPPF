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

package org.jppf.node.provisioning;

/**
 * This class contains constant definitions for the names of the properties used for node provisioning.
 * @author Laurent Cohen
 * @since 4.1
 */
public final class NodeProvisioningConstants {
  /**
   * Name of the property which defines a node as master. Value defaults to {@code true}.
   */
  public static final String MASTER_PROPERTY = "jppf.node.provisioning.master";
  /**
   * Name of the property which defines a node as a slave. Vllaue defaults to {@code false}.
   */
  public static final String SLAVE_PROPERTY = "jppf.node.provisioning.slave";
  /**
   * Name of the property that specifies the path prefix used for the root directory of each slave node.
   * The value of this property defaults to the path slave_nodes/node_", relative to the master's root directory.
   */
  public static final String SLAVE_PATH_PREFIX_PROPERTY = "jppf.node.provisioning.slave.path.prefix";
  /**
   * Name of the property specifying the directory where template configuration files, other than the jppf configuration, are located.
   * The value of this property defaults to the path "config", relative to the master's root directory .
   */
  public static final String SLAVE_CONFIG_PATH_PROPERTY = "jppf.node.provisioning.slave.config.path";
  /**
   * A set of space-spearted JVM options that will always be added to the slaves' startup command,
   * even if the property "jppf.jvm.options" is already supplied.
   */
  public static final String SLAVE_JVM_OPTIONS_PROPERTY = "jppf.node.provisioning.slave.jvm.options";
  /**
   * Number of slaves to launch when the master starts up.
   * @exclude
   */
  public static final String STARTUP_SLAVES_PROPERTY = "jppf.node.provisioning.startup.slaves";

  /**
   * Instanciation not permitted.
   */
  private NodeProvisioningConstants() {
  }
}
