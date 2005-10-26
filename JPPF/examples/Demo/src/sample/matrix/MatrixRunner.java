/*
 * Java Parallel Processing Framework.
 * Copyright (C) 2005 Laurent Cohen.
 * lcohen@osp-chicago.com
 * 
 * This program is free software; you can redistribute it and/or modify it under the terms of
 * the GNU General Public License as published by the Free Software Foundation;
 * either version 2 of the License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License along with this program;
 * if not, write to the Free Software Foundation, Inc.,
 * 59 Temple Place, Suite 330,
 * Boston, MA 02111-1307 USA
 */
package sample.matrix;

import java.util.*;
import org.apache.log4j.Logger;
import org.jppf.comm.*;
import org.jppf.task.*;
import org.jppf.task.admin.*;
import org.jppf.task.storage.*;
import org.jppf.utils.PropertyManager;

/**
 * RUnner class for the Mattrix example.
 * @author Laurent Cohen
 */
public class MatrixRunner
{
	/**
	 * Log4j logger for this class.
	 */
	static Logger log = Logger.getLogger(MatrixRunner.class);

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
			int size = PropertyManager.getInt("test", "matrix.size");
			int iterations = PropertyManager.getInt("test", "matrix.iterations");
			perform(size, iterations);
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
	 * @throws ExecutionServiceException  if an error is raised during the execution.
	 */
	private static void perform(int size, int iterations) throws ExecutionServiceException
	{
		// initialize the 2 matrices to multiply
		Matrix a = new Matrix(size);
		a.assignRandomValues();
		Matrix b = new Matrix(size);
		b.assignRandomValues();

		// perform "iteration" times
		for (int iter=0; iter<iterations; iter++)
		{
			// create a task for each row in matrix a
			List<Task> tasks = new ArrayList<Task>();
			for (int i=0; i<size; i++) tasks.add(new MatrixTask(a.getRow(i)));
			// create the request to send to the remote service
			ExecutionRequest request = new ExecutionRequest();
			request.setContent(tasks);
			// create a data provider to share matrix b among all tasks
			DataProvider dataProvider = new MemoryMapDataProvider();
			dataProvider.setValue(MatrixTask.DATA_KEY, b);
			request.setDataProvider(dataProvider);
			// get a reference to the execution service
			RequestQueue requestQueue = RequestQueueFactory.getRemoteQueue();
			// submit the execution request and wait for its completion
			ExecutionResponse response = (ExecutionResponse) requestQueue.submitBlocking(request);
			// initialize the resulting matrix
			Matrix c = new Matrix(size);
			// Get the matrix c values from the tasks results
			for (int i=0; i<response.getContent().size(); i++)
			{
				MatrixTask matrixTask = (MatrixTask) response.getContent().get(i);
				double[] row = matrixTask.getResult();
				for (int j=0; j<row.length; j++) c.setValueAt(i, j, row[j]);
			}
		}
	}
}
