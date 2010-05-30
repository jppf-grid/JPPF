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

package test.manynodes;

import java.io.File;

import org.jppf.client.JPPFClient;
import org.jppf.utils.FileUtils;

/**
 * Unit tests for {@link JPPFExecutorService}.
 * @author Laurent Cohen
 */
public class ManyNodes
{
	/**
	 * Message used for successful task execution.
	 */
	public static final String EXECUTION_SUCCESSFUL_MESSAGE = "execution successful";
	/**
	 * The node to lunch for the test.
	 */
	private static NodeProcessLauncher[] nodes = null;
	/**
	 * The node to lunch for the test.
	 */
	private static DriverProcessLauncher driver = null;
	/**
	 * The jppf client to use.
	 */
	private static JPPFClient client = null;
	/**
	 * Shutdown hook used to destroy the driver and node processes, in case the JVM terminates abnormally.
	 */
	private static Thread shutdownHook = null;
	/**
	 * Default duration for tasks that use a duration. Adjust the value for slow hardware.
	 */
	private static final long TASK_DURATION = 100L;
	/**
	 * Number of nodes to start.
	 */
	private static int nbNodes = 8;

	/**
	 * Entry point.
	 * @param args not used.
	 */
	public static void main(String...args)
	{
		try
		{
			setup();
			Thread.sleep(60000);
			cleanup();
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}
	}

	/**
	 * Launches a driver and node and start the client.
	 * @throws Exception if a process could not be started.
	 */
	public static void setup() throws Exception
	{
		shutdownHook = new Thread()
		{
			public void run()
			{
				stopProcesses();
			}
		};
		Runtime.getRuntime().addShutdownHook(shutdownHook);
		int nbNodes = 250;
		nodes = new NodeProcessLauncher[nbNodes];

		/*
		driver = new DriverProcessLauncher();
		driver.startProcess();
		*/
		for (int i=0; i<nbNodes; i++)
		{
			createNodeConfigFile(i+1);
			createNodeLoggingFile(i+1);
			nodes[i] = new NodeProcessLauncher(i+1);
		}
		for (int i=0; i<nbNodes; i++) nodes[i].startProcess();
		//client = new JPPFClient();
	}

	/**
	 * Stops the driver and node and close the client.
	 * @throws Exception if a process could not be stopped.
	 */
	public static void cleanup() throws Exception
	{
		stopProcesses();
		Runtime.getRuntime().removeShutdownHook(shutdownHook);
		if (client != null) client.close();
	}

	/**
	 * Stop all remote processes.
	 */
	private static void stopProcesses()
	{
		if (nodes != null) for (NodeProcessLauncher node: nodes) node.stopProcess();
		if (driver != null) driver.stopProcess();
	}

	/**
	 * Creates a node's JPPF configuration file.
	 * @param n the node number.
	 * @throws Exception if any error occurs.
	 */
	private static void createNodeConfigFile(int n) throws Exception
	{
		File file = new File("config/manynodes/node" + n + ".properties");
		if (file.exists()) return;
		StringBuilder sb = new StringBuilder();
		sb.append("jppf.server.host = 192.168.1.11\n");
		sb.append("jppf.jvm.options = -server -Xmx32m\n");
		sb.append("processing.threads = 1\n");
		sb.append("reconnect.max.time = 5\n");
		sb.append("id = ").append(n).append("\n");
		sb.append("\n");
		FileUtils.writeTextFile(file.getPath(), sb.toString());
	}

	/**
	 * Creates a node's log4j configuration file.
	 * @param n the node number.
	 * @throws Exception if any error occurs.
	 */
	private static void createNodeLoggingFile(int n) throws Exception
	{
		File file = new File("config/manynodes/log4j-node" + n + ".properties");
		if (file.exists()) return;
		StringBuilder sb = new StringBuilder();
		sb.append("log4j.appender.JPPF=org.apache.log4j.FileAppender\n");
		sb.append("log4j.appender.JPPF.File=jppf-node").append(n).append(".log\n");
		sb.append("log4j.appender.JPPF.Append=false\n");
		sb.append("log4j.appender.JPPF.layout=org.apache.log4j.PatternLayout\n");
		sb.append("log4j.appender.JPPF.layout.ConversionPattern=%d [%-5p][%c.%M(%L)]: %m\\n\n");
		sb.append("log4j.rootLogger=INFO, JPPF\n");
		sb.append("log4j.logger.org.jppf.comm.discovery=INFO\n");
		FileUtils.writeTextFile(file.getPath(), sb.toString());
	}
}
