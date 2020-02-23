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
 * Visits the JPPF MBean in a remote JVM and generates a reference documentation page in HTML format.
 * The intent is to be able to copy  the HTLK page and paste it into a Libre Office document, with minimal manual reformatting.
 * @author Laurent Cohen
 */
public class HTMLMBeanInfoWriter extends AbstractMBeanInfoWriter<HTMLMBeanInfoWriter> {
  /**
   * The highest headling level used in the generated HTML.
   */
  private static final int START_HEADING_LEVEL = 3;

  /**
   * Initialize this visitor witht he specified {@link Writer}.
   * @param writer the writer in which to print the generated wiki code.
   */
  public HTMLMBeanInfoWriter(final Writer writer) {
    super(writer);
  }

  @Override
  public void start(final DriverConnectionInfo connectionInfo) throws Exception {
    println(applyHeading("MBeans in a JPPF " + connectionInfo.getName(), START_HEADING_LEVEL)).println();
  }

  @Override
  public void end(final DriverConnectionInfo connectionInfo) throws Exception {
    writer.flush();
  }

  @Override
  public void startMBean(final ObjectName name, final MBeanInfo info) throws Exception {
    final Descriptor descriptor = info.getDescriptor();
    final String inf = (String) descriptor.getFieldValue("interfaceClassName");
    println(applyHeading(inf.substring(inf.lastIndexOf('.') + 1), START_HEADING_LEVEL + 1)).println();
    println("<ul>");
    print("  ").println(applyTag("object name: " + applyTag(name.toString(), "b"), "li"));
    print("  ").println(applyTag("interface name: " + applyTag(formatType(inf), "tt"), "li"));
    final String desc = (String) descriptor.getFieldValue(MBeanInfoExplorer.DESCRIPTION_FIELD);
    if (desc != null) print("  ").println(applyTag("description: " + desc, "li"));
    println("</ul><br>");
    final String notifDesc = (String) descriptor.getFieldValue(MBeanInfoExplorer.NOTIF_DESCRIPTION_FIELD);
    if (notifDesc != null) {
      println();
      println(applyHeading("Notifications", START_HEADING_LEVEL + 2)).println();
      println("<ul>");
      print("  ").println(applyTag("description: " + notifDesc, "li"));
      print("  ").println(applyTag("type: " + applyTag(formatType((String) descriptor.getFieldValue(MBeanInfoExplorer.NOTIF_CLASS_FIELD)), "tt"), "li"));
      final String userDataDesc = (String) descriptor.getFieldValue(MBeanInfoExplorer.NOTIF_USER_DATA_DESCRIPTION_FIELD);
      final String userDataType = (String) descriptor.getFieldValue(MBeanInfoExplorer.NOTIF_USER_DATA_CLASS_FIELD);
      if (!isEmpty(userDataDesc)) {
        print("  ").println(applyTag("user data: " + userDataDesc, "li"));
        final String type = Object.class.getName().equals(userDataType) ? "any type" : formatType(userDataType);
        print("  ").println(applyTag("user data type: " + applyTag(type, "tt"), "li"));
      } else {
        if (!Object.class.getName().equals(userDataType)) print("  ").println(applyTag("user data type: " + applyTag(formatType(userDataType), "tt"), "li"));
      }
      println("</ul><br>");
    }
    println();
  }

  @Override
  public void startAttributes(final MBeanAttributeInfo[] attributes) throws Exception {
    if ((attributes != null) && (attributes.length > 0)) println(applyHeading("Attributes", START_HEADING_LEVEL + 2)).println();
  }

  @Override
  public void visitAttribute(final MBeanAttributeInfo attribute) throws Exception {
    final Descriptor descriptor = attribute.getDescriptor();
    final String desc = (String) descriptor.getFieldValue(MBeanInfoExplorer.DESCRIPTION_FIELD);
    print("<p>" + applyTag(attribute.getName(), "b") + ":");
    if (desc != null) print(" " + desc);
    println();
    println("<ul>");
    print("  ").println(applyTag("type: " + applyTag(handleType(attribute.getType(), descriptor), "tt"), "li"));
    final boolean r = attribute.isReadable(), w = attribute.isWritable();
    String s = null;
    if (r && w) s = "readable / writable";
    else if (r) s = "readable";
    else if (w) s = "writable";
    if (s != null) print("  ").println(applyTag(s, "li"));
    println("</ul><br>");
    println();
  }

  @Override
  public void startOperations(final MBeanOperationInfo[] operations) throws Exception {
    if ((operations != null) && (operations.length > 0)) println(applyHeading("Operations", START_HEADING_LEVEL + 2)).println();
  }

  @Override
  public void visitOperation(final MBeanOperationInfo operation) throws Exception {
    final Descriptor descriptor = operation.getDescriptor();
    final String desc = (String) descriptor.getFieldValue(MBeanInfoExplorer.DESCRIPTION_FIELD);
    print("<p>" + applyTag(operation.getName(), "b") + ":");
    if (desc != null) print(" " + desc);
    println();
    final StringBuilder sb = new StringBuilder();
    final MBeanParameterInfo[] params = operation.getSignature();
    int count = 0;
    for (final MBeanParameterInfo param: params) {
      count++;
      /*if ((count % 2 == 1) && (count > 1)) sb.append(",<br>&nbsp;&nbsp;&nbsp;&nbsp;");
      else*/ if (count > 1) sb.append(", ");
      sb.append(formatType(param.getType()));
      final String name = (String) param.getDescriptor().getFieldValue(MBeanInfoExplorer.PARAM_NAME_FIELD);
      if (name != null) sb.append(' ').append(name);
    }
    
    println("<pre class='mbean'>%s %s(%s)</pre>", handleType(operation.getReturnType(), descriptor), operation.getName(), sb);
    println();
  }

  /**
   * Apply the specified heading level to the specified source String.
   * @param source the string to surround with heading tags.
   * @param level the heading level.
   * @return the source string surrounded by the heading tag.
   */
  private static String applyHeading(final String source, final int level) {
    return applyTag(source, "h" + level);
  }

  /**
   * Apply the specified heading level to the specified source String.
   * @param source the string to surround with heading tags.
   * @param tag the name of the tag to apply. 
   * @return the source string surrounded by the tag.
   */
  private static String applyTag(final String source, final String tag) {
    return new StringBuilder("<").append(tag).append(">").append(source).append("</").append(tag).append('>').toString();
  }

  @Override
  String formatObjectType(final String type) {
    final String name = type.startsWith("L") ? type.substring(1, type.length() - 1) : type;
    final String ref = formatGenericType(name);
    final String url = (name.startsWith("org.jppf."))
      ? "https://www.jppf.org/javadoc/" + JPPF_VERSION + "/index.html?" + ref.replace(".", "/") + ".html"
      : "https://docs.oracle.com/javase/8/docs/api/index.html?" + ref.replace(".", "/") + ".html";
    final String label = name.substring(name.lastIndexOf('.') + 1).replace("<", "&lt;").replace(">", "&gt;");
    return "<a href='" + url + "'>" + label + "</a>";
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
        MBeanInfoExplorer.visit(new HTMLMBeanInfoWriter(writer), remote, mbeanFilter, feature -> !StringUtils.isOneOf(feature.getName(), false, filteredElements));
      }
      writer.append("</div>\n");
      writer.append("{{NavPathBottom|[[Main Page]] > [[Management and monitoring]] > [[MBeans reference]]}}");
    } catch (final Exception e) {
      e.printStackTrace();
    }
  }
}
