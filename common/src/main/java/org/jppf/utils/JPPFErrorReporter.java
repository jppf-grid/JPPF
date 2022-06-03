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
package org.jppf.utils;

import java.util.*;

/**
 * Utility class used to collect error and warning messages for various operations,
 * including XML parsing and validation.
 * @author Laurent Cohen
 * @exclude
 */
public class JPPFErrorReporter {
  /**
   * A list of collected warnings.
   */
  public final List<String> warnings = new ArrayList<>();
  /**
   * A list of collected errors.
   */
  public final List<String> errors = new ArrayList<>();
  /**
   * A list of fatal (non recoverable) errors.
   */
  public final List<String> fatalErrors = new ArrayList<>();
  /**
   * A name used to identify this error reporter.
   */
  public final String name;

  /**
   * Initialize this error reporter with the specified name.
   * @param name a name used to identify this error reporter.
   */
  public JPPFErrorReporter(final String name) {
    this.name = name;
  }

  /**
   * Get a string concatenating all the error messages.
   * @return a string.
   */
  public String allErrorsAsStrings() {
    return concatenateMessages(errors);
  }

  /**
   * Get a string concatenating all the fatal error messages.
   * @return a string.
   */
  public String allFatalErrorsAsStrings() {
    return concatenateMessages(fatalErrors);
  }

  /**
   * Get a string concatenating all the warning messages.
   * @return a string.
   */
  public String allWarningsAsStrings() {
    return concatenateMessages(warnings);
  }

  /**
   * Concatenate all string messages in a list into a single string.
   * @param list the list to get the messages to concatenate from.
   * @return a concatenation of all messages in the list, separated by new lines.
   */
  protected String concatenateMessages(final List<String> list) {
    final StringBuilder sb = new StringBuilder();
    for (int i=0; i<list.size(); i++) {
      if (i > 0) sb.append('\n');
      sb.append(list.get(i));
    }
    return sb.toString();
  }
}
