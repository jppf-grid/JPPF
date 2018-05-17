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

package org.jppf.management.diagnostics;

import java.util.*;

import org.jppf.management.diagnostics.provider.MonitoringDataProvider;
import org.jppf.utils.ServiceFinder;
import org.jppf.utils.configuration.JPPFProperty;

/**
 * 
 * @author Laurent Cohen
 * @exclude
 */
public class MonitoringDataProviderHandler {
  /**
   * The providers discovered via SPI.
   */
  private final List<MonitoringDataProvider> providers = new ArrayList<>();
  /**
   * The list of properties of all providers.
   */
  private Map<String, JPPFProperty<?>> properties;
  /**
   * The list of properties of all providers.
   */
  private List<JPPFProperty<?>> propertyList;

  /**
   * Load the providers found via SPI.
   * @return the list of providers discovered via SPI.
   */
  public List<MonitoringDataProvider> loadProviders() {
    final List<MonitoringDataProvider> list = new ServiceFinder().findProviders(MonitoringDataProvider.class);
    if (list != null) providers.addAll(list);
    return providers;
  }

  /**
   * Initialize the providers found via SPI.
   */
  public void initProviders() {
    for (final MonitoringDataProvider provider: providers) provider.init();
  }

  /**
   * @return the list of providers discovered via SPI.
   */
  public List<MonitoringDataProvider> getProviders() {
    return providers;
  }

  /**
   * @return the list of properties of all providers.
   */
  public List<JPPFProperty<?>> defineProperties() {
    int size = 0;
    for (final MonitoringDataProvider provider: providers) {
      provider.defineProperties();
      size += provider.getProperties().size();
    }
    properties = new LinkedHashMap<>(size);
    for (final MonitoringDataProvider provider: providers) {
      for (final JPPFProperty<?> property: provider.getProperties()) properties.put(property.getName(), property);
    }
    propertyList = Collections.unmodifiableList(new ArrayList<>(properties.values()));
    return propertyList;
  }

  /**
   * @return the list of properties of all providers.
   */
  public List<JPPFProperty<?>> getPropertyList() {
    return propertyList;
  }

  /**
   * @return the map list of properties of all providers.
   */
  public Map<String, JPPFProperty<?>> getPropertyMap() {
    return properties;
  }
}
