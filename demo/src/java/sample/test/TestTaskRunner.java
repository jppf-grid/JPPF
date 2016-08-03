/*
 * JPPF.
 * Copyright (C) 2005-2016 JPPF Team.
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
package sample.test;

import java.util.List;

import org.jppf.JPPFException;
import org.jppf.client.*;
import org.jppf.node.protocol.*;
import org.jppf.utils.*;
import org.slf4j.*;

/**
 * Runner class used for testing the framework.
 * @author Laurent Cohen
 */
public class TestTaskRunner
{
  /**
   * Logger for this class.
   */
  static Logger log = LoggerFactory.getLogger(TestTaskRunner.class);
  /**
   * JPPF client used to submit execution requests.
   */
  private static JPPFClient jppfClient = null;
  /**
   * Separator for each test.
   */
  private static String banner = '\n' + StringUtils.padLeft("", '-', 80) + '\n';

  /**
   * Entry point for this class, performs a matrix multiplication a number of times.
   * @param args not used.
   */
  public static void main(final String...args)
  {
    try
    {
      jppfClient = new JPPFClient();
      performNonSerializableAttributeTest();
      /*
			performBasicTest();
			performSecurityTest();
			performEmptyTaskListTest();
			performExceptionTest();
			performURLTest();
			performEmptyConstantTaskTest();
			performClassNotFoundTaskTest();
			performInnerTask();
			performDB2LoadingTaskTest();
			performXMLParsingTaskTest();
			performMyTaskTest();
			performTimeoutTaskTest();
			performAnonymousInnerClassTaskTest();
			performOutOfMemoryTest();
			performLargeDataTest();
			performAnnotatedTaskTest();
       */
    }
    catch(Exception e)
    {
      e.printStackTrace();
    }
    finally
    {
      jppfClient.close();
    }
  }

  /**
   * Perform the test.
   * @throws JPPFException if an error is raised during the execution.
   */
  static void performExceptionTest() throws JPPFException
  {
    System.out.println(banner);
    System.out.println("Starting exception testing...");
    try
    {
      JPPFJob job = new JPPFJob();
      job.add(new ExceptionTestTask());
      List<Task<?>> results = jppfClient.submitJob(job);
      Task<?> resultTask = results.get(0);
      if (resultTask.getThrowable() != null)
      {
        System.out.println("Exception was caught:" + ExceptionUtils.getStackTrace(resultTask.getThrowable()));
      }
    }
    catch(Exception e)
    {
      throw new JPPFException(e);
    }
    finally
    {
      System.out.println("Exception testing complete.");
    }
  }

  /**
   * Test a task that reads a file from an HTTP url and uploads it to an FTP server.
   * @throws JPPFException if an error is raised during the execution.
   */
  static void performURLTest() throws JPPFException
  {
    System.out.println(banner);
    System.out.println("Starting URL testing...");
    try
    {
      JPPFJob job = new JPPFJob();
      job.add(new FileDownloadTestTask("http://www.jppf.org/Options.xsd"));
      List<Task<?>> results = jppfClient.submitJob(job);
      Task<?> resultTask = results.get(0);
      if (resultTask.getThrowable() != null)
      {
        System.out.println("Exception was caught:" + ExceptionUtils.getStackTrace(resultTask.getThrowable()));
      }
      else
      {
        System.out.println("Result is: "+resultTask.getResult());
      }
    }
    catch(Exception e)
    {
      throw new JPPFException(e);
    }
    finally
    {
      System.out.println("URL testing complete.");
    }
  }

  /**
   * Test various permission violations.
   * @throws JPPFException if an error is raised during the execution.
   */
  static void performSecurityTest() throws JPPFException
  {
    System.out.println(banner);
    System.out.println("Starting security testing...");
    try
    {
      JPPFJob job = new JPPFJob();
      job.add(new SecurityTestTask());
      List<Task<?>> results = jppfClient.submitJob(job);
      Task<?> resultTask = results.get(0);
      System.out.println("Result is:\n"+resultTask);
    }
    catch(Exception e)
    {
      throw new JPPFException(e);
    }
    finally
    {
      System.out.println("Security testing complete.");
    }
  }

