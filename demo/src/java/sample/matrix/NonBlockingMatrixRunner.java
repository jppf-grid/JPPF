/*
 * Java Parallel Processing Framework.
 * Copyright (C) 2005-2008 JPPF Team.
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
package sample.matrix;

import java.util.*;

import org.apache.commons.logging.*;
import org.jppf.JPPFException;
import org.jppf.client.JPPFClient;
import org.jppf.client.event.*;
import org.jppf.server.JPPFStats;
import org.jppf.server.protocol.JPPFTask;
import org.jppf.task.storage.*;
import org.jppf.utils.*;

/**
 * Runner class for the matrix multiplication demo.
 * @author Laurent Cohen
 */
public class NonBlockingMatrixRunner implements TaskResultListener
{
	/**
	 * Logger for this class.
	 */
	static Log log = LogFactory.getLog(NonBlockingMatrixRunner.class);
	/**
	 * JPPF client used to submit execution requests.
	 */
	private static JPPFClient jppfClient = null;
	/**
	 * Count of results received.
	 */
	private int count = 0;
	/**
	 * Number of tasks whose results ot wait for.
	 */
	private int nbTasks = 0;
	/**
	 * 
	 */
	private Map<Integer, List<JPPFTask>> resultMap = new TreeMap<Integer, List<JPPFTask>>();

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
			int size = props.getInt("matrix.size",300);
			int iterations = props.getInt("matrix.iterations",10);
			System.out.println("Running Matrix demo with matrix size = "+size+"*"+size+" for "+iterations+" iterations");
			NonBlockingMatrixRunner runner = new NonBlockingMatrixRunner();
			runner.perform(size, iterations);
			jppfClient.close();
			System.exit(0);
		}
		catch(Exception e)
		{
			e.printStackTrace();
			System.exit(1);
		}
	}
	
	/**
	 * Perform the multiplication of 2 matrices with the specified size, for a specified number of times.
	 * @param size the size of the matrices.
	 * @param iterations the number of times the multiplication will be performed.
	 * @throws JPPFException if an error is raised during the execution.
	 */
	public synchronized void perform(int size, int iterations) throws JPPFException
	{
		try
		{
			// initialize the 2 matrices to multiply
			Matrix a = new Matrix(size);
			a.assignRandomValues();
			Matrix b = new Matrix(size);
			b.assignRandomValues();
	
			// perform "iteration" times
			for (int iter=0; iter<iterations; iter++)
			{
				count = 0;
				resultMap.clear();
				nbTasks = size;
				long start = System.currentTimeMillis();
				// create a task for each row in matrix a
				List<JPPFTask> tasks = new ArrayList<JPPFTask>();
				for (int i=0; i<size; i++) tasks.add(new MatrixTask(a.getRow(i)));
				// create a data provider to share matrix b among all tasks
				DataProvider dataProvider = new MemoryMapDataProvider();
				dataProvider.setValue(MatrixTask.DATA_KEY, b);
				// submit the tasks for execution
				jppfClient.submitNonBlocking(tasks, dataProvider, this);
				waitForResults();
				List<JPPFTask> results = new ArrayList<JPPFTask>();
				for (Integer n: resultMap.keySet()) results.addAll(resultMap.get(n));
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
				System.out.println("Iteration #"+(iter+1)+" performed in "+StringUtils.toStringDuration(elapsed));
			}
			JPPFStats stats = jppfClient.requestStatistics();
			if (stats != null) System.out.println("End statistics :\n"+stats.toString());
		}
		catch(Exception e)
		{
			throw new JPPFException(e.getMessage(), e);
		}
	}

	/**
	 * 
	 * @param event notification that a set of tasks results have been received. 
	 * @see org.jppf.client.event.TaskResultListener#resultsReceived(org.jppf.client.event.TaskResultEvent)
	 */
	public synchronized void resultsReceived(TaskResultEvent event)
	{
		int idx = event.getStartIndex();
		List<JPPFTask> tasks = event.getTaskList();
		System.out.println("Received results for tasks " + idx + " - " + (idx + tasks.size() - 1));
		resultMap.put(idx, tasks);
		count += tasks.size();
		notify();
	}
	
	/**
	 * Wait until all results of a request have been collected.
	 */
	private synchronized void waitForResults()
	{
		while (count < nbTasks)
		{
			try
			{
				wait();
			}
			catch(InterruptedException e)
			{
				e.printStackTrace();
			}
		}
	}
}
