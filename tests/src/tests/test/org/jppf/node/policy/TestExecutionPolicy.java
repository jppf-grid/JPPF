/*
 * JPPF.
 * Copyright (C) 2005-2016 JPPF Team.
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

import org.jppf.client.JPPFJob;
import org.jppf.management.JPPFSystemInformation;
import org.jppf.node.policy.*;
import org.jppf.utils.*;
import org.jppf.utils.stats.*;
import org.junit.*;

import test.org.jppf.test.setup.BaseTest;
import test.org.jppf.test.setup.common.*;

/**
 * Test for functions commons to all execution policies.
 * @author Laurent Cohen
 */
public class TestExecutionPolicy extends BaseTest {
  /** used for testing logical policies */
  private final ExecutionPolicy truePolicy1 = new Equal("int.1", 1);
  /** used for testing logical policies */
  private final ExecutionPolicy truePolicy2 = new Equal("int.2", 2);
  /** used for testing logical policies */
  private final ExecutionPolicy truePolicy3 = new Equal("int.3", 3);
  /** used for testing logical policies */
  private final ExecutionPolicy falsePolicy1 = new Equal("int.1", 2);
  /** used for testing logical policies */
  private final ExecutionPolicy falsePolicy2 = new Equal("int.2", 4);
  /** used for testing logical policies */
  private final ExecutionPolicy falsePolicy3 = new Equal("int.3", 6);
  /** */
  private static JPPFSystemInformation systemInfo;

  /**
   * 
   * @throws Exception if any error occurs.
   */
  @BeforeClass
  public static void setup() throws Exception {
    systemInfo = new JPPFSystemInformation("test", true, false);
    TypedProperties test = new TypedProperties()
      .setInt("int.1", 1)
      .setInt("int.2", 2)
      .setInt("int.3", 3)
      .setString("string.1", "string1")
      .setString("string.2", "string2")
      .setString("string.3", "string3")
      .setString("string.4a", "string4")
      .setString("string.4b", "stRIng4")
      .setBoolean("boolean.1", true)
      .setBoolean("boolean.2", false);
    systemInfo.addProperties("test", test);
  }

  /**
   * @throws Exception if any error occurs.
   */
  @Test(timeout=5000)
  public void testAnd() throws Exception {
    checkPolicy(truePolicy1.and(truePolicy2).and(truePolicy3), true);
    checkPolicy(truePolicy1.and(truePolicy2).and(falsePolicy3), false);
  }

  /**
   * @throws Exception if any error occurs.
   */
  @Test(timeout=5000)
  public void testOr() throws Exception {
    checkPolicy(truePolicy1.or(truePolicy2).or(truePolicy3), true);
    checkPolicy(truePolicy1.or(truePolicy2).or(falsePolicy3), true);
    checkPolicy(truePolicy1.or(falsePolicy2).or(falsePolicy3), true);
    checkPolicy(falsePolicy1.or(falsePolicy2).or(falsePolicy3), false);
  }

  /**
   * @throws Exception if any error occurs.
   */
  @Test(timeout=5000)
  public void testXor() throws Exception {
    checkPolicy(truePolicy1.xor(truePolicy2), false);
    checkPolicy(truePolicy1.xor(falsePolicy2), true);
    checkPolicy(falsePolicy1.xor(truePolicy2), true);
    checkPolicy(falsePolicy1.xor(falsePolicy2), false);
  }

  /**
   * @throws Exception if any error occurs.
   */
  @Test(timeout=5000)
  public void testNot() throws Exception {
    checkPolicy(truePolicy1.not(), false);
    checkPolicy(falsePolicy1.not(), true);
    checkPolicy(ExecutionPolicy.Not(truePolicy2), false);
    checkPolicy(ExecutionPolicy.Not(falsePolicy2), true);
  }

  /**
   * @throws Exception if any error occurs.
   */
  @Test(timeout=5000)
  public void testEqualString() throws Exception {
    checkPolicy(new Equal("string.4a", true, "string4"), true);
    checkPolicy(new Equal("string.4a", true, "STRING4"), true);
    checkPolicy(new Equal("string.4a", false, "STRING4"), false);
    checkPolicy(new Equal("string.4a", true, "string51"), false);
    checkPolicy(new Equal("string.4b", true, "string4"), true);
    checkPolicy(new Equal("string.4b", false, "string4"), false);
    checkPolicy(new Equal("string.4b", false, "stRIng4"), true);
    checkPolicy(new Equal("string.4b", true, "string51"), false);
  }

