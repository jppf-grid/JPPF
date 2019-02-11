/*
 * JPPF.
 * Copyright (C) 2005-2018 JPPF Team.
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

import java.util.*;

import org.jppf.JPPFRuntimeException;
import org.jppf.client.JPPFJob;
import org.jppf.management.JPPFSystemInformation;
import org.jppf.node.policy.*;
import org.jppf.node.policy.ExecutionPolicy.*;
import org.jppf.utils.*;
import org.jppf.utils.stats.*;
import org.junit.Test;

import test.org.jppf.test.setup.common.*;

/**
 * Test for functions commons to all execution policies.
 * @author Laurent Cohen
 */
public class TestExecutionPolicy extends AbstractTestExecutionPolicy {
  /** @throws Exception if any error occurs. */
  @Test(timeout=5000)
  public void testAnd() throws Exception {
    checkPolicy(truePolicy1.and(truePolicy2).and(truePolicy3), true);
    checkPolicy(truePolicy1.and(truePolicy2).and(falsePolicy3), false);
  }

  /** @throws Exception if any error occurs. */
  @Test(timeout=5000)
  public void testOr() throws Exception {
    checkPolicy(truePolicy1.or(truePolicy2).or(truePolicy3), true);
    checkPolicy(truePolicy1.or(truePolicy2).or(falsePolicy3), true);
    checkPolicy(truePolicy1.or(falsePolicy2).or(falsePolicy3), true);
    checkPolicy(falsePolicy1.or(falsePolicy2).or(falsePolicy3), false);
  }

  /** @throws Exception if any error occurs. */
  @Test(timeout=5000)
  public void testXor() throws Exception {
    checkPolicy(truePolicy1.xor(truePolicy2), false);
    checkPolicy(truePolicy1.xor(falsePolicy2), true);
    checkPolicy(falsePolicy1.xor(truePolicy2), true);
    checkPolicy(falsePolicy1.xor(falsePolicy2), false);
  }

  /** @throws Exception if any error occurs. */
  @Test(timeout=5000)
  public void testNot() throws Exception {
    checkPolicy(truePolicy1.not(), false);
    checkPolicy(falsePolicy1.not(), true);
    checkPolicy(ExecutionPolicy.Not(truePolicy2), false);
    checkPolicy(ExecutionPolicy.Not(falsePolicy2), true);
  }

  /** @throws Exception if any error occurs. */
  @Test(timeout=5000)
  public void testEqualString() throws Exception {
    checkPolicy(new Equal("string.4a", true, "string4"), true);
    checkPolicy(new Equal("string.4a", false, "string4"), true);
    checkPolicy(new Equal("string.4a", true, "STRING4"), true);
    checkPolicy(new Equal("string.4a", false, "STRING4"), false);
    checkPolicy(new Equal("string.4a", true, "string51"), false);
    checkPolicy(new Equal("string.4b", true, "string4"), true);
    checkPolicy(new Equal("string.4b", false, "string4"), false);
    checkPolicy(new Equal("string.4b", true, "stRIng4"), true);
    checkPolicy(new Equal("string.4b", true, "string51"), false);
    checkPolicy(new Equal("$script{ '${string}' + 4; }$", true, "string4"), true);
    checkPolicy(new Equal("$script{ '${string}' + 4; }$", true, "${string.4a}"), true);
  }

  /** @throws Exception if any error occurs. */
  @Test(timeout=5000)
  public void testEqualBoolean() throws Exception {
    checkPolicy(new Equal("boolean.1", true), true);
    checkPolicy(new Equal("boolean.1", false), false);
    checkPolicy(new Equal("boolean.2", true), false);
    checkPolicy(new Equal("boolean.2", false), true);
    checkPolicy(new Equal(ValueType.BOOLEAN, "$script{ '${string.tr}' + '${string.ue}'; }$", "${boolean.1}"), true);
  }

