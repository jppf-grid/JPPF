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

package org.jppf.example.android.demo;

import java.io.File;
import java.util.List;

import org.jppf.client.*;
import org.jppf.location.*;
import org.jppf.node.protocol.*;
import org.jppf.scheduling.JPPFSchedule;
import org.jppf.utils.*;

/**
 * This class is a simple JPPF client application that submits a job for execution on an Android node.
 * @author Laurent Cohen
 */
public class Runner {
  /**
   * Create a JPPF client and submit a job with Java tasks.
   * @param args not used.
   */
  public static void main(final String[] args) {
    // extract the parameters of the demo from the JPPF configuration
    final TypedProperties config = JPPFConfiguration.getProperties();
    final int nbTasks = config.getInt("demo.nbTasks", 1);
    final long duration = config.getLong("demo.taskDuration", 2000L);
    System.out.printf("Android demo parameters: nb tasks=%,d; task duration=%,d ms�n", nbTasks, duration);
    // create and start the JPPF client
    try (final JPPFClient client = new JPPFClient()) {
      // create the JPPF job
      System.out.println("creating job");
      final JPPFJob job = new JPPFJob();
      job.setName("JPPF Android Demo");
      for (int i=1; i<=nbTasks; i++) {
        job.add(new DemoAndroidTask(duration)).setId("#" + i);
      }
      // add the dexed jar to the job's classpath
      System.out.println("setting classpath");
      addToJobClassPath(job, new File("dex-demo.jar"));
      // set a dispatch timeout of 5 mn to avoid the job being stuck, should the node fail
      job.getSLA().setDispatchExpirationSchedule(new JPPFSchedule(5L * 60_000L));
      // submit the job and get the resutls
      System.out.println("submitting job");
      final List<Task<?>> results = client.submitJob(job);
      // process the job's results
      for (final Task<?> task : results) {
        if (task.getThrowable() != null) System.out.printf("task %s raised an exception : %s%n", task.getId(), ExceptionUtils.getStackTrace(task.getThrowable()));
        else System.out.printf("Task %s has a result: %s%n", task.getId(), task.getResult());
      }
    } catch (final Exception e) {
      e.printStackTrace();
    }
  }

  /**
   * Add the specified file (jar or apk) to the specified job).
   * @param job the JPPF job whose classpath is to be updated.
   * @param file a dexed jar or Android apk file path. 
   * @throws Exception if any error occurs.
   */
  public static void addToJobClassPath(final JPPFJob job, final File file) throws Exception {
    // copy the file in memory
    final Location<String> fileLoc = new FileLocation(file);
    final Location<byte[]> memoryLoc = fileLoc.copyTo(new MemoryLocation(file.length()));
    // add the memory location to the classpath
    final ClassPath classpath = job.getSLA().getClassPath();
    classpath.add(file.getName(), memoryLoc);
  }
}
