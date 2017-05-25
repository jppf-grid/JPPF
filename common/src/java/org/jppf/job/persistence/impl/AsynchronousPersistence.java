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
import org.jppf.utils.collections.DescendingIntegerComparator;
import org.slf4j.*;

/**
 * An asynchronous wrapper for any other job persistence implementation. The methods of {@link JobPersistence} that do not return a result
 * ({@code void} return type) are non-blocking and return immediately. All other methods will block until they are executed and their result is available.
 * The execution of the interface's methods are delegated to a thread pool, whose size can be defined in the configuration or defaults to {@link Runtime.getRuntime().availableProcessors()}.
 * <p>This asynchronous persistence can be configured in two forms:
 * <pre style="padding: 5px 5px 5px 0px; display: inline-block; background-color: #E0E0F0">
 * <span style="color: green"># shorten the configuration value for clarity</span>
 * wrapper = org.jppf.job.persistence.impl.AsynchronousPersistence
 * <span style="color: green"># asynchronous persistence with default thread pool size</span>
 * jppf.job.persistence = ${wrapper} &lt;actual_persistence&gt; param1 ... paramN
 * <span style="color: green"># asynchronous persistence with a specified thread pool size</span>
 * jppf.job.persistence = ${wrapper} pool_size &lt;actual_persistence&gt; param1 ... paramN</pre>
 * <p>Here is an example configuration for an asynchronous database persistence:
 * <pre style="padding: 5px 5px 5px 0px; display: inline-block; background-color: #E0E0F0">
 * pkg = org.jppf.job.persistence.impl
 * <span style="color: green"># asynchronous database persistence with pool of 4 threads,</span>
 * <span style="color: green"># a tabke name 'JPPF_TEST' and datasource name 'JobDS'</span>
 * jppf.job.persistence = ${pkg}.AsynchronousPersistence 4 ${pkg}.DatabasePersistence JPPF_TEST JobDS</pre>
 * @author Laurent Cohen
 */
public class AsynchronousPersistence implements JobPersistence {
  /**
   * Assigns priorities for the types of persisted objects.
   */
  private static final Map<PersistenceObjectType, Integer> PRIORITIES = new EnumMap<PersistenceObjectType, Integer>(PersistenceObjectType.class) {{
    put(PersistenceObjectType.JOB_HEADER, 400);
    put(PersistenceObjectType.DATA_PROVIDER, 300);
    put(PersistenceObjectType.TASK, 200);
    put(PersistenceObjectType.TASK_RESULT, 100);
  }};
  /**
   * The actual persistence implementation to which operations are delegated.
   */
  private final JobPersistence delegate;
  /**
   * Performs asycnhronous operations.
   */
  private final ExecutorService executor;

  /**
   *
   * @param delegate the persisitence to delegate to.
   * @throws JobPersistenceException if any error occurs.
   */
  public AsynchronousPersistence(final JobPersistence delegate) throws JobPersistenceException {
    if (delegate == null) throw new JobPersistenceException("could not create write-behind job persistence from null persistence");
    this.delegate = delegate;
    executor = createExecutor(Runtime.getRuntime().availableProcessors());
  }

  /**
   *
   * @param params .
   * @throws JobPersistenceException if any error occurs.
   */
  public AsynchronousPersistence(final String...params) throws JobPersistenceException {
    if ((params == null) || (params.length < 1) || (params[0] == null)) throw new JobPersistenceException("too few parameters");
    int n = Runtime.getRuntime().availableProcessors();
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
  public void store(final PersistenceInfo info) throws JobPersistenceException {
    execute(new PersistenceTask<Void>(PRIORITIES.get(info.getType()), false) {
      @Override
      public Void execute() throws JobPersistenceException {
          delegate.store(info);
          return null;
      }
    });
  }

  @Override
  public InputStream load(final PersistenceInfo info) throws JobPersistenceException {
    return submit(new PersistenceTask<InputStream>(PRIORITIES.get(info.getType()), true) {
      @Override
      public InputStream execute() throws JobPersistenceException {
        return delegate.load(info);
      }
    });
  }

  @Override
  public List<String> getPersistedJobUuids() throws JobPersistenceException {
    return submit(new PersistenceTask<List<String>>(1000, true) {
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
    return submit(new PersistenceTask<int[]>(900, true) {
      @Override
      public int[] execute() throws JobPersistenceException {
        return (type == PersistenceObjectType.TASK) ? delegate.getTaskPositions(jobUuid) : delegate.getTaskResultPositions(jobUuid);
      }
    });
  }

  @Override
  public void deleteJob(final String jobUuid) throws JobPersistenceException {
    execute(new PersistenceTask<Void>(0, false) {
      @Override
      public Void execute() throws JobPersistenceException {
          delegate.deleteJob(jobUuid);
          return null;
      }
    });
  }

  /**
   * @param max the maximum thread pool size.
   * @return an {@link ExecutorService}.
   */
  private ExecutorService createExecutor(final int max) {
    PriorityBlockingQueue<Runnable> queue = new PriorityBlockingQueue<>(100, new PersistenceTaskComparator());
    return new ThreadPoolExecutor(1, max, 0L, TimeUnit.MILLISECONDS, queue, new JPPFThreadFactory("AsyncPersistence"));
  }

  /**
   * Submit the specified persistence task for asynchronus execution and return the results once they are available.
   * @param task the task to execute.
   * @param <T> the type o result to return.
   * @return the expected result form the task.
   * @throws JobPersistenceException if any error occurs.
   */
  private <T> T submit(final PersistenceTask<T> task) throws JobPersistenceException {
    Future<PersistenceTask<T>> f = executor.submit(task, task);
    try {
      PersistenceTask<T> t = f.get();
      if (t.exception != null) throw t.exception;
      return t.result;
    } catch (InterruptedException | ExecutionException e) {
      throw new JobPersistenceException(e);
    }
  }

  /**
   * Submit the specified persistence task for execution some time in the future.
   * @param task the task to execute.
   * @param <T> the type o result the task returns.
   */
  private <T> void execute(final PersistenceTask<T> task) {
    executor.execute(task);
  }

  /**
   * Compares {@link PersistenceInfo} objects in descending order of their type's priority.
   */
  private static class PersistenceTaskComparator implements Comparator<Runnable> {
    /**
     * Compares the priorities.
     */
    private final DescendingIntegerComparator priorityComparator = new DescendingIntegerComparator();

    @Override
    public int compare(final Runnable r1, final Runnable r2) {
      final PersistenceTask<?> o1 = (PersistenceTask<?>) r1, o2 = (PersistenceTask<?>) r2;
      if (o1 == null) return (o2 == null) ? 0 : 1;
      else if (o2 == null) return -1;
      return priorityComparator.compare(o1.priority, o2.priority);
    }
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
     * This task's priority.
     */
    private final int priority;
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
     * @param priority the persistence information.
     * @param hasResult whether this task is expected to have a result.
     */
    private PersistenceTask(final int priority, final boolean hasResult) {
      this.priority = priority;
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
    protected abstract T execute() throws JobPersistenceException;
  }
}
