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
 *
 * @author Laurent Cohen
 */
public class DriverMBeanProviderManager extends AbstractMBeanProviderManager<JPPFDriverMBeanProvider> {
  /**
   * Initialize this mbean provider manager and register the MBeans implementing the specified provider interface.
   * @param clazz the class object for the provider interface.
   * @param cl the class loader used to oad the MBean implementation classes.
   * @param createParams the parameters used to create the MBean implementations.
   * @param server the MBean server on which to register.
   * @throws Exception if the registration failed.
   */
  public DriverMBeanProviderManager(final Class<JPPFDriverMBeanProvider> clazz, final ClassLoader cl, final Object server, final Object... createParams) throws Exception {
    super(clazz, cl, server, createParams);
  }

  @Override
  protected Object createMBeanImpl(final JPPFDriverMBeanProvider provider, final Object...params) throws Exception {
    return provider.createMBean((JPPFDriver) params[0]);
  }
}
