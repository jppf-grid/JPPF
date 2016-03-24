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

package org.jppf.example.android.demo;

import java.io.File;
import java.util.List;

import org.jppf.client.*;
import org.jppf.location.*;
import org.jppf.node.protocol.*;
import org.jppf.utils.ExceptionUtils;

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
    // create and start the JPPF client
    try (JPPFClient client = new JPPFClient()) {
      // create the JPPF job
      System.out.println("creating job");
      JPPFJob job = new JPPFJob();
      job.setName("JPPF Android Demo");
      job.add(new DemoAndroidTask());
      // add the dexed jar to the job's classpath
      System.out.println("setting classpath");
      addToJobClassPath(job, new File("dex-demo.jar"));
      // submit the job and get the resutls
      System.out.println("submitting job");
      List<Task<?>> results = client.submitJob(job);
      // process the job's results
      for (Task<?> task : results) {
        if (task.getThrowable() != null) System.out.println("got exception: " + ExceptionUtils.getStackTrace(task.getThrowable()));
        else System.out.println("got result: " + task.getResult());
      }
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  /**
   * Add the specified file (jar or apk) to the specified job).
   * @param job the JPPF job whose classpath is to be updated.
   * @param file a dexed jar or Android apk file path. 
   * @throws Exception if any error occurs.
   */
  @SuppressWarnings("unchecked")
  public static void addToJobClassPath(final JPPFJob job, final File file) throws Exception {
    // copy the file in memory
    Location fileLoc = new FileLocation(file);
    Location memoryLoc = fileLoc.copyTo(new MemoryLocation(file.length()));
    // add the memory location to the classpath
    ClassPath classpath = job.getSLA().getClassPath();
    classpath.add(file.getName(), memoryLoc);
  }
}
