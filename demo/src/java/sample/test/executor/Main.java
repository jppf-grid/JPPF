/*
 * JPPF.
 * Copyright (C) 2005-2011 JPPF Team.
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

import java.util.concurrent.*;

import org.jppf.client.JPPFClient;
import org.jppf.scheduling.JPPFSchedule;


/**
 * Test of the executor service.
 */
public class Main
{
  /**
   * 
   */
  private static JPPFClient client;
  /**
   * 
   */
  private static ExecutorService executor;

	/**
	 * Entry point.
	 * @param args  not used.
	 */
	public static void main(String[] args)
	{
		System.out.println("Starting test");
    try
    {
      executor = createExecutor();
      MyCallable task = new MyCallable();
      Future<String> future = executor.submit(task);
      System.out.println("result: " + future.get());
    }
    catch (Exception e)
    {
      e.printStackTrace();
    }
    finally
    {
      shutdownExecutor();
    }
	}

	/**
	 * Create and configure the executor.
	 * @return an <code>ExecutorService</code> instance.
	 * @throws Exception if any error occurs.
	 */
	public static ExecutorService createExecutor() throws Exception
	{
    client = new JPPFClient();
		MyJPPFExecutorService executor = new MyJPPFExecutorService(client);
		executor.registerClass(MyCallable.class, new ExecutionProperties(new JPPFSchedule(2000L), "taskExpired"));
		return executor;
	}

	/**
	 * Shutdown the executor.
	 */
	public static void shutdownExecutor()
	{
    executor.shutdown();
    client.close();
	}
}
