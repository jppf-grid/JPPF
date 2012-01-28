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

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import org.jppf.utils.JPPFThreadFactory;

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
  static int n = 10;
  /**
   * 
   */
  static AtomicInteger count = new AtomicInteger(n * million);
  /**
   * 
   * @param args not used.
   */
  public static void main(final String[] args)
  {
    try
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
    catch (Exception e)
    {
      e.printStackTrace();
    }
  }
}
