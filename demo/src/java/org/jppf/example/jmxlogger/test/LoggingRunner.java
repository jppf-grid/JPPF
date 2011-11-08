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
package org.jppf.example.jmxlogger.test;

import java.io.PrintWriter;
import java.util.*;
import java.util.concurrent.*;

import javax.management.*;

import org.jppf.client.*;
import org.jppf.logging.jmx.JmxLogger;
import org.jppf.management.*;
import org.jppf.server.protocol.JPPFTask;
import org.slf4j.*;

/**
 * This is a template JPPF application runner.
 * It is fully commented and is designed to be used as a starting point
 * to write an application using JPPF.
 * @author Laurent Cohen
 */
public class LoggingRunner implements NotificationListener
{
	/**
	 * Logger for this class.
	 */
	static Logger log = LoggerFactory.getLogger(LoggingRunner.class);
	/**
	 * The JPPF client, handles all communications with the server.
	 * It is recommended to only use one JPPF client per JVM, so it
	 * should generally be created and used as a singleton.
	 */
	private static JPPFClient jppfClient =  null;
	/**
	 * Proxies to the MBean server of each node.
	 */
	private List<JMXNodeConnectionWrapper> jmxConnections = new ArrayList<JMXNodeConnectionWrapper>();
	/**
	 * Used to sequentialize the processing of notifications from multiple nodes.
	 */
	private ExecutorService executor = Executors.newSingleThreadExecutor();
	/**
	 * Used to print remote JMX log messages to a file.
	 */
	private PrintWriter jmxPrinter = null;

	/**
	 * The entry point for this application runner to be run from a Java command line.
	 * @param args by default, we do not use the command line arguments,
	 * however nothing prevents us from using them if need be.
	 */
	public static void main(final String...args)
	{
		try
		{
			// create the JPPFClient. This constructor call causes JPPF to read the configuration file
			// and connect with one or multiple JPPF drivers.
			jppfClient = new JPPFClient();

			// create a runner instance.
			LoggingRunner runner = new LoggingRunner();
			try
			{
				// subscribe to the notifications from all nodes
				runner.registerToMBeans();
				Thread.sleep(300000L);
				//JPPFJob job = runner.createJob();
				//runner.executeBlockingJob(job);
			}
			finally
			{
				runner.close();
			}
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

	/**
	 * Create a JPPF job that can be submitted for execution.
	 * @return an instance of the {@link org.jppf.client.JPPFJob JPPFJob} class.
	 * @throws Exception if an error occurs while creating the job or adding tasks.
	 */
	public JPPFJob createJob() throws Exception
	{
		// create a JPPF job
		JPPFJob job = new JPPFJob();

		// give this job a readable unique id that we can use to monitor and manage it.
		job.setName("Task Notification Job");

		int nbTasks = 1;
		for (int i=1; i<=nbTasks; i++)
		{
			// add a task to the job.
			job.addTask(new LoggingTask("" + i));
		}

		// there is no guarantee on the order of execution of the tasks,
		// however the results are guaranteed to be returned in the same order as the tasks.
		return job;
	}

	/**
	 * Execute a job in blocking mode. The application will be blocked until the job
	 * execution is complete.
	 * @param job the JPPF job to execute.
	 * @throws Exception if an error occurs while executing the job.
	 */
	public void executeBlockingJob(final JPPFJob job) throws Exception
	{
		// set the job in blocking mode.
		job.setBlocking(true);

		// Submit the job and wait until the results are returned.
		// The results are returned as a list of JPPFTask instances,
		// in the same order as the one in which the tasks where initially added the job.
		List<JPPFTask> results = jppfClient.submit(job);

		// process the results
		for (JPPFTask task: results)
		{
			// if the task execution resulted in an exception
			if (task.getException() != null)
			{
				System.out.println("Task " + task.getId() + " in error: " + task.getException().getMessage());
			}
			else
			{
				System.out.println("Task " + task.getId() + " successful: " + task.getResult());
			}
		}
	}

	/**
	 * Subscribe to notifications from all the nodes.
	 * @throws Exception if any error occurs.
	 */
	public void registerToMBeans() throws Exception
	{
		jmxPrinter = new PrintWriter("remote-jmx.log");
		//String name = "com.parallel.matters:name=jmxlogger,type=log4j";
		//String name = "com.parallel.matters:name=jmxlogger,type=jdk";
		String name = JmxLogger.DEFAULT_MBEAN_NAME;
		// obtain the driver connection object
		JPPFClientConnectionImpl connection = (JPPFClientConnectionImpl) jppfClient.getClientConnection();
		// get its jmx connection to the driver MBean server
		JMXDriverConnectionWrapper jmxDriver = connection.getJmxConnection();
		jmxDriver.connectAndWait(5000L);
		JmxLogger driverProxy = jmxDriver.getProxy(name, JmxLogger.class);
		// used as handback object so we know where the log messages comes from.
		String source = "driver " + jmxDriver.getHost() + ':' + jmxDriver.getPort();
		// subscribe to all notifications from the MBean
		driverProxy.addNotificationListener(this, null, source);
		/*
		 */
		// collect the information to connect to the nodes' mbean servers
		Collection<JPPFManagementInfo> nodes = jmxDriver.nodesInformation();
		for (JPPFManagementInfo node: nodes)
		{
			try
			{
				// get a jmx connection to the node MBean server
				JMXNodeConnectionWrapper jmxNode = new JMXNodeConnectionWrapper(node.getHost(), node.getPort());
				JmxLogger nodeProxy = jmxNode.getProxy(name, JmxLogger.class);

				// used as handback object so we know where the log messages comes from.
				source = "node   " + jmxNode.getHost() + ':' + jmxNode.getPort();
				// subscribe to all notifications from the MBean
				nodeProxy.addNotificationListener(this, null, source);
				jmxConnections.add(jmxNode);
			}
			catch(Exception e)
			{
				log.error(e.getMessage());
			}
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void handleNotification(final Notification notification, final Object handback)
	{
		// to smoothe the throughput of notifications processing,
		// we submit each notification to a queue instead of handling it directly
		final String message = notification.getMessage();
		Runnable r = new Runnable()
		{
			@Override
			public void run()
			{
				String s = handback.toString() + ": " + message;
				// process the notification; here we simply display the message
				System.out.print(s);
				jmxPrinter.print(s);
				jmxPrinter.flush();
			}
		};
		executor.submit(r);
	}

	/**
	 * Close the connections to all nodes.
	 */
	public void close()
	{
		for (JMXNodeConnectionWrapper jmxNode: jmxConnections)
		{
			try
			{
				jmxNode.close();
			}
			catch(Exception e)
			{
				e.printStackTrace();
			}
		}
		if (executor != null) executor.shutdown();
		jmxPrinter.close();
	}
}
