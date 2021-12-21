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
package sample.test.profiling;

import java.util.*;

import org.jppf.client.*;
import org.jppf.node.protocol.Task;
import org.jppf.utils.*;
import org.jppf.utils.configuration.JPPFProperties;
import org.slf4j.*;

/**
 * Runner class for the &quot;Long Task&quot; demo.
 * @author Laurent Cohen
 */
public class ProfilingRunner {
  /**
   * Logger for this class.
   */
  static Logger log = LoggerFactory.getLogger(ProfilingRunner.class);
  /**
   * JPPF client used to submit execution requests.
   */
  private static JPPFClient jppfClient = null;
  /**
   * Size of the data in each task, in KB.
   */
  private static int dataSize = JPPFConfiguration.getProperties().getInt("profiling.data.size");

  /**
   * Entry point for this class, submits the tasks with a set duration to the server.
   * @param args not used.
   */
  public static void main(final String... args) {
    try {
      final TypedProperties config = JPPFConfiguration.getProperties();
      final int nbTask = config.getInt("profiling.nbTasks");
      final int iterations = config.getInt("profiling.iterations");
      int nbChannels = config.getInt("profiling.channels");
      if (nbChannels < 1) nbChannels = 1;
      config.set(JPPFProperties.POOL_SIZE, nbChannels);
      jppfClient = new JPPFClient();
      System.out.println("Running with " + nbTask + " tasks of size=" + dataSize + " for " + iterations + " iterations, nb channels = " + nbChannels);
      //performSequential(nbTask, true);
      //performSequential(nbTask, false);
      perform(nbTask, iterations);
      //StreamUtils.waitKeyPressed();
    } catch (final Exception e) {
      e.printStackTrace();
    } finally {
      if (jppfClient != null) jppfClient.close();
    }
  }

  /**
   * Execute the specified number of tasks for the specified number of iterations.
   * @param nbTask the number of tasks to send at each iteration.
   * @param iterations the number of times the the tasks will be sent.
   * @throws Exception if an error is raised during the execution.
   */
  private static void perform(final int nbTask, final int iterations) throws Exception {
    for (int iter = 1; iter <= iterations; iter++) {
      final long start = System.nanoTime();
      final JPPFJob job = new JPPFJob();
      job.setName("profiling-" + iter);
      for (int i = 0; i < nbTask; i++) job.add(new EmptyTask(dataSize));
      jppfClient.submit(job);
      final long elapsed = System.nanoTime() - start;
      System.out.println("Iteration #" + iter + " performed in " + StringUtils.toStringDuration(elapsed / 1000000));
    }
  }

  /**
   * Execute the specified number of tasks for the specified number of iterations.
   * @param nbTask the number of tasks to send at each iteration.
   * @param silent determines whether results should be displayed on the console.
   * @throws Exception if an error is raised during the execution.
   */
  @SuppressWarnings("unused")
  private static void performSequential(final int nbTask, final boolean silent) throws Exception {
    final long start = System.nanoTime();
    final List<Task<?>> tasks = new ArrayList<>();
    for (int i = 0; i < nbTask; i++) tasks.add(new EmptyTask(dataSize));
    for (Task<?> task : tasks) task.run();
    final long elapsed = System.nanoTime() - start;
    if (!silent) System.out.println("Sequential iteration performed in " + StringUtils.toStringDuration(elapsed / 1000000));
  }
}
