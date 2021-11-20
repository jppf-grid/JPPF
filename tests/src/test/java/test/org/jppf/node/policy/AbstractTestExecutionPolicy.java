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

package test.org.jppf.node.policy;

import static org.junit.Assert.*;

import java.lang.reflect.Constructor;
import java.util.*;

import org.jppf.JPPFRuntimeException;
import org.jppf.management.JPPFSystemInformation;
import org.jppf.node.policy.*;
import org.jppf.utils.*;
import org.jppf.utils.configuration.JPPFProperties;
import org.junit.BeforeClass;

import test.org.jppf.test.setup.BaseTest;

/**
 * Test for functions commons to all execution policies.
 * @author Laurent Cohen
 */
public class AbstractTestExecutionPolicy extends BaseTest {
  /** used for testing logical policies. */
  final ExecutionPolicy truePolicy1 = new Equal("int.1", 1), truePolicy2 = new Equal("int.2", 2), truePolicy3 = new Equal("int.3", 3);
  /** used for testing logical policies. */
  final ExecutionPolicy falsePolicy1 = new Equal("int.1", 2), falsePolicy2 = new Equal("int.2", 4), falsePolicy3 = new Equal("int.3", 6);
  /** */
  static JPPFSystemInformation systemInfo;

  /** @throws Exception if any error occurs. */
  @BeforeClass
  public static void setup() throws Exception {
    systemInfo = new JPPFSystemInformation(JPPFConfiguration.getProperties(), "test", true, false);
    final TypedProperties test = new TypedProperties()
      .setInt("int.1", 1).setInt("int.2", 2).setInt("int.3", 3).setInt("int.10", 10)
      .setString("string", "string").setString("string.tr", "tr").setString("string.ue", "ue")
      .setString("string.1", "string1").setString("string.1a", "stri").setString("string.1b", "ng1")
      .setString("string.2", "string2").setString("string.3", "string3").setString("string.4a", "string4")
      .setString("string.4b", "stRIng4").setString("string.5", "string1-string2").setBoolean("boolean.1", true)
      .setBoolean("boolean.2", false).set(JPPFProperties.PROVISIONING_MASTER, true).set(JPPFProperties.PROVISIONING_SLAVE, false);
    systemInfo.addProperties("test", test);
  }

  /**
   * Check that the specified execution policy's evaluation returns the expected result.
   * @param policy the policy to evaluate.
   * @param expected the policy's expected return value.
   * @throws Exception if any error occurs.
   */
  static void checkPolicy(final ExecutionPolicy policy, final boolean expected) throws Exception {
    assertEquals(expected, policy.accepts(systemInfo));
    final String s1 = policy.toString();
    print(false, false, "checking policy:\n%s", s1);
    final String str = new StringBuilder("<jppf:ExecutionPolicy xmlns:jppf='http://www.jppf.org/schemas/ExecutionPolicy.xsd'>\n")
      .append(s1).append("</jppf:ExecutionPolicy>\n").toString();
    try {
      PolicyParser.validatePolicy(str);
    } catch (final Exception e) {
      final String message = String.format("policy validation exception: %s", ExceptionUtils.getStackTrace(e));
      print(false, false, message);
      fail(message);
    }
    final ExecutionPolicy parsed = PolicyParser.parsePolicy(str);
    assertEquals(policy.getClass(), parsed.getClass());
    assertNotNull(parsed);
    assertEquals(expected, parsed.accepts(systemInfo));
    assertEquals(s1, parsed.toString());
  }

  /**
   * Instantiate a policy of the specified class using the specified arguments and check if it results in an exception or not.
   * @param expectException whether to expect an exception as a result or not.
   * @param c the class of the execution policy to instantiate.
   * @param args the arguments to provide to the constructor of the policy.
   * @throws Exception if any error occurs, other than the expected exception.
   */
  static void checkLogicalRule(final boolean expectException, final Class<? extends ExecutionPolicy> c, final ExecutionPolicy...args) throws Exception {
    try {
      final Constructor<? extends ExecutionPolicy> constructor = c.getConstructor(ExecutionPolicy[].class);
      print(false, false, "class = %s, constructor = %s, args = %s", c.getName(), constructor, Arrays.toString(args));
      constructor.newInstance((Object) args);
    } catch(final Exception e) {
      print(false, false, "cause exception = %s", e.getCause());
      if (expectException) assertTrue(e.getCause() instanceof JPPFRuntimeException);
      else throw e;
    }
  }

