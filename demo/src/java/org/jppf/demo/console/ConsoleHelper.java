/*
 * Java Parallel Processing Framework.
 * Copyright (C) 2005-2007 JPPF Team.
 * http://www.jppf.org
 *
 * This library is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as published by the
 * Free Software Foundation; either version 2.1 of the License, or (at your
 * option) any later version.
 *
 * This library is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this library; if not, write to the Free Software Foundation,
 * Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA
 */
package org.jppf.demo.console;

import java.util.Properties;

import org.apache.log4j.Logger;
import org.jppf.process.*;
import org.jppf.ui.options.*;
import org.jppf.utils.StringUtils;

/**
 * 
 * @author Laurent Cohen
 */
public final class ConsoleHelper
{
	/**
	 * Logging level to use in the create processes.
	 */
	private static final String LOGGING_LEVEL = "INFO";
	/**
	 * Log4j logger for this class.
	 */
	static Logger log = Logger.getLogger(ConsoleHelper.class);
	/**
	 * Singleton console helper instance.
	 */
	private static ConsoleHelper instance = new ConsoleHelper();
	/**
	 * The size of the matrices.
	 */
	private int matrixSize = 0;
	/**
	 * The number of times the multiplication will be performed.
	 */
	private int nbIter = 0;
	/**
	 * Determines whether the tasks should be submitted in blocking or non-blocking mode.
	 */
	private boolean blocking = true;
	/**
	 * The size of the connection pool to  use.
	 */
	private int poolSize = 1;
	/**
	 * The text area to send output to.
	 */
	private TextAreaOption area = null;
	/**
	 * The number of nodes.
	 */
	private int nbNodes = 1;
	/**
	 * Wrapper for the driver process started by the demo.
	 */
	private ProcessWrapper driver = null;
	/**
	 * Array of wrappers for the node processes started by the demo.
	 */
	private ProcessWrapper[] nodes = null;
	/**
	 * Wrapper for the client process started by the demo.
	 */
	private ProcessWrapper sample = null;
	/**
	 * Hook executed when the JVM is terminated. This is used to ensure that
	 * all subprocesses are terminated, even if the JVM is abnormally terminated.
	 */
	private Thread jvmShutdownHook = null;
	/**
	 * Thread from which processes are started. 
	 */
	private Runnable mainExecRunnable = null;

	/**
	 * Initialize this demo helper with the specified parameters.
	 */
	private ConsoleHelper()
	{
		Runnable r = new Runnable()
		{
			public void run()
			{
				destroyProcesses();
			}
		};
		jvmShutdownHook = new Thread(r);

		mainExecRunnable = new Runnable()
		{
			public void run()
			{
				try
				{
					perform();
				}
				catch(Throwable t)
				{
					t.printStackTrace();
				}
			}
		};
	}

	/**
	 * Get the singleton console helper instance.
	 * @return an instance of <code>ConsoleHelper</code>.
	 */
	public static ConsoleHelper getInstance()
	{
		return instance;
	}

	/**
	 * Initialize this demo helper with the specified parameters.
	 * @param matrixSize the size of the matrices.
	 * @param nbIter the number of times the multiplication will be performed.
	 * @param blocking determines whether the tasks should be submitted in blocking or non-blocking mode.
	 * @param poolSize the size of the connection pool to  use.
	 * @param nbNodes the number of nodes.
	 * @param area the text area to send output to.
	 * @return this console helper.
	 */
	public ConsoleHelper configure(int matrixSize, int nbIter, boolean blocking, int poolSize, int nbNodes, TextAreaOption area)
	{
		this.matrixSize = matrixSize;
		this.nbIter = nbIter;
		this.blocking = blocking;
		this.poolSize = poolSize;
		this.nbNodes = nbNodes;
		this.area = area;
		return this;
	}
	
	/**
	 * Create a new node process using default values.
	 * @param nodeNumber used to distinguish the node's log files form that of other nodes.
	 * @return the started node process.
	 * @throws Exception if an error occurs while building the process.
	 */
	public ProcessWrapper startNodeProcess(int nodeNumber) throws Exception
	{
		Properties nodeConfig = ProcessConfig.buildNodeConfig();
		Properties log4jConfig = ProcessConfig.buildLog4jConfig("node" + nodeNumber + ".log", false, LOGGING_LEVEL);
		return ProcessCommand.buildProcess("org.jppf.node.NodeLauncher", nodeConfig, log4jConfig, 64);
	}
	
