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

package org.jppf.logging.jdk;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.logging.*;

import org.jppf.utils.*;

/**
 * Formats log records in format yyyy-MM-dd hh:mm:ss.SSS [LEVEL][thread name][package.ClassName.method()]: message.
 * @author Laurent Cohen
 */
public class JPPFLogFormatter extends Formatter {
  /**
   * Date format used in log entries.
   */
  private final SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS ");

  /**
   * Format a log record.
   * @param record the record to format.
   * @return a string representation of the record according to this formatter.
   * @see java.util.logging.Formatter#format(java.util.logging.LogRecord)
   */
  @Override
  public String format(final LogRecord record) {
    StringBuilder sb = new StringBuilder();
    sb.append(sdf.format(new Date(record.getMillis())));
    sb.append('[');
    String name = record.getLevel().getName();
    sb.append(name);
    // pad to 7 chars
    for (int i = 0; i < 7 - name.length(); i++)
      sb.append(' ');
    sb.append(']');

    sb.append('[');
    String s = "" + Thread.currentThread().getName();
    sb.append(StringUtils.padRight(s, ' ', 20, true));
    sb.append(']');

    sb.append('[');
    /* String s = record.getSourceClassName();
     * if (s != null) sb.append(s); */
    s = record.getSourceClassName();
    String shortName = getShortName(s);
    StackTraceElement[] elts = new Throwable().getStackTrace();
    StackTraceElement elt = null;
    for (StackTraceElement elt1 : elts) {
      if (getShortName(elt1.getClassName()).equals(shortName)) {
        elt = elt1;
        break;
      }
    }
    if (elt != null) {
      sb.append(elt.getClassName());
      if (elt.getMethodName() != null) sb.append('.').append(elt.getMethodName());
      sb.append('(');
      if (elt.getLineNumber() >= 0) sb.append(elt.getLineNumber());
      sb.append(')');
    } else {
      if (s != null) sb.append(s);
      s = record.getSourceMethodName();
      if (s != null) sb.append('.').append(s).append("()");
    }
    sb.append(']');
    sb.append(": ");
    s = record.getMessage();
    if (s != null) sb.append(s);
    Object[] params = record.getParameters();
    if (params != null) for (Object o : params)
      sb.append('|').append(o);
    sb.append('\n');
    if (record.getThrown() != null) sb.append(ExceptionUtils.getStackTrace(record.getThrown())).append("\n");
    return sb.toString();
  }

  /**
   * Get the short name of a class, without the package name.
   * @param fqn - the fully qualified name of the class.
   * @return a string representing the short name of a class.
   */
  private static String getShortName(final String fqn) {
    if (fqn == null) return "";
    int idx = fqn.lastIndexOf('.');
    return idx >= 0 ? fqn.substring(idx + 1) : fqn;
  }
}
