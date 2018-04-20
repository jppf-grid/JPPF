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

import java.util.*;

import javax.management.*;

/**
 * Enumeration of the possible types of JMX messages.
 * @author Laurent Cohen
 */
public class JMXMessageType {
  /**
   * Identifier for the initial handshake of a connection.
   */
  public static final byte CONNECT = 1;
  /**
   * Identifier for a connection close request. A close request does not expect a response.
   */
  public static final byte CLOSE = 2;
  /**
   * Identifier for the method {@link MBeanServerConnection#addNotificationListener(ObjectName, NotificationListener, NotificationFilter, Object)}.
   */
  public static final byte ADD_NOTIFICATION_LISTENER = 3;
  /**
   * Identifier for the method {@link MBeanServerConnection#addNotificationListener(ObjectName, ObjectName, NotificationFilter, Object)}.
   */
  public static final byte ADD_NOTIFICATION_LISTENER_OBJECTNAME = 4;
  /**
   * Identifier for the method {@link MBeanServerConnection#createMBean(String, ObjectName)}.
   */
  public static final byte CREATE_MBEAN = 5;
  /**
   * Identifier for the method {@link MBeanServerConnection#createMBean(String, ObjectName, Object[], String[])}.
   */
  public static final byte CREATE_MBEAN_PARAMS = 6;
  /**
   * Identifier for the method {@link MBeanServerConnection#createMBean(String, ObjectName, ObjectName)}.
   */
  public static final byte CREATE_MBEAN_LOADER = 7;
  /**
   * Identifier for the method {@link MBeanServerConnection#createMBean(String, ObjectName, ObjectName, Object[], String[])}.
   */
  public static final byte CREATE_MBEAN_LOADER_PARAMS = 8;
  /**
   * Identifier for the method {@link MBeanServerConnection#getAttribute(ObjectName, String)}.
   */
  public static final byte GET_ATTRIBUTE = 9;
  /**
   * Identifier for the method {@link MBeanServerConnection#getAttributes(ObjectName, String[])}.
   */
  public static final byte GET_ATTRIBUTES = 10;
  /**
   * Identifier for the method {@link MBeanServerConnection#getDefaultDomain()}.
   */
  public static final byte GET_DEFAULT_DOMAIN = 11;
  /**
   * Identifier for the method {@link MBeanServerConnection#getDomains()}.
   */
  public static final byte GET_DOMAINS = 12;
  /**
   * Identifier for the method {@link MBeanServerConnection#getMBeanCount()}.
   */
  public static final byte GET_MBEAN_COUNT = 13;
  /**
   * Identifier for the method {@link MBeanServerConnection#getMBeanInfo(ObjectName)}.
   */
  public static final byte GET_MBEAN_INFO = 14;
  /**
   * Identifier for the method {@link MBeanServerConnection#getObjectInstance(ObjectName)}.
   */
  public static final byte GET_OBJECT_INSTANCE = 15;
  /**
   * Identifier for the method {@link MBeanServerConnection#invoke(ObjectName, String, Object[], String[])}.
   */
  public static final byte INVOKE = 16;
  /**
   * Identifier for the method {@link MBeanServerConnection#isInstanceOf(ObjectName, String)}.
   */
  public static final byte IS_INSTANCE_OF = 17;
  /**
   * Identifier for the method {@link MBeanServerConnection#isRegistered(ObjectName)}.
   */
  public static final byte IS_REGISTERED = 18;
  /**
   * Identifier for the method {@link MBeanServerConnection#queryMBeans(ObjectName, QueryExp)}.
   */
  public static final byte QUERY_MBEANS = 19;
  /**
   * Identifier for the method {@link MBeanServerConnection#queryNames(ObjectName, QueryExp)}.
   */
  public static final byte QUERY_NAMES = 20;
  /**
   * Identifier for the method {@link MBeanServerConnection#removeNotificationListener(ObjectName, NotificationListener)}.
   */
  public static final byte REMOVE_NOTIFICATION_LISTENER = 21;
  /**
   *  Identifier for the method {@link MBeanServerConnection#removeNotificationListener(ObjectName, NotificationListener, NotificationFilter, Object)}.
   */
  public static final byte REMOVE_NOTIFICATION_LISTENER_FILTER_HANDBACK = 22;
  /**
   * Identifier for the method {@link MBeanServerConnection#removeNotificationListener(ObjectName, ObjectName)}.
   */
  public static final byte REMOVE_NOTIFICATION_LISTENER_OBJECTNAME = 23;
  /**
   * Identifier for the method {@link MBeanServerConnection#removeNotificationListener(ObjectName, ObjectName, NotificationFilter, Object)}.
   */
  public static final byte REMOVE_NOTIFICATION_LISTENER_OBJECTNAME_FILTER_HANDBACK = 24;
  /**
   * Identifier for the method {@link MBeanServerConnection#setAttribute(ObjectName, Attribute)}.
   */
  public static final byte SET_ATTRIBUTE = 25;
  /**
   * Identifier for the method {@link MBeanServerConnection#setAttributes(ObjectName, AttributeList)}.
   */
  public static final byte SET_ATTRIBUTES = 26;
  /**
   * Identifier for the method {@link MBeanServerConnection#unregisterMBean(ObjectName)}.
   */
  public static final byte UNREGISTER_MBEAN = 27;
  /**
   * Identifier for a notification.
   */
  public static final byte NOTIFICATION = 28;
  /**
   * A non-modifiable mapping of type values to readable names.
   */
  private static final Map<Byte, String> TYPE_NAMES = Collections.unmodifiableMap(initTypeNames());

