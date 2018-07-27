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

package com.sun.jmx.remote.opt.util;

import java.util.*;

import javax.management.remote.message.MBeanServerRequestMessage;

/**
 * 
 * @author Laurent Cohen
 */
public class GeneralUtils {
  /**
   * Mapping of method ids to readable names.
   */
  private static final Map<Integer, String> REQUEST_METHOD_NAMES = initRequestMethodNames();

  /**
   * @return a mapping of method ids to readable names.
   */
  private static Map<Integer, String> initRequestMethodNames() {
    final Map<Integer, String> map = new HashMap<>();
    map.put(MBeanServerRequestMessage.ADD_NOTIFICATION_LISTENERS, "ADD_NOTIFICATION_LISTENERS");
    map.put(MBeanServerRequestMessage.ADD_NOTIFICATION_LISTENER_OBJECTNAME, "ADD_NOTIFICATION_LISTENER_OBJECTNAME");
    map.put(MBeanServerRequestMessage.CREATE_MBEAN, "CREATE_MBEAN");
    map.put(MBeanServerRequestMessage.CREATE_MBEAN_PARAMS, "CREATE_MBEAN_PARAMS");
    map.put(MBeanServerRequestMessage.CREATE_MBEAN_LOADER, "CREATE_MBEAN_LOADER");
    map.put(MBeanServerRequestMessage.CREATE_MBEAN_LOADER_PARAMS, "CREATE_MBEAN_LOADER_PARAMS");
    map.put(MBeanServerRequestMessage.GET_ATTRIBUTE, "GET_ATTRIBUTE");
    map.put(MBeanServerRequestMessage.GET_ATTRIBUTES, "GET_ATTRIBUTES");
    map.put(MBeanServerRequestMessage.GET_DEFAULT_DOMAIN, "GET_DEFAULT_DOMAIN");
    map.put(MBeanServerRequestMessage.GET_DOMAINS, "GET_DOMAINS");
    map.put(MBeanServerRequestMessage.GET_MBEAN_COUNT, "GET_MBEAN_COUNT");
    map.put(MBeanServerRequestMessage.GET_MBEAN_INFO, "GET_MBEAN_INFO");
    map.put(MBeanServerRequestMessage.GET_OBJECT_INSTANCE, "GET_OBJECT_INSTANCE");
    map.put(MBeanServerRequestMessage.INVOKE, "INVOKE");
    map.put(MBeanServerRequestMessage.IS_INSTANCE_OF, "IS_INSTANCE_OF");
    map.put(MBeanServerRequestMessage.IS_REGISTERED, "IS_REGISTERED");
    map.put(MBeanServerRequestMessage.QUERY_MBEANS, "QUERY_MBEANS");
    map.put(MBeanServerRequestMessage.QUERY_NAMES, "QUERY_NAMES");
    map.put(MBeanServerRequestMessage.REMOVE_NOTIFICATION_LISTENER, "REMOVE_NOTIFICATION_LISTENER");
    map.put(MBeanServerRequestMessage.REMOVE_NOTIFICATION_LISTENER_FILTER_HANDBACK, "REMOVE_NOTIFICATION_LISTENER_FILTER_HANDBACK");
    map.put(MBeanServerRequestMessage.REMOVE_NOTIFICATION_LISTENER_OBJECTNAME, "REMOVE_NOTIFICATION_LISTENER_OBJECTNAME");
    map.put(MBeanServerRequestMessage.REMOVE_NOTIFICATION_LISTENER_OBJECTNAME_FILTER_HANDBACK, "REMOVE_NOTIFICATION_LISTENER_OBJECTNAME_FILTER_HANDBACK");
    map.put(MBeanServerRequestMessage.SET_ATTRIBUTE, "SET_ATTRIBUTE");
    map.put(MBeanServerRequestMessage.SET_ATTRIBUTES, "SET_ATTRIBUTES");
    map.put(MBeanServerRequestMessage.UNREGISTER_MBEAN, "UNREGISTER_MBEAN");
    return map;
  }

  /**
   * Get the name of a method given its id.
   * @param id the method id.
   * @return a readable for the method.
   */
  public static String getMethodName(final int id) {
    return REQUEST_METHOD_NAMES.get(id);
  }
}
