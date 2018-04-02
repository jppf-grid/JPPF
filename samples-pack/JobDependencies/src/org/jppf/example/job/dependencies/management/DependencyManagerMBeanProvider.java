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

package org.jppf.example.job.dependencies.management;

import org.jppf.management.spi.JPPFDriverMBeanProvider;

/**
 * Implemention of the Service Provider Interface (SPI) for a pluggable server MBean
 * which managages the job dependency graph. 
 * @author Laurent Cohen
 */
public class DependencyManagerMBeanProvider implements JPPFDriverMBeanProvider {
  @Override
  public String getMBeanInterfaceName() {
    return DependencyManagerMBean.class.getName();
  }

  @Override
  public String getMBeanName() {
    return DependencyManagerMBean.MBEAN_NAME;
  }

  @Override
  public Object createMBean() {
    return new DependencyManager();
  }
}
