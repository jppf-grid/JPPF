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

import java.io.Closeable;

/**
 * Interface for printing a {@link ThreadDump}.
 * @author Laurent Cohen
 */
public interface ThreadDumpWriter extends Closeable
{
  /**
   * Print the specified string without line terminator.
   * @param message th string to print.
   */
  void printString(String message);
  /**
   * Print the deadlocked threads information.
   * @param threadDump the thread dump which provides the information to print.
   */
  void printDeadlocks(ThreadDump threadDump);
  /**
   * Print information about a thread.
   * @param threadInformation the information to print.
   */
  void printThread(ThreadInformation threadInformation);
  /**
   * Print the specified thread dump.
   * @param threadDump the thread dump to print.
   */
  void printThreadDump(ThreadDump threadDump);
}
