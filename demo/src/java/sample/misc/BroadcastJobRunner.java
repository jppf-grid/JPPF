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

package sample.misc;

import static org.jppf.utils.configuration.JPPFProperties.*;

import java.util.List;

import org.jppf.client.*;
import org.jppf.client.concurrent.*;
import org.jppf.node.protocol.*;
import org.jppf.scheduling.JPPFSchedule;
import org.jppf.utils.*;

/**
 * 
 * @author Laurent Cohen
 */
public class BroadcastJobRunner {
  /**
   *
   * @param args not used.
   */
  public static void main(final String[] args) {
    try {
      //submitJob();
      submitJobWithExecutor();
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  /**
   * Submit a broadcast job.
   * @throws Exception if any error occurs.
   */
  private static void submitJob() throws Exception {
    boolean remoteEnabled = true;
    JPPFConfiguration.set(DISCOVERY_ENABLED, false)
      .set(DRIVERS, "driver1")
      .setString("driver1.jppf.server.host", "localhost")
      .setInt("driver1.jppf.server.port", 11111)
      .set(REMOTE_EXECUTION_ENABLED, remoteEnabled)
      .set(LOCAL_EXECUTION_ENABLED, true)
      .set(LOAD_BALANCING_ALGORITHM, "manual")
      .set(LOAD_BALANCING_PROFILE, "manual")
      .setInt(LOAD_BALANCING_PROFILE.getName() + ".manual.size", 10);
    try (JPPFClient client = new JPPFClient()) {
      JPPFJob job = new JPPFJob();
      job.setName("my job");
      job.getSLA().setBroadcastJob(true);
      job.getClientSLA().setJobExpirationSchedule(new JPPFSchedule(5000L));
      job.getClientSLA().setMaxChannels(2);
      for (int i=0; i<1; i++) job.add(new LongTask(10L)).setId("my task" + (i + 1));
      List<Task<?>> results = client.submitJob(job);
      System.out.println("job complete");
      for (Task<?> task : results) {
        if (task.getThrowable() != null) System.out.printf("got exception for task '%' : %s%n", task.getId(), ExceptionUtils.getStackTrace(task.getThrowable()));
        else System.out.printf("got result for task '%s': %s%n", task.getId(), task.getResult());
      }
    }
  }

  /**
   * Submit a broadcast job with a {@link JPPFExecutorService}.
   * @throws Exception if any error occurs.
   */
  private static void submitJobWithExecutor() throws Exception {
    boolean remoteEnabled = true;
    JPPFConfiguration.set(DISCOVERY_ENABLED, false)
      .setString("jppf.drivers", "driver1")
      .setString("driver1.jppf.server.host", "localhost")
      .setInt("driver1.jppf.server.port", 11111)
      .set(REMOTE_EXECUTION_ENABLED, remoteEnabled)
      .set(LOCAL_EXECUTION_ENABLED, true)
      .set(LOAD_BALANCING_ALGORITHM, "manual")
      .set(LOAD_BALANCING_PROFILE, "manual")
      .setInt(LOAD_BALANCING_PROFILE.getName() + ".manual.size", 10);
    System.out.println("starting client");
    JPPFClient client = new JPPFClient();
    System.out.println("creating executor");
    int nbTasks = 10;
    JPPFExecutorService executor = new JPPFExecutorService(client);
    executor.setBatchSize(nbTasks);
    JPPFCompletionService completionService = new JPPFCompletionService(executor);
    ExecutorServiceConfiguration cfg = executor.getConfiguration();
    JobConfiguration jobConfig = cfg.getJobConfiguration();
    jobConfig.getClientSLA().setMaxChannels(2);
    jobConfig.getSLA().setBroadcastJob(true);
    System.out.println("submitting tasks");
    for (int i=0; i<nbTasks; i++) {
      Task<?> task = new LongTask(10L);
      task.setId("my task " + (i + 1));
      completionService.submit(task, null);
    }
    System.out.println("getting results");
    int count = 0;
    while (count < nbTasks) {
      JPPFTaskFuture<?> future = (JPPFTaskFuture<?>) completionService.take();
      Object o = future.get();
      Task<?> task = future.getTask();
      System.out.printf("got result for task '%s': %s%n", task.getId(), o);
      count++;
    }
    System.out.println("executor shutdown");
    //executor.shutdown();
    executor.shutdownNow();
    System.out.println("closing client");
    client.close();
    System.out.println("done");
  }

  /**
   * A simple task that sleeps for a specified time.
   */
  public static class LongTask extends AbstractTask<String> {
    /**
     * Determines how long this task will run.
     */
    private final long duration;

    /**
     * Initialize this task with a predefined length of time, in milliseconds, during which it will run.
     * @param duration determines how long this task will run.
     */
    public LongTask(final long duration) {
      this.duration = duration;
    }

    @Override
    public void run() {
      long start = System.nanoTime();
      try {
        if (duration > 0) Thread.sleep(duration);
        long elapsed = (System.nanoTime() - start) / 1_000_000L;
        String result = "task '" + getId() + "' has run for " + elapsed + " ms";
        setResult(result);
        System.out.println(result);
      } catch(Exception e) {
        setThrowable(e);
      }
    }
  }
}
