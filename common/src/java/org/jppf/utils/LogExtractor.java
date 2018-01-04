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

package org.jppf.utils;

import java.io.*;
import java.util.regex.Pattern;

/**
 *
 * @author Laurent Cohen
 */
public class LogExtractor {
  /**
   * The input file.
   */
  private final File inFile;
  /**
   * The output file.
   */
  private final File outFile;
  /**
   * 
   */
  private final Pattern pattern;

  /**
   *
   * @param inFilename the input file path.
   * @param outFilename the output file path.
   * @param searchString the string to search for in the file.
   */
  public LogExtractor(final String inFilename, final String outFilename, final String searchString) {
    super();
    this.inFile = new File(inFilename);
    this.outFile = new File(outFilename);
    this.pattern = Pattern.compile(searchString);
  }

  /**
   * Extract the matching lines form the input file and put them in the output file.
   */
  public void extract() {
    try (BufferedReader in = new BufferedReader(new FileReader(inFile));
      BufferedWriter out = new BufferedWriter(new FileWriter(outFile))) {
      String line = null;
      int count = 0;
      int nbLinesMatching = 0;
      while ((line = in.readLine()) != null) {
        count++;
        if (pattern.matcher(line).matches()) {
          nbLinesMatching++;
          out.append(line).append('\n');
        }
      }
      System.out.printf("found %,d lines matching pattern '%s' out of %,d in file '%s'%n", nbLinesMatching, pattern, count, inFile);
    } catch (final Exception e) {
      e.printStackTrace();
    }
  }

  /**
   *
   * @param args .
   */
  public static void main(final String[] args) {
    new LogExtractor(args[0], args[1], args[2]).extract();
  }
}
