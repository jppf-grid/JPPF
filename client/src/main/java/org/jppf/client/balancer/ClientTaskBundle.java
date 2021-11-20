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

package org.jppf.client.balancer;

import java.util.*;
import java.util.concurrent.atomic.AtomicLong;

import org.jppf.client.*;
import org.jppf.node.protocol.*;
import org.jppf.node.protocol.graph.*;
import org.jppf.utils.collections.*;
import org.slf4j.*;

/**
 * Instances of this class group tasks from the same client together, so they are sent to the same node,
 * avoiding unnecessary transport overhead.<br>
 * The goal is to provide a performance enhancement through an adaptive bundling of tasks originating from the same client.
 * The bundle size is computed dynamically, depending on the number of nodes connected to the server, and other factors.
 * @author Laurent Cohen
 */
public class ClientTaskBundle extends JPPFTaskBundle {
  /**
   * Logger for this class.
   */
  private static final Logger log = LoggerFactory.getLogger(ClientTaskBundle.class);
  /**
   * Explicit serialVersionUID.
   */
  private static final long serialVersionUID = 1L;
  /**
   * Count of instances of this class.
   */
  private static final AtomicLong instanceCount = new AtomicLong(0L);
  /**
   * The id for this bundle.
   */
  private final long bundleId = instanceCount.incrementAndGet();
  /**
   * The job to execute.
   */
  private final ClientJob job;
  /**
   * The shared data provider for this task bundle.
   */
  private transient DataProvider dataProvider;
  /**
   * The tasks to be executed by the node.
   */
  private transient List<Task<?>> tasks;
  /**
   * The broadcast UUID.
   */
  private transient String broadcastUUID;
  /**
   * Job requeue indicator.
   */
  private boolean requeued;
  /**
   * Job cancel indicator.
   */
  private boolean cancelled;
  /**
   * Information about the task graph, if any, for a job.
   */
  private TaskGraphInfo graphInfo;

  /**
   * Initialize this task bundle and set its build number.
   * @param job the job to execute.
   * @param tasks the list of tasks to execute. This list is copy before being stored in this object,
   * such that no link subsists between the input list and the one retained by this {@code ClientTaskBundle}.
   */
  public ClientTaskBundle(final ClientJob job, final Collection<Task<?>> tasks) {
    if (job == null) throw new IllegalArgumentException("job is null");
    this.job = job;
    this.setSLA(job.getSLA());
    this.setMetadata(job.getJob().getMetadata());
    this.tasks = new ArrayList<>(tasks);
    this.setName(job.getJob().getName());
    setUuid(job.getUuid());
    setTaskCount(this.tasks.size());
    resolveDependencies();
  }

  /**
   * Get the job this job is for.
   * @return a {@link JPPFJob} instance.
   */
  public JPPFJob getJob() {
    return job.getJob();
  }

  /**
   * Get the client job this job is for.
   * @return a {@link ClientJob} instance.
   */
  public ClientJob getClientJob() {
    return job;
  }

  /**
   * Get shared data provider for this task.
   * @return a {@code DataProvider} instance.
   */
  public DataProvider getDataProviderL() {
    return dataProvider;
  }

  /**
   * Set shared data provider for this task.
   * @param dataProvider a {@code DataProvider} instance.
   */
  public void setDataProviderL(final DataProvider dataProvider) {
    this.dataProvider = dataProvider;
  }

  /**
   * Get the tasks to be executed by the node.
   * @return the tasks as a {@code List} of arrays of bytes.
   */
  public List<Task<?>> getTasksL() {
    return tasks;
  }

  /**
   * Make a copy of this bundle.
   * @return a new {@code ClientTaskBundle} instance.
   */
  @Override
  public ClientTaskBundle copy() {
    throw new UnsupportedOperationException();
  }

  /**
   * Get the broadcast UUID.
   * @return the broadcast UUID as a {@code String}.
   */
  public String getBroadcastUUID() {
    return broadcastUUID;
  }

  /**
   * Set the broadcast UUID.
   * @param broadcastUUID the broadcast UUID as a {@code String}.
   */
  public void setBroadcastUUID(final String broadcastUUID) {
    this.broadcastUUID = broadcastUUID;
  }

