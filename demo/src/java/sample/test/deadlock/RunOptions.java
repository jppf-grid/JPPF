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

package sample.test.deadlock;

import org.jppf.utils.*;

/**
 * Java wrapper for the options of the jobs streaming demo.
 * @author Laurent Cohen
 */
public class RunOptions {
  /**
   * The JPPF configuration.
   */
  public final TypedProperties config = JPPFConfiguration.getProperties();
  /**
   * Max concurrent jobs that can be submitted by the client.
   */
  public final int concurrencyLimit = config.getInt("deadlock.concurrencyLimit", 1);
  /**
   * Number of driver connections.
   */
  public final int clientConnections = config.getInt("deadlock.clientConnections", concurrencyLimit);
  /**
   * Number of slave nodes for each master node. Total number of nodes = nbMasters * (1 + slaves).
   */
  public final int slaves = config.getInt("deadlock.slaveNodes", 0);
  /**
   * Total number of jobs to execute.
   */
  public final int nbJobs = config.getInt("deadlock.nbJobs", 10);
  /**
   * Number of tasks per job.
   */
  public final int tasksPerJob = config.getInt("deadlock.tasksPerJob", 10);
  /**
   * Duration of each task in ms.
   */
  public final long taskDuration = config.getLong("deadlock.taskDuration", 10L);
  /**
   * Duration of each task in nanos.
   */
  public final int taskDurationNanos = config.getInt("deadlock.taskDurationNanos", 0);
  /**
   * Time interval in ms for un-provisioning of the slave nodes, when simulating node crashes
   */
  public final long waitTime = config.getLong("deadlock.waitTime", 15000L);
  /**
   * Wetehr to simulate slave nodes crashes.
   */
  public final boolean simulateNodeCrashes = config.getBoolean("deadlock.simulateNodeCrashes", false);
  /**
   * Whether the tasks should consume CPU rather than just idling via a call to Thread.sleep() for their assigned duration.
   */
  public final boolean useCPU = config.getBoolean("deadlock.useCPU", false);
  /**
   * Size in bytes of the data associated with each task. If < 0 then a null byte[] is initialized, otherwise a bye[] of the specified size.
   */
  public final int dataSize = config.getInt("deadlock.dataSize", -1);
  /**
   * After how many jobs to submit the one that triggers a deadlock in one of the nodes.
   */
  public final int triggerNodeDeadlockAfter = config.getInt("deadlock.triggerNodeDeadlockAfter", -1);
  /**
   * Callback invoked when a job is created by the job streaming pattern.
   */
  public JobCreationCallback jobCreationCallback;
}
