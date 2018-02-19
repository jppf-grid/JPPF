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

import org.jppf.*;
import org.jppf.utils.*;
import org.jppf.utils.collections.SoftReferenceValuesMap;

/**
 * An invocation used by MBean proxies created by JPPF.
 * @author Laurent Cohen
 */
public class MBeanInvocationHandler implements InvocationHandler {
  /**
   * Invocation of an arbitrary MBean method other than an attribute setter or getter.
   */
  private static final int INVOKE = 1;
  /**
   * Invocation of a {@code set<Attribute>(...)} method.
   */
  private static final int SET_ATTRIBUTE = 2;
  /**
   * Invocation of a {@code get<Attribute>()} method.
   */
  private static final int GET_ATTRIBUTE = 3;
  /**
   * Invocation of {@code addNotificationListener(NotificationListener, NotificationFilter, Object)}.
   */
  private static final int ADD_NOTIFICATION_LISTENER = 4;
  /**
   * Invocation of {@code removeNotificationListener(NotificationListener)}.
   */
  private static final int REMOVE_NOTIFICATION_LISTENER = 5;
  /**
   * Invocation of {@code removeNotificationListener(NotificationListener, NotificationFilter, Object)}.
   */
  private static final int REMOVE_NOTIFICATION_LISTENER_FILTER_NANDBACK = 6;
  /**
   * Method {@code addNotificationListener(NotificationListener, NotificationFilter, Object)}.
   */
  private static final Method ADD_NOTIFICATION_LISTENER_METHOD =
    ReflectionHelper.findMethod(NotificationBroadcaster.class, "addNotificationListener", NotificationListener.class, NotificationFilter.class, Object.class);
  /**
   * Method {@code removeNotificationListener(NotificationListener}.
   */
  private static final Method REMOVE_NOTIFICATION_LISTENER_METHOD = ReflectionHelper.findMethod(NotificationBroadcaster.class, "removeNotificationListener", NotificationListener.class);
  /**
   * Method {@code removeNotificationListener(NotificationListener, NotificationFilter, Object)}.
   */
  private static final Method REMOVE_NOTIFICATION_LISTENER_3_METHOD =
    ReflectionHelper.findMethod(NotificationEmitter.class, "removeNotificationListener", NotificationListener.class, NotificationFilter.class, Object.class);
  /**
   * Constant for signature of methods with no parameters.
   */
  private static final String[] EMPTY_SIG = {};
  /**
   * Mapping of methods to their descriptor.
   */
  private static final Map<Method, MethodInfo> methodMap = new SoftReferenceValuesMap<>();
  /**
   * Used to synchronize access to the method map.
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
   * Initialize with the specified MBean connection and MBean object name.
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

      case ADD_NOTIFICATION_LISTENER:
        mbsc.addNotificationListener(objectName, (NotificationListener) args[0], (NotificationFilter) args[1], args[2]);
        break;

      case REMOVE_NOTIFICATION_LISTENER:
        mbsc.removeNotificationListener(objectName, (NotificationListener) args[0]);
        break;

      case REMOVE_NOTIFICATION_LISTENER_FILTER_NANDBACK:
        mbsc.removeNotificationListener(objectName, (NotificationListener) args[0], (NotificationFilter) args[1], args[2]);
        break;

      default:
        throw new JPPFUnsupportedOperationException("unsupported method type for " + method);
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
      else if ("addNotificationListener".equals(method.getName())) {
        type = (ReflectionUtils.sameSignature(method, ADD_NOTIFICATION_LISTENER_METHOD)) ? ADD_NOTIFICATION_LISTENER : INVOKE;
      } else if ("removeNotificationListener".equals(method.getName())) {
        type = (ReflectionUtils.sameSignature(method, REMOVE_NOTIFICATION_LISTENER_METHOD))
          ? REMOVE_NOTIFICATION_LISTENER : (ReflectionUtils.sameSignature(method, REMOVE_NOTIFICATION_LISTENER_3_METHOD) ? REMOVE_NOTIFICATION_LISTENER_FILTER_NANDBACK : INVOKE);
      } else type = INVOKE;
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

        case ADD_NOTIFICATION_LISTENER:
        case REMOVE_NOTIFICATION_LISTENER:
        case REMOVE_NOTIFICATION_LISTENER_FILTER_NANDBACK:
          attribute = null;
          signature = EMPTY_SIG;
          break;

        default:
          throw new JPPFUnsupportedOperationException("unsupported method type for " + method);
      }
    }
  }
}