  /**
   * Test with an empty list of tasks.
   * @throws JPPFException if an error is raised during the execution.
   */
  static void performEmptyTaskListTest() throws JPPFException
  {
    System.out.println(banner);
    System.out.println("Starting empty tasks list testing...");
    try
    {
      jppfClient.submitJob(new JPPFJob());
    }
    catch(Exception e)
    {
      throw new JPPFException(e);
    }
    finally
    {
      System.out.println("Empty tasks list testing complete.");
    }
  }

  /**
   * Check that correct results are returned by the framework.
   * @throws JPPFException if an error is raised during the execution.
   */
  static void performEmptyConstantTaskTest() throws JPPFException
  {
    System.out.println(banner);
    System.out.println("Starting constant tasks testing...");
    try
    {
      int n = 50;
      JPPFJob job = new JPPFJob();
      for (int i=0; i<n; i++) job.add(new ConstantTask(i));
      List<Task<?>> results = jppfClient.submitJob(job);
      for (int i=0; i<n; i++)
      {
        System.out.println("result for task #"+i+" is : "+results.get(i).getResult());
      }
    }
    catch(Exception e)
    {
      throw new JPPFException(e);
    }
    finally
    {
      System.out.println("Constant tasks testing complete.");
    }
  }

  /**
   * Check that correct results are returned by the framework.
   * @throws JPPFException if an error is raised during the execution.
   */
  static void performClassNotFoundTaskTest() throws JPPFException
  {
    System.out.println(banner);
    System.out.println("Starting ClassNotFound task testing...");
    String cp = System.getProperty("java.class.path");
    System.out.println("classpath: "+cp);
    try
    {
      JPPFJob job = new JPPFJob();
      job.add(new ClassNotFoundTestTask());
      List<Task<?>> results = jppfClient.submitJob(job);
      Task<?> resultTask = results.get(0);
      if (resultTask.getThrowable() != null)
      {
        System.out.println("Exception was caught: "+ExceptionUtils.getStackTrace(resultTask.getThrowable()));
      }
      else
      {
        System.out.println("Result is: "+resultTask.getResult());
      }
    }
    catch(Exception e)
    {
      throw new JPPFException(e);
    }
    finally
    {
      System.out.println("ClassNotFound task testing complete.");
    }
  }

  /**
   * Test with a non-static inner task.
   */
  static void performInnerTask()
  {
    System.out.println(banner);
    System.out.println("Starting InnerTask<?> task testing...");
    HelloJPPF h = new HelloJPPF();
    JPPFJob job = new JPPFJob();
    try
    {
      for (int i=1; i<4;i++) job.add(h.new InnerTask(i));
      // execute tasks
      List<Task<?>> results = jppfClient.submitJob(job);
      // show results
      System.out.println("Got "+results.size()+" results: ");
      System.out.println("Result is:");
      for (Task<?> t: results)
      {
        System.out.println(""+t.getResult());
        if  (null != t.getThrowable())
        {
          t.getThrowable().printStackTrace();
        }
      }
    }
    catch (Exception e)
    {
      e.printStackTrace();
    }
    finally
    {
      System.out.println("InnerTask<?> task testing complete.");
    }
  }

  /**
   * Check that correct results are returned by the framework.
   * @throws JPPFException if an error is raised during the execution.
   */
  static void performDB2LoadingTaskTest() throws JPPFException
  {
    int n = 1;
    Object[] tasks = new Object[n];
    for (int i=0; i<n; i++) tasks[i] = new DB2LoadingTask();
    singleTest("DB2 Loading task", null, tasks);
  }

  /**
   * Check that correct results are returned by the framework.
   * @throws JPPFException if an error is raised during the execution.
   */
  static void performXMLParsingTaskTest() throws JPPFException
  {
    int n = 1;
    Object[] tasks = new Object[n];
    for (int i=0; i<n; i++) tasks[i] = new ParserTask("build.xml");
    singleTest("XML parsing task", null, tasks);
  }

