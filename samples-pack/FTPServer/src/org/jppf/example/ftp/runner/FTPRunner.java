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
package org.jppf.example.ftp.runner;

import java.util.List;

import org.jppf.client.*;
import org.jppf.management.JMXDriverConnectionWrapper;
import org.jppf.node.protocol.Task;
import org.jppf.task.storage.DataProvider;
import org.jppf.task.storage.MemoryMapDataProvider;
import org.jppf.utils.ExceptionUtils;
import org.jppf.utils.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Runner class for the matrix multiplication demo.
 * @author Laurent Cohen
 */
public class FTPRunner
{
  /**
   * Logger for this class.
   */
  static Logger log = LoggerFactory.getLogger(FTPRunner.class);
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
      perform();
    }
    catch(Exception e)
    {
      e.printStackTrace();
    }
    finally
    {
      if (jppfClient != null) jppfClient.close();
    }
  }

  /**
   * Perform the test.
   * @throws Exception if an error is raised during the execution.
   */
  private static void perform() throws Exception
  {
    output("Running FTP demo");
    long totalTime = System.nanoTime();
    JPPFJob job = new JPPFJob();
    job.setName("FTP server example job");
    // fetch the host from the JPPF client, so we don't have to hard-code it in the task.
    JPPFClientConnection c = null;
    while ((c = jppfClient.getClientConnection()) == null) Thread.sleep(10L);
    JMXDriverConnectionWrapper jmx = null;
    while ((jmx = c.getConnectionPool().getJmxConnection()) == null) Thread.sleep(10L);
    while (!jmx.isConnected()) Thread.sleep(10L);
    String host = jmx.getHost();
    // store the host in a data provider
    DataProvider dataProvider = new MemoryMapDataProvider();
    dataProvider.setParameter("ftp.host", host);
    job.setDataProvider(dataProvider);
    // add a single task
    job.add(new FTPTask("input.txt", "output.html"));
    List<Task<?>> results = jppfClient.submitJob(job);
    for (Task<?> t: results)
    {
      if (t.getThrowable() != null) System.out.println("task error: " +  ExceptionUtils.getStackTrace(t.getThrowable()));
      else System.out.println("task result: " + t.getResult());
    }
    totalTime = System.nanoTime() - totalTime;
    output("Computation time: " + StringUtils.toStringDuration(totalTime/1000000L));
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
