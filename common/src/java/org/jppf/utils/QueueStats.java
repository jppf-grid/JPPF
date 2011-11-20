/*
 * JPPF.
 * Copyright (C) 2005-2011 JPPF Team.
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

package org.jppf.utils;

import java.io.Serializable;

/**
 * Instances of this class represent statistics for the content of a queue.
 * @author Laurent Cohen
 */
public class QueueStats implements Serializable
{
  /**
   * Explicit serialVersionUID.
   */
  private static final long serialVersionUID = 1L;
  /**
   * Time statistics for the queued objects.
   */
  private StatsSnapshot times = new StatsSnapshot("time");
  /**
   * Time statistics for the queued objects.
   */
  private StatsSnapshot sizes = new StatsSnapshot("size");
  /**
   * Title for this queue snapshot, used in the {@link #toString()} method.
   */
  public String title = "";

  /**
   * Initialize this queue snapshot with a specified title.
   * @param title the title for this snapshot.
   */
  public QueueStats(final String title)
  {
    this.title = title;
    times = new StatsSnapshot(title);
  }

  /**
   * Get the time snapshot.
   * @return a {@link StatsSnapshot} instance.
   */
  public StatsSnapshot getTimes()
  {
    return times;
  }

  /**
   * Set the time snapshot.
   * @param sizes a {@link StatsSnapshot} instance.
   */
  public void setSizes(final StatsSnapshot sizes)
  {
    this.sizes = sizes;
  }

  /**
   * Get the time snapshot.
   * @return a {@link StatsSnapshot} instance.
   */
  public StatsSnapshot getSizes()
  {
    return sizes;
  }

  /**
   * Set the time snapshot.
   * @param times a {@link StatsSnapshot} instance.
   */
  public void setTimes(final StatsSnapshot times)
  {
    this.times = times;
  }

  /**
   * Get the title.
   * @return the title string.
   */
  public String getTitle()
  {
    return title;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public String toString()
  {
    StringBuilder sb = new StringBuilder();
    sb.append(title).append(" queue");
    sb.append(sizes.toString());
    sb.append(times.toString());
    return sb.toString();
  }

  /**
   * Male a copy of this queue stats object.
   * @return a {@link QueueStats} instance.
   */
  public QueueStats makeCopy()
  {
    QueueStats qs = new QueueStats(title);
    qs.setSizes(sizes.makeCopy());
    qs.setTimes(times.makeCopy());
    return qs;
  }
}
