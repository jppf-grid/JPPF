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
package org.jppf.utils;


import java.io.*;
import java.net.*;
import java.util.*;

import org.jppf.utils.streams.StreamUtils;

/**
 * Extension of the <code>java.util.Properties</code> class to handle the conversion of
 * string values to other types.
 * @author Laurent Cohen
 */
public class TypedProperties extends Properties {
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
  public TypedProperties(final Map<Object, Object> map) {
    if (map != null) {
      for (Map.Entry<Object, Object> entry: map.entrySet()) {
        if ((entry.getKey() instanceof String) && (entry.getValue() instanceof String)) {
          setProperty((String) entry.getKey(), (String) entry.getValue());
        }
      }
    }
  }

  /**
   * Get the string value of a property with a specified name.
   * @param key the name of the property to look for.
   * @return the value of the property as a string, or null if it is not found.
   */
  public String getString(final String key) {
    return getString(key, null);
  }

  /**
   * Get the string value of a property with a specified name.
   * @param key the name of the property to look for.
   * @param defValue a default value to return if the property is not found.
   * @return the value of the property as a string, or the default value if it is not found.
   */
  public String getString(final String key, final String defValue) {
    String val = getProperty(key);
    return (val == null) ? defValue : val;
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
    int intVal = defValue;
    String val = getProperty(key, null);
    if (val != null) {
      try {
        intVal = Integer.parseInt(val.trim());
      } catch(NumberFormatException e) {
      }
    }
    return intVal;
  }

  /**
   * Get the integer value of a property with a specified name.
   * @param key the name of the property to look for.
   * @param defValue a default value to return if the property is not found.
   * @param min the minimum value the property can have.
   * @param max the maximum value the property can have.
   * @return the value of the property as a float, or the default value if it is not found, adjusted to ensure it is in the [min, max] range.
   */
  public int getInt(final String key, final int defValue, final int min, final int max) {
    if (min > max) throw new IllegalArgumentException("min (" + min + ") must be <= max (" + max + ')');
    int val = getInt(key, defValue);
    if (val > max) val = max;
    if (val < min) val = min;
    return val;
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
    long longVal = defValue;
    String val = getProperty(key, null);
    if (val != null) {
      try {
        longVal = Long.parseLong(val.trim());
      } catch(NumberFormatException e) {
      }
    }
    return longVal;
  }

  /**
   * Get the long value of a property with a specified name.
   * @param key the name of the property to look for.
   * @param defValue a default value to return if the property is not found.
   * @param min the minimum value the property can have.
   * @param max the maximum value the property can have.
   * @return the value of the property as a float, or the default value if it is not found, adjusted to ensure it is in the [min, max] range.
   */
  public long getLong(final String key, final long defValue, final long min, final long max) {
    if (min > max) throw new IllegalArgumentException("min (" + min + ") must be <= max (" + max + ')');
    long val = getLong(key, defValue);
    if (val > max) val = max;
    if (val < min) val = min;
    return val;
  }

  /**
   * Get the single precision value of a property with a specified name.
   * @param key the name of the property to look for.
   * @return the value of the property as a float, or zero if it is not found.
   */
  public float getFloat(final String key) {
    return getFloat(key, 0.0f);
  }

  /**
   * Get the single precision value of a property with a specified name.
   * @param key the name of the property to look for.
   * @param defValue a default value to return if the property is not found.
   * @return the value of the property as a float, or the default value if it is not found.
   */
  public float getFloat(final String key, final float defValue) {
    float floatVal = defValue;
    String val = getProperty(key, null);
    if (val != null) {
      try {
        floatVal = Float.parseFloat(val.trim());
      } catch(NumberFormatException e) {
      }
    }
    return floatVal;
  }

  /**
   * Get the single precision value of a property with a specified name.
   * @param key the name of the property to look for.
   * @param defValue a default value to return if the property is not found.
   * @param min the minimum value the property can have.
   * @param max the maximum value the property can have.
   * @return the value of the property as a float, or the default value if it is not found, adjusted to ensure it is in the [min, max] range.
   */
  public float getFloat(final String key, final float defValue, final float min, final float max) {
    if (min > max) throw new IllegalArgumentException("min (" + min + ") must be <= max (" + max + ')');
    float val = getFloat(key, defValue);
    if (val > max) val = max;
    if (val < min) val = min;
    return val;
  }

  /**
   * Get the double precision value of a property with a specified name.
   * If the key is not found a default value of 0d is returned.
   * @param key the name of the property to look for.
   * @return the value of the property as a double, or zero if it is not found.
   */
  public double getDouble(final String key) {
    return getDouble(key, 0.0d);
  }

  /**
   * Get the double precision value of a property with a specified name.
   * @param key the name of the property to look for.
   * @param defValue a default value to return if the property is not found.
   * @return the value of the property as a double, or the default value if it is not found.
   */
  public double getDouble(final String key, final double defValue) {
    double doubleVal = defValue;
    String val = getProperty(key, null);
    if (val != null) {
      try {
        doubleVal = Double.parseDouble(val.trim());
      } catch(NumberFormatException e) {
      }
    }
    return doubleVal;
  }

