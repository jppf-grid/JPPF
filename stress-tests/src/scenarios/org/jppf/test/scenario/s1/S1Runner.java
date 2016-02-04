/*
 * JPPF.
 * Copyright (C) 2005-2016 JPPF Team.
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

package org.jppf.test.scenario.s1;

import java.util.List;

import org.jppf.client.JPPFJob;
import org.jppf.node.protocol.*;
import org.jppf.test.scenario.AbstractScenarioRunner;
import org.jppf.utils.*;
import org.jppf.utils.configuration.JPPFProperties;
import org.slf4j.*;

/**
 * 
 * @author Laurent Cohen
 */
public class S1Runner extends AbstractScenarioRunner {
  /**
   * Logger for this class.
   */
  static Logger log = LoggerFactory.getLogger(S1Runner.class);
  /**
   * The count of itearations runs.
   */
  private int iterationsCount = 0;

  @Override
  public void run() {
    try {
      TypedProperties props = getConfiguration().getProperties();
      int size = props.getInt("matrix.size", 300);
      int iterations = props.getInt("matrix.iterations", 10);
      int nbChannels = props.getInt("matrix.nbChannels", 1);
      if (nbChannels < 1) nbChannels = 1;
      JPPFConfiguration.getProperties().set(JPPFProperties.POOL_SIZE, nbChannels);
      int nbRows = props.getInt("task.nbRows", 1);
      output("performing " + size + 'x' + size + " matrix multiplication for " + iterations + " iterations, using " + nbChannels + " channels");
  
      Matrix a = new Matrix(size);
      a.assignRandomValues();
      Matrix b = new Matrix(size);
      b.assignRandomValues();
      long totalIterationTime = 0L;
      long min = Long.MAX_VALUE;
      long max = 0L;
  
      // perform "iteration" times
      for (int iter=0; iter<iterations; iter++) {
        long elapsed = performParallelMultiplication(a, b, nbRows, nbChannels);
        if (elapsed < min) min = elapsed;
        if (elapsed > max) max = elapsed;
        totalIterationTime += elapsed;
        output("Iteration #" + (iter+1) + " performed in " + StringUtils.toStringDuration(elapsed));
      }
      output("Average iteration time: " + StringUtils.toStringDuration(totalIterationTime / iterations) +
          ", min = " + StringUtils.toStringDuration(min) + ", max = " + StringUtils.toStringDuration(max) + 
          ", total time: " + StringUtils.toStringDuration(totalIterationTime));
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  /**
   * Perform the sequential multiplication of 2 squares matrices of equal sizes.
   * @param a the left-hand matrix.
   * @param b the right-hand matrix.
   * @param nbRows number of rows of matrix a per task.
   * @param nbChannels number of driver channels to use for each job.
   * @return the elapsed time for the computation.
   * @throws Exception if an error is raised during the execution.
   */
  private long performParallelMultiplication(final Matrix a, final Matrix b, final int nbRows, final int nbChannels) throws Exception {
    long start = System.nanoTime();
    int size = a.getSize();
    JPPFJob job = new JPPFJob();
    job.setName("matrix sample " + (iterationsCount++));
    job.getClientSLA().setMaxChannels(nbChannels);
    int remaining = size;
    for (int i=0; i<size; i+= nbRows) {
      double[][] rows = null;
      if (remaining >= nbRows) {
        rows = new double[nbRows][];
        remaining -= nbRows;
      }
      else rows = new double[remaining][];
      for (int j=0; j<rows.length; j++) rows[j] = a.getRow(i + j);
      job.add(new ExtMatrixTask(rows));
    }
    // create a data provider to share matrix b among all tasks
    job.setDataProvider(new MemoryMapDataProvider());
    job.getDataProvider().setParameter(ExtMatrixTask.DATA_KEY, b);
    List<Task<?>> results = getSetup().getClient().submitJob(job);
    Matrix c = new Matrix(size);
    int rowIdx = 0;
    for (Task<?> matrixTask : results) {
      if (matrixTask.getThrowable() != null) throw new Exception(matrixTask.getThrowable());
      double[][] rows = (double[][]) matrixTask.getResult();
      for (int j = 0; j < rows.length; j++) {
        for (int k = 0; k < size; k++) c.setValueAt(rowIdx + j, k, rows[j][k]);
      }
      rowIdx += rows.length;
    }
    long elapsed = System.nanoTime() - start;
    return elapsed/1_000_000L;
  }

  /**
   * Print a message to the console and/or log file.
   * @param message - the message to print.
   */
  private static void output(final String message) {
    System.out.println(message);
    log.info(message);
  }
}
