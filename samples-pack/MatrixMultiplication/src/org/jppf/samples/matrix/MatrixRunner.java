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
package org.jppf.samples.matrix;

import java.util.List;

import org.jppf.client.*;
import org.jppf.node.policy.*;
import org.jppf.server.protocol.JPPFTask;
import org.jppf.task.storage.MemoryMapDataProvider;
import org.jppf.utils.*;
import org.slf4j.*;

/**
 * Runner class for the square matrix multiplication demo.
 * @author Laurent Cohen
 */
public class MatrixRunner
{
	/**
	 * Logger for this class.
	 */
	static Logger log = LoggerFactory.getLogger(MatrixRunner.class);
	/**
	 * JPPF client used to submit execution requests.
	 */
	private static JPPFClient jppfClient = null;
	/**
	 * Keeps track of the current iteration number.
	 */
	private static int iterationsCount = 0;

	/**
	 * Entry point for this class, performs a matrix multiplication a number of times.,<br>
	 * The number of times is specified as a configuration property named &quot;matrix.iterations&quot;.<br>
	 * The size of the matrices is specified as a configuration property named &quot;matrix.size&quot;.<br>
	 * @param args - not used.
	 */
	public static void main(String...args)
	{
		try
		{
			if ((args != null) && (args.length > 0)) jppfClient = new JPPFClient(args[0]);
			else jppfClient = new JPPFClient();
			TypedProperties props = JPPFConfiguration.getProperties();
			int size = props.getInt("matrix.size", 300);
			int iterations = props.getInt("matrix.iterations", 10);
			int nbRows = props.getInt("task.nbRows", 1);
			output("Running Matrix demo with matrix size = "+size+"*"+size+" for "+iterations+" iterations");
			perform(size, iterations, nbRows);
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
	 * Perform the multiplication of 2 matrices with the specified size, for a specified number of times.
	 * @param size - the size of the matrices.
	 * @param iterations - the number of times the multiplication will be performed.
	 * @param nbRows - number of rows of matrix a per task.
	 * @throws Exception if an error is raised during the execution.
	 */
	private static void perform(int size, int iterations, int nbRows) throws Exception
	{
		try
		{
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
			/*
			if (JPPFConfiguration.getProperties().getBoolean("jppf.management.enabled"))
			{
				JPPFStats stats = jppfClient.requestStatistics();
				output("End statistics :\n" + stats.toString());
			}
			*/
		}
		catch(Exception e)
		{
			throw e;
		}
	}

	/**
	 * Perform the sequential multiplication of 2 squares matrices of equal sizes.
	 * @param a - the left-hand matrix.
	 * @param b - the right-hand matrix.
	 * @param nbRows - number of rows of matrix a per task.
	 * @param policy - the execution policy to apply to the submitted job, may be null.
	 * @return the elapsed time for the computation.
	 * @throws Exception if an error is raised during the execution.
	 */
	private static long performParallelMultiplication(Matrix a, Matrix b, int nbRows, ExecutionPolicy policy) throws Exception
	{
		long start = System.currentTimeMillis();
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
		return System.currentTimeMillis() - start;
	}

	/**
	 * Perform the sequential multiplication of 2 squares matrices of equal sizes.
	 * @param a - the left-hand matrix.
	 * @param b - the right-hand matrix.
	 */
	private static void performSequentialMultiplication(Matrix a, Matrix b)
	{
		long start = System.currentTimeMillis();
		Matrix c = a.multiply(b);
		long elapsed = System.currentTimeMillis() - start;
		output("Sequential computation performed in "+StringUtils.toStringDuration(elapsed));
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
}