	/**
	 * Create a new driver process using default values.
	 * @return the started node process.
	 * @throws Exception if an error occurs while building the process.
	 */
	public ProcessWrapper startDriverProcess() throws Exception
	{
		Properties driverConfig = ProcessConfig.buildDriverConfig();
		driverConfig.setProperty("task.bundle.autotuned.strategy", "test");
		driverConfig.setProperty("strategy.test.minSamplesToAnalyse", "100");
		driverConfig.setProperty("strategy.test.minSamplesToCheckConvergence", "50");
		Properties log4jConfig = ProcessConfig.buildLog4jConfig("driver.log", false, LOGGING_LEVEL);
		return ProcessCommand.buildProcess("org.jppf.server.DriverLauncher", driverConfig, log4jConfig, 32);
	}
	
	/**
	 * Create a new matrix sample process using default values.
	 * @return the started node process.
	 * @throws Exception if an error occurs while building the process.
	 */
	public ProcessWrapper startMatrixSampleProcess() throws Exception
	{
		Properties cfg = new Properties();
		StringBuilder sb = new StringBuilder();
		for (int i=0; i<poolSize; i++)
		{
			if (i > 0) sb.append(" ");
			sb.append("connection").append(i+1);
		}
		cfg.setProperty("jppf.drivers", sb.toString());
		for (int i=1; i<=poolSize; i++)
		{
			String name = "connection" + (i + 1);
			cfg.setProperty(name + ".jppf.server.host", "localhost");
			cfg.setProperty(name + ".class.server.port", "11111");
			cfg.setProperty(name + ".app.server.port", "11112");
			cfg.setProperty(name + ".priority", "1");
		}

		cfg.setProperty("reconnect.initial.delay", "1");
		cfg.setProperty("reconnect.max.time", "60");
		cfg.setProperty("reconnect.interval", "1");

		cfg.setProperty("matrix.size", "" + matrixSize);
		cfg.setProperty("matrix.iterations", "" + nbIter);
		cfg.setProperty("submission.blocking", "" + blocking);
		cfg.setProperty("pool.size", "" + poolSize);
		Properties log4jConfig = ProcessConfig.buildLog4jConfig("matrix.log", true, LOGGING_LEVEL);
		return ProcessCommand.buildProcess("org.jppf.demo.console.MatrixDemoRunner", cfg, log4jConfig, 64);
	}

	/**
	 * Run the demo in a separate thread.
	 * @throws Exception if an error occurs while running the test.
	 */
	public void start() throws Exception
	{
		new Thread(mainExecRunnable).start();
	}

	/**
	 * Stop the demo.
	 */
	public void stop()
	{
		try
		{
			destroyProcesses();
		}
		finally
		{
			Runtime.getRuntime().removeShutdownHook(jvmShutdownHook);
		}
	}

	/**
	 * Run the demo in the current thread.
	 * @throws Exception if an error occurs while running the test.
	 */
	public void perform() throws Exception
	{
		try
		{
			Runtime.getRuntime().addShutdownHook(jvmShutdownHook);
			String line = StringUtils.padLeft("", '#', 80);
			driver = startDriverProcess();
			Thread.sleep(500);
			nodes = new ProcessWrapper[nbNodes];
			for (int j=0; j<nbNodes; j++) nodes[j] = startNodeProcess(j);
			Thread.sleep(1000);
			long start = System.currentTimeMillis();
			output(line);
			TextAreaProcessListener listener = new TextAreaProcessListener(area);
			sample = startMatrixSampleProcess();
			sample.addProcessWrapperEventListener(listener);
			
			sample.getProcess().waitFor();
			area.findFirstWithName("/RunMatrixDemo").setEnabled(true);
			area.findFirstWithName("/CancelMatrixDemo").setEnabled(false);
			sample = null;
			long time = System.currentTimeMillis() - start;
			output("Test performed in "+StringUtils.toStringDuration(time)+" ("+time+" ms)");
		}
		finally
		{
			destroyProcesses();
			Runtime.getRuntime().removeShutdownHook(jvmShutdownHook);
		}
	}

	/**
	 * Destroy all running subprocesses still running.
	 */
	private void destroyProcesses()
	{
		if (nodes != null)
		{
			for (ProcessWrapper p: nodes) p.getProcess().destroy();
			nodes = null;
		}
		if (driver != null)
		{
			driver.getProcess().destroy();
			driver = null;
		}
		if (sample != null)
		{
			sample.getProcess().destroy();
			sample = null;
		}
	}

	/**
	 * Print a message to the console and/or log file.
	 * @param message the message to print.
	 */
	private static void output(String message)
	{
		//System.out.println(message);
		log.info(message);
	}
}
