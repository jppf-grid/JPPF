/*
 * JPPF.
 * Copyright (C) 2005-2019 JPPF Team.
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

package org.jppf.utils;

import java.lang.management.*;

import javax.management.*;

import org.jppf.management.ObjectNameCache;
import org.jppf.utils.configuration.JPPFProperties;
import org.slf4j.*;

/**
 *
 * @author Laurent Cohen
 */
public class ManagementUtils {
  /**
   * Logger for this class.
   */
  private static final Logger log = LoggerFactory.getLogger(ManagementUtils.class);
  /**
   * Determines whether the debug level is enabled in the logging configuration, without the cost of a method call.
   */
  private static boolean managementAvailable = true;
  /**
   * The the thread MXBean iself.
   */
  private static ThreadMXBean threadMXBean;
  /**
   * Whether getting the cpu time is supported.
   */
  private static boolean cpuTimeEnabled;
  /**
   * The name of the operating system MXBean.
   */
  private static MBeanServer platformMBeanServer;

  static {
    try {
      threadMXBean =  ManagementFactory.getThreadMXBean();
      cpuTimeEnabled = threadMXBean.isThreadCpuTimeSupported();
      if (cpuTimeEnabled) threadMXBean.setThreadCpuTimeEnabled(true);

      platformMBeanServer = ManagementFactory.getPlatformMBeanServer();
      log.debug("management successfully initialized");
    } catch (final Exception e) {
      managementAvailable = false;
      if (!JPPFConfiguration.get(JPPFProperties.NODE_ANDROID)) log.error("management could not be initialized, exception: {}", ExceptionUtils.getStackTrace(e));
    }
  }

  /**
   * Get the given attribute of the given mbean via the specified mbean server connection.
   * @param connection the mbean server connection to use.
   * @param mbeanName the name of the mbean on which to invoke an attribute.
   * @param attributeName the name of the attribute to get.
   * @return the value of the attrribute, or {@code null} if management is not available.
   * @throws Exception if any error occurs.
   */
  public static Object getAttribute(final MBeanServerConnection connection, final String mbeanName, final String attributeName) throws Exception {
    if (!isManagementAvailable()) return null;
    return connection.getAttribute(ObjectNameCache.getObjectName(mbeanName), attributeName);
  }

  /**
   * Determine whether management is available for the current JVM.
   * @return {@code true} if management is available, {@code false} otherwise.
   */
  public static boolean isManagementAvailable() {
    return managementAvailable;
  }

  /**
   * Get the platform MBean server.
   * @return a {@code MBeanServerObject}.
   */
  public static MBeanServer getPlatformServer() {
    return platformMBeanServer;
  }

  /**
   * Determine whether the MBean with the specified is already registered.
   * @param mbeanName the name of the MBean to check.
   * @return {@code true} if the MBean is registered, {@code false} otherwise.
   * @throws Exception if any error occurs.
   */
  public static boolean isMBeanRegistered(final String mbeanName) throws Exception {
    if (!isManagementAvailable()) return false;
    return platformMBeanServer.isRegistered(ObjectNameCache.getObjectName(mbeanName));
  }

  /**
   * Whether CPU time measurement is enabled/supported.
   * @return {@code true} if time measurement is enabled, {@code false} otherwise.
   */
  public static boolean isCpuTimeEnabled() {
    return cpuTimeEnabled;
  }

  /**
   * Determien the CPU time for the specified thread.
   * @param threadID the id of the thread.
   * @return the CPU time in nanoseconds, or -1L if the cpu time is not supported or could not be measured.
   */
  public static long getThreadCpuTime(final long threadID) {
    if (!isManagementAvailable()) return -1L;
    try {
      return threadMXBean.getThreadCpuTime(threadID);
    } catch(@SuppressWarnings("unused") final Exception e) {
      return -1L;
    }
  }

  /**
   * Determien the user time for the specified thread.
   * @param threadID the id of the thread.
   * @return the user time in nanoseconds, or -1L if the user time is not supported or could not be measured.
   */
  public static long getThreadUserTime(final long threadID) {
    if (!isManagementAvailable()) return -1L;
    try {
      return threadMXBean.getThreadUserTime(threadID);
    } catch(@SuppressWarnings("unused") final Exception e) {
      return -1L;
    }
  }
}
