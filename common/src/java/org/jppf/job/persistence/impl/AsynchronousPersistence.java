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

package org.jppf.job.persistence.impl;

import java.io.InputStream;
import java.util.*;
import java.util.concurrent.*;

import org.jppf.job.persistence.*;
import org.jppf.utils.*;
import org.jppf.utils.concurrent.JPPFThreadFactory;
import org.jppf.utils.configuration.JPPFProperties;
import org.slf4j.*;

/**
 * An asynchronous wrapper for any other job persistence implementation. The methods of {@link JobPersistence} that do not return a result
 * ({@code void} return type) are non-blocking and return immediately. All other methods will block until they are executed and their result is available.
 * The execution of the interface's methods are delegated to a thread pool, whose size can be defined in the configuration or defaults to {@link Runtime#availableProcessors() Runtime.getRuntime().availableProcessors()}.
 * <p>This asynchronous persistence can be configured in two forms:
 * <pre class="jppf_pre">
 * <span style="color: green"># shorten the configuration value for clarity</span>
 * wrapper = org.jppf.job.persistence.impl.AsynchronousPersistence
 * <span style="color: green"># asynchronous persistence with default thread pool size</span>
 * jppf.job.persistence = ${wrapper} &lt;actual_persistence&gt; &lt;param1&gt; ... &lt;paramN&gt;
 * <span style="color: green"># asynchronous persistence with a specified thread pool size</span>
 * jppf.job.persistence = ${wrapper} &lt;pool_size&gt; &lt;actual_persistence&gt; &lt;param1&gt; ... &lt;paramN&gt;</pre>
 * <p>Here is an example configuration for an asynchronous database persistence:
 * <pre class="jppf_pre">
 * pkg = org.jppf.job.persistence.impl
 * <span style="color: green"># asynchronous database persistence with pool of 4 threads,</span>
 * <span style="color: green"># a table named 'JPPF_TEST' and datasource named 'JobDS'</span>
 * jppf.job.persistence = ${pkg}.AsynchronousPersistence 4 ${pkg}.DefaultDatabasePersistence JPPF_TEST JobDS</pre>
 * @author Laurent Cohen
 */
public class AsynchronousPersistence implements JobPersistence {
  /**
   * Logger for this class.
   */
  private static Logger log = LoggerFactory.getLogger(AsynchronousPersistence.class);
  /**
   * Determines whether the debug level is enabled in the log configuration, without the cost of a method call.
   */
  private static boolean debugEnabled = log.isDebugEnabled();
  /**
   * The actual persistence implementation to which operations are delegated.
   */
  private final JobPersistence delegate;
  /**
   * Performs asycnhronous operations.
   */
  private final ExecutorService executor;
  /**
   * Percentage of used heap above which asynchronous mode is switched off, to prevent out of memory conditions.
   * When the used heap passes below this threshold, the asynchronous mode resumes.
   */
  private static final double MEMORY_THRESHOLD = JPPFConfiguration.get(JPPFProperties.JOB_PERSISTENCE_MEMORY_THRESHOLD);

  /**
   * Initialize this persistence with the specified parameters.
   * @param params if the first parameter is a number, then it represents the number of threads that perform the asynchronous processing, and the remaining parameters
   * represent the wrapped persistence implementation. Otherwise it represents the wrapped persistence and the remaining parameters are those of the wrapped persistence. 
   * @throws JobPersistenceException if any error occurs.
   */
  public AsynchronousPersistence(final String...params) throws JobPersistenceException {
    if ((params == null) || (params.length < 1) || (params[0] == null)) throw new JobPersistenceException("too few parameters");
    int n = 1;
    String[] forwardParams = null;
    try {
      n = Integer.valueOf(params[0]);
      forwardParams = new String[params.length - 1];
      System.arraycopy(params, 1, forwardParams, 0, params.length - 1);
    } catch (@SuppressWarnings("unused") NumberFormatException e) {
      forwardParams = params;
    }
    if (n < 1) n = 1;
    this.delegate = ReflectionHelper.invokeDefaultOrStringArrayConstructor(JobPersistence.class, getClass().getSimpleName(), forwardParams);
    if (delegate == null) throw new JobPersistenceException("could not create job persistence " + Arrays.asList(params));
    executor = createExecutor(n);
  }

  @Override
  public void store(final Collection<PersistenceInfo> infos) throws JobPersistenceException {
    if (debugEnabled) log.debug("storing {}", infos);
    if (SystemUtils.heapUsagePct() >= MEMORY_THRESHOLD) delegate.store(infos);
    else {
      execute(new PersistenceTask<Void>(false) {
        @Override
        public Void execute() throws JobPersistenceException {
          delegate.store(infos);
          return null;
        }
      });
    }
  }

