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

package org.jppf.utils.cli;

/**
 * 
 * @author Laurent Cohen
 */
class CLIArgument {
  /**
   * This argument's name.
   */
  private final String name;
  /**
   * If {@code true} then this argument has a value explicitly specified on the command line,
   * when {@code false} it is a boolean switch set to {@code true} when present or {@code false} when unspecified.
   */
  private final boolean switchArg;
  /**
   * A description of this argument.
   */
  private final String usage;

  /**
   * Initialize this argument with the specified name and excplicit value flag.
   * @param name this argument's name.
   * @param switchArg if {@code true} then this argument has a value explicitly specified on the command line,
   * when {@code false} it is a boolean switch set to {@code true} when present or {@code false} when unspecified.
   * @param usage a string describing this argument's usage.
   */
  public CLIArgument(final String name, final boolean switchArg, final String usage) {
    this.name = name;
    this.switchArg = switchArg;
    this.usage = usage == null ? "" : usage;
  }

  /**
   * Get this argument's name.
   * @return the name of this argument.
   */
  public String getName() {
    return name;
  }

  /**
   * Whether this argument is boolean switch.
   * @return {@code false} when this argument has a value explicitly specified on the command line,
   * {@code true} when it is a boolean switch set to {@code true} when present or {@code false} when unspecified.
   */
  public boolean isSwitch() {
    return switchArg;
  }

  /**
   * Get a description of this argument.
   * @return a string describing this argument's usage.
   */
  public String getUsage() {
    return usage;
  }
}
