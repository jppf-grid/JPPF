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

package test.generic;

import java.text.NumberFormat;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import org.jppf.utils.*;

/**
 * 
 * @author Laurent Cohen
 */
public class TestScheduledExecutor
{
  /**
   * 
   */
  static int million = 1000 * 1000;
  /**
   * 
   */
  static int n = 1;
  /**
   * 
   */
  static AtomicInteger count = new AtomicInteger(n * million);
  /**
   * 
   */
  static int nbThreads = 4;

  /**
   * 
   * @param args not used.
   */
  public static void main(final String[] args)
  {
    try
    {
      //perform2();
      perform3();
    }
    catch (Exception e)
    {
      e.printStackTrace();
    }
  }

  /**
   * 
   * @throws Exception if any error occurs.
   */
  public static void perform1() throws Exception
  {
    ScheduledExecutorService executor = Executors.newScheduledThreadPool(1, new JPPFThreadFactory("TestScheduledExecutor"));
    for (int i=0; i<n*million; i++)
    {
      if ((i+1) % million == 0) System.out.println("done " + (i/million) + "/" + n + " M");
      Runnable r = new Runnable()
      {
        @Override
        public void run()
        {
          count.decrementAndGet();
        }
      };
      executor.schedule(r, 1, TimeUnit.MICROSECONDS);
    }
    System.out.println("scheduled all tasks");
    while (count.get() > 0) Thread.sleep(1);
    //executor.
    System.out.println("all tasks executed");
    Thread.sleep(100000);
    //executor.shutdownNow();
  }

  /**
   * 
   * @throws Exception if any error occurs.
   */
  public static void perform2() throws Exception
  {
    System.out.println(" Test JPPF uuid = " + new JPPFUuid(JPPFUuid.HEXADECIMAL_CHAR, 32).toString());
    long start = System.nanoTime();
    for (int i=0; i<n*million; i++)
    {
      //String uuid = new JPPFUuid(JPPFUuid.HEXADECIMAL, 32).toString();
      String uuid = new JPPFUuid(JPPFUuid.HEXADECIMAL_CHAR, 32).toString();
      //String uuid = new JPPFUuid().toString();
    }
    long elapsed1 = System.nanoTime() - start;
    long avg1 = elapsed1 / (n*million);
    System.out.println(" JPPF uuid time = " + StringUtils.toStringDuration(elapsed1/million) + ", avg = " + avg1 + " ns");
    System.out.println(" Test JDK uuid = " + UUID.randomUUID().toString());
    start = System.nanoTime();
    for (int i=0; i<n*million; i++)
    {
      String uuid = UUID.randomUUID().toString();
    }
    long elapsed2 = System.nanoTime() - start;
    long avg2 = elapsed2 / (n*million);
    System.out.println(" JDK uuid time = " + StringUtils.toStringDuration(elapsed2/million) + ", avg = " + avg2 + " ns");
  }

  /**
   * 
   * @throws Exception if any error occurs.
   */
  public static void perform3() throws Exception
  {
    int[] threadValues = { 1, 2, 4, 8, 16, 24, 32 };
    //int[] threadValues = { 1 };
    for (int i=0; i<threadValues.length; i++)
    {
      nbThreads = threadValues[i];
      performTest3();
    }
  }

  /**
   * 
   * @throws Exception if any error occurs.
   */
  private static void performTest3() throws Exception
  {
    NumberFormat nf = NumberFormat.getNumberInstance(new Locale("en", "US"));
    nf.setGroupingUsed(true);
    nf.setMinimumFractionDigits(0);
    nf.setMaximumFractionDigits(0);
    ExecutorService executor = Executors.newFixedThreadPool(nbThreads);
    try
    {
      System.out.println(StringUtils.padRight("", '-', 40));
      System.out.println("Running test with nbThreads = " + nbThreads + ", runs per thread = " + nf.format(n * million));
      System.out.println("Test JPPF uuid = " + new JPPFUuid(JPPFUuid.HEXADECIMAL_CHAR, 32).toString());
      Runnable task1 = new Runnable()
      {
        @Override
        public void run()
        {
          for (int i=0; i<n*million; i++)
          {
            //String uuid = new JPPFUuid(JPPFUuid.HEXADECIMAL, 32).toString();
            String uuid = new JPPFUuid(JPPFUuid.HEXADECIMAL_CHAR, 32).toString();
            //String uuid = new JPPFUuid().toString();
          }
        }
      };
      executeTest(executor, task1, "JPPF");

      System.out.println("Test JDK uuid = " + UUID.randomUUID().toString());
      Runnable task2 = new Runnable()
      {
        @Override
        public void run()
        {
          for (int i=0; i<n*million; i++)
          {
            String uuid = UUID.randomUUID().toString();
          }
        }
      };
      executeTest(executor, task2, "JDK");
    }
    finally
    {
      executor.shutdownNow();
    }
  }

  /**
   * 
   * @param executor .
   * @param task .
   * @param uuidProvider .
   * @throws Exception .
   */
  private static void executeTest(final ExecutorService executor, final Runnable task, final String uuidProvider) throws Exception
  {
    Future<?>[] futures = new Future<?>[nbThreads];
    long start = System.nanoTime();
    for (int i=0; i<nbThreads; i++) futures[i] = executor.submit(task);
    for (int i=0; i<nbThreads; i++) futures[i].get();
    long elapsed = System.nanoTime() - start;
    long avg = elapsed / (nbThreads*n*million);
    System.out.println(uuidProvider + " uuid time = " + StringUtils.toStringDuration(elapsed/million) + ", avg = " + avg + " ns");
  }
}