  @Override
  public List<InputStream> load(final Collection<PersistenceInfo> infos) throws JobPersistenceException {
    if (SystemUtils.heapUsagePct() >= MEMORY_THRESHOLD) return delegate.load(infos);
    return submit(new PersistenceTask<List<InputStream>>(true) {
      @Override
      public List<InputStream> execute() throws JobPersistenceException {
        return delegate.load(infos);
      }
    });
  }

  @Override
  public List<String> getPersistedJobUuids() throws JobPersistenceException {
    if (SystemUtils.heapUsagePct() >= MEMORY_THRESHOLD) return delegate.getPersistedJobUuids();
    return submit(new PersistenceTask<List<String>>(true) {
      @Override
      public List<String> execute() throws JobPersistenceException {
        return delegate.getPersistedJobUuids();
      }
    });
  }

  @Override
  public int[] getTaskPositions(final String jobUuid) throws JobPersistenceException {
    return getPositions(jobUuid, PersistenceObjectType.TASK);
  }

  @Override
  public int[] getTaskResultPositions(final String jobUuid) throws JobPersistenceException {
    return getPositions(jobUuid, PersistenceObjectType.TASK_RESULT);
  }

  /**
   * Get the  positions for all the objects of the specified type in the specified job.
   * @param jobUuid the uuid of the job for which to get the positions.
   * @param type the type of object for which to get the positions, one of {@link PersistenceObjectType#TASK TASK} or {@link PersistenceObjectType#TASK_RESULT TASK_RESULT}.
   * @return an array of int holding the positions.
   * @throws JobPersistenceException if any error occurs.
   */
  private int[] getPositions(final String jobUuid, final PersistenceObjectType type) throws JobPersistenceException {
    if (SystemUtils.heapUsagePct() >= MEMORY_THRESHOLD) return (type == PersistenceObjectType.TASK) ? delegate.getTaskPositions(jobUuid) : delegate.getTaskResultPositions(jobUuid);
    return submit(new PersistenceTask<int[]>(true) {
      @Override
      public int[] execute() throws JobPersistenceException {
        return (type == PersistenceObjectType.TASK) ? delegate.getTaskPositions(jobUuid) : delegate.getTaskResultPositions(jobUuid);
      }
    });
  }

  @Override
  public void deleteJob(final String jobUuid) throws JobPersistenceException {
    if (SystemUtils.heapUsagePct() >= MEMORY_THRESHOLD) delegate.deleteJob(jobUuid);
    else execute(new PersistenceTask<Void>(false) {
      @Override
      public Void execute() throws JobPersistenceException {
        delegate.deleteJob(jobUuid);
        return null;
      }
    });
  }

  @Override
  public boolean isJobPersisted(final String jobUuid) throws JobPersistenceException {
    if (SystemUtils.heapUsagePct() >= MEMORY_THRESHOLD) return delegate.isJobPersisted(jobUuid);
    return submit(new PersistenceTask<Boolean>(true) {
      @Override
      public Boolean execute() throws JobPersistenceException {
        return delegate.isJobPersisted(jobUuid);
      }
    });
  }

  /**
   * @param max the maximum thread pool size.
   * @return an {@link ExecutorService}.
   */
  private ExecutorService createExecutor(final int max) {
    LinkedBlockingQueue<Runnable> queue = new LinkedBlockingQueue<>();
    return new ThreadPoolExecutor(1, max, 0L, TimeUnit.MILLISECONDS, queue, new JPPFThreadFactory("AsyncPersistence"));
  }

  /**
   * Submit the specified persistence task for asynchronous execution and return the results once they are available.
   * @param task the task to execute.
   * @param <T> the type of result to return.
   * @return the expected result form the task.
   * @throws JobPersistenceException if any error occurs.
   */
  private <T> T submit(final PersistenceTask<T> task) throws JobPersistenceException {
    try {
      Future<PersistenceTask<T>> f = executor.submit(task, task);
      PersistenceTask<T> t = f.get();
      if (t.exception != null) throw t.exception;
      if (debugEnabled) log.debug("got result = {}", t.result);
      return t.result;
    } catch (ClassCastException e) {
      log.error(e.getMessage(), e);
      throw new JobPersistenceException(e);
    } catch (InterruptedException | ExecutionException e) {
      throw new JobPersistenceException(e);
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
    private static Logger log = LoggerFactory.getLogger(AsynchronousPersistence.PersistenceTask.class);
    /**
     * The optional result of this task's execution.
     */
    T result;
    /**
     * An exception that may result from this task's execution.
     */
    JobPersistenceException exception;
    /**
     * Whether this task is expected to have a result.
     */
    private final boolean hasResult;

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
      } catch (JobPersistenceException e) {
        exception = e;
        if (!hasResult) log.error(e.getMessage(), e);
      }
    }

    /**
     * Execute the task.
     * @return the execution result.
     * @throws JobPersistenceException if any error occurs.
     */
    abstract T execute() throws JobPersistenceException;
  }
}
