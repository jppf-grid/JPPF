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
import org.jppf.utils.*;
import org.jppf.utils.configuration.JPPFProperty;
import org.slf4j.*;

/**
 * This class provides methods to find, load and access {@link MonitoringDataProvider}s.
 * It is implemented as a singleton.
 * @author Laurent Cohen
 */
public final class MonitoringDataProviderHandler {
  /**
   * Logger for this class.
   */
  private static final Logger log = LoggerFactory.getLogger(MonitoringDataProviderHandler.class);
  /**
   * The providers discovered via SPI.
   */
  private static final List<MonitoringDataProvider> providers = new ArrayList<>();
  /**
   * The list of properties of all providers.
   */
  private static List<JPPFProperty<?>> propertyList;
  /**
   * Whether the providers have already been loaded.
   */
  private static boolean loaded;
  /**
   * Whether the providers have already been initalized.
   */
  private static boolean initalized;
  /**
   * Whether the properties of the providers have already been defined.
   */
  private static boolean defined;

  /**
   * Instantiation is not permitted.
   */
  private MonitoringDataProviderHandler() {
  }

  /**
   * Initialize the providers found via SPI.
   */
  static synchronized void initProviders() {
    if (!initalized) {
      initalized = true;
      for (final MonitoringDataProvider provider: getProviders()) {
        try {
          provider.init();
        } catch (final Exception e) {
          log.error("error initializing provider {}\n{}", provider, ExceptionUtils.getStackTrace(e));
        }
      }
    }
  }

  /**
   * Get a list of all the {@link MonitoringDataProvider}s that were found in the classpath.
   * @return the list of providers discovered via SPI.
   */
  public static synchronized List<MonitoringDataProvider> getProviders() {
    if (!loaded) {
      loaded = true;
      try {
        final List<MonitoringDataProvider> list = new ServiceFinder().findProviders(MonitoringDataProvider.class);
        if (list != null) providers.addAll(list);
      } catch (final Exception e) {
        log.error("error loading providers: {}", ExceptionUtils.getStackTrace(e));
      }
    }
    return providers;
  }

  /**
   * Get a consolidated list of all properties defined by all {@link MonitoringDataProvider}s.
   * @return the list of properties of all providers.
   */
  public static synchronized List<JPPFProperty<?>> getAllProperties() {
    if (!defined) {
      defined = true;
      final List<MonitoringDataProvider> providers = getProviders();
      int size = 0;
      for (final MonitoringDataProvider provider: providers) {
        try {
          provider.defineProperties();
          size += provider.getProperties().size();
        } catch (final Exception e) {
          log.error("error defining properties for provider {}\n{}", provider, ExceptionUtils.getStackTrace(e));
        }
      }
      final List<JPPFProperty<?>> list = new ArrayList<>(size);
      for (final MonitoringDataProvider provider: providers) {
        for (final JPPFProperty<?> property: provider.getProperties()) list.add(property);
      }
      propertyList = Collections.unmodifiableList(list);
    }
    return propertyList;
  }
}
