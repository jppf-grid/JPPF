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

import java.lang.reflect.Method;
import java.util.*;

import org.jppf.utils.*;
import org.slf4j.*;

/**
 * 
 * @param <E>
 * @author Laurent Cohen
 */
public class Hook<E> {
  /**
   * Logger for this class.
   */
  private static Logger log = LoggerFactory.getLogger(Hook.class);
  /**
   * Determines whether debug-level logging is enabled.
   */
  private static boolean debugEnabled = log.isDebugEnabled();
  /**
   * The hook's interface.
   */
  private final String infName;
  /**
   * The name of the method to invoke on each hook instance.
   */
  private final String methodName;
  /**
   * The list of instances of this hook.
   */
  private final List<HookInstance<E>> instances = new ArrayList<HookInstance<E>>();
  /**
   * The type of this hook.
   */
  private final HookType type;

  /**
   * Register a hook defined via a configuration property, of which a single instance is discovered and invoked.
   * @param property the name of the property used to specify the hook implementation class name.
   * @param infClass the class of the hook's interface.
   * @param defaultImpl the default implementation, which may be null.
   * @param methodName the name of the implementation method to invoke.
   * @param loader the class loader used to load the implemntation.
   */
  public Hook(final String property, final Class<E> infClass, final E defaultImpl, final String methodName, final ClassLoader loader) {
    if (infClass == null) throw new IllegalArgumentException("interface class cannot be null");
    if (methodName == null) throw new IllegalArgumentException("method name cannot be null");
    this.infName = infClass.getName();
    this.methodName = methodName;
    this.type = HookType.CONFIG_SINGLE_INSTANCE;
    ClassLoader cl = findClassLoader(loader);
    String fqn = JPPFConfiguration.getProperties().getString(property);
    HookInstance instance = null;
    if (fqn != null) {
      try {
        Class<E> clazz = (Class<E>) Class.forName(fqn, true, cl);
        E concrete = clazz.newInstance();
        Method method = ReflectionHelper.findMethodAnyArgs(clazz, methodName);
        instance = new HookInstance(method, concrete);
      } catch(Exception e) {
        String format = "failed to initialize hook instance for {}, methodName={], type={}, exception={}";
        if (debugEnabled) log.debug(format, new Object[] {infClass, methodName, type, ExceptionUtils.getStackTrace(e)});
        else log.warn(format, new Object[] {infClass, methodName, type, ExceptionUtils.getMessage(e)});
      }
    }
    if ((instance == null) && (defaultImpl != null)) {
      try {
        Class<?> clazz = defaultImpl.getClass();
        Method method = ReflectionHelper.findMethodAnyArgs(clazz, methodName);
        instance = new HookInstance(method, defaultImpl);
      } catch(Exception e) {
        String format = "failed to initialize default hook instance for {}, methodName={], type={}, exception={}";
        if (debugEnabled) log.debug(format, new Object[] {infClass, methodName, type, ExceptionUtils.getStackTrace(e)});
        else log.warn(format, new Object[] {infClass, methodName, type, ExceptionUtils.getMessage(e)});
      }
    }
    if (instance != null) instances.add(instance);
  }

  /**
   * Register a hook defined via a configuration property, of which a single instance is discovered and invoked.
   * @param infClass the class of the hook's interface.
   * @param defaultImpl the default implementation, which may be null.
   * @param methodName the name of the implementation method to invoke.
   * @param loader the class loader used to load the implemntation.
   * @param single determines whether only the first looked up implementation should be used, or all the instances found.
   */
  public Hook(final Class<E> infClass, final E defaultImpl, final String methodName, final ClassLoader loader, final boolean single)
  {
    if (infClass == null) throw new IllegalArgumentException("interface class cannot be null");
    if (methodName == null) throw new IllegalArgumentException("method name cannot be null");
    this.infName = infClass.getName();
    this.methodName = methodName;
    this.type = single ? HookType.SPI_SINGLE_INSTANCE : HookType.SPI_MULTIPLE_INSTANCES;
    ClassLoader cl = findClassLoader(loader);
    Iterator<E> it = ServiceFinder.lookupProviders(infClass, cl, single);
    while (it.hasNext()) {
      E concrete = it.next();
      Class<?> clazz = concrete.getClass();
      try {
        Method method = ReflectionHelper.findMethodAnyArgs(clazz, methodName);
        instances.add(new HookInstance(method, concrete));
      } catch(Exception e) {
        String format = "failed to initialize hook instance for {}, concrete class={}, methodName={}, type={}, exception={}";
        if (debugEnabled) log.debug(format, new Object[] {infClass, clazz, methodName, type, ExceptionUtils.getStackTrace(e)});
        else log.warn(format, new Object[] {infClass, clazz, methodName, type, ExceptionUtils.getMessage(e)});
      }
    }
    if (instances.isEmpty() && (defaultImpl != null)) {
      try {
        Class<?> clazz = defaultImpl.getClass();
        Method method = ReflectionHelper.findMethodAnyArgs(clazz, methodName);
        instances.add(new HookInstance(method, defaultImpl));
      } catch(Exception e) {
        String format = "failed to initialize default hook instance for {}, methodName={], type={}, exception={}";
        if (debugEnabled) log.debug(format, new Object[] {infClass, methodName, type, ExceptionUtils.getStackTrace(e)});
        else log.warn(format, new Object[] {infClass, methodName, type, ExceptionUtils.getMessage(e)});
      }
    }
  }

  /**
   * Get the hook interface.
   * @return the class object for the hook's interface.
   */
  public String getInterfaceName()
  {
    return infName;
  }

  /**
   * Get the name of the method to invoke for each hook with this definition.
   * @return the name of the method as a string.
   */
  public String getMethodName()
  {
    return methodName;
  }

  /**
   * Get the type of this hook.
   * @return the type as a typesafe {@link HookType} enum value.
   */
  public HookType getType()
  {
    return type;
  }

  /**
   * Invoke each instance of this hook.
   * @param parameters the parameters supplied to each method invocation.
   * @return this method always return null.
   * @throws Exception if any error occcurs.
   */
  public Object invoke(final Object...parameters) throws Exception
  {
    switch(type)
    {
      case CONFIG_SINGLE_INSTANCE:
      case SPI_SINGLE_INSTANCE:
        if (!instances.isEmpty()) return instances.get(0).invoke(parameters);
        break;
      case SPI_MULTIPLE_INSTANCES:
        if (!instances.isEmpty()) for (HookInstance instance: instances) instance.invoke(parameters);
        break;
    }
    return null;
  }

  /**
   * Lookup a non-null class loader if the supplied one is null.
   * @param loader the class loader to check.
   * @return the supplied class loader if not null, or another one if it is.
   */
  private ClassLoader findClassLoader(final ClassLoader loader)
  {
    ClassLoader cl = loader;
    if (cl == null) cl = Thread.currentThread().getContextClassLoader();
    if (cl == null) cl = getClass().getClassLoader();
    return cl;
  }
}
