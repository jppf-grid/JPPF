/*
 * Java Parallel Processing Framework.
 * Copyright (C) 2005-2007 JPPF Team.
 * http://www.jppf.org
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
			if ((args != null) && (args.length > 0)) 
				jppfClient = new JPPFClient(args[0]);
			else jppfClient = new JPPFClient();
			TypedProperties props = JPPFConfiguration.getProperties();
			int size = props.getInt("matrix.size", 300);
			int iterations = props.getInt("matrix.iterations", 10);
			int nbRows = props.getInt("task.nbRows", 1);
			output("Running Matrix demo with matrix size = "+size+"*"+size+" for "+iterations+" iterations");
			perform(size, iterations, nbRows);
			System.exit(0);
		}
		catch(Exception e)
		{
			e.printStackTrace();
			output("before exit(1)");
			System.exit(1);
		}
	}
	
	/**
	 * Perform the multiplication of 2 matrices with the specified size, for a specified number of times.
	 * @param size the size of the matrices.
	 * @param iterations the number of times the multiplication will be performed.
	 * @param nbRows number of rows of matrix a per task.
	 * @throws JPPFException if an error is raised during the execution.
	 */
	private static void perform(int size, int iterations, int nbRows) throws JPPFException
	{
		try
		{
			// initialize the 2 matrices to multiply
			Matrix a = new Matrix(size);
			a.assignRandomValues();
			Matrix b = new Matrix(size);
			b.assignRandomValues();
			performSequentialMultiplication(a, b);
			long totalIterationTime = 0L;
	
			// perform "iteration" times
			for (int iter=0; iter<iterations; iter++)
			{
				long elapsed = performParallelMultiplication(a, b, nbRows);
				totalIterationTime += elapsed;
				output("Iteration #"+(iter+1)+" performed in "+StringUtils.toStringDuration(elapsed));
			}
			output("Average iteration time: " + StringUtils.toStringDuration(totalIterationTime / iterations));
			JPPFStats stats = jppfClient.requestStatistics();
			output("End statistics :\n"+stats.toString());
		}
		catch(Exception e)
		{
			throw new JPPFException(e.getMessage(), e);
		}
	}

	/**
	 * Perform the sequential multiplication of 2 squares matrices of equal sizes.
	 * @param a the left-hand matrix.
	 * @param b the right-hand matrix.
	 * @param nbRows number of rows of matrix a per task.
	 * @return the elpased time for the computation.
	 * @throws Exception if an error is raised during the execution.
	 */
	private static long performParallelMultiplication(Matrix a, Matrix b, int nbRows) throws Exception
	{
		long start = System.currentTimeMillis();
		int size = a.getSize();
		// create a task for each row in matrix a
		List<JPPFTask> tasks = new ArrayList<JPPFTask>();
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
			tasks.add(new ExtMatrixTask(rows));
		}
		// create a data provider to share matrix b among all tasks
		DataProvider dataProvider = new MemoryMapDataProvider();
		dataProvider.setValue(MatrixTask.DATA_KEY, b);
		// submit the tasks for execution
		List<JPPFTask> results = jppfClient.submit(tasks, dataProvider);
		// initialize the resulting matrix
		Matrix c = new Matrix(size);
		// Get the matrix values from the tasks results
		int rowIdx = 0;
		long start2 = System.currentTimeMillis();
		for (int i=0; i<results.size(); i++)
		{
			ExtMatrixTask matrixTask = (ExtMatrixTask) results.get(i);
			double[][] rows = (double[][]) matrixTask.getResult();
			for (int j=0; j<rows.length; j++)
			{
				for (int k=0; k<size; k++) c.setValueAt(rowIdx + j, k, rows[j][k]);
			}
			rowIdx += rows.length;
		}
		long elapsed2 = System.currentTimeMillis() - start2;
		//output("results processing performed in "+StringUtils.toStringDuration(elapsed2));
		return System.currentTimeMillis() - start;
	}

	/**
	 * Perform the sequential multiplication of 2 squares matrices of equal sizes.
	 * @param a the left-hand matrix.
	 * @param b the right-hand matrix.
	 */
	private static void performSequentialMultiplication(Matrix a, Matrix b)
	{
		long start = System.currentTimeMillis();
		int size = a.getSize();
		Matrix c = new Matrix(size);
		// iterate over the rows of a
		for (int i=0; i<size; i++)
		{
			// iterate over the columns of b
			for (int j=0; j<size; j++)
			{
				double val = 0d;
				for (int k=0; k<size; k++) val += a.getValueAt(i, k) * b.getValueAt(k, j); 
				c.setValueAt(i, j, val);
			}
		}
		long elapsed = System.currentTimeMillis() - start;
		output("Sequential computation performed in "+StringUtils.toStringDuration(elapsed));
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
