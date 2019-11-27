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

import java.util.*;

import javax.management.*;

import org.jppf.discovery.DriverConnectionInfo;
import org.jppf.management.*;
import org.jppf.management.forwarding.AbstractMBeanForwarder;
import org.jppf.utils.*;

/**
 * Generate the source code for static proxies forwzrding requests to node MBeans through a driver.
 * @author Laurent Cohen
 */
public class NodeForwardingProxyGenerator extends AbstractForwardingCodeGenerator<NodeForwardingProxyGenerator> {
  /**
   * Initialize this visitor.
   */
  public NodeForwardingProxyGenerator() {
    super("org.jppf.management.forwarding.generated", "../common/src/java");
    this.arraySuffix = "[]";
  }

  @Override
  void startMBean(final InterfaceInfo info, final ObjectName mbeanName, final MBeanInfo mbean) throws Exception {
    importedTypes.add(info.interfaceName);
    println("/**");
    println(" * Forwarding proxy for the {@link " + info.interfaceSimpleName + "} MBean.");
    if (info.desc != null) println(" * MBean description: " + info.desc  + (info.desc.endsWith(".") ? "" : "."));

    // add javadoc for the notifications
    if (notifInfo.desc != null) {
      println(" * <p>This Mbean emits notification of type {@link %s}:", formatType((String) info.descriptor.getFieldValue(MBeanInfoExplorer.NOTIF_CLASS_FIELD)));
      println(" * <br>- " + dottedEnd(notifInfo.desc));
      if (!isEmpty(notifInfo.userDataType)) importedTypes.add(notifInfo.userDataType);
      if (!isEmpty(notifInfo.userDataDesc)) {
        println(" * <br>- user data: %s", dottedEnd(notifInfo.userDataDesc));
        if (Object.class.getName().equals(notifInfo.userDataType)) println(" * <br>- user data type: any type.");
        else println(" * <br>- user data type: {@link %s}.", formatType(notifInfo.userDataType));
      } else {
        if (!Object.class.getName().equals(notifInfo.userDataType)) println(" * <br>- user data type: {@link %s}.", formatType(notifInfo.userDataType));
      }
    }
    println(" * @since 6.2");
    println(" */");
    println("public class " + className + " extends " + AbstractMBeanForwarder.class.getSimpleName() + " {");
    println("  /**");
    println("   * Initialize this proxy.");
    println("   * @param jmx a {@link %s} instance.", JMXDriverConnectionWrapper.class.getSimpleName());
    println("   * @throws Exception if any error occurs..");
    println("   */");
    println("  public " + className + "(final " + JMXDriverConnectionWrapper.class.getSimpleName() + " jmx) throws Exception {");
    println("    super(jmx, \"" + mbeanName + "\");");
    println("  }");
  }

  @Override
  void finalizeImports(final ObjectName name, final MBeanInfo info) throws Exception {
    importedTypes.add(AbstractMBeanForwarder.class.getName());
    importedTypes.add(JMXDriverConnectionWrapper.class.getName());
    if ((nbAttributes > 0) || (nbOperations > 0)) {
      importedTypes.add(ResultsMap.class.getName());
      importedTypes.add(NodeSelector.class.getName());
    }
  }

