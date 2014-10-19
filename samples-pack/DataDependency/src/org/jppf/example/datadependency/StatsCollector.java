/*
 * JPPF.
 * Copyright (C) 2005-2014 JPPF Team.
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

package org.jppf.example.datadependency;

import java.text.NumberFormat;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import org.jppf.node.protocol.Task;

/**
 * This class collects statistics about job and tasks executions
 * @author Laurent Cohen
 */
public class StatsCollector
{
  /**
   * Total number of data updates.
   */
  private AtomicInteger nbUpdates = new AtomicInteger(0);
  /**
   * Total number of tasks.
   */
  private AtomicInteger nbTasks = new AtomicInteger(0);
  /**
   * Total number of tasks.
   */
  private AtomicInteger nbJobs = new AtomicInteger(0);
  /**
   * Total processing time.
   */
  private AtomicLong totalTime = new AtomicLong(0L);

  /**
   * Increment the number of updates.
   */
  public void dataUpdated()
  {
    nbUpdates.incrementAndGet();
  }

  /**
   * Update the statistics for a newly processed job.
   * @param results the results of the job that was processed.
   * @param time the job's total processing time.
   */
  public void jobProcessed(final List<Task<?>> results, final long time)
  {
    nbJobs.incrementAndGet();
    nbTasks.addAndGet(results.size());
    totalTime.addAndGet(time);
  }

  /**
   * Get the total number of updates.
   * @return the number of updates as an int value.
   */
  public int getNbUpdates()
  {
    return nbUpdates.get();
  }

  /**
   * Get the total number of jobs.
   * @return the number of jobs as an int value.
   */
  public int getNbJobs()
  {
    return nbJobs.get();
  }

  /**
   * Get the total number of tasks.
   * @return the number of tasks as an int value.
   */
  public int getNbTasks()
  {
    return nbTasks.get();
  }

  /**
   * Get the total processing time.
   * @return the total time as a long value.
   */
  public long getTotalTime()
  {
    return totalTime.get();
  }

  /**
   * Set the total processing time.
   * @param time the total time as a long value.
   */
  public void setTotalTime(final long time)
  {
    totalTime.set(time);
  }

  /**
   * Dump the statistics to a string.
   * @return a string.
   * @see java.lang.Object#toString()
   */
  @Override
  public String toString()
  {
    StringBuilder sb = new StringBuilder();
    sb.append("total updates:        ").append(getNbUpdates()).append("\n");
    sb.append("total jobs:           ").append(getNbJobs()).append("\n");
    sb.append("total tasks:          ").append(getNbTasks()).append("\n");
    sb.append("total time:           ").append(getTotalTime()).append(" ms\n");
    NumberFormat nf = NumberFormat.getInstance();
    nf.setMinimumFractionDigits(2);
    nf.setMaximumFractionDigits(2);
    nf.setMinimumIntegerDigits(1);
    sb.append("avg jobs per update:  ").append(nf.format(nbJobs.doubleValue()/nbUpdates.get())).append("\n");
    sb.append("avg tasks per update: ").append(nf.format(nbTasks.doubleValue()/nbUpdates.get())).append("\n");
    sb.append("avg tasks per job:    ").append(nf.format(nbTasks.doubleValue()/nbJobs.get())).append("\n");
    sb.append("avg time per update:  ").append(nf.format(totalTime.doubleValue()/nbUpdates.get())).append(" ms\n");
    sb.append("avg time per job:     ").append(nf.format(totalTime.doubleValue()/nbJobs.get())).append(" ms\n");
    sb.append("avg time per task:    ").append(nf.format(totalTime.doubleValue()/nbTasks.get())).append(" ms\n");
    return sb.toString();
  }
}
