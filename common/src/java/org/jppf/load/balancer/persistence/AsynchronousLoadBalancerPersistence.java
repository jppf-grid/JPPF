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

package org.jppf.load.balancer.persistence;

import java.util.*;
import java.util.concurrent.*;

import org.jppf.utils.*;
import org.jppf.utils.concurrent.JPPFThreadFactory;
import org.slf4j.*;

/**
 * An asynchronous wrapper for any other load-balancer persistence implementation. The methods of {@link LoadBalancerPersistence} that do not return a result
 * ({@code void} return type) are non-blocking and return immediately. All other methods will block until they are executed and their result is available.
 * The execution of the interface's methods are delegated to a thread pool, whose size can be defined in the configuration or defaults to 1.
 * <p>This asynchronous persistence can be configured in two forms:
 * <pre class="jppf_pre">
 * <span style="color: green"># shorten the configuration value for clarity</span>
 * wrapper = org.jppf.load.balancer.persistence.AsynchronousLoadBalancerPersistence
 * <span style="color: green"># asynchronous persistence with default thread pool size</span>
 * jppf.load.balancing.persistence = ${wrapper} &lt;actual_persistence&gt; &lt;param1&gt; ... &lt;paramN&gt;
 * <span style="color: green"># asynchronous persistence with a specified thread pool size</span>
 * jppf.load.balancer.persistence = ${wrapper} &lt;pool_size&gt; &lt;actual_persistence&gt; &lt;param1&gt; ... &lt;paramN&gt;</pre>
 * <p>Here is an example configuration for an asynchronous database persistence:
 * <pre class="jppf_pre">
 * pkg = org.jppf.load.balancer.persistence
 * <span style="color: green"># asynchronous database persistence with pool of 4 threads,</span>
 * <span style="color: green"># a table named 'JPPF_TEST' and datasource named 'loadBalancerDS'</span>
 * jppf.load.balancing.persistence = ${pkg}.AsynchronousLoadBalancerPersistence 4 \
 *   ${pkg}.DatabaseLoadBalancerPersistence JPPF_TEST loadBalancerDS</pre>
 *
 * @author Laurent Cohen
 * @since 6.0
 */
public class AsynchronousLoadBalancerPersistence implements LoadBalancerPersistence {
  /**
   * Logger for this class.
   */
  private static Logger log = LoggerFactory.getLogger(AsynchronousLoadBalancerPersistence.class);
  /**
   * Determines whether the debug level is enabled in the log configuration, without the cost of a method call.
   */
  private static boolean debugEnabled = log.isDebugEnabled();
  /**
   * The actual persistence implementation to which operations are delegated.
   */
  private final LoadBalancerPersistence delegate;
  /**
   * Performs asycnhronous operations.
   */
  private final ExecutorService executor;
  /**
   * Contains unexecuted persistence (actually "store" operations) tasks. The size of this map is never greater than {@code nbNodes * nbAlgorithms}.
   * <p>If a new entry is posted for a channelID + algorithm and an entry already exists, then the existing entry is overriden. This avoids useless
   * "store" operations when entries are posted faster than the persistence can handle them.
   * <p>Because of that, there is no need to provide a mechanism that limits the memory usage, since it is already bounded.
   */
  private final LinkedHashMap<Pair<String, String>, LoadBalancerPersistenceInfo> pendingTasks = new LinkedHashMap<>();
  /**
   * Number of threads in the pool.
   */
  private int nbThreads;

  /**
   * Initialize this persistence with the specified parameters.
   * @param params if the first parameter is a number, then it represents the number of threads that perform the asynchronous processing, and the remaining parameters
   * represent the wrapped persistence implementation. Otherwise it represents the wrapped persistence and the remaining parameters are those of the wrapped persistence. 
   * @throws LoadBalancerPersistenceException if any error occurs.
   */
  public AsynchronousLoadBalancerPersistence(final String...params) throws LoadBalancerPersistenceException {
    if ((params == null) || (params.length < 1) || (params[0] == null)) throw new LoadBalancerPersistenceException("too few parameters");
    nbThreads = 1;
    String[] forwardParams = null;
    try {
      nbThreads = Integer.valueOf(params[0]);
      forwardParams = new String[params.length - 1];
      System.arraycopy(params, 1, forwardParams, 0, params.length - 1);
    } catch (@SuppressWarnings("unused") final NumberFormatException e) {
      forwardParams = params;
    }
    if (nbThreads < 1) nbThreads = 1;
    this.delegate = ReflectionHelper.invokeDefaultOrStringArrayConstructor(LoadBalancerPersistence.class, getClass().getSimpleName(), forwardParams);
    if (delegate == null) throw new LoadBalancerPersistenceException("could not create load-balancer persistence " + Arrays.asList(params));
    executor = createExecutor(nbThreads);
    final Thread thread = new Thread(new PendingTasksThread(), "PendingTasksThread");
    thread.setDaemon(true);
    thread.start();
  }

  @Override
  public Object load(final LoadBalancerPersistenceInfo info) throws LoadBalancerPersistenceException {
    return submit(new PersistenceTask<Object>(true) {
      @Override
      protected Object execute() throws LoadBalancerPersistenceException {
        return delegate.load(info);
      }
    });
  }

