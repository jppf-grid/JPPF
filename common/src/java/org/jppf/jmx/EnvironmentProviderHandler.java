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

package org.jppf.jmx;

import java.util.*;

/**
 * Instances of this class load and provide access to environment proivders.
 * @param <T> the type of the providers to load.
 * @author Laurent Cohen
 */
public class EnvironmentProviderHandler<T> {
  /**
   * The client providers.
   */
  private final List<T> providers;

  /**
   * Initialize this handler witht he specified provider class.
   * @param clazz the class of the providers to laod.
   */
  public EnvironmentProviderHandler(final Class<T> clazz) {
    providers = Collections.unmodifiableList(loadProviders(clazz));
  }

  /**
   * Load the providers of specified type via SPI.
   * @param clazz the class of the providers to load.
   * @return a list of the loaded providers, possibly empty.
   */
  private List<T> loadProviders(final Class<T> clazz) {
    final List<T> list = new ArrayList<>();
    final ServiceLoader<T> sl = ServiceLoader.load(clazz);
    final Iterator<T> it = sl.iterator();
    boolean end = false;
    while (!end) {
      // hasNext() and next() may throw a ServiceCOnfigurationError
      try {
        if (it.hasNext()) list.add(it.next());
        else end = true;
      } catch(@SuppressWarnings("unused") final Exception|ServiceConfigurationError e) {
        end = true;
      }
    }
    return list;
  }

  /**
   * Get the client providers.
   * @return a list of privder instances.
   */
  public List<T> getProviders() {
    return providers;
  }
}
