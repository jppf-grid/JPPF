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

package test.org.jppf.test.setup.common;

import java.util.concurrent.atomic.AtomicInteger;

import org.jppf.client.event.*;
import org.jppf.utils.LoggingUtils;
import org.slf4j.*;

/**
 * A simple job listener.
 */
public class CountingJobListener extends JobListenerAdapter {
  /**
   * Logger for this class.
   */
  private static Logger log = LoggerFactory.getLogger(CountingJobListener.class);
  /**
   * Determines whether DEBUG logging level is enabled.
   */
  private static boolean debugEnabled = LoggingUtils.isDebugEnabled(log);

  /**
   * The count of 'jobStarted' notifications.
   */
  public AtomicInteger startedCount = new AtomicInteger(0);
  /**
   * The count of 'jobEnded' notifications.
   */
  public AtomicInteger endedCount = new AtomicInteger(0);
  /**
   * The count of 'jobDispatched' notifications.
   */
  public AtomicInteger dispatchedCount = new AtomicInteger(0);
  /**
   * The count of 'jobReturned' notifications.
   */
  public AtomicInteger returnedCount = new AtomicInteger(0);

  @Override
  public void jobStarted(final JobEvent event) {
    startedCount.incrementAndGet();
    if (debugEnabled) log.debug("JOB_STARTED for {}", event.getJob());
  }

  @Override
  public void jobEnded(final JobEvent event) {
    endedCount.incrementAndGet();
    if (debugEnabled) log.debug("JOB_ENDED for {}", event.getJob());
  }

  @Override
  public void jobDispatched(final JobEvent event) {
    dispatchedCount.incrementAndGet();
    if (debugEnabled) log.debug("JOB_DISPATCHED for {}", event.getJob());
  }

  @Override
  public void jobReturned(final JobEvent event) {
    returnedCount.incrementAndGet();
    if (debugEnabled) log.debug("JOB_RETURNED for {}", event.getJob());
  }
}