  /**
   * Called when all or part of a job is dispatched to a node.
   * @param channel the node to which the job is dispatched.
   */
  public void jobDispatched(final ChannelWrapper channel) {
    job.jobDispatched(this, channel);
  }

  /**
   * Called to notify that the results of a number of tasks have been received from the server.
   * @param results the list of tasks whose results have been received from the server.
   */
  public void resultsReceived(final List<Task<?>> results) {
    job.resultsReceived(this, results);
  }

  /**
   * Called to notify that throwable eventually raised while receiving the results.
   * @param throwable the throwable that was raised while receiving the results.
   */
  public void resultsReceived(final Throwable throwable) {
    job.resultsReceived(this, throwable);
  }

  /**
   * Called to notify that the execution of a task has completed.
   * @param exception the {@link Exception} thrown during job execution or {@code null}.
   */
  public void taskCompleted(final Exception exception) {
    job.taskCompleted(this, exception);
  }

  /**
   * Called when this task bundle should be resubmitted.
   */
  public synchronized void resubmit() {
    if (getSLA().isBroadcastJob()) return; // broadcast jobs cannot be resubmitted.
    requeued = true;
  }

  /**
   * Get the requeued indicator.
   * @return {@code true} if job is requeued, {@code false} otherwise.
   */
  public synchronized boolean isRequeued() {
    return requeued;
  }

  /**
   * Called when this task bundle is cancelled.
   */
  public synchronized void cancel() {
    this.cancelled = true;
  }

  /**
   * Get the cancelled indicator.
   * @return <{@code true} if job is cancelled, {@code false} otherwise.
   */
  public synchronized boolean isCancelled() {
    return cancelled;
  }

  @Override
  public String toString() {
    final StringBuilder sb = new StringBuilder(getClass().getSimpleName()).append('[');
    sb.append("bundleId=").append(bundleId);
    sb.append(", name=").append(getName());
    sb.append(", jobUuid=").append(getUuid());
    sb.append(", initialTaskCount=").append(getInitialTaskCount());
    sb.append(", taskCount=").append(getTaskCount());
    sb.append(", requeue=").append(isRequeued());
    sb.append(", cancelled=").append(isCancelled());
    sb.append(']');
    return sb.toString();
  }

  /**
   * @return the id for this bundle.
   */
  @Override
  public Long getBundleId() {
    return bundleId;
  }

  /**
   * Resolve the dependneices, if any, of the atsks in this bundle.
   */
  private void resolveDependencies() {
    final TaskGraph graph = job.getTaskGraph();
    if (graph == null) return;
    if (!job.getJob().getClientSLA().isGraphTraversalInClient()) return;
    List<PositionalElement<?>> dependencies = null;
    CollectionMap<Integer, Integer> dependenciesMap = null;
    final JobResults jobResults = job.getJob().getResults();
    final Set<Integer> nullResultPositions = new HashSet<>();
    for (final Task<?> task: tasks) {
      final TaskGraph.Node node = graph.nodeAt(task.getPosition());
      if (node == null) continue;
      final List<TaskGraph.Node> deps = node.getDependencies();
      if ((deps != null) && !deps.isEmpty()) {
        for (final TaskGraph.Node dep: deps) {
          if (dependencies == null) {
            dependencies = new ArrayList<>();
            dependenciesMap = new ArrayListHashMap<>();
          }
          final int depPosition = dep.getPosition();
          if (nullResultPositions.contains(depPosition)) continue;
          final Task<?> depTask = jobResults.getResultTask(depPosition);
          if (depTask == null) {
            log.warn("null dependency at position {} added to {}", depPosition, task);
            nullResultPositions.add(depPosition);
          } else { 
            dependencies.add(depTask);
            dependenciesMap.putValue(task.getPosition(), depPosition);
          }
        }
      }
    }
    if (dependencies != null) {
      final int[] depsPositions = new int[dependencies.size()];
      int count = 0;
      for (PositionalElement<?> elt: dependencies) depsPositions[count++] = elt.getPosition();
      this.graphInfo = new TaskGraphInfo(dependencies.size(), dependenciesMap, depsPositions);
      this.graphInfo.setDependencies(dependencies);
    }
  }

  /**
   * @return information about the task graph, if any, for a job. 
   */
  public TaskGraphInfo getGraphInfo() {
    return graphInfo;
  }
}
