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

package org.jppf.management.diagnostics;

import java.io.*;
import java.util.*;

import org.jppf.utils.StringUtils;
import org.slf4j.*;

/**
 * This class prints a thread dump nicely formatted as plain text to a character stream.
 * @author Laurent Cohen
 * @exclude
 */
public class TextThreadDumpWriter extends AbstractThreadDumpWriter {
  /**
   * Logger for this class.
   */
  private static final Logger log = LoggerFactory.getLogger(TextThreadDumpWriter.class);
  /**
   * The new line sequence.
   */
  private static final String BR = "\n";

  /**
   * Intiialize this printer with the specified writer.
   * @param writer the writer to print to.
   * @param title the tittle given for the output.
   */
  public TextThreadDumpWriter(final Writer writer, final String title) {
    super(writer, title, "  ");
  }

  @Override
  public void printDeadlocks(final ThreadDump threadDump) {
    final String hr = StringUtils.padRight("", '-', 80);
    final long[] ids = threadDump.getDeadlockedThreads();
    if ((ids == null) || (ids.length <= 0)) return;
    out.println(hr);
    out.println("Deadlock detected" + BR);
    final Map<Long, ThreadInformation> threadsMap = threadDump.getThreads();
    for (final long id: ids) {
      final ThreadInformation ti = threadsMap.get(id);
      final LockInformation li = ti.getLockInformation();
      final ThreadInformation owner = threadsMap.get(ti.getLockOwnerId());
      out.println("- " + simpleName(ti) + " is waiting to lock " + simpleName(li) + " which is held by " + simpleName(owner));
    }
    out.println("Stack trace information for the threads listed above" + BR);
    for (final long id: ids) printThread(threadsMap.get(id));
    out.println(hr + BR);
  }

  @Override
  public void printThread(final ThreadInformation thread) {
    final StringBuilder sb = new StringBuilder();
    sb.append("\"").append(thread.getName()).append('"').append(" - ").append(thread.getId());
    sb.append(" - state: ").append(thread.getState());
    sb.append(" - blocked count: ").append(thread.getBlockedCount());
    sb.append(" - blocked time: ").append(thread.getBlockedTime());
    sb.append(" - wait count: ").append(thread.getWaitCount());
    sb.append(" - wait time: ").append(thread.getWaitTime());
    if (thread.isSuspended()) sb.append(" - suspended");
    if (thread.isInNative()) sb.append(" - in native code");
    sb.append(BR);
    incIndent();
    final List<StackFrameInformation> stackTrace = thread.getStackTrace();
    if ((stackTrace != null) && !stackTrace.isEmpty()) {
      int count = 0;
      for (final StackFrameInformation sfi: stackTrace) {
        sb.append(getIndent()).append("at ").append(sfi).append(BR);
        if ((count == 0) && (thread.getLockInformation() != null)) sb.append(getIndent()).append("- waiting on ").append(simpleName(thread.getLockInformation())).append(BR);
        final LockInformation li = sfi.getLock();
        if (li != null) sb.append(getIndent()).append("- locked ").append(simpleName(li)).append(BR);
        count++;
      }
    }
    final List<LockInformation> synchronizers = thread.getOwnableSynchronizers();
    if ((synchronizers != null) && !synchronizers.isEmpty()) {
      sb.append(BR).append(getIndent()).append("Locked ownable synchronizers:").append(BR);
      for (final LockInformation li: synchronizers) {
        sb.append(getIndent()).append("- ").append(simpleName(li)).append(BR);
      }
    }
    decIndent();
    out.println(sb);
  }

  @Override
  public void printThreadDump(final ThreadDump threadDump) {
    final String hr = StringUtils.padRight("", '-', title.length());
    out.println(hr);
    out.println(title);
    out.println(hr + BR);
    super.printThreadDump(threadDump);
  }

  /**
   * Print the specified thread dump directly to a plain text string.
   * @param dump the thread dump to print.
   * @param title title given to the dump.
   * @return the thread dump printed to a string, or null if it could not be printed.
   */
  public static String printToString(final ThreadDump dump, final String title) {
    String result = null;
    try (final StringWriter sw = new StringWriter(); final ThreadDumpWriter writer = new TextThreadDumpWriter(sw, title)) {
      writer.printThreadDump(dump);
      result = sw.toString();
    } catch(final Exception e) {
      log.error(e.getMessage(), e);
    }
    return result;
  }
}
