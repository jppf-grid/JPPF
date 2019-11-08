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
import java.util.concurrent.atomic.AtomicInteger;

import javax.management.*;

import org.jppf.discovery.DriverConnectionInfo;
import org.jppf.management.NodeSelector;
import org.jppf.utils.*;

/**
 * Generate the source code for static proxies forwzrding requests to node MBeans through a driver.
 * @author Laurent Cohen
 */
public class NodeForwardingProxyTestGenerator extends AbstractForwardingCodeGenerator<NodeForwardingProxyTestGenerator> {
  /**
   * Mapping of type names to Java expressions for a default value of each type. 
   */
  final static Map<String, String> DEFAULT_VALUES = new HashMap<>();
  static {
    DEFAULT_VALUES.put("boolean", "false");
    DEFAULT_VALUES.put("Boolean", "Boolean.FALSE");
    DEFAULT_VALUES.put("char", "(char) 0");
    DEFAULT_VALUES.put("Character", "Character.valueOf((char) 0)");
    DEFAULT_VALUES.put("byte", "(byte) 0");
    DEFAULT_VALUES.put("Byte", "Byte.valueOf((byte) 0)");
    DEFAULT_VALUES.put("short", "(short) 0");
    DEFAULT_VALUES.put("Short", "Short.valueOf((short) 0)");
    DEFAULT_VALUES.put("int", "0");
    DEFAULT_VALUES.put("Integer", "Integer.valueOf(0)");
    DEFAULT_VALUES.put("long", "0L");
    DEFAULT_VALUES.put("Long", "Long.valueOf(0L)");
    DEFAULT_VALUES.put("float", "0f");
    DEFAULT_VALUES.put("Float", "Float.valueOf(0f)");
    DEFAULT_VALUES.put("double", "0d");
    DEFAULT_VALUES.put("Double", "Double.valueOf(0d)");
    DEFAULT_VALUES.put("String", "\"some_string\"");
    DEFAULT_VALUES.put("Void", "null");
    DEFAULT_VALUES.put("DelegationModel", "DelegationModel.PARENT_FIRST");
  }
  /**
   * Count of each method name, in the case an mbean method is overloaded (i.e. sevral versions with different signatures).
   */
  final Map<String, AtomicInteger> methodCounts = new HashMap<>();

  /**
   * Initialize this visitor.
   */
  public NodeForwardingProxyTestGenerator() {
    super("test.org.jppf.management.forwarding.generated", "../tests/src/tests");
    this.arraySuffix = "[]";
    this.classNamePrefix = "Test";
  }

  @Override
  void startMBean(final InterfaceInfo info, final ObjectName mbeanName, final MBeanInfo mbean) throws Exception {
    methodCounts.clear();
    if ((nbAttributes <= 0) && (nbOperations <= 0)) return;
    importedTypes.add(info.interfaceName);
    println("/**");
    println(" * Test of the forwarding proxy for the {@link " + info.interfaceSimpleName + "} MBean.");
    final String desc = (String) info.descriptor.getFieldValue(MBeanInfoExplorer.DESCRIPTION_FIELD);
    if (desc != null) println(" * MBean description: " + desc  + (desc.endsWith(".") ? "" : "."));
    println(" * @since 6.2");
    println(" */");
    println("public class " + className  + " extends AbstractTestForwarderProxy {");
    println("  /**"); 
    println("   * Reference to the forwarding proxy."); 
    println("   */"); 
    println("  private %s proxy;", info.interfaceSimpleName + classNameSuffix); 
    println(); 
    println("  /**"); 
    println("   * Initial setup.");
    println("   * @throws Exception if any error occurs."); 
    println("   */"); 
    println("  @Before"); 
    println("  public void setupInstance() throws Exception {"); 
    println("    if (proxy == null) proxy = (%s) getForwardingProxy(%s.class);", info.interfaceSimpleName + classNameSuffix, info.interfaceSimpleName); 
    println("  }"); 
  }

  @Override
  public void endMBean(final ObjectName name, final MBeanInfo info) throws Exception {
    if ((nbAttributes <= 0) && (nbOperations <= 0)) return;
    super.endMBean(name, info);
  }

  @Override
  void finalizeImports(final ObjectName name, final MBeanInfo info) throws Exception {
    importedTypes.add("org.jppf.management.forwarding.generated." + interfaceInfo.interfaceSimpleName + classNameSuffix);
    if ((nbAttributes > 0) || (nbOperations > 0)) {
      importedTypes.add("org.junit.Test");
      importedTypes.add("org.junit.Before");
      importedTypes.add("test.org.jppf.management.forwarding.AbstractTestForwarderProxy");
      importedTypes.add(ResultsMap.class.getName());
      importedTypes.add(NodeSelector.class.getName());
    }
  }

