/*
 * Java Parallel Processing Framework.
 *  Copyright (C) 2005-2009 JPPF Team. 
 * http://www.jppf.org
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * 	 http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package test.processes;

import java.io.*;
import java.util.Properties;

import org.apache.commons.logging.*;
import org.jppf.process.*;
import org.jppf.utils.StringUtils;

/**
 * 
 * @author Laurent Cohen
 */
public class TestProcess
{
	/**
	 * Logging level to use in the create processes.
	 */
	private static final String LOGGING_LEVEL = "INFO";
	/**
	 * Constant identifying the standard output of a process.
	 */
	public static final String STANDARD_OUTPUT = "std";
	/**
	 * Constant identifying the error output of a process.
	 */
	public static final String ERROR_OUTPUT = "err";
	/**
	 * Logger for this class.
	 */
	static Log log = LogFactory.getLog(TestProcess.class);

	/**
	 * Create a new node process using default values.
	 * @param nodeNumber used to distinguish the node's log files form that of other nodes.
	 * @return the started node process.
	 * @throws Exception if an error occurs while building the process.
	 */
	public ProcessWrapper startNodeProcess(int nodeNumber) throws Exception
	{
		Properties jppfConfig = ProcessConfig.buildNodeConfig();
		Properties log4jConfig = ProcessConfig.buildLog4jConfig("node" + nodeNumber + ".log", false, LOGGING_LEVEL);
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
		Properties log4jConfig = ProcessConfig.buildLog4jConfig("driver.log", false, LOGGING_LEVEL);
		return ProcessCommand.buildProcess("org.jppf.server.DriverLauncher", jppfConfig, log4jConfig, 32);
	}
	
	/**
	 * Create a new matrix sample process using default values.
	 * @param matrixSize size of the matrix to use.
	 * @param nbIter number of times the computation will be performed.
	 * @return the started node process.
	 * @throws Exception if an error occurs while building the process.
	 */
	public ProcessWrapper startMatrixSampleProcess(int matrixSize, int nbIter) throws Exception
	{
		Properties jppfConfig = ProcessConfig.buildDriverConfig();
		jppfConfig.setProperty("matrix.size", ""+matrixSize);
		jppfConfig.setProperty("matrix.iterations", ""+nbIter);
		Properties log4jConfig = ProcessConfig.buildLog4jConfig("matrix.log", true, LOGGING_LEVEL);
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
			/*
			TestProcess tp = new TestProcess();
			ProcessWrapper driver = tp.startDriverProcess();
			Thread.sleep(500);
			ProcessWrapper node = tp.startNodeProcess(1);
			Thread.sleep(1000);
			ProcessWrapper sample = tp.startMatrixSampleProcess(300, 1);
			sample.getProcess().waitFor();
			printProcessOutput(sample, "Matrix Demo");
			printProcessOutput(node, "node");
			printProcessOutput(driver, "driver");
			node.getProcess().destroy();
			driver.getProcess().destroy();
			*/
			testMatrixPerformance();
		}
		catch(Throwable t)
		{
			t.printStackTrace();
		}
		System.exit(1);
	}

	/**
	 * Performance testing for the Matrix demo, with various sizes of the square matrix.
	 * @throws Exception if an error occurs while running the test.
	 */
	public static void testMatrixPerformance() throws Exception
	{
		String line = StringUtils.padLeft("", '#', 80);
		TestProcess tp = new TestProcess();
		ProcessWrapper driver = tp.startDriverProcess();
		Thread.sleep(500);
		int[] matrixSize = new int[] { 300, 500, 700, 900, 1000 };
		int[] nbNodes = new int[] { 1, 2 };
		for (int i=0; i<nbNodes.length; i++)
		{
			ProcessWrapper[] nodes = new ProcessWrapper[nbNodes[i]];
			for (int j=0; j<nbNodes[i]; j++) nodes[i] = tp.startNodeProcess(j);
			Thread.sleep(1000);
			for (int j=0; j<matrixSize.length; j++)
			{
				long start = System.currentTimeMillis();
				output(line);
				output("Sarting test with nbNodes = "+nbNodes[i]+", matrix size = "+matrixSize[j]+", nbIter = 15");
				ProcessWrapper sample = tp.startMatrixSampleProcess(matrixSize[j], 15);
				sample.getProcess().waitFor();
				long time = System.currentTimeMillis() - start;
				output("Test performed in "+StringUtils.toStringDuration(time)+" ("+time+" ms)");
			}
			for (int j=0; j<nbNodes[i]; j++) nodes[i].getProcess().destroy();
		}
		driver.getProcess().destroy();
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
		//output("\nstandard output:\n" + p.getStandardOutput().toString().trim());
		//output("\nerror output:\n" + p.getErrorOutput().toString().trim());
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
			try
			{
				String s = "";
				while ((s != null) && (is.available() > 0))
				{
					s = reader.readLine();
					if (s != null) sb.append(s).append("\n");
				}
			}
			finally
			{
				reader.close();
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
