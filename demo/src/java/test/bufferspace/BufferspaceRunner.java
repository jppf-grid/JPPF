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
package test.bufferspace;

import java.io.*;
import java.util.*;

import org.apache.commons.logging.*;
import org.jppf.client.*;
import org.jppf.management.*;
import org.jppf.server.protocol.JPPFTask;
import org.jppf.utils.*;

/**
 * Runner class for the matrix multiplication demo.
 * @author Laurent Cohen
 */
public class BufferspaceRunner
{
	/**
	 * Logger for this class.
	 */
	static Log log = LogFactory.getLog(BufferspaceRunner.class);
	/**
	 * JPPF client used to submit execution requests.
	 */
	private static JPPFClient jppfClient = null;
	/**
	 * One kilobyte.
	 */
	private static final int KILO = 1024;
	/**
	 * One megabyte.
	 */
	private static final int MEGA = 1024 * KILO;
	/**
	 * JMX connection to one node.
	 */
	private static JMXNodeConnectionWrapper nodeJmx = null;

	/**
	 * Entry point for this class, performs a matrix multiplication a number of times.,<br>
	 * The number of times is specified as a configuration property named &quot;matrix.iterations&quot;.<br>
	 * The size of the matrices is specified as a configuration property named &quot;matrix.size&quot;.<br>
	 * @param args not used.
	 */
	public static void main(String...args)
	{
		try
		{
			jppfClient = new JPPFClient();
			TypedProperties props = JPPFConfiguration.getProperties();
			int iterations = props.getInt("datasize.iterations", 1);
			int datasize = props.getInt("datasize.size", 1);
			int nbTasks = props.getInt("datasize.nbTasks", 10);
			String unit = props.getString("datasize.unit", "b").toLowerCase();
			if ("k".equals(unit)) datasize *= KILO;
			else if ("m".equals(unit)) datasize *= MEGA;
			long duration = props.getLong("datasize.duration", -1L);
			output("Running datasize demo with data size = " + datasize + " with " + nbTasks + " tasks of duration " + duration + " ms for " + iterations + " iterations");
			for (int i=0; i<iterations; i++) perform(i, datasize, nbTasks, duration);
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
	 * Perform the test.
	 * @param iteration the iteration number.
	 * @param datasize the data size for each task.
	 * @param nbTasks the number of tasks.
	 * @param duration the duration of each task.
	 * @throws Exception if an error is raised during the execution.
	 */
	private static void perform(int iteration, int datasize, int nbTasks, long duration) throws Exception
	{
		long totalTime = System.currentTimeMillis();
		JPPFJob job = new JPPFJob();
		for (int i=0; i<nbTasks; i++)
		{
			BufferspaceTask task = new BufferspaceTask(datasize, duration);
			task.setId("" + i);
			job.addTask(task);
		}
		JPPFResultCollector collector = new JPPFResultCollector(job.getTasks().size());
		job.setBlocking(false);
		job.setResultListener(collector);
		jppfClient.submit(job);
		//Thread.sleep(100L);
		//resetNodeJmx();
		List<JPPFTask> results = collector.waitForResults();
		for (JPPFTask t: results)
		{
			if (t instanceof BufferspaceTask)
			{
				BufferspaceTask task = (BufferspaceTask) t;
				if (!task.isExecuted())
				{
					Exception e = task.getException();
					System.out.println(" task #" + task.getId() + " was not executed, exception = " + ((e == null) ? "null" : getStackTrace(e)));
				}
			}
			if (t.getException() != null) throw t.getException();
		}
		totalTime = System.currentTimeMillis() - totalTime;
		output("iteration #" + iteration +" performed in " + StringUtils.toStringDuration(totalTime));
	}

	/**
	 * Reste the jmx node connection.
	 * @throws Exception if any error occurs.
	 */
	private static void resetNodeJmx() throws Exception
	{
		JPPFClientConnectionImpl connection = (JPPFClientConnectionImpl) jppfClient.getAllConnections().get(0);
		JMXDriverConnectionWrapper driverJmx = connection.getJmxConnection();
		if (!driverJmx.isConnected()) driverJmx.connectAndWait(2000L);
		Collection<JPPFManagementInfo> nodes = driverJmx.nodesInformation();
		JPPFManagementInfo info = nodes.iterator().next();
		nodeJmx = new JMXNodeConnectionWrapper(info.getHost(), info.getPort());
		nodeJmx.connectAndWait(5000L);
		if (!nodeJmx.isConnected()) output("not connected to node through JMX !!");
		nodeJmx.restart();
	}

	/**
	 * Print a message to the console and/or log file.
	 * @param message the message to print.
	 */
	private static void output(String message)
	{
		System.out.println(message);
		log.info(message);
	}

	/**
	 * Return an exception stack trace as a string.
	 * @param t the throwable to get the stack trace from.
	 * @return a string.
	 */
	static String getStackTrace(Throwable t)
	{
		try
		{
			StringWriter sw = new StringWriter();
			PrintWriter writer = new PrintWriter(sw);
			t.printStackTrace(writer);
			return sw.toString();
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}
		return "";
	}
}
