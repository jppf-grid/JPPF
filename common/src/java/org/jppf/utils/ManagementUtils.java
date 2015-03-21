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

package org.jppf.utils;

import java.lang.reflect.*;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.*;

/**
 *
 * @author Laurent Cohen
 */
public class ManagementUtils {
  /**
   * Logger for this class.
   */
  private static Logger log = LoggerFactory.getLogger(ManagementUtils.class);
  /**
   * Determines whether the debug level is enabled in the logging configuration, without the cost of a method call.
   */
  private static boolean debugEnabled = LoggingUtils.isDebugEnabled(log);
  /**
   * Determines whether the debug level is enabled in the logging configuration, without the cost of a method call.
   */
  private static boolean managementAvailable = true;
  /**
   * The name of the thread MXBean.
   */
  private static Object THREAD_MXBEAN_NAME = null;
  /**
   * The the thread MXBean iself.
   */
  private static Object THREAD_MXBEAN = null;
  /**
   * Whether getting the cpu time is supported.
   */
  private static boolean CPU_TIME_ENABLED = false;
  /**
   * The method that gets the thread cpu time from the {@code ThreadMXBean}.
   */
  private static Method GET_THREAD_CPU_TIME_METHOD = null;
  /**
   * The method that gets the user time from the {@code ThreadMXBean}.
   */
  private static Method GET_THREAD_USER_TIME_METHOD = null;
  /**
   * The name of the operating system MXBean.
   */
  private static Object OS_MXBEAN_NAME = null;
  /**
   * The name of the operating system MXBean.
   */
  private static Object RUNTIME_MXBEAN_NAME = null;
  /**
   * The name of the operating system MXBean.
   */
  private static Object PLATFORM_SERVER = null;
  /**
   * The name of the operating system MXBean.
   */
  private static Method GET_ATTRIBUTE_METHOD = null;
  /**
   * The name of the operating system MXBean.
   */
  private static Method SET_ATTRIBUTE_METHOD = null;
  /**
   * Constructor for {@code javax.management.Attribute.Attribute(String, Object)}.
   */
  private static Constructor<?> ATTRIBUTE_CONSTRUCTOR = null;
  /**
   * The name of the operating system MXBean.
   */
  private static Method INVOKE_METHOD = null;
  /**
   * Method {@code javax.management.MBeanServerConnection.isRegistered(ObjectName)}.
   */
  private static Method IS_MBEAN_REGISTERED_METHOD = null;
  /**
   * Method {@code javax.management.JMX.newMBeanProxy(MBeanServerConnection, ObjectName, CLass<?>, boolean)}.
   */
  private static Method NEW_PROXY_METHOD = null;
  /**
   * Method {@code javax.management.MBeanServerConnection.addNotificationListener(ObjectName, NotificationListener, NotificationFilter, Object)}.
   */
  private static Method ADD_NOTIFICATION_LISTENER_METHOD = null;
  /**
   * Method {@code javax.management.MBeanServerConnection.removeNotificationListener(ObjectName, NotificationListener, NotificationFilter, Object)}.
   */
  private static Method REMOVE_NOTIFICATION_LISTENER_METHOD = null;
  /**
   * Method {@code javax.management.MBeanServerConnection.getMBeanInfo(ObjectName)}.
   */
  private static Method GET_MBEAN_INFO_METHOD = null;
  /**
   * Method {@code javax.management.MBeanInfo.getNotifications()}.
   */
  private static Method GET_MBEAN_NOTIFICATIONS_INFO_METHOD = null;
  /**
   * The class object {@code for javax.management.ObjectName}.
   */
  private static Class<?> OBJECT_NAME_CLASS;
  /**
   * Constructor for {@code javax.management.ObjectName.ObjectName(String)}.
   */
  private static Constructor<?> OBJECT_NAME_CONSTRUCTOR = null;
  /**
   *
   */
  private static final ConcurrentHashMap<String, Object> objectNames = new ConcurrentHashMap<>();

