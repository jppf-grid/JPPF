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

package org.jppf.example.configuration;

import java.util.*;

import org.jppf.utils.FileUtils;
import org.jppf.utils.collections.*;
import org.jppf.utils.configuration.*;

/**
 * Converts the JPPF predefined properties into an HTML refrence.
 * The properties are grouped by tag abd presented as a tabke ib a sungle HTML page.  
 * @author Laurent Cohen
 */
public class ConfigurationHTMLPrinter {
  /**
   * HTML character entity conversions.
   */
  private final static Map<String, String> HTML_CONVERSIONS = new LinkedHashMap<String, String>() {{
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
  private final static Map<String, String> DESC_HTML_CONVERSIONS = new LinkedHashMap<String, String>() {{
    put("|", "&#124;");
  }};
  /**
   * Names of the properties whose default value is {@code Runtime.getRuntime().availableProcessors()}.
   */
  final Set<String> AVAILABLE_PROCESSORS_NAMES = new HashSet<>(Arrays.asList(
    "jppf.node.forwarding.pool.size", "jppf.recovery.reaper.pool.size",
    "jppf.processing.threads", "jppf.local.execution.threads"));
  /**
   * Mapping of tags to readable names.
   */
  private final static Map<String, String> TAG_NAMES = new LinkedHashMap<String, String>() {{
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
    put("internal", "Internal use");
    put("nio", "NIO");
    put("persistence", "Persistence");
    put("jmxremote", "JMX remote");
    put("management", "Management");
  }};
  /**
   * Tag ordering.
   */
  private static final List<String> TAG_ORDER = new ArrayList<>(TAG_NAMES.keySet());
  /**
   * Holds the printed HTML document.
   */
  private StringBuilder sb;
  /**
   * Holds and maintains the indentation level in the printed document.
   */
  private int indent = 0;

  /**
   * Entry point.
   * @param args not used.
   */
  public static void main(final String[] args) {
    try {
      // generate the HTML content
      final String content = new ConfigurationHTMLPrinter().printProperties("JPPF configuration properties", JPPFProperties.allProperties());
      // print the content to a file
      FileUtils.writeTextFile("JPPFConfiguration.html", content);
      System.out.println("Wrote content to JPPFConfiguration.html");
    } catch (final Exception e) {
      e.printStackTrace();
    }
  }

  /**
   * Print the specified JPPF properties grouped by tag. 
   * @param title the page title.
   * @param properties the list of properties to print.
   * @return a string holding the resulting HTML document.
   */
  public String printProperties(final String title, final List<JPPFProperty<?>> properties) {
    sb = new StringBuilder();
    prologue(title);
    printTitle(title);
    // multimap with keys in tags' ascending order and values in property names' ascending order
    final CollectionMap<String, JPPFProperty<?>> taggedMap = new SortedSetSortedMap<>(new TagComparator<String>(), new PropertyNameComparator());
    for (final JPPFProperty<?> property: properties) {
      final Set<String> tags = property.getTags();
      for (final String tag: tags) {
        taggedMap.putValue(tag, property);
      }
    }
    System.out.printf("%d properties were found, distributed in %d tags/categories\n", properties.size(), taggedMap.keySet().size());
    for (final String tag: taggedMap.keySet()) {
      final Collection<JPPFProperty<?>> values = taggedMap.getValues(tag);
      final int size = values.size();
      System.out.printf("generating documentation for %2d propert%-3s in '%s'\n", size, (size > 1 ? "ies" : "y"), convertTag(tag));
      printTable(tag, values);
    }
    epilogue();
    return sb.toString();
  }

  /**
   * Print the title for the generated html page.
   * @param title the title to print.
   * @return this printer, for method call chaining.
   */
  private ConfigurationHTMLPrinter printTitle(final String title) {
    printIndent().print("<h1 style='color: #6D78B6'>").print(convertForHTML(title)).println("</h1><br>");
    return this;
  }

  /**
   * Print a HTML table for the given tag and corresponding properties.
   * @param tag the properties tag.
   * @param properties the properties to print in the table.
   * @return this printer, for method call chaining.
   */
  private ConfigurationHTMLPrinter printTable(final String tag, final Collection<JPPFProperty<?>> properties) {
    try {
      doTagTitle(tag);
      for (final JPPFProperty<?> property: properties) {
        doPropertyRow(property);
      }
    } catch (final Exception e) {
      e.printStackTrace();
    }
    return this;
  }

  /**
   * Print the start of the HTML document, icluding the head section.
   * @param title the page title.
   * @return this printer, for method call chaining.
   */
  private ConfigurationHTMLPrinter prologue(final String title) {
    println("<html>").incrementIndent().println("<head>").incrementIndent();
    print("<title>").print(title).println("</title>");
    println("<link rel='stylesheet' type='text/css' href='http://www.jppf.org/jppf.css' title='Style'>");
    println("<link rel='shortcut icon' href='http://www.jppf.org/images/jppf-icon.ico' type='image/x-icon'>");
    println("<style>").incrementIndent();
    println("h1, h2, h3 , h4, h5  { font-family: Arial, Verdana, sans-serif; color: white }");
    println("h1, h2, h3  { margin: 0pt }");
    println("table, td, th { border: solid 1px #6D78B6 }");
    println("table { border-bottom: solid 2px #6D78B6; border-right: solid 2px #6D78B6; width: 100% }");
    println("td, th { padding: 3px }");
    println("td { border-bottom: none; border-right: none }");
    println("th, .header_cell { text-align: left; font-weight: bold; background-color: #C5D0F0; color: #6D78B6; white-space: nowrap; border-color: white }");
    println(".tag_cell { border-left: none; border-top: solid 1px white; background-color: #6D78B6; border-color: #6D78B6; border-top-width: 2px; border-left-width: 2px }");
    println(".deprecated { text-decoration: line-through }");
    decrementIndent().println("</style>");
    return decrementIndent().println("</head>").println("<body>").incrementIndent().startTable();
  }

  /**
   * Print the end of the HTML document.
   * @return this printer, for method call chaining.
   */
  private ConfigurationHTMLPrinter epilogue() {
    return endTable().decrementIndent().println("</body").decrementIndent().println("</html>");
  }

  /**
   * Print a tag title row.
   * @param tag the tag name.
   * @return this printer, for method call chaining.
   */
  private ConfigurationHTMLPrinter doTagTitle(final String tag) {
    println("<tr>").incrementIndent();
    print("<td class='header_cell tag_cell' colspan='5'><h3>").print0(convertTag(tag)).print0ln("</h3></td>");
    decrementIndent().println("</tr>");
    return doHeaderRow();
  }

  /**
   * Print the start of the HTML table.
   * @return this printer, for method call chaining.
   */
  private ConfigurationHTMLPrinter startTable() {
    return println("<table border='0' cellspacing='0' cellpadding='0'>").incrementIndent();
  }

  /**
   * Print the end of the HTML table.
   * @return this printer, for method call chaining.
   */
  private ConfigurationHTMLPrinter endTable() {
    return decrementIndent().println("</table>");
  }

  /**
   * Print a table header row.
   * @return this printer, for method call chaining.
   */
  private ConfigurationHTMLPrinter doHeaderRow() {
    println("<tr>").incrementIndent();
    doHeaderCell("Name");
    doHeaderCell("Default value");
    doHeaderCell("Aliases");
    doHeaderCell("Value type");
    doHeaderCell("Description");
    return decrementIndent().println("</tr>");
  }

  /**
   * Print a table row for the specified proeprty.
   * @param property the property whose information is printed in the rowx.
   * @return this printer, for method call chaining.
   */
  private ConfigurationHTMLPrinter doPropertyRow(final JPPFProperty<?> property) {
    println("<tr>").incrementIndent();
    // property name
    doCell(deprecationStyle(property, convertForHTML(property.getName())));
    // default value
    Object value = property.getDefaultValue();
    if (AVAILABLE_PROCESSORS_NAMES.contains(property.getName())) value = "available processors";
    else if ("jppf.resource.cache.dir".equals(property.getName())) value = "sys.property \"java.io.tmpdir\"";
    else if ("jppf.notification.offload.memory.threshold".equals(property.getName())) value = "80% of max heap size";
    else if (value instanceof String[]) value = toString((String[]) value);
    else if ("".equals(value)) value = "empty string";
    final String val = ((value == null) ? "null" : convertForHTML(value.toString()));
    doCell(deprecationStyle(property, val));
    // aliases
    doCell(deprecationStyle(property, toString(property.getAliases())));
    // value type
    doCell(deprecationStyle(property, property.valueType().getSimpleName()));
    // description
    value = getPropertyDoc(property);
    doCell(value == null ? "" : convertDescription(value.toString()));
    return decrementIndent().println("</tr>");
  }

  /**
   * Checks whether a property is deprecated, and apply the asociated css style tot he specified value if it is.
   * @param property the property to check.
   * @param value the value to convert and print.
   * @return the converted value.
   */
  private static String deprecationStyle(final JPPFProperty<?> property, final String value) {
    return property.isDeprecated() ? "<span class=\"deprecated\">" + value + "</span>" : value;
  }

  /**
   * Prints the specified value into a table cell.
   * @param value the value to print.
   * @return this printer, for method call chaining.
   */
  private ConfigurationHTMLPrinter doCell(final String value) {
    return print("<td>").print0(value).print0ln("</td>");
  }

  /**
   * Prints the specified value into a table header cell.
   * @param value the value to print.
   * @return this printer, for method call chaining.
   */
  private ConfigurationHTMLPrinter doHeaderCell(final String value) {
    return print("<td class='header_cell'>").print0(value).print0ln("</td>");
  }

  /**
   * Print a non-indented value into the document.
   * @param s the value to print.
   * @return this printer, for method call chaining.
   */
  private ConfigurationHTMLPrinter print0(final String s) {
    sb.append(s);
    return this;
  }

  /**
   * Print a non-indented value into the document, with a new line at the end.
   * @param s the value to print.
   * @return this printer, for method call chaining.
   */
  private ConfigurationHTMLPrinter print0ln(final String s) {
    sb.append(s).append('\n');
    return this;
  }

  /**
   * Print an indented value into the document.
   * @param s the value to print.
   * @return this printer, for method call chaining.
   */
  private ConfigurationHTMLPrinter print(final String s) {
    return printIndent().print0(s);
  }

  /**
   * Print an indented value into the document, with a new line at the end.
   * @param s the value to print.
   * @return this printer, for method call chaining.
   */
  private ConfigurationHTMLPrinter println(final String s) {
    return printIndent().print0ln(s);
  }

  /**
   * Print an indent correspoding to the current indent level.
   * @return this printer, for method call chaining.
   */
  private ConfigurationHTMLPrinter printIndent() {
    for (int i=0; i<indent; i++) sb.append("  ");
    return this;
  }

  /**
   * Decrement the indentation level.
   * @return this printer, for method call chaining.
   */
  private ConfigurationHTMLPrinter decrementIndent() {
    indent--;
    return this;
  }

  /**
   * Increment the indentation level.
   * @return this printer, for method call chaining.
   */
  private ConfigurationHTMLPrinter incrementIndent() {
    indent++;
    return this;
  }

  /**
   * Converts an array of strings into a comma-separated list. 
   * @param array the array of string to convert.
   * @return a comma-separated list of the strings in the array.
   */
  private static String toString(final String...array) {
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
  private static class PropertyNameComparator implements Comparator<JPPFProperty<?>> {
    @Override
    public int compare(final JPPFProperty<?> p1, final JPPFProperty<?> p2) {
      return p1.getName().compareTo(p2.getName());
    }
  }

  /**
   * A tag comparator which uses a predefined ordering.
   * @param <T> the type of the compared objects.
   */
  private static class TagComparator<T extends Comparable<T>> implements Comparator<T> {
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
   * Get the documentation for a property, including the description of its eventual parameters and the reason for deprecation, if any.
   * @param property the property for which to get the documentation.
   * @return a string describing the propery.
   */
  private static String getPropertyDoc(final JPPFProperty<?> property) {
    final StringBuilder sb = new StringBuilder();
    if (property.isDeprecated()) {
      sb.append("<i><b>Deprecated:</b> ").append(convertForHTML(property.getDeprecatedDoc())).append("</i><br>");
    }
    sb.append(property.getDocumentation());
    final String[] params = property.getParameters();
    if ((params != null) && (params.length > 0)) {
      for (final String param: params) {
        sb.append("<br>- <i>").append(param).append("</i>: ");
        final String doc = property.getParameterDoc(param);
        if (doc != null) sb.append(doc);
      }
    }
    return sb.toString();
  }

  /**
   * Convert special html characters into character entities.
   * @param src the source string to convert.
   * @return the converted string.
   */
  private static String convertForHTML(final String src) {
    String s = src;
    for (Map.Entry<String, String> entry: HTML_CONVERSIONS.entrySet()) s = s.replace(entry.getKey(), entry.getValue());
    return s;
  }

  /**
   * Convert special characters into character entities in the description column.
   * @param src the source string to convert.
   * @return the converted string.
   */
  private static String convertDescription(final String src) {
    String s = src;
    for (Map.Entry<String, String> entry: DESC_HTML_CONVERSIONS.entrySet()) s = s.replace(entry.getKey(), entry.getValue());
    return s;
  }

  /**
   * Convert tag names into heading titles.
   * @param src the source string to convert.
   * @return the converted string.
   */
  private static String convertTag(final String src) {
    final String s = TAG_NAMES.get(src);
    return s != null ? s : src;
  }
}
