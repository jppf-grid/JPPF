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
package sample.prime;

import java.util.List;

import org.jppf.JPPFException;
import org.jppf.client.*;
import org.jppf.node.protocol.Task;
import org.jppf.utils.*;
import org.jppf.utils.stats.JPPFStatistics;
import org.slf4j.*;

/**
 * Runner class for the matrix multiplication demo.
 * @author Laurent Cohen
 */
public class PrimeRunner
{
  /**
   * Logger for this class.
   */
  static Logger log = LoggerFactory.getLogger(PrimeRunner.class);
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
      if ((args != null) && (args.length > 0)) jppfClient = new JPPFClient(args[0]);
      else jppfClient = new JPPFClient();
      perform();
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
   * @throws JPPFException if an error is raised during the execution.
   */
  private static void perform() throws JPPFException
  {
    TypedProperties props = JPPFConfiguration.getProperties();
    int limit = props.getInt("prime.limit", 10000);
    int batchSize = props.getInt("prime.batch.size", 100);
    int count = props.getInt("prime.start", 10000);
    output("Running Mersenne prime demo with limit = " + limit + " with a batch size of " + batchSize + " tasks, starting with an exponent of " + count);
    try
    {
      long totalTime = System.currentTimeMillis();
      int pending = limit;
      //int count = 3;
      //count = 33222592; // about 10 million digits
      while (pending > 0)
      {
        JPPFJob job = new JPPFJob();
        int nbTasks = (pending > batchSize) ? batchSize : pending;
        for (int i=0; i<nbTasks; i++)
        {
          job.add(new PrimeTask(count++));
        }
        pending -= nbTasks;
        List<Task<?>> results = jppfClient.submitJob(job);
        for (Task t: results)
        {
          if (t.getResult() != null)
          {
            output("Found Mersenne prime for exponent = " + t.getResult());
          }
        }
      }
      totalTime = System.currentTimeMillis() - totalTime;
      output("Computation time: " + StringUtils.toStringDuration(totalTime));
      JPPFStatistics stats = jppfClient.getClientConnection().getConnectionPool().getJmxConnection().statistics();
      output("End statistics :\n"+stats.toString());
    }
    catch(Exception e)
    {
      throw new JPPFException(e.getMessage(), e);
    }
  }

  /**
   * Print a message to the console and/or log file.
   * @param message the message to print.
   */
  private static void output(final String message)
  {
    System.out.println(message);
    log.info(message);
  }
}
