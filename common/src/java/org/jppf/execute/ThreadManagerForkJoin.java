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

import java.util.*;
import java.util.concurrent.*;

import org.jppf.utils.*;
import org.slf4j.*;

/**
 * This class manages the fork join pool for the node's execution manager.
 * @author Martin JANDA
 */
public class ThreadManagerForkJoin extends AbstractThreadManager {
  /**
   * Logger for this class.
   */
  private static Logger log = LoggerFactory.getLogger(ThreadManagerForkJoin.class);
  /**
   * The thread pool that really processes the tasks
   */
  private ForkJoinPool threadPool;
  /**
   * The factory used to create thread in the pool.
   */
  private FJThreadFactory threadFactory;

  /**
   * Initialized thread manager.
   * @param poolSize the initial size of the thread pool.
   */
  public ThreadManagerForkJoin(final int poolSize) {
    this.threadFactory = new FJThreadFactory(THREAD_NAME_PREFIX, CpuTimeCollector.isCpuTimeEnabled());
    threadPool = new ForkJoinPool(poolSize, threadFactory, new Thread.UncaughtExceptionHandler() {
      @Override
      public void uncaughtException(final Thread t, final Throwable e) {
        log.error(String.format("UncaughtException in thread[%d:%s] - %s", t.getId(), t.getName(), ExceptionUtils.getStackTrace(e)));
      }
    }, false) {
      @Override
      protected <T> RunnableFuture<T> newTaskFor(final Runnable runnable, final T value) {
        final RunnableFuture<T> future = super.newTaskFor(runnable, value);
        if (runnable instanceof NodeTaskWrapper) {
          ((NodeTaskWrapper) runnable).setFuture(future);
        }
        return future;
      }
    };
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
  }

  @Override
  public int getPoolSize() {
    return threadPool.getParallelism();
  }

  @Override
  public int getPriority() {
    return threadFactory.getPriority();
  }

  @Override
  public void setPriority(final int priority) {
    threadFactory.setPriority(priority);
  }

  @Override
  public UsedClassLoader useClassLoader(final ClassLoader classLoader) {
    return threadFactory.useClassLoader(classLoader);
  }

  /**
   *
   */
  protected class FJThreadFactory implements ForkJoinPool.ForkJoinWorkerThreadFactory {
    /**
     * The name used as prefix for the constructed threads name.
     */
    private String name = null;
    /**
     * List of monitored thread IDs.
     */
    private List<Long> threadIDs;
    /**
     * The thread group that contains the threads of this factory.
     */
    private ThreadGroup threadGroup = null;
    /**
     * Priority assigned to the threads created by this factory.
     */
    private int priority = Thread.NORM_PRIORITY;
    /**
     * Summary information for all terminated threads.
     */
    private ExecutionInfo terminatedInfo = new ExecutionInfo();
    /**
     * List of monitored threads.
     */
    private final List<Thread> threadList = new ArrayList<>();
    /**
     * A context class loader used during execution.
     */
    private ClassLoader classLoader = null;
    /**
     * A set of used class loaders used during execution.
     */
    private final Set<UsedClassLoader> usedClassLoaders = new HashSet<>();

    /**
     * Initialize this thread factory with the specified name.
     * @param name the name used as prefix for the constructed threads name.
     */
    public FJThreadFactory(final String name) {
      this(name, false, Thread.NORM_PRIORITY);
    }

    /**
     * Initialize this thread factory with the specified name.
     * @param name the name used as prefix for the constructed threads name.
     * @param priority priority assigned to the threads created by this factory.
     */
    public FJThreadFactory(final String name, final int priority) {
      this(name, false, priority);
    }

    /**
     * Initialize this thread factory with the specified name.
     * @param name the name used as prefix for the constructed threads name.
     * @param monitoringEnabled determines whether the threads created by this factory can be monitored.
     */
    public FJThreadFactory(final String name, final boolean monitoringEnabled) {
      this(name, monitoringEnabled, Thread.NORM_PRIORITY);
    }

    /**
     * Initialize this thread factory with the specified name.
     * @param name the name used as prefix for the constructed threads name.
     * @param monitoringEnabled determines whether the threads created by this factory can be monitored.
     * @param priority priority assigned to the threads created by this factory.
     */
    public FJThreadFactory(final String name, final boolean monitoringEnabled, final int priority) {
      this.name = name == null ? "JPPFThreadFactory" : name;
      this.priority = priority;
      threadGroup = new ThreadGroup(this.name + " thread group");
      threadGroup.setMaxPriority(Thread.MAX_PRIORITY);
      if (monitoringEnabled) threadIDs = new ArrayList<>();
      else threadIDs = null;
    }

