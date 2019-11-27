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
 * Visits the JPPF MBean in a remote JVM and generates a reference documentation page in wikimedia format.
 * The intent is to copy/paste the generated wikimedia markup into the JPPF online doc wiki.
 * @author Laurent Cohen
 */
public class WikiMBeanInfoWriter extends AbstractMBeanInfoWriter<WikiMBeanInfoWriter> {
  /**
   * 
   */
  private static final int START_HEADING_LEVEL = 1;
  /**
   * A cache of converted types, for performance optimization.
   */
  private final Map<Integer, String> headings = new HashMap<>();

  /**
   * Initialize this visitor witht he specified {@link Writer}.
   * @param writer the writer in which to print the generated wiki code.
   */
  public WikiMBeanInfoWriter(final Writer writer) {
    super(writer);
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
      println();
      heading = getHeading(START_HEADING_LEVEL + 2);
      println("%s Notifications %s", heading, heading).println();
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
    final Descriptor descriptor = attribute.getDescriptor();
    final String desc = (String) descriptor.getFieldValue(MBeanInfoExplorer.DESCRIPTION_FIELD);
    print("'''%s''':", attribute.getName());
    if (desc != null) print(" %s", desc);
    println().println();
    println("* type: <tt>%s</tt>", handleType(attribute.getType(), descriptor));
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
      if ((count % 2 == 1) && (count > 1)) sb.append(",<br>&nbsp;&nbsp;&nbsp;&nbsp;");
      else if (count > 1) sb.append(", ");
      sb.append(formatType(param.getType()));
      final String name = (String) param.getDescriptor().getFieldValue(MBeanInfoExplorer.PARAM_NAME_FIELD);
      if (name != null) sb.append(' ').append(name);
    }
    
    println("<div class='mbean'>%s %s(%s)</div>", handleType(operation.getReturnType(), descriptor), operation.getName(), sb);
    println();
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
   * Convert the specified string into a javadoc URL.
   * @param type the type to convert.
   * @return the converted type.
   */
  @Override
  String formatObjectType(final String type) {
    final String name = type.startsWith("L") ? type.substring(1, type.length() - 1) : type;
    final String url = (name.startsWith("org.jppf."))
      ? "[{{SERVER}}/javadoc/6.2/index.html?" + name.replace(".", "/") + ".html"
      : "https://docs.oracle.com/javase/8/docs/api/index.html?" + name.replace(".", "/") + ".html";
    final String label = name.substring(name.lastIndexOf('.') + 1);
    return "[" + url + " " + label + "]";
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
}
