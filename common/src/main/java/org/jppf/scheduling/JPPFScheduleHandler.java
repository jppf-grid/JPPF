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

package org.jppf.scheduling;

import java.text.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import org.jppf.utils.concurrent.JPPFThreadFactory;
import org.slf4j.*;

/**
 * This class handles a timer.
 * @author Laurent Cohen
 * @exclude
 */
public class JPPFScheduleHandler {
  /**
   * Logger for this class.
   */
  private static final Logger log = LoggerFactory.getLogger(JPPFScheduleHandler.class);
  /**
   * Determines whether the debug level is enabled in the logging configuration, without the cost of a method call.
   */
  private static final boolean debugEnabled = log.isDebugEnabled();
  /**
   * The scheduled executor used for scheduling actions.
   */
  private ScheduledExecutorService executor;
  /**
   * Count of the instances of this class, added as a suffix to the timer's name.
   */
  private static AtomicInteger instanceCount = new AtomicInteger(0);
  /**
   * Mapping of timer tasks to a key.
   */
  private Map<Object, ScheduledFuture<?>> futureMap = new Hashtable<>();
  /**
   * The name given to this schedule handler's internal timer.
   */
  private final String name;
  /**
   * Used to debug date information.
   */
  private final SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");

  /**
   * Initialize this schedule handler with a default name.
   */
  public JPPFScheduleHandler() {
    this(null);
  }

  /**
   * Initialize this schedule handler with the specified name.
   * @param name the name given to this schedule handler.
   */
  public JPPFScheduleHandler(final String name) {
    this.name = (name != null) ? name : "JPPFScheduleHandler timer - " + instanceCount.incrementAndGet();
    createExecutor();
  }

  /**
   * Schedule an action.
   * @param key key used to retrieve or cancel the action at a later time.
   * @param schedule the schedule at which the action is triggered.
   * @param action the action to perform when the schedule date is reached.
   * @throws ParseException if the schedule date could not be parsed
   */
  public void scheduleAction(final Object key, final JPPFSchedule schedule, final Runnable action) throws ParseException {
    scheduleAction(key, schedule, action, System.currentTimeMillis());
  }

  /**
   * Schedule an action.
   * @param key key used to retrieve or cancel the action at a later time.
   * @param schedule the schedule at which the action is triggered.
   * @param action the action to perform when the schedule date is reached.
   * @param start the start time to use if the schedule is expressed as a duration.
   * @throws ParseException if the schedule date could not be parsed
   */
  public void scheduleAction(final Object key, final JPPFSchedule schedule, final Runnable action, final long start) throws ParseException {
    if (debugEnabled) {
      synchronized(sdf) {
        log.debug(name + " : scheduling action[key=" + key + ", " + schedule + ", action=" + action + ", start=" + sdf.format(new Date(start)));
      }
    }
    final long epoch = schedule.toLong(start);
    final ScheduledFuture<?> future = executor.schedule(new ScheduledAction(key, action), epoch - start, TimeUnit.MILLISECONDS);
    futureMap.put(key, future);
    if (debugEnabled) {
      synchronized(sdf) {
        log.debug(name + " : date=" + sdf.format(new Date(schedule.toLong(start))) + ", key=" + key + ", future=" + future);
      }
    }
  }

  /**
   * Determine whether an action is already regsitered for the specified job uuid.
   * @param uuid the uuid of a job to check.
   * @return {@code true} if an action is already scheduled for the job, {@code false} otherwise.
   */
  public boolean hasAction(final String uuid) {
    return futureMap.get(uuid) != null;
  }

  /**
   * Cancel the scheduled action identified by the specified key.
   * @param key the key associated with the action.
   */
  public void cancelAction(final Object key) {
    cancelAction(key, true);
  }

  /**
   * Cancel the scheduled action identified by the specified key.
   * @param key the key associated with the action.
   * @param mayInterruptIfRunning whether the thread that runs the task should be interrupted if the task is being executed.
   */
  public void cancelAction(final Object key, final boolean mayInterruptIfRunning) {
    if (key == null) return;
    final ScheduledFuture<?> future = futureMap.remove(key);
    if (debugEnabled) log.debug(name + " : cancelling action for key=" + key + ", future=" + future);
    if (future != null) future.cancel(mayInterruptIfRunning);
  }

  /**
   * Cleanup this schedule handler.
   */
  public void clear() {
    clear(false);
  }

  /**
   * Shutdown this schedule handler.
   * @param shutdown flag indicating whether this schedule handler should be shutdown.
   */
  public void clear(final boolean shutdown) {
    futureMap.forEach((key, future) -> {
      if (future != null) future.cancel(true);
    });
    futureMap.clear();
    if (shutdown) executor.shutdownNow();
  }

  /**
   * Create the executor used for task scheduling.
   */
  private void createExecutor() {
    executor = Executors.newScheduledThreadPool(1, new JPPFThreadFactory(this.name));
    if (debugEnabled) log.debug("created executor with name=" + name);
  }

  /**
   * 
   */
  private final class ScheduledAction implements Runnable {
    /**
     * The key associated with the action.
     */
    private final Object key;
    /**
     * The action to run upon expiration.
     */
    private final Runnable action;

    /**
     * Initialize this scheduled action.
     * @param key the key associated with the action.
     * @param action the action to run upon expiration.
     */
    private ScheduledAction(final Object key, final Runnable action) {
      this.key = key;
      this.action = action;
    }

    @Override
    public void run() {
      futureMap.remove(key);
      action.run();
    }
  }
}
