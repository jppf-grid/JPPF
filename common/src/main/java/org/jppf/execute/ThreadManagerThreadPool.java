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
import java.util.concurrent.atomic.AtomicInteger;

import org.jppf.utils.concurrent.JPPFThreadFactory;
import org.slf4j.*;

/**
 * This class manages the thread pool pool for the node's execution manager.
 * @author Laurent Cohen
 * @author Martin JANDA
 */
public class ThreadManagerThreadPool extends AbstractThreadManager {
  /**
   * Logger for this class.
   */
  private static Logger log = LoggerFactory.getLogger(ThreadManagerThreadPool.class);
  /**
   * Instance count for this class.
   */
  private static final AtomicInteger instanceCount = new AtomicInteger(0);
  /**
   * The thread pool that really processes the tasks
   */
  private final ThreadPoolExecutor threadPool;
  /**
   * The factory used to create thread in the pool.
   */
  private final JPPFThreadFactory threadFactory;

  /**
   * Initialize this thread manager.
   */
  public ThreadManagerThreadPool() {
    this(Runtime.getRuntime().availableProcessors(), Long.MAX_VALUE);
  }

  /**
   * Initialize this thread manager.
   * @param poolSize the initial size of the thread pool.
   */
  public ThreadManagerThreadPool(final int poolSize) {
    this(poolSize, Long.MAX_VALUE);
  }

  /**
   * Initialize this thread manager.
   * @param poolSize the initial size of the thread pool.
   * @param ttl the time to live of the threads, in seconds.
   */
  public ThreadManagerThreadPool(final int poolSize, final long ttl) {
    super();
    threadFactory = new JPPFThreadFactory(String.format("%s-%03d", THREAD_NAME_PREFIX, instanceCount.incrementAndGet()), isCpuTimeEnabled());
    final LinkedBlockingQueue<Runnable> queue = new LinkedBlockingQueue<>();
    threadPool = new ThreadPoolExecutor(poolSize, poolSize, ttl, TimeUnit.SECONDS, queue, threadFactory) {
      @Override
      protected <T> RunnableFuture<T> newTaskFor(final Runnable runnable, final T value) {
        final RunnableFuture<T> future = super.newTaskFor(runnable, value);
        if (runnable instanceof NodeTaskWrapper) ((NodeTaskWrapper) runnable).setFuture(future);
        return future;
      }
    };
    threadPool.allowCoreThreadTimeOut(true);
  }

  @Override
  protected long[] getThreadIds() {
    return threadFactory.getThreadIDs();
  }

  @Override
  public ExecutorService getExecutorService() {
    return threadPool;
  }

  @Override
  public void setPoolSize(final int size) {
    if (size <= 0) {
      log.warn("ignored attempt to set the thread pool size to 0 or less: " + size);
      return;
    }
    if (getPoolSize() == size) return;

    final ThreadPoolExecutor tpe = threadPool;
    if (size > tpe.getCorePoolSize()) {
      tpe.setMaximumPoolSize(size);
      tpe.setCorePoolSize(size);
    } else if (size < tpe.getCorePoolSize()) {
      tpe.setCorePoolSize(size);
      tpe.setMaximumPoolSize(size);
    }
  }

  @Override
  public int getPoolSize() {
    return threadPool.getMaximumPoolSize();
  }

  @Override
  public int getPriority() {
    return threadFactory.getPriority();
  }

  @Override
  public void setPriority(final int priority) {
    threadFactory.updatePriority(priority);
  }

  @Override
  public UsedClassLoader useClassLoader(final ClassLoader classLoader) {
    final ClassLoader oldClassLoader = Thread.currentThread().getContextClassLoader();
    if (classLoader != null) Thread.currentThread().setContextClassLoader(classLoader);
    return new UsedClassLoaderThread(classLoader, oldClassLoader);
  }

  /**
   * Helper class that implements used class loader for thread pool thread manager.
   */
  private static final class UsedClassLoaderThread extends UsedClassLoader {
    /**
     * An original <code>ClassLoader</code> instance.
     */
    private final ClassLoader oldClassLoader;

    /**
     * Initialize this used class loader with specified parameters.
     * @param classLoader a <code>ClassLoader</code> instance.
     * @param oldClassLoader an original <code>ClassLoader</code> instance that will be restored when dispose is called.
     */
    private UsedClassLoaderThread(final ClassLoader classLoader, final ClassLoader oldClassLoader) {
      super(classLoader);
      this.oldClassLoader = oldClassLoader;
    }

    @Override
    public void dispose() {
      if (getClassLoader() != null) Thread.currentThread().setContextClassLoader(oldClassLoader);
    }
  }
}
