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

package org.jppf.management.diagnostics;

import java.io.*;
import java.util.*;

import org.jppf.utils.streams.StreamUtils;

/**
 * This class prints a thread dump nicely formatted as HTML to a character stream.
 * <br/>This is used by the adminsitration console to display a thread dump for a selected driver or node.
 * @author Laurent Cohen
 */
public class HTMLThreadDumpWriter extends AbstractThreadDumpWriter {
  /**
   * End of line character sequence.
   */
  private static final String BR = "<br/>\n";
  /**
   * The title given to the printed thread dump.
   */
  private final String title;
  /**
   * The font size used in the html rendering of the thread dump.
   */
  private int fontSize = 12;
  /**
   * Whether to add the html and body tags.
   */
  final boolean includeBody;

  /**
   * Intiialize this printer with the specified writer.
   * @param writer the writer to print to.
   * @param title the tittle given for the output.
   */
  public HTMLThreadDumpWriter(final Writer writer, final String title) {
    this(writer, "&nbsp;&nbsp;", 12, true);
  }

  /**
   * Intiialize this printer with the specified writer.
   * @param writer the writer to print to.
   * @param title the title given for the output.
   * @param fontSize the font size used in the html rendering of the thread dump.
   * @param includeBody whether to add the html and body tags.
   */
  public HTMLThreadDumpWriter(final Writer writer, final String title, final int fontSize, final boolean includeBody) {
    super(writer, "&nbsp;&nbsp;");
    this.title = title;
    this.fontSize = fontSize;
    this.includeBody = includeBody;
  }

  @Override
  public void printDeadlocks(final ThreadDump threadDump) {
    long[] ids = threadDump.getDeadlockedThreads();
    if ((ids == null) || (ids.length <= 0)) return;
    out.println("<hr><h3><font color='red'>Deadlock detected</font></h3>");
    Map<Long, ThreadInformation> threadsMap = threadDump.getThreads();
    for (long id : ids) {
      ThreadInformation ti = threadsMap.get(id);
      LockInformation li = ti.getLockInformation();
      ThreadInformation owner = threadsMap.get(ti.getLockOwnerId());
      out.println("- " + simpleName(ti) + " is waiting to lock " + simpleName(li) + " which is held by " + simpleName(owner) + BR);
    }
    out.println("<p><b>Stack trace information for the threads listed above</b>");
    for (long id : ids)
      printThread(threadsMap.get(id));
    out.print(BR);
    out.println("<hr>");
  }

  @Override
  public void printThread(final ThreadInformation thread) {
    StringBuilder sb = new StringBuilder();
    sb.append("<p><b>");
    sb.append("<span class='").append(threadClass(thread.getState())).append("'>");
    sb.append('"').append(thread.getName()).append('"').append(" - ").append(thread.getId());
    sb.append(" - state: ").append(thread.getState());
    sb.append(" - blocked count: ").append(thread.getBlockedCount());
    sb.append(" - blocked time: ").append(thread.getBlockedTime());
    sb.append(" - wait count: ").append(thread.getWaitCount());
    sb.append(" - wait time: ").append(thread.getWaitTime());
    if (thread.isSuspended()) sb.append(" - suspended");
    if (thread.isInNative()) sb.append(" - in native code");
    sb.append("</span>");
    sb.append("</b>").append(BR);
    incIndent();
    List<StackFrameInformation> stackTrace = thread.getStackTrace();
    if ((stackTrace != null) && !stackTrace.isEmpty()) {
      int count = 0;
      for (StackFrameInformation sfi : stackTrace) {
        sb.append(getIndent()).append("at ").append(sfi).append(BR);
        if ((count == 0) && (thread.getLockInformation() != null)) {
          sb.append(getIndent()).append("<span class='t_lock'>");
          sb.append("- waiting on ").append(simpleName(thread.getLockInformation()));
          sb.append("</span>").append(BR);
        }
        LockInformation li = sfi.getLock();
        if (li != null) {
          sb.append(getIndent()).append("<span class='t_lock'>");
          sb.append("- locked ").append(simpleName(li));
          sb.append("</span>").append(BR);
        }
        count++;
      }
    }
    List<LockInformation> synchronizers = thread.getOwnableSynchronizers();
    if ((synchronizers != null) && !synchronizers.isEmpty()) {
      sb.append("<p>").append(getIndent()).append("Locked ownable synchronizers:").append(BR);
      for (LockInformation li : synchronizers) {
        sb.append(getIndent()).append("- ").append(simpleName(li)).append(BR);
      }
    }
    decIndent();
    out.println(sb);
  }

  @Override
  public void printThreadDump(final ThreadDump threadDump) {
    if (includeBody) {
      out.println("<html>");
      out.println("<style type='text/css'>");
      out.println("body { font-size: " + fontSize + "pt; font-family: Arial, sans-serif; padding: 5px; }");
      out.println(".t_new        { background-color: white; }");
      out.println(".t_runnable   { background-color: #80FF80; }");
      out.println(".t_wait       { background-color: yellow; }");
      out.println(".t_time_wait  { background-color: yellow; }");
      out.println(".t_blocked    { background-color: #FF8080; }");
      out.println(".t_terminated { background-color: #FAFAFA; }");
      out.println(".t_lock { font-style: italic; font-size: " + (fontSize - 1) + "pt; }");
      out.println("</style>");
      out.println("<body>");
    }
    out.println("<h2>" + title + "</h2>");
    super.printThreadDump(threadDump);
    if (includeBody) {
      out.println("</body>");
      out.println("</html>");
    }
  }

  @Override
  public void close() throws IOException {
    out.close();
  }

  /**
   * Compute a CSS class from a thread state.
   * @param state the state to use.
   * @return a CSS class name.
   */
  private String threadClass(final Thread.State state) {
    switch (state) {
      case NEW:
        return "t_new";
      case RUNNABLE:
        return "t_runnable";
      case WAITING:
        return "t_wait";
      case TIMED_WAITING:
        return "t_time_wait";
      case BLOCKED:
        return "t_blocked";
      case TERMINATED:
        return "t_terminated";
    }
    return "";
  }

  /**
   * Print the specified thread dump directly to a string.
   * @param dump the thread dump to print.
   * @param title title given to the dump.
   * @param includeBody whether to add the html and body tags.
   * @param fontSize the size of the font used to write the thread dump.
   * @return the thread dump printed to an HTML string, or null if it could not be printed.
   */
  public static String printToString(final ThreadDump dump, final String title, final boolean includeBody, final int fontSize) {
    String result = null;
    HTMLThreadDumpWriter writer = null;
    try {
      StringWriter sw = new StringWriter();
      writer = new HTMLThreadDumpWriter(sw, title, fontSize, includeBody);
      writer.printThreadDump(dump);
      result = sw.toString();
    } finally {
      if (writer != null) StreamUtils.closeSilent(writer);
    }
    return result;
  }
}
