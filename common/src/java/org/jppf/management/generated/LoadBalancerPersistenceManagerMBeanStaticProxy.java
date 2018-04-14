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

package org.jppf.management.generated;

import java.util.List;
import org.jppf.load.balancer.persistence.LoadBalancerPersistenceManagerMBean;
import org.jppf.management.AbstractMBeanStaticProxy;
import org.jppf.management.JMXConnectionWrapper;

/**
 * Generated static proxy for the {@link org.jppf.load.balancer.persistence.LoadBalancerPersistenceManagerMBean} MBean interface.
 * @author /common/src/java/org/jppf/utils/generator/MBeanStaticProxyGenerator.java
 */
@SuppressWarnings({"rawtypes", "unchecked"})
public class LoadBalancerPersistenceManagerMBeanStaticProxy extends AbstractMBeanStaticProxy implements LoadBalancerPersistenceManagerMBean {
  /**
   * Initialize this MBean static proxy.
   * @param connection the JMX connection used to invoke remote MBean methods.
   */
  public LoadBalancerPersistenceManagerMBeanStaticProxy(final JMXConnectionWrapper connection) {
    super(connection, "org.jppf:name=loadBalancerPersistenceManager,type=driver");
  }

  /**
   * Get the JMX object name for this MBean static proxy.
   * @return the object name as a string.
   */
  public static final String getMBeanName() {
    return "org.jppf:name=loadBalancerPersistenceManager,type=driver";
  }

  @Override
  public boolean hasAlgorithm(final String param0, final String param1) {
    return (boolean) invoke("hasAlgorithm", new Object[] { param0, param1 }, new String[] { "java.lang.String", "java.lang.String" });
  }

  @Override
  public boolean isPersistenceEnabled() {
    return (boolean) getAttribute("PersistenceEnabled");
  }

  @Override
  public List listAlgorithms(final String param0) {
    return (List) invoke("listAlgorithms", new Object[] { param0 }, new String[] { "java.lang.String" });
  }

  @Override
  public List listAllChannels() {
    return (List) invoke("listAllChannels", (Object[]) null, (String[]) null);
  }

  @Override
  public List listAllChannelsWithAlgorithm(final String param0) {
    return (List) invoke("listAllChannelsWithAlgorithm", new Object[] { param0 }, new String[] { "java.lang.String" });
  }

  @Override
  public void delete(final String param0, final String param1) {
    invoke("delete", new Object[] { param0, param1 }, new String[] { "java.lang.String", "java.lang.String" });
  }

  @Override
  public void deleteAlgorithm(final String param0) {
    invoke("deleteAlgorithm", new Object[] { param0 }, new String[] { "java.lang.String" });
  }

  @Override
  public void deleteAll() {
    invoke("deleteAll", (Object[]) null, (String[]) null);
  }

  @Override
  public void deleteChannel(final String param0) {
    invoke("deleteChannel", new Object[] { param0 }, new String[] { "java.lang.String" });
  }
}
