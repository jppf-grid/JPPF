/*
 * JPPF.
 * Copyright (C) 2005-2015 JPPF Team.
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

package org.jppf.utils.hooks;

import java.lang.reflect.Method;
import java.util.Arrays;

import org.jppf.caching.*;
import org.jppf.utils.*;
import org.slf4j.*;

/**
 * Instance of a {@link Hook}.
 * @param <E> the type of th ehook's interface.
 * @author Laurent Cohen
 */
public class HookInstance<E> {
  /**
   * Logger for this class.
   */
  private static Logger log = LoggerFactory.getLogger(HookInstance.class);
  /**
   * Determines whether debug-level logging is enabled.
   */
  private static boolean debugEnabled = log.isDebugEnabled();
  /**
   * A cache of invoked methods.
   */
  private final JPPFMapCache<String, Method> methodCache = new JPPFSynchronizedSoftCache<>();
  /**
   * The hook instance on which to invoke the method.
   */
  private final E instance;

  /**
   * Initialize this hook with the specified method and instance.
   * @param instance the hook instance on which to invoke the method.
   */
  public HookInstance(final E instance) {
    this.instance = instance;
  }

  /**
   * Invoke the specified method on the hook instance, using the specified parameters.
   * @param methodName the name of the method to invoke.
   * @param parameters the parmeters to use in the method invocation.
   * @return the return value of th emethod, or <code>null</code> if the method does not return anything.
   */
  public Object invoke(final String methodName, final Object...parameters) {
    int nbArgs = (parameters == null) ? 0 : parameters.length;
    try {
      String key = methodName + ':' + nbArgs;
      Method method = methodCache.get(key);
      if (method == null) {
        method = ReflectionHelper.findMethod(instance.getClass(), methodName, nbArgs);
        methodCache.put(key, method);
      }
      return method.invoke(instance, parameters);
    } catch (Exception e) {
      String format = "failed to invoke '{}()' with params={} on {} with exception={}";
      Object[] params = new Object[] {methodName, nbArgs == 0 ? "{}" : Arrays.asList(parameters), this, debugEnabled ? ExceptionUtils.getStackTrace(e) : ExceptionUtils.getMessage(e)};
      if (debugEnabled) log.debug(format, params);
      else log.warn(format, params);
    }
    return null;
  }

  /**
   * Cleanup this hook instance and release its resources.
   */
  public void dispose() {
    methodCache.clear();
  }

  @Override
  public String toString() {
    return new StringBuilder(getClass().getSimpleName()).append('[').append("instance=").append(instance).append(']').toString();
  }

  /**
   * Get the concrete instance.
   * @return an instance of a hook interface implementation.
   */
  public E getInstance() {
    return instance;
  }
}
