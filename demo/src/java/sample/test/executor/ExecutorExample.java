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

package sample.test.executor;

import java.io.Serializable;
import java.util.*;
import java.util.concurrent.*;

import org.jppf.client.JPPFClient;
import org.jppf.client.concurrent.JPPFExecutorService;

/**
 * 
 * @author Laurent Cohen
 */
public class ExecutorExample {

  /**
   * 
   * @param args not used.
   */
  public static void main(final String[] args)
  {
    System.out.println("Starting test");
    JPPFClient client = new JPPFClient();
    try
    {
      JPPFExecutorService executor = new JPPFExecutorService(client);
      List<Future<Integer>> futures = new ArrayList<Future<Integer>>(20);
      System.out.println("Adding tasks");
      for (int i = 0; i < 20; i++) futures.add(executor.submit(new SimpleCountTask(i)));
      System.out.println("Waiting for pending tasks to complete");
      executor.shutdown();
      while (!executor.isTerminated()) Thread.sleep(1000);
      System.out.println("Pending tasks completed");
      for (int i = 0; i < 20; i++)
      {
        System.out.println("Checking task " + i);
        int returned = futures.get(i).get();
        if (returned != i) throw new Exception("Invalid future response");
      }
      System.out.println("All completed tasks checked");
    }
    catch (Exception e)
    {
      System.out.println("Error " + e);
    }
    finally
    {
      client.close();
    }
    System.out.println("exiting");
  }

  /**
   * 
   */
  private static class SimpleCountTask implements Callable<Integer>, Serializable {

    /**
     * Serial version UID.
     */
    private static final long serialVersionUID = 3044260680117586115L;
    /**
     * ID of this task.
     */
    private int number;

    /**
     * 
     * @param number the task number.
     */
    public SimpleCountTask(final int number) {
      this.number = number;
    }

    @Override
    public Integer call() throws Exception {
      System.out.println("From " + number);
      System.out.println("From stdout " + number);
      return number;
    }

  }
}