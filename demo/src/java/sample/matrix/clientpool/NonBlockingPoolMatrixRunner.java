/*
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
package sample.matrix.clientpool;

import java.util.*;

import org.apache.commons.logging.*;
import org.jppf.JPPFException;
import org.jppf.client.*;
import org.jppf.server.JPPFStats;
import org.jppf.server.protocol.JPPFTask;
import org.jppf.task.storage.*;
import org.jppf.utils.*;

import sample.matrix.*;

/**
 * Runner class for the matrix multiplication demo.
 * @author Laurent Cohen
 */
public class NonBlockingPoolMatrixRunner
{
	/**
	 * Log4j logger for this class.
	 */
	static Log log = LogFactory.getLog(NonBlockingPoolMatrixRunner.class);
	/**
	 * JPPF client used to submit execution requests.
	 */
	private static JPPFClient jppfClient = null;
	/**
	 * Number of submissions that have not yet been fully executed.
	 */
	private int pendingSubmissions = 0;
	/**
	 * Number of tasks whose results ot wait for.
	 */
	private int nbTasks = 0;
	/**
	 * 
	 */
	private JPPFResultCollector[] collector = null; 
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
			int nbSubmissions = props.getInt("nb.submissions", 5);
			System.out.println("Running Matrix demo with matrix size = "+size+"*"+size+" for "+iterations+" iterations");
			NonBlockingPoolMatrixRunner runner = new NonBlockingPoolMatrixRunner();
			runner.perform(size, iterations, nbSubmissions);
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
	 * @param nbSubmissions the number of concurrent task submissions for each iteration.
	 * @throws JPPFException if an error is raised during the execution.
	 */
	public void perform(int size, int iterations, int nbSubmissions) throws JPPFException
	{
		try
		{
			// initialize the 2 matrices to multiply
			Matrix a = new Matrix(size);
			a.assignRandomValues();
			Matrix b = new Matrix(size);
			b.assignRandomValues();
	
			collector = new JPPFResultCollector[nbSubmissions];
			// perform "iteration" times
			for (int iter=0; iter<iterations; iter++)
			{
				nbTasks = size;
				pendingSubmissions = nbSubmissions;
				long start = System.currentTimeMillis();
				List<List<JPPFTask>> submissions = new ArrayList<List<JPPFTask>>();
				for (int n=0; n<nbSubmissions; n++)
				{
					collector[n] = new JPPFResultCollector(size);
					// create a task for each row in matrix a
					List<JPPFTask> tasks = new ArrayList<JPPFTask>();
					for (int i=0; i<size; i++) tasks.add(new MatrixTask(a.getRow(i)));
					submissions.add(tasks);
				}
				// create a data provider to share matrix b among all tasks
				DataProvider dataProvider = new MemoryMapDataProvider();
				dataProvider.setValue(MatrixTask.DATA_KEY, b);
				// submit the tasks for execution
				for (int n=0; n<nbSubmissions; n++)
				{
					jppfClient.submitNonBlocking(submissions.get(n), dataProvider, collector[n]);
				}

				for (int p=0; p<nbSubmissions; p++)
				{
					List<JPPFTask> results = collector[p].waitForResults();
					// initialize the resulting matrix
					Matrix c = new Matrix(size);
					// Get the matrix values from the tasks results
					for (int i=0; i<results.size(); i++)
					{
						MatrixTask matrixTask = (MatrixTask) results.get(i);
						double[] row = (double[]) matrixTask.getResult();
						for (int j=0; j<row.length; j++) c.setValueAt(i, j, row[j]);
					}
				}
				long elapsed = System.currentTimeMillis() - start;
				System.out.println("Iteration #"+(iter+1)+" performed in "+StringUtils.toStringDuration(elapsed));
			}
			JPPFStats stats = jppfClient.requestStatistics();
			System.out.println("End statistics :\n"+stats.toString());
		}
		catch(Exception e)
		{
			throw new JPPFException(e.getMessage(), e);
		}
	}
}