  /**
   * @throws Exception if any error occurs.
   */
  @Test(timeout=5000)
  public void testEqualBoolean() throws Exception {
    checkPolicy(new Equal("boolean.1", true), true);
    checkPolicy(new Equal("boolean.1", false), false);
    checkPolicy(new Equal("boolean.2", true), false);
    checkPolicy(new Equal("boolean.2", false), true);
  }

  /**
   * @throws Exception if any error occurs.
   */
  @Test(timeout=5000)
  public void testEqualNumeric() throws Exception {
    checkPolicy(new Equal("int.1", 1), true);
    checkPolicy(new Equal("int.1", 2), false);
    checkPolicy(new Equal("int.1", 1L), true);
    checkPolicy(new Equal("int.1", 2L), false);
    checkPolicy(new Equal("int.1", 1f), true);
    checkPolicy(new Equal("int.1", 2f), false);
    checkPolicy(new Equal("int.1", 1d), true);
    checkPolicy(new Equal("int.1", 2d), false);
  }

  /**
   * @throws Exception if any error occurs.
   */
  @Test(timeout=5000)
  public void testBetweenII() throws Exception {
    checkPolicy(new BetweenII("int.3", 0, 6), true);
    checkPolicy(new BetweenII("int.3", 3, 6), true);
    checkPolicy(new BetweenII("int.3", 0, 3), true);
    checkPolicy(new BetweenII("int.3", 4, 6), false);
  }

  /**
   * @throws Exception if any error occurs.
   */
  @Test(timeout=5000)
  public void testBetweenIE() throws Exception {
    checkPolicy(new BetweenIE("int.3", 0, 6), true);
    checkPolicy(new BetweenIE("int.3", 3, 6), true);
    checkPolicy(new BetweenIE("int.3", 0, 3), false);
    checkPolicy(new BetweenIE("int.3", 4, 6), false);
  }

  /**
   * @throws Exception if any error occurs.
   */
  @Test(timeout=5000)
  public void testBetweenEI() throws Exception {
    checkPolicy(new BetweenEI("int.3", 0, 6), true);
    checkPolicy(new BetweenEI("int.3", 3, 6), false);
    checkPolicy(new BetweenEI("int.3", 0, 3), true);
    checkPolicy(new BetweenEI("int.3", 4, 6), false);
  }

  /**
   * @throws Exception if any error occurs.
   */
  @Test(timeout=5000)
  public void testBetweenEE() throws Exception {
    checkPolicy(new BetweenEE("int.3", 0, 6), true);
    checkPolicy(new BetweenEE("int.3", 3, 6), false);
    checkPolicy(new BetweenEE("int.3", 0, 3), false);
    checkPolicy(new BetweenEE("int.3", 4, 6), false);
  }

  /**
   * @throws Exception if any error occurs.
   */
  @Test(timeout=5000)
  public void testAtLeast() throws Exception {
    checkPolicy(new AtLeast("int.2", 1), true);
    checkPolicy(new AtLeast("int.2", 2), true);
    checkPolicy(new AtLeast("int.2", 3), false);
  }

  /**
   * @throws Exception if any error occurs.
   */
  @Test(timeout=5000)
  public void testAtMost() throws Exception {
    checkPolicy(new AtMost("int.2", 3), true);
    checkPolicy(new AtMost("int.2", 2), true);
    checkPolicy(new AtMost("int.2", 1), false);
  }

  /**
   * @throws Exception if any error occurs.
   */
  @Test(timeout=5000)
  public void testMoreThan() throws Exception {
    checkPolicy(new MoreThan("int.2", 1), true);
    checkPolicy(new MoreThan("int.2", 2), false);
    checkPolicy(new MoreThan("int.2", 3), false);
  }

  /**
   * @throws Exception if any error occurs.
   */
  @Test(timeout=5000)
  public void testLessThan() throws Exception {
    checkPolicy(new LessThan("int.2", 3), true);
    checkPolicy(new LessThan("int.2", 2), false);
    checkPolicy(new LessThan("int.2", 1), false);
  }

