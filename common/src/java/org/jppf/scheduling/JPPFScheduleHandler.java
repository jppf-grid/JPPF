/*
 * JPPF.
 * Copyright (C) 2005-2015 JPPF Team.
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

import org.jppf.utils.*;
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
  private static Logger log = LoggerFactory.getLogger(JPPFScheduleHandler.class);
  /**
   * Determines whether the debug level is enabled in the logging configuration, without the cost of a method call.
   */
  private static boolean debugEnabled = LoggingUtils.isDebugEnabled(log);
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
  private String name = null;
  /**
   * Used to debug date information.
   */
  private final SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");

  /**
   * Initialize this schedule handler with a default name.
   */
  public JPPFScheduleHandler() {
    this("JPPFScheduleHandler timer - " + instanceCount.incrementAndGet());
  }

  /**
   * Initialize this schedule handler with the specified name.
   * @param name the name given to this schedule handler.
   */
  public JPPFScheduleHandler(final String name) {
    this.name = name;
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
    Date date = schedule.toDate(start);
    ScheduledFuture<?> future = executor.schedule(action, date.getTime() - start, TimeUnit.MILLISECONDS);
    futureMap.put(key, future);
    if (debugEnabled) {
      synchronized(sdf) {
        log.debug(name + " : date=" + sdf.format(date) + ", key=" + key + ", future=" + future);
      }
    }
  }

  /**
   * Cancel the scheduled action identified by the specified key.
   * @param key the key associated with the action.
   */
  public void cancelAction(final Object key) {
    if (key == null) return;
    ScheduledFuture<?> future = futureMap.remove(key);
    if (debugEnabled) log.debug(name + " : cancelling action for key=" + key + ", future=" + future);
    if (future != null) future.cancel(true);
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
    for (Map.Entry<Object, ScheduledFuture<?>> entry: futureMap.entrySet()) {
      ScheduledFuture<?> f = entry.getValue();
      if (f != null) f.cancel(true);
    }
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
}
