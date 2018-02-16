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

import java.lang.reflect.*;
import java.util.Map;
import java.util.concurrent.locks.*;

import javax.management.*;

import org.jppf.JPPFException;
import org.jppf.utils.ReflectionUtils;
import org.jppf.utils.collections.SoftReferenceValuesMap;

/**
 *
 * @author Laurent Cohen
 */
public class MBeanInvocationHandler implements InvocationHandler {
  /**
   * Possible types of methods to invoke.
   */
  private static final int INVOKE = 1, SET_ATTRIBUTE = 2, GET_ATTRIBUTE = 3,
    ADD_NOTIFICATION_LISTENER = 4, ADD_NOTIFICATION_LISTENER_FILTER_NANDBACK = 5, REMOVE_NOTIFICATION_LISTENER = 6;
  /**
   * Constant for signature of methods with no parameters.
   */
  private static final String[] EMPTY_SIG = {};
  /**
   * Mapping of methods to their descriptor.
   */
  private static final Map<Method, MethodInfo> methodMap = new SoftReferenceValuesMap<>();
  /**
   * Used to synchronize acces to the map.
   */
  private static final Lock mapLock = new ReentrantLock();
  /**
   * The MBean server connection.
   */
  private final MBeanServerConnection mbsc;
  /**
   * The object name of the MBean to proxy.
   */
  private final ObjectName objectName;

  /**
   * 
   * @param mbsc the MBean server connection..
   * @param objectName the object name of the MBean to proxy.
   */
  public MBeanInvocationHandler(final MBeanServerConnection mbsc, final ObjectName objectName) {
    this.mbsc = mbsc;
    this.objectName = objectName;
  }

  @Override
  public Object invoke(final Object proxy, final Method method, final Object[] args) throws Throwable {
    MethodInfo info;
    mapLock.lock();
    try {
      info = methodMap.get(method);
      if (info == null) {
        info = new MethodInfo(method);
        methodMap.put(method, info);
      }
    } finally {
      mapLock.unlock();
    }
    switch(info.type) {
      case SET_ATTRIBUTE:
        mbsc.setAttribute(objectName, new Attribute(info.attribute, args[0]));
        break;
      case GET_ATTRIBUTE:
        return mbsc.getAttribute(objectName, info.attribute);
      case INVOKE:
        return mbsc.invoke(objectName, method.getName(), args, info.signature);
      default:
        throw new JPPFException("unsupported method type for " + method);
    }
    return null;
  }

  /**
   * Create a new proxy instance for the specified MBean.
   * @param <T> the type of the proxy interface.
   * @param inf the class of the MBean's infterface.
   * @param mbsc the MBean server connection.
   * @param objectName the object name of the MBean to proxy.
   * @return a proxy of the same type as the provided interface.
   */
  @SuppressWarnings("unchecked")
  public static <T> T newMBeanProxy(final Class<T> inf, final MBeanServerConnection mbsc, final ObjectName objectName) {
    return (T) Proxy.newProxyInstance(inf.getClassLoader(), new Class<?>[] {inf, NotificationEmitter.class}, new MBeanInvocationHandler(mbsc, objectName));
  }

  /**
   * Instances of this class describe a method invoked by the invocation handler.
   */
  private static final class MethodInfo {
    /**
     * The method signature, if any.
     */
    private final String[] signature;
    /**
     * The attribute name, if any.
     */
    private final String attribute;
    /**
     * The type of operation performed by the method, i.e. INVOKE, SET_ATTRIBUTE or GET_ATTRIBUTE.
     */
    private final int type;

    /**
     * Initialize the info about the method.
     * @param method the mthod to describe.
     * @throws Exception if any error occurs.
     */
    private MethodInfo(final Method method) throws Exception {
      if (ReflectionUtils.isSetter(method)) type = SET_ATTRIBUTE;
      else if (ReflectionUtils.isGetter(method)) type = GET_ATTRIBUTE;
      //else if ("addNotificationListener
      else type = INVOKE;
      switch(type) {
        case SET_ATTRIBUTE:
        case GET_ATTRIBUTE:
          signature = EMPTY_SIG;
          attribute = ReflectionUtils.getMBeanAttributeName(method);
          break;

        case INVOKE:
          attribute = null;
          final Class<?>[] paramTypes = method.getParameterTypes();
          if (paramTypes.length <= 0) signature = EMPTY_SIG;
          else {
            signature = new String[paramTypes.length];
            for (int i=0; i<paramTypes.length; i++) signature[i] = paramTypes[i].getName();
          }
          break;

        default:
          throw new JPPFException("unsupported method type for " + method);
      }
    }
  }
}
