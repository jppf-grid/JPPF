/*
 * JPPF.
 *  Copyright (C) 2005-2009 JPPF Team. 
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
package org.jppf.demo.console;

import java.util.*;

import org.apache.commons.logging.*;
import org.jppf.JPPFException;
import org.jppf.client.*;
import org.jppf.server.protocol.JPPFTask;
import org.jppf.task.storage.*;
import org.jppf.utils.*;

import sample.matrix.*;

/**
 * Runner class for the matrix multiplication demo.
 * @author Laurent Cohen
 */
public class MatrixDemoRunner
{
	/**
	 * Logger for this class.
	 */
	static Log log = LogFactory.getLog(MatrixDemoRunner.class);
	/**
	 * JPPF client used to submit execution requests.
	 */
	private static JPPFClient jppfClient = null;

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
			int size = props.getInt("matrix.size", 300);
			int iterations = props.getInt("matrix.iterations", 10);
			int poolSize = props.getInt("pool.size", 1);
			boolean blocking = props.getBoolean("submission.blocking", true);
			System.out.println("Running Matrix demo with matrix size = " + size + "*" + size + " for " + iterations + " iterations");
			System.out.println("submission mode: " + (blocking ? "" : "non-") + "blocking, poolSize: " + poolSize);
			MatrixDemoRunner runner = new MatrixDemoRunner();
			runner.perform(size, iterations, blocking, poolSize);
			System.exit(0);
		}
		catch(Throwable t)
		{
			t.printStackTrace();
			System.exit(1);
		}
	}
	
	/**
	 * Perform the multiplication of 2 matrices with the specified size, for a specified number of times.
	 * @param size the size of the matrices.
	 * @param iterations the number of times the multiplication will be performed.
	 * @param blocking determines whether the tasks should be submitted in blocking or non-blocking mode.
	 * @param poolSize the size of the connection pool to  use
	 * @throws JPPFException if an error is raised during the execution.
	 */
	public void perform(int size, int iterations, boolean blocking, int poolSize) throws JPPFException
	{
		try
		{
			// initialize the 2 matrices to multiply
			Matrix a = new Matrix(size);
			a.assignRandomValues();
			Matrix b = new Matrix(size);
			b.assignRandomValues();
	
			// perform "iteration" times
			long totalElapsed = 0L;
			for (int iter=0; iter<iterations; iter++)
			{
				long start = System.currentTimeMillis();
				// create a task for each row in matrix a
				List<JPPFTask> tasks = new ArrayList<JPPFTask>();
				for (int i=0; i<size; i++) tasks.add(new MatrixTask(a.getRow(i)));
				// create a data provider to share matrix b among all tasks
				DataProvider dataProvider = new MemoryMapDataProvider();
				dataProvider.setValue(MatrixTask.DATA_KEY, b);
				// submit the tasks for execution
				List<JPPFTask> results = null;
				if (blocking) results = submit(tasks, dataProvider);
				else if (poolSize == 1) results = submitAsynchronous(tasks, dataProvider);
				else results = submitPooled(tasks, dataProvider, poolSize);
				// initialize the resulting matrix
				Matrix c = new Matrix(size);
				// Get the matrix values from the tasks results
				for (int i=0; i<results.size(); i++)
				{
					MatrixTask matrixTask = (MatrixTask) results.get(i);
					double[] row = (double[]) matrixTask.getResult();
					for (int j=0; j<row.length; j++) c.setValueAt(i, j, row[j]);
				}
				long elapsed = System.currentTimeMillis() - start;
				totalElapsed += elapsed;
				System.out.println("Iteration #"+(iter+1)+" performed in "+StringUtils.toStringDuration(elapsed));
			}
			System.out.println("Average iteration time = " + StringUtils.toStringDuration(totalElapsed/iterations));
		}
		catch(Exception e)
		{
			throw new JPPFException(e.getMessage(), e);
		}
	}

	/**
	 * Submit tasks in a blocking way.
	 * @param tasks the list of tasks to submit.
	 * @param dataProvider the corresponding dataprovider.
	 * @return the results as a list of <code>JPPFTask</code> instances.
	 * @throws Exception if an error occurs during the submission.
	 */
	private List<JPPFTask> submit(List<JPPFTask> tasks, DataProvider dataProvider) throws Exception
	{
		return jppfClient.submit(tasks, dataProvider);
	}

	/**
	 * Submit tasks asynchronously.
	 * @param tasks the list of tasks to submit.
	 * @param dataProvider the corresponding dataprovider.
	 * @return the results as a list of <code>JPPFTask</code> instances.
	 * @throws Exception if an error occurs during the submission.
	 */
	private List<JPPFTask> submitAsynchronous(List<JPPFTask> tasks, DataProvider dataProvider) throws Exception
	{
		JPPFResultCollector collector = new JPPFResultCollector(tasks.size());
		jppfClient.submitNonBlocking(tasks, dataProvider, collector);
		return collector.waitForResults();
	}

	/**
	 * Submit tasks asynchronously using a connection pool to balance the load.
	 * The set of tasks is divided into multiple subsets of (almost) equal size.
	 * The subsets are then submitted concurrently through the connection pool.
	 * @param tasks the list of tasks to submit.
	 * @param dataProvider the corresponding dataprovider.
	 * @param poolSize the size of the connection pool to  use
	 * @return the results as a list of <code>JPPFTask</code> instances.
	 * @throws Exception if an error occurs during the submission.
	 */
	private List<JPPFTask> submitPooled(List<JPPFTask> tasks, DataProvider dataProvider, int poolSize) throws Exception
	{
		int size = tasks.size();
		// we divide the task list into multiple list of equal size
		// (except for one that might be larger with)
		int[] sizes = new int[poolSize];
		for (int i=0; i<poolSize - 1; i++) sizes[i] = size / poolSize;
		sizes[poolSize-1] = size - ((poolSize-1) *  (size / poolSize));
		List<List<JPPFTask>> allTasks = new ArrayList<List<JPPFTask>>();
		int idx = 0;
		for (int i=0; i<poolSize; i++)
		{
			List<JPPFTask> subList = new ArrayList<JPPFTask>();
			for (int j=0; j<sizes[i]; j++) subList.add(tasks.get(idx++));
			allTasks.add(subList);
		}
		
		JPPFResultCollector[] collector = new JPPFResultCollector[poolSize];
		for (int i=0; i<poolSize; i++) collector[i] = new JPPFResultCollector(sizes[i]);
		for (int i=0; i<poolSize; i++) jppfClient.submitNonBlocking(allTasks.get(i), dataProvider, collector[i]);
		List<JPPFTask> results = new ArrayList<JPPFTask>();
		for (int i=0; i<poolSize; i++)
		{
			List<JPPFTask> res = collector[i].waitForResults();
			for (JPPFTask task: res) results.add(task);
		}
		return results;
	}

}
