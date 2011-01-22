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
package sample.test.resubmit;

import java.util.List;

import org.jppf.client.*;
import org.jppf.management.JMXDriverConnectionWrapper;
import org.jppf.server.protocol.JPPFTask;
import org.jppf.utils.*;

/**
 * Runner class for the &quot;Long Task&quot; demo.
 * @author Laurent Cohen
 */
public class MyRunner
{
	/**
	 * JPPF client used to submit execution requests.
	 */
	private static JPPFClient jppfClient = null;

	/**
	 * Entry point for this class, submits the tasks to the server.
	 * @param args not used.
	 */
	public static void main(String...args)
	{
		try
		{
			jppfClient = new JPPFClient();
			TypedProperties props = JPPFConfiguration.getProperties();
			int nbTask = 20;
			System.out.println("Running " + nbTask + " tasks");
			long start = System.currentTimeMillis();
			JPPFJob job = new JPPFJob();
			for (int i=0; i<nbTask; i++) job.addTask(new MyTask());
			job.setId("my job");
			job.setBlocking(false);
			MyResultCollector collector = new MyResultCollector(nbTask, job);
			job.setResultListener(collector);
			// submit the tasks for execution
			JPPFClientConnectionImpl c = (JPPFClientConnectionImpl) jppfClient.getClientConnection();
			//jppfClient.submit(job);
			c.submit(job);
			Thread.sleep(3000L);
			System.out.println("killing driver1");
			JMXDriverConnectionWrapper jmx = c.getJmxConnection();
			// kill the driver and restart it after 10 seconds
			jmx.restartShutdown(1L, -1L);
			List<JPPFTask> results = collector.waitForResults();
			long elapsed = System.currentTimeMillis() - start;
			System.out.println("Execution performed in " + StringUtils.toStringDuration(elapsed));
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}
		finally
		{
			if (jppfClient != null) jppfClient.close();
		}
	}
}
