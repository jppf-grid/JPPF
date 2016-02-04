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

package org.jppf.execute;

import java.util.concurrent.*;

/**
 * Interface for all thread managers.
 * @author Laurent Cohen
 * @exclude
 */
public interface ThreadManager
{
  /**
   * Set the size of the node's thread pool.
   * @param size the size as an int.
   */
  void setPoolSize(final int size);

  /**
   * Get the size of the node's thread pool.
   * @return the size as an int.
   */
  int getPoolSize();

  /**
   * Computes the total CPU time used by the execution threads.
   * @return a <code>NodeExecutionInfo</code> instance.
   */
  ExecutionInfo computeExecutionInfo();

  /**
   * Computes the CPU time used by thread identified by threadID.
   * @param threadID the thread ID.
   * @return a <code>NodeExecutionInfo</code> instance.
   */
  ExecutionInfo computeExecutionInfo(final long threadID);

  /**
   * Get the current cpu time for the thread identified by the specified id.
   * @param threadId the id of the thread to the cpu time from.
   * @return the cpu time as a long value.
   */
  long getCpuTime(final long threadId);

  /**
   * Get the priority assigned to the execution threads.
   * @return the priority as an int value.
   */
  int getPriority();

  /**
   * Update the priority of all execution threads.
   * @param newPriority the new priority to set.
   */
  void setPriority(final int newPriority);

  /**
   * Get the thread pool that really processes the tasks
   * @return a {@link ThreadPoolExecutor} instance.
   */
  ExecutorService getExecutorService();

  /**
   * Determines whether the thread cpu time measurement is supported and enabled.
   * @return true is cpu time measurement is enabled, false otherwise.
   */
  boolean isCpuTimeEnabled();

  /**
   * Use class loader in this thread manager.
   * @param classLoader  a <code>ClassLoader</code> instance.
   * @return a <code>UsedClassLoader</code> instance. Never return <code>null</code>.
   */
  UsedClassLoader useClassLoader(final ClassLoader classLoader);

  /**
   * Helper class for managing used class loaders.
   * @exclude
   */
  public static abstract class UsedClassLoader
  {
    /**
     * A <code>ClassLoader</code> instance.
     */
    private final ClassLoader classLoader;

    /**
     *
     * @param classLoader a <code>ClassLoader</code> instance.
     */
    protected UsedClassLoader(final ClassLoader classLoader)
    {
      this.classLoader = classLoader;
    }

    /**
     * Get a class loader instance.
     * @return a <code>ClassLoader</code> instance.
     */
    public ClassLoader getClassLoader() {
      return classLoader;
    }

    /**
     * Disposes usage for classLoader.
     */
    public abstract void dispose();
  }
}
