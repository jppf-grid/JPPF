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

package org.jppf.examples.jobrecovery;

import java.util.*;

import org.jppf.client.*;
import org.jppf.client.persistence.*;
import org.jppf.management.JMXDriverConnectionWrapper;
import org.jppf.server.protocol.JPPFTask;
import org.jppf.server.scheduler.bundle.LoadBalancingInformation;
import org.jppf.utils.*;

/**
 * Demonstration of the job persistence API to implement jobs failover and recovery
 * in the use case of an application crash before it completes.
 * @author Laurent Cohen
 */
public class Runner
{
	/**
	 * The JPPF client.
	 */
	private static JPPFClient client = null;

	/**
	 * Entry point.
	 * @param args not used.
	 */
	public static void main(final String[] args)
	{
		JobPersistence<String> persistenceManager = null;
		try
		{
			client = new JPPFClient();
			// configure the driver so it behaves suitably for this demo
			int nbNodes = configureDriver();
			System.out.println("updated load-balancing settings, " + nbNodes + " nodes connected to the driver");
			// Create the persistence manager, the root path of the underlying store
			// is in the folder "job_store" under the current directory
			persistenceManager = new DefaultFilePersistenceManager("./job_store");
			Collection<String> keys = persistenceManager.allKeys();
			// if there is no job in the persistent store,
			// we submit a job normally and simulate an application crash
			if (keys.isEmpty())
			{
				int nbTasks = 10 * nbNodes;
				System.out.println("no job found in persistence store, creating a new job with " + nbTasks + " tasks");
				JPPFJob job = new JPPFJob();
				// add 10 tasks per node, each task waiting for 1 second
				for (int i=0; i<nbTasks; i++) job.addTask(new MyTask(1000L, i+1));
				// set the persistence manager so the job will be persisted
				// each time completed tasks are received from the driver
				job.setPersistenceManager(persistenceManager);
				// the application will exit after 6 seconds (simulated crash)
				Runnable quit = new Runnable() {
					@Override
					public void run() {
						try {
							Thread.sleep(6000L);
						} catch (Exception e) {
						}
						System.exit(1);
					}
				};
				new Thread(quit).start();
				// meanwhile, start the job execution
				executeJob(job);
			}
			// otherwise, if there are jobs in the persistence store,
			// we load them and execute them on the grid
			else
			{
				System.out.println("found jobs in persistence store: " + keys);
				for (String key: keys)
				{
					// load the job from the persistent store, using its key (= job uuid)
					JPPFJob job = persistenceManager.loadJob(key);
					System.out.println("loaded job '" + key +"' from persistence store " + persistenceManager);
					// don't forget this! the application may crash again
					job.setPersistenceManager(persistenceManager);
					// start the job execution, only non-completed tasks will be executed
					executeJob(job);
					// delete the persisted job after successful completion
					persistenceManager.deleteJob(key);
				}
			}
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
		finally
		{
			if (client != null) client.close();
			if (persistenceManager != null) persistenceManager.close();
		}
	}

	/**
	 * Execute the specified job.
	 * @param job the job to execute.
	 * @throws Exception if any error occurs.
	 */
	private static void executeJob(final JPPFJob job) throws Exception
	{
		List<JPPFTask> results = client.submit(job);
		for (JPPFTask task: results)
		{
			if (task.getException() != null) System.out.println("task "+ task.getId() + " exception occurred: " + StringUtils.getStackTrace(task.getException()));
			else System.out.println("task "+ task.getId() + " result: " + task.getResult());
		}
	}

	/**
	 * This method updates the load balancer setting of the driver,
	 * to configure the &quot;manual&quot; algorithm with a size of 1.
	 * This means no more than one task will be sent to each node at any given time.
	 * @return the number of nodes connected to the driver.
	 * @throws Exception if any error occurs while configuring the driver.
	 */
	private static int configureDriver() throws Exception
	{
		// wait until the client is fully connected
		while (!client.hasAvailableConnection()) Thread.sleep(10L);
		// get a connection to the driver's JMX server
		JPPFClientConnectionImpl c = (JPPFClientConnectionImpl) client.getClientConnection();
		JMXDriverConnectionWrapper jmxDriver = c.getJmxConnection();
		// obtain the current load-balancing settings
		LoadBalancingInformation lbi = jmxDriver.loadBalancerInformation();
		if (lbi == null) return 1;
		TypedProperties props = lbi.parameters;
		props.setProperty("size", "1");
		// set load-balancing alogrithm to "manual" with a size of 1
		jmxDriver.changeLoadBalancerSettings("manual", props);
		// return the current number of nodes
		return (int) jmxDriver.statistics().getNodes().getLatest();
	}
}
