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
import org.jppf.management.*;
import org.jppf.utils.PropertiesCollection;
import org.slf4j.*;

/**
 * An execution policy rule that checks whether a specified number of nodes match a node execution policy.
 * The number of nodes is expressed as a condtion of type <tt>actualNbNodes <i>comp</i> expectedNbNodes</tt>, where <tt><i>comp</i></tt>
 * is a {@link Operator comparison operator} among <tt>==, !=, &lt;, &gt;, &lt;=, &gt;=</tt>.
 * <p>Instances of this class can only apply to, and be used in, a server.
 * <p>As an example, the condition "if there are more than 4 nodes idle and with at least 2 processors" can be expressed as follows:
 * <pre>
 * ExecutionPolicy nodePolicy = new Equal("jppf.node.idle", true).and(new AtLeast("availableProcessors", 2));
 * ExecutionPolicy globalPolicy = new NodesMatching(Operator.GREATER, 4, nodePolicy);
 * </pre>
 * <p>Alternatively, it can also be written as an XML document:
 * <pre>
 * &lt;NodesMatching operator="GREATER" expected="4"&gt;
 *   &lt;AND&gt;
 *     &lt;Equal valueType="boolean"&gt;
 *       &lt;Property&gt;jppf.node.idle&lt;/Property&gt;
 *       &lt;Value&gt;true&lt;/Value&gt;
 *     &lt;/Equal&gt;
 *     &lt;AtLeast&gt;
 *       &lt;Property&gt;availableProcessors&lt;/Property&gt;
 *       &lt;Value&gt;2&lt;/Value&gt;
 *     &lt;/AtLeast&gt;
 *   &lt;/AND&gt;
 * &lt;/NodesMatching&gt;
 * </pre>
 * @author Laurent Cohen
 * @since 5.2
 */
public class NodesMatching extends ExecutionPolicy {
  /**
   * Logger for this class.
   */
  private static final Logger log = LoggerFactory.getLogger(NodesMatching.class);
  /**
   * Name of the corresponding XML element.
   */
  public static final String XML_ELT = NodesMatching.class.getSimpleName();
  /**
   * The comparison operator to use for the number of nodes.
   */
  private final Operator operator;
  /**
   * The expected number of nodes to use in the comparison.
   */
  private final long expectedNodes;
  /**
   * The execution policy to match the nodes against.
   */
  private final ExecutionPolicy nodePolicy;

  /**
   * Initialize this execution policy.
   * @param operator the comparison operator to use for the number of nodes.
   * @param expectedNodes the expected number of nodes to use in the comparison.
   * @param nodePolicy the execution policy to match the nodes against. If {@code null}, then all the nodes will match.
   */
  public NodesMatching(final Operator operator, final long expectedNodes, final ExecutionPolicy nodePolicy) {
    this.operator = operator == null ? Operator.EQUAL : operator;
    this.expectedNodes = expectedNodes;
    this.nodePolicy = nodePolicy;
  }

  @Override
  public boolean accepts(final PropertiesCollection info) {
    int nbNodes = 0;
    try (JMXDriverConnectionWrapper jmx = new JMXDriverConnectionWrapper()) {
      jmx.connect();
      nbNodes = jmx.nbNodes((nodePolicy == null) ? NodeSelector.ALL_NODES : new ExecutionPolicySelector(nodePolicy));
    } catch (Exception e) {
      log.error("error evaluating global policy", e);
    }
    return operator.evaluate(nbNodes, expectedNodes);
  }

  /**
   * Print this object to a string.
   * @return an XML string representation of this object
   */
  @Override
  public String toString() {
    if (computedToString == null) {
      synchronized (ExecutionPolicy.class) {
        StringBuilder sb = new StringBuilder();
        sb.append(indent()).append('<').append(XML_ELT);
        sb.append(" operator=\"").append(operator.name()).append("\" expected=\"").append(expectedNodes).append("\">\n");
        if (nodePolicy != null) {
          toStringIndent++;
          sb.append(nodePolicy.toString());
          toStringIndent--;
        }
        sb.append(indent()).append("</").append(XML_ELT).append(">\n");
        computedToString = sb.toString();
      }
    }
    return computedToString;
  }

  @Override
  void initializeRoot() {
    super.initializeRoot();
    if (nodePolicy != null) nodePolicy.setContext(context);
  }

  @Override
  void initializeRoot(final ExecutionPolicy root) {
    super.initializeRoot(root);
    if (nodePolicy != null) nodePolicy.setContext(getContext());
  }
}
