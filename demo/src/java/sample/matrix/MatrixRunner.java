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
package sample.matrix;

import java.util.*;
import org.apache.log4j.Logger;
import org.jppf.JPPFException;
import org.jppf.client.JPPFClient;
import org.jppf.server.JPPFStats;
import org.jppf.server.protocol.JPPFTask;
import org.jppf.task.storage.*;
import org.jppf.utils.*;

/**
 * Runner class for the matrix multiplication demo.
 * @author Laurent Cohen
 */
public class MatrixRunner
{
	/**
	 * Log4j logger for this class.
	 */
	static Logger log = Logger.getLogger(MatrixRunner.class);
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
			int size = props.getInt("matrix.size",300);
			int iterations = props.getInt("matrix.iterations",10);
			System.out.println("Running Matrix demo with matrix size = "+size+"*"+size+" for "+iterations+" iterations");
			perform(size, iterations);
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
	private static void perform(int size, int iterations) throws JPPFException
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
				long start = System.currentTimeMillis();
				// create a task for each row in matrix a
				List<JPPFTask> tasks = new ArrayList<JPPFTask>();
				for (int i=0; i<size; i++) tasks.add(new MatrixTask(a.getRow(i)));
				// create a data provider to share matrix b among all tasks
				DataProvider dataProvider = new MemoryMapDataProvider();
				dataProvider.setValue(MatrixTask.DATA_KEY, b);
				// submit the tasks for execution
				jppfClient.submit(tasks, dataProvider);
				List<JPPFTask> results = jppfClient.submit(tasks, dataProvider);
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
			System.out.println("End statistics :\n"+stats.toString());
		}
		catch(Exception e)
		{
			throw new JPPFException(e.getMessage(), e);
		}
	}
}
