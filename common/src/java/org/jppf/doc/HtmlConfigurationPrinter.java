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

import org.jppf.utils.FileUtils;
import org.jppf.utils.configuration.*;

/**
 * Converts the JPPF predefined properties into the "Configuration properties reference" section of the doc as wiki text.
 * @author Laurent Cohen
 * @exclude
 */
public class HtmlConfigurationPrinter extends AbstractConfigurationPrinter {
  /**
   * HTML character entity conversions for the description column.
   */
  private final static Map<String, String> DESCRIPTION_CONVERSIONS = new LinkedHashMap<String, String>() {{
    put("_i_", "<i>");
    put("_/i_", "</i>");
    put("_b_", "<b>");
    put("_/b_", "</b>");
    put("_br_", "<br>");
  }};

  /**
   * Entry point.
   * @param args not used.
   */
  public static void main(final String[] args) {
    try {
      if ((args == null) || (args.length <= 0))
        FileUtils.writeTextFile("JPPFConfiguration.html", new HtmlConfigurationPrinter().printProperties("JPPF configuration properties", JPPFProperties.allProperties()));
      else for (final String arg: args) {
        final String[] tokens = arg.split("\\|");
        final Class<?> c = Class.forName(tokens[0]);
        final List<JPPFProperty<?>> props = ConfigurationUtils.allProperties(c);
        FileUtils.writeTextFile(tokens[1], new HtmlConfigurationPrinter().printProperties(tokens[2], props));
      }
    } catch (final Exception e) {
      e.printStackTrace();
    }
  }

  /**
   * Print the start of the HTML document, icluding the head section.
   * @return this printer, for method call chaining.
   */
  @Override
  HtmlConfigurationPrinter prologue() {
    //
    return println("<html><head><link rel=\"stylesheet\" type=\"text/css\" href=\"http://www.jppf.org/jppf.css\" title=\"Style\"></head><body>");
  }

  /**
   * Print the end of the HTML document.
   * @return this printer, for method call chaining.
   */
  @Override
  HtmlConfigurationPrinter epilogue() {
    return println("</body></html>");
  }

  /**
   * Print a tag title row.
   * @param tag the tag name.
   * @return this printer, for method call chaining.
   */
  @Override
  HtmlConfigurationPrinter doTagTitle(final String tag) {
    return print("<h2>").print(convertTag(tag)).print(" properties").println("</h2>").println("");
  }

  /**
   * Print the start of the HTML table.
   * @return this printer, for method call chaining.
   */
  @Override
  HtmlConfigurationPrinter startTable() {
    return println("<table border=\"1\" cellspacing=\"0\" cellpadding=\"2\" width=\"100%\">");
  }

  /**
   * Print the end of the HTML table.
   * @return this printer, for method call chaining.
   */
  @Override
  HtmlConfigurationPrinter endTable() {
    return println("</table>").println("");
  }

  /**
   * Print a table header row.
   * @return this printer, for method call chaining.
   */
  @Override
  HtmlConfigurationPrinter doHeaderRow() {
    println("<tr style=\"background-color: #E8EAFD\">");
    doHeaderCell("Name");
    doHeaderCell("Default value");
    doHeaderCell("Description");
    println("</tr>");
    return this;
  }

  /**
   * Print a table row for the specified proeprty.
   * @param property the property whose information is printed in the rowx.
   * @return this printer, for method call chaining.
   */
  @Override
  HtmlConfigurationPrinter doPropertyRow(final JPPFProperty<?> property) {
    println("<tr>");
    // property name
    String name = convert(property.getName());
    if (property.isDeprecated()) name = "<span style=\"text-decoration: line-through\">" + name + "</span>";
    doCell(name);
    // default value
    Object value = property.getDefaultValue();
    if (AVAILABLE_PROCESSORS_NAMES.contains(property.getName())) value = "available processors";
    else if ("jppf.resource.cache.dir".equals(property.getName())) value = "sys.property \"java.io.tmpdir\"";
    else if ("jppf.notification.offload.memory.threshold".equals(property.getName())) value = "80% of max heap size";
    else if (value instanceof String[]) value = toString((String[]) value);
    else if ("".equals(value)) value = "empty string";
    String val = ((value == null) ? "null" : convert(value.toString()));
    if (property.isDeprecated()) val = "<span style=\"text-decoration: line-through\">" + val + "</span>";
    doCell(val);
    // description
    value = getPropertyDoc(property);
    val = ((value == null) ? "null" :convertDescriptionWithFormatting(convert(value.toString())));
    doCell(val);
    println("</tr>");
    return this;
  }

  /**
   * Prints the specified value into a table cell.
   * @param value the value to print.
   * @return this printer, for method call chaining.
   */
  @Override
  HtmlConfigurationPrinter doCell(final String value) {
    return print("<td>").print(value).println("</td>");
  }

  /**
   * Prints the specified value into a table header cell.
   * @param value the value to print.
   * @return this printer, for method call chaining.
   */
  @Override
  HtmlConfigurationPrinter doHeaderCell(final String value) {
    return print("<td><b>").print(value).println("</b></td>");
  }

  /**
   * Print a non-indented value into the document.
   * @param s the value to print.
   * @return this printer, for method call chaining.
   */
  @Override
  HtmlConfigurationPrinter print(final String s) {
    sb.append(s);
    return this;
  }

  /**
   * Print a non-indented value into the document, with a new line at the end.
   * @param s the value to print.
   * @return this printer, for method call chaining.
   */
  @Override
  HtmlConfigurationPrinter println(final String s) {
    sb.append(s).append('\n');
    return this;
  }

  /**
   * Get the documentation for a property, including the description of its eventual parameters.
   * @param property the property for which to get the documentation.
   * @return a string describing the propery.
   */
  @Override
  String getPropertyDoc(final JPPFProperty<?> property) {
    final StringBuilder sb = new StringBuilder();
    if (property.isDeprecated()) sb.append("_i__b_Deprecated:_/b_ ").append(convert(property.getDeprecatedDoc())).append("_/i__br_");
    sb.append(property.getDocumentation());
    final String[] params = property.getParameters();
    if ((params != null) && (params.length > 0)) for (String param: params) {
      sb.append("_br_- _i_").append(param).append("_/i_: ");
      final String doc = property.getParameterDoc(param);
      if (doc != null) sb.append(convert(doc));
    }
    return sb.toString();
  }

  /**
   * Convert special characters into character entities in the description column.
   * @param src the source string to convert.
   * @return the converted string.
   */
  private static String convertDescriptionWithFormatting(final String src) {
    String s = src;
    for (Map.Entry<String, String> entry: DESCRIPTION_CONVERSIONS.entrySet()) s = s.replace(entry.getKey(), entry.getValue());
    return s;
  }

}
