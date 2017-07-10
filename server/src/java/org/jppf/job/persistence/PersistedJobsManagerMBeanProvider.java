/*
 * JPPF.
 * Copyright (C) 2005-2017 JPPF Team.
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

package org.jppf.job.persistence;

import org.jppf.management.spi.JPPFDriverMBeanProvider;

/**
 * Provider for the default JPPF driver management and monitoring features.
 * @author Laurent Cohen
 * @exclude
 */
public class PersistedJobsManagerMBeanProvider implements JPPFDriverMBeanProvider {
  @Override
  public String getMBeanInterfaceName() {
    return PersistedJobsManagerMBean.class.getName();
  }

  @Override
  public Object createMBean() {
    return new PersistedJobsManager();
  }

  @Override
  public String getMBeanName() {
    return PersistedJobsManagerMBean.MBEAN_NAME;
  }
}