  /**
   * @throws Exception if any error occurs.
   */
  @Test(timeout=5000)
  public void testContains() throws Exception {
    checkPolicy(new Contains("string.4b", true, "ring"), true);
    checkPolicy(new Contains("string.4a", false, "ring"), true);
    checkPolicy(new Contains("string.4b", false, "ring"), false);
    checkPolicy(new Contains("string.4b", false, "RIng"), true);
  }

  /**
   * @throws Exception if any error occurs.
   */
  @Test(timeout=5000)
  public void testOneOfString() throws Exception {
    checkPolicy(new OneOf("string.4a", true, "1", "string4", "3"), true);
    checkPolicy(new OneOf("string.4a", true, "1", "stRING4", "3"), true);
    checkPolicy(new OneOf("string.4a", true, "1", "4", "3"), false);
    checkPolicy(new OneOf("string.4a", false, "1", "stRING4", "3"), false);
  }

  /**
   * @throws Exception if any error occurs.
   */
  @Test(timeout=5000)
  public void testOneOfNumeric() throws Exception {
    checkPolicy(new OneOf("int.2", 0, 1, 2, 4), true);
    checkPolicy(new OneOf("int.2", 0, 1, 2L, 4), true);
    checkPolicy(new OneOf("int.2", 0, 1, 2f, 4), true);
    checkPolicy(new OneOf("int.2", 0, 1, 2d, 4), true);
    checkPolicy(new OneOf("int.2", 0, 1, 3d, 4), false);
  }

  /**
   * @throws Exception if any error occurs.
   */
  @Test(timeout=5000)
  public void testRegEx() throws Exception {
    checkPolicy(new RegExp("string.1", "s.*ng.?"), true);
    checkPolicy(new RegExp("string.1", "s.*4ng"), false);
  }

  /**
   * Check that the specified execution policy's evaluation returns the expected result.
   * @param policy the policy to evaluate.
   * @param expected the policy's expected return value.
   * @throws Exception if any error occurs.
   */
  private void checkPolicy(final ExecutionPolicy policy, final boolean expected) throws Exception {
    assertEquals(expected, policy.accepts(systemInfo));
    String s1 = policy.toString();
    StringBuilder sb = new StringBuilder("<jppf:ExecutionPolicy xmlns:jppf='http://www.jppf.org/schemas/ExecutionPolicy.xsd'>\n")
      .append(s1).append("</jppf:ExecutionPolicy>\n");
    ExecutionPolicy parsed = PolicyParser.parsePolicy(sb.toString());
    assertEquals(expected, parsed.accepts(systemInfo));
    assertEquals(s1, parsed.toString());
  }

  /**
   * Test that the context for an execution policy is properly set for all the elements in the policy graph.
   * @throws Exception if any error occurs.
   */
  @Test(timeout=5000)
  public void testMatches() throws Exception {
    JPPFSystemInformation info = new JPPFSystemInformation(JPPFUuid.normalUUID(), false, false);
    info.getRuntime().setString("ipv4.addresses", "localhost|192.168.1.14");
    TestCustomPolicy tcp = new TestCustomPolicy();
    ExecutionPolicy policy = new Contains("jppf.uuid", true, "AB").and(tcp);
    JPPFJob job = BaseTestHelper.createJob(ReflectionUtils.getCurrentClassAndMethod(), true, false, 1, LifeCycleTask.class, 0L);
    JPPFStatistics stats = JPPFStatisticsHelper.createServerStatistics();
    policy.setContext(job.getSLA(), job.getClientSLA(), job.getMetadata(), 2, stats);
    PolicyContext ctx = tcp.getContext();
    assertNotNull(ctx);
    assertEquals(job.getSLA(), ctx.getSLA());
    assertEquals(job.getClientSLA(), ctx.getClientSLA());
    assertEquals(job.getMetadata(), ctx.getMetadata());
    assertEquals(2, ctx.getJobDispatches());
    assertEquals(stats, ctx.getStats());
  }

  /**
   * 
   */
  public static class TestCustomPolicy extends CustomPolicy {
    @Override
    public boolean accepts(final PropertiesCollection<String> info) {
      return false;
    }
  }
}
