/*
 * JPPF.
 * Copyright (C) 2005-2015 JPPF Team.
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
import org.jppf.utils.streams.StreamUtils;

/**
 * This class prints a thread dump nicely formatted as plain text to a character stream.
 * @author Laurent Cohen
 */
public class TextThreadDumpWriter extends AbstractThreadDumpWriter
{
  /**
   * The new line sequence.
   */
  private static final String BR = "\n";
  /**
   * Title to print for the thread dump.
   */
  private final String title;

  /**
   * Intiialize this printer with the specified writer.
   * @param writer the writer to print to.
   * @param title the tittle given for the output.
   */
  public TextThreadDumpWriter(final Writer writer, final String title)
  {
    super(writer, "  ");
    this.title = title == null ? "Thread dump" : title;
  }

  @Override
  public void printDeadlocks(final ThreadDump threadDump)
  {
    String hr = StringUtils.padRight("", '-', 80);
    long[] ids = threadDump.getDeadlockedThreads();
    if ((ids == null) || (ids.length <= 0)) return;
    out.println(hr);
    out.println("Deadlock detected" + BR);
    Map<Long, ThreadInformation> threadsMap = threadDump.getThreads();
    for (long id: ids)
    {
      ThreadInformation ti = threadsMap.get(id);
      LockInformation li = ti.getLockInformation();
      ThreadInformation owner = threadsMap.get(ti.getLockOwnerId());
      out.println("- " + simpleName(ti) + " is waiting to lock " + simpleName(li) + " which is held by " + simpleName(owner));
    }
    out.println("Stack trace information for the threads listed above" + BR);
    for (long id: ids) printThread(threadsMap.get(id));
    out.println(hr + BR);
  }

  @Override
  public void printThread(final ThreadInformation thread)
  {
    StringBuilder sb = new StringBuilder();
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
    List<StackFrameInformation> stackTrace = thread.getStackTrace();
    if ((stackTrace != null) && !stackTrace.isEmpty())
    {
      int count = 0;
      for (StackFrameInformation sfi: stackTrace)
      {
        sb.append(getIndent()).append("at ").append(sfi).append(BR);
        if ((count == 0) && (thread.getLockInformation() != null))
          sb.append(getIndent()).append("- waiting on ").append(simpleName(thread.getLockInformation())).append(BR);
        LockInformation li = sfi.getLock();
        if (li != null) sb.append(getIndent()).append("- locked ").append(simpleName(li)).append(BR);
        count++;
      }
    }
    List<LockInformation> synchronizers = thread.getOwnableSynchronizers();
    if ((synchronizers != null) && !synchronizers.isEmpty())
    {
      sb.append(BR).append(getIndent()).append("Locked ownable synchronizers:").append(BR);
      for (LockInformation li: synchronizers)
      {
        sb.append(getIndent()).append("- ").append(simpleName(li)).append(BR);
      }
    }
    decIndent();
    out.println(sb);
  }

  @Override
  public void printThreadDump(final ThreadDump threadDump)
  {
    String hr = StringUtils.padRight("", '-', title.length());
    out.println(hr);
    out.println(title);
    out.println(hr + BR);
    super.printThreadDump(threadDump);
  }

  @Override
  public void close() throws IOException
  {
    out.close();
  }

  /**
   * Print the specified thread dump directly to a plain text string.
   * @param dump the thread dump to print.
   * @param title title given to the dump.
   * @return the thread dump printed to a string, or null if it could not be printed.
   */
  public static String printToString(final ThreadDump dump, final String title)
  {
    String result = null;
    ThreadDumpWriter writer = null;
    try
    {
      StringWriter sw = new StringWriter();
      writer = new TextThreadDumpWriter(sw, title);
      writer.printThreadDump(dump);
      result = sw.toString();
    }
    finally
    {
      if (writer != null) StreamUtils.closeSilent(writer);
    }
    return result;
  }
}
