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

package test.org.jppf.test.setup;

import org.junit.*;

/**
 * Unit tests for {@link JPPFExecutorService}.
 * This class starts and stops a driver and a node before and after
 * running the tests in a unit test class.
 * @author Laurent Cohen
 */
public class Setup1D1N
{
	/**
	 * The node to lunch for the test.
	 */
	protected static NodeProcessLauncher node = null;
	/**
	 * The node to lunch for the test.
	 */
	protected static DriverProcessLauncher driver = null;
	/**
	 * Shutdown hook used to destroy the driver and node processes, in case the JVM terminates abnormally.
	 */
	protected static Thread shutdownHook = null;
	/**
	 * Specifies whether to launch the driver and node processes, or rely on externally launched ones.
	 */
	protected static boolean launchProcesses = true;

	/**
	 * Stop driver and node processes.
	 */
	protected static void stopProcesses()
	{
		try
		{
			if (node != null) node.stopProcess();
			if (driver != null) driver.stopProcess();
		}
		catch(Throwable t)
		{
			t.printStackTrace();
		}
	}

	/**
	 * Create the shutdown hook.
	 */
	protected static void createShutdownHook()
	{
		shutdownHook = new Thread()
		{
			@Override
            public void run()
			{
				stopProcesses();
			}
		};
		Runtime.getRuntime().addShutdownHook(shutdownHook);
	}

	/**
	 * Launches a driver and node and start the client.
	 * @throws Exception if a process could not be started.
	 */
	@BeforeClass
	public static void setup() throws Exception
	{
		System.out.println("performing setup");
		if (launchProcesses)
		{
			createShutdownHook();
			(driver = new DriverProcessLauncher()).startProcess();
			// to avoid driver and node producing the same UUID
			Thread.sleep(51L);
			(node = new NodeProcessLauncher(1)).startProcess();
		}
	}

	/**
	 * Stops the driver and node and close the client.
	 * @throws Exception if a process could not be stopped.
	 */
	@AfterClass
	public static void cleanup() throws Exception
	{
		Thread.sleep(1000L);
		if (launchProcesses)
		{
			stopProcesses();
			Runtime.getRuntime().removeShutdownHook(shutdownHook);
		}
	}
}
