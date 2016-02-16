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

package org.jppf.utils.cli;

import java.util.*;

import org.jppf.utils.*;

/**
 *
 * @author Laurent Cohen
 */
public class NamedArguments extends TypedProperties {
  /**
   * The argument definitions.
   */
  private final Map<String, NamedArg> argDefs = new LinkedHashMap<>();
  /**
   * The title diaplayed in the {@link #printUsage()} method.
   */
  private String title;

  /**
   * Add a new argument with an explicit value.
   * @param name the name of the argument.
   * @param usage a string describing the argument's usage.
   * @return return this object.
   */
  public NamedArguments addArg(final String name, final String usage) {
    argDefs.put(name, new NamedArg(name, true, usage));
    return this;
  }

  /**
   * Add a new argument as a boollean switch.
   * @param name the name of the argument.
   * @param usage a string describing the argument's usage.
   * @return return this object.
   */
  public NamedArguments addSwitch(final String name, final String usage) {
    argDefs.put(name, new NamedArg(name, false, usage));
    return this;
  }

  /**
   * Print usage of the arguments.
   * @return this object.
   */
  public NamedArguments printUsage() {
    if (title != null) System.out.println(title);
    int maxLen = 0;
    for (NamedArg arg: argDefs.values()) {
      String s = arg.isSwitch() ? arg.getName() : arg.getName() + " <value>";
      if (s.length() > maxLen) maxLen = s.length();
    }
    String format = "%" + maxLen + "s : %s%n";
    for (Map.Entry<String, NamedArg> entry: argDefs.entrySet()) {
      NamedArg arg = entry.getValue();
      String s = arg.isSwitch() ? arg.getName() : arg.getName() + " <value>";
      System.out.printf(format, s, arg.getUsage());
    }
    return this;
  }

  /**
   * Parse the specified command line arguments.
   * @param clArgs the arguments to parse.
   * @throws Exception if any error occurs.
   * @return this object.
   */
  public NamedArguments parseArguments(final String...clArgs)  throws Exception {
    boolean end = false;
    int pos = 0;
    try {
      while (pos < clArgs.length) {
        String name = clArgs[pos++];
        NamedArg arg = argDefs.get(name);
        if (arg == null) throw new IllegalArgumentException("Unknown argument: " + name);
        if (!arg.isSwitch()) setBoolean(name, true);
        else setString(name, clArgs[pos++]);
      }
    } catch (Exception e) {
      printError(null, e, clArgs);
      throw e;
    }
    return this;
  }

  /**
   * Print an error to the console.
   * @param message an optional message ot dispaly.
   * @param t an optional throwable.
   * @param clArgs the list of arguments being parsed.
   */
  private void printError(final String message, final Throwable t, final String...clArgs) {
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
  public NamedArguments setTitle(final String title) {
    this.title = title;
    return this;
  }
}
