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
import org.jppf.utils.StringUtils;
import org.slf4j.*;



/**
 * Test of the executor service.
 */
public class Main
{
  /**
   * Logger for this class.
   */
  private static Logger logger = LoggerFactory.getLogger(Main.class);

  /**
   * Entry point.
   * @param args  not used.
   */
  public static void main(final String[] args)
  {
    print("Starting test");
    JPPFClient client = new JPPFClient();
    JPPFExecutorService executor = new JPPFExecutorService(client);
    int nbTasks = 1000;
    int size = 10;
    try
    {
      while (!client.hasAvailableConnection()) Thread.sleep(10L);
      //executor.setBatchSize(1);
      //executor.setBatchTimeout(1000L);
      List<Future<?>> futures = new ArrayList<Future<?>>();
      print("Adding tasks");
      for (int i=0; i<nbTasks; i++)
      {
        int[] array = new int[size];
        for (int n=0; n<size; n++) array[n] = i*size + n;
        MyRunnableTask task = new MyRunnableTask(i, array);
        futures.add(executor.submit(task));
      }
      print("Waiting for pending tasks to complete");
      for (int i=0; i<nbTasks; i++)
      {
        print("Checking task {" + i + "}");
        futures.get(i).get();
        //if (futures.get(i).get() != i) throw new Exception("Invalid future response");
      }
      print("Pending tasks completed");

    }
    catch (Exception e)
    {
      logger.error("Error", e);
    }
    finally
    {
      executor.shutdownNow();
      client.close();
    }
  }

  /**
   * Print the specified message.
   * @param msg the message to print.
   */
  private static void print(final String msg)
  {
    logger.info(msg);
    System.out.println(msg);
  }

  /**
   * 
   */
  public static class MyRunnableTask implements Runnable, Serializable
  {
    /**
     * 
     */
    private int id = -1;
    /**
     * 
     */
    private int[] array = null;

    /**
     * 
     */
    public MyRunnableTask()
    {
    }

    /**
     * 
     * @param id the task id.
     * @param array an array of int values.
     */
    public MyRunnableTask(final int id, final int[] array)
    {
      this.id = id;
      this.array = array;
    }

    @Override
    public void run()
    {
      System.out.println("MyRunnableTask#" + id + " : array=" + array + " [" + StringUtils.buildString(array) + ']');
    }
  }
}
