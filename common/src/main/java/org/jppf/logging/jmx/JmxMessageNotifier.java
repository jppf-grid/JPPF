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

package org.jppf.logging.jmx;

import java.lang.management.ManagementFactory;
import java.util.concurrent.locks.ReentrantLock;

import javax.management.*;

import org.jppf.management.ObjectNameCache;
import org.jppf.utils.ExceptionUtils;

/**
 * Instances of this class send messages as JMX notifications.
 * @author Laurent Cohen
 */
public class JmxMessageNotifier {
  /**
   * The logger to which appends are delegated for a JPPF driver.
   */
  private JmxLogger jmxLogger = null;
  /**
   * The name of the MBean that sends the log messages as JMX notifications.
   */
  private ObjectName objectName = null;
  /**
   * Used to synchronize rhe registration of the MBean with the MBean server.
   */
  private static final ReentrantLock LOCK = new ReentrantLock();
  /**
   * The MBean server with which the MBean is registered.
   */
  private static MBeanServer server = null;
  /**
   * Flag used to avoid stack overflow error when using JDK logging.
   * The error is caused by the fact that {@link ManagementFactory#getPlatformMBeanServer()}
   * is performing some logging of its own, so we need to prevent the recursion the first time this notifier is used.
   */
  private static boolean initializing = false;

  /**
   * Initialize this notifier with the default MBean name.
   */
  public JmxMessageNotifier() {
    initializeJmx(JmxLogger.DEFAULT_MBEAN_NAME);
  }

  /**
   * Initialize this notifier with the name of the MBean that will send the notifications.
   * @param name the name of MBean, following the conventions specified in {@link ObjectName}.
   */
  public JmxMessageNotifier(final String name) {
    //initializeJmx(name == null ? JmxLogger.DEFAULT_MBEAN_NAME : name);
    initializeJmx(JmxLogger.DEFAULT_MBEAN_NAME);
  }

  /**
   * Send the specified message via JMX.
   * @param message the message to send.
   */
  public void sendMessage(final String message) {
    if (jmxLogger == null) return;
    jmxLogger.log(message);
  }

  /**
   * Retrieve the JMX logger if it is already registered with the MBean server.
   * @param name the name of the registered mbean to find.
   */
  private void initializeJmx(final String name) {
    initObjectName(name);
    if (objectName == null) return;
    registerMBean();
    if (server == null) return;
    initializeProxy();
  }

  /**
   * Initialize the object name of the MBean.
   * @param name the name of the mbean.
   */
  private void initObjectName(final String name) {
    try {
      if (objectName == null) objectName = ObjectNameCache.getObjectName(name);
    } catch (final Exception e) {
      try {
        System.out.println("Error in logging configuration: JMX logger name '" + name + "' is invalid (" + ExceptionUtils.getMessage(e) + ')');
        objectName = ObjectNameCache.getObjectName(JmxLogger.DEFAULT_MBEAN_NAME);
      } catch (final Exception e2) {
        System.out.println("Failed to initialize jmx based logging with default MBean name:" + ExceptionUtils.getMessage(e2));
      }
    }
  }

  /**
   * Retrieve the JMX logger if it is already registered with the MBean server.
   */
  private void initializeProxy() {
    try {
      if (jmxLogger == null) jmxLogger = MBeanServerInvocationHandler.newProxyInstance(server, objectName, JmxLogger.class, true);
    } catch (final Exception e) {
      System.out.println("Error initializing the JMX logger MBean '" + objectName + "' : " + ExceptionUtils.getMessage(e));
    }
  }

  /**
   * Register the JMX logger MBean with an MBean Server.
   */
  private void registerMBean() {
    LOCK.lock();
    try {
      if (initializing) return;
      initializing = true;
      if (server != null) return;
      server = obtainMBeanServer();
      if (server == null) return;
      if (server.isRegistered(objectName)) return;
      try {
        final JmxLoggerImpl impl = new JmxLoggerImpl();
        final StandardEmitterMBean mbean = new StandardEmitterMBean(impl, JmxLogger.class, impl);
        server.registerMBean(mbean, objectName);
      } catch (final Exception e) {
        System.out.println("Error registering the JMX logger MBean '" + objectName + "' : " + ExceptionUtils.getMessage(e));
      }
    } finally {
      initializing = false;
      LOCK.unlock();
    }
  }

  /**
   * Obtain an MBean Server.
   * @return an {@link MBeanServer} instance.
   */
  private static MBeanServer obtainMBeanServer() {
    try {
      return ManagementFactory.getPlatformMBeanServer();
    } catch (final Exception e) {
      System.out.println("Failed to obtain the MBean server: " + ExceptionUtils.getMessage(e));
      return null;
    }
  }
}
