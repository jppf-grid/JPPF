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

package org.jppf.client.event;

import java.util.*;

import org.jppf.client.*;
import org.jppf.client.balancer.ChannelWrapperRemote;
import org.jppf.execute.ExecutorChannel;
import org.jppf.node.protocol.Task;

/**
 * Event emitted by a job when its execution starts or completes.
 * @author Laurent Cohen
 */
public class JobEvent extends EventObject {
  /**
   * The type of event.
   * @exclude
   */
  public enum Type {
    /**
     * The job started.
     */
    JOB_START,
    /**
     * The job ended.
     */
    JOB_END,
    /**
     * The job was disatched to a channel.
     */
    JOB_DISPATCH,
    /**
     * The returnd from a channel.
     */
    JOB_RETURN
  }

  /**
   * The channel to which a job is dispatched or from which it returns.
   */
  private final ExecutorChannel channel;
  /**
   * The tasks that were dispatched or returned.
   */
  private final List<Task<?>> tasks;

  /**
   * Initialize this event with the specified job as its source.
   * @param source the source of this event.
   * @exclude
   */
  public JobEvent(final JPPFJob source) {
    this(source, null, null);
  }

  /**
   * Initialize this event with the specified job as its source.
   * @param source the source of this event.
   * @param channel the channel to which a job is dispatched or from which it returns.
   * @param tasks the tasks that were dispatched or returned.
   * @exclude
   */
  public JobEvent(final JPPFJob source, final ExecutorChannel channel, final List<Task<?>> tasks) {
    super(source);
    this.channel = channel;
    this.tasks = tasks;
  }

  /**
   * Get the source of this event.
   * @return the source as a {@link JPPFJob} object.
   */
  public JPPFJob getJob() {
    return (JPPFJob) getSource();
  }

  /**
   * Get the tasks that were dispatched or returned.
   * <p>This method returns a non <code>null</code> value only for <code>jobDispatched()</code> and <code>jobReturned()</code> events.
   * @return a list of {@link Task} instances.
   * @since 4.0
   */
  public List<Task<?>> getJobTasks() {
    return tasks;
  }

  /**
   * Determine whether the current job dispatch is sent to a remote driver.
   * Note that this method is only useful in the scope of the {@link JobListener#jobDispatched(JobEvent)} and {@link JobListener#jobReturned(JobEvent)} notifications.
   * @return {@code true} if the current job dispatch is executed remotely or the job has not yet been dispatched, {@code false} if the job dispatch is executed locally.
   * @since 4.2
   */
  public boolean isRemoteExecution() {
    return channel == null ? false : !channel.isLocal();
  }

  /**
   * Get the remote driver connection through which the job dispatch was sent.
   * Note that this method is only useful in the scope of the {@link JobListener#jobDispatched(JobEvent)} and {@link JobListener#jobReturned(JobEvent)} notifications.
   * @return a {@link JPPFClientConnection} instance, or {@code null} if {@link #isRemoteExecution()} returns {@code false}.
   * @since 4.2
   */
  public JPPFClientConnection getConnection() {
    return isRemoteExecution() ? ((ChannelWrapperRemote) channel).getChannel() : null;
  }
}
