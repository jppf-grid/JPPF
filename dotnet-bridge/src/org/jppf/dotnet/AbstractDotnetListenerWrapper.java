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

package org.jppf.dotnet;

import java.lang.reflect.Method;
import java.util.*;

import org.jppf.JPPFRuntimeException;
import org.jppf.utils.*;
import org.slf4j.*;

/**
 * 
 * @author Laurent Cohen
 * @since 5.0
 * @exclude
 */
public class AbstractDotnetListenerWrapper {
  /**
   * Logger for this class.
   */
  private static Logger log = LoggerFactory.getLogger(AbstractDotnetListenerWrapper.class);
  /**
   * Determines whether the debug level is enabled in the log configuration, without the cost of a method call.
   */
  private final boolean debugEnabled;
  /**
   * Cache mapping method names to reflection {@link Method} objects.
   */
  protected final Map<String, Method> methodMap = new HashMap<>();
  /**
   * The object which propagates event notifications to the actual .Net listener.
   */
  protected final system.Object dotnetDispatcher;
  /**
   * 
   */
  private final String namePrefix;

  /**
   * Initialize this wrapper with the .Net dispatcher and names of notification methods.
   * @param debugEnabled whether debug output is enabledr.
   * @param dotnetDispatcher the object which propagates event notifications to the actual .Net listener.
   * @param methodNames the names of the .Net listener notification methods.
   */
  public AbstractDotnetListenerWrapper(final boolean debugEnabled, final system.Object dotnetDispatcher, final String...methodNames) {
    if (debugEnabled) log.debug(String.format("init of %s wrapper for dispatcher %s, methods=%s", getClass().getSimpleName(), dotnetDispatcher, StringUtils.arrayToString(methodNames)));
    if (dotnetDispatcher == null) throw new IllegalArgumentException(".Net listener cannot be null");
    this.dotnetDispatcher = dotnetDispatcher;
    this.debugEnabled = debugEnabled;
    namePrefix = dotnetDispatcher.getClass().getSimpleName();
    try {
      Class<?> c = dotnetDispatcher.getClass();
      for (String name: methodNames) {
        Method method = c.getMethod(name, Object.class);
        methodMap.put(name, method);
      }
    } catch (Exception e) {
      e.printStackTrace();
      if (e instanceof RuntimeException) throw (RuntimeException) e;
      throw new JPPFRuntimeException("Error initializing " + getClass().getName(), e);
    }
  }

  /**
   * Delegate the specified event to the specified .Net method
   * @param event the event to send.
   * @param methodName the name of the method to invoke on the proxy to the .Net listener.
   */
  protected void delegate(final Object event, final String methodName) {
    if (dotnetDispatcher == null) return;
    if (debugEnabled) log.debug(String.format("delegating to method %s.%s() event=%s", namePrefix, methodName, event));
    try {
      Method m = methodMap.get(methodName);
      m.invoke(dotnetDispatcher, event);
    } catch (Exception e) {
      log.error(String.format("error invoking %s.%s() with event=%s :%n%s", namePrefix, methodName, event, ExceptionUtils.getStackTrace(e)));
    }
  }
}
