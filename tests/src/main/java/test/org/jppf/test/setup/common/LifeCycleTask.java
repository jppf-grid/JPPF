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

package test.org.jppf.test.setup.common;

import java.io.Serializable;

import org.jppf.node.protocol.*;
import org.jppf.utils.*;
import org.slf4j.*;

/**
 * A simple JPPF task for unit-testing the task life cycle.
 * @author Laurent Cohen
 */
public class LifeCycleTask extends AbstractTask<String> {
  /**
   * Explicit serialVersionUID.
   */
  private static final long serialVersionUID = 1L;
  /**
   * Logger for this class.
   */
  private static Logger log = LoggerFactory.getLogger(LifeCycleTask.class);
  /**
   * Determines whether the trace level is enabled in the log configuration, without the cost of a method call.
   */
  private static final boolean traceEnabled = log.isTraceEnabled();
  /**
   * One million.
   */
  protected static final int ONE_MILLION = 1000 * 1000;
  /**
   * The duration of this task.
   */
  protected long duration;
  /**
   * used to store the task's execution start time in nanoseconds.
   */
  protected double start;
  /**
   * Measures the time elapsed between the task execution start and its completion in nanoseconds.
   */
  protected double elapsed;
  /**
   * Determines whether this task was cancelled.
   */
  protected boolean cancelled;
  /**
   * Determines whether this task timed out.
   */
  protected boolean timedout;
  /**
   * Determines whether this task was executed in a node or in the client's local executor.
   */
  protected boolean executedInNode = true;
  /**
   * The uuid of the node this task executes on.
   */
  protected String nodeUuid;
  /**
   * Whether this task can be interrupted upon cancellation or timeout.
   */
  protected boolean interruptible = true;
  /**
   * Whether the thread running this task was interrupted upon cancellation or timeout.
   */
  protected boolean interrupted;
  /**
   * A message to send via JMX.
   */
  protected String startNotification;
  /**
   * The uuid of the node obtained via {@code Task.getNode().getUuid()}.
   */
  protected String uuidFromNode;
  /**
   * Info on the job this itask is a part of.
   */
  protected JobInfo jobInfo;

  /**
   * Initialize this task.
   */
  public LifeCycleTask() {
  }

  /**
   * Initialize this task.
   * @param duration specifies the duration of this task.
   */
  public LifeCycleTask(final long duration) {
    this(duration, true, null);
  }

  /**
   * Initialize this task.
   * @param duration specifies the duration of this task.
   * @param interruptible whether this task can be interrupted upon cancellation or timeout.
   */
  public LifeCycleTask(final long duration, final boolean interruptible) {
    this(duration, interruptible, null);
  }

  /**
   * Initialize this task.
   * @param duration specifies the duration of this task.
   * @param interruptible whether this task can be interrupted upon cancellation or timeout.
   * @param startNotification a message to send via JMX.
   */
  public LifeCycleTask(final long duration, final boolean interruptible, final String startNotification) {
    this.duration = duration;
    this.interruptible = interruptible;
    this.startNotification = startNotification;
  }

  @Override
  public void run() {
    final long nanoStart = System.nanoTime();
    start = System.currentTimeMillis();
    start *= ONE_MILLION;
    final JPPFDistributedJob job = this.getJob();
    if (job != null) jobInfo = new JobInfo(job);
    if (startNotification != null) {
      if (traceEnabled) log.trace("firing user task notification '{}'", startNotification);
      fireNotification(startNotification, true);
    } else if (traceEnabled) log.trace("no user task notification");
    try {
      executedInNode = isInNode();
      final TypedProperties config = isInNode() ? getNode().getConfiguration() : JPPFConfiguration.getProperties();
      synchronized(config) {
        nodeUuid = config.getString("jppf.node.uuid");
        if (nodeUuid == null) {
          nodeUuid = JPPFUuid.normalUUID();
          config.setProperty("jppf.node.uuid", nodeUuid);
        }
      }
      if (isInNode()) uuidFromNode = getNode().getUuid();
      else uuidFromNode = "local_channel";
      final long sleepStart = System.nanoTime();
      if (duration > 0L) Thread.sleep(duration);
      final long sleepElapsed = (System.nanoTime() - sleepStart) / 1_000_000L;
      setResult(BaseTestHelper.EXECUTION_SUCCESSFUL_MESSAGE);
      displayTask("successful - slept for " + sleepElapsed + " ms");
    } catch(final Exception e) {
      setThrowable(e);
    } finally {
      elapsed = System.nanoTime() - nanoStart;
    }
  }

