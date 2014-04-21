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
package org.jppf.example.aparapi;

import java.io.Serializable;
import java.util.Random;

/**
 * This class represents a square matrix of arbitrary size.
 * The values are stored in a one-dimensional float array of length <code>size*size</code>.
 * @author Laurent Cohen
 */
public class SquareMatrix implements Serializable
{
  /**
   * Explicit serialVersionUID.
   */
  private static final long serialVersionUID = 1L;
  /**
   * The range of values for random values.
   */
  private static final float RANDOM_RANGE = 1e6f;
  /**
   * The size of this matrix. The matrix contains size*size values.
   */
  private int size;
  /**
   * The values in this matrix.
   */
  private float[] values;

  /**
   * Initialize this matrix with a specified size.
   * @param size the size of this matrix.
   */
  public SquareMatrix(final int size)
  {
    this.size = size;
    values = new float[size*size];
  }

  /**
   * Initialize this matrix from an array of flot values.
   * @param values the values of the matrix.
   */
  public SquareMatrix(final float[] values)
  {
    if ((values == null) || (values.length < 1)) throw new IllegalArgumentException("matrix values must be a non empty float[]");
    int newSize = (int) (Math.sqrt(values.length));
    if (newSize * newSize != values.length)  throw new IllegalArgumentException("not a square matrix");
    this.size = newSize;
    this.values = values;
  }

  /**
   * Initialize this matrix with random values.
   */
  public void assignRandomValues()
  {
    Random rand = new Random(System.nanoTime());
    for (int i=0; i<values.length; i++)
    {
      // values in ]-RANDOM_RANGE, +RANDOM_RANGE[
      values[i] = RANDOM_RANGE * (2f * rand.nextFloat() - 1f);
    }
  }

  /**
   * Get the size of this matrix.
   * @return the size as an integer value.
   */
  public int getSize()
  {
    return size;
  }

  /**
   * Get a value at the specified coordinates.
   * @param row the row coordinate.
   * @param column the column coordinate.
   * @return the specified value as a float.
   */
  public float getValueAt(final int row, final int column)
  {
    return values[row*size + column];
  }

  /**
   * Set a value to the specified coordinates.
   * @param row the row coordinate.
   * @param column the column coordinate.
   * @param value the value to set.
   */
  public void setValueAt(final int row, final int column, final float value)
  {
    values[row*size + column] = value;
  }

  /**
   * Compute the result of multiplying this matrix by another: thisMatrix x otherMatrix.
   * @param otherMatrix the matrix to multiply this one by.
   * @return a new matrix containing the result of the multiplication.
   */
  public SquareMatrix multiply(final SquareMatrix otherMatrix)
  {
    if (otherMatrix.getSize() != size) return null;
    SquareMatrix result = new SquareMatrix(size);
    for (int i=0; i<size; i++)
    {
      for (int j=0; j<size; j++)
      {
        float value = 0f;
        for (int k=0; k< size; k++) value += otherMatrix.getValueAt(k, j) * getValueAt(i, k);
        result.setValueAt(j, i, value);
      }
    }
    return result;
  }

  /**
   * Multiply a row of this matrix by another matrix.
   * The result is a row in the resulting matrix multiplication.
   * @param n the index of the row in this matrix.
   * @param matrix the matrix to multiply by.
   * @return a new row represented as an array of <code>float</code> values.
   */
  public float[] multiplyRow(final int n, final SquareMatrix matrix)
  {
    float[] result = new float[size];
    for (int col=0; col<size; col++)
    {
      float sum = 0f;
      for (int row=0; row<size; row++)
      {
        sum += matrix.getValueAt(row, col) * getValueAt(n, row);
      }
      result[col] = sum;
    }
    return result;
  }

  /**
   * Get the values.
   * @return an array of float.
   */
  public float[] getValues()
  {
    return values;
  }
}
