/*
 * JPPF.
 * Copyright (C) 2005-2015 JPPF Team.
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

import java.util.Map;
import org.jppf.classloader.DelegationModel;
import org.jppf.management.AbstractMBeanStaticProxy;
import org.jppf.management.JMXConnectionWrapper;
import org.jppf.management.JPPFNodeAdminMBean;
import org.jppf.management.JPPFNodeState;
import org.jppf.management.JPPFSystemInformation;
import org.jppf.management.NodePendingAction;

/**
 * Generated static proxy for the {@link org.jppf.management.JPPFNodeAdminMBean} MBean interface.
 * @author /common/src/java/org/jppf/utils/generator/MBeanStaticProxyGenerator.java
 */
public class JPPFNodeAdminMBeanStaticProxy extends AbstractMBeanStaticProxy implements JPPFNodeAdminMBean {
  /**
   * Initialize this MBean static proxy.
   * @param connection the JMX connection used to invoke remote MBean methods.
   */
  public JPPFNodeAdminMBeanStaticProxy(final JMXConnectionWrapper connection) {
    super(connection, "org.jppf:name=admin,type=node");
  }

  /**
   * Get the JMX object name for this MBean static proxy.
   * @return the object name as a string.
   */
  public static final String getMBeanName() {
    return "org.jppf:name=admin,type=node";
  }

  @Override
  public boolean cancelPendingAction() {
    return (boolean) invoke("cancelPendingAction", (Object[]) null, (String[]) null);
  }

  @Override
  public DelegationModel getDelegationModel() {
    return (DelegationModel) getAttribute("DelegationModel");
  }

  @Override
  public JPPFNodeState state() {
    return (JPPFNodeState) invoke("state", (Object[]) null, (String[]) null);
  }

  @Override
  public JPPFSystemInformation systemInformation() {
    return (JPPFSystemInformation) invoke("systemInformation", (Object[]) null, (String[]) null);
  }

  @Override
  public NodePendingAction pendingAction() {
    return (NodePendingAction) invoke("pendingAction", (Object[]) null, (String[]) null);
  }

  @Override
  public void cancelJob(final String param0, final Boolean param1) {
    invoke("cancelJob", new Object[] { param0, param1 }, new String[] { "java.lang.String", "java.lang.Boolean" });
  }

  @Override
  public void resetTaskCounter() {
    invoke("resetTaskCounter", (Object[]) null, (String[]) null);
  }

  @Override
  public void restart() {
    invoke("restart", (Object[]) null, (String[]) null);
  }

  @Override
  public void restart(final Boolean param0) {
    invoke("restart", new Object[] { param0 }, new String[] { "java.lang.Boolean" });
  }

  @Override
  public void setDelegationModel(final DelegationModel param0) {
    setAttribute("DelegationModel", param0);
  }

  @Override
  public void setTaskCounter(final Integer param0) {
    setAttribute("TaskCounter", param0);
  }

  @Override
  public void shutdown() {
    invoke("shutdown", (Object[]) null, (String[]) null);
  }

  @Override
  public void shutdown(final Boolean param0) {
    invoke("shutdown", new Object[] { param0 }, new String[] { "java.lang.Boolean" });
  }

  @Override
  public void updateConfiguration(final Map param0, final Boolean param1) {
    invoke("updateConfiguration", new Object[] { param0, param1 }, new String[] { "java.util.Map", "java.lang.Boolean" });
  }

  @Override
  public void updateThreadPoolSize(final Integer param0) {
    invoke("updateThreadPoolSize", new Object[] { param0 }, new String[] { "java.lang.Integer" });
  }

  @Override
  public void updateThreadsPriority(final Integer param0) {
    invoke("updateThreadsPriority", new Object[] { param0 }, new String[] { "java.lang.Integer" });
  }
}
