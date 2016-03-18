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

package org.jppf.node.policy;

import org.jppf.client.Operator;

/**
 * Instances of this class build an execution policy graph, based on a policy
 * descriptor parsed from an XML document.
 * @author Laurent Cohen
 */
public class PolicyBuilder {
  /**
   * Build an execution policy from a parsed policy descriptor.
   * @param desc the descriptor parsed from an XML document.
   * @return an <code>ExecutionPolicy</code> instance.
   * @throws Exception if an error occurs while generating a policy object.
   */
  public ExecutionPolicy buildPolicy(final PolicyDescriptor desc) throws Exception {
    switch(desc.type) {
      case "NOT": return buildNotPolicy(desc);
      case "AND": return buildAndPolicy(desc);
      case "OR": return buildOrPolicy(desc);
      case "XOR": return buildXorPolicy(desc);
      case "LessThan": return buildLessThanPolicy(desc);
      case "MoreThan": return buildMoreThanPolicy(desc);
      case "AtMost": return buildAtMostPolicy(desc);
      case "AtLeast": return buildAtLeastPolicy(desc);
      case "BetweenII": return buildBetweenIIPolicy(desc);
      case "BetweenIE": return buildBetweenIEPolicy(desc);
      case "BetweenEI": return buildBetweenEIPolicy(desc);
      case "BetweenEE": return buildBetweenEEPolicy(desc);
      case "Equal": return buildEqualPolicy(desc);
      case "Contains": return buildContainsPolicy(desc);
      case "OneOf": return buildOneOfPolicy(desc);
      case "RegExp": return buildRegExpPolicy(desc);
      case "CustomRule": return buildCustomPolicy(desc);
      case "Script": return buildScriptedPolicy(desc);
      case "Preference": return buildPreferencePolicy(desc);
      case "IsInIPv4Subnet":
      case "IsInIPv6Subnet": return buildIsinIPSubnetPolicy(desc);
      case "NodesMatching": return buildNodesMatchingPolicy(desc);
    }
    return null;
  }

  /**
   * Build a NOT policy from a descriptor.
   * @param desc the descriptor to use.
   * @return an <code>ExecutionPolicy</code> instance.
   * @throws Exception if an error occurs while generating the policy object.
   */
  private ExecutionPolicy buildNotPolicy(final PolicyDescriptor desc) throws Exception {
    return new ExecutionPolicy.NotRule(buildPolicy(desc.children.get(0)));
  }

  /**
   * Build an AND policy from a descriptor.
   * @param desc the descriptor to use.
   * @return an <code>ExecutionPolicy</code> instance.
   * @throws Exception if an error occurs while generating the policy object.
   */
  private ExecutionPolicy buildAndPolicy(final PolicyDescriptor desc) throws Exception {
    ExecutionPolicy[] rules = new ExecutionPolicy[desc.children.size()];
    int count = 0;
    for (PolicyDescriptor child: desc.children) rules[count++] = buildPolicy(child);
    return new ExecutionPolicy.AndRule(rules);
  }

  /**
   * Build an OR policy from a descriptor.
   * @param desc the descriptor to use.
   * @return an <code>ExecutionPolicy</code> instance.
   * @throws Exception if an error occurs while generating the policy object.
   */
  private ExecutionPolicy buildOrPolicy(final PolicyDescriptor desc) throws Exception {
    ExecutionPolicy[] rules = new ExecutionPolicy[desc.children.size()];
    int count = 0;
    for (PolicyDescriptor child: desc.children) rules[count++] = buildPolicy(child);
    return new ExecutionPolicy.OrRule(rules);
  }

  /**
   * Build an XOR policy from a descriptor.
   * @param desc the descriptor to use.
   * @return an <code>ExecutionPolicy</code> instance.
   * @throws Exception if an error occurs while generating the policy object.
   */
  private ExecutionPolicy buildXorPolicy(final PolicyDescriptor desc) throws Exception {
    ExecutionPolicy[] rules = new ExecutionPolicy[desc.children.size()];
    int count = 0;
    for (PolicyDescriptor child: desc.children) rules[count++] = buildPolicy(child);
    return new ExecutionPolicy.XorRule(rules);
  }

  /**
   * Build a LessThan policy from a descriptor.
   * @param desc the descriptor to use.
   * @return an <code>ExecutionPolicy</code> instance.
   */
  private ExecutionPolicy buildLessThanPolicy(final PolicyDescriptor desc) {
    String s = desc.operands.get(1);
    double value = 0.0d;
    try {
      value = Double.valueOf(s);
    } catch(NumberFormatException e) {
      throw new IllegalArgumentException('\'' +s+"' is not a double value", e);
    }
    return new LessThan(desc.operands.get(0), value);
  }

