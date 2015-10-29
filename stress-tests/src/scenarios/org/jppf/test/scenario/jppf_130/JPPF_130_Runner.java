/*
 * JPPF.
 * Copyright (C) 2005-2015 JPPF Team.
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

package org.jppf.test.scenario.jppf_130;

import java.util.List;

import org.jppf.client.*;
import org.jppf.client.event.*;
import org.jppf.node.protocol.Task;
import org.jppf.test.scenario.AbstractScenarioRunner;
import org.jppf.utils.*;
import org.slf4j.*;

/**
 * This scenario reproduces the class loader issue described in
 * bug <a href="http://www.jppf.org/tracker/tbg/jppf/issues/JPPF-130">JPPF-130</a>.
 * @author Laurent Cohen
 */
public class JPPF_130_Runner extends AbstractScenarioRunner
{
  /**
   * Logger for this class.
   */
  private static Logger log = LoggerFactory.getLogger(JPPF_130_Runner.class);

  @Override
  public void run()
  {
    Thread.setDefaultUncaughtExceptionHandler(new JPPFDefaultUncaughtExceptionHandler());
    List<JPPFClientConnection> list = getSetup().getClient().getAllConnections();
    MyConnectionListener listener = new MyConnectionListener();
    try
    {
      TypedProperties config = getConfiguration().getProperties();
      int nbTasks = config.getInt("nbTasks", 1);
      int nbIter = config.getInt("nbJobs", 1);
      int nbLookups = config.getInt("nbLookups", 1);
      print("running demo with " + nbIter + " iterations, " + nbTasks + " tasks per iteration");
      for (JPPFClientConnection c: list) c.addClientConnectionStatusListener(listener);
      for (int n=0; n<nbIter; n++)
      {
        long start = System.nanoTime();
        try
        {
          JPPFJob job = new JPPFJob("job_" + n);
          job.getClientSLA().setMaxChannels(10);
          job.setName("ruleset job_" + n);
          for (int i=0; i<nbTasks; i++) job.add(new JPPF_130_Task(nbLookups));
          job.setBlocking(false);
          getSetup().getClient().submitJob(job);
          List<Task<?>> results = job.awaitResults();
          for (Task<?> task: results)
          {
            Throwable t = task.getThrowable();
            if (t != null)
            {
              if (t instanceof Exception) throw (Exception) t;
              else if (t instanceof Error) throw (Error) t;
              throw new RuntimeException(t);
            }
          }
        }
        finally
        {
          long elapsed = System.nanoTime() - start;
          print("Iteration #" + (n+1) + " performed in " + StringUtils.toStringDuration(elapsed/1000000L));
        }
      }
    }
    catch (Exception e)
    {
      //e.printStackTrace();
      if (e instanceof RuntimeException) throw (RuntimeException) e;
      throw new RuntimeException(e);
    }
    finally
    {
      for (JPPFClientConnection c: list) c.removeClientConnectionStatusListener(listener);
    }
  }

  /**
   * The symptom of the problem is that a class loader client channel gets disconnected.
   * This translates to an invalid status change on the corresponding client connection.
   */
  private class MyConnectionListener implements ClientConnectionStatusListener
  {
    @Override
    public void statusChanged(final ClientConnectionStatusEvent event)
    {
      JPPFClientConnectionStatus status = event.getClientConnectionStatusHandler().getStatus();
      if ((status != JPPFClientConnectionStatus.ACTIVE) && (status != JPPFClientConnectionStatus.EXECUTING))
      {
        try
        {
          getSetup().cleanup();
        }
        catch(Exception e)
        {
          e.printStackTrace();
        }
        finally
        {
          String msg = "\n***** Detected client disconnection - Exiting immediately *****";
          log.error(msg);
          System.err.println(msg);
          // exit immediately to ensure the next cenario iteratrion is not exedcuted,
          // and thus the log files are not overriden.
          System.exit(1);
        }
      }
    }
  }

  /**
   * Print the specified message to both the log and <code>System.out</code>.
   * @param msg the message to rpint.
   */
  private static void print(final String msg)
  {
    System.out.println(msg);
    log.info(msg);
  }
}
