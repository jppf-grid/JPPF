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
package org.jppf.utils;


import java.io.*;
import java.net.*;
import java.util.*;

import org.jppf.utils.configuration.*;

/**
 * Extension of the <code>java.util.Properties</code> class to handle the conversion of string values to other types.
 * @author Laurent Cohen
 */
public class TypedProperties extends AbstractTypedProperties {
  /**
   * Explicit serialVersionUID.
   */
  private static final long serialVersionUID = 1L;

  /**
   * Default constructor.
   */
  public TypedProperties() {
  }

  /**
   * Initialize this object with a set of existing properties.
   * This will copy into the present object all map entries such that both key and value are strings.
   * @param map the properties to be copied. No reference to this parameter is kept in this TypedProperties object.
   */
  public TypedProperties(final Map<?, ?> map) {
    super(map);
  }

  /**
   * Get the string value of a property with a specified name.
   * @param key the name of the property to look for.
   * @return the value of the property as a string, or null if it is not found.
   */
  public String getString(final String key) {
    return getProperty(key, null);
  }

  /**
   * Get the string value of a property with a specified name.
   * @param key the name of the property to look for.
   * @param defValue a default value to return if the property is not found.
   * @return the value of the property as a string, or the default value if it is not found.
   */
  public String getString(final String key, final String defValue) {
    return getProperty(key, defValue);
  }

  /**
   * Set a property with the specified String value.
   * @param key the name of the property to set.
   * @param value the value to set on the property.
   * @return this {@code TypedProperties} object.
   */
  public TypedProperties setString(final String key, final String value) {
    setProperty(key, value);
    return this;
  }

  /**
   * Get the integer value of a property with a specified name.
   * @param key the name of the property to look for.
   * @return the value of the property as an int, or zero if it is not found.
   */
  public int getInt(final String key) {
    return getInt(key, 0);
  }

  /**
   * Get the integer value of a property with a specified name.
   * @param key the name of the property to look for.
   * @param defValue a default value to return if the property is not found.
   * @return the value of the property as an int, or the default value if it is not found.
   */
  public int getInt(final String key, final int defValue) {
    return PropertiesHelper.toInt(getProperty(key, null), defValue);
  }

  /**
   * Set a property with the specified int value.
   * @param key the name of the property to set.
   * @param value the value to set on the property.
   * @return this {@code TypedProperties} object.
   */
  public TypedProperties setInt(final String key, final int value) {
    return setString(key, Integer.toString(value));
  }

  /**
   * Get the long integer value of a property with a specified name.
   * @param key the name of the property to look for.
   * @return the value of the property as a long, or zero if it is not found.
   */
  public long getLong(final String key) {
    return getLong(key, 0L);
  }

  /**
   * Get the long integer value of a property with a specified name.
   * @param key the name of the property to look for.
   * @param defValue a default value to return if the property is not found.
   * @return the value of the property as a long, or the default value if it is not found.
   */
  public long getLong(final String key, final long defValue) {
    return PropertiesHelper.toLong(getProperty(key, null), defValue);
  }

  /**
   * Set a property with the specified long value.
   * @param key the name of the property to set.
   * @param value the value to set on the property.
   * @return this {@code TypedProperties} object.
   */
  public TypedProperties setLong(final String key, final long value) {
    return setString(key, Long.toString(value));
  }

  /**
   * Get the single precision value of a property with a specified name.
   * @param key the name of the property to look for.
   * @return the value of the property as a float, or zero if it is not found.
   */
  public float getFloat(final String key) {
    return getFloat(key, 0f);
  }

  /**
   * Get the single precision value of a property with a specified name.
   * @param key the name of the property to look for.
   * @param defValue a default value to return if the property is not found.
   * @return the value of the property as a float, or the default value if it is not found.
   */
  public float getFloat(final String key, final float defValue) {
    return PropertiesHelper.toFloat(getProperty(key, null), defValue);
  }

  /**
   * Set a property with the specified float value.
   * @param key the name of the property to set.
   * @param value the value to set on the property.
   * @return this {@code TypedProperties} object.
   */
  public TypedProperties setFloat(final String key, final float value) {
    return setString(key, Float.toString(value));
  }

