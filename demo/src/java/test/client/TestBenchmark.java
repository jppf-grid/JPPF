/*
 * Java Parallel Processing Framework.
 * Copyright (C) 2005-2007 JPPF Team.
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

package test.client;

import java.util.*;

import org.apache.commons.logging.*;
import org.jppf.process.*;
import org.jppf.process.NodePropertiesBuilder.NodePermission;

/**
 * 
 * @author Laurent Cohen
 */
public class TestBenchmark
{
	/**
	 * Log4j logger for this class.
	 */
	private static Log log = LogFactory.getLog(TestBenchmark.class);
	/**
	 * Logging level to use in the create processes.
	 */
	private static final String LOGGING_LEVEL = "INFO";
	/**
	 * Array of matrix sizes and corresponding number of iterations.
	 */
	static final int MATRIX_ITER[][] = new int[][] { {300, 30}, {500, 25}, {700, 20}, {900, 15} };
	//static final int MATRIX_ITER[][] = new int[][] { {300, 1}, {500, 1}, {700, 1}, {900, 1} };
	//static final int MATRIX_ITER[][] = new int[][] { {500, 10} };
	/**
	 * Array of matrix sizes and corresponding number of iterations.
	 */
	static final int NODES_THREADS[][] = new int[][] { {1, 1}, {1, 2}, {2, 1} };
	//static final int NODES_THREADS[][] = new int[][] { {1, 2} };
	/**
	 * Separator for log and standard output.
	 */
	static final String SEPARATOR =
		"*--------------------------------------------------------------------------------------------------*\n";	

	/**
	 * Entrypoint for this class.
	 * @param args not used
	 */
	public static void main(String...args)
	{
		try
		{
			TestBenchmark tb = new TestBenchmark();
			log(SEPARATOR);
			log("java.vendor : " + System.getProperty("java.vendor"));
			log("java.vendor.url : " + System.getProperty("java.vendor.url"));
			log("java.version : " + System.getProperty("java.version"));
			log("java.vm.name : " + System.getProperty("java.vm.name"));
			log("java.vm.version : " + System.getProperty("java.vm.version"));
			log("java.compiler : " + System.getProperty("java.compiler"));
			log("");
			for (int i=0; i<NODES_THREADS.length; i++)
			{
				log(SEPARATOR);
				log("new node configuration");
				log("");
				for (int j=0; j<MATRIX_ITER.length; j++)
				{
					tb.test(NODES_THREADS[i][0], NODES_THREADS[i][1], MATRIX_ITER[j][0], MATRIX_ITER[j][1]);
					Thread.sleep(3000);
				}
			}
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}
	}

	/**
	 * Create a new node process using default values.
	 * @param nodeConfig JPPF configuration properties for the node.
	 * @param permissions a list of descriptions of the permissions granted to the node.
	 * @param log4jConfig log4j configuration properties for the node.
	 * @return the started node process.
	 * @throws Exception if an error occurs while building the process.
	 */
	public ProcessWrapper startNodeProcess(Properties nodeConfig, List<NodePermission> permissions,
		Properties log4jConfig) throws Exception
	{
		return ProcessCommand.buildNodeProcess("org.jppf.node.NodeLauncher", nodeConfig, permissions, log4jConfig, 128);
	}

	/**
	 * Create a new driver process using default values.
	 * @param driverConfig JPPF configuration properties for the driver.
	 * @param log4jConfig log4j configuration properties for the driver.
	 * @return the started node process.
	 * @throws Exception if an error occurs while building the process.
	 */
	public ProcessWrapper startDriverProcess(Properties driverConfig, Properties log4jConfig) throws Exception
	{
		return ProcessCommand.buildProcess("org.jppf.server.DriverLauncher", driverConfig, log4jConfig, 32);
	}
	
	/**
	 * Create a new matrix sample process using default values and a client connect to 2 drivers.
	 * @param matrixSize size of the matrix to use.
	 * @param nbIter number of times the computation will be performed.
	 * @return the started client process.
	 * @throws Exception if an error occurs while building the process.
	 */
	public ProcessWrapper startMatrixSampleProcess(int matrixSize, int nbIter) throws Exception
	{
		Properties c1 = ClientPropertiesBuilder.buildDriverConnection("driver1", "localhost", 11111, 11112, 10);
		Properties clientConfig = ClientPropertiesBuilder.buildClientConfig("driver1", new Properties[] {c1});
		clientConfig.setProperty("matrix.size", ""+matrixSize);
		clientConfig.setProperty("matrix.iterations", ""+nbIter);
		Properties log4jConfig = ProcessConfig.buildLog4jConfig("matrix.log", false, LOGGING_LEVEL);
		return ProcessCommand.buildProcess("sample.matrix.MatrixRunner", clientConfig, log4jConfig, 128);
	}

	/**
	 * Perform first test.
	 * @param nbNodes number of nodes used for the test.
	 * @param threadsPerNode number of execution threads for each node.
	 * @param matrixSize size of the matrix to use.
	 * @param nbIter number of times the computation will be performed.
	 * @throws Exception if any error occurs.
	 */
	public void test(int nbNodes, int threadsPerNode, int matrixSize, int nbIter) throws Exception
	{
		int size = 2 + nbNodes;
		int n = 0;
		ProcessWrapper[] process = new ProcessWrapper[size];
		try
		{
			Properties log4jConfig = ProcessConfig.buildLog4jConfig("driver1.log", false, LOGGING_LEVEL);
			process[n++] = startDriverProcess(DriverPropertiesBuilder.DRIVER_1, log4jConfig);
			Thread.sleep(1000);
			List<NodePermission> permissions = NodePropertiesBuilder.buildBasePermissions();
			Properties nodeConfig = NodePropertiesBuilder.buildNodeConfig("localhost", 11111, 11113, threadsPerNode);
			for (int i=0; i<nbNodes; i++)
			{
				log4jConfig = ProcessConfig.buildLog4jConfig("node" + n + ".log", false, LOGGING_LEVEL);
				process[n++] = startNodeProcess(nodeConfig, permissions, log4jConfig);
			}
			Thread.sleep(1000);
			process[n] = startMatrixSampleProcess(matrixSize, nbIter);
			process[n].getProcess().waitFor();
			log(SEPARATOR);
			log("" + nbNodes + " node(s) - " + threadsPerNode + " thread(s)\n");
			log(process[n].getStandardOutput().toString());
			log("");
		}
		finally
		{
			for (int i=0; i<size; i++) process[i].getProcess().destroy();
		}
	}

	/**
	 * Log a string on the console and in the log file.
	 * @param msg the string to log.
	 */
	private static void log(String msg)
	{
		System.out.println(msg);
		log.info(msg);
	}
}
