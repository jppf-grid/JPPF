/*
 * Java Parallel Processing Framework.
 * Copyright (C) 2005-2009 JPPF Team.
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

package sample.test.job.management;

import java.util.List;

import org.jppf.client.*;
import org.jppf.management.JMXDriverConnectionWrapper;
import org.jppf.server.protocol.JPPFTask;
import org.jppf.utils.*;

import sample.dist.tasklength.LongTask;

/**
 * 
 * @author Laurent Cohen
 */
public class JobManagementTestRunner
{
	/**
	 * The JPPF client.
	 */
	private static JPPFClient client = null;

	/**
	 * Run the first test.
	 * @throws Exception if any error occurs.
	 */
	public void runTest1() throws Exception
	{
		TypedProperties props = JPPFConfiguration.getProperties();
		int nbTasks = props.getInt("job.management.nbTasks", 2);
		long duration = props.getLong("job.management.duration", 1000L);
		String jobId = "test1";
		JPPFResultCollector collector = new JPPFResultCollector(nbTasks);
		JPPFJob job = new JPPFJob();
		job.setId(jobId);
		job.setBlocking(false);
		job.setResultListener(collector);
		for (int i=0; i<nbTasks; i++)
		{
			job.addTask(new LongTask(duration));
		}
		client.submit(job);
		JMXDriverConnectionWrapper driver = new JMXDriverConnectionWrapper("localhost", 11198);
		driver.connect();
		while (!driver.isConnected()) Thread.sleep(10);
		// wait to ensure the job has been dispatched to the nodes
		Thread.sleep(1000);
		driver.cancelJob(jobId);
		driver.close();
		List<JPPFTask> results = collector.waitForResults();
		System.out.println("Test ended");
	}

	/**
	 * Entry point.
	 * @param args not used.
	 */
	public static void main(String...args)
	{
		try
		{
			client = new JPPFClient();
			JobManagementTestRunner runner = new JobManagementTestRunner();
			runner.runTest1();
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
}
