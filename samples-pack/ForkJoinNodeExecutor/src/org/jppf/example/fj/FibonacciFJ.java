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

package org.jppf.example.fj;

import java.util.concurrent.ForkJoinTask;

import org.jppf.client.JPPFClient;
import org.jppf.client.JPPFJob;
import org.jppf.node.protocol.Task;
import org.jppf.server.protocol.JPPFTask;
import org.jppf.utils.JPPFConfiguration;
import org.jppf.utils.TypedProperties;

/**
 * Sample class for fork join support demonstration.
 * @author Martin JANDA
 */
public class FibonacciFJ {
  /**
   * Number of tasks to execute.
   */
  public static int COUNT = 10;
  /**
   * Order of Fibonacci number to compute.
   */
  public static int N     = 10;

  /**
   * Main method for demonstration of fork join support
   * @param args not used.
   */
  public static void main(final String[] args) {
    JPPFClient client = null;
    try {
      client = new JPPFClient();
      JPPFJob job = new JPPFJob();
      job.setBlocking(true);

      TypedProperties config = JPPFConfiguration.getProperties();
      COUNT = config.getInt("fib.fj.nbTasks", 10);
      if (COUNT < 1) COUNT = 1;
      N = config.getInt("fib.fj.N", 10);
      if (N < 1) N = 1;
      System.out.printf("Creating %d tasks: fib(%d)%n", COUNT, N);
      for (int index = 0; index < COUNT; index++) job.add(new JPPFTaskFibonacci(N));

      System.out.println("Submitting job...");
      long dur = System.nanoTime();
      client.submitJob(job);
      dur = System.nanoTime() - dur;
      System.out.printf("Job done in %.3f ms%n", dur / 1000000.0);

      for (Task<?> task : job.getResults().getAllResults()) {
        if (task.getResult() instanceof FibonacciResult) {
          FibonacciResult result = (FibonacciResult) task.getResult();
          System.out.printf("  %2d. ForkJoin: %s, Result: %d%n", task.getPosition(), result.isForkJoinUsed(), result.getResult());
        }
      }
    } catch (Exception e) {
      e.printStackTrace();
    } finally {
      if (client != null) client.close();
    }
  }

  /**
   * Implementation of JPPF Fibonacci task.
   */
  public static class JPPFTaskFibonacci extends JPPFTask {
    /**
     * Explicit serialVersionUID.
     */
    private static final long serialVersionUID = 1L;
    /**
     * Order of Fibonacci number to compute.
     */
    private final int n;

    /**
     * Initializes Fibonacci task with given order.
     * @param n order of Fibonacci number to compute. Must be greater or equal to zero.
     */
    public JPPFTaskFibonacci(final int n) {
      this.n = n;
    }

    @Override
    public void run() {
      FibonacciResult result;
      if(ForkJoinTask.inForkJoinPool()) result = new FibonacciResult(true, new FibonacciTaskFJ(n).compute());
      else result = new FibonacciResult(false, fib(n));
      setResult(result);
    }

    /**
     * Compute Fibonacci number.
     * @param n order of Fibonacci number to compute.
     * @return the Fibonacci number.
     */
    private static long fib(final int n) {
      if(n <= 1) return n;
      return fib(n - 1) + fib(n - 2);
    }
  }
}
