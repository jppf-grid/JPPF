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

package org.jppf.node.policy;

import java.util.*;

import org.jppf.utils.Operator;


/**
 * Instances of this class build an execution policy graph, based on a policy
 * descriptor parsed from an XML document.
 * @author Laurent Cohen
 */
class PolicyBuilder {
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
      case "Equal":
      case "NotEqual": return buildEqualityPolicy(desc);
      case "Contains": return buildContainsPolicy(desc);
      case "OneOf": return buildOneOfPolicy(desc);
      case "RegExp": return buildRegExpPolicy(desc);
      case "CustomRule": return buildCustomPolicy(desc);
      case "Script": return buildScriptedPolicy(desc);
      case "Preference": return buildPreferencePolicy(desc);
      case "IsInIPv4Subnet":
      case "IsInIPv6Subnet": return buildIsinIPSubnetPolicy(desc);
      case "NodesMatching": return buildNodesMatchingPolicy(desc);
      case "AcceptAll": 
      case "RejectAll": return buildAcceptOrRejectAll(desc.type, desc);
      case "IsMasterNode": return new IsMasterNode();
      case "IsSlaveNode": return new IsSlaveNode();
      case "IsLocalChannel": return new IsLocalChannel();
      case "IsPeerDriver": return new IsPeerDriver();
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
    final ExecutionPolicy[] rules = new ExecutionPolicy[desc.children.size()];
    int count = 0;
    for (final PolicyDescriptor child: desc.children) rules[count++] = buildPolicy(child);
    return new ExecutionPolicy.AndRule(rules);
  }

  /**
   * Build an OR policy from a descriptor.
   * @param desc the descriptor to use.
   * @return an <code>ExecutionPolicy</code> instance.
   * @throws Exception if an error occurs while generating the policy object.
   */
  private ExecutionPolicy buildOrPolicy(final PolicyDescriptor desc) throws Exception {
    final ExecutionPolicy[] rules = new ExecutionPolicy[desc.children.size()];
    int count = 0;
    for (final PolicyDescriptor child: desc.children) rules[count++] = buildPolicy(child);
    return new ExecutionPolicy.OrRule(rules);
  }

  /**
   * Build an XOR policy from a descriptor.
   * @param desc the descriptor to use.
   * @return an <code>ExecutionPolicy</code> instance.
   * @throws Exception if an error occurs while generating the policy object.
   */
  private ExecutionPolicy buildXorPolicy(final PolicyDescriptor desc) throws Exception {
    final ExecutionPolicy[] rules = new ExecutionPolicy[desc.children.size()];
    int count = 0;
    for (final PolicyDescriptor child: desc.children) rules[count++] = buildPolicy(child);
    return new ExecutionPolicy.XorRule(rules);
  }

  /**
   * Build a LessThan policy from a descriptor.
   * @param desc the descriptor to use.
   * @return an <code>ExecutionPolicy</code> instance.
   */
  private static ExecutionPolicy buildLessThanPolicy(final PolicyDescriptor desc) {
    final String s = desc.operands.get(1);
    try {
      return new LessThan(desc.operands.get(0), Double.valueOf(s));
    } catch(@SuppressWarnings("unused") final NumberFormatException e) {
      return new LessThan(desc.operands.get(0), s);
    }
  }

  /**
   * Build an AtMost policy from a descriptor.
   * @param desc the descriptor to use.
   * @return an <code>ExecutionPolicy</code> instance.
   */
  private static ExecutionPolicy buildAtMostPolicy(final PolicyDescriptor desc) {
    final String s = desc.operands.get(1);
    try {
      return new AtMost(desc.operands.get(0), Double.valueOf(s));
    } catch(@SuppressWarnings("unused") final NumberFormatException e) {
      return new AtMost(desc.operands.get(0), s);
    }
  }

  /**
   * Build a MoreThan policy from a descriptor.
   * @param desc the descriptor to use.
   * @return an <code>ExecutionPolicy</code> instance.
   */
  private static ExecutionPolicy buildMoreThanPolicy(final PolicyDescriptor desc) {
    final String s = desc.operands.get(1);
    try {
      return new MoreThan(desc.operands.get(0), Double.valueOf(s));
    } catch(@SuppressWarnings("unused") final NumberFormatException e) {
      return new MoreThan(desc.operands.get(0), s);
    }
  }

  /**
   * Build an AtLeast policy from a descriptor.
   * @param desc the descriptor to use.
   * @return an <code>ExecutionPolicy</code> instance.
   */
  private static ExecutionPolicy buildAtLeastPolicy(final PolicyDescriptor desc) {
    final String s = desc.operands.get(1);
    try {
      return new AtLeast(desc.operands.get(0), Double.valueOf(s));
    } catch(@SuppressWarnings("unused") final NumberFormatException e) {
      return new AtLeast(desc.operands.get(0), s);
    }
  }

  /**
   * Build a BetweenII policy from a descriptor.
   * @param desc the descriptor to use.
   * @return an <code>ExecutionPolicy</code> instance.
   */
  private ExecutionPolicy buildBetweenIIPolicy(final PolicyDescriptor desc) {
    final BetweenArgs args = new BetweenArgs(desc);
    if ((args.value1 != null) && (args.value2 != null)) return new BetweenII(args.prop, args.value1, args.value2);
    else if ((args.value1 == null) && (args.value2 != null)) return new BetweenII(args.prop, args.expr1, args.value2);
    else if ((args.value1 != null) && (args.value2 == null)) return new BetweenII(args.prop, args.value1, args.expr2);
    return new BetweenII(args.prop, args.expr1, args.expr2);
  }

  /**
   * Build a BetweenIE policy from a descriptor.
   * @param desc the descriptor to use.
   * @return an <code>ExecutionPolicy</code> instance.
   */
  private ExecutionPolicy buildBetweenIEPolicy(final PolicyDescriptor desc) {
    final BetweenArgs args = new BetweenArgs(desc);
    if ((args.value1 != null) && (args.value2 != null)) return new BetweenIE(args.prop, args.value1, args.value2);
    else if ((args.value1 == null) && (args.value2 != null)) return new BetweenIE(args.prop, args.expr1, args.value2);
    else if ((args.value1 != null) && (args.value2 == null)) return new BetweenIE(args.prop, args.value1, args.expr2);
    return new BetweenIE(args.prop, args.expr1, args.expr2);
  }

  /**
   * Build a BetweenEI policy from a descriptor.
   * @param desc the descriptor to use.
   * @return an <code>ExecutionPolicy</code> instance.
   */
  private ExecutionPolicy buildBetweenEIPolicy(final PolicyDescriptor desc) {
    final BetweenArgs args = new BetweenArgs(desc);
    if ((args.value1 != null) && (args.value2 != null)) return new BetweenEI(args.prop, args.value1, args.value2);
    else if ((args.value1 == null) && (args.value2 != null)) return new BetweenEI(args.prop, args.expr1, args.value2);
    else if ((args.value1 != null) && (args.value2 == null)) return new BetweenEI(args.prop, args.value1, args.expr2);
    return new BetweenEI(args.prop, args.expr1, args.expr2);
  }

  /**
   * Build a BetweenEE policy from a descriptor.
   * @param desc the descriptor to use.
   * @return an <code>ExecutionPolicy</code> instance.
   */
  private ExecutionPolicy buildBetweenEEPolicy(final PolicyDescriptor desc) {
    final BetweenArgs args = new BetweenArgs(desc);
    if ((args.value1 != null) && (args.value2 != null)) return new BetweenEE(args.prop, args.value1, args.value2);
    else if ((args.value1 == null) && (args.value2 != null)) return new BetweenEE(args.prop, args.expr1, args.value2);
    else if ((args.value1 != null) && (args.value2 == null)) return new BetweenEE(args.prop, args.value1, args.expr2);
    return new BetweenEE(args.prop, args.expr1, args.expr2);
  }

  /**
   * Build a Equal policy from a descriptor.
   * @param desc the descriptor to use.
   * @return an {@link ExecutionPolicy} instance.
   */
  private static ExecutionPolicy buildEqualityPolicy(final PolicyDescriptor desc) {
    final boolean isEqualPolicy = "Equal".equals(desc.type);
    final String s = desc.operands.get(1);
    if ("string".equals(desc.valueType)) {
      final boolean ignoreCase = (desc.ignoreCase == null) ? false : Boolean.valueOf(desc.ignoreCase);
      return isEqualPolicy ? new Equal(desc.operands.get(0), ignoreCase, s) : new NotEqual(desc.operands.get(0), ignoreCase, s);
    }
    if ("numeric".equals(desc.valueType)) {
      double value = 0.0d;
      try {
        value = Double.valueOf(s);
        return isEqualPolicy ? new Equal(desc.operands.get(0), value) : new NotEqual(desc.operands.get(0), value);
      } catch(@SuppressWarnings("unused") final NumberFormatException e) {
        return isEqualPolicy ? new Equal(ValueType.NUMERIC, desc.operands.get(0), s) : new NotEqual(ValueType.NUMERIC, desc.operands.get(0), s);
      }
    }
    return isEqualPolicy ? new Equal(ValueType.BOOLEAN, desc.operands.get(0), s) : new NotEqual(ValueType.BOOLEAN, desc.operands.get(0), s);
  }

  /**
   * Build a Contains policy from a descriptor.
   * @param desc the descriptor to use.
   * @return an <code>ExecutionPolicy</code> instance.
   */
  private static ExecutionPolicy buildContainsPolicy(final PolicyDescriptor desc) {
    final boolean ignoreCase = (desc.ignoreCase == null) ? false : Boolean.valueOf(desc.ignoreCase);
    return new Contains(desc.operands.get(0), ignoreCase, desc.operands.get(1));
  }

  /**
   * Build a OneOf policy from a descriptor.
   * @param desc the descriptor to use.
   * @return an <code>ExecutionPolicy</code> instance.
   */
  private static ExecutionPolicy buildOneOfPolicy(final PolicyDescriptor desc) {
    if ("numeric".equals(desc.valueType)) {
      final int size = desc.operands.size() - 1;
      final List<String> values = new ArrayList<>(size);
      for (int i=1; i<desc.operands.size(); i++) {
        values.add(desc.operands.get(i));
      }
      return new OneOf(desc.operands.get(0), values.toArray(new String[size]));
    }
    final String[] values = new String[desc.operands.size() - 1];
    for (int i=1; i<desc.operands.size(); i++) values[i-1] = desc.operands.get(i);
    final boolean ignoreCase = Boolean.valueOf(desc.ignoreCase);
    return new OneOf(desc.operands.get(0), ignoreCase, values);
  }

  /**
   * Build a RegExp policy from a descriptor.
   * @param desc the descriptor to use.
   * @return an <code>ExecutionPolicy</code> instance.
   */
  private static ExecutionPolicy buildRegExpPolicy(final PolicyDescriptor desc) {
    return new RegExp(desc.operands.get(0), desc.operands.get(1));
  }

  /**
   * Build a custom policy from a descriptor.
   * @param desc the descriptor to use.
   * @return an <code>ExecutionPolicy</code> instance.
   * @throws Exception if an error occurs while generating the custom policy object.
   */
  private static ExecutionPolicy buildCustomPolicy(final PolicyDescriptor desc) throws Exception {
    final Class<?> clazz = Class.forName(desc.className);
    final CustomPolicy policy = (CustomPolicy) clazz.newInstance();
    policy.setArgs(desc.arguments.toArray(new String[desc.arguments.size()]));
    policy.initialize();
    return policy;
  }

  /**
   * Build a scripted policy from a descriptor.
   * @param desc the descriptor to use.
   * @return an <code>ExecutionPolicy</code> instance.
   */
  private static ExecutionPolicy buildScriptedPolicy(final PolicyDescriptor desc)  {
    return new ScriptedPolicy(desc.language, desc.script);
  }

  /**
   * Build an AND policy from a descriptor.
   * @param desc the descriptor to use.
   * @return an <code>ExecutionPolicy</code> instance.
   * @throws Exception if an error occurs while generating the policy object.
   */
  private ExecutionPolicy buildPreferencePolicy(final PolicyDescriptor desc) throws Exception {
    final ExecutionPolicy[] rules = new ExecutionPolicy[desc.children.size()];
    int count = 0;
    for (final PolicyDescriptor child: desc.children) rules[count++] = buildPolicy(child);
    return new Preference(rules);
  }

  /**
   * Build a OneOf policy from a descriptor.
   * @param desc the descriptor to use.
   * @return an <code>ExecutionPolicy</code> instance.
   */
  private static ExecutionPolicy buildIsinIPSubnetPolicy(final PolicyDescriptor desc) {
    return "IsInIPv4Subnet".equals(desc.type) ? new IsInIPv4Subnet(desc.operands) : new IsInIPv6Subnet(desc.operands);
  }

  /**
   * Build a global policy.
   * @param desc the descriptor to use.
   * @return an <code>ExecutionPolicy</code> instance.
   * @throws Exception if an error occurs while generating the policy object.
   */
  private ExecutionPolicy buildNodesMatchingPolicy(final PolicyDescriptor desc)  throws Exception {
    final ExecutionPolicy child = (desc.children != null) && (desc.children.size() > 0) ? buildPolicy(desc.children.get(0)) : null;
    final Operator operator = Operator.valueOf(desc.operator.toUpperCase());
    try {
      final long expected = Long.valueOf(desc.expected);
      return new NodesMatching(operator, expected, child);
    } catch(@SuppressWarnings("unused") final NumberFormatException e) {
      return new NodesMatching(operator, desc.expected, child);
    }
  }

  /**
   * Build a global policy.
   * @param name the name of the execution policy rule to build.
   * @param desc the descriptor to use.
   * @return an {@code ExecutionPolicy} instance.
   * @throws Exception if an error occurs while generating the policy object.
   */
  private ExecutionPolicy buildAcceptOrRejectAll(final String name, final PolicyDescriptor desc)  throws Exception {
    final ExecutionPolicy child = (desc.children != null) && (desc.children.size() > 0) ? buildPolicy(desc.children.get(0)) : null;
    if ("AcceptAll".equals(name)) return (child == null) ? new AcceptAll() : new AcceptAll(child);
    return (child == null) ? new RejectAll() : new RejectAll(child);
  }

  /** */
  class BetweenArgs {
    /** */
    String prop, expr1, expr2;
    /** */
    Double value1, value2;

    /**
     * @param desc the descriptor to use.
     */
    BetweenArgs(final PolicyDescriptor desc) {
      prop = desc.operands.get(0);
      String s = desc.operands.get(1);
      try {
        value1 = Double.valueOf(s);
      } catch(@SuppressWarnings("unused") final NumberFormatException e) {
        expr1 = s;
      }
      s = desc.operands.get(2);
      try {
        value2 = Double.valueOf(s);
      } catch(@SuppressWarnings("unused") final NumberFormatException e) {
        expr2 = s;
      }
    }
  }
}
