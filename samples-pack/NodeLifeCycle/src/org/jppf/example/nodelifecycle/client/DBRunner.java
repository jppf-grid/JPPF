/*
 * JPPF.
 * Copyright (C) 2005-2017 JPPF Team.
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
package org.jppf.example.nodelifecycle.client;

import java.sql.*;
import java.util.Collection;

import org.jppf.client.*;
import org.jppf.client.event.*;
import org.jppf.management.*;
import org.jppf.node.protocol.Task;
import org.jppf.utils.*;
import org.jppf.utils.Operator;
import org.slf4j.*;

/**
 * Runner class for the square matrix multiplication demo.
 * @author Laurent Cohen
 */
public class DBRunner {
  /**
   * Logger for this class.
   */
  private static Logger log = LoggerFactory.getLogger(DBRunner.class);
  /**
   * JPPF client used to submit execution requests.
   */
  private static JPPFClient jppfClient = null;
  /**
   * A JMX connection to one of the nodes.
   */
  private static JMXNodeConnectionWrapper jmxNode = null;

  /***
   * Send a job with a number of tasks that insert a row in a database table.
   * We use the management APIs to kill a node before the job execution is complete,
   * so we can demonstrate the transaction recovery mechanism (implemented in Atomikos)
   * on the node side. Once the job is complete, we display all the rows in the table.
   * @param args the first argument, if any, will be used as the JPPF client's uuid.
   */
  public static void main(final String... args) {
    try {
      final TypedProperties config = JPPFConfiguration.getProperties();
      final int nbTasks = config.getInt("job.nbtasks", 20);
      final long taskSleepTime = config.getLong("task.sleep.time", 2000L);
      final long timeBeforeRestartNode = config.getLong("time.before.restart.node", 6000L);
      if ((args != null) && (args.length > 0)) jppfClient = new JPPFClient(args[0]);
      else jppfClient = new JPPFClient();
      // Initialize the JMX connection to the node
      getNode();
      // Create a job with the specified number of tasks
      final JPPFJob job = new JPPFJob();
      job.setName("NodeLifeCycle demo job");
      for (int i = 1; i <= nbTasks; i++) {
        final DBTask task = new DBTask(taskSleepTime);
        task.setId("" + i);
        job.add(task);
      }
      job.setBlocking(false);
      // customize the result listener to display a message each time a task result is received
      final JobListener jobListener = new JobListenerAdapter() {
        @Override
        public synchronized void jobReturned(final JobEvent event) {
          for (Task<?> task : event.getJobTasks()) {
            if (task.getThrowable() != null) output("task " + task.getId() + " error: " + task.getThrowable().getMessage());
            else output("task " + task.getId() + " result: " + task.getResult());
          }
        }
      };
      job.addJobListener(jobListener);
      jppfClient.submitJob(job);
      Thread.sleep(timeBeforeRestartNode);
      // restart the node to demonstrate the transaction recovery
      output("restarting node");
      restartNode();
      // wait for the job completion
      job.awaitResults();
      // display the list of rows in the DB table
      if (config.getBoolean("display.db.content", false)) displayDBContent();
      output("demo ended");
    } catch (final Exception e) {
      e.printStackTrace();
    } finally {
      if (jppfClient != null) jppfClient.close();
    }
  }

  /**
   * Get a JMX connection to one of the nodes.
   * @return the node connection as a {@link JMXNodeConnectionWrapper} instance.
   * @throws Exception if the node connection could not be established.
   */
  private static JMXNodeConnectionWrapper getNode() throws Exception {
    if (jmxNode == null) {
      final JMXDriverConnectionWrapper jmxDriver = jppfClient.awaitActiveConnectionPool().awaitJMXConnections(Operator.AT_LEAST, 1, true).get(0);
      final Collection<JPPFManagementInfo> nodesInfo = jmxDriver.nodesInformation();
      final JPPFManagementInfo info = nodesInfo.iterator().next();
      jmxNode = new JMXNodeConnectionWrapper(info.getHost(), info.getPort());
      jmxNode.connect();
    }
    return jmxNode;
  }

  /**
   * Kill the node.
   */
  private static void restartNode() {
    try {
      final JMXNodeConnectionWrapper jmxNode = getNode();
      jmxNode.restart();
    } catch (final Exception e) {
      output("Could not restart a node:\n" + ExceptionUtils.getStackTrace(e));
    }
  }

  /**
   * List all the rows in the TASK_RESULT table.
   * @throws Exception if any error occurs.
   */
  private static void displayDBContent() throws Exception {
    Class.forName("org.h2.Driver");
    final Connection c = DriverManager.getConnection("jdbc:h2:tcp://localhost:9092/./jppf_samples;SCHEMA=PUBLIC", "jppf", "jppf");
    //Connection c = DriverManager.getConnection("jdbc:postgresql://localhost:5432/jppf_samples", "jppf", "jppf");
    final String sql = "SELECT * FROM task_result";
    final Statement stmt = c.createStatement();
    final ResultSet rs = stmt.executeQuery(sql);
    int count = 1;
    output("\n***** displaying the DB table content *****");
    while (rs.next()) {
      final StringBuilder sb = new StringBuilder();
      sb.append("row ").append(count).append(": ");
      sb.append("id=").append(rs.getObject("id"));
      sb.append(", task_id=").append(rs.getObject("task_id"));
      sb.append(", message=").append(rs.getObject("message"));
      output(sb.toString());
      count++;
    }
    output("***** end of DB table content *****");
    rs.close();
    stmt.close();
    c.close();
  }

  /**
   * Print a message to the console and/or log file.
   * @param message the message to print.
   */
  private static void output(final String message) {
    System.out.println(message);
    log.info(message);
  }
}
