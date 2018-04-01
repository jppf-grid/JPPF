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

package org.jppf.execute;

/**
 * This class manages the thread for the node's execution manager.
 * @author Laurent Cohen
 * @author Martin JANDA
 */
public abstract class AbstractThreadManager implements ThreadManager {
  /**
   * Prefix used for the names of the threads created by the executor.
   */
  public static final String THREAD_NAME_PREFIX = "Processing";
  /**
   * Determines whether CPU time measurement is supported for the current JVM.
   */
  private final static boolean cpuTimeEnabled = determineCpuTimeEnabled();

  /**
   * Initialize this execution manager with the specified node.
   */
  protected AbstractThreadManager() {
  }

  @Override
  public ExecutionInfo computeExecutionInfo() {
    final ExecutionInfo info = new ExecutionInfo();
    final long[] ids = getThreadIds();
    for (final long id: ids) info.add(computeExecutionInfo(id));
    return info;
  }

  @Override
  public ExecutionInfo computeExecutionInfo(final long threadID) {
    return CpuTimeCollector.computeExecutionInfo(threadID);
  }

  /**
   * Get the ids of all managed threads.
   * @return the ids as an array of long values, may be an empty but never null array.
   */
  protected abstract long[] getThreadIds();

  @Override
  public long getCpuTime(final long threadId) {
    return CpuTimeCollector.getCpuTime(threadId);
  }

  @Override
  public boolean isCpuTimeEnabled() {
    return cpuTimeEnabled;
  }

  /**
   * Determine whether CPU time measurement is supported for the current JVM.
   * @return {@code true} if CPU time measurement is supported, {@code false} otherwise.
   */
  private static boolean determineCpuTimeEnabled() {
    try {
      return CpuTimeCollector.isCpuTimeEnabled();
    } catch(@SuppressWarnings("unused") final Exception|NoClassDefFoundError e) {
      return false;
    }
  }
}
