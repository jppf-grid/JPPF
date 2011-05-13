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
package sample.dist.matrix;

import java.io.PrintWriter;
import java.util.*;
import java.util.concurrent.*;

import javax.management.*;

import org.jppf.client.*;
import org.jppf.logging.jmx.JmxLogger;
import org.jppf.management.*;
import org.jppf.node.policy.*;
import org.jppf.server.JPPFStats;
import org.jppf.server.protocol.JPPFTask;
import org.jppf.task.storage.MemoryMapDataProvider;
import org.jppf.utils.*;
import org.slf4j.*;

/**
 * Runner class for the square matrix multiplication demo.
 * @author Laurent Cohen
 */
public class MatrixRunner implements NotificationListener
{
	/**
	 * Logger for this class.
	 */
	static Logger log = LoggerFactory.getLogger(MatrixRunner.class);
	/**
	 * JPPF client used to submit execution requests.
	 */
	private JPPFClient jppfClient = null;
	/**
	 * Keeps track of the current iteration number.
	 */
	private int iterationsCount = 0;
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
	 * Entry point for this class, performs a matrix multiplication a number of times.,<br>
	 * The number of times is specified as a configuration property named &quot;matrix.iterations&quot;.<br>
	 * The size of the matrices is specified as a configuration property named &quot;matrix.size&quot;.<br>
	 * @param args - not used.
	 */
	public static void main(String...args)
	{
		MatrixRunner runner = null;
		try
		{
			/*
			System.out.println("press any key when ready to start");
			int c = System.in.read();
			*/
			String clientUuid = ((args != null) && (args.length > 0)) ? args[0] : null;
			TypedProperties props = JPPFConfiguration.getProperties();
			int size = props.getInt("matrix.size", 300);
			int iterations = props.getInt("matrix.iterations", 10);
			int nbRows = props.getInt("task.nbRows", 1);
			output("Running Matrix demo with matrix size = "+size+"*"+size+" for "+iterations+" iterations");
			runner = new MatrixRunner();
			//runner.registerToMBeans();
			runner.perform(size, iterations, nbRows, clientUuid);
			//runner.perform2(size, iterations, nbRows, clientUuid);
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}
	}
	
	/**
	 * Perform the multiplication of 2 matrices with the specified size, for a specified number of times.
	 * @param size the size of the matrices.
	 * @param iterations the number of times the multiplication will be performed.
	 * @param nbRows number of rows of matrix a per task.
	 * @param clientUuid an optional uuid to set on the JPPF client.
	 * @throws Exception if an error is raised during the execution.
	 */
	public void perform(int size, int iterations, int nbRows, String clientUuid) throws Exception
	{
		try
		{
			if (clientUuid != null) jppfClient = new JPPFClient(clientUuid);
			else jppfClient = new JPPFClient();

			// initialize the 2 matrices to multiply
			Matrix a = new Matrix(size);
			a.assignRandomValues();
			Matrix b = new Matrix(size);
			b.assignRandomValues();
			if (size <= 500) performSequentialMultiplication(a, b);
			long totalIterationTime = 0L;
	
			// determine whether an execution policy should be used
			ExecutionPolicy policy = null;
			String s = JPPFConfiguration.getProperties().getString("jppf.execution.policy");
			if (s != null)
			{
				PolicyParser.validatePolicy(s);
				policy = PolicyParser.parsePolicy(s);
			}
			// perform "iteration" times
			for (int iter=0; iter<iterations; iter++)
			{
				long elapsed = performParallelMultiplication(a, b, nbRows, policy);
				totalIterationTime += elapsed;
				output("Iteration #" + (iter+1) + " performed in " + StringUtils.toStringDuration(elapsed));
			}
			output("Average iteration time: " + StringUtils.toStringDuration(totalIterationTime / iterations));
			if (JPPFConfiguration.getProperties().getBoolean("jppf.management.enabled"))
			{
				JPPFStats stats = jppfClient.requestStatistics();
				output("End statistics :\n" + stats.toString());
			}
		}
		finally
		{
			if (jppfClient != null) jppfClient.close();
		}
	}