  @Override
  public void visitAttribute(final AttributeInfo info, final MBeanAttributeInfo attribute) throws Exception {
    if (info.readable) {
      println();
      println("  /**");
      print("   * Get the value of the {@code %s} attribute for all selected nodes", attribute.getName());
      if (info.desc != null) print(" (%s)", info.desc);
      println(".");
      println("   * @param selector a {@link NodeSelector} instance.");
      println("   * @return a mapping of node uuids to {@link %s} instances.", info.wrappedType);
      println("   * @throws Exception if any error occurs.");
      println("   */");
      final String prefix = attribute.isIs() ? "is" : "get";
      println("  public ResultsMap<String, %s> %s%s(final NodeSelector selector) throws Exception {", info.wrappedType, prefix, attribute.getName());
      println("    return getAttribute(selector, \"%s\");", attribute.getName());
      println("  }");
    }
    if (info.writable) {
      println();
      println("  /**");
      print("   * Set the value of the {@code %s} attribute on all selected nodes", attribute.getName());
      if (info.desc != null) print(" (%s)", info.desc);
      println(".");
      println("   * @param selector a {@link NodeSelector} instance.");
      print("   * @param value the value to set, a ");
      if (info.wrappedType.equals(info.type)) println("{@link %s} instance.", info.wrappedType);
      else println("{@code %s}.", info.wrappedType);
      println("   * @return a mapping of node uuids to invocation results which may either be null or an exception.");
      println("   * @throws Exception if any error occurs.");
      println("   */");
      println("  public ResultsMap<String, Void> set%s(final NodeSelector selector, final %s value) throws Exception {", attribute.getName(), info.type);
      println("    return setAttribute(selector, \"%s\", value);", attribute.getName());
      println("  }");
    }
  }

  @Override
  void visitOperation (final OperationInfo info, final MBeanOperationInfo operation) throws Exception {
    println();
    println("  /**");
    print("   * Invoke the {@code %s} operation for all selected nodes", operation.getName());
    if (info.desc != null) print(" (%s)", info.desc);
    println(".");
    println("   * @param selector a {@link NodeSelector} instance.");
    for (int i=0; i<info.params.length; i++) {
      final String type = info.paramTypes[i], wrappedType = wrapperType(type);
      print("   * @param %s a ", info.paramNames[i]);
      if (wrappedType.equals(type)) println("{@link %s} instance.", wrappedType);
      else println("{@code %s}.", wrappedType);
    }
    println("   * @return a mapping of node uuids to objects that wrap either %s or an exeption.", ("Void".equals(info.wrappedReturnType)) ? "{@code null}" : "a {@link " + info.wrappedReturnType + "}");
    println("   * @throws Exception if any error occurs.");
    println("   */");

    print("  public ResultsMap<String, %s> %s(final NodeSelector selector", wrapperType(info.returnType), operation.getName());
    for (int i=0; i<info.params.length; i++) print(", final %s %s", info.genericTypes[i], info.paramNames[i]);
    println(") throws Exception {");
    if (info.params.length > 0) {
      final StringBuilder sb1 = new StringBuilder(), sb2 = new StringBuilder();
      for (int i=0; i<info.params.length; i++) {
        if (i > 0) {
          sb1.append(", ");
          sb2.append(", ");
        }
        sb1.append(info.paramNames[i]);
        sb2.append(info.paramTypes[i] + ".class.getName()");
      }
      println("    return invoke(selector, \"%s\", new Object[] { %s }, new String[] { %s });", operation.getName(), sb1, sb2);
    } else {
      println("    return invoke(selector, \"%s\");", operation.getName());
    }
    println("  }");
  }

  /**
   * Entry point.
   * @param args not used.
   */
  public static void main(final String[] args) {
    final String[] filteredMBeans = { "JMXNioServerMBean", "PeerDriverMBean", "ServerDebugMBean", "JmxLogger", "PersistedJobsManagerMBean", "NodeDebugMBean" , "NodeForwardingMBean" };
    final String[] filteredElements = { "addNotificationListener", "removeNotificationListener", "NotificationInfo" };
    final List<DriverConnectionInfo> remotes = Arrays.asList(new DriverConnectionInfo("node", "localhost", 12001));
    try {
      final MBeanFilter mbeanFilter = info -> {
        final String inf = (String) info.getDescriptor().getFieldValue("interfaceClassName");
        return !StringUtils.isOneOf(inf.substring(inf.lastIndexOf('.') + 1), false, filteredMBeans);
      };
      for (final DriverConnectionInfo remote: remotes) {
        MBeanInfoExplorer.visit(new NodeForwardingProxyGenerator(), remote, mbeanFilter, feature -> !StringUtils.isOneOf(feature.getName(), false, filteredElements));
      }
    } catch (final Exception e) {
      e.printStackTrace();
    }
  }
}
