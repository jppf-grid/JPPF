/*
 * Java Parallel Processing Framework.
 *  Copyright (C) 2005-2009 JPPF Team. 
 * http://www.jppf.org
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * 	 http://www.apache.org/licenses/LICENSE-2.0
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
import org.jppf.server.protocol.JPPFTask;

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
			client = new JPPFClient();
			performJobSubmissions();
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}
		finally
		{
			if (client != null) client.close();
		}
	}

	/**
	 * Submit non-blocking tasks using the <code>JPPFJob</code> APIs.
	 * @throws Exception if any error occurs.
	 */
	public static void performJobSubmissions() throws Exception
	{
		int n = 10;
		System.out.println("trace 1");
		List<JPPFJob> jobList = new ArrayList<JPPFJob>();
		System.out.println("trace 2");
		jobList.add(createJob(new WaitTask(2000L), 0));
		System.out.println("trace 3");
		for (int i=1; i<=n; i++) jobList.add(createJob(new PrioritizedTask(i), i)); 
		//for (int i=n; i>=1; i--) jobList.add(createJob(new PrioritizedTask(i))); 
		System.out.println("trace 4");
		for (JPPFJob job: jobList) client.submit(job);
		System.out.println("trace 5");
		for (JPPFJob job: jobList) ((JPPFResultCollector) job.getResultListener()).waitForResults();
		System.out.println("trace 6");
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
		job.setPriority(priority);
		job.setBlocking(false);
		job.setResultListener(new JPPFResultCollector(1));
		return job;
	}
}
