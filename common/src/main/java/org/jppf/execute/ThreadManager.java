/*
 * JPPF.
 * Copyright (C) 2005-2019 JPPF Team.
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

import org.jppf.utils.*;
import org.jppf.utils.configuration.*;
import org.slf4j.*;

/**
 * Interface for all thread managers.
 * @author Laurent Cohen
 * @exclude
 */
public interface ThreadManager {
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
   * @return a {@code NodeExecutionInfo} instance.
   */
  ExecutionInfo computeExecutionInfo();

  /**
   * Computes the CPU time used by thread identified by threadID.
   * @param threadID the thread ID.
   * @return a {@code NodeExecutionInfo} instance.
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
   * Create the thread manager instance. Default is {@link ThreadManagerThreadPool}.
   * @param config the configuration to get the thread manager properties from.
   * @param nbThreadsProperty the name of the property which configures the number of threads.
   * @return an instance of {@link ThreadManager}.
   */
  static ThreadManager newInstance(final TypedProperties config, JPPFProperty<Integer> nbThreadsProperty) {
    final Logger log = LoggerFactory.getLogger(ThreadManager.class);
    ThreadManager result = null;
    final int poolSize = computePoolSize(config, nbThreadsProperty);
    config.set(nbThreadsProperty, poolSize);
    final String s = config.get(JPPFProperties.THREAD_MANAGER_CLASS);
    if (!"default".equalsIgnoreCase(s) && !ThreadManagerThreadPool.class.getName().equals(s) && s != null) {
      try {
        final Class<?> clazz = Class.forName(s);
        final Object instance = ReflectionHelper.invokeConstructor(clazz, new Class[]{Integer.TYPE}, poolSize);
        if (instance instanceof ThreadManager) {
          result = (ThreadManager) instance;
          log.info("Using custom thread manager: {}", s);
        }
      } catch(final Exception e) {
        log.error(e.getMessage(), e);
      }
    }
    if (result == null) {
      final long ttl = retrieveTTL(config, JPPFProperties.PROCESSING_THREADS_TTL);
      log.info("Using default thread manager with poolSize = {} and ttl = {}", poolSize, (ttl == Long.MAX_VALUE) ? "Long.MAX_VALUE" : ttl);
      return new ThreadManagerThreadPool(poolSize, ttl);
    }
    log.info("Node running {} processing thread{}", poolSize, poolSize > 1 ? "s" : "");
    final boolean cpuTimeEnabled = result.isCpuTimeEnabled();
    config.setBoolean("cpuTimeSupported", cpuTimeEnabled);
    log.info("Thread CPU time measurement is {}supported", cpuTimeEnabled ? "" : "not ");
    return result;
  }

  /**
   * Compute a pool size based on the specified configuration and size property.
   * @param config the config to read the property from.
   * @param nbThreadsProperty the property that configures the pool size.
   * @return the computed size.
   */
  static int computePoolSize(final TypedProperties config, JPPFProperty<Integer> nbThreadsProperty) {
    final int poolSize = config.get(nbThreadsProperty);
    return (poolSize <= 0) ? Runtime.getRuntime().availableProcessors() : poolSize;
  }

  /**
   * Compute a thread TTL based on the specified configuration and ttl property.
   * @param config the config to read the property from.
   * @param ttlProperty the property that configures the time to live of the threads.
   * @return the computed size.
   */
  static long retrieveTTL(final TypedProperties config, JPPFProperty<Long> ttlProperty) {
    final long ttl = config.get(ttlProperty);
    return (ttl <= 0) ? Long.MAX_VALUE : ttl;
  }

  /**
   * Use class loader in this thread manager.
   * @param classLoader  a {@code ClassLoader} instance.
   * @return a {@code UsedClassLoader} instance. Never return {@code null}.
   */
  UsedClassLoader useClassLoader(final ClassLoader classLoader);

  /**
   * Helper class for managing used class loaders.
   * @exclude
   */
  public static abstract class UsedClassLoader {
    /**
     * A {@code ClassLoader} instance.
     */
    private final ClassLoader classLoader;

    /**
     *
     * @param classLoader a {@code ClassLoader} instance.
     */
    protected UsedClassLoader(final ClassLoader classLoader) {
      this.classLoader = classLoader;
    }

    /**
     * Get a class loader instance.
     * @return a {@code ClassLoader} instance.
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
