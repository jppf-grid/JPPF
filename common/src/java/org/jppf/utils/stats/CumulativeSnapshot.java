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

package org.jppf.utils.stats;

import org.jppf.utils.ExceptionUtils;
import org.slf4j.*;

/**
 * In this implementation, {@code getLatest()} is computed as the cumulated sum of all values added to the snapshot.
 * If values are only added, and not removed, then it will always return the same value as getTotal().
 * @author Laurent Cohen
 */
public class CumulativeSnapshot extends AbstractJPPFSnapshot {
  /**
   * Logger for this class.
   */
  private static Logger log = LoggerFactory.getLogger(CumulativeSnapshot.class);
  /**
   * Determines whether the debug level is enabled in the log configuration, without the cost of a method call.
   */
  private static boolean debugEnabled = log.isDebugEnabled();
  /**
   * Explicit serialVersionUID.
   */
  private static final long serialVersionUID = 1L;

  /**
   * Initialize this snapshot with a specified title.
   * @param label the title for this snapshot.
   * @exclude
   */
  public CumulativeSnapshot(final String label) {
    super(label);
  }

  /**
   * {@inheritDoc}
   * @exclude
   */
  @Override
  public synchronized void addValues(final double accumulatedValues, final long count) {
    total += accumulatedValues;
    if (count > 0L) {
      valueCount += count;
      if (debugEnabled && (label == JPPFStatisticsHelper.TASK_QUEUE_COUNT)) {
        log.debug(String.format("latest=%5d; adding %4d; new value=%5d", (long) latest, (long) accumulatedValues, (long) (latest + accumulatedValues)));
        String name = Thread.currentThread().getName();
        if ((accumulatedValues <= 0d) && (name != null) && name.startsWith("JPPF NIO-")) log.debug("call stack:\n{}", ExceptionUtils.getCallStack());
      }
      latest += accumulatedValues;
      if (latest > max) max = latest;
      if (latest < min) min = latest;
      if (valueCount != 0d) avg = total / valueCount;
    }
  }

  /**
   * {@inheritDoc}
   * @exclude
   */
  @Override
  public synchronized void assignLatestToMax() {
    max = latest;
    total = latest;
    valueCount = 1L;
    avg = latest;
    min = 0d;
  }

  /**
   * {@inheritDoc}
   * @exclude
   */
  @Override
  public JPPFSnapshot copy() {
    return copy(new CumulativeSnapshot(getLabel()));
  }
}
