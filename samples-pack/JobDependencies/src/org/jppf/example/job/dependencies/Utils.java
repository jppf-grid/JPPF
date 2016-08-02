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

package org.jppf.example.job.dependencies;

import java.io.*;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * A collection of utility methods.
 * @author Laurent Cohen
 */
public class Utils {
  /**
   * The date format used in print statements.
   */
  private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("YYYY-MM-dd HH:mm:ss.SSS");
  /**
   * A constant representing an empty set of dependencies.
   */
  private static final String[] NO_DEPENDENCY = new String[0];

  /**
   * Print a formatted message to the output console, prefixed with the string 'handler' and a timestamp.
   * @param format the message format.
   * @param params the parameters for the formatted string, if any.
   */
  static void print(final String format, final Object...params) {
    System.out.printf("[" + DATE_FORMAT.format(new Date()) + "] " + format + "%n", params);
  }

  /**
   * Read the dependencies graph from a file.
   * @return a mapping of job ids to the list of ids of the jobs they depend on.
   * @throws Exception if any error occurs.
   */
  static List<DependencySpec> readDependencies() throws Exception {
    List<DependencySpec> result = new ArrayList<>();
    try (BufferedReader reader = new BufferedReader(new FileReader("dependency_graph.txt"))) {
      String s;
      while ((s = reader.readLine()) != null) {
        s = s.trim();
        // skip empty lines and comments
        if (s.isEmpty() || s.startsWith("#")) continue;
        // each line is in the form jobId ==> dependency1, ..., dependencyN | remove
        // the list of dependencies can be empty and the '| remove' flag is optional
        String[] keyValue = s.split("==>");
        if ((keyValue == null) || (keyValue.length <= 0)) continue;
        String jobId = keyValue[0].trim();
        String[] dependencies = NO_DEPENDENCY;
        boolean removeUponCompletion = false;
        if (keyValue.length > 1) {
          String val = keyValue[1].trim();
          // check if we have '| remove' at the end
          String[] values = val.split("\\|");
          if (values.length > 1) removeUponCompletion = true;
          dependencies = values[0].split(",");
          if ((dependencies != null) && (dependencies.length > 0)) {
            for (int i=0; i<dependencies.length; i++) dependencies[i] = dependencies[i].trim();
          }
        }
        result.add(new DependencySpec(jobId, dependencies, removeUponCompletion));
      }
    }
    return result;
  }
}
