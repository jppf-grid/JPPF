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
package test.annotated;

import java.io.File;
import java.net.*;
import java.util.*;

import org.jppf.client.*;
import org.jppf.node.protocol.Task;
import org.jppf.utils.StringUtils;
import org.slf4j.*;

/**
 * Runner class for the matrix multiplication demo.
 * @author Laurent Cohen
 */
public class AnnotatedRunner {
  /**
   * Logger for this class.
   */
  static Logger log = LoggerFactory.getLogger(AnnotatedRunner.class);
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
  public static void main(final String... args) {
    try {
      jppfClient = new JPPFClient();
      perform2();
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
  @SuppressWarnings("unused")
  private static void perform() throws Exception {
    final int nbJobs = 50;
    final long time = 0L;

    output("Running demo with time = " + time + " for " + nbJobs + " jobs");
    long totalTime = System.nanoTime();
    final List<JPPFJob> jobs = new ArrayList<>();
    for (int i = 0; i < nbJobs; i++) {
      final JPPFJob job = new JPPFJob();
      job.setName("demo job " + (i + 1));
      job.add(new AnnotatedTask(time, (i + 1)));
      jobs.add(job);
      jppfClient.submitAsync(job);
    }
    for (final JPPFJob job: jobs) {
      final List<Task<?>> results = job.awaitResults();
      final Task<?> t = results.get(0);
      output((String) t.getResult());
    }
    totalTime = (System.nanoTime() - totalTime) / 1_000_000L;
    output("Computation time: " + StringUtils.toStringDuration(totalTime));
  }

  /**
   * Perform the test.
   * @throws Exception if an error is raised during the execution.
   */
  private static void perform2() throws Exception {
    final int nbJobs = 1;
    final long time = 0L;

    output("Running demo with time = " + time + " for " + nbJobs + " jobs");
    final File file = new File("../jppftest/bin");
    final URL url = file.toURI().toURL();
    final URLClassLoader cl = new URLClassLoader(new URL[] { url }, AnnotatedRunner.class.getClassLoader());
    Thread.currentThread().setContextClassLoader(cl);
    long totalTime = System.nanoTime();
    final List<JPPFJob> jobs = new ArrayList<>();
    for (int i = 0; i < nbJobs; i++) {
      final JPPFJob job = new JPPFJob();
      job.setName("demo job " + (i + 1));
      final Task<?> task = (Task<?>) cl.loadClass("test.TestClass").newInstance();
      job.add(task);
      jobs.add(job);
      jppfClient.submitAsync(job);
    }
    for (final JPPFJob job: jobs) {
      final List<Task<?>> results = job.awaitResults();
      final Task<?> t = results.get(0);
      output((String) t.getResult());
    }
    totalTime = (System.nanoTime() - totalTime) / 1_000_000L;
    output("Computation time: " + StringUtils.toStringDuration(totalTime));
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