  /**
   * Get the double precision value of a property with a specified name.
   * If the key is not found a default value of 0d is returned.
   * @param key the name of the property to look for.
   * @return the value of the property as a double, or zero if it is not found.
   */
  public double getDouble(final String key) {
    return getDouble(key, 0d);
  }

  /**
   * Get the double precision value of a property with a specified name.
   * @param key the name of the property to look for.
   * @param defValue a default value to return if the property is not found.
   * @return the value of the property as a double, or the default value if it is not found.
   */
  public double getDouble(final String key, final double defValue) {
    return PropertiesHelper.toDouble(getProperty(key, null), defValue);
  }

  /**
   * Set a property with the specified double value.
   * @param key the name of the property to set.
   * @param value the value to set on the property.
   * @return this {@code TypedProperties} object.
   */
  public TypedProperties setDouble(final String key, final double value) {
    return setString(key, Double.toString(value));
  }

  /**
   * Get the boolean value of a property with a specified name.
   * If the key is not found a default value of false is returned.
   * @param key the name of the property to look for.
   * @return the value of the property as a boolean, or <code>false</code> if it is not found.
   */
  public boolean getBoolean(final String key) {
    return getBoolean(key, false);
  }

  /**
   * Get the boolean value of a property with a specified name.
   * @param key the name of the property to look for.
   * @param defValue a default value to return if the property is not found.
   * @return the value of the property as a boolean, or the default value if it is not found.
   */
  public boolean getBoolean(final String key, final boolean defValue) {
    boolean booleanVal = defValue;
    final String val = getProperty(key, null);
    if (val != null) booleanVal = Boolean.valueOf(val.trim()).booleanValue();
    return booleanVal;
  }

  /**
   * Set a property with the specified boolean value.
   * @param key the name of the property to set.
   * @param value the value to set on the property.
   * @return this {@code TypedProperties} object.
   */
  public TypedProperties setBoolean(final String key, final boolean value) {
    return setString(key, Boolean.toString(value));
  }

  /**
   * Get the char value of a property with a specified name.
   * If the key is not found a default value of ' ' is returned.
   * @param key the name of the property to look for.
   * @return the value of the property as a char, or the default value ' ' (space character) if it is not found.
   */
  public char getChar(final String key) {
    return getChar(key, ' ');
  }

  /**
   * Get the char value of a property with a specified name.
   * If the value has more than one character, the first one will be used.
   * @param key the name of the property to look for.
   * @param defValue a default value to return if the property is not found.
   * @return the value of the property as a char, or the default value if it is not found.
   */
  public char getChar(final String key, final char defValue) {
    char charVal = defValue;
    final String val = getProperty(key, null);
    if ((val != null) && (val.length() > 0)) charVal = val.charAt(0);
    return charVal;
  }

  /**
   * Set a property with the specified char value.
   * @param key the name of the property to set.
   * @param value the value to set on the property.
   * @return this {@code TypedProperties} object.
   */
  public TypedProperties setChar(final String key, final char value) {
    return setString(key, Character.toString(value));
  }

  /**
   * Get the value of the specified property as a {@link File}.
   * @param key the name of the property to look up.
   * @return an abstract file path based on the value of the property, or null if the property is not defined.
   */
  public File getFile(final String key) {
    return getFile(key, null);
  }

  /**
   * Get the value of the specified property as a {@link File}.
   * @param key the name of the property to look up.
   * @param defValue the value to return if the property is not found.
   * @return an abstract file path based on the value of the property, or the default value if the property is not defined.
   */
  public File getFile(final String key, final File defValue) {
    final String s = getProperty(key);
    return (s == null) || s.trim().isEmpty() ? defValue : new File(s);
  }

  /**
   * Set the value of the specified property as a {@link File}.
   * @param key the name of the property to look up.
   * @param value the file whose path to set as the property value.
   * @return this {@code TypedProperties} object.
   */
  public TypedProperties setFile(final String key, final File value) {
    if (value != null) setProperty(key, value.getPath());
    return this;
  }

