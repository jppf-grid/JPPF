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

import com.amd.aparapi.Kernel;

/**
 * Kernel implementation which performs the matrix multiplication on the GPU.
 */
public class MatrixKernel extends Kernel {
  /**
   * The first matrix of the multiplication (1st operand).
   */
  private float[] kernelMatrixA;
  /**
   * The second matrix of the multiplication (2nd operand).
   */
  private float[] kernelMatrixB;
  /**
   * The resulting matrix after multiplication.
   */
  private float[] kernelResults;
  /**
   * The size of the matrices.
   */
  private int size = 0;

  /**
   * Initialize this kernel.
   * @param _matrixA the first matrix of the multiplication (1st operand).
   * @param _matrixB the second matrix of the multiplication (2nd operand).
   * @param _size the size of the resulting matrix.
   */
  public MatrixKernel(final float[] _matrixA, final float[] _matrixB, final int _size) {
    kernelMatrixA = _matrixA;
    kernelMatrixB = _matrixB;
    size = _size;
    kernelResults = new float[size*size];
  }

  @Override
  public void run() {
    int rowA = getGlobalId();
    // the loop performs the multiplication of each row of matrix A by the entire matrix B.
    // this is the part of the computation that is executed in parallel.
    for (int colB=0; colB<size; colB++) multiply(rowA, colB);
  }

  /**
   * Multiply a row of matrix A by a column of matrix B.
   * @param rowA the row of matrix A to mulitply.
   * @param columnB the column of matrix B to multiply by.
   */
  public void multiply(final int rowA, final int columnB) {
    float sum = 0f;
    for (int i=0; i<size; i++) {
      sum += kernelMatrixA[rowA * size + i] * kernelMatrixB[i * size + columnB];
    }
    kernelResults[columnB * size + rowA] = sum;
  }

  /**
   * Get the results of the computation.
   * @return an array of flot representing the result of the matrix multiplication.
   */
  public float[] getKernelResults() {
    return kernelResults;
  }
}
