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

package org.jppf.node.protocol;

import org.jppf.scheduling.JPPFSchedule;


/**
 * This interface represents the Service Level Agreement between a JPPF job and a server.
 * It determines the state, conditions and order in which a job will be executed.
 * @author Laurent Cohen
 */
public interface JobSLA extends JobCommonSLA
{
  /**
   * Get the priority of this job.
   * @return the priority as an int.
   */
  int getPriority();

  /**
   * Set the priority of this job.
   * @param priority the priority as an int.
   */
  void setPriority(int priority);

  /**
   * Get the maximum number of nodes this job can run on.
   * @return the number of nodes as an int value.
   */
  int getMaxNodes();

  /**
   * Set the maximum number of nodes this job can run on.
   * @param maxNodes the number of nodes as an int value. A value <= 0 means no limit on the number of nodes.
   */
  void setMaxNodes(int maxNodes);

  /**
   * Determine whether this job is initially suspended.
   * @return true if the job is suspended, false otherwise.
   */
  boolean isSuspended();

  /**
   * Specify whether this job is initially suspended.
   * @param suspended true if the job is suspended, false otherwise.
   */
  void setSuspended(boolean suspended);

  /**
   * Determine whether the job is a broadcast job.
   * @return true for a broadcast job, false otherwise.
   */
  boolean isBroadcastJob();

  /**
   * Specify whether the job is a broadcast job.
   * @param broadcastJob true for a broadcast job, false otherwise.
   */
  void setBroadcastJob(boolean broadcastJob);

  /**
   * Determine whether the job should be canceled by the driver if the client gets disconnected.
   * @return <code>true</code> if the job should be canceled (this is the default), <code>false</code> otherwise.
   */
  boolean isCancelUponClientDisconnect();

  /**
   * Specify whether the job should be canceled by the driver if the client gets disconnected.
   * @param cancelUponClientDisconnect <code>true</code> if the job should be canceled, <code>false</code> otherwise.
   */
  void setCancelUponClientDisconnect(boolean cancelUponClientDisconnect);

  /**
   * Get the strategy used to return the results back to the client.
   * @return the name of the strategy to use.
   * @exclude
   */
  String getResultsStrategy();

  /**
   * Set the strategy used to return the results back to the client.
   * @param name the name of the strategy to use.
   * @exclude
   */
  void setResultsStrategy(String name);

  /**
   * Get the class path associated with the job.
   * @return an instance of {@link ClassPath}.
   */
  ClassPath getClassPath();

  /**
   * Set the class path associated with the job.
   * @param classpath an instance of {@link ClassPath}.
   */
  void setClassPath(ClassPath classpath);

  /**
   * Get the expiration schedule for any subset of the job dispatched to a node.
   * @return a {@link JPPFSchedule} instance.
   */
  JPPFSchedule getDispatchExpirationSchedule();

  /**
   * Set the expiration schedule for any subset of the job dispatched to a node.
   * @param schedule a {@link JPPFSchedule} instance.
   */
  void setDispatchExpirationSchedule(JPPFSchedule schedule);

  /**
   * Get the number of times a dispatched task can expire before it is finally cancelled.
   * @return the number of expirations as an int.
   */
  int getMaxDispatchExpirations();

  /**
   * Set the number of times a dispatched task can expire before it is finally cancelled.
   * @param max the number of expirations as an int.
   */
  void setMaxDispatchExpirations(int max);

  /**
   * Get the naximum number of times a task can resubmit itself via {@link org.jppf.node.protocol.AbstractTask#setResubmit(boolean) AbstractTask.setResubmit(boolean)}.
   * The default value is 1, meaning that a task can resubmit itself at most once.
   * @return the maximum number of resubmits; a value of 0 or less means tasks in the job cannot be resubmitted.
   */
  int getMaxTaskResubmits();

  /**
   * Set the naximum number of times a task can resubmit itself via {@link org.jppf.node.protocol.AbstractTask#setResubmit(boolean) AbstractTask.setResubmit(boolean)}.
   * @param maxResubmits the maximum number of resubmits; a value of 0 or less means tasks in the job cannot be resubmitted.
   */
  void setMaxTaskResubmits(int maxResubmits);
}
