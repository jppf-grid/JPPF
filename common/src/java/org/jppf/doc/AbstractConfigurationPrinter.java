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

package org.jppf.doc;

import java.util.*;

import org.jppf.utils.collections.*;
import org.jppf.utils.configuration.JPPFProperty;

/**
 * Converts the JPPF predefined properties into the "Configuration properties reference" section of the doc as wiki text. 
 * @author Laurent Cohen
 * @exclude
 */
public abstract class AbstractConfigurationPrinter {
  /**
   * HTML character entity conversions.
   */
  final static Map<String, String> HTML_CONVERSIONS = new LinkedHashMap<String, String>() {{
    put("\\", "/");
    put("&", "&amp;");
    put("<", "&lt;");
    put(">", "&gt;");
    put("|", "&#124;");
    put("defined with the 'jppf.drivers' property", "in 'jppf.drivers'");
    put("peer driver names defined with the 'jppf.peers' property", "names defined in 'jppf.peers'");
    put("org.jppf.job.persistence.impl", "o.j.j.p.i");
    put("9223372036854775807", "Long.MAX_VALUE");
    put("2147483647", "Integer.MAX_VALUE");
  }};
  /**
   * HTML character entity conversions for the description column.
   */
  final static Map<String, String> DESC_HTML_CONVERSIONS = new LinkedHashMap<String, String>() {{
    put("|", "&#124;");
  }};
  /**
   * Mapping of tags to readable names.
   */
  final static Map<String, String> TAG_NAMES = new LinkedHashMap<String, String>() {{
    put("driver", "Driver");
    put("node", "Node");
    put("screensaver", "Node screensaver");
    put("client", "Client");
    put("console", "Desktop console");
    put("web console", "Web console");
    put("admin", "Desktop and Web consoles");
    put("common", "Common");
    put(".net", ".Net");
    put("persistence", "Persistence");
    put("ssl", "SSL/TLS");
    put("jmxremote", "JMX remote");
    put("memory", "Memory usage optimization");
  }};
  /**
   * Names of the properties whose default value is {@code Runtime.getRuntime().availableProcessors()}.
   */
  final Set<String> AVAILABLE_PROCESSORS_NAMES = new HashSet<>(Arrays.asList("jppf.node.forwarding.pool.size", "jppf.recovery.reaper.pool.size",
    "jppf.processing.threads", "jppf.local.execution.threads"));
  /**
   * Tag ordering.
   */
  static final List<String> TAG_ORDER = new ArrayList<>(TAG_NAMES.keySet());
  /**
   * Tags to exclude.
   */
  static Set<String> EXCLUDED_TAGS = new HashSet<>(Arrays.asList("internal", "nio", "persistence", "jmxremote"));
  /**
   * Holds the printed HTML document.
   */
  StringBuilder sb;

  /**
   * Print all JPPF properties grouped by tag. 
   * @param title the page title.
   * @param properties the list of properties to print.
   * @return a string holding the resulting HTML document.
   */
  public String printProperties(final String title, final List<JPPFProperty<?>> properties) {
    sb = new StringBuilder();
    prologue();
    final CollectionMap<String, JPPFProperty<?>> taggedMap = new SortedSetSortedMap<>(new TagComparator<String>(), new PropertyNameComparator());
    for (final JPPFProperty<?> property: properties) {
      final Set<String> tags = new HashSet<>(property.getTags());
      if (!tags.removeAll(EXCLUDED_TAGS)) {
        for (final String tag: tags) {
          taggedMap.putValue(tag, property);
        }
      }
    }
    System.out.println("all tags: " + taggedMap.keySet());
    for (final String tag: taggedMap.keySet()) {
      printTable(tag, taggedMap.getValues(tag));
    }
    epilogue();
    return sb.toString();
  }

  /**
   * Print the start of the HTML document, icluding the head section.
   * @return this printer, for method call chaining.
   */
  abstract AbstractConfigurationPrinter prologue();

  /**
   * Print the end of the HTML document.
   * @return this printer, for method call chaining.
   */
  abstract AbstractConfigurationPrinter epilogue();

  /**
   * Print a HTML table for the given tag and corresponding properties.
   * @param tag the properties tag.
   * @param properties the properties to print in the table.
   * @return this printer, for method call chaining.
   */
  AbstractConfigurationPrinter printTable(final String tag, final Collection<JPPFProperty<?>> properties) {
    try {
      doTagTitle(tag);
      startTable();
      doHeaderRow();
      for (JPPFProperty<?> prop: properties) doPropertyRow(prop);
      endTable();
    } catch (final Exception e) {
      e.printStackTrace();
    }
    return this;
  }

