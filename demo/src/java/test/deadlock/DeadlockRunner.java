/*
 * JPPF.
 * Copyright (C) 2005-2013 JPPF Team.
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
package test.deadlock;

import java.util.List;

import org.jppf.client.*;
import org.jppf.client.event.*;

/**
 * This is a template JPPF application runner.
 * It is fully commented and is designed to be used as a starting point
 * to write an application using JPPF.
 * @author Laurent Cohen
 */
public class DeadlockRunner {
  /**
   * The JPPF client, handles all communications with the server.
   * It is recommended to only use one JPPF client per JVM, so it
   * should generally be created and used as a singleton.
   */
  private static JPPFClient jppfClient =  null;

  /**
   * The entry point for this application runner to be run from a Java command line.
   * @param args by default, we do not use the command line arguments,
   * however nothing prevents us from using them if need be.
   * @throws Exception if any error occurs
   */
  public static void main(final String[] args) throws Exception {
    try {
      jppfClient = new JPPFClient();
      while (!jppfClient.hasAvailableConnection()) Thread.sleep(1L);
      List<JPPFClientConnection> list = jppfClient.getAllConnections();
      System.out.println("client uuid=" + jppfClient.getUuid());
      for (JPPFClientConnection c: list) {
        System.out.println("connection name=" + c.getName() + ", uuid=" + c.getDriverUuid() + ", connectionUuid=" + c.getConnectionUuid());
      }
      JPPFJob job = createJob();
      for (int i = 0; i < 100*1000; i++) {
        job.addTask(new SampleJPPFTask(i));
      }
      job.setBlocking(false);
      //job.getSLA().setCancelUponClientDisconnect(false);
      job.addJobListener(new JobListener() {
        @Override public void jobStarted(final JobEvent event) {
        }
        @Override public void jobReturned(final JobEvent event) {
          System.out.println("job '" + event.getJob().getName() + "' : " + event.getTasks().size() + " tasks returned");
        }
        @Override public void jobEnded(final JobEvent event) {
          System.out.println("job '" + event.getJob().getName() + "' ended");
          System.exit(0);
        }
        @Override public void jobDispatched(final JobEvent event) {
          System.out.println("job '" + event.getJob().getName() + "' : " + event.getTasks().size() + " tasks dispatched to channel " + event.getChannel());
        }
      });

      /*
      */
      job.setResultListener(new TaskResultListener() {
        @Override
        public void resultsReceived(final TaskResultEvent event) {
          //throw new RuntimeException("Should cause reconnect.");
        }
      });
      jppfClient.submit(job);
      Thread.sleep(3000L);
      System.out.println("requesting job cancel");
      jppfClient.cancelJob(job.getUuid());
      //cancelJobWithJMX(job.getUuid());
      while (true) {
        Thread.sleep(1);
      }
    } finally {
      if (jppfClient != null) {
        jppfClient.close();
      }
    }
  }
  /**
   * Create a JPPF job that can be submitted for execution.
   * @return an instance of the {@link org.jppf.client.JPPFJob JPPFJob} class.
   * @throws Exception if an error occurs while creating the job or adding tasks.
   */
  public static JPPFJob createJob() throws Exception {
    JPPFJob job = new JPPFJob();
    job.setName("Template Job Id");
    return job;
  }

  /**
   * Cancel the specified job.
   * @param uuid uuid of the job to cancel.
   * @throws Exception if any error occurs.
   */
  private static void cancelJobWithJMX(final String uuid) throws Exception {
    JPPFClientConnection c = jppfClient.getClientConnection();
    c.getJmxConnection().cancelJob(uuid);
  }
}