	/**
	 * Perform the sequential multiplication of 2 squares matrices of equal sizes.
	 * @param a the left-hand matrix.
	 * @param b the right-hand matrix.
	 * @param nbRows number of rows of matrix a per task.
	 * @param policy the execution policy to apply to the submitted job, may be null.
	 * @return the elapsed time for the computation.
	 * @throws Exception if an error is raised during the execution.
	 */
	private long performParallelMultiplication(Matrix a, Matrix b, int nbRows, ExecutionPolicy policy) throws Exception
	{
		//long start = System.currentTimeMillis();
		long start = System.nanoTime();
		int size = a.getSize();
		// create a task for each row in matrix a
		JPPFJob job = new JPPFJob();
		job.setId("matrix sample " + (iterationsCount++));
		int remaining = size;
		for (int i=0; i<size; i+= nbRows)
		{
			double[][] rows = null;
			if (remaining >= nbRows)
			{
				rows = new double[nbRows][];
				remaining -= nbRows;
			}
			else rows = new double[remaining][];
			for (int j=0; j<rows.length; j++) rows[j] = a.getRow(i + j);
			job.addTask(new ExtMatrixTask(rows));
		}
		// create a data provider to share matrix b among all tasks
		job.setDataProvider(new MemoryMapDataProvider());
		job.getDataProvider().setValue(MatrixTask.DATA_KEY, b);
		job.getJobSLA().setExecutionPolicy(policy);
		//job.getJobSLA().setMaxNodes(8);
		// submit the tasks for execution
		List<JPPFTask> results = jppfClient.submit(job);
		// initialize the resulting matrix
		Matrix c = new Matrix(size);
		// Get the matrix values from the tasks results
		int rowIdx = 0;
		for (int i=0; i<results.size(); i++)
		{
			JPPFTask matrixTask = results.get(i);
			if (matrixTask.getException() != null) throw matrixTask.getException();
			double[][] rows = (double[][]) matrixTask.getResult();
			for (int j=0; j<rows.length; j++)
			{
				for (int k=0; k<size; k++) c.setValueAt(rowIdx + j, k, rows[j][k]);
			}
			rowIdx += rows.length;
		}
		//long elapsed = System.currentTimeMillis() - start;
		//return elapsed;
		long elapsed = System.nanoTime() - start;
		return elapsed/1000000L;
	}

	/**
	 * Perform the multiplication of 2 matrices with the specified size, for a specified number of times.
	 * Here we create and close a JPPF client for each iteration.
	 * @param size the size of the matrices.
	 * @param iterations the number of times the multiplication will be performed.
	 * @param nbRows number of rows of matrix a per task.
	 * @param clientUuid an optional uuid to set on the JPPF client.
	 * @throws Exception if an error is raised during the execution.
	 */
	public void perform2(int size, int iterations, int nbRows, String clientUuid) throws Exception
	{
		try
		{
			// initialize the 2 matrices to multiply
			Matrix a = new Matrix(size);
			a.assignRandomValues();
			Matrix b = new Matrix(size);
			b.assignRandomValues();
			long totalIterationTime = 0L;
	
			// perform "iteration" times
			for (int iter=0; iter<iterations; iter++)
			{
				try
				{
					if (clientUuid != null) jppfClient = new JPPFClient(clientUuid);
					else jppfClient = new JPPFClient();
					long elapsed = performParallelMultiplication(a, b, nbRows, null);
					totalIterationTime += elapsed;
					output("Iteration #" + (iter+1) + " performed in " + StringUtils.toStringDuration(elapsed));
				}
				finally
				{
					jppfClient.close();
				}
			}
			output("Average iteration time: " + StringUtils.toStringDuration(totalIterationTime / iterations));
		}
		catch(Exception e)
		{
			throw e;
		}
	}

	/**
	 * Perform the sequential multiplication of 2 squares matrices of equal sizes.
	 * @param a the left-hand matrix.
	 * @param b the right-hand matrix.
	 */
	private void performSequentialMultiplication(Matrix a, Matrix b)
	{
		long start = System.nanoTime();
		a.multiply(b);
		long elapsed = System.nanoTime() - start;
		output("Sequential computation performed in "+StringUtils.toStringDuration(elapsed/1000000));
	}

	/**
	 * Print a message to the console and/or log file.
	 * @param message - the message to print.
	 */
	private static void output(String message)
	{
		System.out.println(message);
		log.info(message);
	}

	/**
	 * Subscribe to notifications from all the nodes.
	 * @throws Exception if any error ocurs.
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
	  String source = "driver " + jmxDriver.getHost() + ":" + jmxDriver.getPort();
	  // subbscribe to all notifications from the MBean
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
			  source = "node   " + jmxNode.getHost() + ":" + jmxNode.getPort();
			  // subbscribe to all notifications from the MBean
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
	public void handleNotification(Notification notification, final Object handback)
	{
		// to smoothe the throughput of notfications processing,
		// we submit each notification to a queue instead of handling it directly
		final String message = notification.getMessage();
		Runnable r = new Runnable()
		{
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
