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
package test.jmx.canceljob;

import java.util.*;

import org.jppf.client.*;
import org.jppf.node.protocol.*;

/**
 * Test class for job cancellation.
 */
public class CancelJobRunner {
  /**
   * Entry point for this class, submits the tasks with a set duration to the server.
   * @param args not used.
   */
  public static void main(final String... args) {
    long taskDuration = 10_000L, sleepTime = 1_000L;
    int nbJobs = 5, nbTasks = 5, nbConnections = 1;
    try (JPPFClient client = new JPPFClient()) {
      JPPFConnectionPool pool = client.awaitWorkingConnectionPool();
      if (pool.getSize() != nbConnections) {
        System.out.printf("changing connection pool size from %d to %d%n", pool.getSize(), nbConnections);
        pool.setSize(nbConnections);
        pool.awaitWorkingConnections(Operator.EQUAL, nbConnections);
        System.out.printf("got %d connections%n", pool.getSize());
      }
      List<JPPFJob> jobs = new ArrayList<>();
      for (int i=1; i<=nbJobs; i++) {
        JPPFJob job = new JPPFJob();
        job.setName("Cancel-" + i);
        job.setBlocking(false);
        for (int j=1; j<=nbTasks; j++) job.add(new MyTask(taskDuration)).setId(job.getName() + ":task-" + j);
        jobs.add(job);
      }
      System.out.println("submitting " + nbJobs + " jobs");
      for (JPPFJob job: jobs) client.submitJob(job);
      Thread.sleep(sleepTime);
      System.out.println("cancelling jobs");
      for (JPPFJob job: jobs) job.cancel();
      System.out.println("jobs cancel request submitted, waiting for results");
      for (JPPFJob job: jobs) {
        List<Task<?>> results = job.awaitResults();
        System.out.printf("- got %s results for job '%s'%n", (results == null ? "null" : "" + results.size()), job.getName());
      }
      System.out.println("got all results");
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  /**
   * A simple task that sleeps for a specified duration.
   */
  public static class MyTask extends AbstractTask<String> {
    /**
     * How long this task will sleep.
     */
    private final long duration;

    /**
     * @param duration how long this task will sleep.
     */
    public MyTask(final long duration) {
      this.duration = duration;
    }

    @Override
    public void run() {
      try {
        Thread.sleep(duration);
        setResult("success");
      } catch (Exception e) {
        setThrowable(e);
      }
    }
  }
}