  static {
    try {
      OBJECT_NAME_CLASS = Class.forName("javax.management.ObjectName");
      OBJECT_NAME_CONSTRUCTOR = OBJECT_NAME_CLASS.getConstructor(String.class);
      Class<?> serverConnectionClass = Class.forName("javax.management.MBeanServerConnection");
      GET_ATTRIBUTE_METHOD = serverConnectionClass.getDeclaredMethod("getAttribute", OBJECT_NAME_CLASS, String.class);
      Class<?> attributeClass = Class.forName("javax.management.Attribute");
      ATTRIBUTE_CONSTRUCTOR = attributeClass.getConstructor(String.class, Object.class);
      SET_ATTRIBUTE_METHOD = serverConnectionClass.getDeclaredMethod("setAttribute", OBJECT_NAME_CLASS, attributeClass);
      INVOKE_METHOD = serverConnectionClass.getDeclaredMethod("invoke", OBJECT_NAME_CLASS, String.class, Object[].class, String[].class);
      IS_MBEAN_REGISTERED_METHOD = serverConnectionClass.getDeclaredMethod("isRegistered", OBJECT_NAME_CLASS);
      Class<?> notifListenerClass = Class.forName("javax.management.NotificationListener");
      Class<?> notifFilterClass = Class.forName("javax.management.NotificationFilter");
      ADD_NOTIFICATION_LISTENER_METHOD = serverConnectionClass.getDeclaredMethod("addNotificationListener", OBJECT_NAME_CLASS, notifListenerClass, notifFilterClass, Object.class);
      REMOVE_NOTIFICATION_LISTENER_METHOD = serverConnectionClass.getDeclaredMethod("removeNotificationListener", OBJECT_NAME_CLASS, notifListenerClass, notifFilterClass, Object.class);
      Class<?> jmxClass = Class.forName("javax.management.JMX");
      NEW_PROXY_METHOD = jmxClass.getDeclaredMethod("newMBeanProxy", serverConnectionClass, OBJECT_NAME_CLASS, Class.class, boolean.class);
      Class<?> mbeanInfoClass = Class.forName("javax.management.MBeanInfo");
      Class<?> mbeanNotificationInfoClass = Class.forName("javax.management.MBeanNotificationInfo");
      GET_MBEAN_INFO_METHOD = serverConnectionClass.getDeclaredMethod("getMBeanInfo", OBJECT_NAME_CLASS);
      GET_MBEAN_NOTIFICATIONS_INFO_METHOD = mbeanInfoClass.getDeclaredMethod("getNotifications");

      Class<?> factoryClass = Class.forName("java.lang.management.ManagementFactory");
      OS_MXBEAN_NAME = getObjectName("java.lang:type=OperatingSystem");
      RUNTIME_MXBEAN_NAME = getObjectName("java.lang:type=Runtime");
      THREAD_MXBEAN_NAME = getObjectName("java.lang:type=Threading");
      Method m = factoryClass.getDeclaredMethod("getThreadMXBean");
      THREAD_MXBEAN = m.invoke(null);
      Class<?> threadMXBeanClass = Class.forName("java.lang.management.ThreadMXBean");
      m = threadMXBeanClass.getDeclaredMethod("isThreadCpuTimeSupported");
      CPU_TIME_ENABLED = (Boolean) m.invoke(THREAD_MXBEAN);
      if (CPU_TIME_ENABLED) {
        m = threadMXBeanClass.getDeclaredMethod("setThreadCpuTimeEnabled", boolean.class);
        m.invoke(THREAD_MXBEAN, true);
        GET_THREAD_CPU_TIME_METHOD = threadMXBeanClass.getDeclaredMethod("getThreadCpuTime", long.class);
        GET_THREAD_USER_TIME_METHOD = threadMXBeanClass.getDeclaredMethod("getThreadUserTime", long.class);
      }

      m = factoryClass.getDeclaredMethod("getPlatformMBeanServer");
      PLATFORM_SERVER = m.invoke(null);
      log.info("management successfully initialized");
    } catch (Exception e) {
      managementAvailable = false;
      log.error("management could not be initialized, exception: ", ExceptionUtils.getStackTrace(e));
      //e.printStackTrace();
    }
  }

