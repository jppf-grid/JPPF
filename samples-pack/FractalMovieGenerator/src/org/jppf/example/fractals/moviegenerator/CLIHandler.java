/*
 * JPPF.
 * Copyright (C) 2005-2014 JPPF Team.
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

package org.jppf.example.fractals.moviegenerator;

import java.util.*;

/**
 * Command-line interface handler.
 * @author Laurent Cohen
 */
public class CLIHandler {
  /**
   * Process and parse the command line arguments.
   * This method will detect and display all errors it can detect:
   * missing, misplaced or unknown options, along with invalid numerical valiues.
   * @param args the arguments to prcess.
   * @return a mapping of parameter names to their value.
   * @throws Exception if any error occurs.
   */
  public Map<String, Object> processArguments(final String[] args) throws Exception {
    Map<String, Object> map = new HashMap<>();
    if (args == null) throwMessage("Error: no command-line arguments are provided");
    int count = 0;
    List<String> options = Arrays.asList(new String[] {"-i", "-o", "-f", "-t"});
    List<String> errors = new ArrayList<>();
    while (count <args.length) {
      String s = args[count].toLowerCase();
      if ("-h".equals(s) || "-?".equals(s)) {
        displayUsage();
        System.exit(0);
      } else if ("-i".equals(s) || "-o".equals(s)) {
        if (count++ < args.length-1) map.put(s, args[count++]);
        else errors.add("missing value for option '" + s + "'");
      } else if ("-f".equals(s) || "-t".equals(s)) {
        if (count++ < args.length-1) {
          String s2 = args[count++];
          int n = -1;
          try {
            n = Integer.valueOf(s2);
            map.put(s, n);
          } catch(NumberFormatException e) {
            errors.add("invalid number format for the value '" + s2 + "' of option '" + s + "'");
          }
        }
        else errors.add("missing value for option '" + s + "'");
      } else {
        errors.add("unknown or misplaced argument '" + s + "'");
        count++;
      }
    }
    for (String s: options) {
      if (!map.containsKey(s)) errors.add("missing or misplaced option '" + s +"'");
    }
    if (!errors.isEmpty()) {
      StringBuilder sb = new StringBuilder();
      sb.append('\n').append("command line arguments:");
      for (String s: args) sb.append(' ').append(s);
      sb.append("\n found errors:");
      for (String s: errors) sb.append("\n- ").append(s);
      throwMessage(sb.toString());
    }
    return map;
  }

  /**
   * Display this program's usage followed by one or more error message(s).
   * @param message the error message(s) to display.
   */
  private void throwMessage(final String message) {
    displayUsage();
    System.out.println(message);
    System.exit(1);
  }

  /**
   * Display usage intructions for this program.
   */
  private void displayUsage() {
    System.out.println("usage:");
    System.out.println("Windows: run.bat [option, ...]");
    System.out.println("Linux: ./run.sh [option, ...]");
    System.out.println("There are two possible sets of options:");
    System.out.println("1. <run_cmd> -h|?");
    System.out.println("  display this screen and exit");
    System.out.println("2. <run_cmd> -i <input_file> -o <output_file> -f <frame_rate> -t <trans_time>");
    System.out.println("where:");
    System.out.println("  input_file: a csv record file produced by the mandelbrot fractal sample");
    System.out.println("  output_file: path to the generated movie file");
    System.out.println("    the .avi extension is added if needed");
    System.out.println("  frame_rate: number of frames per second");
    System.out.println("  trans_time: the duration (in seconds) of a transition");
    System.out.println("    between 2 records in the input file");
    System.out.println("note: the total number of frames in the generated movie is equal to");
    System.out.println("  (nb_input_records-1) * frame_rate * trans_time");
  }
}
