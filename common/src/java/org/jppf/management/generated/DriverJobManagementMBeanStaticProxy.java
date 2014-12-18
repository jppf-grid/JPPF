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

import org.jppf.job.JobInformation;
import org.jppf.management.AbstractMBeanStaticProxy;
import org.jppf.management.JMXConnectionWrapper;
import org.jppf.server.job.management.DriverJobManagementMBean;
import org.jppf.server.job.management.NodeJobInformation;

/**
 * Generated static proxy for the {@link org.jppf.server.job.management.DriverJobManagementMBean} MBean interface.
 * @author /common/src/java/org/jppf/utils/generator/MBeanStaticProxyGenerator.java
 */
public class DriverJobManagementMBeanStaticProxy extends AbstractMBeanStaticProxy implements DriverJobManagementMBean {
  /**
   * Initialize this MBean static proxy.
   * @param connection the JMX connection used to invoke remote MBean methods.
   */
  public DriverJobManagementMBeanStaticProxy(final JMXConnectionWrapper connection) {
    super(connection, "org.jppf:name=jobManagement,type=driver");
  }

  @Override
  public void updateMaxNodes(final String param0, final Integer param1) {
    invoke("updateMaxNodes", new Object[] { param0, param1 }, new String[] { "java.lang.String", "java.lang.Integer" });
  }

  @Override
  public JobInformation getJobInformation(final String param0) {
    return (JobInformation) invoke("getJobInformation", new Object[] { param0 }, new String[] { "java.lang.String" });
  }

  @Override
  public NodeJobInformation[] getNodeInformation(final String param0) {
    return (NodeJobInformation[]) invoke("getNodeInformation", new Object[] { param0 }, new String[] { "java.lang.String" });
  }

  @Override
  public void updatePriority(final String param0, final Integer param1) {
    invoke("updatePriority", new Object[] { param0, param1 }, new String[] { "java.lang.String", "java.lang.Integer" });
  }

  @Override
  public void cancelJob(final String param0) {
    invoke("cancelJob", new Object[] { param0 }, new String[] { "java.lang.String" });
  }

  @Override
  public void suspendJob(final String param0, final Boolean param1) {
    invoke("suspendJob", new Object[] { param0, param1 }, new String[] { "java.lang.String", "java.lang.Boolean" });
  }

  @Override
  public void resumeJob(final String param0) {
    invoke("resumeJob", new Object[] { param0 }, new String[] { "java.lang.String" });
  }

  @Override
  public String[] getAllJobIds() {
    return (String[]) getAttribute("AllJobIds");
  }
}
