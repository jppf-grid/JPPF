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

package org.jppf.example.aparapi;

import java.util.List;

import org.jppf.client.JPPFClient;
import org.jppf.client.JPPFJob;
import org.jppf.node.protocol.Task;
import org.jppf.utils.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 
 * @author Laurent Cohen
 */
public class AparapiRunner
{
  /**
   * Logger for this class.
   */
  private static Logger log = LoggerFactory.getLogger(AparapiRunner.class);
  /**
   * The JPPF client singleton.
   */
  private static JPPFClient client = null;

  /**
   * Entry poit for this applications.
   * @param args command line arguments are not used.
   * @throws Throwable if any error occurs.
   */
  public static void main(final String[] args) throws Throwable {
    try {
      print("creating client");
      client = new JPPFClient();
      perform();
    } catch (Exception e) {
      e.printStackTrace();
    } finally {
      client.close();
    }
  }

  /**
   * Submit matrix multiplication jobs to the grid, for execution on a GPU on the nodes' machines.
   * @throws Throwable if any error occurs.
   */
  public static void perform() throws Throwable {
    TypedProperties config = JPPFConfiguration.getProperties();
    int iterations = config.getInt("iterations", 10);
    int tasksPerJob = config.getInt("tasksPerJob", 1);
    int matrixSize = config.getInt("matrixSize", 1500);
    String execMode = config.getString("execMode", "GPU");
    if (!"GPU".equalsIgnoreCase(execMode) && !"JTP".equalsIgnoreCase(execMode)) execMode = "GPU";
    print("starting GPU test with " + iterations + " jobs, " + tasksPerJob + " tasks per job and a matrix size of " + matrixSize + ", execution mode: " + execMode);

    // initial values for execution timing stats
    long totalIterationTime = 0L;
    long min = Long.MAX_VALUE;
    long max = 0L;

    // one job per iteration
    for (int n=0; n<iterations; n++) {
      SquareMatrix matrixA = new SquareMatrix(matrixSize);
      matrixA.assignRandomValues();
      SquareMatrix matrixB = new SquareMatrix(matrixSize);
      matrixB.assignRandomValues();
      long start = System.nanoTime();
      JPPFJob job = new JPPFJob();
      job.setName("gpu_job_" + n);
      for (int i=0; i<tasksPerJob; i++) job.add(new AparapiTask(matrixA, matrixB, execMode));
      // submit and get the results
      List<Task<?>> results = client.submitJob(job);
      for (Task<?> task: results) {
        if (task.getThrowable() != null) throw task.getThrowable();
        AparapiTask t = (AparapiTask) task;
        assert t.getResult() instanceof SquareMatrix;
        //print("result for " + task.getId() + ": " + task.getResult());
      }
      long elapsed = (System.nanoTime() - start) / 1000000;
      if (elapsed < min) min = elapsed;
      if (elapsed > max) max = elapsed;
      totalIterationTime += elapsed;
      print("Iteration #" + (n+1) + " performed in " + StringUtils.toStringDuration(elapsed));
    }
    print("total time: " + StringUtils.toStringDuration(totalIterationTime) +
        ", average time: " + StringUtils.toStringDuration(totalIterationTime / iterations) +
        ", min = " + StringUtils.toStringDuration(min) + ", max = " + StringUtils.toStringDuration(max));
  }

  /**
   * Print a message to the log and to the console.
   * @param msg the message to print.
   */
  private static void print(final String msg)
  {
    System.out.println(msg);
    log.info(msg);
  }
}
