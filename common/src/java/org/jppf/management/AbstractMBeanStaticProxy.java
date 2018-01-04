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

package org.jppf.management;

import javax.management.*;

import org.jppf.utils.LoggingUtils;
import org.slf4j.*;

/**
 * Abstract super class for all generated MBean static proxies.
 * @author Laurent Cohen
 */
public class AbstractMBeanStaticProxy {
  /**
   * Logger for this class.
   */
  protected static Logger log = LoggerFactory.getLogger(JMXConnectionWrapper.class);
  /**
   * Determines whether debug log statements are enabled.
   */
  protected static boolean debugEnabled = LoggingUtils.isDebugEnabled(log);
  /**
   * The JMX connection used to invoke remote MBean methods.
   */
  protected final JMXConnectionWrapper connection;
  /**
   * The object name of the mbean for which this object is a proxy.
   */
  protected final String mbeanName;

  /**
   * Initialize this mbean static proxy.
   * @param connection the JMX connection used to invoke remote MBean methods.
   * @param mbeanName the object name of the mbean for which this object is a proxy.
   */
  public AbstractMBeanStaticProxy(final JMXConnectionWrapper connection, final String mbeanName) {
    this.connection = connection;
    this.mbeanName = mbeanName;
  }

  /**
   * Invoke a method on the MBean.
   * @param methodName the name of the method to invoke.
   * @param params the method parameter values.
   * @param signature the types of the method parameters.
   * @return an object or null.
   */
  public Object invoke(final String methodName, final Object[] params, final String[] signature) {
    Object result = null;
    try {
      result = connection.invoke(mbeanName, methodName, params, signature);
    } catch(final Exception e) {
      if (debugEnabled) log.debug(connection.getId() + " : error while invoking a method with the JMX connection", e);
    }
    return result;
  }

  /**
   * Get the value of an attribute of the specified MBean.
   * @param attribute the name of the attribute to read.
   * @return an object or null.
   */
  public Object getAttribute(final String attribute) {
    Object result = null;
    try {
      result = connection.getAttribute(mbeanName, attribute);
    } catch(final Exception e) {
      if (debugEnabled) log.debug(connection.getId() + " : error while getting an attribute with the JMX connection", e);
    }
    return result;
  }

  /**
   * Set the value of an attribute of the specified MBean.
   * @param attribute the name of the attribute to write.
   * @param value the value to set on the attribute.
   */
  public void setAttribute(final String attribute, final Object value) {
    try {
      connection.setAttribute(mbeanName, attribute, value);
    } catch(final Exception e) {
      if (debugEnabled) log.debug(connection.getId() + " : error while setting an attribute with the JMX connection", e);
    }
  }

  /**
   * Register a notification listener with the MBean.
   * @param listener the listener to register.
   * @param filter an optional filter, may be {@code null}.
   * @param handback a handback object.
   */
  public void addNotificationListener(final NotificationListener listener, final NotificationFilter filter, final Object handback) {
    try {
      connection.addNotificationListener(mbeanName, listener, filter, handback);
    } catch (final Exception e) {
      if (debugEnabled) log.debug(connection.getId() + " : error while adding notification filter", e);
    }
  }

  /**
   * Unregister a notification listener from the MBean.
   * @param listener the listener to unregister.
   */
  public void removeNotificationListener(final NotificationListener listener) {
    try {
      connection.removeNotificationListener(mbeanName, listener, null, null);
    } catch (final Exception e) {
      if (debugEnabled) log.debug(connection.getId() + " : error while removing notification filter", e);
    }
  }

  /**
   * Unregister a notification listener from the MBean.
   * @param listener the listener to unregister.
   * @param filter an optional filter, may be {@code null}.
   * @param handback a handback object.
   */
  public void removeNotificationListener(final NotificationListener listener, final NotificationFilter filter, final Object handback) {
    try {
      connection.removeNotificationListener(mbeanName, listener, filter, handback);
    } catch (final Exception e) {
      if (debugEnabled) log.debug(connection.getId() + " : error while removing notification filter", e);
    }
  }

  /**
   * Get the MBean notification information.
   * @return an array of {@link MBeanNotificationInfo} instances.
   */
  public MBeanNotificationInfo[] getNotificationInfo() {
    try {
      return connection.getNotificationInfo(mbeanName);
    } catch (final Exception e) {
      if (debugEnabled) log.debug(connection.getId() + " : error getting MBeanNotificationInfo[]", e);
    }
    return new MBeanNotificationInfo[0];
  }
}
