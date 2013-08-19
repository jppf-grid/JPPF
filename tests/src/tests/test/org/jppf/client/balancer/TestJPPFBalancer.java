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

package test.org.jppf.client.balancer;

import org.jppf.client.JPPFClient;
import org.jppf.client.JPPFJob;
import org.jppf.client.JPPFResultCollector;
import org.jppf.client.event.*;
import org.jppf.client.submission.SubmissionStatus;
import org.jppf.server.protocol.JPPFTask;
import org.jppf.utils.JPPFConfiguration;
import org.jppf.utils.TypedProperties;

import java.util.Date;
import java.util.UUID;

/**
 * Tester for client balancer.
 * @author Martin JANDA
 */
public class TestJPPFBalancer
{
  /**
   * Test of the client balancer.
   * @param args not used.
   */
  public static void main(final String[] args)
  {
    JPPFClient client = null;
    try
    {
      configure();
      System.out.println("Connecting...");
      client = new JPPFClient(UUID.randomUUID().toString());
      Thread.sleep(3000L);
      System.out.println("Connecting...DONE.");

      JPPFJob job = new JPPFJob();
      job.addTask(new TestTask("Task BROADCAST", false));
      job.setBlocking(true);
      job.getSLA().setBroadcastJob(true);

      job = new JPPFJob();
      job.setBlocking(true);
      job.addJobListener(new JobListenerAdapter()
      {
        @Override
        public void jobStarted(final JobEvent event)
        {
          System.out.println("jobStarted: " + event.getJob());
        }

        @Override
        public void jobEnded(final JobEvent event)
        {
          System.out.println("jobEnded: " + event.getJob());
        }
      });
      for(int index = 1; index <= 24; index++) {
        job.addTask(new TestTask(String.format("Task %d", index), index == 9));
      }
//      job.getSLA().setBroadcastJob(true);
      JPPFResultCollector collector = new JPPFResultCollector(job) {
        @Override
        public synchronized void setStatus(final SubmissionStatus newStatus) {
          System.out.println("setStatus: " + newStatus);
          super.setStatus(newStatus);
        }

        @Override
        public synchronized void resultsReceived(final TaskResultEvent event) {
          System.out.println("resultsReceived: " + event.getTaskList().size());
          if(event.getThrowable() != null)
            event.getThrowable().printStackTrace(System.out);
          else {
            for (JPPFTask task : event.getTaskList()) {
              System.out.println("Ex: " + task.getException() + "\t result: " + task.getResult());
            }
          }
          super.resultsReceived(event);
        }
      };
      collector.addSubmissionStatusListener(new SubmissionStatusListener() {
        @Override
        public void submissionStatusChanged(final SubmissionStatusEvent event) {
          System.out.println("submissionStatusChanged: " + event.getStatus());
        }
      });
      System.out.println("Submission status: " + collector.getStatus());
      job.setResultListener(collector);

      System.out.println("Submitting job...");
      long dur = System.nanoTime();
      client.submit(job);
      dur = System.nanoTime() - dur;
      System.out.println("Submitting job...DONE in " + (dur / 1000000.0));

    } catch (Throwable t) {
      t.printStackTrace(System.out);
    } finally {
      System.out.println("Closing...");
      if (client != null) client.close();
      System.out.println("Closing...DONE");
    }
    System.exit(0);
  }

  /**
   * Set the JPPF configuration properties for this test.
   */
  private static void configure()
  {
    String poolSize = "1";

    TypedProperties properties = JPPFConfiguration.getProperties();
    properties.setProperty("jppf.balancer.old.enabled", "false");
    properties.setProperty("jppf.load.balancing.algorithm", "manual");
    properties.setProperty("jppf.load.balancing.profile", "manual");
    properties.setProperty("jppf.load.balancing.profile.manual.size", "1");
    properties.setProperty("jppf.local.execution.enabled", "true");
    properties.setProperty("driver1.jppf.server.host", "localhost");
    properties.setProperty("driver1.jppf.pool.size", poolSize);
    properties.setProperty("driver1.class.server.port", "11111");
    properties.setProperty("driver1.app.server.port", "11112");
    properties.setProperty("driver1.priority" , "10");
    properties.setProperty("driver2.jppf.server.host", "hs2.crcdata.cz");
    properties.setProperty("driver2.jppf.pool.size", poolSize);
    properties.setProperty("driver2.priority" , "10");
    properties.setProperty("driver2.app.server.port", "11112");
    properties.setProperty("driver2.class.server.port", "11111");
    properties.setProperty("jppf.remote.execution.enabled", "false");

//    if(paramJPPF.isAutoDiscovery() || (sb.length() == 0 && !paramJPPF.isLocalExecution()))
//    properties.setProperty("jppf.discovery.enabled", "true");
//    else {
    properties.setProperty("jppf.discovery.enabled", "false");
//      properties.setProperty("jppf.drivers", "driver1");
    properties.setProperty("jppf.drivers", "driver1 driver2");
//    }

    properties.setProperty("jppf.pool.size", poolSize);
  }

  /**
   * A simple JPPF task for testing.
   * @author Martin JANDA
   */
  public static class TestTask extends JPPFTask
  {
    /**
     * Explicit serialVersionUID.
     */
    private static final long serialVersionUID = 1L;
    /**
     * The text describing this task.
     */
    private final String text;
    /**
     * Indicates whether exception should be fired during task execution.
     */
    private final boolean exception;

    /**
     * Initializes task with describing text.
     * @param text describing this task.
     * @param exception indicates whether exception should be fired.
     */
    public TestTask(final String text, final boolean exception)
    {
      this.text = text;
      this.exception = exception;
    }

    @Override
    public void run()
    {
      System.out.printf("Test task: '%s'%n", text);
      if(exception)
      {
        throw new NullPointerException("Task exception: " + text);
      }
      try
      {
        Thread.sleep(20000L);
      }
      catch (InterruptedException e)
      {
        System.out.println("Interrupted: " + text + " - " + new Date());
        e.printStackTrace(System.out);
        setThrowable(e);
      } finally {
        System.out.println("Task finished: " + text);
      }
    }

    @Override
    public String toString()
    {
      final StringBuilder sb = new StringBuilder();
      sb.append("TestTask");
      sb.append("{text='").append(text).append('\'');
      sb.append('}');
      return sb.toString();
    }
  }
}
