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

package org.jppf.management;

import java.io.Serializable;

import org.jppf.management.doc.MBeanDescription;

/**
 * Management interface for the administration of a JPPF component, driver or node.
 * @author Laurent Cohen
 */
public interface JPPFAdminMBean extends Serializable {
  /**
   * Get detailed information about the node's JVM properties, environment variables
   * and runtime information such as memory usage and available processors.
   * @return a <code>JPPFSystemInformation</code> instance.
   * @throws Exception if any error occurs.
   */
  @MBeanDescription("get detailed information on the system where the JPPF server or node is runnning: " +
    "environement variables, JVM system properties, JPPF configuration, runtime information, storage details, network interfaces, statistics")
  JPPFSystemInformation systemInformation() throws Exception;
}
