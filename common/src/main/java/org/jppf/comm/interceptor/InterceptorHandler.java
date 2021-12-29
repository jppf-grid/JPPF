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

package org.jppf.comm.interceptor;

import java.net.Socket;
import java.nio.channels.SocketChannel;
import java.util.*;

import org.jppf.comm.socket.SocketWrapper;
import org.jppf.utils.*;
import org.slf4j.*;

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
  private static final Logger log = LoggerFactory.getLogger(InterceptorHandler.class);
  /**
   * Determines whether debug-level logging is enabled.
   */
  private static final boolean debugEnabled = log.isDebugEnabled();
  /**
   * The list of interceptors loaded via SPI.
   */
  private static List<NetworkConnectionInterceptor> INTERCEPTORS;
  /**
   * Whether an attempt to load the interceptors was made.
   */
  private static boolean lookedUp;

  /**
   * Load the interceptors via the SPI mechanism.
   * @return a list of {@link NetworkConnectionInterceptor} instances, possibly empty.
   */
  private static List<NetworkConnectionInterceptor> loadInterceptors() {
    if (debugEnabled) log.debug("loading network interceptors");
    log.warn("loading network interceptors");
    final ServiceFinder sf = new ServiceFinder();
    final List<NetworkConnectionInterceptor> result = sf.findProviders(NetworkConnectionInterceptor.class);
    if (debugEnabled) log.debug("found {} interceptors in the classpath: {}", result.size(), result);
    return Collections.unmodifiableList(result);
  }

  /**
   * Determine whether at least one interceptor was succesfully loaded.
   * @return {@code true} if there is at least one interceptor, {@code false} otherwise.
   */
  public static boolean hasInterceptor() {
    final List<NetworkConnectionInterceptor> interceptors = getInterceptors();
    return (interceptors != null) && !interceptors.isEmpty();
  }

  /**
   * Invoke all interceptors' {@code onConnect()} method with the specified socket.
   * @param connectedSocket the socket to intercept.
   * @param channelDescriptor provdes information on the connected channel.
   * @return {@code true} if all {@code onConnect()} invocations returned {@code true}, {@code false} otherwise.
   */
  public static boolean invokeOnConnect(final Socket connectedSocket, final JPPFChannelDescriptor channelDescriptor) {
    if (!hasInterceptor()) return true;
    if (debugEnabled) log.debug("invoking onConnect() on {}, channelDescriptor = [}", connectedSocket, channelDescriptor);
    for (NetworkConnectionInterceptor interceptor: getInterceptors()) {
      if (debugEnabled) log.debug("invoking onConnect() of {}", interceptor);
      if (!interceptor.onConnect(connectedSocket, channelDescriptor)) return false;
    }
    return true;
  }

  /**
   * Invoke all interceptors' {@code onConnect()} method with the specified socket channel.
   * @param connectedChannel the socket channel to intercept.
   * @param channelDescriptor provdes information on the connected channel.
   * @return {@code true} if all {@code onConnect()} invocations returned {@code true}, {@code false} otherwise.
   */
  public static boolean invokeOnConnect(final SocketChannel connectedChannel, final JPPFChannelDescriptor channelDescriptor) {
    if (!hasInterceptor()) return true;
    if (debugEnabled) log.debug("invoking onConnect() on {}, channelDescriptor = {}", connectedChannel, channelDescriptor);
    for (NetworkConnectionInterceptor interceptor: getInterceptors()) {
      if (debugEnabled) log.debug("invoking onConnect() of {}", interceptor);
      if (!interceptor.onConnect(connectedChannel, channelDescriptor)) return false;
    }
    return true;
  }

  /**
   * Invoke all interceptors' {@code onAccept()} method with the specified socket.
   * @param acceptedSocket the socket to intercept.
   * @param channelDescriptor provdes information on the accepted channel.
   * @return {@code true} if all {@code onAccept()} invocations returned {@code true}, {@code false} otherwise.
   */
  public static boolean invokeOnAccept(final Socket acceptedSocket, final JPPFChannelDescriptor channelDescriptor) {
    if (!hasInterceptor()) return true;
    if (debugEnabled) log.debug("invoking onAccept() on {}, channelDescriptor = {}", acceptedSocket, channelDescriptor);
    for (NetworkConnectionInterceptor interceptor: getInterceptors()) {
      if (debugEnabled) log.debug("invoking onAccept() of {}", interceptor);
      if (!interceptor.onAccept(acceptedSocket, channelDescriptor)) return false;
    }
    return true;
  }

  /**
   * Invoke all interceptors' {@code onAccept()} method with the specified socket channel.
   * @param acceptedChannel the socket channel to intercept.
   * @param channelDescriptor provdes information on the accepted channel.
   * @return {@code true} if all {@code onAccept()} invocations returned {@code true}, {@code false} otherwise.
   */
  public static boolean invokeOnAccept(final SocketChannel acceptedChannel, final JPPFChannelDescriptor channelDescriptor) {
    if (!hasInterceptor()) return true;
    if (debugEnabled) log.debug("invoking onAccept() on {}, channelDescriptor = {}", acceptedChannel, channelDescriptor);
    for (NetworkConnectionInterceptor interceptor: getInterceptors()) {
      if (debugEnabled) log.debug("invoking onAccept() of {}", interceptor);
      if (!interceptor.onAccept(acceptedChannel, channelDescriptor)) return false;
    }
    return true;
  }

  /**
   * Invoke all interceptors' {@code onConnect()} method with the specified socket wrapper.
   * @param socketWrapper holds the socket to intercept.
   * @param channelDescriptor provdes information on the connected channel.
   * @return {@code true} if all {@code onConnect()} invocations returned {@code true}, {@code false} otherwise.
   */
  public static boolean invokeOnConnect(final SocketWrapper socketWrapper, final JPPFChannelDescriptor channelDescriptor) {
    if (socketWrapper == null) return true;
    return invokeOnConnect(socketWrapper.getSocket(), channelDescriptor);
  }

  /**
   * Invoke all interceptors' {@code onAccept()} method with the specified socket wrapper.
   * @param socketWrapper holds the socket to intercept.
   * @param channelDescriptor provdes information on the accepted channel.
   * @return {@code true} if all {@code onAccept()} invocations returned {@code true}, {@code false} otherwise.
   */
  public static boolean invokeOnAccept(final SocketWrapper socketWrapper, final JPPFChannelDescriptor channelDescriptor) {
    if (socketWrapper == null) return true;
    return invokeOnAccept(socketWrapper.getSocket(), channelDescriptor);
  }

  /**
   * @return the list of interceptors loaded via SPI.
   */
  static synchronized List<NetworkConnectionInterceptor> getInterceptors() {
    if (!lookedUp && (INTERCEPTORS == null)) {
      if (debugEnabled) log.debug("loading network interceptors");
      log.warn("loading network interceptors");
      INTERCEPTORS = loadInterceptors();
      lookedUp = true;
    }
    return INTERCEPTORS;
  }
}