  /**
   * Get the value of the specified property as an array of strings.
   * @param key the name of the property to look up.
   * @param delimiter the delimiter used to split the value into an array of strings. It MUST be a litteral string, i.e. not a regex.
   * @return an abstract file path based on the value of the property, or null if the property is not defined.
   * @since 5.2
   */
  public String[] getStringArray(final String key, final String delimiter) {
    return getStringArray(key, delimiter, null);
  }

  /**
   * Get the value of the specified property as an array of strings.
   * @param key the name of the property to look up.
   * @param delimiter the delimiter used to split the value into an array of strings. It MUST be a litteral string, i.e. not a regex.
   * @param defValue the value to return if the property is not found.
   * @return an abstract file path based on the value of the property, or the default value if the property is not defined.
   * @since 5.2
   */
  public String[] getStringArray(final String key, final String delimiter, final String[] defValue) {
    final String s = getProperty(key);
    if ((s == null) || s.trim().isEmpty()) return defValue;
    final String[] array = StringUtils.parseStringArray(s, delimiter, false);
    return (array == null) ? defValue : array;
  }

  /**
   * Set the value of the specified property as an array of strings.
   * @param key the name of the property to look up.
   * @param delimiter the delimiter that will be inserted between any two adjacent strings. It MUST be a litteral string, i.e. not a regex.
   * @param value an array of strings.
   * @return this {@code TypedProperties} object.
   * @since 5.2
   */
  public TypedProperties setStringArray(final String key, final String delimiter, final String[] value) {
    if (value != null) {
      final StringBuilder sb = new StringBuilder();
      for (int i=0; i<value.length; i++) {
        if (i > 0) sb.append(delimiter);
        if (value[i] != null) sb.append(value[i]);
      }
      setProperty(key, sb.toString());
    }
    return this;
  }

  /**
   * Get the value of a property with the specified name as a set of a properties.
   * @param key the name of the property to look for.
   * Its value is the path to another properties file. Relative paths are evaluated against the current application directory.
   * @return the value of the property as another set of properties, or null if it is not found.
   */
  public TypedProperties getProperties(final String key) {
    return getProperties(key, null);
  }

  /**
   * Get the value of a property with the specified name as a set of properties.
   * @param key the name of the property to look for.
   * Its value is the path to another properties file. Relative paths are evaluated against the current application directory.
   * @param def a default value to return if the property is not found.
   * @return the value of the property as another set of properties, or the default value if it is not found.
   */
  public TypedProperties getProperties(final String key, final TypedProperties def) {
    try (InputStream is = FileUtils.getFileInputStream(getString(key))) {
      final TypedProperties res = new TypedProperties();
      res.load(is);
      return res;
    } catch(@SuppressWarnings("unused") final IOException e) {
      return def;
    }
  }

  /**
   * Get the value of a property with the specified name as an {@link InetAddress}.
   * @param key the name of the property to retrieve.
   * @return the property as an {@link InetAddress} instance, or null if the property is not defined or the host doesn't exist.
   */
  public InetAddress getInetAddress(final String key) {
    return getInetAddress(key, null);
  }

  /**
   * Get the value of a property with the specified name as an {@link InetAddress}.
   * @param key the name of the property to retrieve.
   * @param def the default value to use if the property is not defined.
   * @return the property as an {@link InetAddress} instance, or the specified default value if the property is not defined.
   */
  public InetAddress getInetAddress(final String key, final InetAddress def) {
    final String val = getString(key);
    if (val == null) return def;
    try {
      return InetAddress.getByName(val);
    } catch(@SuppressWarnings("unused") final UnknownHostException e) {
      return def;
    }
  }

  /**
   * Extract the properties that pass the specified filter.
   * @param filter the filter to use, if {@code null} then all properties are retruned.
   * @return a new {@code TypedProperties} object containing only the properties matching the filter.
   */
  public TypedProperties filter(final Filter filter) {
    final TypedProperties result = new TypedProperties();
    for (String key: stringPropertyNames()) {
      final String value = getProperty(key);
      if ((filter == null) || filter.accepts(key, value)) result.put(key, value);
    }
    return result;
  }

