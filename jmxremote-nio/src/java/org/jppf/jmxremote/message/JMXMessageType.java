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

package org.jppf.jmxremote.message;

import javax.management.*;

/**
 * ENumeration of the possible types of JMX messages.
 * @author Laurent Cohen
 */
public enum JMXMessageType {
  /**
   * Identifier for the initial handshake of a copnnection.
   */
  CONNECT,
  /**
   * Identifier for the method {@link MBeanServerConnection#addNotificationListener(ObjectName, NotificationListener, NotificationFilter, Object)}.
   */
  ADD_NOTIFICATION_LISTENER,
  /**
   * Identifier for the method {@link MBeanServerConnection#addNotificationListener(ObjectName, ObjectName, NotificationFilter, Object)}.
   */
  ADD_NOTIFICATION_LISTENER_OBJECTNAME,
  /**
   * Identifier for the method {@link MBeanServerConnection#createMBean(String, ObjectName)}.
   */
  CREATE_MBEAN,
  /**
   * Identifier for the method {@link MBeanServerConnection#createMBean(String, ObjectName, Object[], String[])}.
   */
  CREATE_MBEAN_PARAMS,
  /**
   * Identifier for the method {@link MBeanServerConnection#createMBean(String, ObjectName, ObjectName)}.
   */
  CREATE_MBEAN_LOADER,
  /**
   * Identifier for the method {@link MBeanServerConnection#createMBean(String, ObjectName, ObjectName, Object[], String[])}.
   */
  CREATE_MBEAN_LOADER_PARAMS,
  /**
   * Identifier for the method {@link MBeanServerConnection#getAttribute(ObjectName, String)}.
   */
  GET_ATTRIBUTE,
  /**
   * Identifier for the method {@link MBeanServerConnection#getAttributes(ObjectName, String[])}.
   */
  GET_ATTRIBUTES,
  /**
   * Identifier for the method {@link MBeanServerConnection#getDefaultDomain()}.
   */
  GET_DEFAULT_DOMAIN,
  /**
   * Identifier for the method {@link MBeanServerConnection#getDomains()}.
   */
  GET_DOMAINS,
  /**
   * Identifier for the method {@link MBeanServerConnection#getMBeanCount()}.
   */
  GET_MBEAN_COUNT,
  /**
   * Identifier for the method {@link MBeanServerConnection#getMBeanInfo(ObjectName)}.
   */
  GET_MBEAN_INFO,
  /**
   * Identifier for the method {@link MBeanServerConnection#getObjectInstance(ObjectName)}.
   */
  GET_OBJECT_INSTANCE,
  /**
   * Identifier for the method {@link MBeanServerConnection#invoke(ObjectName, String, Object[], String[])}.
   */
  INVOKE,
  /**
   * Identifier for the method {@link MBeanServerConnection#isInstanceOf(ObjectName, String)}.
   */
  IS_INSTANCE_OF,
  /**
   * Identifier for the method {@link MBeanServerConnection#isRegistered(ObjectName)}.
   */
  IS_REGISTERED,
  /**
   * Identifier for the method {@link MBeanServerConnection#queryMBeans(ObjectName, QueryExp)}.
   */
  QUERY_MBEANS,
  /**
   * Identifier for the method {@link MBeanServerConnection#queryNames(ObjectName, QueryExp)}.
   */
  QUERY_NAMES,
  /**
   * Identifier for the method {@link MBeanServerConnection#removeNotificationListener(ObjectName, NotificationListener)}.
   */
  REMOVE_NOTIFICATION_LISTENER,
  /**
   *  Identifier for the method {@link MBeanServerConnection#removeNotificationListener(ObjectName, NotificationListener, NotificationFilter, Object)}.
   */
  REMOVE_NOTIFICATION_LISTENER_FILTER_HANDBACK,
  /**
   * Identifier for the method {@link MBeanServerConnection#removeNotificationListener(ObjectName, ObjectName)}.
   */
  REMOVE_NOTIFICATION_LISTENER_OBJECTNAME,
  /**
   * Identifier for the method {@link MBeanServerConnection#removeNotificationListener(ObjectName, ObjectName, NotificationFilter, Object)}.
   */
  REMOVE_NOTIFICATION_LISTENER_OBJECTNAME_FILTER_HANDBACK,
  /**
   * Identifier for the method {@link MBeanServerConnection#setAttribute(ObjectName, Attribute)}.
   */
  SET_ATTRIBUTE,
  /**
   * Identifier for the method {@link MBeanServerConnection#setAttributes(ObjectName, AttributeList)}.
   */
  SET_ATTRIBUTES,
  /**
   * Identifier for the method {@link MBeanServerConnection#unregisterMBean(ObjectName)}.
   */
  UNREGISTER_MBEAN,
  /**
   * Identifier for a notification.
   */
  NOTIFICATION
}
