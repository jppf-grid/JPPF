/*
 * Java Parallel Processing Framework.
 * Copyright (C) 2005-2006 Laurent Cohen.
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

import org.jppf.server.protocol.JPPFTask;

/**
 * This task performs the multiplication of a matrix row by another matrix, as part of
 * the multiplication of 2 whole matrices.
 * @author Laurent Cohen
 */
public class LongMatrixTask extends JPPFTask
{
	/**
	 * Data provider key mapping to the second matrix operand in the multiplication.
	 */
	public static final String DATA_KEY = "matrix";
	/**
	 * The result of this task's execution, ie a matrix row.
	 */
	private long[] result = null;
	/**
	 * The row of values to multiply by a matrix.
	 */
	private long[] rowValues = null;

	/**
	 * Initialize this task with a specified row of values to multiply.
	 * @param rowValues the values as an array of <code>double</code> values.
	 */
	public LongMatrixTask(long[] rowValues)
	{
		this.rowValues = rowValues;
	}
	
	/**
	 * Get the result this task's execution, ie a matrix row.
	 * @return a matrix column as an array of <code>double</code> values.
	 * @see org.jppf.task.Task#getResult()
	 */
	public long[] getResult()
	{
		return result;
	}

	/**
	 * Perform the multiplication of a matrix row by another matrix.
	 * @see java.lang.Runnable#run()
	 */
	public void run()
	{
		LongTypeMatrix matrix = (LongTypeMatrix) getDataProvider().getValue(DATA_KEY);
		int size = matrix.getSize();
		result = new long[size];

		for (int col=0; col<size; col++)
		{
			long sum = 0;
			for (int row=0; row<size; row++)
			{
				sum += matrix.getValueAt(row, col) * rowValues[row];
			}
			result[col] = sum;
		}
	}
}
