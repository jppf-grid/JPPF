/*
 * JPPF.
 * Copyright (C) 2005-2010 JPPF Team.
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

package test.callable;

import java.io.Serializable;
import java.util.*;
import java.util.concurrent.*;

import junit.framework.TestCase;

import org.jppf.client.*;
import org.jppf.client.concurrent.JPPFExecutorService;
import org.jppf.server.protocol.JPPFTask;

/**
 * JPPF Test.
 * @author Laurent Cohen
 */
public class TryJPPF extends TestCase implements Serializable
{
	/**
	 * Test executor service.
	 * @throws Exception if any error occurs.
	 */
	public void testJPPFExecutor() throws Exception
	{
		ExecutorService executor = new JPPFExecutorService(new JPPFClient());
		Set<Future> out = new HashSet<Future>();
		for (int i = 0; i < 20; i++)
		{
			Future result = executor.submit(new Greeting("executor " + i));
			out.add(result);
		}
		executor.shutdown();

		int size = 0;
		int failures = 0;
		while (!out.isEmpty())
		{
			Iterator<Future> i = out.iterator();
			while (i.hasNext())
			{
				Future task = i.next();
				if (task.isDone())
				{
					try
					{
						System.out.println(task.get());
					}
					catch (Exception e)
					{
						e.printStackTrace();
					}
					i.remove();
				}
			}
		}
	}

	/**
	 * Test JPPF job.
	 * @throws Exception if any error occurs.
	 */
	public void __testJPPFJob() throws Exception
	{
		JPPFClient client = new JPPFClient();
		JPPFJob job = new JPPFJob();
		job.setBlocking(false);
		int nbTasks = 2;
		for (int i = 0; i < nbTasks; i++)
		{
			JPPFTask task = job.addTask(new Greeting("job " + i));
		}
		// needed for asynchronous job (blocking = false)
		JPPFResultCollector collector = new JPPFResultCollector(nbTasks);
		job.setResultListener(collector);
		client.submit(job);

		List<JPPFTask> tasks = collector.waitForResults();
		for (JPPFTask task : tasks)
		{
			Object out = task.getResult();
			System.out.println(out);
		}
	}

	/**
	 * 
	 */
	private static class Greeting implements Callable, Serializable
	{
		/**
		 * Result message.
		 */
		private String msg;

		/**
		 * Initialize with the specified message.
		 * @param msg message to use as a result.
		 */
		public Greeting(String msg)
		{
			this.msg = msg;
		}

		/**
		 * {@inheritDoc}
		 */
		public Object call() throws Exception
		{
			Thread.sleep(2000);
			return "return value: " + msg;
		}
	}
}