  /**
   * Get the double precision value of a property with a specified name.
   * @param key the name of the property to look for.
   * @param defValue a default value to return if the property is not found.
   * @param min the minimum value the property can have.
   * @param max the maximum value the property can have.
   * @return the value of the property as a double, or the default value if it is not found, adjusted to ensure it is in the [min, max] range.
   */
  public double getDouble(final String key, final double defValue, final double min, final double max) {
    if (min > max) throw new IllegalArgumentException("min (" + min + ") must be <= max (" + max + ')');
    double val = getDouble(key, defValue);
    if (val > max) val = max;
    if (val < min) val = min;
    return val;
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
    String val = getProperty(key, null);
    if (val != null) booleanVal = Boolean.valueOf(val.trim()).booleanValue();
    return booleanVal;
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
    String val = getProperty(key, null);
    if ((val != null) && (val.length() > 0)) charVal = val.charAt(0);
    return charVal;
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
    String path = getString(key);
    TypedProperties res = new TypedProperties();
    InputStream is = null;
    try {
      is = FileUtils.getFileInputStream(path);
      res.load(is);
    } catch(IOException e) {
      return def;
    } finally {
      StreamUtils.closeSilent(is);
    }
    return res;
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
    String val = getString(key);
    if (val == null) return def;
    try {
      return InetAddress.getByName(val);
    } catch(UnknownHostException e) {
      return def;
    }
  }

  /**
   * Convert this set of properties into a string.
   * @return a representation of this object as a string.
   */
  public String asString() {
    StringBuilder sb = new StringBuilder();
    Set<Map.Entry<Object, Object>> entries = entrySet();
    for (Map.Entry<Object, Object> entry: entries) {
      if ((entry.getKey() instanceof String) && (entry.getValue() instanceof String)) {
        sb.append(entry.getKey()).append(" = ").append(entry.getValue()).append('\n');
      }
    }
    return sb.toString();
  }

  /**
   * Load this set of properties from a string.
   * @param source the string to load from.
   * @return this object.
   * @throws IOException if any error occurs.
   */
  public TypedProperties loadString(final CharSequence source) throws IOException {
    if (source != null) {
      ByteArrayInputStream bais = new ByteArrayInputStream(source.toString().getBytes());
      load(bais);
      StreamUtils.closeSilent(bais);
    }
    return this;
  }

  /**
   * Get an int value from the first specified property if it exists, or from the specified second one if it doesn't.
   * If the first property does not exist, it will be created with the value of the second. If <code>doRemove</code> is true,
   * then the second property will be removed. This method is essentially used for backward compatibility with previous versions
   * of JPPF configuration files.
   * @param name1 the name of the first property.
   * @param name2 the name of the second property.
   * @param def the default value to use if neither the first nor the second properties are defined.
   * @param doRemove <code>true</code> to force the remove of the second property, <code>false</code> otherwise.
   * @return the value of the first property, or the value of the second if it is not found, or the default value.
   */
  public int getAndReplaceInt(final String name1, final String name2, final int def, final boolean doRemove) {
    int value = getInt(name1, -1);
    if (value < 0) {
      value = getInt(name2, def);
      setProperty(name1, "" + value);
    }
    if (doRemove) remove(name2);
    return value;
  }

  /**
   * Get a String value from the first specified property if it exists, or from the specified second one if it doesn't.
   * If the first property does not exist, it will be created with the value of the second. If <code>doRemove</code> is true,
   * then the second property will be removed. This method is essentially used for backward compatibility with previous versions
   * of JPPF configuration files.
   * @param name1 the name of the first property.
   * @param name2 the name of the second property.
   * @param def the default value to use if neither the first nor the second properties are defined.
   * @param doRemove <code>true</code> to force the remove of the second property, <code>false</code> otherwise.
   * @return the value of the first property, or the value of the second if it is not found, or the default value.
   */
  public String getAndReplaceString(final String name1, final String name2, final String def, final boolean doRemove)
  {
    String value = getString(name1, null);
    if (value == null) {
      value = getString(name2, def);
      setProperty(name1, "" + value);
    }
    if (doRemove) remove(name2);
    return value;
  }

  /**
   * Extract the properties that passs the specfied filter.
   * @param filter the filter to use, if <code>null</code> then all properties are retruned.
   * @return a new <code>TypedProperties</code> object containing only the properties matching the filter.
   */
  public TypedProperties filter(final Filter filter) {
    TypedProperties result = new TypedProperties();
    for (Map.Entry<Object, Object> entry: entrySet()) {
      if ((entry.getKey() instanceof String) && (entry.getKey() instanceof String)) {
        if ((filter == null) || filter.accepts((String) entry.getKey(), (String) entry.getValue()))
          result.put(entry.getKey(), entry.getValue());
      }
    }
    return result;
  }

  /**
   * A filter for <code>TypedProperties</code> objects.
   */
  public interface Filter {
    /**
     * Determine whether this filter accepts a property with the specirfied name and value.
     * @param name the name of the property.
     * @param value the value of the property.
     * @return <code>true</code> if the property is accepted, <code>false</code> otherwise.
     */
    boolean accepts(String name, String value);
  }
}
