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

package org.jppf.management.generated;

import java.util.Map;
import org.jppf.classloader.DelegationModel;
import org.jppf.management.AbstractMBeanStaticProxy;
import org.jppf.management.JMXConnectionWrapper;
import org.jppf.management.NodeSelector;
import org.jppf.management.forwarding.JPPFNodeForwardingMBean;
import org.jppf.utils.TypedProperties;

/**
 * Generated static proxy for the {@link org.jppf.management.forwarding.JPPFNodeForwardingMBean} MBean interface.
 * @author /common/src/java/org/jppf/utils/generator/MBeanStaticProxyGenerator.java
 */
public class JPPFNodeForwardingMBeanStaticProxy extends AbstractMBeanStaticProxy implements JPPFNodeForwardingMBean {
  /**
   * Initialize this MBean static proxy.
   * @param connection the JMX connection used to invoke remote MBean methods.
   */
  public JPPFNodeForwardingMBeanStaticProxy(final JMXConnectionWrapper connection) {
    super(connection, "org.jppf:name=nodeForwarding,type=driver");
  }

  /**
   * Get the JMX object name for this MBean static proxy.
   * @return the object name as a string.
   */
  public static final String getMBeanName() {
    return "org.jppf:name=nodeForwarding,type=driver";
  }

  @Override
  public String registerForwardingNotificationListener(final NodeSelector param0, final String param1) {
    return (String) invoke("registerForwardingNotificationListener", new Object[] { param0, param1 }, new String[] { "org.jppf.management.NodeSelector", "java.lang.String" });
  }

  @Override
  public Map cancelJob(final NodeSelector param0, final String param1, final Boolean param2) {
    return (Map) invoke("cancelJob", new Object[] { param0, param1, param2 }, new String[] { "org.jppf.management.NodeSelector", "java.lang.String", "java.lang.Boolean" });
  }

  @Override
  public Map forwardGetAttribute(final NodeSelector param0, final String param1, final String param2) {
    return (Map) invoke("forwardGetAttribute", new Object[] { param0, param1, param2 }, new String[] { "org.jppf.management.NodeSelector", "java.lang.String", "java.lang.String" });
  }

  @Override
  public Map forwardInvoke(final NodeSelector param0, final String param1, final String param2) {
    return (Map) invoke("forwardInvoke", new Object[] { param0, param1, param2 }, new String[] { "org.jppf.management.NodeSelector", "java.lang.String", "java.lang.String" });
  }

  @Override
  public Map forwardInvoke(final NodeSelector param0, final String param1, final String param2, final Object[] param3, final String[] param4) {
    return (Map) invoke("forwardInvoke", new Object[] { param0, param1, param2, param3, param4 }, new String[] { "org.jppf.management.NodeSelector", "java.lang.String", "java.lang.String", "[Ljava.lang.Object;", "[Ljava.lang.String;" });
  }

  @Override
  public Map forwardSetAttribute(final NodeSelector param0, final String param1, final String param2, final Object param3) {
    return (Map) invoke("forwardSetAttribute", new Object[] { param0, param1, param2, param3 }, new String[] { "org.jppf.management.NodeSelector", "java.lang.String", "java.lang.String", "java.lang.Object" });
  }

  @Override
  public Map gc(final NodeSelector param0) {
    return (Map) invoke("gc", new Object[] { param0 }, new String[] { "org.jppf.management.NodeSelector" });
  }

  @Override
  public Map getDelegationModel(final NodeSelector param0) {
    return (Map) invoke("getDelegationModel", new Object[] { param0 }, new String[] { "org.jppf.management.NodeSelector" });
  }

  @Override
  public Map getNbSlaves(final NodeSelector param0) {
    return (Map) invoke("getNbSlaves", new Object[] { param0 }, new String[] { "org.jppf.management.NodeSelector" });
  }

  @Override
  public Map healthSnapshot(final NodeSelector param0) {
    return (Map) invoke("healthSnapshot", new Object[] { param0 }, new String[] { "org.jppf.management.NodeSelector" });
  }

  @Override
  public Map provisionSlaveNodes(final NodeSelector param0, final int param1) {
    return (Map) invoke("provisionSlaveNodes", new Object[] { param0, param1 }, new String[] { "org.jppf.management.NodeSelector", "int" });
  }

  @Override
  public Map provisionSlaveNodes(final NodeSelector param0, final int param1, final boolean param2) {
    return (Map) invoke("provisionSlaveNodes", new Object[] { param0, param1, param2 }, new String[] { "org.jppf.management.NodeSelector", "int", "boolean" });
  }