  @Override
  public void onCancel() {
    cancelled = true;
    displayTask("cancelled");
  }

  @Override
  public void onTimeout() {
    timedout = true;
    displayTask("timed out");
  }

  /**
   * Log or display a message showing the execution status and elapsed of this task.
   * @param message a short message describing the life cycle status.
   */
  private void displayTask(final String message) {
    log.info("displaying [{}] task {}", message, this);
  }

  /**
   * Determine whether this task was cancelled.
   * @return <code>true</code> if the task was cancelled, <code>false</code> otherwise.
   */
  public boolean isCancelled() {
    return cancelled;
  }

  /**
   * Determine whether this task timed out.
   * @return <code>true</code> if the task timed out, <code>false</code> otherwise.
   */
  public boolean isTimedout() {
    return timedout;
  }

  /**
   * Determine whether this task was executed in a node or in the client's local executor.
   * @return <code>true</code> if this task was executed in a node, <code>false</code> otherwise.
   */
  public boolean isExecutedInNode() {
    return executedInNode;
  }

  @Override
  public String toString() {
    final StringBuilder sb = new StringBuilder(getClass().getSimpleName()).append('[');
    sb.append("id=").append(getId());
    sb.append(", duration=").append(duration);
    sb.append(", timedout=").append(timedout);
    sb.append(", cancelled=").append(cancelled);
    sb.append(", executedInNode=").append(executedInNode);
    sb.append(", elapsed=").append(elapsed);
    sb.append(", result=").append(getResult());
    sb.append(", nodeUuid=").append(nodeUuid);
    sb.append(']');
    return sb.toString();
  }

  /**
   * Get the task's execution start time.
   * @return the start time in nanoseconds.
   */
  public double getStart() {
    return start;
  }

  /**
   * Get the time elapsed between the task execution start its completion.
   * @return the elapsed time in nanoseconds.
   */
  public double getElapsed() {
    return elapsed;
  }

  /**
   * Get the uuid of the node this task executes on.
   * @return the uuid as a string.
   */
  public String getNodeUuid() {
    return nodeUuid;
  }

  @Override
  public boolean isInterruptible() {
    return interruptible;
  }

  /**
   * Specify whether this task can be interrupted upon cancellation or timeout.
   * @param interruptible {@code true} to mark the task as interruptible, false otherwise.
   */
  public void setInterruptible(final boolean interruptible) {
    this.interruptible = interruptible;
  }

  /**
   * Determine whether the thread running this task was interrupted upon cancellation or timeout.
   * @return {@code true} if the thread was interrupted, {@code false} otherwise.
   */
  public boolean isInterrupted() {
    return interrupted;
  }

  /**
   * The uuid of the node obtained via {@code Task.getNode().getUuid()}.
   * @return the node uuid, of {@code null} if the task did not execute in a node.
   */
  public String getUuidFromNode() {
    return uuidFromNode;
  }

  /**
   * @return info on the job this itask is a part of.
   */
  public JobInfo getJobInfo() {
    return jobInfo;
  }

  /**
   * Info on the job this itask is a part of.
   */
  public static class JobInfo implements Serializable {
    /** */
    public final String uuid;
    /** */
    public final String name;
    /** */
    public final int taskCount;
    /** */
    public final String jobClassName;

    /**
     * @param job .
     */
    public JobInfo(final JPPFDistributedJob job) {
      this.uuid = job.getUuid();
      this.name = job.getName();
      this.taskCount = job.getTaskCount();
      this.jobClassName = job.getClass().getName();
    }
  }
}
