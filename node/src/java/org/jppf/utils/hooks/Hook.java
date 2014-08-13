/*
 * JPPF.
 * Copyright (C) 2005-2014 JPPF Team.
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
   * The list of instances of this hook.
   */
  private final List<HookInstance<E>> instances = new ArrayList<>();
  /**
   * The type of this hook.
   */
  private final HookType type;

  /**
   * Register a hook defined via a configuration property, of which a single instance is discovered and invoked.
   * @param property the name of the property used to specify the hook implementation class name.
   * @param infClass the class of the hook's interface.
   * @param defaultImpl the default implementation, which may be null.
   * @param loader the class loader used to load the implemntation.
   */
  public Hook(final String property, final Class<E> infClass, final E defaultImpl, final ClassLoader loader) {
    if (infClass == null) throw new IllegalArgumentException("interface class cannot be null");
    this.infName = infClass.getName();
    this.type = HookType.CONFIG_SINGLE_INSTANCE;
    ClassLoader cl = findClassLoader(loader);
    String fqn = JPPFConfiguration.getProperties().getString(property);
    if ((fqn != null) && !"".equals(fqn.trim())) {
      try {
        Class<E> clazz = (Class<E>) Class.forName(fqn, true, cl);
        processConcreteInstance(clazz.newInstance(), false);
        if (debugEnabled) log.debug("added concrete class {} for {}={}", new Object[] {clazz.getName(), property, fqn});
      } catch(Exception e) {
        String format = "failed to instantiate concrete class for {}, {}={}, exception={}";
        Object[] params = new Object[] {this, property, fqn, debugEnabled ? ExceptionUtils.getStackTrace(e) : ExceptionUtils.getMessage(e)};
        if (debugEnabled) log.debug(format, params);
        else log.warn(format, params);
      }
    }
    processConcreteInstance(defaultImpl, true);
  }

  /**
   * Register a hook defined via a configuration property, of which a single instance is discovered and invoked.
   * @param infClass the class of the hook's interface.
   * @param defaultImpl the default implementation, which may be null.
   * @param loader the class loader used to load the implemntation.
   * @param single determines whether only the first looked up implementation should be used, or all the instances found.
   */
  public Hook(final Class<E> infClass, final E defaultImpl, final ClassLoader loader, final boolean single)
  {
    if (infClass == null) throw new IllegalArgumentException("interface class cannot be null");
    this.infName = infClass.getName();
    this.type = single ? HookType.SPI_SINGLE_INSTANCE : HookType.SPI_MULTIPLE_INSTANCES;
    ClassLoader cl = findClassLoader(loader);
    Iterator<E> it = ServiceFinder.lookupProviders(infClass, cl, single);
    while (it.hasNext()) processConcreteInstance(it.next(), false);
    processConcreteInstance(defaultImpl, true);
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
   * Get the type of this hook.
   * @return the type as a typesafe {@link HookType} enum value.
   */
  public HookType getType()
  {
    return type;
  }

  /**
   * Invoke each instance of this hook.
   * @param methodName the name of the implementation method to invoke.
   * @param parameters the parameters supplied to each method invocation.
   * @return this method always return null.
   */
  public Object[] invoke(final String methodName, final Object...parameters)
  {
    List<Object> results = new ArrayList<>();
    switch(type)
    {
      case CONFIG_SINGLE_INSTANCE:
      case SPI_SINGLE_INSTANCE:
        if (!instances.isEmpty()) results.add(instances.get(0).invoke(methodName, parameters));
        break;
      case SPI_MULTIPLE_INSTANCES:
        if (!instances.isEmpty()) for (HookInstance instance: instances) results.add(instance.invoke(methodName, parameters));
        break;
    }
    return results.toArray(new Object[results.size()]);
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

  /**
   * 
   * @param concrete a concrete implementation, which may be null.
   * @param isDefault <code>true</code> if the implementation is the specified default, <code>false</code> otherwise.
   */
  private void processConcreteInstance(final E concrete, final boolean isDefault)
  {
    if ((concrete != null) && (!isDefault || instances.isEmpty())) {
      if (debugEnabled) log.debug("adding concrete instance {}, default={}", concrete, isDefault);
      instances.add(new HookInstance(concrete));
    }
  }

  @Override
  public String toString()
  {
    StringBuilder sb = new StringBuilder();
    sb.append(getClass().getSimpleName()).append('[');
    sb.append("interface=").append(infName);
    sb.append(", type=").append(type);
    sb.append(", instances=").append(instances);
    sb.append(']');
    return sb.toString();
  }

  /**
   * Get the list of instances of this hook.
   * @return a list of {@link HookInstance} objects.
   */
  public List<HookInstance<E>> getInstances()
  {
    return instances;
  }

  /**
   * Cleanup this hook and release its resources.
   */
  public void dispose()
  {
    for (HookInstance<E> instance: instances) instance.dispose();
    instances.clear();
  }
}