  @Override
  public Map provisionSlaveNodes(final NodeSelector param0, final int param1, final boolean param2, final TypedProperties param3) {
    return (Map) invoke("provisionSlaveNodes", new Object[] { param0, param1, param2, param3 }, new String[] { "org.jppf.management.NodeSelector", "int", "boolean", "org.jppf.utils.TypedProperties" });
  }

  @Override
  public Map provisionSlaveNodes(final NodeSelector param0, final int param1, final TypedProperties param2) {
    return (Map) invoke("provisionSlaveNodes", new Object[] { param0, param1, param2 }, new String[] { "org.jppf.management.NodeSelector", "int", "org.jppf.utils.TypedProperties" });
  }

  @Override
  public Map resetTaskCounter(final NodeSelector param0) {
    return (Map) invoke("resetTaskCounter", new Object[] { param0 }, new String[] { "org.jppf.management.NodeSelector" });
  }

  @Override
  public Map restart(final NodeSelector param0) {
    return (Map) invoke("restart", new Object[] { param0 }, new String[] { "org.jppf.management.NodeSelector" });
  }

  @Override
  public Map restart(final NodeSelector param0, final Boolean param1) {
    return (Map) invoke("restart", new Object[] { param0, param1 }, new String[] { "org.jppf.management.NodeSelector", "java.lang.Boolean" });
  }

  @Override
  public Map setDelegationModel(final NodeSelector param0, final DelegationModel param1) {
    return (Map) invoke("setDelegationModel", new Object[] { param0, param1 }, new String[] { "org.jppf.management.NodeSelector", "org.jppf.classloader.DelegationModel" });
  }

  @Override
  public Map setTaskCounter(final NodeSelector param0, final Integer param1) {
    return (Map) invoke("setTaskCounter", new Object[] { param0, param1 }, new String[] { "org.jppf.management.NodeSelector", "java.lang.Integer" });
  }

  @Override
  public Map shutdown(final NodeSelector param0) {
    return (Map) invoke("shutdown", new Object[] { param0 }, new String[] { "org.jppf.management.NodeSelector" });
  }

  @Override
  public Map shutdown(final NodeSelector param0, final Boolean param1) {
    return (Map) invoke("shutdown", new Object[] { param0, param1 }, new String[] { "org.jppf.management.NodeSelector", "java.lang.Boolean" });
  }

  @Override
  public Map state(final NodeSelector param0) {
    return (Map) invoke("state", new Object[] { param0 }, new String[] { "org.jppf.management.NodeSelector" });
  }

  @Override
  public Map systemInformation(final NodeSelector param0) {
    return (Map) invoke("systemInformation", new Object[] { param0 }, new String[] { "org.jppf.management.NodeSelector" });
  }

  @Override
  public Map threadDump(final NodeSelector param0) {
    return (Map) invoke("threadDump", new Object[] { param0 }, new String[] { "org.jppf.management.NodeSelector" });
  }

  @Override
  public Map updateConfiguration(final NodeSelector param0, final Map param1, final Boolean param2) {
    return (Map) invoke("updateConfiguration", new Object[] { param0, param1, param2 }, new String[] { "org.jppf.management.NodeSelector", "java.util.Map", "java.lang.Boolean" });
  }

  @Override
  public Map updateConfiguration(final NodeSelector param0, final Map param1, final Boolean param2, final Boolean param3) {
    return (Map) invoke("updateConfiguration", new Object[] { param0, param1, param2, param3 }, new String[] { "org.jppf.management.NodeSelector", "java.util.Map", "java.lang.Boolean", "java.lang.Boolean" });
  }

  @Override
  public Map updateThreadPoolSize(final NodeSelector param0, final Integer param1) {
    return (Map) invoke("updateThreadPoolSize", new Object[] { param0, param1 }, new String[] { "org.jppf.management.NodeSelector", "java.lang.Integer" });
  }

  @Override
  public Map updateThreadsPriority(final NodeSelector param0, final Integer param1) {
    return (Map) invoke("updateThreadsPriority", new Object[] { param0, param1 }, new String[] { "org.jppf.management.NodeSelector", "java.lang.Integer" });
  }

  @Override
  public void unregisterForwardingNotificationListener(final String param0) {
    invoke("unregisterForwardingNotificationListener", new Object[] { param0 }, new String[] { "java.lang.String" });
  }
}
