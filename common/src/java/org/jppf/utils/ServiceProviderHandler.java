/*
 * JPPF.
 * Copyright (C) 2005-2018 JPPF Team.
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

package org.jppf.utils;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * A utility class that handles a SPI mechanism. It loads all implementations of a service provider interface found in the classpath and provides access to them.
 * @param <T> the type of the SPI interface.
 * @author Laurent Cohen
 * @exclude
 */
public class ServiceProviderHandler<T> {
  /**
   * The interface to handle.
   */
  protected final Class<T> inf;
  /**
   * The list of provider instances.
   */
  protected final List<T> providers = new CopyOnWriteArrayList<>();
  /**
   * The class loader used to lookup the service implementations.
   */
  protected final ClassLoader classLoader;

  /**
   * 
   * @param inf the service interface to handle.
   */
  public ServiceProviderHandler(final Class<T> inf) {
    this(inf, Thread.currentThread().getContextClassLoader());
  }

  /**
   * 
   * @param inf the service interface to handle.
   * @param classLoader the class loader used to lookup the service implementations.
   */
  public ServiceProviderHandler(final Class<T> inf, final ClassLoader classLoader) {
    this.inf = inf;
    this.classLoader = classLoader;
  }

  /**
   * Add a providers to the list of providers.
   * @param provider the provider to add.
   */
  public void addProvider(final T provider) {
    if (provider == null) return;
    providers.add(provider);
  }

  /**
   * Remove a listener from the list of providers.
   * @param provider the provider to remove.
   */
  public void removeProvider(final T provider) {
    if (provider == null) return;
    providers.remove(provider);
  }

  /**
   * Remove all providers from the list of providers.
   */
  public void removeAllProviders() {
    providers.clear();
  }

  /**
   * Get all the rpoviders loaded by htis handler.
   * @return a List of provider implementations.
   */
  public List<T> getProviders() {
    return providers;
  }

  /**
   * Load all provider instances found in the class path via a service definition.
   */
  public void loadProviders() {
    for (final T provider: new ServiceFinder().findProviders(inf, classLoader)) addProvider(provider);
  }
}