  /** @throws Exception if any error occurs. */
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
    checkPolicy(new Equal(ValueType.NUMERIC, "$script{ ${int.1} + ${int.2}; }$", "${int.3}"), true);
  }

  /** @throws Exception if any error occurs. */
  @Test(timeout=5000)
  public void testBetweenII() throws Exception {
    checkPolicy(new BetweenII("int.3", 0, 6), true);
    checkPolicy(new BetweenII("int.3", 3, 6), true);
    checkPolicy(new BetweenII("int.3", 0, 3), true);
    checkPolicy(new BetweenII("int.3", 4, 6), false);
    checkPolicy(new BetweenII("int.3", "$script{ ${int.1} + ${int.2}; }$", 6), true);
    checkPolicy(new BetweenII("int.3", 0, "$script{ ${int.1} + ${int.2}; }$"), true);
    checkPolicy(new BetweenII("int.3", "$script{ ${int.1} + ${int.2}; }$", "$script{ ${int.10} / 2; }$"), true);
    checkPolicy(new BetweenII("int.3", "$script{ ${int.1} + ${int.3}; }$", "$script{ ${int.10} / 2; }$"), false);
  }

  /** @throws Exception if any error occurs. */
  @Test(timeout=5000)
  public void testBetweenIE() throws Exception {
    checkPolicy(new BetweenIE("int.3", 0, 6), true);
    checkPolicy(new BetweenIE("int.3", 3, 6), true);
    checkPolicy(new BetweenIE("int.3", 0, 3), false);
    checkPolicy(new BetweenIE("int.3", 4, 6), false);
    checkPolicy(new BetweenIE("int.3", "$script{ ${int.1} + ${int.2}; }$", 6), true);
    checkPolicy(new BetweenIE("int.3", 0, "$script{ ${int.1} + ${int.2}; }$"), false);
    checkPolicy(new BetweenIE("int.3", "$script{ ${int.1} + ${int.2}; }$", "$script{ ${int.10} / 2; }$"), true);
    checkPolicy(new BetweenIE("int.3", "$script{ ${int.1} + ${int.3}; }$", "$script{ ${int.10} / 2; }$"), false);
  }

  /** @throws Exception if any error occurs. */
  @Test(timeout=5000)
  public void testBetweenEI() throws Exception {
    checkPolicy(new BetweenEI("int.3", 0, 6), true);
    checkPolicy(new BetweenEI("int.3", 3, 6), false);
    checkPolicy(new BetweenEI("int.3", 0, 3), true);
    checkPolicy(new BetweenEI("int.3", 4, 6), false);
    checkPolicy(new BetweenEI("int.3", "$script{ ${int.1} + ${int.2}; }$", 6), false);
    checkPolicy(new BetweenEI("int.3", 0, "$script{ ${int.1} + ${int.2}; }$"), true);
    checkPolicy(new BetweenEI("int.3", "$script{ ${int.1} + 1; }$", "$script{ ${int.10} / 2; }$"), true);
    checkPolicy(new BetweenEI("int.3", "$script{ ${int.1} + ${int.3}; }$", "$script{ ${int.10} / 2; }$"), false);
  }

  /** @throws Exception if any error occurs. */
  @Test(timeout=5000)
  public void testBetweenEE() throws Exception {
    checkPolicy(new BetweenEE("int.3", 0, 6), true);
    checkPolicy(new BetweenEE("int.3", 3, 6), false);
    checkPolicy(new BetweenEE("int.3", 0, 3), false);
    checkPolicy(new BetweenEE("int.3", 4, 6), false);
    checkPolicy(new BetweenEE("int.3", "$script{ ${int.1} + ${int.2}; }$", 6), false);
    checkPolicy(new BetweenEE("int.3", 0, "$script{ ${int.1} + ${int.2}; }$"), false);
    checkPolicy(new BetweenEE("int.3", "$script{ ${int.1} + 1; }$", "$script{ ${int.10} / 2; }$"), true);
    checkPolicy(new BetweenEE("int.3", "$script{ ${int.1} + ${int.3}; }$", "$script{ ${int.10} / 2; }$"), false);
  }

  /** @throws Exception if any error occurs. */
  @Test(timeout=5000)
  public void testAtLeast() throws Exception {
    checkPolicy(new AtLeast("int.2", 1), true);
    checkPolicy(new AtLeast("int.2", 2), true);
    checkPolicy(new AtLeast("int.2", 3), false);
    checkPolicy(new AtLeast("int.3", "$script{ ${int.1} + ${int.2}; }$"), true);
    checkPolicy(new AtLeast("int.10", "$script{ ${int.3} * 2 + ${int.1}; }$"), true);
    checkPolicy(new AtLeast("int.3", "$script{ ${int.1} + ${int.2} + 4; }$"), false);
  }

  /** @throws Exception if any error occurs. */
  @Test(timeout=5000)
  public void testAtMost() throws Exception {
    checkPolicy(new AtMost("int.2", 3), true);
    checkPolicy(new AtMost("int.2", 2), true);
    checkPolicy(new AtMost("int.2", 1), false);
    checkPolicy(new AtMost("int.3", "$script{ ${int.1} + ${int.2}; }$"), true);
    checkPolicy(new AtMost("int.3", "$script{ ${int.1} + ${int.2} + 4; }$"), true);
    checkPolicy(new AtMost("int.3", "$script{ ${int.1} + 1; }$"), false);
  }

  /** @throws Exception if any error occurs. */
  @Test(timeout=5000)
  public void testMoreThan() throws Exception {
    checkPolicy(new MoreThan("int.2", 1), true);
    checkPolicy(new MoreThan("int.2", 2), false);
    checkPolicy(new MoreThan("int.2", 3), false);
    checkPolicy(new MoreThan("int.3", "$script{ ${int.1} + ${int.2}; }$"), false);
    checkPolicy(new MoreThan("int.3", "$script{ ${int.1} + ${int.2} + 4; }$"), false);
    checkPolicy(new MoreThan("int.3", "$script{ ${int.1} + 1; }$"), true);
  }

  /** @throws Exception if any error occurs. */
  @Test(timeout=5000)
  public void testLessThan() throws Exception {
    checkPolicy(new LessThan("int.2", 3), true);
    checkPolicy(new LessThan("int.2", 2), false);
    checkPolicy(new LessThan("int.2", 1), false);
    checkPolicy(new LessThan("int.10", "$script{ ${int.3} * 4; }$"), true);
    checkPolicy(new LessThan("int.10", "$script{ ${int.1} + ${int.2} + 4; }$"), false);
    checkPolicy(new LessThan("int.3", "$script{ ${int.1} + ${int.2}; }$"), false);
  }

  /** @throws Exception if any error occurs. */
  @Test(timeout=5000)
  public void testContains() throws Exception {
    checkPolicy(new Contains("string.4b", true, "ring"), true);
    checkPolicy(new Contains("string.4a", false, "ring"), true);
    checkPolicy(new Contains("string.4b", false, "ring"), false);
    checkPolicy(new Contains("string.4b", false, "RIng"), true);
    checkPolicy(new Contains("string.1", false, "$script{ '${string.1a}' + '${string.1b}'; }$"), true);
    checkPolicy(new Contains("string.1", false, "$script{ 'st' + 'ri'; }$"), true);
    checkPolicy(new Contains("string.1", false, "$script{ 'ST' + 'ri'; }$"), false);
    checkPolicy(new Contains("string.1", true, "$script{ 'ST' + 'ri'; }$"), true);
  }

  /** @throws Exception if any error occurs. */
  @Test(timeout=5000)
  public void testOneOfString() throws Exception {
    checkPolicy(new OneOf("string.4a", true, "1", "string4", "3"), true);
    checkPolicy(new OneOf("string.4a", true, "1", "stRING4", "3"), true);
    checkPolicy(new OneOf("string.4a", true, "1", "4", "3"), false);
    checkPolicy(new OneOf("string.4a", false, "1", "stRING4", "3"), false);
    checkPolicy(new OneOf("string.5", true, "${string.1}", "string1-string2", "4", "3"), true);
    checkPolicy(new OneOf("string.5", false, "${string.1}", "string1-STRING2", "4", "3"), false);
    checkPolicy(new OneOf("string.5", true, "${string.1}", "string1-STRING2", "4", "3"), true);
    checkPolicy(new OneOf("string.5", true, "1", "${string.1}-${string.2}", "3"), true);
    checkPolicy(new OneOf("string.5", true, "1", "$script{ '${string.1}' + '-' + '${string.2}'; }$", "3"), true);
  }

  /** @throws Exception if any error occurs. */
  @Test(timeout=5000)
  public void testOneOfNumeric() throws Exception {
    checkPolicy(new OneOf("int.2", 0, 1, 2, 4), true);
    checkPolicy(new OneOf("int.2", 0, 1, 2L, 4), true);
    checkPolicy(new OneOf("int.2", 0, 1, 2f, 4), true);
    checkPolicy(new OneOf("int.2", 0, 1, 2d, 4), true);
    checkPolicy(new OneOf("int.2", 0, 1, 3d, 4), false);
    checkPolicy(new OneOf("int.2", "$script{ ${int.1} + 1; }$", "3", "$script{ ${int.1} + ${int.3}; }$", "5"), true);
    checkPolicy(new OneOf("int.2", "3", "$script{ ${int.1} + ${int.3}; }$"), false);
  }

  /** @throws Exception if any error occurs. */
  @Test(timeout=5000)
  public void testRegEx() throws Exception {
    checkPolicy(new RegExp("string.1", "s.*ng.?"), true);
    checkPolicy(new RegExp("string.1", "s.*4ng"), false);
  }

  /** @throws Exception if any error occurs. */
  @Test(timeout=5000)
  public void testAcceptAll() throws Exception {
    checkPolicy(new AcceptAll(), true);
    checkPolicy(new AcceptAll(new RejectAll()), true);
  }

  /** @throws Exception if any error occurs. */
  @Test(timeout=5000)
  public void testRejectAll() throws Exception {
    checkPolicy(new RejectAll(), false);
    checkPolicy(new RejectAll(new AcceptAll()), false);
  }

  /** @throws Exception if any error occurs. */
  @Test(timeout=5000)
  public void testIsMasterNode() throws Exception {
    checkPolicy(new IsMasterNode(), true);
    final String xml = "<jppf:ExecutionPolicy xmlns:jppf='http://www.jppf.org/schemas/ExecutionPolicy.xsd'><IsMasterNode></IsMasterNode></jppf:ExecutionPolicy>";
    PolicyParser.validatePolicy(xml);
    final ExecutionPolicy policy = PolicyParser.parsePolicy(xml);
    assertTrue(policy instanceof IsMasterNode);
  }

  /** @throws Exception if any error occurs. */
  @Test(timeout=5000)
  public void testIsSlaveNode() throws Exception {
    checkPolicy(new IsSlaveNode(), false);
    final String xml = "<jppf:ExecutionPolicy xmlns:jppf='http://www.jppf.org/schemas/ExecutionPolicy.xsd'><IsSlaveNode></IsSlaveNode></jppf:ExecutionPolicy>";
    PolicyParser.validatePolicy(xml);
    final ExecutionPolicy policy = PolicyParser.parsePolicy(xml);
    assertTrue(policy instanceof IsSlaveNode);
  }

  /** @throws Exception if any error occurs. */
  //@Test(timeout=5000)
  @Test
  public void testIsLocalChannel() throws Exception {
    checkPolicy(new IsLocalChannel(), true);
  }

  /** @throws Exception if any error occurs. */
  @Test(timeout=5000)
  public void testIsPeerDriver() throws Exception {
    checkPolicy(new IsPeerDriver(), false);
  }

  /**
   * Test that the context for an execution policy is properly set for all the elements in the policy graph.
   * @throws Exception if any error occurs.
   */
  @Test(timeout=5000)
  public void testMatches() throws Exception {
    final JPPFSystemInformation info = new JPPFSystemInformation(JPPFConfiguration.getProperties(), JPPFUuid.normalUUID(), false, false);
    info.getRuntime().setString("ipv4.addresses", "localhost|192.168.1.14");
    final TestCustomPolicy tcp = new TestCustomPolicy();
    final ExecutionPolicy policy = new Contains("jppf.uuid", true, "AB").and(tcp);
    final JPPFJob job = BaseTestHelper.createJob(ReflectionUtils.getCurrentClassAndMethod(), true, false, 1, LifeCycleTask.class, 0L);
    final JPPFStatistics stats = JPPFStatisticsHelper.createServerStatistics();
    policy.setContext(job.getSLA(), job.getClientSLA(), job.getMetadata(), 2, stats);
    final PolicyContext ctx = tcp.getContext();
    assertNotNull(ctx);
    assertEquals(job.getSLA(), ctx.getSLA());
    assertEquals(job.getClientSLA(), ctx.getClientSLA());
    assertEquals(job.getMetadata(), ctx.getMetadata());
    assertEquals(2, ctx.getJobDispatches());
    assertEquals(stats, ctx.getStats());
  }

  /** @throws Exception if any error occurs. */
  @Test(timeout=5000)
  public void testEmptyPolicy() throws Exception {
    final String str = "<jppf:ExecutionPolicy xmlns:jppf='http://www.jppf.org/schemas/ExecutionPolicy.xsd'></jppf:ExecutionPolicy>";
    Exception exception = null;
    try {
      PolicyParser.validatePolicy(str);
    } catch (final Exception e) {
      exception = e;
      print(false, false, "policy validation exception: %s", ExceptionUtils.getStackTrace(e));
    }
    assertNotNull(exception);

    ExecutionPolicy policy = null;
    exception = null;
    try {
      policy = PolicyParser.parsePolicy(str);
    } catch (final Exception e) {
      exception = e;
      print(false, false, "policy parsing exception: %s", ExceptionUtils.getStackTrace(e));
    }
    assertNotNull(exception);
    assertNull(policy);
  }

  /** @throws Exception if any error occurs. */
  @Test(timeout=5000)
  public void testLogicalRuleArgumentsValidity() throws Exception {
    for (final Class<? extends LogicalRule> c: Arrays.asList(AndRule.class, OrRule.class, XorRule.class)) {
      print(false, false, "checking '%s' policy", c.getSimpleName());
      checkLogicalRule(true,  c, null, null);
      checkLogicalRule(true,  c, (ExecutionPolicy) null);
      checkLogicalRule(true,  c, (ExecutionPolicy[]) null);
      checkLogicalRule(true,  c);
      checkLogicalRule(false, c, truePolicy1);
      checkLogicalRule(false, c, falsePolicy1);
      checkLogicalRule(false, c, truePolicy1, falsePolicy1);
    }
  }

  /** @throws Exception if any error occurs. */
  @Test(timeout=5000)
  public void testIsInSubnetRuleArgumentsValidity() throws Exception {
    for (final Class<? extends AbstractIsInIPSubnet<?>> c: Arrays.asList(IsInIPv4Subnet.class, IsInIPv6Subnet.class)) {
      print(false, false, "checking '%s' policy", c.getSimpleName());
      checkIsInSubnet(true,  c, null, null);
      checkIsInSubnet(true,  c, (String) null);
      checkIsInSubnet(true,  c, (String[]) null);
      checkIsInSubnet(true,  c);
      checkIsInSubnet(false, c, "1");
      checkIsInSubnet(false, c, "1", "2");
      checkIsInSubnet(true,  c, Arrays.asList((String) null, (String) null));
      checkIsInSubnet(true,  c, Arrays.asList((String) null));
      checkIsInSubnet(true,  c, (List<String>) null);
      checkIsInSubnet(true,  c, Arrays.<String>asList());
      checkIsInSubnet(false, c, Arrays.asList("1"));
      checkIsInSubnet(false, c, Arrays.asList("1", "2"));
    }
  }

  /** @throws Exception if any error occurs. */
  @Test(timeout=5000)
  public void testNodesMatchingArgumentsValidity() throws Exception {
    checkNodesMatching(true, -1L,         truePolicy1);
    checkNodesMatching(true,  1L,         null);
    checkNodesMatching(true, -1L,         null);
    checkNodesMatching(false, 1L,         truePolicy1);
    checkNodesMatching(true,  null,       truePolicy1);
    checkNodesMatching(true,  "${int.1}", null);
    checkNodesMatching(false, "${int.1}", truePolicy1);
  }

  /** @throws Exception if any error occurs. */
  @Test(timeout=5000)
  public void testNotRuleArgumentsValidity() throws Exception {
    new NotRule(truePolicy1);
    new NotRule(falsePolicy1);
    Exception exception = null;
    try {
      new NotRule(null);
    } catch (final Exception e) {
      exception = e;
    }
    assertTrue(exception instanceof JPPFRuntimeException);
  }
}
