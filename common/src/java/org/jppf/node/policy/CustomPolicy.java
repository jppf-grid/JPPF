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

/**
 * Abstract superclass for all user-defined policies.
 * @author Laurent Cohen
 */
public abstract class CustomPolicy extends ExecutionPolicy {
  /**
   * Explicit serialVersionUID.
   */
  private static final long serialVersionUID = 1L;
  /**
   * The list of user-defined arguments.
   */
  private String[] args = null;

  /**
   * Initialize this policy with the specified arguments.
   * @param args the user-defined arguments for this policy.
   */
  public CustomPolicy(final String... args) {
    this.args = args;
  }

  /**
   * Perform optional initializations. This default implementation does nothing.<br>
   * This method is called after instantiating the policy object and setting the arguments.
   * It allows for user-defined initializations when the custom policy is built from an XML document.
   */
  public void initialize() {
  }

  /**
   * Get the arguments of this policy.
   * @return the arguments as an array of strings.
   */
  public final String[] getArgs() {
    return args;
  }

  /**
   * Set the arguments of this policy.
   * @param args the arguments as an array of strings.
   */
  public final void setArgs(final String... args) {
    this.args = args;
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
        sb.append(indent()).append("<CustomRule class=\"").append(this.getClass().getName()).append("\">\n");
        toStringIndent++;
        if (args != null) {
          for (String s : args)
            sb.append(indent()).append("<Arg>").append(s).append("</Arg>\n");
        }
        toStringIndent--;
        sb.append(indent()).append("</CustomRule>\n");
        computedToString = sb.toString();
      }
    }
    return computedToString;
  }
}