  /**
   * Print a tag title row.
   * @param tag the tag name.
   * @return this printer, for method call chaining.
   */
  abstract AbstractConfigurationPrinter doTagTitle(final String tag);

  /**
   * Print the start of the HTML table.
   * @return this printer, for method call chaining.
   */
  abstract AbstractConfigurationPrinter startTable();

  /**
   * Print the end of the HTML table.
   * @return this printer, for method call chaining.
   */
  abstract AbstractConfigurationPrinter endTable();

  /**
   * Print a table header row.
   * @return this printer, for method call chaining.
   */
  abstract AbstractConfigurationPrinter doHeaderRow();

  /**
   * Print a table row for the specified proeprty.
   * @param property the property whose information is printed in the rowx.
   * @return this printer, for method call chaining.
   */
  abstract AbstractConfigurationPrinter doPropertyRow(final JPPFProperty<?> property);

  /**
   * Prints the specified value into a table cell.
   * @param value the value to print.
   * @return this printer, for method call chaining.
   */
  abstract AbstractConfigurationPrinter doCell(final String value);

  /**
   * Prints the specified value into a table header cell.
   * @param value the value to print.
   * @return this printer, for method call chaining.
   */
  abstract AbstractConfigurationPrinter doHeaderCell(final String value);

  /**
   * Print a non-indented value into the document.
   * @param s the value to print.
   * @return this printer, for method call chaining.
   */
  abstract AbstractConfigurationPrinter print(final String s);

  /**
   * Print a non-indented value into the document, with a new line at the end.
   * @param s the value to print.
   * @return this printer, for method call chaining.
   */
  abstract AbstractConfigurationPrinter println(final String s);

  /**
   * Converts an array of strings into a comma-separated list. 
   * @param array the array of string to convert.
   * @return a comma-separated list of the strings in the array.
   */
  static String toString(final String[] array) {
    if ((array == null) || (array.length <= 0)) return "&nbsp;";
    final StringBuilder sb = new StringBuilder();
    for (int i=0; i<array.length; i++) {
      if (i > 0) sb.append(", ");
      sb.append(array[i]);
    }
    return sb.toString();
  }

  /**
   * Compares {@link JPPFProperty} instances based on their names in ascending order.
   */
  static class PropertyNameComparator implements Comparator<JPPFProperty<?>> {
    @Override
    public int compare(final JPPFProperty<?> o1, final JPPFProperty<?> o2) {
      return o1.getName().compareTo(o2.getName());
    }
  }

  /**
   * A generic comparator.
   * @param <T> the type of the compared objects.
   */
  static class TagComparator<T extends Comparable<T>> implements Comparator<T> {
    @Override
    public int compare(final T o1, final T o2) {
      final int idx1 = TAG_ORDER.indexOf(o1);
      final int idx2 = TAG_ORDER.indexOf(o2);
      if ((idx1 < 0) && (idx2 < 0)) return 0;
      if (idx1 < 0) return 1;
      if (idx2 < 0) return -1;
      return (idx1 < idx2) ? -1 : ((idx1 > idx2) ? 1 : 0);
    }
  }

  /**
   * Get the documentation for a property, including the description of its eventual parameters.
   * @param property the property for which to get the documentation.
   * @return a string describing the propery.
   */
  abstract String getPropertyDoc(final JPPFProperty<?> property);

  /**
   * Convert special html characters into character entities.
   * @param src the source string to convert.
   * @return the converted string.
   */
  static String convert(final String src) {
    String s = src;
    for (Map.Entry<String, String> entry: HTML_CONVERSIONS.entrySet()) s = s.replace(entry.getKey(), entry.getValue());
    return s;
  }

  /**
   * Convert special characters into character entities in the description column.
   * @param src the source string to convert.
   * @return the converted string.
   */
  static String convertDescription(final String src) {
    String s = src;
    for (Map.Entry<String, String> entry: DESC_HTML_CONVERSIONS.entrySet()) s = s.replace(entry.getKey(), entry.getValue());
    return s;
  }

  /**
   * Convert tag names into heading titles.
   * @param src the source string to convert.
   * @return the converted string.
   */
  static String convertTag(final String src) {
    final String s = TAG_NAMES.get(src);
    return s != null ? s : src;
  }
}
