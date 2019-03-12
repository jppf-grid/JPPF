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

import org.jppf.server.JPPFDriver;

/**
 * Service provider interface for pluggable management beans for JPPF drivers.
 * @author Laurent Cohen
 */
public interface JPPFDriverMBeanProvider extends JPPFMBeanProvider {
  /**
   * Return a concrete MBean. The class of this MBean must implement the interface defined by {@link JPPFMBeanProvider#getMBeanInterfaceName() getMBeanInterfaceName()}.
   * @return an {@code Object} that is an implementation of the MBean interface.
   */
  Object createMBean();

  /**
   * Return a concrete MBean. The class of this MBean must implement the interface defined by {@link JPPFMBeanProvider#getMBeanInterfaceName() getMBeanInterfaceName()}.
   * <p>This default implementation simply calls {@link #createMBean()} without using the {@code driver} parameter.
   * @param driver a reference to the JPPF driver that registered this mbean. MBean provider implementations that wish to use the
   * {@link JPPFDriver} API should override this method.
   * @return an {@code Object} that is an implementation of the MBean interface.
   */
  default Object createMBean(@SuppressWarnings("unused") final JPPFDriver driver) {
    return createMBean();
  }
}
