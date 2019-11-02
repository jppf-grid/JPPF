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

package org.jppf.management.doc;

import java.io.*;
import java.util.*;

import javax.management.*;

import org.jppf.discovery.DriverConnectionInfo;
import org.jppf.utils.StringUtils;

/**
 * Visits the JPPF MBean in a remote JVM and generates a reference documentation page
 * in wikimedia format.
 * @author Laurent Cohen
 */
public class WikiMBeanInfoWriter extends MBeanInfoVisitorAdapter {
  /**
   * The writer in which to print the generated wiki code.
   */
  private final Writer writer;
  /**
   * 
   */
  private static final int START_HEADING_LEVEL = 2;
  /**
   * A cache of converted types, for performance optimization.
   */
  private final Map<String, String> typeCache = new HashMap<>();
  /**
   * A cache of converted types, for performance optimization.
   */
  private final Map<Integer, String> headings = new HashMap<>();

  /**
   * Initialize this visitor witht he specified {@link Writer}.
   * @param writer the writer in which to print the generated wiki code.
   */
  public WikiMBeanInfoWriter(final Writer writer) {
    if (writer == null) throw new NullPointerException("writer cannot be null");
    this.writer = writer;
  }

  @Override
  public void start(final DriverConnectionInfo connectionInfo) throws Exception {
    final String heading = getHeading(START_HEADING_LEVEL);
    println("%s MBeans in a JPPF %s %s", heading, connectionInfo.getName(), heading).println();
  }

  @Override
  public void end(final DriverConnectionInfo connectionInfo) throws Exception {
    writer.flush();
  }

  @Override
  public void startMBean(final ObjectName name, final MBeanInfo info) throws Exception {
    final Descriptor descriptor = info.getDescriptor();
    final String inf = (String) descriptor.getFieldValue("interfaceClassName");
    String heading = getHeading(START_HEADING_LEVEL + 1);
    println("%s %s %s", heading, inf.substring(inf.lastIndexOf('.') + 1), heading).println();
    println("* object name: '''%s'''", name);
    println("* interface name: <tt>%s</tt>", formatType(inf));
    final String desc = (String) descriptor.getFieldValue(MBeanInfoExplorer.DESCRIPTION_FIELD);
    if (desc != null) println("* description: %s", desc);
    final String notifDesc = (String) descriptor.getFieldValue(MBeanInfoExplorer.NOTIF_DESCRIPTION_FIELD);
    if (notifDesc != null) {
      heading = getHeading(START_HEADING_LEVEL + 2);
      println("%s Notifications %s", heading, heading);
      println("* description: %s", notifDesc);
      println("* type: <tt>%s</tt>", formatType((String) descriptor.getFieldValue(MBeanInfoExplorer.NOTIF_CLASS_FIELD)));
      final String userDataDesc = (String) descriptor.getFieldValue(MBeanInfoExplorer.NOTIF_USER_DATA_DESCRIPTION_FIELD);
      final String userDataType = (String) descriptor.getFieldValue(MBeanInfoExplorer.NOTIF_USER_DATA_CLASS_FIELD);
      if (!isEmpty(userDataDesc)) {
        println("* user data: %s", userDataDesc);
        final String type = Object.class.getName().equals(userDataType) ? "any type" : formatType(userDataType);
        println("* user data type: <tt>%s</tt>", type);
      } else {
        if (!Object.class.getName().equals(userDataType)) println("* user data type: <tt>%s</tt>", formatType(userDataType));
      }
    }
    println();
  }

  @Override
  public void startAttributes(final MBeanAttributeInfo[] attributes) throws Exception {
    final String heading = getHeading(START_HEADING_LEVEL + 2);
    if ((attributes != null) && (attributes.length > 0)) println("%s Attributes %s", heading, heading).println();
  }

  @Override
  public void visitAttribute(final MBeanAttributeInfo attribute) throws Exception {
    final String desc = (String) attribute.getDescriptor().getFieldValue(MBeanInfoExplorer.DESCRIPTION_FIELD);
    print("'''%s''':", attribute.getName());
    if (desc != null) print(" %s", desc);
    println().println();
    println("* type: <tt>%s</tt>", formatType(attribute.getType()));
    final boolean r = attribute.isReadable(), w = attribute.isWritable();
    if (r && w) println("* readable / writable");
    else if (r) println("* readable");
    else if (w) println("* writable");
    println();
  }

  @Override
  public void startOperations(final MBeanOperationInfo[] operations) throws Exception {
    final String heading = getHeading(START_HEADING_LEVEL + 2);
    if ((operations != null) && (operations.length > 0)) println("%s Operations %s", heading, heading).println();
  }

  @Override
  public void visitOperation(final MBeanOperationInfo operation) throws Exception {
    final Descriptor descriptor = operation.getDescriptor();
    final String desc = (String) descriptor.getFieldValue(MBeanInfoExplorer.DESCRIPTION_FIELD);
    print("'''%s''':", operation.getName());
    if (desc != null) print(" %s", desc);
    println().println();
    final StringBuilder sb = new StringBuilder();
    final MBeanParameterInfo[] params = operation.getSignature();
    int count = 0;
    for (final MBeanParameterInfo param: params) {
      count++;
      if ((count % 3 == 1) && (count > 1)) sb.append(",<br>&nbsp;&nbsp;&nbsp;&nbsp;");
      else if (count > 1) sb.append(", ");
      sb.append(formatType(param.getType()));
      final String name = (String) param.getDescriptor().getFieldValue(MBeanInfoExplorer.PARAM_NAME_FIELD);
      if (name != null) sb.append(' ').append(name);
    }
    println(" %s %s(%s)", formatType(operation.getReturnType()), operation.getName(), sb);
    println();
  }

