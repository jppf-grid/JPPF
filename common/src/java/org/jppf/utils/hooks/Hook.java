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

package org.jppf.utils.hooks;

import java.lang.reflect.Constructor;
import java.util.*;

import org.jppf.utils.*;
import org.jppf.utils.configuration.JPPFProperty;
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
  private static boolean debugEnabled = LoggingUtils.isDebugEnabled(log);
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
  public Hook(final JPPFProperty<String> property, final Class<E> infClass, final E defaultImpl, final ClassLoader loader) {
    if (infClass == null) throw new IllegalArgumentException("interface class cannot be null");
    this.infName = infClass.getName();
    this.type = HookType.CONFIG_SINGLE_INSTANCE;
    final ClassLoader cl = findClassLoader(loader);
    final String fqn = JPPFConfiguration.get(property);
    if ((fqn != null) && !"".equals(fqn.trim())) {
      try {
        @SuppressWarnings("unchecked")
        final Class<E> clazz = (Class<E>) Class.forName(fqn, true, cl);
        processConcreteInstance(clazz.newInstance(), false);
      } catch (final Exception e) {
        final String format = "failed to instantiate concrete class for {}, {}={}, exception={}";
        final Object[] params = new Object[] { this, property, fqn, debugEnabled ? ExceptionUtils.getStackTrace(e) : ExceptionUtils.getMessage(e) };
        if (debugEnabled) log.debug(format, params);
        else log.warn(format, params);
      }
    }
    processConcreteInstance(defaultImpl, true);
  }

  /**
   * Register a hook defined via a configuration property, of which a single instance is discovered and invoked.
   * @param property the name of the property used to specify the hook implementation class name.
   * @param infClass the class of the hook's interface.
   * @param defaultImpl the default implementation, which may be null.
   * @param loader the class loader used to load the implemntation.
   * @param paramTypes types of the parameters to pass to the constructor.
   * @param params the parameters to pass to the constructor.
   */
  @SuppressWarnings("unchecked")
  public Hook(final JPPFProperty<String> property, final Class<E> infClass, final E defaultImpl, final ClassLoader loader, final Class<?>[] paramTypes, final Object...params) {
    if (infClass == null) throw new IllegalArgumentException("interface class cannot be null");
    this.infName = infClass.getName();
    this.type = HookType.CONFIG_SINGLE_INSTANCE;
    final ClassLoader cl = findClassLoader(loader);
    final String fqn = JPPFConfiguration.get(property);
    if ((fqn != null) && !"".equals(fqn.trim())) {
      try {
        final Class<E> clazz = (Class<E>) Class.forName(fqn, true, cl);
        final Constructor<E> c = (Constructor<E>) ReflectionHelper.findConstructor(clazz, paramTypes);
        processConcreteInstance((c == null) ? clazz.newInstance() : c.newInstance(params), false);
      } catch (final Exception e) {
        log.warn("failed to instantiate concrete class for {}, {}={}, exception=\n{}", this, property, fqn, ExceptionUtils.getStackTrace(e));
      }
    }
    processConcreteInstance(defaultImpl, true);
  }

  /**
   * Register a hook defined via SPI.
   * @param infClass the class of the hook's interface.
   * @param defaultImpl the default implementation, which may be {@code null}.
   * @param loader the class loader used to load the implemntation.
   * @param single determines whether only the first looked up implementation should be used, or all the instances found.
   */
  public Hook(final Class<E> infClass, final E defaultImpl, final ClassLoader loader, final boolean single) {
    if (infClass == null) throw new IllegalArgumentException("interface class cannot be null");
    this.infName = infClass.getName();
    this.type = single ? HookType.SPI_SINGLE_INSTANCE : HookType.SPI_MULTIPLE_INSTANCES;
    final ClassLoader cl = findClassLoader(loader);
    final Iterator<E> it = ServiceFinder.lookupProviders(infClass, cl, single);
    while (it.hasNext()) processConcreteInstance(it.next(), false);
    processConcreteInstance(defaultImpl, true);
  }

  /**
   * Register a hook defined via SPI.
   * @param infClass the class of the hook's interface.
   * @param defaultImpl the default implementation, which may be {@code null}.
   * @param loader the class loader used to load the implemntation.
   * @param single determines whether only the first looked up implementation should be used, or all the instances found.
   * @param paramTypes types of the parameters to pass to the constructor.
   * @param params the parameters to pass to the constructor.
   */
  @SuppressWarnings("unchecked")
  public Hook(final Class<E> infClass, final E defaultImpl, final ClassLoader loader, final boolean single, final Class<?>[] paramTypes, final Object...params) {
    if (infClass == null) throw new IllegalArgumentException("interface class cannot be null");
    this.infName = infClass.getName();
    this.type = single ? HookType.SPI_SINGLE_INSTANCE : HookType.SPI_MULTIPLE_INSTANCES;
    final ClassLoader cl = findClassLoader(loader);
    final List<String> implClassNames = new ServiceFinder().findServiceDefinitions("META-INF/services/" + infClass.getName(), cl);
    for (int i=0; i<implClassNames.size(); i++) {
      if (single && i > 0) break;
      final String name = implClassNames.get(i);
      try {
        final Class<E> clazz = (Class<E>) Class.forName(name, true, cl);
        final Constructor<E> c = (Constructor<E>) ReflectionHelper.findConstructor(clazz, paramTypes);
        processConcreteInstance((c == null) ? clazz.newInstance() : c.newInstance(params), false);
      } catch(final Exception e) {
        log.warn("error instantiating class '{}' implementing interface '{}', with single={}\nparamTypes={}\nparams={}\n{}",
          name, infClass.getName(), single, Arrays.toString(paramTypes), Arrays.toString(params), ExceptionUtils.getStackTrace(e));
      }
    }
    processConcreteInstance(defaultImpl, true);
  }

  /**
   * Get the hook interface.
   * @return the class object for the hook's interface.
   */
  public String getInterfaceName() {
    return infName;
  }

  /**
   * Get the type of this hook.
   * @return the type as a typesafe {@link HookType} enum value.
   */
  public HookType getType() {
    return type;
  }

  /**
   * Invoke each instance of this hook.
   * @param methodName the name of the implementation method to invoke.
   * @param parameters the parameters supplied to each method invocation.
   * @return this method always return null.
   */
  public Object[] invoke(final String methodName, final Object... parameters) {
    final List<Object> results = new ArrayList<>();
    switch (type) {
      case CONFIG_SINGLE_INSTANCE:
      case SPI_SINGLE_INSTANCE:
        if (!instances.isEmpty()) results.add(instances.get(0).invoke(methodName, parameters));
        break;
      case SPI_MULTIPLE_INSTANCES:
        if (!instances.isEmpty()) {
          for (final HookInstance<?> instance : instances) results.add(instance.invoke(methodName, parameters));
        }
        break;
    }
    return results.toArray(new Object[results.size()]);
  }

  /**
   * Lookup a non-null class loader if the supplied one is null.
   * @param loader the class loader to check.
   * @return the supplied class loader if not null, or another one if it is.
   */
  private ClassLoader findClassLoader(final ClassLoader loader) {
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
  private void processConcreteInstance(final E concrete, final boolean isDefault) {
    if ((concrete != null) && (!isDefault || instances.isEmpty())) {
      instances.add(new HookInstance<>(concrete));
    }
  }

  @Override
  public String toString() {
    final StringBuilder sb = new StringBuilder();
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
  public List<HookInstance<E>> getInstances() {
    return instances;
  }

  /**
   * Cleanup this hook and release its resources.
   */
  public void dispose() {
    for (HookInstance<E> instance : instances)
      instance.dispose();
    instances.clear();
  }
}
