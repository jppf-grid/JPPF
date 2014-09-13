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

/**
 * Copyright 2013, Somete Group, LLC. All rights reserved. $LastChangedDate$
 * $LastChangedBy$ $Revision$
 */
package sample.test.deadlock;

import java.util.*;
import java.util.concurrent.CountDownLatch;

import javax.management.*;

import org.jppf.client.*;
import org.jppf.job.*;
import org.jppf.management.*;
import org.jppf.management.NodeSelector.AllNodesSelector;
import org.jppf.server.job.management.DriverJobManagementMBean;
import org.jppf.utils.stats.*;
import org.slf4j.*;

/**
 * 
 */
public final class ServerMonitorClient {

  /**
   * Logger for this class.
   */
  private static Logger LOG = LoggerFactory.getLogger(ServerMonitorClient.class);

  /**
   * The JPPF client, handles all communications with the server. It is
   * recommended to only use one JPPF client per JVM, so it should generally be
   * created and used as a singleton. This constructor call causes JPPF to read
   * its configuration file and connect with one or multiple JPPF drivers
   * (pool.size in jppf.config). Connection is not immediate.
   */
  private static final JPPFClient jppfClient = new JPPFClient();

  /**
   * JMX Driver Connection Wrapper for getting information from driver's MBean
   */
  private static JMXDriverConnectionWrapper jmx;

  /**
   * Storing job Names for server cleanup
   */
  private static Set<String> jobNames = new HashSet<>();

  /**
   * Keeps this monitor going until all nodes are shutdown
   */
  private static CountDownLatch doneSignal;

  /**
   * Tracks information on cloud servers
   */
  //private static ServerManager manager;
  /** */
  public int nbDispatch = 0;
  /** */
  public int nbReturn = 0;
  /** */
  public int nbQueued = 0;
  /** */
  public int nbEnded = 0;
  /** */
  public int nbUpdated = 0;
  

  /**
   * Prevent outside instantiation
   */
  private ServerMonitorClient() {
  }

  /**
   * Entry point to the server monitor.
   *
   * @param args
   *        Currently unused. TODO config file with server control parameters
   */
  public static void main(final String... args) {
    // Keep signal around until ready to shut down client
    doneSignal = new CountDownLatch(1);
    // Read config to manage cloud servers
    //manager = new ServerManager();
    // Instantiate and run
    ServerMonitorClient client = new ServerMonitorClient();
    client.monitor();

    // Done with setup. Awaiting shutdown signal
    try {
      doneSignal.await();
      System.out.printf("queued=%d, dispatched=%d, returns=%d, ended=%d, updated=%d\n", client.nbQueued, client.nbDispatch, client.nbReturn, client.nbEnded, client.nbUpdated);
      // Clean up any remaining servers from previous runs or if the individual
      // deletion call has timed out and exited without successful deletion
      //manager.shutdownNodes(jobNames);
      if (LOG.isInfoEnabled()) LOG.info("Server cleanup complete.  Shutting down monitoring client.");
    } catch (InterruptedException e) {
      e.printStackTrace();
    } finally {
      if (jppfClient != null)
        jppfClient.close();
    }
  }

