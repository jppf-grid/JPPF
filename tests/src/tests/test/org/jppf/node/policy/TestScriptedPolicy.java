/*
 * JPPF.
 * Copyright (C) 2005-2013 JPPF Team.
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
import org.jppf.client.JPPFJob;
import org.jppf.node.policy.*;
import org.jppf.utils.FileUtils;
import org.jppf.utils.stats.*;
import org.junit.Test;

/**
 * Unit tests for the <code>Range</code> class.
 * @author Laurent Cohen
 */
public class TestScriptedPolicy {
  /**
   * A valid XML representation of a scripted policy, whose {@code accepts()} method returns {@code true}.
   */
  private String validTrueXML = new StringBuilder()
    .append("<jppf:ExecutionPolicy xmlns:jppf='http://www.jppf.org/schemas/ExecutionPolicy.xsd'>\n")
    .append("  <Script language='groovy'>return true</Script>\n")
    .append("</jppf:ExecutionPolicy>\n").toString();

  /**
   * A valid XML representation of a scripted policy, whose {@code accepts()} method returns {@code false}.
   */
  private String validFalseXML = new StringBuilder()
    .append("<jppf:ExecutionPolicy xmlns:jppf='http://www.jppf.org/schemas/ExecutionPolicy.xsd'>\n")
    .append("  <Script language='groovy'>return false</Script>\n")
    .append("</jppf:ExecutionPolicy>\n").toString();

  /**
   * An invalid XML representation of a scripted policy.<br/>
   * 'unknownattribute' attribute is not permitted, and 'someElement' element isn't permitted.
   */
  private String invalidXML = new StringBuilder()
    .append("<jppf:ExecutionPolicy xmlns:jppf='http://www.jppf.org/schemas/ExecutionPolicy.xsd'>\n")
    .append("  <Script language='groovy' unknownattribute='whatever'>return true\n")
    .append("    <someElement>some text</someElement>\n")
    .append("  </Script>\n")
    .append("</jppf:ExecutionPolicy>\n").toString();

  /**
   * Test that an XML representation of a scripted policy is valid according to the ExecutionPolicy schema.
   * @throws Exception if any error occurs
   */
  @Test(timeout=5000)
  public void testValidXML() throws Exception {
    PolicyParser.validatePolicy(validTrueXML);
  }

  /**
   * Test that an XML representation of a scripted policy is valid according to the ExecutionPolicy.xsd schema.
   * @throws Exception if any error occurs
   */
  @Test(timeout=5000)
  public void testInvalidXML() throws Exception {
    try {
      PolicyParser.validatePolicy(invalidXML);
      throw new IllegalStateException("the policy is invalid but passes the validation");
    } catch(Exception e) {
      assertTrue("e = " + e, e instanceof JPPFException);
      //e.printStackTrace();
    }
  }

  /**
   * Test that the given scripted policy return {@code true} when its {@code accepts()} method is called.
   * @throws Exception if any error occurs
   */
  @Test(timeout=5000)
  public void testSimpleTruePolicy() throws Exception {
    ExecutionPolicy p = PolicyParser.parsePolicy(validTrueXML);
    assertTrue(p instanceof ScriptedPolicy);
    assertTrue(p.accepts(null));
  }

  /**
   * Test that the given scripted policy return {@code false} when its {@code accepts()} method is called.
   * @throws Exception if any error occurs
   */
  @Test(timeout=5000)
  public void testSimpleFalsePolicy() throws Exception {
    ExecutionPolicy p = PolicyParser.parsePolicy(validFalseXML);
    assertTrue(p instanceof ScriptedPolicy);
    assertFalse(p.accepts(null));
  }

  /**
   * Test that the given scripted policy return {@code true} when its {@code accepts()} method is called.
   * @throws Exception if any error occurs
   */
  @Test(timeout=5000)
  public void testComplexPolicy() throws Exception {
    String script = FileUtils.readTextFile(getClass().getPackage().getName().replace('.', '/') + "/TestScriptedPolicy.groovy");
    ScriptedPolicy p = new ScriptedPolicy("groovy", script);
    JPPFStatistics stats = new JPPFStatistics();
    JPPFSnapshot sn = stats.createSnapshot(true, "nodes");
    sn.addValues(10, 10);
    JPPFJob job = new JPPFJob();
    job.getSLA().setPriority(7);
    p.setVariables(job.getSLA(), job.getClientSLA(), null, 3, stats);
    assertTrue(p.accepts(null));
    p.setVariables(job.getSLA(), job.getClientSLA(), null, 7, stats);
    assertFalse(p.accepts(null));
  }
}
