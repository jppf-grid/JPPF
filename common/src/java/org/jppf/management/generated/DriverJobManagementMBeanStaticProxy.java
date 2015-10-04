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
import org.jppf.job.JobInformation;
import org.jppf.job.JobSelector;
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

  /**
   * Get the JMX object name for this MBean static proxy.
   * @return the object name as a string.
   */
  public static final String getMBeanName() {
    return "org.jppf:name=jobManagement,type=driver";
  }

  @Override
  public void cancelJob(final String param0) {
    invoke("cancelJob", new Object[] { param0 }, new String[] { "java.lang.String" });
  }

  @Override
  public void resumeJobs(final JobSelector param0) {
    invoke("resumeJobs", new Object[] { param0 }, new String[] { "org.jppf.job.JobSelector" });
  }

  @Override
  public String[] getAllJobIds() {
    return (String[]) getAttribute("AllJobIds");
  }

  @Override
  public void suspendJob(final String param0, final Boolean param1) {
    invoke("suspendJob", new Object[] { param0, param1 }, new String[] { "java.lang.String", "java.lang.Boolean" });
  }

  @Override
  public void cancelJobs(final JobSelector param0) {
    invoke("cancelJobs", new Object[] { param0 }, new String[] { "org.jppf.job.JobSelector" });
  }

  @Override
  public void suspendJobs(final JobSelector param0, final Boolean param1) {
    invoke("suspendJobs", new Object[] { param0, param1 }, new String[] { "org.jppf.job.JobSelector", "java.lang.Boolean" });
  }

  @Override
  public void resumeJob(final String param0) {
    invoke("resumeJob", new Object[] { param0 }, new String[] { "java.lang.String" });
  }

  @Override
  public void updateMaxNodes(final JobSelector param0, final Integer param1) {
    invoke("updateMaxNodes", new Object[] { param0, param1 }, new String[] { "org.jppf.job.JobSelector", "java.lang.Integer" });
  }

  @Override
  public void updateMaxNodes(final String param0, final Integer param1) {
    invoke("updateMaxNodes", new Object[] { param0, param1 }, new String[] { "java.lang.String", "java.lang.Integer" });
  }

  @Override
  public String[] getAllJobUuids() {
    return (String[]) getAttribute("AllJobUuids");
  }

  @Override
  public JobInformation getJobInformation(final String param0) {
    return (JobInformation) invoke("getJobInformation", new Object[] { param0 }, new String[] { "java.lang.String" });
  }

  @Override
  public JobInformation[] getJobInformation(final JobSelector param0) {
    return (JobInformation[]) invoke("getJobInformation", new Object[] { param0 }, new String[] { "org.jppf.job.JobSelector" });
  }

  @Override
  public Map getNodeInformation(final JobSelector param0) {
    return (Map) invoke("getNodeInformation", new Object[] { param0 }, new String[] { "org.jppf.job.JobSelector" });
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
  public void updatePriority(final JobSelector param0, final Integer param1) {
    invoke("updatePriority", new Object[] { param0, param1 }, new String[] { "org.jppf.job.JobSelector", "java.lang.Integer" });
  }
}