  @Override
  public void store(final LoadBalancerPersistenceInfo info) throws LoadBalancerPersistenceException {
    if (debugEnabled) log.debug("scheduling {}", info);
    try {
      synchronized(pendingTasks) {
        pendingTasks.put(new Pair<>(info.getChannelID(), info.getAlgorithmID()), info);
        pendingTasks.notifyAll();
      }
    } catch (final Exception e) {
      throw new LoadBalancerPersistenceException(e);
    }
  }

  @Override
  public void delete(final LoadBalancerPersistenceInfo info) throws LoadBalancerPersistenceException {
    execute(new PersistenceTask<Void>(false) {
      @Override
      protected Void execute() throws LoadBalancerPersistenceException {
        delegate.delete(info);
        return null;
      }
    });
  }

  @Override
  public List<String> list(final LoadBalancerPersistenceInfo info) throws LoadBalancerPersistenceException {
    return submit(new PersistenceTask<List<String>>(true) {
      @Override
      protected List<String> execute() throws LoadBalancerPersistenceException {
        return delegate.list(info);
      }
    });
  }

  /**
   * @param max the maximum thread pool size.
   * @return an {@link ExecutorService}.
   */
  private static ExecutorService createExecutor(final int max) {
    final LinkedBlockingQueue<Runnable> queue = new LinkedBlockingQueue<>();
    return new ThreadPoolExecutor(1, max, 0L, TimeUnit.MILLISECONDS, queue, new JPPFThreadFactory("AsyncLBPersistence"));
  }

  /**
   * Submit the specified persistence task for asynchronous execution and return the results once they are available.
   * @param task the task to execute.
   * @param <T> the type of result to return.
   * @return the expected result form the task.
   * @throws LoadBalancerPersistenceException if any error occurs.
   */
  private <T> T submit(final PersistenceTask<T> task) throws LoadBalancerPersistenceException {
    try {
      final Future<PersistenceTask<T>> f = executor.submit(task, task);
      final PersistenceTask<T> t = f.get();
      if (t.exception != null) throw t.exception;
      if (debugEnabled) log.debug("got result = " + t.result);
      return t.result;
    } catch (final ClassCastException e) {
      log.error(e.getMessage(), e);
      throw new LoadBalancerPersistenceException(e);
    } catch (InterruptedException | ExecutionException e) {
      throw new LoadBalancerPersistenceException(e);
    }
  }

  /**
   * Submit the specified persistence task for execution some time in the future.
   * @param task the task to execute.
   * @param <T> the type of result the task returns.
   */
  private <T> void execute(final PersistenceTask<T> task) {
    executor.execute(task);
  }

  /**
   * A Runnable task that performs asynchronous delegation of a single operation
   * of a concrete, sequential synchronous persistence implementation.
   * @param <T> the type of result this task returns.
   */
  private static abstract class PersistenceTask<T> implements Runnable {
    /**
     * Logger for this class.
     */
    private static Logger logger = LoggerFactory.getLogger(PersistenceTask.class);
    /**
     * The optional result of this task's execution.
     */
    T result;
    /**
     * An exception that may result from this task's execution.
     */
    LoadBalancerPersistenceException exception;
    /**
     * Whether this task is expected to have a result.
     */
    final boolean hasResult;

    /**
     * @param hasResult whether this task is expected to have a result.
     */
    private PersistenceTask(final boolean hasResult) {
      this.hasResult = hasResult;
    }

    @Override
    public void run() {
      try {
        result = execute();
      } catch (final LoadBalancerPersistenceException e) {
        exception = e;
        if (!hasResult) logger.error(e.getMessage(), e);
      }
    }

    /**
     * Execute the task.
     * @return the execution result.
     * @throws LoadBalancerPersistenceException if any error occurs.
     */
    protected abstract T execute() throws LoadBalancerPersistenceException;
  }

  /**
   * Polls the pending tasks map and submits a persistence task for each entry in the map.
   */
  private class PendingTasksThread implements Runnable {
    @Override
    public void run() {
      try {
        while(true) {
          synchronized(pendingTasks) {
            while (pendingTasks.isEmpty()) pendingTasks.wait(50L);
            if (debugEnabled) log.debug("PendingTasksThread processing {} pending tasks", pendingTasks.size());
            final Map<Pair<String, String>, LoadBalancerPersistenceInfo> temp = new HashMap<>(pendingTasks);
            pendingTasks.clear();
            for (Map.Entry<Pair<String, String>, LoadBalancerPersistenceInfo> entry: temp.entrySet()) {
              final Pair<String, String> id = entry.getKey();
              final LoadBalancerPersistenceInfo info = entry.getValue();
              execute(new PersistenceTask<Void>(false) {
                @Override
                protected Void execute() throws LoadBalancerPersistenceException {
                  LoadBalancerPersistenceInfo newInfo = null;
                  synchronized(pendingTasks) {
                    newInfo = pendingTasks.remove(id);
                  }
                  delegate.store(newInfo == null ? info : newInfo);
                  return null;
                }
              });
            }
          }
        }
      } catch (final Exception e) {
        log.error(e.getMessage(), e);
      }
    }
  }

  @Override
  public String toString() {
    return new StringBuilder("AsynchronousLoadBalancerPersistence[nbThreads=").append(nbThreads).append(", delegate=").append(delegate).append(']').toString();
  }
}
