/*
 * JPPF.
 * Copyright (C) 2005-2012 JPPF Team.
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

package org.jppf.client.balancer;

import org.jppf.server.protocol.JPPFTask;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * @author Martin JANDA
 */
public class ClientJob
{
  /**
   * The underlying task bundle.
   */
  private final ClientTaskBundle job;
  /**
   * The list of of the tasks.
   */
  private final List<JPPFTask> tasks;
  /**
   * The task completion listener to notify, once the execution of this task has completed.
   */
  private ClientCompletionListener completionListener = null;

  /**
   * Initialized client job with task bundle and list of tasks to execute.
   * @param job underlying task bundle.
   * @param tasks list of tasks to execute.
   */
  public ClientJob(final ClientTaskBundle job, final List<JPPFTask> tasks)
  {
    if(job == null) throw new IllegalArgumentException("job is null");
    if(tasks == null) throw new IllegalArgumentException("tasks is null");
    
    this.job = job;
    this.tasks = Collections.unmodifiableList(tasks);
  }

  /**
   * Get the underlying task bundle.
   * @return a <code>ClientTaskBundle</code> instance.
   */
 public ClientTaskBundle getJob() {
    return job;
  }

  /**
   * Get the list of of the tasks.
   * @return a list of <code>JPPFTask</code> instances.
   */
  public List<JPPFTask> getTasks() {
    return Collections.unmodifiableList(tasks);
  }

  /**
   * Make a copy of this client job wrapper.
   * @return a new <code>ClientJob</code> instance.
   */
  public ClientJob copy() {
    return copy(this.tasks.size());
  }

  /**
   * Make a copy of this client job wrapper containing only the first nbTasks tasks it contains.
   * @param nbTasks the number of tasks to include in the copy.
   * @return a new <code>ClientJob</code> instance.
   */
  public ClientJob copy(final int nbTasks)
  {
    if(nbTasks == this.tasks.size())
      return new ClientJob(job.copy(), this.tasks);
    else
      return new ClientJob(job.copy(), this.tasks.subList(0, nbTasks));
  }

  /**
   * Merge this client job wrapper with another.
   * @param that the wrapper to merge with.
   * @param after determines whether the tasks from other should be added first or last.
   */
  public void merge(final ClientJob that, final boolean after)
  {
    List<JPPFTask> list = new ArrayList<JPPFTask>(this.tasks.size() + that.tasks.size());
    if(!after) list.addAll(that.tasks);
    list.addAll(this.tasks);
    if(after) list.addAll(that.tasks);
    
    
//      int n = that.getJob().getTaskCount();
//      job.setTaskCount(job.getTaskCount() + n);
//      job.getSLA().setSuspended(that.getJob().getSLA().isSuspended());
//      if (after)
//      {
//        for (DataLocation task: that.getTasks()) tasks.add(task);
//      }
//      else
//      {
//        for (int i=n-1; i>=0; i--) tasks.add(0, that.getTasks().get(i));
//      }
  }

  /**
   * Get the task completion listener to notify, once the execution of this task has completed.
   * @return a <code>TaskCompletionListener</code> instance.
   */
 public ClientCompletionListener getCompletionListener()
  {
    return completionListener;
  }

  /**
   * Set the task completion listener to notify, once the execution of this task has completed.
   * @param completionListener a <code>TaskCompletionListener</code> instance.
   */
  public void setCompletionListener(final ClientCompletionListener completionListener)
  {
    this.completionListener = completionListener;
  }

  /**
   * Notifies that execution of this task has completed.
   */
 public void fireTaskCompleted() {
    if(this.completionListener != null) this.completionListener.taskCompleted(this);
  }
}
