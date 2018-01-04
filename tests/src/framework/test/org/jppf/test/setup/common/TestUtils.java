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

package test.org.jppf.test.setup.common;

import java.text.SimpleDateFormat;
import java.util.Date;

import org.slf4j.Logger;

/**
 * Collection of utility methods used in the tests.
 * @author Laurent Cohen
 */
public class TestUtils {
  /**
   * Used to format timestamps in the std and err outputs.
   */
  private static final SimpleDateFormat SDF = new SimpleDateFormat("hh:mm:ss.SSS");

  /**
   * Print to System.out the specified formatted message.
   * @param format the format to use as in {@link String#format()}.
   * @param params the parmaters of the formatted string.
   */
  public static void printf(final String format, final Object...params) {
    printf(null, format, params);
  }

  /**
   * Print to System.out, and optionally log the specified formatted message.
   * @param log an optional logger, may be {@code null}.
   * @param format the format to use as in {@link String#format()}.
   * @param params the parmaters of the formatted string.
   */
  public static void printf(final Logger log, final String format, final Object...params) {
    final String formatted = String.format(format, params);
    final String s = prefixWithTimestamp("  client", SDF, formatted);
    System.out.println(s);
    if (log != null) log.info(formatted);
  }

 /**
  * Add a specified prefix and timestamp to the specified string.
  * @param prefix the prefix to add, may be {@code null}.
  * @param sdf the format for the time stamp.
  * @param formatted the string to add to.
  * @return a new string with the specified prefix and timestamp added to the begining.
  */
 public static String prefixWithTimestamp(final String prefix, final SimpleDateFormat sdf, final String formatted) {
   final StringBuilder sb = new StringBuilder();
   if ((prefix != null) && !"".equals(prefix)) sb.append('[').append(prefix).append(']').append(' ');
   if (sdf != null) {
     synchronized(sdf) {
       sb.append('[').append(sdf.format(new Date())).append(']').append(' ');
     }
   }
   sb.append(formatted);
   return sb.toString();
 }
}
