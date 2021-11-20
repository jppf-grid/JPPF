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

import java.util.List;

import org.jppf.utils.FileUtils;
import org.jppf.utils.configuration.*;

/**
 * Converts the JPPF predefined properties into the "Configuration properties reference" section of the doc as wiki text.
 * @author Laurent Cohen
 * @exclude
 */
public class WikiConfigurationPrinter extends AbstractConfigurationPrinter {
  /**
   * Entry point.
   * @param args not used.
   */
  public static void main(final String[] args) {
    try {
      if ((args == null) || (args.length <= 0))
        FileUtils.writeTextFile("JPPFConfiguration.html", new WikiConfigurationPrinter().printProperties("JPPF configuration properties", JPPFProperties.allProperties()));
      else for (final String arg: args) {
        final String[] tokens = arg.split("\\|");
        final Class<?> c = Class.forName(tokens[0]);
        final List<JPPFProperty<?>> props = ConfigurationUtils.allProperties(c);
        FileUtils.writeTextFile(tokens[1], new WikiConfigurationPrinter().printProperties(tokens[2], props));
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
  WikiConfigurationPrinter prologue() {
    return println("{{NavPath|[[Main Page]] > [[Configuration properties reference]]}}<br/>").println("");
  }

  /**
   * Print the end of the HTML document.
   * @return this printer, for method call chaining.
   */
  @Override
  WikiConfigurationPrinter epilogue() {
    return println("{{NavPathBottom|[[Main Page]] > [[Configuration properties reference]]}}");
  }

  /**
   * Print a tag title row.
   * @param tag the tag name.
   * @return this printer, for method call chaining.
   */
  @Override
  WikiConfigurationPrinter doTagTitle(final String tag) {
    return print("=== ").print(convertTag(tag)).print(" properties").println(" ===").println("");
  }

  /**
   * Print the start of the HTML table.
   * @return this printer, for method call chaining.
   */
  @Override
  WikiConfigurationPrinter startTable() {
    return println("{| border=\"1\" cellspacing=\"0\" cellpadding=\"2\" width=\"100%\"");
  }

  /**
   * Print the end of the HTML table.
   * @return this printer, for method call chaining.
   */
  @Override
  WikiConfigurationPrinter endTable() {
    return println("|}").println("");
  }

  /**
   * Print a table header row.
   * @return this printer, for method call chaining.
   */
  @Override
  WikiConfigurationPrinter doHeaderRow() {
    println("|-style=\"background-color: #E8EAFD; line-height: 1.3em\"");
    doHeaderCell("Name");
    doHeaderCell("Default value");
    /*
    doHeaderCell("Aliases");
    doHeaderCell("Value type");
     */
    doHeaderCell("Description");
    return this;
  }

  /**
   * Print a table row for the specified proeprty.
   * @param property the property whose information is printed in the rowx.
   * @return this printer, for method call chaining.
   */
  @Override
  WikiConfigurationPrinter doPropertyRow(final JPPFProperty<?> property) {
    println("|-style=\"line-height: 1.1em\"");
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
    /*
    // aliases
    doCell(toString(prop.getAliases()));
    // value type
    doCell(prop.valueType().getSimpleName());
     */
    // description
    value = getPropertyDoc(property);
    doCell(value == null ? "" : convertDescription(value.toString()));
    return this;
  }

  /**
   * Prints the specified value into a table cell.
   * @param value the value to print.
   * @return this printer, for method call chaining.
   */
  @Override
  WikiConfigurationPrinter doCell(final String value) {
    return print("| ").println(value);
  }

  /**
   * Prints the specified value into a table header cell.
   * @param value the value to print.
   * @return this printer, for method call chaining.
   */
  @Override
  WikiConfigurationPrinter doHeaderCell(final String value) {
    return print("| '''").print(value).println("'''");
  }

  /**
   * Print a non-indented value into the document.
   * @param s the value to print.
   * @return this printer, for method call chaining.
   */
  @Override
  WikiConfigurationPrinter print(final String s) {
    sb.append(s);
    return this;
  }

  /**
   * Print a non-indented value into the document, with a new line at the end.
   * @param s the value to print.
   * @return this printer, for method call chaining.
   */
  @Override
  WikiConfigurationPrinter println(final String s) {
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
    if (property.isDeprecated()) sb.append("'''''Deprecated:''' ").append(convert(property.getDeprecatedDoc())).append("''<br>");
    sb.append(property.getDocumentation());
    final String[] params = property.getParameters();
    if ((params != null) && (params.length > 0)) for (String param: params) {
      sb.append("<br>- <i>").append(param).append("</i>: ");
      final String doc = property.getParameterDoc(param);
      if (doc != null) sb.append(convert(doc));
    }
    return sb.toString();
  }
}