  /**
   *
   */
  private void monitor() {

    while (!jppfClient.hasAvailableConnection()) {
      try {
        Thread.sleep(10L);
      } catch (InterruptedException e) {
        e.printStackTrace();
      }
    }
    JPPFClientConnection connection = jppfClient.getClientConnection();
    JPPFConnectionPool pool;

    // get a JMX connection pool
    while ((pool = connection.getConnectionPool()) == null) {
      try {
        Thread.sleep(10L);
      } catch (InterruptedException e) {
        e.printStackTrace();
      }
    }

    // wait until a JMX connection is created
    while ((jmx = pool.getJmxConnection()) == null) {
      try {
        Thread.sleep(10L);
      } catch (InterruptedException e) {
        e.printStackTrace();
      }
    }

    // wait until the JMX connection is established
    while (!jmx.isConnected()) {
      try {
        Thread.sleep(10L);
      } catch (InterruptedException e) {
        e.printStackTrace();
      }
    }

    // Register listeners
    NotificationListener myJobNotificationListener = new MyJobNotificationListener();
    // wait until a standard connection to the server is established
    try {
      DriverJobManagementMBean proxy = jmx.getProxy(DriverJobManagementMBean.MBEAN_NAME, DriverJobManagementMBean.class);
      // subscribe to all notifications from the MBean
      proxy.addNotificationListener(myJobNotificationListener, null, null);
    } catch (Exception e) {
      e.printStackTrace();
    }
    LOG.info("Job Notification Listener registered.");
    try {
      JPPFStatistics stats = (JPPFStatistics) jmx.invoke(JPPFDriverAdminMBean.MBEAN_NAME, "statistics");
      stats.reset();
      LOG.info("Driver statistics reset.");
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  /**
   * this class prints a message each time a job is added to the server's queue
   */
  public class MyJobNotificationListener implements NotificationListener {

    /**
     *
     */
    private Map<String, Long> startTime = new HashMap<>();

    //
    /**
     * Handle an MBean notification
     * @param notification .
     * @param handback .
     */
    @Override
    public void handleNotification(final Notification notification, final Object handback) {
      try {
        JobNotification jobNotif = (JobNotification) notification;
        String jobId = jobNotif.getJobInformation().getJobName();
        String jobUuid = jobNotif.getJobInformation().getJobUuid();
        JobEventType eventType = jobNotif.getEventType();
        int queueSize = (int) queueSize();
        switch (eventType) {
          case JOB_ENDED:
            nbEnded++;
            if (LOG.isInfoEnabled()) LOG.info(String.format("Job %s ended. Elapsed time %.1f minutes.", jobId, (jobNotif.getTimeStamp() - startTime.get(jobUuid)) / 60000f));
            // If empty queue, check if any jobs still executing
            if (queueSize <= 0) {
              try {
                // Check state of all nodes
                Map<String, Object> nodeState = jmx.getNodeForwarder().state(new AllNodesSelector());
                // Check if any nodes still executing
                boolean executing = false;
                for (Map.Entry<String, Object> entry : nodeState.entrySet()) {
                  if (!(entry.getValue() instanceof Exception)) {
                    JPPFNodeState state = (JPPFNodeState) entry.getValue();
                    if (state.getExecutionStatus() == JPPFNodeState.ExecutionState.EXECUTING) {
                      executing = true;
                      break;
                    }
                  }
                }
                if (!executing) doneSignal.countDown();
              } catch (Exception e) {
                e.printStackTrace();
              }
            }
            break;
          case JOB_DISPATCHED:
            nbDispatch++;
            if (!startTime.containsKey(jobUuid)) {
              startTime.put(jobUuid, jobNotif.getTimeStamp());
              if (LOG.isInfoEnabled()) LOG.info(String.format("Job %s started", jobId));
            }
            if (LOG.isInfoEnabled()) {
              LOG.info(String.format("Job %s dispatched %d tasks to %s. Queue now has %d tasks.", jobId, jobNotif.getJobInformation().getTaskCount(), getHost(jobNotif), (long) queueSize()));
              LOG.info(printQueueInfo());
            }
            break;
          case JOB_RETURNED:
            nbReturn++;
            if (LOG.isInfoEnabled()) LOG.info(String.format("Job %s returned %d tasks from %s.", jobId, jobNotif.getJobInformation().getTaskCount(), getHost(jobNotif)));
            break;
          case JOB_QUEUED:
            nbQueued++;
            // Initialize per-job Queue length. Save the job name for later server deletion
            jobNames.add(jobNotif.getJobInformation().getJobName());
            if (LOG.isInfoEnabled()) LOG.info(String.format("Job %s queued. Queue now has %d tasks.", jobId, queueSize));
            break;
          case JOB_UPDATED:
            nbUpdated++;
            //manager.addServers(getQueueInfo());
          default:
        }
      } catch(Exception e) {
        e.printStackTrace();
      }
    }

    /**
     *
     * @return .
     */
    private String printQueueInfo() {
      StringBuilder q = new StringBuilder("Tasks in queue per job:");
      Map<String, Integer> queueInfo = getQueueInfo();
      for (Map.Entry<String, Integer> entry : queueInfo.entrySet()) q.append(" ").append(entry.getKey()).append("=").append(queueInfo.get(entry.getKey()));
      return q.toString();
    }

    /**
     *
     * @return .
     */
    private synchronized Map<String, Integer> getQueueInfo() {
      Map<String, Integer> queueInfo = new HashMap<>();
      try {
        DriverJobManagementMBean proxy = jmx.getJobManager();
        String[] allIds = proxy.getAllJobIds();
        LOG.info("allIds={}", Arrays.asList(allIds));
        for (String jobUUID : allIds) {
          JobInformation jobInfo = proxy.getJobInformation(jobUUID);
          if (jobInfo != null) {
            String jobName = jobInfo.getJobName();
            queueInfo.put(jobName, proxy.getJobInformation(jobUUID).getTaskCount());
          } else LOG.error("jobInfo is null for jobUUID={}", jobUUID);
        }
      } catch (Exception e) {
        e.printStackTrace();
      }
      return queueInfo;
    }

    /**
     *
     * @return .
     */
    private double queueSize() {
      try {
        JPPFStatistics stats = (JPPFStatistics) jmx.invoke(JPPFDriverAdminMBean.MBEAN_NAME, "statistics");
        return stats.getSnapshot(JPPFStatisticsHelper.TASK_QUEUE_COUNT).getLatest();
      } catch (Exception e) {
        e.printStackTrace();
      }
      return Double.NaN;
    }

    /**
     *
     * @param jobNotif .
     * @return .
     */
    private String getHost(final JobNotification jobNotif) {
      String host = jobNotif.getNodeInfo().getHost();
      if (host == null) {
        host = "local node";
      }
      return host;
    }
  };

}
