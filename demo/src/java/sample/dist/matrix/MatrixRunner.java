/*
 * JPPF.
 * Copyright (C) 2005-2017 JPPF Team.
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
package sample.dist.matrix;

import java.io.File;
import java.util.List;

import org.jppf.JPPFException;
import org.jppf.client.*;
import org.jppf.location.*;
import org.jppf.node.policy.ExecutionPolicy;
import org.jppf.node.protocol.*;
import org.jppf.utils.*;
import org.jppf.utils.configuration.JPPFProperties;
import org.jppf.utils.streams.StreamUtils;
import org.slf4j.*;

/**
 * Runner class for the square matrix multiplication demo.
 * @author Laurent Cohen
 */
public class MatrixRunner {
  /**
   * Logger for this class.
   */
  static Logger log = LoggerFactory.getLogger(MatrixRunner.class);
  /**
   * JPPF client used to submit execution requests.
   */
  private JPPFClient jppfClient = null;
  /**
   * Keeps track of the current iteration number.
   */
  private int iterationsCount = 0;
  /**
   * 
   */
  private ClassPath classpath = null;

  /**
   * Entry point for this class, performs a matrix multiplication a number of times.,<br>
   * The number of times is specified as a configuration property named &quot;matrix.iterations&quot;.<br>
   * The size of the matrices is specified as a configuration property named &quot;matrix.size&quot;.<br>
   * @param args not used.
   */
  public static void main(final String...args) {
    MatrixRunner runner = null;
    try {
      final String clientUuid = ((args != null) && (args.length > 0)) ? args[0] : null;
      final TypedProperties props = JPPFConfiguration.getProperties();
      final int size = props.getInt("matrix.size", 300);
      final int iterations = props.getInt("matrix.iterations", 10);
      int nbChannels = props.getInt("matrix.nbChannels", 1);
      if (nbChannels < 1) nbChannels = 1;
      final int nbRows = props.getInt("task.nbRows", 1);
      StreamUtils.printf(log, "Running Matrix demo with matrix size = "+size+ '*'+size+" for "+iterations+" iterations"  + " with " + nbChannels  + " channels");
      runner = new MatrixRunner();
      runner.perform(size, iterations, nbRows, clientUuid, nbChannels);
    } catch(final Exception e) {
      e.printStackTrace();
    }
  }

  /**
   * Perform the multiplication of 2 matrices with the specified size, for a specified number of times.
   * @param size the size of the matrices.
   * @param iterations the number of times the multiplication will be performed.
   * @param nbRows number of rows of matrix a per task.
   * @param clientUuid an optional uuid to set on the JPPF client.
   * @param nbChannels number of driver channels to use for each job.
   * @throws Exception if an error is raised during the execution.
   */
  public void perform(final int size, final int iterations, final int nbRows, final String clientUuid, final int nbChannels) throws Exception {
    try {
      JPPFConfiguration.set(JPPFProperties.POOL_SIZE, nbChannels);
      final String s = JPPFConfiguration.getProperties().getString("matrix.classpath");
      if (s != null) {
        classpath = new ClassPathImpl();
        final String[] paths = s.split("\\|");
        for (final String path: paths) {
          final String p = path.trim();
          final String name = new File(p).getName();
          final Location<?> fileLoc = new FileLocation(p);
          final Location<?> jar = fileLoc.copyTo(new MemoryLocation((int) fileLoc.size()));
          classpath.add(name, jar);
        }
      }
      if (clientUuid != null) jppfClient = new JPPFClient(clientUuid);
      else jppfClient = new JPPFClient();
      final JPPFConnectionPool pool = jppfClient.awaitWorkingConnectionPool();
      pool.setSize(nbChannels);
      pool.awaitWorkingConnections(Operator.AT_LEAST, nbChannels);
      // initialize the 2 matrices to multiply
      final Matrix a = new Matrix(size).assignRandomValues();
      final Matrix b = new Matrix(size).assignRandomValues();
      if (size <= 500) performSequentialMultiplication(a, b);
      long totalIterationTime = 0L;
      long min = Long.MAX_VALUE;
      long max = 0L;

      // perform "iteration" times
      for (int iter=0; iter<iterations; iter++) {
        final long elapsed = performParallelMultiplication(a, b, nbRows, null, nbChannels);
        if (elapsed < min) min = elapsed;
        if (elapsed > max) max = elapsed;
        totalIterationTime += elapsed;
        StreamUtils.printf(log, "Iteration #" + (iter+1) + " performed in " + StringUtils.toStringDuration(elapsed));
      }
      StreamUtils.printf(log, "Average iteration time: " + StringUtils.toStringDuration(totalIterationTime / iterations) +
          ", min = " + StringUtils.toStringDuration(min) + ", max = " + StringUtils.toStringDuration(max));
    } finally {
      StreamUtils.printf(log, "closing the client");
      if (jppfClient != null) jppfClient.close();
    }
  }

  /**
   * Perform the sequential multiplication of 2 squares matrices of equal sizes.
   * @param a the left-hand matrix.
   * @param b the right-hand matrix.
   * @param nbRows number of rows of matrix a per task.
   * @param policy the execution policy to apply to the submitted job, may be null.
   * @param nbChannels number of driver channels to use for each job.
   * @return the elapsed time for the computation.
   * @throws Exception if an error is raised during the execution.
   */
  private long performParallelMultiplication(final Matrix a, final Matrix b, final int nbRows, final ExecutionPolicy policy, final int nbChannels) throws Exception {
    final long start = System.nanoTime();
    final int size = a.getSize();
    // create a task for each row in matrix a
    final JPPFJob job = new JPPFJob();
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
    job.getDataProvider().setParameter(MatrixTask.DATA_KEY, b);
    job.getSLA().setExecutionPolicy(policy);
    if (classpath != null) job.getSLA().setClassPath(classpath);
    //job.getJobSLA().setMaxNodes(8);
    // submit the tasks for execution
    final List<Task<?>> results = jppfClient.submitJob(job);
    // initialize the resulting matrix
    final Matrix c = new Matrix(size);
    // Get the matrix values from the tasks results
    int rowIdx = 0, pos = 0;
    for (final Task<?> matrixTask : results) {
      if (matrixTask.getThrowable() != null) {
        StreamUtils.printf(log, "got exception: " + ExceptionUtils.getStackTrace(matrixTask.getThrowable()));
        throw new JPPFException(matrixTask.getThrowable());
      }
      if (pos != matrixTask.getPosition()) throw new JPPFException(String.format("pos=%d is different from task.getPosition()=%d", pos, matrixTask.getPosition()));
      final double[][] rows = (double[][]) matrixTask.getResult();
      for (int j = 0; j < rows.length; j++) {
        for (int k = 0; k < size; k++) c.setValueAt(rowIdx + j, k, rows[j][k]);
      }
      rowIdx += rows.length;
      pos++;
    }
    return (System.nanoTime() - start) / 1_000_000L;
  }

  /**
   * Perform the sequential multiplication of 2 squares matrices of equal sizes.
   * @param a the left-hand matrix.
   * @param b the right-hand matrix.
   */
  private static void performSequentialMultiplication(final Matrix a, final Matrix b) {
    final long start = System.nanoTime();
    a.multiply(b);
    final long elapsed = System.nanoTime() - start;
    StreamUtils.printf(log, "Sequential computation performed in " + StringUtils.toStringDuration(elapsed / 1_000_000L));
  }
}
