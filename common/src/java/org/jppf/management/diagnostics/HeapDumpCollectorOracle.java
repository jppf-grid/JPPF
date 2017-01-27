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

package org.jppf.management.diagnostics;

import java.lang.management.ManagementFactory;
import java.lang.reflect.Method;
import java.util.concurrent.atomic.AtomicLong;

import javax.management.MBeanServer;

import org.jppf.JPPFException;

/**
 * Generate a heap dump for an Oracle JVM.
 * @author Laurent Cohen
 * @exclude
 */
public class HeapDumpCollectorOracle implements HeapDumpCollector {
  /**
   * The name of the HotSpot Diagnostic MBean.
   */
  private static final String HOTSPOT_BEAN_NAME = "com.sun.management:type=HotSpotDiagnostic";
  /**
   * The hotspot diagnostic MBean
   */
  private static Object hotspotDiagnosticsMXBean = getHotspotDiagnosticsMXBean();
  /**
   * COunt of heap dumps taken, used to proivde unique file names.
   */
  private static AtomicLong dumpCount = new AtomicLong(0L);

  @Override
  public String dumpHeap() throws Exception {
    if (hotspotDiagnosticsMXBean == null) throw new JPPFException("hotspot diagnostics MBean is not avaialable - no heap dump taken");
    Class<?> clazz = Class.forName("com.sun.management.HotSpotDiagnosticMXBean");
    Method m = clazz.getMethod("dumpHeap", String.class, boolean.class);
    String filename = "heapdump-" + dumpCount.incrementAndGet() + ".hprof";
    m.invoke(hotspotDiagnosticsMXBean, filename, true);
    return "heap dump saved at '" + filename + "'";
  }

  /**
   * Get the hotspot diagnostic MBean from the platform MBean server
   * @return the mbean as an object.
   */
  private static Object getHotspotDiagnosticsMXBean() {
    try {
      Class<?> clazz = Class.forName("com.sun.management.HotSpotDiagnosticMXBean");
      MBeanServer server = ManagementFactory.getPlatformMBeanServer();
      Object bean = ManagementFactory.newPlatformMXBeanProxy(server, HOTSPOT_BEAN_NAME, clazz);
      return bean;
    } catch (@SuppressWarnings("unused") Exception e) {
      return null;
    }
  }
}
