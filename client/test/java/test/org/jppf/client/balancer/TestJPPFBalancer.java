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

package test.org.jppf.client.balancer;

import org.jppf.client.JPPFClient;
import org.jppf.client.JPPFJob;
import org.jppf.server.protocol.JPPFTask;

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
      System.out.println("Connecting...");
      client = new JPPFClient(UUID.randomUUID().toString());
      System.out.println("Connecting...DONE.");

      JPPFJob job = new JPPFJob();
//      job.addTask(new TestTask("Task 1"));
//      job.setBlocking(false);
//
//      System.out.println("Submitting job...");
//      client.submit(job);
//      System.out.println("Submitting job...DONE");
//
//      job = new JPPFJob();
      job.addTask(new TestTask("Task 2"));
      job.getSLA().setBroadcastJob(true);
      job.setBlocking(true);

      System.out.println("Submitting job...");
      client.submit(job);
      System.out.println("Submitting job...DONE");
    } catch (Throwable t) {
      t.printStackTrace(System.out);
    }
    finally
    {
      System.out.println("Closing...");
      if(client != null) client.close();
      System.out.println("Closing...DONE");
    }
    System.exit(0);
  }

  /**
   * A simple JPPF task for testing.
   * @author Martin JANDA
   */
  public static class TestTask extends JPPFTask
  {
    /**
     * The text describing this task.
     */
    private final String text;

    /**
     * Initializes task with describing text.
     * @param text describing this task.
     */
    public TestTask(final String text)
    {
      this.text = text;
    }

    @Override
    public void run()
    {
      System.out.printf("Test task: '%s'%n", text);
      try
      {
        Thread.sleep(2000L);
      }
      catch (InterruptedException e)
      {
        e.printStackTrace();
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