  /**
   * Print a formatted message.
   * @param format the message format.
   * @param params the message parameters.
   * @return this object, for method call chaining.
   * @throws Exception if any error occurs.
   */
  private WikiMBeanInfoWriter print(final String format, final Object...params) throws Exception {
    final String msg = String.format(format, params);
    writer.write(msg);
    return this;
  }

  /**
   * Print a message, appending a line terminator.
   * @param format the message format.
   * @param params the message parameters.
   * @return this object, for method call chaining.
   * @throws Exception if any error occurs.
   */
  private WikiMBeanInfoWriter println(final String format, final Object...params) throws Exception {
    return print(format, params).println();
  }

  /**
   * Print a formatted message.
   * @param msg the message format.
   * @return this object, for method call chaining.
   * @throws Exception if any error occurs.
   */
  private WikiMBeanInfoWriter print(final String msg) throws Exception {
    writer.write(msg);
    return this;
  }

  /**
   * Print a message, appending a line terminator.
   * @param msg the message format.
   * @return this object, for method call chaining.
   * @throws Exception if any error occurs.
   */
  private WikiMBeanInfoWriter println(final String msg) throws Exception {
    return print(msg).println();
  }

  /**
   * Print a blank line.
   * @return this object, for method call chaining.
   * @throws Exception if any error occurs.
   */
  private WikiMBeanInfoWriter println() throws Exception {
    writer.write("\n");
   return this;
  }

  /**
   * Get the heading marker for the pseicfied level. 
   * @param level the heading level.
   * @return the heading marker.
   */
  private String getHeading(final int level) {
    if (level <= 0) return "";
    String heading = headings.get(level);
    if (heading == null) {
      heading = "";
      for (int i=0; i<level; i++) heading += "=";
      headings.put(level, heading);
    }
    return heading;
  }

  /**
   * COnvert the specified string into a url pointing to either a JPPF ajavadoc page or a J2SE jaavdoc page, or a primitive type name.
   * @param type the type to convert.
   * @return the converted type.
   */
  private String formatType(final String type) {
    String result =  typeCache.get(type);
    if (result != null) return result;
    if (type.startsWith("[")) result = formatArrayType(type);
    else if (type.startsWith("L") || type.contains(".")) result = formatObjectType(type);
    else result = type;
    typeCache.put(type, result);
    return result;
  }

  /**
   * Convert the specified string into a javadoc URL.
   * @param type the type to convert.
   * @return the converted type.
   */
  private static String formatObjectType(final String type) {
    final String name = type.startsWith("L") ? type.substring(1, type.length() - 1) : type;
    if (name.startsWith("org.jppf.")) return String.format("[{{SERVER}}/javadoc/6.2/index.html?%s.html %s]", name.replace(".", "/"), name.substring(name.lastIndexOf('.') + 1));
    return String.format("[https://docs.oracle.com/javase/8/docs/api/index.html?%s.html %s]", name.replace(".", "/"), name.substring(name.lastIndexOf('.') + 1));
  }

  /**
   * Convert the specified string into a java-like syntax representing an array type.
   * @param type the type to convert.
   * @return the converted type.
   */
  private static String formatArrayType(final String type) {
    int count = 0;
    while (type.charAt(count) == '[') count++;
    final String compSig = type.substring(count);
    final String compName = compSig.startsWith("L") ? formatObjectType(compSig): compSig;
    final StringBuilder sb = new StringBuilder();
    for (int i=0; i<count; i++) sb.append("&#91;&#93;");
    return compName + sb.toString();
  }

  /**
   * Entry point.
   * @param args not used.
   */
  public static void main(final String[] args) {
    final String[] filteredMBeans = { "JMXNioServerMBean", "PeerDriverMBean", "ServerDebugMBean", "JmxLogger", "PersistedJobsManagerMBean", "NodeDebugMBean" };
    final String[] filteredElements = { "addNotificationListener", "removeNotificationListener", "NotificationInfo" };
    final List<DriverConnectionInfo> remotes = Arrays.asList(new DriverConnectionInfo("driver", "localhost", 11111), new DriverConnectionInfo("node", "localhost", 12001));
    try (final BufferedWriter writer = new BufferedWriter(new FileWriter("mbeans-wiki.txt"))) {
      writer.append("{{NavPath|[[Main Page]] > [[Management and monitoring]] > [[MBeans reference]]}}<br/>\n");
      writer.append("<div align='justify'>\n\n");
      final MBeanFilter mbeanFilter = info -> {
        final String inf = (String) info.getDescriptor().getFieldValue("interfaceClassName");
        return !StringUtils.isOneOf(inf.substring(inf.lastIndexOf('.') + 1), false, filteredMBeans);
      };
      for (final DriverConnectionInfo remote: remotes) {
        MBeanInfoExplorer.visit(new WikiMBeanInfoWriter(writer), remote, mbeanFilter, feature -> !StringUtils.isOneOf(feature.getName(), false, filteredElements));
      }
      writer.append("</div>\n");
      writer.append("{{NavPathBottom|[[Main Page]] > [[Management and monitoring]] > [[MBeans reference]]}}");
    } catch (final Exception e) {
      e.printStackTrace();
    }
  }

  /**
   * Determine whether the specified string is either {@code null} or empty.
   * @param s the string to check.
   * @return {@code true} if the string is empty, {@code false} otherwise.
   */
  private static boolean isEmpty(final String s) {
    return (s == null) || s.isEmpty();
  }
}
