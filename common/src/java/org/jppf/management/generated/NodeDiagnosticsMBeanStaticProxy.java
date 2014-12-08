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
import org.jppf.management.diagnostics.DiagnosticsMBean;
import org.jppf.management.diagnostics.HealthSnapshot;
import org.jppf.management.diagnostics.MemoryInformation;
import org.jppf.management.diagnostics.ThreadDump;

/**
 * Generated static proxy for the {@link org.jppf.management.diagnostics.DiagnosticsMBean} MBean interface.
 * @author /common/src/java/org/jppf/utils/generator/MBeanStaticProxyGenerator.java
 */
public class NodeDiagnosticsMBeanStaticProxy extends AbstractMBeanStaticProxy implements DiagnosticsMBean {
  /**
   * Initialize this MBean static proxy.
   * @param connection the JMX connection used to invoke remote MBean methods.
   */
  public NodeDiagnosticsMBeanStaticProxy(final JMXConnectionWrapper connection) {
    super(connection, "org.jppf:name=diagnostics,type=node");
  }

  @Override
  public void gc() {
    invoke("gc", (Object[]) null, (String[]) null);
  }

  @Override
  public MemoryInformation memoryInformation() {
    return (MemoryInformation) invoke("memoryInformation", (Object[]) null, (String[]) null);
  }

  @Override
  public HealthSnapshot healthSnapshot() {
    return (HealthSnapshot) invoke("healthSnapshot", (Object[]) null, (String[]) null);
  }

  @Override
  public String[] threadNames() {
    return (String[]) invoke("threadNames", (Object[]) null, (String[]) null);
  }

  @Override
  public Boolean hasDeadlock() {
    return (Boolean) invoke("hasDeadlock", (Object[]) null, (String[]) null);
  }

  @Override
  public ThreadDump threadDump() {
    return (ThreadDump) invoke("threadDump", (Object[]) null, (String[]) null);
  }

  @Override
  public String heapDump() {
    return (String) invoke("heapDump", (Object[]) null, (String[]) null);
  }

  @Override
  public Double cpuLoad() {
    return (Double) invoke("cpuLoad", (Object[]) null, (String[]) null);
  }
}
