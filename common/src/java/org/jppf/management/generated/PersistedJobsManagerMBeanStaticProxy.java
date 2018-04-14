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

import java.util.Collection;
import java.util.List;
import org.jppf.job.JobSelector;
import org.jppf.job.persistence.PersistedJobsManagerMBean;
import org.jppf.job.persistence.PersistenceObjectType;
import org.jppf.management.AbstractMBeanStaticProxy;
import org.jppf.management.JMXConnectionWrapper;

/**
 * Generated static proxy for the {@link org.jppf.job.persistence.PersistedJobsManagerMBean} MBean interface.
 * @author /common/src/java/org/jppf/utils/generator/MBeanStaticProxyGenerator.java
 */
@SuppressWarnings({"rawtypes", "unchecked"})
public class PersistedJobsManagerMBeanStaticProxy extends AbstractMBeanStaticProxy implements PersistedJobsManagerMBean {
  /**
   * Initialize this MBean static proxy.
   * @param connection the JMX connection used to invoke remote MBean methods.
   */
  public PersistedJobsManagerMBeanStaticProxy(final JMXConnectionWrapper connection) {
    super(connection, "org.jppf:name=job_persistence,type=driver");
  }

  /**
   * Get the JMX object name for this MBean static proxy.
   * @return the object name as a string.
   */
  public static final String getMBeanName() {
    return "org.jppf:name=job_persistence,type=driver";
  }

  @Override
  public boolean deleteLoadRequest(final long param0) {
    return (boolean) invoke("deleteLoadRequest", new Object[] { param0 }, new String[] { "long" });
  }

  @Override
  public boolean isJobComplete(final String param0) {
    return (boolean) invoke("isJobComplete", new Object[] { param0 }, new String[] { "java.lang.String" });
  }

  @Override
  public boolean isJobersisted(final String param0) {
    return (boolean) invoke("isJobersisted", new Object[] { param0 }, new String[] { "java.lang.String" });
  }

  @Override
  public int[][] getPersistedJobPositions(final String param0) {
    return (int[][]) invoke("getPersistedJobPositions", new Object[] { param0 }, new String[] { "java.lang.String" });
  }

  @Override
  public Object getPersistedJobObject(final String param0, final PersistenceObjectType param1, final int param2) {
    return invoke("getPersistedJobObject", new Object[] { param0, param1, param2 }, new String[] { "java.lang.String", "org.jppf.job.persistence.PersistenceObjectType", "int" });
  }

  @Override
  public Object getPersistedJobObject(final long param0, final String param1, final PersistenceObjectType param2, final int param3) {
    return invoke("getPersistedJobObject", new Object[] { param0, param1, param2, param3 }, new String[] { "long", "java.lang.String", "org.jppf.job.persistence.PersistenceObjectType", "int" });
  }

  @Override
  public List deletePersistedJobs(final JobSelector param0) {
    return (List) invoke("deletePersistedJobs", new Object[] { param0 }, new String[] { "org.jppf.job.JobSelector" });
  }

  @Override
  public List getPersistedJobUuids(final JobSelector param0) {
    return (List) invoke("getPersistedJobUuids", new Object[] { param0 }, new String[] { "org.jppf.job.JobSelector" });
  }

  @Override
  public long requestLoad(final Collection param0) {
    return (long) invoke("requestLoad", new Object[] { param0 }, new String[] { "java.util.Collection" });
  }
}