  /**
   * Instantiate a policy of the specified class using the specified arguments and check if it results in an exception or not.
   * @param expectException whether to expect an exception as a result or not.
   * @param c the class of the execution policy to instantiate.
   * @param args the arguments to provide to the constructor of the policy.
   * @throws Exception if any error occurs, other than the expected exception.
   */
  static void checkIsInSubnet(final boolean expectException, final Class<? extends AbstractIsInIPSubnet<?>> c, final String...args) throws Exception {
    try {
      final Constructor<? extends AbstractIsInIPSubnet<?>> constructor = c.getConstructor(String[].class);
      print(false, false, "class = %s, constructor = %s, args = %s", c.getName(), constructor, Arrays.toString(args));
      constructor.newInstance((Object) args);
    } catch(final Exception e) {
      print(false, false, "cause exception = %s", e.getCause());
      if (expectException) assertTrue(e.getCause() instanceof JPPFRuntimeException);
      else throw e;
    }
  }

  /**
   * Instantiate a policy of the specified class using the specified arguments and check if it results in an exception or not.
   * @param expectException whether to expect an exception as a result or not.
   * @param c the class of the execution policy to instantiate.
   * @param args the arguments to provide to the constructor of the policy.
   * @throws Exception if any error occurs, other than the expected exception.
   */
  static void checkIsInSubnet(final boolean expectException, final Class<? extends AbstractIsInIPSubnet<?>> c, final List<String> args) throws Exception {
    try {
      final Constructor<? extends AbstractIsInIPSubnet<?>> constructor = c.getConstructor(Collection.class);
      print(false, false, "class = %s, constructor = %s, args = %s", c.getName(), constructor, args);
      constructor.newInstance(args);
    } catch(final Exception e) {
      print(false, false, "cause exception = %s", e.getCause());
      if (expectException) assertTrue(e.getCause() instanceof JPPFRuntimeException);
      else throw e;
    }
  }

  /**
   * Instantiate {@link NodesMatching} policy with the specified policy argument.
   * @param expectException whether to expect an exception as a result or not.
   * @param expectedNodes the number of nodes that must match the policy argument, as a literal.
   * @param policy the policy that must be matched by the nodes.
   * @throws Exception if any error occurs, other than the expected exception.
   */
  static void checkNodesMatching(final boolean expectException, final long expectedNodes, final ExecutionPolicy policy) throws Exception {
    try {
      print(false, false, "expectedNodes = %d, policy = %s", expectedNodes, policy);
      new NodesMatching(Operator.AT_LEAST, expectedNodes, policy);
    } catch(final Exception e) {
      print(false, false, "exception = %s", e);
      if (expectException) assertTrue(e instanceof JPPFRuntimeException);
      else throw e;
    }
  }

  /**
   * Instantiate {@link NodesMatching} policy with the specified policy argument.
   * @param expectException whether to expect an exception as a result or not.
   * @param expectedNodes the number of nodes that must match the policy argument, as an expression.
   * @param policy the policy that must be matched by the nodes.
   * @throws Exception if any error occurs, other than the expected exception.
   */
  static void checkNodesMatching(final boolean expectException, final String expectedNodes, final ExecutionPolicy policy) throws Exception {
    try {
      print(false, false, "expectedNodes = %s, policy = %s", expectedNodes, policy);
      new NodesMatching(Operator.AT_LEAST, expectedNodes, policy);
    } catch(final Exception e) {
      print(false, false, "exception = %s", e);
      if (expectException) assertTrue(e instanceof JPPFRuntimeException);
      else throw e;
    }
  }

  /** */
  public static class TestCustomPolicy extends CustomPolicy {
    /** Explicit serialVersionUID. */
    private static final long serialVersionUID = 1L;

    @Override
    public boolean accepts(final PropertiesCollection<String> info) {
      return false;
    }
  }
}