  /**
   * Invoke the given operation on the given mbean via the specified mbean server connection.
   * @param connection the mbean server connection to use.
   * @param mbeanName the name of the mbean on which to invoke an operation.
   * @param operationName the name of the operation to invoke.
   * @param params the params of the operation invocation.
   * @param signature the types of the parameters.
   * @return the result of the invokcation, or {@code null} if management is not available.
   * @throws Exception if any error occurs.
   */
  public static Object invoke(final Object connection, final String mbeanName, final String operationName, final Object[] params, final String[] signature) throws Exception {
    if (!isManagementAvailable()) return null;
    return INVOKE_METHOD.invoke(connection, getObjectName(mbeanName), operationName, params, signature);
  }

  /**
   * Get the given attribute of the given mbean via the specified mbean server connection.
   * @param connection the mbean server connection to use.
   * @param mbeanName the name of the mbean on which to invoke an attribute.
   * @param attributeName the name of the attribute to get.
   * @return the value of the attrribute, or {@code null} if management is not available.
   * @throws Exception if any error occurs.
   */
  public static Object getAttribute(final Object connection, final String mbeanName, final String attributeName) throws Exception {
    if (!isManagementAvailable()) return null;
    return GET_ATTRIBUTE_METHOD.invoke(connection, getObjectName(mbeanName), attributeName);
  }

  /**
   * Get the given attribute of the given mbean via the specified mbean server connection.
   * @param connection the mbean server connection to use.
   * @param mbeanName the name of the mbean on which to invoke an attribute.
   * @param attributeName the name of the attribute to get.
   * @param value the value of the attrribute to set.
   * @throws Exception if any error occurs.
   */
  public static void setAttribute(final Object connection, final String mbeanName, final String attributeName, final Object value) throws Exception {
    if (!isManagementAvailable()) return;
    Object attribute = ATTRIBUTE_CONSTRUCTOR.newInstance(attributeName, value);
    SET_ATTRIBUTE_METHOD.invoke(connection, getObjectName(mbeanName), attribute);
  }

  /**
   * Create a proxy for the specified mbean interface.
   * @param <T> the type of the proxy to return.
   * @param connection the connection through which to get the rpoxy.
   * @param mbeanName the name of the mbean for which to get a proxy.
   * @param inf the mbean interface for which to get a proxy.
   * @return a proxy instance of the the psecified interface, or {@code null} if management is not available.
   * @throws Exception if any error occurs.
   */
  public static <T> T newProxy(final Object connection, final String mbeanName, final Class<T> inf) throws Exception {
    if (!isManagementAvailable()) return null;
    return newProxy(connection, getObjectName(mbeanName), inf);
  }

  /**
   * Create a proxy for the specified mbean interface.
   * @param <T> the type of the proxy to return.
   * @param connection the connection through which to get the proxy.
   * @param mbeanName the name of the mbean for which to get a proxy should be an instance of {@code ObjectName}.
   * @param inf the mbean interface for which to get a proxy.
   * @return a proxy instance of the the psecified interface, or {@code null} if management is not available.
   * @throws Exception if any error occurs.
   */
  public static <T> T newProxy(final Object connection, final Object mbeanName, final Class<T> inf) throws Exception {
    if (!isManagementAvailable()) return null;
    return (T) NEW_PROXY_METHOD.invoke(null, connection, mbeanName, inf, true);
  }

