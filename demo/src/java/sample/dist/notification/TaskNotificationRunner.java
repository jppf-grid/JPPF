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
package sample.dist.notification;

import java.util.*;

import org.apache.commons.logging.*;
import org.jppf.JPPFException;
import org.jppf.client.*;
import org.jppf.management.*;
import org.jppf.server.*;
import org.jppf.server.protocol.JPPFTask;
import org.jppf.utils.*;

/**
 * Runner class for the &quot;Task Notification&quot; demo.
 * @author Laurent Cohen
 */
public class TaskNotificationRunner
{
	/**
	 * Logger for this class.
	 */
	static Log log = LogFactory.getLog(TaskNotificationRunner.class);
	/**
	 * JPPF client used to submit execution requests.
	 */
	private static JPPFClient jppfClient = null;

	/**
	 * Entry point for this class, submits the tasks with a set duration to the server.
	 * @param args not used.
	 */
	public static void main(String...args)
	{
		try
		{
			jppfClient = new JPPFClient();
			TypedProperties props = JPPFConfiguration.getProperties();
			long duration = props.getInt("notification.duration");
			int nbStages = props.getInt("notification.nbStages");
			int nbTasks = props.getInt("notification.nbTasks");
			int iterations = props.getInt("notification.iterations");
			System.out.println("Running Task notification demo with " + nbTasks + " tasks with " + nbStages +
				" stages and a stage duration of "+ duration + " ms, for "+iterations+" iterations");
			perform(nbTasks, nbStages, duration, iterations);
			System.exit(0);
		}
		catch(Exception e)
		{
			e.printStackTrace();
			System.exit(1);
		}
	}
	
	/**
	 * Run a number of tasks that simulate the execution of a specified number of stages.
	 * @param nbTasks the number of tasks to send at each iteration.
	 * @param nbStages the number of stages in each task.
	 * @param duration the duration of each stage.
	 * @param iterations the number of times the the tasks will be sent.
	 * @throws JPPFException if an error is raised during the execution.
	 */
	private static void perform(int nbTasks, int nbStages, long duration, int iterations) throws JPPFException
	{
		try
		{
			initializeJmxNotifications();
			// perform "iteration" times
			for (int iter=0; iter<iterations; iter++)
			{
				long start = System.currentTimeMillis();
				// create the tasks
				JPPFJob job = new JPPFJob();
				for (int i=0; i<nbTasks; i++) job.addTask(new StagedTask(i, nbStages, duration));
				// submit the tasks for execution
				List<JPPFTask> results = jppfClient.submit(job);
				for (JPPFTask task: results)
				{
					Exception e = task.getException();
					if (e != null) throw e;
				}
				long elapsed = System.currentTimeMillis() - start;
				System.out.println("Iteration #"+(iter+1)+" performed in "+StringUtils.toStringDuration(elapsed));
			}
			JPPFStats stats = jppfClient.requestStatistics();
			System.out.println("End statistics :\n"+stats.toString());
		}
		catch(Exception e)
		{
			throw new JPPFException(e.getMessage(), e);
		}
	}

	/**
	 * Query the JPPF driver for its attached nodes and establish a connection
	 * to their JMX server.
	 * @throws Exception if the ocnnection to the driver failed.
	 */
	public static void initializeJmxNotifications() throws Exception
	{
		// get a handle to the driver connection
		JPPFClientConnectionImpl c = (JPPFClientConnectionImpl) jppfClient.getClientConnection();
		// wait until the connection to the driver is active
		while (!c.getStatus().equals(JPPFClientConnectionStatus.ACTIVE))
			Thread.sleep(100L);
		// query the driver's JMX server for the attached nodes
		Collection<NodeManagementInfo> nodeList = null;
		int count = 20;
		while ((count-- > 0) && (nodeList == null))
		{
			JMXDriverConnectionWrapper wrapper = c.getJmxConnection();
			if (wrapper != null) nodeList = wrapper.nodesInformation();
			if (nodeList == null) Thread.sleep(100L);
		}
		if (nodeList == null) return;
		// establish the connection to every node
		final List<JMXNodeConnectionWrapper> jmxConnections = new ArrayList<JMXNodeConnectionWrapper>();
		for (NodeManagementInfo info: nodeList)
		{
			JMXNodeConnectionWrapper jmxClient = new JMXNodeConnectionWrapper(info.getHost(), info.getPort());
			jmxClient.connect();
			jmxConnections.add(jmxClient);
		}
		// create a task that will periodically query each node for the latest task notification
		TimerTask timerTask = new TimerTask()
		{
			public void run()
			{
				for (JMXNodeConnectionWrapper jmxClient: jmxConnections)
				{
					queryTaskNotification(jmxClient);
				}
			}
		};
		Timer timer = new Timer("JMX notifications timer");
		// schedule the task with a 1 second delay between executions
		timer.schedule(timerTask, 100L, 1000L);
	}

	/**
	 * Get the latest task notification from a node.
	 * @param jmxClient the JMX connection to the node.
	 */
	public static void queryTaskNotification(JMXNodeConnectionWrapper jmxClient)
	{
		try
		{
			// invoke the method "notification" on the node's MBean
			TaskNotification notif = (TaskNotification) jmxClient.notification();
			// display the resulting notification
			System.out.println("Monitored node " + jmxClient.getId() +
				" received notification: " + notif);
		}
		catch (Exception e)
		{
			// ... handle the exception ...
			System.out.println(e.getMessage());
		}
	}
}
