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
package sample.dist.matrix;

import java.io.File;
import java.util.List;

import org.jppf.JPPFException;
import org.jppf.client.*;
import org.jppf.location.*;
import org.jppf.node.policy.ExecutionPolicy;
import org.jppf.node.protocol.*;
import org.jppf.utils.*;
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
      String clientUuid = ((args != null) && (args.length > 0)) ? args[0] : null;
      TypedProperties props = JPPFConfiguration.getProperties();
      int size = props.getInt("matrix.size", 300);
      int iterations = props.getInt("matrix.iterations", 10);
      int nbChannels = props.getInt("matrix.nbChannels", 1);
      if (nbChannels < 1) nbChannels = 1;
      int nbRows = props.getInt("task.nbRows", 1);
      output("Running Matrix demo with matrix size = "+size+ '*'+size+" for "+iterations+" iterations"  + " with " + nbChannels  + " channels");
      runner = new MatrixRunner();
      runner.perform(size, iterations, nbRows, clientUuid, nbChannels);
      //runner.perform2(size, iterations, nbRows, clientUuid);
      //StreamUtils.waitKeyPressed("***** press {Enter] to exit ...");
    } catch(Exception e) {
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
      JPPFConfiguration.getProperties().setProperty("jppf.pool.size", String.valueOf(nbChannels));
      String s = JPPFConfiguration.getProperties().getString("matrix.classpath");
      if (s != null) {
        classpath = new ClassPathImpl();
        String[] paths = s.split("\\|");
        for (String path: paths) {
          String p = path.trim();
          String name = new File(p).getName();
          Location fileLoc = new FileLocation(p);
          Location jar = fileLoc.copyTo(new MemoryLocation((int) fileLoc.size()));
          classpath.add(name, jar);
        }
      }
      if (clientUuid != null) jppfClient = new JPPFClient(clientUuid);
      else jppfClient = new JPPFClient();
      while (!jppfClient.hasAvailableConnection()) Thread.sleep(1L);

      // initialize the 2 matrices to multiply
      Matrix a = new Matrix(size);
      a.assignRandomValues();
      Matrix b = new Matrix(size);
      b.assignRandomValues();
      if (size <= 500) performSequentialMultiplication(a, b);
      long totalIterationTime = 0L;
      long min = Long.MAX_VALUE;
      long max = 0L;

      // perform "iteration" times
      for (int iter=0; iter<iterations; iter++) {
        long elapsed = performParallelMultiplication(a, b, nbRows, null, nbChannels);
        if (elapsed < min) min = elapsed;
        if (elapsed > max) max = elapsed;
        totalIterationTime += elapsed;
        output("Iteration #" + (iter+1) + " performed in " + StringUtils.toStringDuration(elapsed));
      }
      output("Average iteration time: " + StringUtils.toStringDuration(totalIterationTime / iterations) +
          ", min = " + StringUtils.toStringDuration(min) + ", max = " + StringUtils.toStringDuration(max));
      /*
      JMXDriverConnectionWrapper jmx = jppfClient.getConnectionPool().getJmxConnection();
      String debug = (String) jmx.invoke("org.jppf:name=debug,type=driver", "all");
      output(debug);
      */
    } finally {
      output("closing the client");
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
    long start = System.nanoTime();
    int size = a.getSize();
    // create a task for each row in matrix a
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
    job.getDataProvider().setParameter(MatrixTask.DATA_KEY, b);
    job.getSLA().setExecutionPolicy(policy);
    if (classpath != null) job.getSLA().setClassPath(classpath);
    //job.getJobSLA().setMaxNodes(8);
    // submit the tasks for execution
    List<Task<?>> results = jppfClient.submitJob(job);
    // initialize the resulting matrix
    Matrix c = new Matrix(size);
    // Get the matrix values from the tasks results
    int rowIdx = 0;
    for (Task matrixTask : results) {
      if (matrixTask.getThrowable() != null) {
        output("got exception: " + ExceptionUtils.getStackTrace(matrixTask.getThrowable()));
        throw new JPPFException(matrixTask.getThrowable());
      }
      double[][] rows = (double[][]) matrixTask.getResult();
      for (int j = 0; j < rows.length; j++) {
        for (int k = 0; k < size; k++) c.setValueAt(rowIdx + j, k, rows[j][k]);
      }
      rowIdx += rows.length;
    }
    return (System.nanoTime() - start)/1000000L;
  }

  /**
   * Perform the multiplication of 2 matrices with the specified size, for a specified number of times.
   * Here we create and close a JPPF client for each iteration.
   * @param size the size of the matrices.
   * @param iterations the number of times the multiplication will be performed.
   * @param nbRows number of rows of matrix a per task.
   * @param clientUuid an optional uuid to set on the JPPF client.
   * @param nbChannels number of driver channels to use for each job.
   * @throws Exception if an error is raised during the execution.
   */
  public void perform2(final int size, final int iterations, final int nbRows, final String clientUuid, final int nbChannels) throws Exception {
    try {
      // initialize the 2 matrices to multiply
      Matrix a = new Matrix(size);
      a.assignRandomValues();
      Matrix b = new Matrix(size);
      b.assignRandomValues();
      long totalIterationTime = 0L;

      // perform "iteration" times
      for (int iter=0; iter<iterations; iter++) {
        try {
          if (clientUuid != null) jppfClient = new JPPFClient(clientUuid);
          else jppfClient = new JPPFClient();
          long elapsed = performParallelMultiplication(a, b, nbRows, null, nbChannels);
          totalIterationTime += elapsed;
          output("Iteration #" + (iter+1) + " performed in " + StringUtils.toStringDuration(elapsed));
        } finally {
          jppfClient.close();
        }
      }
      output("Average iteration time: " + StringUtils.toStringDuration(totalIterationTime / iterations));
    } catch(Exception e) {
      throw e;
    }
  }

  /**
   * Perform the sequential multiplication of 2 squares matrices of equal sizes.
   * @param a the left-hand matrix.
   * @param b the right-hand matrix.
   */
  private void performSequentialMultiplication(final Matrix a, final Matrix b) {
    long start = System.nanoTime();
    a.multiply(b);
    long elapsed = System.nanoTime() - start;
    output("Sequential computation performed in "+StringUtils.toStringDuration(elapsed/1000000));
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
