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

package org.jppf.client.concurrent;

import java.util.*;
import java.util.concurrent.*;

import org.jppf.client.JPPFJob;
import org.jppf.client.event.*;
import org.jppf.node.protocol.Task;
import org.jppf.utils.LoggingUtils;
import org.slf4j.*;

/**
 * A {@link CompletionService} which works specifically with {@link JPPFExecutorService}s.
 * @param <V> the type of results returned by the submitted tasks.
 * @author Laurent Cohen
 */
public class JPPFCompletionService<V> implements CompletionService<V> {
  /**
   * Logger for this class.
   */
  private static Logger log = LoggerFactory.getLogger(JPPFCompletionService.class);
  /**
   * Determines whether debug-level logging is enabled.
   */
  private static boolean debugEnabled = LoggingUtils.isDebugEnabled(log);
  /**
   * The executor to which tasks are submitted.
   */
  private final JPPFExecutorService executor;
  /**
   * Maps all the futures for the tasks submitted via this completion service.
   */
  private final Map<String, Map<Integer, JPPFTaskFuture<V>>> futureMap = new HashMap<>();
  /**
   * Listens to notifications of results received for the tasks submitted via this completion service. 
   */
  private final ResultCollectorListener listener = new ResultCollectorListener();
  /**
   * Holds the queue of futures that can be obtained from this completion service.
   */
  private final BlockingQueue<Future<V>> queue = new LinkedBlockingDeque<>();

  /**
   * Initialize this completion service with the specified executor.
   * @param executor the executor to which tasks are submitted.
   */
  public JPPFCompletionService(final JPPFExecutorService executor) {
    this.executor = executor;
  }

  @Override
  public Future<V> submit(final Callable<V> task) {
    return processFuture((JPPFTaskFuture<V>) executor.submit(task));
  }

  @Override
  public Future<V> submit(final Runnable task, final V result) {
    return processFuture((JPPFTaskFuture<V>) executor.submit(task, result));
  }

  @Override
  public Future<V> take() throws InterruptedException {
    return queue.take();
  }

  @Override
  public Future<V> poll() {
    return queue.poll();
  }

  @Override
  public Future<V> poll(final long timeout, final TimeUnit unit) throws InterruptedException {
    return queue.poll(timeout, unit);
  }

  /**
   * Process the future of a submitted task.
   * @param future the future to process.
   * @return the process future.
   */
  private JPPFTaskFuture<V> processFuture(final JPPFTaskFuture<V> future) {
    JPPFJob job = future.getJob();
    String uuid = future.getJob().getUuid();
    synchronized(futureMap) {
      Map<Integer, JPPFTaskFuture<V>> map = futureMap.get(uuid);
      if (map == null) {
        job.addJobListener(listener);
        map = new HashMap<>();
        futureMap.put(uuid, map);
      }
      map.put(future.getPosition(), future);
    }
    return future;
  }

  /**
   * Process the completion of a task future.
   * @param future the future to process.
   */
  private void processFutureCompletion(final JPPFTaskFuture<V> future) {
    if (future == null) throw new IllegalArgumentException("future should not be null");
    try {
      future.getResult(0L);
    } catch (TimeoutException e) {
      e.printStackTrace();
    }
    queue.offer(future);
  }

  /**
   * Listens to notifications from the <code>FutureResultCollector</code> associated to
   * each job submitted by the {@link JPPFExecutorService}, and updates the queue according
   * to the tasks that are completed.
   */
  private class ResultCollectorListener extends JobListenerAdapter {
    @Override
    public void jobReturned(final JobEvent event) {
      List<Task<?>> tasks = event.getJobTasks();
      if (tasks != null) {
        JPPFJob job = event.getJob();
        String uuid = job.getUuid();
        Map<Integer, JPPFTaskFuture<V>> map = null;
        synchronized(futureMap) {
          map = futureMap.get(uuid);
        }
        if (map == null) return;
        for (Task<?> task: tasks) {
          JPPFTaskFuture<V> future = null;
          synchronized(futureMap) {
            future = map.remove(task.getPosition());
          }
          if (future != null) processFutureCompletion(future);
          if (debugEnabled) log.debug("added future[job uuid=" + uuid + ", position=" + task.getPosition() + "] to the queue");
        }
        synchronized(futureMap) {
          if (map.isEmpty()) futureMap.remove(uuid);
        }
      }
    }

    @Override
    public void jobEnded(final JobEvent event) {
      JPPFJob job = event.getJob();
      String uuid = job.getUuid();
      Map<Integer, JPPFTaskFuture<V>> map = null;
      synchronized(futureMap) {
        map = futureMap.remove(uuid);
      }
      if (map != null) {
        for (Map.Entry<Integer, JPPFTaskFuture<V>> entry: map.entrySet()) {
          JPPFTaskFuture<V> future = entry.getValue();
          processFutureCompletion(future);
          if (debugEnabled) log.debug("added future[job uuid=" + uuid + ", position=" + future.getPosition() + "] to the queue");
        }
        synchronized(futureMap) {
          futureMap.remove(uuid);
        }
      }
    }
  }
}