  /**
   * Get a name corresponding to a message type.
   * @param type the type of the message for which to get a name.
   * @return a readable message name.
   */
  public static final String name(final byte type) {
    final String s = TYPE_NAMES.get(type);
    return (s == null) ? Byte.toString(type) : s;
  }

  /**
   * @return a mapping of message type values to readable names.
   */
  private static Map<Byte, String> initTypeNames() {
    final Map<Byte, String> map = new HashMap<>();
    map.put(CONNECT, "CONNECT");
    map.put(CLOSE, "CLOSE");
    map.put(ADD_NOTIFICATION_LISTENER, "ADD_NOTIFICATION_LISTENER");
    map.put(ADD_NOTIFICATION_LISTENER_OBJECTNAME, "ADD_NOTIFICATION_LISTENER_OBJECTNAME");
    map.put(CREATE_MBEAN, "CREATE_MBEAN");
    map.put(CREATE_MBEAN_PARAMS, "CREATE_MBEAN_PARAMS");
    map.put(CREATE_MBEAN_LOADER, "CREATE_MBEAN_LOADER");
    map.put(CREATE_MBEAN_LOADER_PARAMS, "CREATE_MBEAN_LOADER_PARAMS");
    map.put(GET_ATTRIBUTE, "GET_ATTRIBUTE");
    map.put(GET_ATTRIBUTES, "GET_ATTRIBUTES");
    map.put(GET_DEFAULT_DOMAIN, "GET_DEFAULT_DOMAIN");
    map.put(GET_DOMAINS, "GET_DOMAINS");
    map.put(GET_MBEAN_COUNT, "GET_MBEAN_COUNT");
    map.put(GET_MBEAN_INFO, "GET_MBEAN_INFO");
    map.put(GET_OBJECT_INSTANCE, "GET_OBJECT_INSTANCE");
    map.put(INVOKE, "INVOKE");
    map.put(IS_INSTANCE_OF, "IS_INSTANCE_OF");
    map.put(IS_REGISTERED, "IS_REGISTERED");
    map.put(QUERY_MBEANS, "QUERY_MBEANS");
    map.put(QUERY_NAMES, "QUERY_NAMES");
    map.put(REMOVE_NOTIFICATION_LISTENER, "REMOVE_NOTIFICATION_LISTENER");
    map.put(REMOVE_NOTIFICATION_LISTENER_FILTER_HANDBACK, "REMOVE_NOTIFICATION_LISTENER_FILTER_HANDBACK");
    map.put(REMOVE_NOTIFICATION_LISTENER_OBJECTNAME, "REMOVE_NOTIFICATION_LISTENER_OBJECTNAME");
    map.put(REMOVE_NOTIFICATION_LISTENER_OBJECTNAME_FILTER_HANDBACK, "REMOVE_NOTIFICATION_LISTENER_OBJECTNAME_FILTER_HANDBACK");
    map.put(SET_ATTRIBUTE, "SET_ATTRIBUTE");
    map.put(SET_ATTRIBUTES, "SET_ATTRIBUTES");
    map.put(UNREGISTER_MBEAN, "UNREGISTER_MBEAN");
    map.put(NOTIFICATION, "NOTIFICATION");
    return map;
  }
}
