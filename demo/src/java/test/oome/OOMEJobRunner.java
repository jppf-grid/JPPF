/*
 * JPPF.
 * Copyright (C) 2005-2012 JPPF Team.
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
package test.oome;

import java.util.List;

import org.jppf.client.*;
import org.jppf.server.protocol.JPPFTask;
import org.jppf.utils.ExceptionUtils;
import org.slf4j.*;

/**
 * Runner class for the matrix multiplication demo.
 * @author Laurent Cohen
 */
public class OOMEJobRunner
{
  /**
   * Logger for this class.
   */
  static Logger log = LoggerFactory.getLogger(OOMEJobRunner.class);
  /**
   * JPPF client used to submit execution requests.
   */
  private static JPPFClient jppfClient = null;

  /**
   * Entry point for this class.
   * @param args not used.
   */
  public static void main(final String...args)
  {
    try
    {
      jppfClient = new JPPFClient();
      for (int i=1; i<=1; i++) perform(i);
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
   * @param i number of iterations
   * @throws Exception if an error is raised during the execution.
   */
  private static void perform(final int i) throws Exception
  {
    output("Start of iteration " + i);
    long totalTime = System.currentTimeMillis();
    submitJob("OOOME job 1", 1, 0L, true, 0);
    submitJob("OOOME job 2", 1, 0L, true, 0);
    /*
    submitJob("OOOME job " + i + "/1", 200, 0L, true, 200*1024);
    submitJob("OOOME job " + i + "/2",   2, 1L, false, 2*1024*1024);
    */
    totalTime = System.currentTimeMillis() - totalTime;
    //output("Computation time for iteration " + i + ": " + StringUtils.toStringDuration(totalTime));
  }

  /**
   * submit a job.
   * @param name the job name.
   * @param nbTasks number of tasks in the job
   * @param time duration of each tiask in millis.
   * @param blocking whether th job is blocking.
   * @param size the data size for each task.
   * @throws Exception if an error is raised during the execution.
   */
  private static void submitJob(final String name, final int nbTasks, final long time, final boolean blocking, final int size) throws Exception
  {
    JPPFJob job = new JPPFJob();
    job.setName(name);
    for (int j=1; j<=nbTasks; j++) job.addTask(new OOMEJobTask(time, j, size));
    job.setBlocking(blocking);
    if (blocking)
    {
      output("* submitting job '" + job.getName() + "'");
      List<JPPFTask> results = jppfClient.submit(job);
      output("+ got results for job " + job.getName());
      for (JPPFTask t: results)
      {
        if (t.getException() == null) output((String) t.getResult());
        else output(ExceptionUtils.getStackTrace(t.getException()));
      }
    }
    else
    {
      JPPFResultCollector collector = new JPPFResultCollector(job);
      job.setResultListener(collector);
      job.getSLA().setCancelUponClientDisconnect(true);
      jppfClient.submit(job);
      output("job '" + job.getName() + "' submitted");
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
