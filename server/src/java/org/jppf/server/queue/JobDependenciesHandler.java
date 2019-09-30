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

package org.jppf.server.queue;

import java.util.*;

import org.jppf.node.protocol.JobDependencySpec;
import org.jppf.node.protocol.graph.*;
import org.jppf.server.protocol.ServerJob;
import org.slf4j.*;

/**
 * This class handles the interactions between the server queue and jobs and the job dependency graph.
 * @author Laurent Cohen
 * @exclude
 */
public class JobDependenciesHandler {
  /**
   * Logger for this class.
   */
  private static final Logger log = LoggerFactory.getLogger(JobDependenciesHandler.class);
  /**
   * Determines whether the debug level is enabled in the log configuration, without the cost of a method call.
   */
  private static final boolean debugEnabled = log.isDebugEnabled();
  /**
   * The dependency graph.
   */
  private final JobDependencyGraph graph = new JobDependencyGraph();
  /**
   * The job queue.
   */
  private final JPPFPriorityQueue queue;

  /**
   * Create this dependency handler.
   * @param queue the job queue.
   */
  public JobDependenciesHandler(final JPPFPriorityQueue queue) {
    this.queue = queue;
  }

  /**
   * Called when a job with dependencies arrives in the client or server queue.
   * @param job the job to processs.
   * @return {@code true} if the job should be cancelled, {@code false} otherwise.
   */
  public boolean jobQueued(final ServerJob job) {
    final JobDependencySpec spec = job.getSLA().getDependencySpec();
    if (spec.getId() != null) {
      try {
        final JobDependencyNode node = graph.addNode(spec, job.getUuid());
        if (debugEnabled) log.debug("'{}' was queued and added to the dependency graph as {}", job.getName(), node);
        return node.isCancelled();
      } catch (final JPPFJobDependencyCycleException e) {
        final JobDependencyNode node = graph.getNode(spec.getId());
        log.error("detected dependency cycle when queuing {}, node = {}", job, node, e);
        if (node != null) node.setRemoveUponCompletion(true);
        final List<String> toCancel = e.getIdPath();
        if ((toCancel != null) && !toCancel.isEmpty()) {
          if (debugEnabled) log.debug("cycle id path: {}", toCancel);
          cancelNodes(toCancel);
        }
      }
    }
    return true;
  }

  /**
   * Called when a job ends and is removed from the client or server queue.
   * @param job the job to process.
   */
  public void jobEnded(final ServerJob job) {
    if (debugEnabled) log.debug("processor: '{}' has ended", job.getName());
    // Retrieve the jobs whose only remaining dependency is the current job and resume them
    final List<JobDependencyNode> toResume = graph.jobEnded(job.getUuid());
    if (debugEnabled && (toResume != null)) {
      for (final JobDependencyNode jobNode: toResume) log.debug("resuming '{}'", jobNode.getId());
    }
    JobDependencyNode node = graph.getNodeByJobUuid(job.getUuid());
    if (node == null) node = graph.getNode(job.getSLA().getDependencySpec().getId());
    if (debugEnabled) log.debug("node ended: {}", node);
    if ((node != null) && node.isRemoveUponCompletion()) graph.removeNode(node);
  }

  /**
   * Determine whether the specified job has any pending dependency.
   * @param id the dependency id of the job to check.
   * @return {@code true} if the job has at least one pendeing dependency, {@code false} otherwise.
   */
  public boolean hasPendingDependencyOrCancelled(final String id) {
    final JobDependencyNode node = graph.getNode(id);
    return (node != null) && (node.hasPendingDependency() || node.isCancelled());
  }

  /**
   * Called when a job with dependencies is cancelled.
   * @param job the job to processs.
   */
  public void jobCancelled(final ServerJob job) {
    final JobDependencySpec spec = job.getSLA().getDependencySpec();
    if ((spec.getId() != null) && spec.isCascadeCancellation()) {
      if (debugEnabled) log.debug("handling cancellation of {}", job);
      final JobDependencyNode node = graph.getNode(spec.getId());
      if (node != null) {
        final List<String> toCancel = processCancellation(node);
        if (debugEnabled) log.debug("cancelling dependent jobs {}", toCancel);
        for (final String uuid: toCancel) {
          final ServerJob dependentJob = queue.getJob(uuid);
          if (dependentJob != null) dependentJob.cancel(queue.driver, true);
        }
      }
    }
  }

  /**
   * Process the cancellation cascade for the specified graph node.
   * @param node the node reprsenting a job being cancelled.
   * @return a list of job uuids to cancel.
   */
  private List<String> processCancellation(final JobDependencyNode node) {
    final List<String> toCancel = new ArrayList<>();
    synchronized(graph) {
      final Collection<JobDependencyNode> dependedOn = node.getDependendedOn();
      if (dependedOn != null) {
        for (final JobDependencyNode dependent: dependedOn) {
          if (debugEnabled) log.debug("cancelling {}", dependent);
          if (!dependent.isCompleted() && !dependent.isCancelled() && (dependent.getJobUuid() != null)) toCancel.add(dependent.getJobUuid());
          dependent.setCancelled(true);
        }
      }
    }
    return toCancel;
  }

  /**
   * @return the dependency graph.
   */
  public JobDependencyGraph getGraph() {
    return graph;
  }

  /**
   * Cancel the nodes with the sepcified ids.
   * @param nodeIds the ids of nodes whose corresponding jobs to cancel.
   */
  private void cancelNodes(final List<String> nodeIds) {
    if (debugEnabled) log.debug("cancelling nodes {}", nodeIds);
    final List<String> toCancel = new ArrayList<>();
    synchronized(graph) {
      for (final String id: nodeIds) {
        final JobDependencyNode node = graph.getNode(id);
        if (node != null) {
          if (debugEnabled) log.debug("cancelling {}", node);
          if (!node.isCompleted() && !node.isCancelled() && (node.getJobUuid() != null)) toCancel.add(node.getJobUuid());
          node.setCancelled(true);
          node.setRemoveUponCompletion(true);
        }
      }
    }
    if (debugEnabled) log.debug("cancelling jobs {}", toCancel);
    for (final String uuid: toCancel) {
      final ServerJob job = queue.getJob(uuid);
      if (job != null) job.cancel(queue.driver, true);
    }
  }
}
