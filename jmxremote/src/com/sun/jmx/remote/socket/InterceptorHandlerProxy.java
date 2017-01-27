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

package com.sun.jmx.remote.socket;

import java.lang.reflect.Method;
import java.net.Socket;

import com.sun.jmx.remote.opt.util.ClassLogger;

/**
 * A proxy class which uses reflection to acess the {@code NetworkConnectionInterceptorHandler}.
 * @author Laurent Cohen
 */
class InterceptorHandlerProxy {
  /**
   * Logger for this class.
   */
  private static final ClassLogger logger = new ClassLogger("com.sun.jmx.remote.socket", "InterceptorHandlerProxy");
  /**
   * The class object for {@code org.jppf.comm.interceptor.NetworkConnectionInterceptorHandler}.
   */
  private static Class<?> handlerClass;
  /**
   * Whether there is at least one interceptor.
   */
  private static boolean hasInterceptor = false;
  /**
   * Method {@code org.jppf.comm.interceptor.NetworkConnectionInterceptorHandler.invokeOnAccept(Socket)}.
   */
  private static Method invokeOnAcceptMethod;
  /**
   * Method {@code org.jppf.comm.interceptor.NetworkConnectionInterceptorHandler.invokeOnConnect(Socket)}.
   */
  private static Method invokeOnConnectMethod;
  static {
    try {
      handlerClass = Class.forName("org.jppf.comm.interceptor.InterceptorHandler");
      invokeOnAcceptMethod = handlerClass.getMethod("invokeOnAccept", Socket.class);
      invokeOnConnectMethod = handlerClass.getMethod("invokeOnConnect", Socket.class);
      Method hasInterceptorMethod = handlerClass.getMethod("hasInterceptor");
      hasInterceptor = (Boolean) hasInterceptorMethod.invoke(null);
    } catch (Exception e) {
      logger.debug("static init", e);
    }
  }

  /**
   * Invoke all interceptors' {@code onConnect()} method with the specified socket.
   * @param connectedSocket the socket to intercept.
   * @return {@code true} if all {@code onConnect()} invocations returned {@code true}, {@code false} otherwise.
   */
  static boolean invokeOnConnect(final Socket connectedSocket) {
    if (!hasInterceptor) return true;
    try {
      return (Boolean) invokeOnConnectMethod.invoke(null, connectedSocket);
    } catch (Exception e) {
      logger.severe("invokeOnConnect", e);
    }
    return false;
  }

  /**
   * Invoke all interceptors' {@code onAccept()} method with the specified socket.
   * @param acceptedSocket the socket to intercept.
   * @return {@code true} if all {@code onAccept()} invocations returned {@code true}, {@code false} otherwise.
   */
  static boolean invokeOnAccept(final Socket acceptedSocket) {
    if (!hasInterceptor) return true;
    try {
      return (Boolean) invokeOnAcceptMethod.invoke(null, acceptedSocket);
    } catch (Exception e) {
      logger.severe("invokeOnAccept", e);
    }
    return false;
  }
}