  /**
   * A filter for {@code TypedProperties} objects.
   */
  public interface Filter {
    /**
     * Determine whether this filter accepts a property with the specirfied name and value.
     * @param name the name of the property.
     * @param value the value of the property.
     * @return {@code true} if the property is accepted, {@code false} otherwise.
     */
    boolean accepts(String name, String value);
  }

  /**
   * Set the value of a predefined property.
   * @param <T> the type of the property.
   * @param property the property whose value to set.
   * @param value the value to set.
   * @return the value of the property according to its type.
   * @since 5.2
   */
  public <T> TypedProperties set(final JPPFProperty<T> property, final T value) {
    setProperty(property.getName(), property.toString(value));
    return this;
  }

  /**
   * Set the value of a predefined parametrized property.
   * @param <T> the type of the property.
   * @param property the property whose value to set.
   * @param value the value to set.
   * @param parameters the values to replace the property's parameters with.
   * @return the value of the property according to its type.
   * @since 6.0
   */
  public <T> TypedProperties set(final JPPFProperty<T> property, final T value, final String...parameters) {
    setProperty(property.resolveName(parameters), property.toString(value));
    return this;
  }

  /**
   * Get the value of a predefined property.
   * @param <T> the type of the property.
   * @param property the property whose value to retrieve.
   * @return the value of the property according to its type.
   * @since 5.2
   */
  public <T> T get(final JPPFProperty<T> property) {
    if (this.containsKey(property.getName())) return property.valueOf(getProperty(property.getName()));
    final String[] aliases = property.getAliases();
    if ((aliases != null) && (aliases.length > 0)) {
      for (String alias: aliases) {
        if (this.containsKey(alias)) return property.valueOf(getProperty(alias));
      }
    }
    return property.getDefaultValue();
  }

  /**
   * Get the value of a predefined parametrized property.
   * @param <T> the type of the property.
   * @param property the property whose value to retrieve.
   * @param parameters the values to replace the property's parameters with.
   * @return the value of the property according to its type.
   * @since 6.0
   */
  public <T> T get(final JPPFProperty<T> property, final String...parameters) {
    String name = property.resolveName(parameters);
    if (this.containsKey(name)) return property.valueOf(getProperty(name));
    final String[] aliases = property.getAliases();
    if ((aliases != null) && (aliases.length > 0)) {
      for (String alias: aliases) {
        name = property.resolveName(alias, parameters);
        if (this.containsKey(name)) return property.valueOf(getProperty(name));
      }
    }
    return property.getDefaultValue();
  }

  /**
   * Remove the specified predefined property.
   * @param <T> the type of the property.
   * @param property the property whose value to retrieve.
   * @return the old value of the property, or {@code null} if it wasn't defined.
   * @since 5.2
   */
  public <T> T remove(final JPPFProperty<T> property) {
    final Object o = remove(property.getName());
    if (!(o instanceof String)) return null; 
    return property.valueOf((String) o);
  }

  /**
   * Determine whether this set of properties contains the specified property.
   * The lookkup is performed on the property's name first, then on its aliases if the name is not found.
   * @param property the property to look for.
   * @return {@code true} if the property or one of its aliases is found, {@code false} otherwise.
   * @since 5.2
   */
  public boolean containsProperty(final JPPFProperty<?> property) {
    if (getProperty(property.getName()) != null) return true;
    if (property.getAliases() != null) {
      for (String name: property.getAliases()) {
        if (getProperty(name) != null) return true;
      }
    }
    return false;
  }

  /**
   * Remove the specified predefined parametrized property.
   * @param <T> the type of the property.
   * @param property the property whose value to retrieve.
   * @param parameters the values to replace the property's parameters with.
   * @return the old value of the property, or {@code null} if it wasn't defined.
   * @since 6.0
   */
  public <T> T remove(final JPPFProperty<T> property, final String...parameters) {
    final Object o = remove(property.resolveName(parameters));
    if (!(o instanceof String)) return null; 
    return property.valueOf((String) o);
  }

  @Override
  public void store(final Writer writer, final String comments) throws IOException {
    PropertiesHelper.store(this, writer);
  }
}
