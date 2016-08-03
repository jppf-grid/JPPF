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

package org.jppf.jca.demo;

import java.util.*;

import org.jppf.client.*;
import org.jppf.jca.cci.*;
import org.jppf.node.protocol.Task;

/**
 * Instances of this class encapsulate a simple call to the JPPF resource adapter.
 * @author Laurent Cohen
 */
public class J2EEDemo {
  /**
   * JNDI name of the JPPFConnectionFactory.
   */
  private final String jndiBinding;

  /**
   * Initialize this test object with a specified jndi location for the connection factory.
   * @param jndiBinding JNDI name of the JPPFConnectionFactory.
   */
  public J2EEDemo(final String jndiBinding) {
    this.jndiBinding = jndiBinding;
  }

  /**
   * Perform a simple call to the JPPF resource adapter.
   * @param duration the duration of the task to submit.
   * @return a string reporting either the task execution result or an error message.
   * @throws Exception if the call to JPPF failed.
   */
  public String testConnector(final int duration) throws Exception {
    JPPFConnection connection = null;
    String id = null;
    try {
      connection = JPPFHelper.getConnection(jndiBinding);
      JPPFJob job = new JPPFJob();
      job.add(new DemoTask(duration));
      id = connection.submit(job);
    } finally {
      if (connection != null) JPPFHelper.closeConnection(connection);
    }
    return id;
  }

  /**
   * Perform a simple call to the JPPF resource adapter.
   * @param jobId the name given to the job.
   * @param duration the duration of the task to submit.
   * @param nbTasks the number of tasks to submit.
   * @return a string reporting either the task execution result or an error message.
   * @throws Exception if the call to JPPF failed.
   */
  public String testConnector(final String jobId, final long duration, final int nbTasks) throws Exception {
    JPPFConnection connection = null;
    JPPFJob job = null;
    String id = null;
    try {
      connection = JPPFHelper.getConnection(jndiBinding);
      job = new JPPFJob();
      job.setName(jobId);
      for (int i=0; i<nbTasks; i++) {
        DemoTask task = new DemoTask(duration);
        task.setId(jobId + " task #" + (i + 1));
        job.add(task);
      }
      id = connection.submit(job);
    } finally {
      if (connection != null) JPPFHelper.closeConnection(connection);
    }
    JPPFHelper.getStatusMap().put(id, job);
    return id;
  }

  /**
   * Perform a simple call to the JPPF resource adapter.
   * @param nbJobs the number of jobs to submit.
   * @param jobNamePrefix the prefix of the name given to each job.
   * @param duration the duration of each task to submit.
   * @param nbTasks the number of tasks to submit for each job.
   * @param blocking whether th jobs are blocking or not.
   * @return a string reporting either the task execution result or an error message.
   * @throws Exception if the call to JPPF failed.
   */
  public String testMultipleJobs(final int nbJobs, final String jobNamePrefix, final long duration, final int nbTasks, final boolean blocking) throws Exception {
    if (nbJobs <= 0) return "Error: the number of jobs must be >= 1";
    if (nbTasks <= 0) return "Error: the number of tasks must be >= 1";
    if (duration <= 0L) return "Error: the duration must be >= 1";
    JPPFConnection connection = null;
    String id = null;
    List<String> idList = new ArrayList<>();
    try {
      JPPFConnectionFactory factory = JPPFHelper.getConnectionFactory(jndiBinding);
      boolean available = factory.isJPPFDriverAvailable();
      // enable local execution, depending on whether a remote connection is available or not.
      if (!available) System.out.println("No available JPPF driver, jobs will be executed locally");
      factory.enableLocalExecution(!available);
      connection = (JPPFConnection) factory.getConnection();
      for (int n=1; n<=nbJobs; n++) {
        JPPFJob job = new JPPFJob();
        String name = jobNamePrefix + ' ' + n;
        job.setName(name);
        job.setBlocking(false);
        for (int i=1; i<=nbTasks; i++) job.add(new DemoTask(duration)).setId(name + " task " + i);
        id = connection.submit(job);
        idList.add(id);
        JPPFHelper.getStatusMap().put(id, job);
        if (blocking) connection.awaitResults(id);
      }
    } finally {
      if (connection != null) JPPFHelper.closeConnection(connection);
    }
    return "success";
  }

  /**
   * Perform a simple call to the JPPF resource adapter.
   * This method blocks until all job results have been received.
   * @param jobId the name given to the job.
   * @param duration the duration of the task to submit.
   * @param nbTasks the number of tasks to submit.
   * @return a string reporting either the task execution result or an error message.
   * @throws Exception if the call to JPPF failed.
   */
  public String testConnectorBlocking(final String jobId, final long duration, final int nbTasks) throws Exception {
    JPPFConnection connection = null;
    JPPFJob job = null;
    String id = null;
    try {
      connection = JPPFHelper.getConnection(jndiBinding);
      job = new JPPFJob();
      job.setName(jobId);
      for (int i=0; i<nbTasks; i++) {
        DemoTask task = new DemoTask(duration);
        task.setId(jobId + " task #" + (i + 1));
        job.add(task);
      }
      id = connection.submit(job);
      List<Task<?>> results = connection.awaitResults(id);
      System.out.println("received " + results.size() + " results for job '" + job.getName() + "'");
    } finally {
      if (connection != null) JPPFHelper.closeConnection(connection);
    }
    JPPFHelper.getStatusMap().put(id, job);
    return id;
  }

  /**
   * Get the map of job uuids to their corresponding job status.
   * @return a map of ids to statuses as strings.
   * @throws Exception if the call to JPPF failed.
   */
  public Map<String, String> getStatusMap() throws Exception {
    Map<String, String> map = new HashMap<>();
    JPPFConnection connection = null;
    try {
      connection = JPPFHelper.getConnection(jndiBinding);
      Collection<String> coll = connection.getAllJobIds();
      for (String id: coll) {
        JobStatus status = connection.getJobStatus(id);
        String s = (status == null) ? "Unknown" : status.toString();
        map.put(id, s);
      }
    } finally {
      if (connection != null) JPPFHelper.closeConnection(connection);
    }
    return map;
  }

  /**
   * Get the resulting message from a submission.
   * @param id the id of the submission to retrieve.
   * @return a string reporting either the task execution result or an error message.
   * @throws Exception if the call to JPPF failed.
   */
  public String getMessage(final String id) throws Exception {
    JPPFConnection connection = null;
    String msg = null;
    try {
      connection = JPPFHelper.getConnection(jndiBinding);
      List<Task<?>> results = connection.getResults(id);
      if (results == null) msg = "submission is not in queue anymore";
      else {
        StringBuilder sb = new StringBuilder();
        for (Task<?> task: results) {
          if (task.getThrowable() == null) sb.append(task.getResult());
          else sb.append("task [").append(task.getId()).append("] ended in error: ").append(task.getThrowable().getMessage());
          sb.append("<br/>");
        }
        msg = sb.toString();
      }
    } finally {
      if (connection != null) JPPFHelper.closeConnection(connection);
    }
    return msg;
  }
}
