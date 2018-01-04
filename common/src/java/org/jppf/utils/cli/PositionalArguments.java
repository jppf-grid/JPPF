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
public class PositionalArguments extends AbstractCLIArguments<PositionalArguments> {
  /**
   * Explicit serialVersionUID.
   */
  private static final long serialVersionUID = 1L;

  @Override
  public PositionalArguments printUsage() {
    if (title != null) System.out.println(title);
    int maxLen = 0;
    for (final CLIArgument arg: argDefs.values()) {
      final int len = arg.getName().length();
      if (len > maxLen) maxLen = len;
    }
    final String format = "%-" + maxLen + "s : %s%n";
    for (final Map.Entry<String, CLIArgument> entry: argDefs.entrySet()) {
      final CLIArgument arg = entry.getValue();
      System.out.printf(format, arg.getName(), arg.getUsage());
    }
    return this;
  }

  @Override
  public PositionalArguments parseArguments(final String...clArgs)  throws Exception {
    int pos = 0;
    try {
      //for (Map.Entry<String, CLIArgument> entry: argDefs.entrySet()) {
      for (int i=0; i<argDefs.size(); i++) {
        if (pos >= clArgs.length) break;
        setString(Integer.toString(pos), clArgs[pos++]);
      }
    } catch (final Exception e) {
      printError(null, e, clArgs);
      throw e;
    }
    return this;
  }

  /**
   * Get the value of the argument at the specified position.
   * @param position the posiiton of the argument ot lookup.
   * @return the argument value, or {@code null} if it is not found.
   */
  public String getString(final int position) {
    return getString(Integer.toString(position));
  }
}
