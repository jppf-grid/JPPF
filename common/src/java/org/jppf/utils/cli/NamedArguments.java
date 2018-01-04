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

import java.util.Map;

/**
 *
 * @author Laurent Cohen
 */
public class NamedArguments extends AbstractCLIArguments<NamedArguments> {
  /**
   * Explicit serialVersionUID.
   */
  private static final long serialVersionUID = 1L;

  @Override
  public NamedArguments addSwitch(final String name, final String usage) {
    return super.addSwitch(name, usage);
  }

  @Override
  public NamedArguments printUsage() {
    if (title != null) System.out.println(title);
    int maxLen = 0;
    for (final CLIArgument arg: argDefs.values()) {
      final String s = arg.isSwitch() ? arg.getName() : arg.getName() + " <value>";
      if (s.length() > maxLen) maxLen = s.length();
    }
    final String format = "%-" + maxLen + "s : %s%n";
    for (final Map.Entry<String, CLIArgument> entry: argDefs.entrySet()) {
      final CLIArgument arg = entry.getValue();
      final String s = arg.isSwitch() ? arg.getName() : arg.getName() + " <value>";
      System.out.printf(format, s, arg.getUsage());
    }
    return this;
  }

  @Override
  public NamedArguments parseArguments(final String...clArgs)  throws Exception {
    int pos = 0;
    try {
      while (pos < clArgs.length) {
        final String name = clArgs[pos++];
        final CLIArgument arg = argDefs.get(name);
        if (arg == null) throw new IllegalArgumentException("Unknown argument: " + name);
        if (arg.isSwitch()) setBoolean(name, true);
        else setString(name, clArgs[pos++]);
      }
    } catch (final Exception e) {
      printError(null, e, clArgs);
      throw e;
    }
    return this;
  }
}
