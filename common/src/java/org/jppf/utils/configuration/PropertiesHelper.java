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

package org.jppf.utils.configuration;

import java.io.*;
import java.util.*;
import java.util.regex.Pattern;

/**
 * 
 * @author Laurent Cohen
 * @exclude
 */
public class PropertiesHelper {
  /**
   * A regex that matches underscore ('_') characters.
   */
  private static final Pattern UNDERSCORE_PATTERN = Pattern.compile("_");

  /**
   * Store the specified properties into the specified writer.
   * @param props the properties to store.
   * @param writer the writer to store with.
   * @throws IOException if any error occurs.
   */
  public static void store(final Properties props, final Writer writer) throws IOException {
    final Set<String> keys = new TreeSet<>(props.stringPropertyNames());
    for (final String k: keys) {
      final Object v = props.get(k);
      if (v instanceof String) {
        writer.write(k);
        writer.write(" = ");
        writer.write((String) v);
        writer.write('\n');
      }
    }
  }

  /**
   * Get the integer value of the specified string.
   * @param val the string val to convert to an int.
   * @param defValue a default value to return if value is {@code null}.
   * @return the value of the property as an int, or the default value if it is not found.
   */
  public static int toInt(final String val, final int defValue) {
    int intVal = defValue;
    if (val != null) {
      final String s = removeUnderscores(val.trim());
      try {
        intVal = Integer.valueOf(s);
      } catch(@SuppressWarnings("unused") final NumberFormatException e) {
        try {
          intVal = Double.valueOf(s).intValue();
        } catch(@SuppressWarnings("unused") final NumberFormatException ignore) { }
      }
    }
    return intVal;
  }

  /**
   * Get the long value of the specified string.
   * @param val the string value to convert to a long.
   * @param defValue the default val to return if value is {@code null}.
   * @return the value of the property a a long, or the default value if it is not found.
   */
  public static long toLong(final String val, final long defValue) {
    long longVal = defValue;
    if (val != null) {
      final String s = removeUnderscores(val.trim());
      try {
        longVal = Long.valueOf(s);
      } catch(@SuppressWarnings("unused") final NumberFormatException e) {
        try {
          longVal = Double.valueOf(s).longValue();
        } catch(@SuppressWarnings("unused") final NumberFormatException ignore) { }
      }
    }
    return longVal;
  }

  /**
   * Get the float value of the specified string.
   * @param val the string value to convert to a float.
   * @param defValue the default value to return if val is {@code null}.
   * @return the value of the property a a float, or the default value if it is not found.
   */
  public static float toFloat(final String val, final float defValue) {
    float floatVal = defValue;
    if (val != null) {
      final String s = removeUnderscores(val.trim());
      try {
        floatVal = Float.valueOf(s);
      } catch(@SuppressWarnings("unused") final NumberFormatException e) {
        try {
          floatVal = Double.valueOf(s).floatValue();
        } catch(@SuppressWarnings("unused") final NumberFormatException ignore) { }
      }
    }
    return floatVal;
  }

  /**
   * Get the double value of the specified string.
   * @param val the string value to convert to a double.
   * @param defValue the default value to return if val is {@code null}.
   * @return the value of the property a an double, or the default value if it is not found.
   */
  public static double toDouble(final String val, final double defValue) {
    double doubleVal = defValue;
    if (val != null) {
      final String s = removeUnderscores(val.trim());
      try {
        doubleVal = Double.valueOf(s);
      } catch(@SuppressWarnings("unused") final NumberFormatException e) {
      }
    }
    return doubleVal;
  }

  /**
   * Remove the underscore characters from the specfied string.
   * @param source the string from whihc to remove the underscore characters.
   * @return a new string stripped of underscore characters, or the source string if it doesn't have any underscore character.
   */
  public static String removeUnderscores(final String source) {
    if (source == null) return null;
    return source.contains("_") ? UNDERSCORE_PATTERN.matcher(source).replaceAll("") : source;
  }
}