  /**
   * Add the specified notification listener.
   * @param connection the connection through which to add the listener.
   * @param mbeanName the name of the mbean on which to a the listener.
   * @param listener the notification listener to add.
   * @param filter the notification filter.
   * @param handback the handback object.
   * @throws Exception if any error occurs.
   */
  public static void addNotificationListener(final Object connection, final String mbeanName, final Object listener, final Object filter, final Object handback) throws Exception {
    if (!isManagementAvailable()) return;
    ADD_NOTIFICATION_LISTENER_METHOD.invoke(connection, mbeanName, getObjectName(mbeanName), listener, filter, handback);
  }

  /**
   * Remove the specified notification listener.
   * @param connection the connection through which to add the listener.
   * @param mbeanName the name of the mbean on which to a the listener.
   * @param listener the notification listener to add.
   * @param filter the notification filter.
   * @param handback the handback object.
   * @throws Exception if any error occurs.
   */
  public static void removeNotificationListener(final Object connection, final String mbeanName, final Object listener, final Object filter, final Object handback) throws Exception {
    if (!isManagementAvailable()) return;
    REMOVE_NOTIFICATION_LISTENER_METHOD.invoke(connection, mbeanName, getObjectName(mbeanName), listener, filter, handback);
  }

  /**
   * Get the information for notifications supported by the specified MBean.
   * @param connection the connection through which to get the mbean info.
   * @param mbeanName the name of the mbean on which to a the mbean info.
   * @return an array of {@code MBeanNotificationInfo} instances.
   * @throws Exception if any error occurs.
   */
  public static Object getMBeanNotificationsInfo(final Object connection, final String mbeanName) throws Exception {
    if (!isManagementAvailable()) return null;
    Object mbeanInfo = GET_MBEAN_INFO_METHOD.invoke(connection, getObjectName(mbeanName));
    return GET_MBEAN_NOTIFICATIONS_INFO_METHOD.invoke(mbeanInfo);
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
  public static Object getPlatformServer() {
    return PLATFORM_SERVER;
  }

  /**
   * Determine whether the MBean with the specified is already registered.
   * @param mbeanName the name of the MBean to check.
   * @return {@code true} if the MBean is registered, {@code false} otherwise.
   * @throws Exception if any error occurs.
   */
  public static boolean isMBeanRegistered(final String mbeanName) throws Exception {
    if (!isManagementAvailable()) return false;
    return (Boolean) IS_MBEAN_REGISTERED_METHOD.invoke(PLATFORM_SERVER, getObjectName(mbeanName));
  }

  /**
   * Get or create and cache the object name for the specified name.
   * @param name the name for which to create a {@code javax.management.ObjectName}.
   * @return an {@code javax.management.ObjectName} as an object.
   */
  private static Object getObjectName(final String name) {
    Object o = objectNames.get(name);
    if (o == null) {
      try {
        o = OBJECT_NAME_CONSTRUCTOR.newInstance(name);
        objectNames.put(name, o);
      } catch (Exception e) {
        if (debugEnabled) log.debug("could not create ObjectName for " + name, e);
      }
    }
    return o;
  }

  /**
   * Whether CPU time measurement is enabled/supported.
   * @return {@code true} if time measurement is enabled, {@code false} otherwise.
   */
  public static boolean isCpuTimeEnabled() {
    return CPU_TIME_ENABLED;
  }

  /**
   * Determien the CPU time for the specified thread.
   * @param threadID the id of the thread.
   * @return the CPU time in nanoseconds, or -1L if the cpu time is not supported or could not be measured.
   */
  public static long getThreadCpuTime(final long threadID) {
    if (!isManagementAvailable()) return -1L;
    try {
      return (Long) GET_THREAD_CPU_TIME_METHOD.invoke(THREAD_MXBEAN, threadID);
    } catch(Exception e) {
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
      return (Long) GET_THREAD_USER_TIME_METHOD.invoke(THREAD_MXBEAN, threadID);
    } catch(Exception e) {
      return -1L;
    }
  }

  /**
   *
   * @param args not use.
   * @throws Throwable if any error occurs.
   */
  public static void main(final String[] args) throws Throwable {
  }
}
