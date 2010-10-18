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

package org.jppf.test.setup;

import org.jppf.client.JPPFClient;
import org.junit.*;

/**
 * Unit tests for {@link JPPFExecutorService}.
 * @author Laurent Cohen
 */
public class Setup1D1N1C
{
	/**
	 * Message used for successful task execution.
	 */
	public static final String EXECUTION_SUCCESSFUL_MESSAGE = "execution successful";
	/**
	 * The node to lunch for the test.
	 */
	protected static NodeProcessLauncher node = null;
	/**
	 * The node to lunch for the test.
	 */
	protected static DriverProcessLauncher driver = null;
	/**
	 * The jppf client to use.
	 */
	protected static JPPFClient client = null;
	/**
	 * Shutdown hook used to destroy the driver and node processes, in case the JVM terminates abnormally.
	 */
	protected static Thread shutdownHook = null;

	/**
	 * Launches a driver and node and start the client.
	 * @throws Exception if a process could not be started.
	 */
	@BeforeClass
	public static void setup() throws Exception
	{
		System.out.println("performing setup");
		/*
		shutdownHook = new Thread()
		{
			public void run()
			{
				node.stopProcess();
				driver.stopProcess();
			}
		};
		Runtime.getRuntime().addShutdownHook(shutdownHook);

		driver = new DriverProcessLauncher();
		driver.startProcess();
		node = new NodeProcessLauncher(1);
		node.startProcess();
		*/
		client = new JPPFClient();
		// give some time for everyone to initialize
		try
		{
			Thread.sleep(1000L);
		}
		catch(Exception e)
		{
		}
	}

	/**
	 * Stops the driver and node and close the client.
	 * @throws Exception if a process could not be stopped.
	 */
	@AfterClass
	public static void cleanup() throws Exception
	{
		System.out.println("performing cleanup");
		try
		{
			Thread.sleep(1000L);
		}
		catch(Exception e)
		{
		}
		client.close();
		/*
		node.stopProcess();
		driver.stopProcess();
		Runtime.getRuntime().removeShutdownHook(shutdownHook);
		*/
	}
}
