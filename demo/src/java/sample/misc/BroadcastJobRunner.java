/*
 * JPPF.
 * Copyright (C) 2005-2015 JPPF Team.
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

import java.util.List;

import org.jppf.client.*;
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
    TypedProperties config = JPPFConfiguration.getProperties();
    boolean remoteEnabled = true;
    config.setBoolean("jppf.remote.execution.enabled", remoteEnabled);
    try (JPPFClient client = new JPPFClient()) {
      client.setLocalExecutionEnabled(true);
      if (remoteEnabled) client.awaitActiveConnectionPool();
      JPPFJob job = new JPPFJob();
      job.setName("my job");
      job.getSLA().setBroadcastJob(true);
      job.getClientSLA().setJobExpirationSchedule(new JPPFSchedule(5000L));
      job.add(new LongTask(10L)).setId("my task");
      List<Task<?>> results = client.submitJob(job);
      System.out.println("job complete");
      for (Task<?> task : results) {
        if (task.getThrowable() != null) System.out.println("got exception: " + ExceptionUtils.getStackTrace(task.getThrowable()));
        else System.out.println("got result: " + task.getResult());
      }
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  /**
   * 
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
