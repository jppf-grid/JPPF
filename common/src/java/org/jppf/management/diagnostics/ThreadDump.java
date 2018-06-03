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

import java.io.Serializable;
import java.lang.management.*;
import java.util.*;

/**
 * This class encapsulates a JVM thread dump, including dealocks information when available.
 * @author Laurent Cohen
 */
public class ThreadDump implements Serializable {
  /**
   * Explicit serialVersionUID.
   */
  private static final long serialVersionUID = 1L;
  /**
   * Information on the threads.
   */
  private final Map<Long, ThreadInformation> threads;
  /**
   * The ids of the deadlocked threads.
   */
  private final long[] deadlockedThreads;

  /**
   * Create this thread dump from the specified thread mxbean.
   * @param threadMXBean the thread MXBean to get the information from.
   */
  public ThreadDump(final ThreadMXBean threadMXBean) {
    final ThreadInfo[] tis = threadMXBean.dumpAllThreads(threadMXBean.isObjectMonitorUsageSupported(), threadMXBean.isSynchronizerUsageSupported());
    if ((tis == null) || (tis.length <= 0)) threads = null;
    else {
      threads = new TreeMap<>();
      for (final ThreadInfo ti: tis) threads.put(ti.getThreadId(), new ThreadInformation(ti));
    }
    final long[] ids = threadMXBean.isSynchronizerUsageSupported() ? threadMXBean.findDeadlockedThreads() : null;
    this.deadlockedThreads = (ids == null) || (ids.length <= 0) ? null : ids;
  }

  /**
   * Get information on the threads.
   * @return a mapping of {@link ThreadInformation} objects to their thread id, or <code>null</code> if no thread information is available.
   */
  public Map<Long, ThreadInformation> getThreads() {
    return threads;
  }

  /**
   * Get the ids of the deadlock threads, if any.
   * @return the ids as an array of <code>long</code> values, or <code>null</code> if none exists.
   */
  public long[] getDeadlockedThreads() {
    return deadlockedThreads;
  }

  @Override
  public String toString() {
    return TextThreadDumpWriter.printToString(this, "ThreadDump");
  }

  /**
   * Print this thread dump to a plain text formatted string.
   * @param title the title given to the thread dump.
   * @return the thread dump printed to a plain test formatted string, or null if it could not be printed.
   */
  public String toPlainTextString(final String title) {
    return TextThreadDumpWriter.printToString(this, title);
  }

  /**
   * Print this thread dump to an HTML formatted string.
   * @param title the title given to the thread dump.
   * @param includeBody whether to add the &lt;html&gt; and &lt;body&gt; tags.
   * @param fontSize the size of the font used to write the thread dump.
   * @return the thread dump printed to an HTML string, or null if it could not be printed.
   */
  public String toHTMLString(final String title, final boolean includeBody, final int fontSize) {
    return HTMLThreadDumpWriter.printToString(this, title, includeBody, fontSize);
  }
}
