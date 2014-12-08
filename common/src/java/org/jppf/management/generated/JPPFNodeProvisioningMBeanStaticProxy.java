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

package org.jppf.management.generated;

import org.jppf.management.AbstractMBeanStaticProxy;
import org.jppf.management.JMXConnectionWrapper;
import org.jppf.node.provisioning.JPPFNodeProvisioningMBean;
import org.jppf.utils.TypedProperties;

/**
 * Generated static proxy for the {@link org.jppf.node.provisioning.JPPFNodeProvisioningMBean} MBean interface.
 * @author /common/src/java/org/jppf/utils/generator/MBeanStaticProxyGenerator.java
 */
public class JPPFNodeProvisioningMBeanStaticProxy extends AbstractMBeanStaticProxy implements JPPFNodeProvisioningMBean {
  /**
   * Initialize this MBean static proxy.
   * @param connection the JMX connection used to invoke remote MBean methods.
   */
  public JPPFNodeProvisioningMBeanStaticProxy(final JMXConnectionWrapper connection) {
    super(connection, "org.jppf:name=provisioning,type=node");
  }

  @Override
  public void provisionSlaveNodes(final int param0, final TypedProperties param1) {
    invoke("provisionSlaveNodes", new Object[] { param0, param1 }, new String[] { "int", "org.jppf.utils.TypedProperties" });
  }

  @Override
  public void provisionSlaveNodes(final int param0, final boolean param1, final TypedProperties param2) {
    invoke("provisionSlaveNodes", new Object[] { param0, param1, param2 }, new String[] { "int", "boolean", "org.jppf.utils.TypedProperties" });
  }

  @Override
  public void provisionSlaveNodes(final int param0) {
    invoke("provisionSlaveNodes", new Object[] { param0 }, new String[] { "int" });
  }

  @Override
  public void provisionSlaveNodes(final int param0, final boolean param1) {
    invoke("provisionSlaveNodes", new Object[] { param0, param1 }, new String[] { "int", "boolean" });
  }

  @Override
  public int getNbSlaves() {
    return (int) getAttribute("NbSlaves");
  }
}
