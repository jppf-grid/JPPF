/*
 * JPPF.
 * Copyright (C) 2005-2014 JPPF Team.
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
package org.jppf.example.matrix;

import org.jppf.server.protocol.JPPFTask;

/**
 * This task performs the multiplication of a matrix row by another matrix, as part of
 * the multiplication of 2 whole matrices.
 * @author Laurent Cohen
 */
public class MatrixTask extends JPPFTask
{
  /**
   * Data provider key mapping to the second matrix operand in the multiplication.
   */
  public static final String DATA_KEY = "matrix";
  /**
   * The result of this task's execution, ie a matrix row.
   */
  private double[] result = null;
  /**
   * The row of values to multiply by a matrix.
   */
  private double[] rowValues = null;

  /**
   * Initialize this task with a specified row of values to multiply.
   * @param rowValues the values as an array of <code>double</code> values.
   */
  public MatrixTask(final double[] rowValues)
  {
    this.rowValues = rowValues;
  }

  /**
   * Get the result this task's execution, ie a matrix row.
   * @return a matrix column as an array of <code>double</code> values.
   * @see org.jppf.server.protocol.JPPFTask#getResult()
   */
  @Override
  public Object getResult()
  {
    return result;
  }

  /**
   * Perform the multiplication of a matrix row by another matrix.
   * @see java.lang.Runnable#run()
   */
  @Override
  public void run()
  {
    try
    {
      Matrix matrix = getDataProvider().getParameter(DATA_KEY);
      int size = matrix.getSize();
      result = new double[size];

      for (int col=0; col<size; col++)
      {
        double sum = 0d;
        for (int row=0; row<size; row++)
        {
          sum += matrix.getValueAt(row, col) * rowValues[row];
        }
        result[col] = sum;
      }
    }
    catch(Exception e)
    {
      setThrowable(e);
    }
  }
}
