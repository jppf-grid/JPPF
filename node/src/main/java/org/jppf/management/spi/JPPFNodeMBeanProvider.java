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

package org.jppf.management.spi;

import org.jppf.node.Node;

/**
 * Service provider interface for pluggable management beans for JPPF nodes.
 * @author Laurent Cohen
 */
public interface JPPFNodeMBeanProvider extends JPPFMBeanProvider {
  /**
   * Return a concrete MBean.<br>
   * The class of this MBean must implement the interface defined by {@link JPPFMBeanProvider#getMBeanInterfaceName() getMBeanInterfaceName()}.
   * @param node the JPPF node that is managed or monitored by the MBean.
   * @return an {@code Object} that is an implementation of the MBean interface.
   */
  Object createMBean(Node node);
}
