/*
 * JPPF.
 * Copyright (C) 2005-2019 JPPF Team.
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
package sample.datasize;

import org.jppf.client.*;
import org.jppf.utils.*;
import org.slf4j.*;

/**
 * Runner class for the matrix multiplication demo.
 * @author Laurent Cohen
 */
public class DataSizeRunner {
  /**
   * Logger for this class.
   */
  static Logger log = LoggerFactory.getLogger(DataSizeRunner.class);
  /**
   * JPPF client used to submit execution requests.
   */
  private static JPPFClient jppfClient = null;
  /**
   * One kilobyte.
   */
  private static final int KILO = 1024;
  /**
   * One kilobyte.
   */
  private static final int MEGA = 1024 * KILO;

  /**
   * Entry point for this class, performs a matrix multiplication a number of times.,<br>
   * The number of times is specified as a configuration property named &quot;matrix.iterations&quot;.<br>
   * The size of the matrices is specified as a configuration property named &quot;matrix.size&quot;.<br>
   * @param args not used.
   */
  public static void main(final String... args) {
    try {
      jppfClient = new JPPFClient();
      perform();
      //perform2();
      //perform3();
    } catch (final Exception e) {
      e.printStackTrace();
    } finally {
      if (jppfClient != null) jppfClient.close();
    }
  }

  /**
   * Perform the test.
   * @throws Exception if an error is raised during the execution.
   */
  private static void perform() throws Exception {
    final TypedProperties config = JPPFConfiguration.getProperties();
    final boolean inNodeOnly = config.getBoolean("datasize.inNodeOnly", false);
    final int iterations = config.getInt("datasize.iterations", 1);
    int datasize = config.getInt("datasize.size", 1);
    final int nbTasks = config.getInt("datasize.nbTasks", 10);
    final String unit = config.getString("datasize.unit", "b").toLowerCase();
    if ("k".equals(unit)) datasize *= KILO;
    else if ("m".equals(unit)) datasize *= MEGA;

    output("Running datasize demo with data size = " + datasize + " with " + nbTasks + " tasks for " + iterations + " iterations");
    long totalTime = 0;
    for (int i = 1; i <= iterations; i++) {
      final long start = System.nanoTime();
      final JPPFJob job = new JPPFJob();
      job.setName("Datasize job " + i);
      for (int j = 0; j < nbTasks; j++) job.add(new DataTask(datasize, inNodeOnly));
      jppfClient.submit(job);
      /* for (JPPFTask t: results)
       * {
       * if (t.getException() != null) System.out.println("task error: " + t.getException().getMessage());
       * else System.out.println("task result: " + t.getResult());
       * } */
      final long elapsed = System.nanoTime() - start;
      totalTime += elapsed;
      output("iteration " + i + " performed in " + StringUtils.toStringDuration(elapsed / 1000000L));
    }
    output("Computation time: " + StringUtils.toStringDuration(totalTime / 1000000L));
  }

  /**
   * Print a message to the console and/or log file.
   * @param message the message to print.
   */
  private static void output(final String message) {
    System.out.println(message);
    log.info(message);
  }
}
