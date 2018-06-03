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

package org.jppf.management.diagnostics.provider;

import java.util.*;

import org.jppf.utils.TypedProperties;
import org.jppf.utils.configuration.*;

/**
 * Base class for pluggable providers of monitoring data.
 * Concrete implementations must have a no-args constructor.
 * @author Laurent Cohen
 */
public abstract class MonitoringDataProvider {
  /**
   * Mapping of the properties to their names.
   */
  private final Map<String, JPPFProperty<?>> properties = new LinkedHashMap<>();

  /**
   * Perform the definition of the properties supplied by this provider.
   * This method is called extactly once in both producer (where the data comes from) and consumer (where the data is rendered, e.g. admin console) sides.
   */
  public abstract void defineProperties();

  /**
   * Perform the initialization of this provider.
   * This method is called exactly once on the producer (where the data comes from) side only.
   */
  public abstract void init();

  /**
   * Get the values for the defined properties.
   * <p>This method is expected to be called repeatedly on the producer side (where the data comes from).
   * @return the values as a {@link TypedProperties} object, mapping the properties names to their value as a {@code String}.
   */
  public abstract TypedProperties getValues();

  /**
   * Get the base path to the localization bundles for this provider.
   * For exemple, if the base path is {@code mypackage.MyProps}, then the packahe {@code mypackage} should at least contain the file {@code MyProps.properties},
   * along with other localized bundles such as {@code MyProps_fr.properties}, {@code MyProps_en_GB.properties}, etc.
   * <p>This default implementation returns {@code null}. It should be overriden to provide a different value.
   * @return the base path to the localization bundles for this provider.
   * @see java.util.ResourceBundle
   * @see java.util.PropertyResourceBundle
   */
  protected String getLocalizationBase() {
    return null;
  }

  /**
   * Add an integer property. If a property with the same name already exists, it is replaced.
   * @param name the name of the property.
   * @param defaultValue the default value.
   * @return this provider, for method call chaining.
   */
  public MonitoringDataProvider setIntProperty(final String name, final int defaultValue) {
    return setProperty(new IntProperty(name, defaultValue));
  }

  /**
   * Add an integer property. If a property with the same name already exists, it is replaced.
   * @param name the name of the property.
   * @param defaultValue the default value.
   * @param minValue the minimum accepted value for this property.
   * @param maxValue the maximum accepted value for this property.
   * @return this provider, for method call chaining.
   */
  public MonitoringDataProvider setIntProperty(final String name, final int defaultValue, final int minValue, final int maxValue) {
    return setProperty(new IntProperty(name, defaultValue, minValue, maxValue));
  }

  /**
   * Add a long property. If a property with the same name already exists, it is replaced.
   * @param name the name of the property.
   * @param defaultValue the default value.
   * @return this provider, for method call chaining.
   */
  public MonitoringDataProvider setLongProperty(final String name, final long defaultValue) {
    return setProperty(new LongProperty(name, defaultValue));
  }

  /**
   * Add a long property. If a property with the same name already exists, it is replaced.
   * @param name the name of the property.
   * @param defaultValue the default value.
   * @param minValue the minimum accepted value for this property.
   * @param maxValue the maximum accepted value for this property.
   * @return this provider, for method call chaining.
   */
  public MonitoringDataProvider setLongProperty(final String name, final long defaultValue, final long minValue, final long maxValue) {
    return setProperty(new LongProperty(name, defaultValue, minValue, maxValue));
  }

  /**
   * Add a float property. If a property with the same name already exists, it is replaced.
   * @param name the name of the property.
   * @param defaultValue the default value.
   * @return this provider, for method call chaining.
   */
  public MonitoringDataProvider setFloatProperty(final String name, final float defaultValue) {
    return setProperty(new FloatProperty(name, defaultValue));
  }

  /**
   * Add a float property. If a property with the same name already exists, it is replaced.
   * @param name the name of the property.
   * @param defaultValue the default value.
   * @param minValue the minimum accepted value for this property.
   * @param maxValue the maximum accepted value for this property.
   * @return this provider, for method call chaining.
   */
  public MonitoringDataProvider setFloatProperty(final String name, final float defaultValue, final float minValue, final float maxValue) {
    return setProperty(new FloatProperty(name, defaultValue, minValue, maxValue));
  }

  /**
   * Add a double property. If a property with the same name already exists, it is replaced.
   * @param name the name of the property.
   * @param defaultValue the default value.
   * @return this provider, for method call chaining.
   */
  public MonitoringDataProvider setDoubleProperty(final String name, final double defaultValue) {
    return setProperty(new DoubleProperty(name, defaultValue));
  }

  /**
   * Add a double property. If a property with the same name already exists, it is replaced.
   * @param name the name of the property.
   * @param defaultValue the default value.
   * @param minValue the minimum accepted value for this property.
   * @param maxValue the maximum accepted value for this property.
   * @return this provider, for method call chaining.
   */
  public MonitoringDataProvider setDoubleProperty(final String name, final double defaultValue, final double minValue, final double maxValue) {
    return setProperty(new DoubleProperty(name, defaultValue, minValue, maxValue));
  }

  /**
   * Add a boolean property. If a property with the same name already exists, it is replaced.
   * @param name the name of the property.
   * @param defaultValue the default value.
   * @return this provider, for method call chaining.
   */
  public MonitoringDataProvider setBooleanProperty(final String name, final boolean defaultValue) {
    return setProperty(new BooleanProperty(name, defaultValue));
  }

  /**
   * Add a String property. If a property with the same name already exists, it is replaced.
   * @param name the name of the property.
   * @param defaultValue the default value.
   * @return this provider, for method call chaining.
   */
  public MonitoringDataProvider setStringProperty(final String name, final String defaultValue) {
    return setProperty(new StringProperty(name, defaultValue));
  }

  /**
   * Get the property with the specified name.
   * @param name the name of the property to lookup.
   * @return a {@link JPPFProperty} instance, or {@code null} if there is no property with the specified name.
   */
  public JPPFProperty<?> getProperty(final String name) {
    return properties.get(name);
  }

  /**
   * Remove the specified property. If a property with the same name already exists, it is replaced.
   * @param name the name of the property to remove.
   * @return this provider, for method call chaining.
   */
  public MonitoringDataProvider removeProperty(final String name) {
    properties.remove(name);
    return this;
  }

  /**
   * Remove the specified property.
   * @param property the property to remove.
   * @return this provider, for method call chaining.
   */
  public MonitoringDataProvider removeProperty(final JPPFProperty<?> property) {
    return removeProperty(property.getName());
  }

  /**
   * Get the properties defined for this provider.
   * @return a collection of the defined properties.
   */
  public Collection<JPPFProperty<?>> getProperties() {
    return properties.values();
  }

  /**
   * Set or replace the specified property with the specified name.
   * @param prop the property to add.
   * @return this provider, for method call chaining.
   */
  private MonitoringDataProvider setProperty(final AbstractJPPFProperty<?> prop) {
    prop.setI18nBase(getLocalizationBase());
    properties.put(prop.getName(), prop);
    return this;
  }
}