  @Override
  public void visitAttribute(final AttributeInfo info, final MBeanAttributeInfo attribute) throws Exception {
    if (info.readable) {
      println();
      println("  /**");
      println("   * Test getting the value of the {@code %s} attribute for all selected nodes.", attribute.getName());
      println("   * @throws Exception if any error occurs.");
      println("   */");
      println("  @Test");
      final String prefix = attribute.isIs() ? "is" : "get";
      println("  public void test%s%s() throws Exception {", prefix, capitalizeFirstChar(attribute.getName()));
      //println("  public ResultsMap<String, %s> %s%s(final NodeSelector selector) throws Exception {", info.wrappedType, prefix, attribute.getName());
      println("    final ResultsMap<String, %s> results = proxy.%s%s(NodeSelector.ALL_NODES);", info.wrappedType, prefix, attribute.getName());
      println("    checkResults(results, %s.class);", info.type);
      println("  }");
    }
    if (info.writable) {
      println();
      println("  /**");
      println("   * Test setting the value of the {@code %s} attribute on all selected nodes.", attribute.getName());
      println("   * @throws Exception if any error occurs.");
      println("   */");
      println("  @Test");
      println("  public void testSet%s() throws Exception {", attribute.getName());
      //println("  public ResultsMap<String, Void> set%s(final NodeSelector selector, final %s value) throws Exception {", attribute.getName(), info.type);
      println("    // nothing yet");
      println("    final ResultsMap<String, Void> results = proxy.set%s(NodeSelector.ALL_NODES, %s);", attribute.getName(), defaultValueExpression(info.type));
      println("    checkResults(results, %s.class);", info.type);
      println("  }");
    }
  }

  @Override
  void visitOperation(final OperationInfo info, final MBeanOperationInfo operation) throws Exception {
    final String name = operation.getName();
    AtomicInteger count = methodCounts.get(name);
    if (count == null) {
      count = new AtomicInteger(0);
      methodCounts.put(name, count);
    }
    final int n = count.incrementAndGet();
    println();
    println("  /**");
    println("   * Test invoking the {@code %s} operation for all selected nodes.", name);
    println("   * @throws Exception if any error occurs.");
    println("   */");
    println("  @Test");
    println("  public void test%s%s() throws Exception {", capitalizeFirstChar(name), (n == 1) ? "" : Integer.toString(n));
    print("    final ResultsMap<String, %s> results = proxy.%s(NodeSelector.ALL_NODES", info.wrappedReturnType, operation.getName());
    for (int i=0; i<info.params.length; i++) print(", %s", defaultValueExpression(info.genericTypes[i]));
    println(");");
    println("    checkResults(results, %s.class);", info.returnType);
    println("  }");
  }

  /**
   * Get a default value for the specified type.
   * @param className the name of the class.
   * @return a valid default value for the type.
   */
  protected static String defaultValueExpression(final String className) {
    final String res = DEFAULT_VALUES.get(className);
    return res == null ? "(" + className + ") null" : res;
  }

  /**
   * Entry point.
   * @param args not used.
   */
  public static void main(final String[] args) {
    final String[] filteredMBeans = { "JMXNioServerMBean", "PeerDriverMBean", "ServerDebugMBean", "JmxLogger", "PersistedJobsManagerMBean", "NodeDebugMBean" , "NodeForwardingMBean" };
    final String[] filteredElements = { "addNotificationListener", "removeNotificationListener", "NotificationInfo", "shutdown", "restart", "heapDump" };
    final List<DriverConnectionInfo> remotes = Arrays.asList(new DriverConnectionInfo("node", "localhost", 12001));
    try {
      final MBeanFilter mbeanFilter = info -> {
        final String inf = (String) info.getDescriptor().getFieldValue("interfaceClassName");
        return !StringUtils.isOneOf(inf.substring(inf.lastIndexOf('.') + 1), false, filteredMBeans);
      };
      for (final DriverConnectionInfo remote: remotes) {
        MBeanInfoExplorer.visit(new NodeForwardingProxyTestGenerator(), remote, mbeanFilter, feature -> !StringUtils.isOneOf(feature.getName(), false, filteredElements));
      }
    } catch (final Exception e) {
      e.printStackTrace();
    }
  }
}
