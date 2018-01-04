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

package test.org.jppf.node.policy;

import static org.junit.Assert.*;

import org.jppf.JPPFException;
import org.jppf.management.JPPFSystemInformation;
import org.jppf.node.policy.*;
import org.jppf.utils.JPPFUuid;
import org.junit.Test;

import test.org.jppf.test.setup.BaseTest;

/**
 * 
 * @author Laurent Cohen
 */
public class TestIsInIPv6Subnet extends BaseTest {
  /**
   * A valid XML representation of an {@code IsInIPv6Subnet} policy.
   */
  private String validXML = new StringBuilder()
  .append("<jppf:ExecutionPolicy xmlns:jppf='http://www.jppf.org/schemas/ExecutionPolicy.xsd'>\n")
  .append("  <IsInIPv6Subnet>\n")
  .append("    <Subnet>::1</Subnet>\n")
  .append("    <Subnet>::</Subnet>\n")
  .append("    <Subnet>::1/128</Subnet>\n")
  .append("  </IsInIPv6Subnet>\n")
  .append("</jppf:ExecutionPolicy>\n").toString();
  /**
   * An invalid XML representation of an {@code IsInIPv6Subnet} policy, which does not contain any subnet in its list.
   */
  private String invalidXML = new StringBuilder()
  .append("<jppf:ExecutionPolicy xmlns:jppf='http://www.jppf.org/schemas/ExecutionPolicy.xsd'>\n")
  .append("  <IsInIPv6Subnet>\n")
  .append("  </IsInIPv6Subnet>\n")
  .append("</jppf:ExecutionPolicy>\n").toString();

  /**
   * Test that an XML representation of an {@code IsInIPv6Subnet} policy is valid according to the ExecutionPolicy schema.
   * @throws Exception if any error occurs.
   */
  @Test(timeout=5000)
  public void testValidXML() throws Exception {
    PolicyParser.validatePolicy(validXML);
  }

  /**
   * Test that an XML representation of an {@code IsInIPv6Subnet} policy is valid according to the ExecutionPolicy.xsd schema.
   * @throws Exception if any error occurs.
   */
  @Test(timeout=5000)
  public void testInvalidXML() throws Exception {
    try {
      PolicyParser.validatePolicy(invalidXML);
      throw new IllegalStateException("the policy is invalid but passes the validation");
    } catch(final Exception e) {
      assertTrue("e = " + e, e instanceof JPPFException);
    }
  }

  /**
   * Test IP subnet matching.
   * @throws Exception if any error occurs.
   */
  @Test(timeout=5000)
  public void testMatches() throws Exception {
    final JPPFSystemInformation info = new JPPFSystemInformation(JPPFUuid.normalUUID(), false, false);
    info.getRuntime().setString("ipv6.addresses", "localhost|0:0:0:0:8888:8888:8888:8888");
    info.getJppf().setString("string.1", "0:0:0:0:8888");
    info.getJppf().setString("string.2", ":8888:8888:8888");
    info.getJppf().setString("string.3", "0:0:0:0:8888:8888:8888:");
    info.getJppf().setInt("int.8", 8);
    info.getJppf().setInt("int.8880", 8880);

    assertFalse(new IsInIPv6Subnet("::1/80").accepts(info));
    assertTrue(new IsInIPv6Subnet("::1/64").accepts(info));
    assertFalse(new IsInIPv6Subnet("0:aaaa-bbbb:0:0:0:0:0:0").accepts(info));
    assertTrue(new IsInIPv6Subnet("0:0:0:0:-:0-FFFF:7000-9000:400-A000").accepts(info));
    assertTrue(new IsInIPv6Subnet("$script{ '${string.1}' + '${string.2}'; }$").accepts(info));
    final ExecutionPolicy p = new IsInIPv6Subnet("$script{ '${string.3}' + (${int.8} + ${int.8880}); }$");
    assertTrue(p.accepts(info));
    final String xml = p.toXML();
    final ExecutionPolicy p2 = PolicyParser.parsePolicy(xml);
    assertEquals(xml, p2.toXML());
  }
}
