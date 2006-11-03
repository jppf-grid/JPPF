/*
 * Java Parallel Processing Framework.
 * Copyright (C) 2005-2006 Laurent Cohen.
 * lcohen@osp-chicago.com
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
package test.processes;

import java.io.*;
import java.util.Properties;
import org.apache.log4j.Logger;
import org.jppf.process.*;

/**
 * 
 * @author Laurent Cohen
 */
public class TestProcess
{
	/**
	 * Constant identifying the standard output of a process.
	 */
	public static final String STANDARD_OUTPUT = "std";
	/**
	 * Constant identifying the error output of a process.
	 */
	public static final String ERROR_OUTPUT = "err";
	/**
	 * Log4j logger for this class.
	 */
	static Logger log = Logger.getLogger(TestProcess.class);

	/**
	 * Create a new node process using default values.
	 * @return the started node process.
	 * @throws Exception if an error occurs while building the process.
	 */
	public ProcessWrapper startNodeProcess() throws Exception
	{
		Properties jppfConfig = ProcessConfig.buildNodeConfig();
		Properties log4jConfig = ProcessConfig.buildLog4jConfig("node.log");
		return ProcessCommand.buildProcess("org.jppf.node.NodeLauncher", jppfConfig, log4jConfig, 64);
	}
	
	/**
	 * Create a new driver process using default values.
	 * @return the started node process.
	 * @throws Exception if an error occurs while building the process.
	 */
	public ProcessWrapper startDriverProcess() throws Exception
	{
		Properties jppfConfig = ProcessConfig.buildDriverConfig();
		Properties log4jConfig = ProcessConfig.buildLog4jConfig("driver.log");
		return ProcessCommand.buildProcess("org.jppf.server.DriverLauncher", jppfConfig, log4jConfig, 32);
	}
	
	/**
	 * Create a new matrix sample process using default values.
	 * @param matrixSize size of the matrix to use.
	 * @param nbIter number of times the computation will be eprformed.
	 * @return the started node process.
	 * @throws Exception if an error occurs while building the process.
	 */
	public ProcessWrapper startMatrixSampleProcess(int matrixSize, int nbIter) throws Exception
	{
		Properties jppfConfig = ProcessConfig.buildDriverConfig();
		jppfConfig.setProperty("matrix.size", ""+matrixSize);
		jppfConfig.setProperty("matrix.iterations", ""+nbIter);
		Properties log4jConfig = ProcessConfig.buildLog4jConfig("matrix.log");
		return ProcessCommand.buildProcess("sample.matrix.MatrixRunner", jppfConfig, log4jConfig, 64);
	}
	
	/**
	 * Entry point for testing this class.
	 * @param args not used.
	 */
	public static void main(String...args)
	{
		try
		{
			TestProcess tp = new TestProcess();
			ProcessWrapper driver = tp.startDriverProcess();
			Thread.sleep(500);
			ProcessWrapper node = tp.startNodeProcess();
			Thread.sleep(1000);
			ProcessWrapper sample = tp.startMatrixSampleProcess(300, 1);
			sample.getProcess().waitFor();
			printProcessOutput(sample, "Matrix Demo");
			printProcessOutput(node, "node");
			printProcessOutput(driver, "driver");
			node.getProcess().destroy();
			driver.getProcess().destroy();
		}
		catch(Throwable t)
		{
			t.printStackTrace();
		}
		System.exit(1);
	}

	/**
	 * Print the standard and error output content of the specified process. 
	 * @param p the process whose output is to be printed.
	 * @param processName the name of the process, printed in the output header.
	 * @throws Exception if an error occurs while fetching the content of the process output.
	 */
	public static void printProcessOutput(ProcessWrapper p, String processName) throws Exception
	{
		output("Output for process ["+processName+"] :");
		output("\nstandard output:\n" + p.getStandardOutput().toString().trim());
		output("\nerror output:\n" + p.getErrorOutput().toString().trim());
	}

	/**
	 * Get the output of the driver process.
	 * @param process the process to get the standard or error output from.
	 * @param streamType detrmines whether to obtain the standard or error output.
	 * @return the output as a string.
	 * @throws Exception if an error occurs while getting the content of the output stream.
	 */
	public static String getOutput(Process process, String streamType) throws Exception
	{
		StringBuilder sb = new StringBuilder();
		InputStream is = null;
		if (STANDARD_OUTPUT.equals(streamType)) is = process.getInputStream();
		else is = process.getErrorStream();
		if (is.available() > 0)
		{
			LineNumberReader reader = new LineNumberReader(new InputStreamReader(is));
			String s = "";
			while ((s != null) && (is.available() > 0))
			{
				s = reader.readLine();
				if (s != null) sb.append(s).append("\n");
			}
		}
		return sb.toString();
	}

	/**
	 * Display a message and wait for any user input.
	 * @throws Exception .
	 */
	public static void input() throws Exception
	{
		System.out.println("enter something");
		System.in.read();
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
}
