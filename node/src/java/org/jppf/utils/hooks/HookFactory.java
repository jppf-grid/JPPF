/*
 * JPPF.
 * Copyright (C) 2005-2013 JPPF Team.
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

import java.util.*;


/**
 * 
 * @author Laurent Cohen
 */
public class HookFactory
{
  /**
   * Mapping of the hooks to their interface name.
   */
  private static Map<String, Hook<?>> hookMap = new Hashtable<String, Hook<?>>();

  /**
   * Register a hook defined via a configuration property, of which a single instance is discovered and invoked.
   * @param <T> the type of the hook interface.
   * @param property the name of the property used to specify the hook implementation class name.
   * @param infClass the class of the hook's interface.
   * @param defaultImpl the default implementation, which may be null.
   * @param methodName the name of the implementation method to invoke.
   * @param classLoader the class loader used to load the implementation.
   * @return the registered hook as a {@link Hook} instance.
   */
  public static <T> Hook<T> registerConfigSingleHook(final String property, final Class<T> infClass, final T defaultImpl, final String methodName, final ClassLoader classLoader)
  {
    return register(new Hook<T>(property, infClass, defaultImpl, methodName, classLoader));
  }

  /**
   * Register a hook defined via SPI, of which a single instance is discovered and invoked.
   * @param <T> the type of the hook interface.
   * @param infClass the class of the hook's interface.
   * @param defaultImpl the default implementation, which may be null.
   * @param methodName the name of the implementation method to invoke.
   * @param classLoader the class loader used to load the implementation.
   * @return the registered hook as a {@link Hook} instance.
   */
  public static <T> Hook<T> registerSPISingleHook(final Class<T> infClass, final T defaultImpl, final String methodName, final ClassLoader classLoader)
  {
    return register(new Hook<T>(infClass, defaultImpl, methodName, classLoader, true));
  }

  /**
   * Register a hook defined via SPI, for which all looked u^p instances are discovered and invoked.
   * @param <T> the type of the hook interface.
   * @param infClass the class of the hook's interface.
   * @param defaultImpl the default implementation, which may be null.
   * @param methodName the name of the implementation method to invoke.
   * @param classLoader the class loader used to load the implementation.
   * @return the registered hook as a {@link Hook} instance.
   */
  public static <T> Hook<T> registerSPIMultipleHook(final Class<T> infClass, final T defaultImpl, final String methodName, final ClassLoader classLoader)
  {
    return register(new Hook<T>(infClass, defaultImpl, methodName, classLoader, false));
  }

  /**
   * Register the specified hook.
   * @param <T> the type of the hook interface.
   * @param hook the hhook to register.
   * @return the supplied hook.
   */
  private static <T> Hook<T> register(final Hook<T> hook)
  {
    hookMap.put(hook.getInterfaceName(), hook);
    return hook;
  }

  /**
   * Invoke all instances of the hook with the specified interface name, with the specified parameters.
   * @param inf the hook's interface name.
   * @param parameters the parameters with which to invoke the hook.
   * @return the hook instance return value if the hook id defined with a single instance, null otherwise.
   * @throws Exception if any error occurs during the invocation.
   */
  public static Object invokeHook(final String inf, final Object...parameters) throws Exception
  {
    Hook<?> hook = hookMap.get(inf);
    if (hook != null) return hook.invoke(parameters);
    return null;
  }

  /**
   * Invoke all instances of the hook with the specified interface name, with the specified parameters.
   * @param inf the hook's interface.
   * @param parameters the parameters with which to invoke the hook.
   * @return the hook instance return value if the hook id defined with a single instance, null otherwise.
   * @throws Exception if any error occurs during the invocation.
   */
  public static Object invokeHook(final Class<?> inf, final Object...parameters) throws Exception
  {
    return invokeHook(inf.getName(), parameters);
  }
}