  /**
   * Build an AtMost policy from a descriptor.
   * @param desc the descriptor to use.
   * @return an <code>ExecutionPolicy</code> instance.
   */
  private ExecutionPolicy buildAtMostPolicy(final PolicyDescriptor desc) {
    String s = desc.operands.get(1);
    double value = 0.0d;
    try {
      value = Double.valueOf(s);
    } catch(NumberFormatException e) {
      throw new IllegalArgumentException('\'' +s+"' is not a double value", e);
    }
    return new AtMost(desc.operands.get(0), value);
  }

  /**
   * Build a MoreThan policy from a descriptor.
   * @param desc the descriptor to use.
   * @return an <code>ExecutionPolicy</code> instance.
   */
  private ExecutionPolicy buildMoreThanPolicy(final PolicyDescriptor desc) {
    String s = desc.operands.get(1);
    double value = 0.0d;
    try {
      value = Double.valueOf(s);
    } catch(NumberFormatException e) {
      throw new IllegalArgumentException('\'' +s+"' is not a double value", e);
    }
    return new MoreThan(desc.operands.get(0), value);
  }

  /**
   * Build an AtLeast policy from a descriptor.
   * @param desc the descriptor to use.
   * @return an <code>ExecutionPolicy</code> instance.
   */
  private ExecutionPolicy buildAtLeastPolicy(final PolicyDescriptor desc) {
    String s = desc.operands.get(1);
    double value = 0.0d;
    try {
      value = Double.valueOf(s);
    } catch(NumberFormatException e) {
      throw new IllegalArgumentException('\'' +s+"' is not a double value", e);
    }
    return new AtLeast(desc.operands.get(0), value);
  }

  /**
   * Build a BetweenII policy from a descriptor.
   * @param desc the descriptor to use.
   * @return an <code>ExecutionPolicy</code> instance.
   */
  private ExecutionPolicy buildBetweenIIPolicy(final PolicyDescriptor desc) {
    String s = desc.operands.get(1);
    double value1 = 0.0d;
    double value2 = 0.0d;
    try {
      value1 = Double.valueOf(s);
      s = desc.operands.get(2);
      value2 = Double.valueOf(s);
    } catch(NumberFormatException e) {
      throw new IllegalArgumentException('\'' +s+"' is not a double value", e);
    }
    return new BetweenII(desc.operands.get(0), value1, value2);
  }

  /**
   * Build a BetweenIE policy from a descriptor.
   * @param desc the descriptor to use.
   * @return an <code>ExecutionPolicy</code> instance.
   */
  private ExecutionPolicy buildBetweenIEPolicy(final PolicyDescriptor desc) {
    String s = desc.operands.get(1);
    double value1 = 0.0d;
    double value2 = 0.0d;
    try {
      value1 = Double.valueOf(s);
      s = desc.operands.get(2);
      value2 = Double.valueOf(s);
    } catch(NumberFormatException e) {
      throw new IllegalArgumentException('\'' +s+"' is not a double value", e);
    }
    return new BetweenIE(desc.operands.get(0), value1, value2);
  }

  /**
   * Build a BetweenEI policy from a descriptor.
   * @param desc the descriptor to use.
   * @return an <code>ExecutionPolicy</code> instance.
   */
  private ExecutionPolicy buildBetweenEIPolicy(final PolicyDescriptor desc) {
    String s = desc.operands.get(1);
    double value1 = 0.0d;
    double value2 = 0.0d;
    try {
      value1 = Double.valueOf(s);
      s = desc.operands.get(2);
      value2 = Double.valueOf(s);
    } catch(NumberFormatException e) {
      throw new IllegalArgumentException('\'' +s+"' is not a double value", e);
    }
    return new BetweenEI(desc.operands.get(0), value1, value2);
  }

  /**
   * Build a BetweenEE policy from a descriptor.
   * @param desc the descriptor to use.
   * @return an <code>ExecutionPolicy</code> instance.
   */
  private ExecutionPolicy buildBetweenEEPolicy(final PolicyDescriptor desc) {
    String s = desc.operands.get(1);
    double value1 = 0.0d;
    double value2 = 0.0d;
    try {
      value1 = Double.valueOf(s);
      s = desc.operands.get(2);
      value2 = Double.valueOf(s);
    } catch(NumberFormatException e) {
      throw new IllegalArgumentException('\'' +s+"' is not a double value", e);
    }
    return new BetweenEE(desc.operands.get(0), value1, value2);
  }

  /**
   * Build a Equal policy from a descriptor.
   * @param desc the descriptor to use.
   * @return an <code>ExecutionPolicy</code> instance.
   */
  private ExecutionPolicy buildEqualPolicy(final PolicyDescriptor desc) {
    String s = desc.operands.get(1);
    if ("string".equals(desc.valueType)) {
      boolean ignoreCase = (desc.ignoreCase == null) ? false : Boolean.valueOf(desc.ignoreCase);
      return new Equal(desc.operands.get(0), ignoreCase, s);
    }
    if ("numeric".equals(desc.valueType)) {
      double value = 0.0d;
      try {
        value = Double.valueOf(s);
        return new Equal(desc.operands.get(0), value);
      } catch(NumberFormatException e) {
        throw new IllegalArgumentException('\'' + s + "' is not a double value", e);
      }
    }
    return new Equal(desc.operands.get(0), Boolean.valueOf(s));
  }

