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

package org.jppf.gigaspaces;

import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import org.jppf.client.*;
import org.jppf.server.protocol.JPPFTask;

/**
 * Implementation of the JPPF job submission service deployed as a GigaSpace.
 * @author Laurent Cohen
 */
public class JPPFServiceImpl implements JPPFService
{
	/**
	 * Unique reference to the JPPF client.
	 */
	private static AtomicReference<JPPFClient> client = new AtomicReference<JPPFClient>(newJPPFClient());

	/**
	 * Default constructor.
	 */
	public JPPFServiceImpl()
	{
	}

	/**
	 * Submit a job sent by a local or remote GigaSpaces client.
	 * @param job the JPPF job to execute.
	 * @return  the job with the initial tasks replaced with the results.
	 * @see org.jppf.gigaspaces.JPPFService#submitJob(org.jppf.client.JPPFJob)
	 */
	public JPPFJob submitJob(JPPFJob job)
	{
		int n = job.getTasks().size();
		System.out.println("received job with " + n + " task" + (n > 1 ? "s" : ""));
		List<JPPFTask> results = null;
		try
		{
			results = getJPPFClient().submit(job);
			job.getTasks().clear();
			for (JPPFTask task: results) job.addTask(task);
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}
		return job;
	}

	/**
	 * Instantiate a new JPPF client.
	 * @return a <code>JPPFClient</code> instance.
	 */
	private static synchronized JPPFClient newJPPFClient()
	{
		JPPFClient client = new JPPFClient();
		return client;
	}

	/**
	 * Get the JPPF client.
	 * @return a <code>JPPFClient</code> instance.
	 */
	private static JPPFClient getJPPFClient()
	{
		return client.get();
	}
}
