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
package sample.test.jppfcallable;

import java.util.List;

import org.jppf.client.*;
import org.jppf.server.protocol.JPPFTask;
import org.jppf.utils.JPPFCallable;
import org.slf4j.*;

import test.job.priority.JobPriorityRunner;


/**
 * Runner class for the &quot;Long Task&quot; demo.
 * @author Laurent Cohen
 */
public class JPPFCallableRunner
{
  /**
   * Logger for this class.
   */
  static Logger log = LoggerFactory.getLogger(JobPriorityRunner.class);
  /**
   * JPPF client used to submit execution requests.
   */
  private static JPPFClient jppfClient = null;
  /**
   * Used to test JPPFTask.compute(JPPFCallable) in method {@link #testComputeCallable()}.
   */
  private static String callableResult = "";

  /**
   * Entry point for this class, submits the tasks with a set duration to the server.
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
   * Perform the test using <code>JPPFClient.submit(JPPFJob)</code> to submit the tasks.
   * @throws Exception if an error is raised during the execution.
   */
  private static void perform() throws Exception
  {
    print("submitting job");
    JPPFJob job = new JPPFJob("testing JPPFTask.compute(JPPFCallable)");
    job.addTask(new MyTask());
    callableResult = "from MyCallable";
    List<JPPFTask> results = jppfClient.submit(job);
    print("result : " + results.get(0).getResult());
  }

  /**
   * Print a message tot he log and to the console.
   * @param msg the message to print.
   */
  private static void print(final String msg)
  {
    log.info(msg);
    System.out.println(msg);
  }

  /**
   * 
   */
  public static class MyTask extends JPPFTask
  {
    @Override
    public void run()
    {
      try
      {
        MyCallable mc = new MyCallable();
        String s = compute(mc);
        System.out.println("result of MyCallable.call() = " + s);
        setResult(s);
      }
      catch (Exception e)
      {
        e.printStackTrace();
        setException(e);
      }
    }
  }

  /**
   * 
   */
  public static class MyCallable implements JPPFCallable<String>
  {
    @Override
    public String call() throws Exception
    {
      System.out.println("result of MyCallable.call() = " + callableResult);
      return callableResult;
    }
  }
}
