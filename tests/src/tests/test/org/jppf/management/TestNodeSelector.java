/*
 * JPPF.
 * Copyright (C) 2005-2017 JPPF Team.
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

package test.org.jppf.management;

import static org.junit.Assert.*;

import java.util.*;

import org.jppf.management.*;
import org.jppf.node.policy.OneOf;
import org.jppf.serialization.ObjectSerializer;
import org.jppf.utils.*;
import org.junit.Test;

import test.org.jppf.test.setup.BaseTest;

/**
 * Unit tests for the node selectors.
 * @author Laurent Cohen
 */
public class TestNodeSelector extends BaseTest {
  /**
   * @throws Exception if any error occurs
   */
  @Test(timeout = 10000)
  public void testAllNodesSelector() throws Exception {
    List<JPPFManagementInfo> nodes = createNodes(3);
    NodeSelector selector = new AllNodesSelector();
    checkSerialization(selector);
    List<JPPFManagementInfo> filtered = filter(nodes, selector);
    assertNotNull(filtered);
    assertEquals(nodes, filtered);
  }

  /**
   * @throws Exception if any error occurs
   */
  @Test(timeout = 10000)
  public void testExecutionPolicySelector() throws Exception {
    List<JPPFManagementInfo> nodes = createNodes(6);
    // select nodes whose uuid ends with an even number
    NodeSelector selector = new ExecutionPolicySelector(new OneOf("jppf.uuid", true, "node2", "node4", "node6"));
    checkSerialization(selector);
    List<JPPFManagementInfo> filtered = filter(nodes, selector);
    assertNotNull(filtered);
    assertEquals(3, filtered.size());
    for (int i=1; i<=filtered.size(); i+=2) {
      assertTrue(filtered.contains(nodes.get(i)));
    }
  }

  /**
   * @throws Exception if any error occurs
   */
  @Test(timeout = 10000)
  public void testUuidSelector() throws Exception {
    List<JPPFManagementInfo> nodes = createNodes(6);
    // select nodes whose uuid ends with an even number
    NodeSelector selector = new UuidSelector("node2", "node4", "node6");
    checkSerialization(selector);
    List<JPPFManagementInfo> filtered = filter(nodes, selector);
    assertNotNull(filtered);
    assertEquals(3, filtered.size());
    for (int i=1; i<=filtered.size(); i+=2) {
      assertTrue(filtered.contains(nodes.get(i)));
    }
  }

  /**
   * @throws Exception if any error occurs
   */
  @Test(timeout = 10000)
  public void testScriptedNodeSelector() throws Exception {
    List<JPPFManagementInfo> nodes = createNodes(6);
    // select nodes whose uuid ends with an even number
    StringBuilder script = new StringBuilder()
      .append("var uuid = nodeInfo.getUuid();\n")
      .append("uuid.equals('node2') || uuid.equals('node4') || uuid.equals('node6');\n");
    NodeSelector selector = new ScriptedNodeSelector("javascript", script.toString());
    checkSerialization(selector);
    List<JPPFManagementInfo> filtered = filter(nodes, selector);
    assertNotNull(filtered);
    assertEquals(3, filtered.size());
    for (int i=1; i<=filtered.size(); i+=2) {
      assertTrue(filtered.contains(nodes.get(i)));
    }
  }

  /**
   * @throws Exception if any error occurs
   */
  @Test(timeout = 10000)
  public void testCustomNodeSelector() throws Exception {
    List<JPPFManagementInfo> nodes = createNodes(6);
    // select nodes whose uuid ends with an even number
    NodeSelector selector = new EvenNodeSelector();
    checkSerialization(selector);
    List<JPPFManagementInfo> filtered = filter(nodes, selector);
    assertNotNull(filtered);
    assertEquals(3, filtered.size());
    for (int i=1; i<=filtered.size(); i+=2) {
      assertTrue(filtered.contains(nodes.get(i)));
    }
  }

  /**
   * Filter the specified list of nodes according to the specified selector.
   * @param nodes the nodes to filter.
   * @param selector the selector to apply.
   * @return a list of {@link JPPFManagementInfo} instances.
   */
  private List<JPPFManagementInfo> filter(final List<JPPFManagementInfo> nodes, final NodeSelector selector) {
    List<JPPFManagementInfo> list = new ArrayList<>(nodes.size());
    for (JPPFManagementInfo node: nodes) {
      if (selector.accepts(node)) list.add(node);
    }
    return list;
  }

  /**
   * Create the specified number of node representations.
   * @param nbNodes the number of nodes to create.
   * @return a list of {@link JPPFManagementInfo} instances.
   */
  private List<JPPFManagementInfo> createNodes(final int nbNodes) {
    List<JPPFManagementInfo> list = new ArrayList<>(nbNodes);
    for (int i=1; i<=nbNodes; i++) list.add(createNodeInfo(i, JPPFManagementInfo.NODE|JPPFManagementInfo.MASTER));
    return list;
  }

  /**
   * Create a new test management info object.
   * @param index the index number, used to generate IP, host, port and uuid.
   * @param type the type of node, a bit-wise comination of the constants defined in {@link JPPFManagementInfo}.
   * @return a new {@link JPPFManagementInfo} instance.
   */
  private JPPFManagementInfo createNodeInfo(final int index, final int type) {
    String uuid = "node" + index;
    HostIP hostIP = new HostIP("www" + index + ".jppf.org", "1.1.1." + index);
    int port = 12000 + index;
    JPPFManagementInfo info = new JPPFManagementInfo(hostIP, port, uuid, type, false);
    info.setSystemInfo(new MySystemInfo(uuid));
    return info;
  }

  /**
   * Check that the specified node selector can be serialized and deserialized.
   * @param selector the selector to check.
   * @throws Exception if any error occurs.
   */
  private void checkSerialization(final NodeSelector selector) throws Exception {
    ObjectSerializer ser = new ObjectSerializerImpl();
    JPPFBuffer buf = ser.serialize(selector);
    ser.deserialize(buf);
  }

  /** */
  public static class MySystemInfo extends JPPFSystemInformation {
    /**
     * @param uuid the uuid to assign.
     */
    public MySystemInfo(final String uuid) {
      super(uuid, false, false);
      TypedProperties test = new TypedProperties();
      for (int i=1; i<=5; i++) test.setString("prop." + i, "value " + i);
      this.addProperties("test", test);
    }
  }

  /** Selects only the nodes whose IP address ends with an even number */
  public static class EvenNodeSelector implements NodeSelector {
    @Override
    public boolean accepts(final JPPFManagementInfo nodeInfo) {
      String ip = nodeInfo.getIpAddress();
      int idx = ip.lastIndexOf('.');
      int n = -1;
      try {
        n = Integer.valueOf(ip.substring(idx + 1));
      } catch (@SuppressWarnings("unused") Exception ignore) {
      }
      return n % 2 == 0;
    }
  }
}