  /**
   * Build a Contains policy from a descriptor.
   * @param desc the descriptor to use.
   * @return an <code>ExecutionPolicy</code> instance.
   */
  private ExecutionPolicy buildContainsPolicy(final PolicyDescriptor desc) {
    boolean ignoreCase = (desc.ignoreCase == null) ? false : Boolean.valueOf(desc.ignoreCase);
    return new Contains(desc.operands.get(0), ignoreCase, desc.operands.get(1));
  }

  /**
   * Build a OneOf policy from a descriptor.
   * @param desc the descriptor to use.
   * @return an <code>ExecutionPolicy</code> instance.
   */
  private ExecutionPolicy buildOneOfPolicy(final PolicyDescriptor desc) {
    if ("numeric".equals(desc.valueType)) {
      double[] values = new double[desc.operands.size() - 1];
      for (int i=1; i<desc.operands.size(); i++) {
        String s = desc.operands.get(i);
        try {
          values[i-1] = Double.valueOf(s);
        } catch(NumberFormatException e) {
          throw new IllegalArgumentException('\'' +s+"' is not a double value", e);
        }
      }
      return new OneOf(desc.operands.get(0), values);
    }
    String[] values = new String[desc.operands.size() - 1];
    for (int i=1; i<desc.operands.size(); i++) values[i-1] = desc.operands.get(i);
    boolean ignoreCase = Boolean.valueOf(desc.ignoreCase);
    return new OneOf(desc.operands.get(0), ignoreCase, values);
  }

  /**
   * Build a RegExp policy from a descriptor.
   * @param desc the descriptor to use.
   * @return an <code>ExecutionPolicy</code> instance.
   */
  private ExecutionPolicy buildRegExpPolicy(final PolicyDescriptor desc) {
    return new RegExp(desc.operands.get(0), desc.operands.get(1));
  }

  /**
   * Build a custom policy from a descriptor.
   * @param desc the descriptor to use.
   * @return an <code>ExecutionPolicy</code> instance.
   * @throws Exception if an error occurs while generating the custom policy object.
   */
  private ExecutionPolicy buildCustomPolicy(final PolicyDescriptor desc) throws Exception {
    Class clazz = Class.forName(desc.className);
    CustomPolicy policy = (CustomPolicy) clazz.newInstance();
    policy.setArgs(desc.arguments.toArray(new String[desc.arguments.size()]));
    policy.initialize();
    return policy;
  }

  /**
   * Build a scripted policy from a descriptor.
   * @param desc the descriptor to use.
   * @return an <code>ExecutionPolicy</code> instance.
   */
  private ExecutionPolicy buildScriptedPolicy(final PolicyDescriptor desc)  {
    return new ScriptedPolicy(desc.language, desc.script);
  }

  /**
   * Build an AND policy from a descriptor.
   * @param desc the descriptor to use.
   * @return an <code>ExecutionPolicy</code> instance.
   * @throws Exception if an error occurs while generating the policy object.
   */
  private ExecutionPolicy buildPreferencePolicy(final PolicyDescriptor desc) throws Exception {
    ExecutionPolicy[] rules = new ExecutionPolicy[desc.children.size()];
    int count = 0;
    for (PolicyDescriptor child: desc.children) rules[count++] = buildPolicy(child);
    return new Preference(rules);
  }

  /**
   * Build a OneOf policy from a descriptor.
   * @param desc the descriptor to use.
   * @return an <code>ExecutionPolicy</code> instance.
   */
  private ExecutionPolicy buildIsinIPSubnetPolicy(final PolicyDescriptor desc) {
    return "IsInIPv4Subnet".equals(desc.type) ? new IsInIPv4Subnet(desc.operands) : new IsInIPv6Subnet(desc.operands);
  }


  /**
   * Build a global policy.
   * @param desc the descriptor to use.
   * @return an <code>ExecutionPolicy</code> instance.
   * @throws Exception if an error occurs while generating the policy object.
   * @since 5.2
   */
  private ExecutionPolicy buildNodesMatchingPolicy(final PolicyDescriptor desc)  throws Exception {
    ExecutionPolicy child = (desc.children != null) && (desc.children.size() > 0) ? buildPolicy(desc.children.get(0)) : null;
    Operator operator = Operator.valueOf(desc.operator.toUpperCase());
    long expected = Long.valueOf(desc.expected);
    return new NodesMatching(operator, expected, child);
  }
}