    @Override
    public synchronized ForkJoinWorkerThread newThread(final ForkJoinPool pool) {
      final ForkJoinWorkerThread thread = new ForkJoinWorkerThread(pool) {
        @Override
        protected void onTermination(final Throwable exception) {
          try {
            terminate(this, exception);
          } finally {
            super.onTermination(exception);
          }
        }
      };
      if (threadIDs != null) threadIDs.add(thread.getId());
      threadList.add(thread);
      thread.setPriority(priority);
      return thread;
    }

    /**
     * Process terminated thread that is no longer used by fork join pool.
     * @param thread the thread instance.
     * @param exception the exception of unrecoverable error or {@code null}
     */
    protected synchronized void terminate(final Thread thread, final Throwable exception) {
      if (threadIDs != null) {
        final long threadID = thread.getId();
        threadIDs.remove(threadID);
        terminatedInfo.add(computeExecutionInfo(threadID));
      }
      if (exception != null) {
        System.out.printf("Thread [%d:%s] terminated with exception: %s%n", thread.getId(), thread.getName(), exception);
        exception.printStackTrace(System.out);
      }
      threadList.remove(thread);
    }

    /**
     * Get the ids of the monitored threads.
     * @return a list of long values.
     */
    public synchronized long[] getThreadIDs() {
      if (threadIDs == null || threadIDs.isEmpty()) return new long[0];
      final long[] ids = new long[threadIDs.size()];
      int dstIndex = 0;
      for (final Long id : threadIDs) ids[dstIndex++] = id;
      return ids;
    }

    /**
     * Update the priority of all threads created by this factory.
     * @param priority the new priority to set.
     */
    public synchronized void setPriority(final int priority) {
      if ((priority < Thread.MIN_PRIORITY) || (priority > Thread.MAX_PRIORITY) || (this.priority == priority)) return;
      for (Thread thread : threadList) thread.setPriority(priority);
      this.priority = priority;
    }

    /**
     * Get the priority assigned to the threads created by this factory.
     * @return the priority as an int value.
     */
    public synchronized int getPriority() {
      return priority;
    }

    /**
     * Get the summary information for all terminated threads.
     * @return a <code>NodeExecutionInfo</code> instance.
     */
    public synchronized ExecutionInfo getTerminatedInfo() {
      return terminatedInfo;
    }

    /**
     * Use class loader in this thread manager.
     * @param classLoader a <code>ClassLoader</code> instance.
     * @return a <code>UsedClassLoader</code> instance. Never return <code>null</code>.
     */
    public synchronized UsedClassLoader useClassLoader(final ClassLoader classLoader) {
      if (usedClassLoaders.isEmpty()) {
        this.classLoader = classLoader;
        for (Thread thread : threadList) thread.setContextClassLoader(classLoader);
      } else if (this.classLoader != classLoader) {
        throw new IllegalStateException("Already used different classLoader");
      }
      final FJUsedClassLoader usedClassLoader = new FJUsedClassLoader(classLoader, this);
      usedClassLoaders.add(usedClassLoader);
      return usedClassLoader;
    }

    /**
     * Disposes used class loaderer from this thread factory.
     * @param usedClassLoader a <code>FJUsedClassLoader</code> instance.
     */
    protected synchronized void dispose(final FJUsedClassLoader usedClassLoader) {
      if (usedClassLoader == null) throw new IllegalArgumentException("usedClassLoader is null");
      if (usedClassLoaders.remove(usedClassLoader)) {
        if (usedClassLoaders.isEmpty()) this.classLoader = null;
      } else {
        throw new IllegalStateException("UsedClassLoader already disposed");
      }
    }
  }

  /**
   * Helper class that implements used class loader for fork join thread manager.
   */
  private static final class FJUsedClassLoader extends UsedClassLoader {
    /**
     * The thread factory that has registered this UsedClassLoader.
     */
    private final FJThreadFactory threadFactory;

    /**
     * Initialize this used class loader with specified parameters.
     * @param classLoader a <code>ClassLoader</code> instance.
     * @param threadFactory a <code>FJThreadFactory</code> instance.
     */
    private FJUsedClassLoader(final ClassLoader classLoader, final FJThreadFactory threadFactory) {
      super(classLoader);
      if (threadFactory == null) throw new IllegalArgumentException("threadFactory is null");
      this.threadFactory = threadFactory;
    }

    @Override
    public void dispose() {
      threadFactory.dispose(this);
    }
  }
}
