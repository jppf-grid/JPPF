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

import java.util.*;

import org.jppf.utils.*;

/**
 * 
 * @param <T> the type of this set of arguments.
 * @author Laurent Cohen
 */
abstract class AbstractCLIArguments<T extends AbstractCLIArguments<?>> extends TypedProperties {
  /**
   * The argument definitions.
   */
  final Map<String, CLIArgument> argDefs = new LinkedHashMap<>();
  /**
   * The title diaplayed in the {@link #printUsage()} method.
   */
  String title;

  /**
   * Add a new argument with an explicit value.
   * @param name the name of the argument.
   * @param usage a string describing the argument's usage.
   * @return return this object.
   */
  @SuppressWarnings("unchecked")
  public T addArg(final String name, final String usage) {
    argDefs.put(name, new CLIArgument(name, false, usage));
    return (T) this;
  }


  /**
   * Add a new argument as a boollean switch.
   * @param name the name of the argument.
   * @param usage a string describing the argument's usage.
   * @return return this object.
   */
  @SuppressWarnings("unchecked")
  T addSwitch(final String name, final String usage) {
    argDefs.put(name, new CLIArgument(name, true, usage));
    return (T) this;
  }

  /**
   * Print usage of the arguments.
   * @return this object.
   */
  public abstract AbstractCLIArguments<?> printUsage();

  /**
   * Parse the specified command line arguments.
   * @param clArgs the arguments to parse.
   * @throws Exception if any error occurs.
   * @return this object.
   */
  public abstract AbstractCLIArguments<?> parseArguments(final String...clArgs)  throws Exception;

  /**
   * Print an error to the console.
   * @param message an optional message ot dispaly.
   * @param t an optional throwable.
   * @param clArgs the list of arguments being parsed.
   */
  void printError(final String message, final Throwable t, final String...clArgs) {
    System.out.println("Error found parsing the arguments " + Arrays.asList(clArgs));
    if (message != null) System.out.println(message);
    if (t != null) System.out.println(ExceptionUtils.getStackTrace(t));
    printUsage();
  }

  /**
   * Get the title diaplayed in the {@link #printUsage()} method.
   * @return the title as a string.
   */
  public String getTitle() {
    return title;
  }

  /**
   * Set the title diaplayed in the {@link #printUsage()} method.
   * @param title the title as a string.
   * @return this object.
   */
  @SuppressWarnings("unchecked")
  public T setTitle(final String title) {
    this.title = title;
    return (T) this;
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    for (Map.Entry<String, CLIArgument> entry: argDefs.entrySet()) {
      String key = entry.getKey();
      String value = getString(key);
      if (value != null) {
        sb.append("  ").append(key);
        if (!entry.getValue().isSwitch()) sb.append(" ").append(value);
        sb.append('\n');
      }
    }
    return sb.toString();
  }
}
