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

package org.jppf.comm.interceptor;

import java.lang.reflect.Method;
import java.net.Socket;
import java.nio.channels.SocketChannel;
import java.util.*;

import org.jppf.comm.socket.SocketWrapper;
import org.jppf.utils.ServiceFinder;

/**
 * This class loads, and provides access to, the {@link NetworkConnectionInterceptor}s discovered via SPI.
 * @author Laurent Cohen
 * @since 5.2
 * @exclude
 */
public class InterceptorHandler {
  /**
   * Logger for this class.
   */
  private static Object log;
  /**
   * Determines whether the debug level is enabled in the logging configuration, without the cost of a method call.
   */
  private static boolean debugEnabled = false;
  /**
   * Method "Logger.debug(String)".
   */
  private static Method logDebugMethod;
  static {
    // using reflection because slf4j jars may not be in the classpath
    try {
      Class<?> loggerFactoryClass = Class.forName("org.slf4j.LoggerFactory");
      Method m = loggerFactoryClass.getMethod("getLogger", Class.class);
      log = m.invoke(null, InterceptorHandler.class);
      m = log.getClass().getMethod("isDebugEnabled");
      debugEnabled = (Boolean) m.invoke(log);
      logDebugMethod = log.getClass().getMethod("debug", String.class);
    } catch(Throwable ignore) {
      ignore.printStackTrace();
    }
  }
  /**
   * The list of interceptors loaded via SPI.
   */
  static final List<NetworkConnectionInterceptor> INTERCEPTORS = Collections.unmodifiableList(loadInterceptors());

  /**
   * Load the interceptors via the SPI mechanism.
   * @return a list of {@link NetworkConnectionInterceptor} instances, possibly empty.
   */
  private static List<NetworkConnectionInterceptor> loadInterceptors() {
    ServiceFinder sf = new ServiceFinder();
    List<NetworkConnectionInterceptor> result = sf.findProviders(NetworkConnectionInterceptor.class);
    if (debugEnabled) debugLog("found %d interceptors in the classpath", result.size());
    return result;
  }

  /**
   * Determine whether at least one interceptor was succesfully loaded.
   * @return {@code true} if there is at least one interceptor, {@code false} otherwise.
   */
  public static boolean hasInterceptor() {
    return !INTERCEPTORS.isEmpty();
  }

  /**
   * Invoke all interceptors' {@code onConnect()} method with the specified socket.
   * @param connectedSocket the socket to intercept.
   * @return {@code true} if all {@code onConnect()} invocations returned {@code true}, {@code false} otherwise.
   */
  public static boolean invokeOnConnect(final Socket connectedSocket) {
    if (debugEnabled) debugLog("invoking onConnect() on %s", connectedSocket);
    for (NetworkConnectionInterceptor interceptor: INTERCEPTORS) {
      if (!interceptor.onConnect(connectedSocket)) return false;
    }
    return true;
  }

  /**
   * Invoke all interceptors' {@code onConnect()} method with the specified socket channel.
   * @param connectedChannel the socket channel to intercept.
   * @return {@code true} if all {@code onConnect()} invocations returned {@code true}, {@code false} otherwise.
   */
  public static boolean invokeOnConnect(final SocketChannel connectedChannel) {
    if (debugEnabled) debugLog("invoking onConnect() on %s", connectedChannel);
    for (NetworkConnectionInterceptor interceptor: INTERCEPTORS) {
      if (!interceptor.onConnect(connectedChannel)) return false;
    }
    return true;
  }

  /**
   * Invoke all interceptors' {@code onAccept()} method with the specified socket.
   * @param acceptedSocket the socket to intercept.
   * @return {@code true} if all {@code onAccept()} invocations returned {@code true}, {@code false} otherwise.
   */
  public static boolean invokeOnAccept(final Socket acceptedSocket) {
    if (debugEnabled) debugLog("invoking onAccept() on %s", acceptedSocket);
    for (NetworkConnectionInterceptor interceptor: INTERCEPTORS) {
      if (!interceptor.onAccept(acceptedSocket)) return false;
    }
    return true;
  }

  /**
   * Invoke all interceptors' {@code onAccept()} method with the specified socket channel.
   * @param acceptedChannel the socket channel to intercept.
   * @return {@code true} if all {@code onAccept()} invocations returned {@code true}, {@code false} otherwise.
   */
  public static boolean invokeOnAccept(final SocketChannel acceptedChannel) {
    if (debugEnabled) debugLog("invoking onAccept() on %s", acceptedChannel);
    for (NetworkConnectionInterceptor interceptor: INTERCEPTORS) {
      if (!interceptor.onAccept(acceptedChannel)) return false;
    }
    return true;
  }

  /**
   * Invoke all interceptors' {@code onConnect()} method with the specified socket wrapper.
   * @param socketWrapper holds the socket to intercept.
   * @return {@code true} if all {@code onConnect()} invocations returned {@code true}, {@code false} otherwise.
   */
  public static boolean invokeOnConnect(final SocketWrapper socketWrapper) {
    if (socketWrapper == null) return true;
    return hasInterceptor() ? invokeOnConnect(socketWrapper.getSocket()) : true;
  }

  /**
   * Invoke all interceptors' {@code onAccept()} method with the specified socket wrapper.
   * @param socketWrapper holds the socket to intercept.
   * @return {@code true} if all {@code onAccept()} invocations returned {@code true}, {@code false} otherwise.
   */
  public static boolean invokeOnAccept(final SocketWrapper socketWrapper) {
    if (socketWrapper == null) return true;
    return hasInterceptor() ? invokeOnAccept(socketWrapper.getSocket()) : true;
  }

  /**
   * Log the specified message at debug level.
   * @param format the message format.
   * @param params the message parameters.
   */
  private static void debugLog(final String format, final Object...params) {
    if (logDebugMethod != null) {
      try {
        logDebugMethod.invoke(log, String.format(format, params));
      } catch (@SuppressWarnings("unused") Exception ignore) {
      }
    }
  }
}