  /**
   * Check that correct results are returned by the framework.
   * @throws Exception if an error is raised during the execution.
   */
  static void performMyTaskTest() throws Exception
  {
    DataProvider dataProvider = new MemoryMapDataProvider();
    dataProvider.setParameter("DATA", new SimpleData("Data and more data"));
    JPPFJob job = new JPPFJob();
    job.setDataProvider(dataProvider);
    singleTest("my task", dataProvider, new MyTask());
  }

  /**
   * Check that correct results are returned by the framework.
   * @throws JPPFException if an error is raised during the execution.
   */
  static void performTimeoutTaskTest() throws JPPFException
  {
    singleTest("timeout", null, new TimeoutTask());
  }

  /**
   * Check that an anonymous inner class fails with a NotSerializableException on the client side.
   * @throws JPPFException if an error is raised during the execution.
   */
  static void performAnonymousInnerClassTaskTest() throws JPPFException
  {
    singleTest("anonymous inner class task", null, new AnonymousInnerClassTask());
  }

  /**
   * Check that an anonymous inner class fails with a NotSerializableException on the client side.
   * @throws JPPFException if an error is raised during the execution.
   */
  static void performOutOfMemoryTest() throws JPPFException
  {
    singleTest("OOM", null, new OutOfMemoryTestTask());
  }

  /**
   * Check that an anonymous inner class fails with a NotSerializableException on the client side.
   * @throws Exception if an error is raised during the execution.
   */
  static void performLargeDataTest() throws Exception
  {
    DataProvider dp = new MemoryMapDataProvider();
    byte[] data = new byte[128 * 1024 * 1204];
    dp.setParameter("test", data);
    singleTest("Large Data", dp, new ConstantTask(1));
  }

  /**
   * Test an annotated task.
   * @throws JPPFException if an error is raised during the execution.
   */
  static void performAnnotatedTaskTest() throws JPPFException
  {
    System.out.println(banner);
    System.out.println("Starting annotation testing...");
    try
    {
      JPPFJob job = new JPPFJob();
      job.add(new TestAnnotatedTask(), 11, "test string");
      job.add(TestAnnotatedStaticTask.class, 22, "test string (static method)");
      List<Task<?>> results = jppfClient.submitJob(job);
      Task<?> res = results.get(0);
      if (res.getThrowable() != null) throw res.getThrowable();
      System.out.println("result is : " + res.getResult());
    }
    catch(Throwable e)
    {
      throw new JPPFException(e);
    }
    finally
    {
      System.out.println("annotation testing complete.");
    }
  }

  /**
   * Test an annotated task.
   * @throws Exception if an error is raised during the execution.
   */
  static void performBasicTest() throws Exception
  {
    Task<?>[] tasks = new Task<?>[1];
    for (int i=0; i<1; i++) tasks[i] = new TemplateJPPFTask(i);
    singleTest("basic", null, (Object[]) tasks);
  }

  /**
   * Test with a non-serializable attribute initially null n the task.
   * This test should fail on the node side when sending results back to the server.
   * @throws JPPFException if an error is raised during the execution.
   */
  static void performNonSerializableAttributeTest() throws JPPFException
  {
    singleTest("non-serializable attribute", null, new NonSerializableAttributeTask());
  }

  /**
   * Perform a test with a single JPPF task and a data provider.
   * @param title the title given to the test.
   * @param dp the data provider.
   * @param tasks the task to execute.
   * @throws JPPFException if an error is raised during the execution.
   */
  static void singleTest(final String title, final DataProvider dp, final Object...tasks) throws JPPFException
  {
    System.out.println(banner);
    System.out.println("Starting " + title + " test ...");
    try
    {
      JPPFJob job = new JPPFJob();
      job.setDataProvider(dp);
      for (Object task: tasks) job.add(task);
      List<Task<?>> results = jppfClient.submitJob(job);
      Task<?> res = results.get(0);
      if (res.getThrowable() != null) throw res.getThrowable();
      System.out.println("result is : " + res.getResult());
    }
    catch(Throwable e)
    {
      throw new JPPFException(e);
    }
    finally
    {
      System.out.println(title + " test complete.");
    }
  }
}
