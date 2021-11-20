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

import org.jppf.utils.PropertiesCollection;

/**
 * An execution policy rule that rejects everything.
 * @author Laurent Cohen
 */
public class RejectAll extends ExecutionPolicy {
  /**
   * Explicit serialVersionUID.
   */
  private static final long serialVersionUID = 1L;
  /**
   * Name of the corresponding XML element.
   */
  public static final String XML_TAG = RejectAll.class.getSimpleName();

  /**
   * Create an accept all policy not wrapping another policy.
   */
  public RejectAll() {
    super();
  }

  /**
   * Create a "reject all" policy wrapping another policy.
   * @param wrappedPolicy the policy to wrap.
   */
  public RejectAll(final ExecutionPolicy wrappedPolicy) {
    super();
  }

  @Override
  public boolean accepts(final PropertiesCollection<String> info) {
    return false;
  }

  @Override
  public String toString(final int i) {
    if (children.length == 0) return indent(i) + "<" + XML_TAG + "/>\n";
    return new StringBuilder(indent(i)).append(tagStart(XML_TAG)).append('\n').append(children[0].toString(i + 1)).append(tagEnd(XML_TAG)).append('\n').toString();
  }
}
