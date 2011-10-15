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

package test.priority;

import java.util.*;

import org.jppf.client.*;
import org.jppf.server.protocol.*;

/**
 * 
 * @author Laurent Cohen
 */
public class PriorityTestRunner
{
	/**
	 * The JPPF client.
	 */
	static JPPFClient client = null;

	/**
	 * Entry point into the test.
	 * @param args not used.
	 */
	public static void main(String...args)
	{
		try
		{
			System.out.println("Starting ...");
			client = new JPPFClient();
			//performJobSubmissions();
			perform();
			//perform2();
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}
		finally
		{
			if (client != null) client.close();
		}
		System.out.println("... done");
	}

	/**
	 * Submit non-blocking tasks using the <code>JPPFJob</code> APIs.
	 * @throws Exception if any error occurs.
	 */
	public static void performJobSubmissions() throws Exception
	{
		int n = 10;
		List<JPPFJob> jobList = new ArrayList<JPPFJob>();
		jobList.add(createJob(new WaitTask(2000L), 0));
		for (int i=1; i<=n; i++) jobList.add(createJob(new PrioritizedTask(i), i)); 
		for (JPPFJob job: jobList) client.submit(job);
		for (JPPFJob job: jobList) ((JPPFResultCollector) job.getResultListener()).waitForResults();
	}

	/**
	 * Submit non-blocking tasks.
	 * @throws Exception if any error occurs.
	 */
	public static void perform() throws Exception
	{
		JPPFJob job = new JPPFJob();
		job.addTask(new PrioritizedTask(0));
		JPPFResultCollector collector = new JPPFResultCollector(1);
		job.setResultListener(collector);
		job.setBlocking(false);
		client.submit(job);
		List<JPPFTask> results = collector.waitForResults();
	}

	/**
	 * Submit blocking tasks.
	 * @throws Exception if any error occurs.
	 */
	public static void perform2() throws Exception
	{
		JPPFJob job = new JPPFJob();
		job.addTask(new PrioritizedTask(0));
		List<JPPFTask> results = client.submit(job);
	}

	/**
	 * Create a non-blocking job with the specified task in it.
	 * @param task the task to put in the job.
	 * @param priority the job priority.
	 * @return a JPPFJob instance.
	 * @throws Exception if any error occurs.
	 */
	private static JPPFJob createJob(JPPFTask task, int priority) throws Exception
	{
		JPPFJob job = new JPPFJob();
		job.addTask(task);
		job.getSLA().setPriority(priority);
		job.setBlocking(false);
		job.setResultListener(new JPPFResultCollector(1));
		return job;
	}
}
