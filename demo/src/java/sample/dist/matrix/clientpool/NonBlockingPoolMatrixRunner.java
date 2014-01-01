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
package sample.dist.matrix.clientpool;

import java.util.*;

import org.jppf.JPPFException;
import org.jppf.client.*;
import org.jppf.node.protocol.Task;
import org.jppf.task.storage.*;
import org.jppf.utils.*;
import org.jppf.utils.stats.JPPFStatistics;
import org.slf4j.*;

import sample.dist.matrix.*;

/**
 * Runner class for the matrix multiplication demo.
 * @author Laurent Cohen
 */
public class NonBlockingPoolMatrixRunner
{
  /**
   * Logger for this class.
   */
  static Logger log = LoggerFactory.getLogger(NonBlockingPoolMatrixRunner.class);
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
  public static void main(final String...args)
  {
    try
    {
      jppfClient = new JPPFClient();
      TypedProperties props = JPPFConfiguration.getProperties();
      int size = props.getInt("matrix.size", 300);
      int iterations = props.getInt("matrix.iterations", 10);
      int nbSubmissions = props.getInt("nb.submissions", 5);
      System.out.println("Running Matrix demo with matrix size = "+size+ '*' + size + " for " + iterations + " iterations");
      NonBlockingPoolMatrixRunner runner = new NonBlockingPoolMatrixRunner();
      runner.perform(size, iterations, nbSubmissions);
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
   * @param nbSubmissions the number of concurrent task submissions for each iteration.
   * @throws JPPFException if an error is raised during the execution.
   */
  public void perform(final int size, final int iterations, final int nbSubmissions) throws JPPFException
  {
    try
    {
      // initialize the 2 matrices to multiply
      Matrix a = new Matrix(size);
      a.assignRandomValues();
      Matrix b = new Matrix(size);
      b.assignRandomValues();

      // perform "iteration" times
      long totalTime = 0L;
      for (int iter=0; iter<iterations; iter++)
      {
        long start = System.currentTimeMillis();
        // create a data provider to share matrix b among all tasks
        DataProvider dataProvider = new MemoryMapDataProvider();
        dataProvider.setParameter(MatrixTask.DATA_KEY, b);
        List<JPPFJob> submissions = new ArrayList<>();
        for (int n=0; n<nbSubmissions; n++)
        {
          JPPFJob job = new JPPFJob(dataProvider);
          for (int i=0; i<size; i++) job.add(new MatrixTask(a.getRow(i)));
          job.setBlocking(false);
          job.setResultListener(new JPPFResultCollector(job));
          // create a task for each row in matrix a
          submissions.add(job);
        }
        // submit the tasks for execution
        for (JPPFJob job: submissions) jppfClient.submitJob(job);

        for (JPPFJob job: submissions)
        {
          JPPFResultCollector collector = (JPPFResultCollector) job.getResultListener();
          List<Task<?>> results = collector.awaitResults();
          // initialize the resulting matrix
          Matrix c = new Matrix(size);
          // Get the matrix values from the tasks results
          for (int i=0; i<results.size(); i++)
          {
            MatrixTask matrixTask = (MatrixTask) results.get(i);
            double[] row = (double[]) matrixTask.getResult();
            for (int j=0; j<row.length; j++) c.setValueAt(i, j, row[j]);
          }
        }
        long elapsed = System.currentTimeMillis() - start;
        totalTime += elapsed;
        System.out.println("Iteration #"+(iter+1)+" performed in "+StringUtils.toStringDuration(elapsed));
      }
      System.out.println("Average iteration time: " + StringUtils.toStringDuration(totalTime/iterations));
      JPPFStatistics stats = ((JPPFClientConnectionImpl) jppfClient.getClientConnection()).getJmxConnection().statistics();
      if (stats != null) System.out.println("End statistics :\n"+stats.toString());
    }
    catch(Exception e)
    {
      throw new JPPFException(e.getMessage(), e);
    }
  }
}
