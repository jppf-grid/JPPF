/*
 * Java Parallel Processing Framework.
 * Copyright (C) 2005-2008 JPPF Team.
 * http://www.jppf.org
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * 	 http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package sample.test;

import java.io.*;
import java.util.*;

import org.apache.commons.logging.*;
import org.jppf.JPPFException;
import org.jppf.client.JPPFClient;
import org.jppf.server.protocol.JPPFTask;
import org.jppf.task.storage.*;
import org.jppf.utils.StringUtils;

/**
 * Runner class used for testing the framework.
 * @author Laurent Cohen
 */
public class TestTaskRunner
{
	/**
	 * Logger for this class.
	 */
	static Log log = LogFactory.getLog(TestTaskRunner.class);
	/**
	 * JPPF client used to submit execution requests.
	 */
	private static JPPFClient jppfClient = null;
	/**
	 * Separator for each test.
	 */
	private static String banner = "\n"+StringUtils.padLeft("", '-', 80)+"\n";

	/**
	 * Entry point for this class, performs a matrix multiplication a number of times.
	 * @param args not used.
	 */
	public static void main(String...args)
	{
		try
		{
			jppfClient = new JPPFClient();
			/*
			performEmptyTaskListTest();
			performExceptionTest();
			performURLTest();
			performSecurityTest();
			performEmptyConstantTaskTest();
			performClassNotFoundTaskTest();
			performInnerTask();
			performDB2LoadingTaskTest();
			performXMLParsingTaskTest();
			performMyTaskTest();
			performTimeoutTaskTest();
			performAnonymousInnerClassTaskTest();
			performOutOfMemoryTest();
			*/
			performLargeDataTest();
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
			List<JPPFTask> tasks = new ArrayList<JPPFTask>();
			JPPFTask task = new ExceptionTestTask();
			tasks.add(task);
			List<JPPFTask> results = jppfClient.submit(tasks, null);
			JPPFTask resultTask = results.get(0);
			if (resultTask.getException() != null)
			{
				System.out.println("Exception was caught:"+getStackTrace(resultTask.getException()));
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
			List<JPPFTask> tasks = new ArrayList<JPPFTask>();
			JPPFTask task = new FileDownloadTestTask("http://www.jppf.org/Options.xsd");
			tasks.add(task);
			List<JPPFTask> results = jppfClient.submit(tasks, new CompositeDataProvider());
			JPPFTask resultTask = results.get(0);
			if (resultTask.getException() != null)
			{
				System.out.println("Exception was caught:"+getStackTrace(resultTask.getException()));
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
			List<JPPFTask> tasks = new ArrayList<JPPFTask>();
			JPPFTask task = new SecurityTestTask();
			tasks.add(task);
			List<JPPFTask> results = jppfClient.submit(tasks, new CompositeDataProvider());
			JPPFTask resultTask = results.get(0);
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
			List<JPPFTask> tasks = new ArrayList<JPPFTask>();
			jppfClient.submit(tasks, null);
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
			List<JPPFTask> tasks = new ArrayList<JPPFTask>();
			for (int i=0; i<n; i++) tasks.add(new ConstantTask(i));
			List<JPPFTask> results = jppfClient.submit(tasks, null);
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
			List<JPPFTask> tasks = new ArrayList<JPPFTask>();
			tasks.add(new ClassNotFoundTestTask());
			List<JPPFTask> results = jppfClient.submit(tasks, null);
			JPPFTask resultTask = results.get(0);
			if (resultTask.getException() != null)
			{
				System.out.println("Exception was caught: "+getStackTrace(resultTask.getException()));
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
		System.out.println("Starting InnerTask task testing...");
		HelloJPPF h = new HelloJPPF();
		List<JPPFTask> tasks = new ArrayList<JPPFTask>();
		for (int i=1; i<4;i++)
		{
			tasks.add(h.new InnerTask(i));
		}
		try
		{
			// execute tasks
			List<JPPFTask> results = jppfClient.submit(tasks, null);
			// show results
			System.out.println("Got "+results.size()+" results: ");
			System.out.println("Result is:");
			for (JPPFTask t: results)
			{
				System.out.println(""+t.getResult());
				if  (null != t.getException())
				{
					t.getException().printStackTrace();
				}
			}
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
		finally
		{
			System.out.println("InnerTask task testing complete.");
		}
	}

	/**
	 * Check that correct results are returned by the framework.
	 * @throws JPPFException if an error is raised during the execution.
	 */
	static void performDB2LoadingTaskTest() throws JPPFException
	{
		System.out.println(banner);
		System.out.println("Starting DB2 Loading task testing...");
		try
		{
			int n = 1;
			List<JPPFTask> tasks = new ArrayList<JPPFTask>();
			for (int i=0; i<n; i++) tasks.add(new DB2LoadingTask());
			List<JPPFTask> results = jppfClient.submit(tasks, null);
			JPPFTask resultTask = results.get(0);
			if (resultTask.getException() != null)
			{
				System.out.println("Exception was caught:"+getStackTrace(resultTask.getException()));
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
			System.out.println("DB2 Loading tasks testing complete.");
		}
	}
	
	/**
	 * Check that correct results are returned by the framework.
	 * @throws JPPFException if an error is raised during the execution.
	 */
	static void performXMLParsingTaskTest() throws JPPFException
	{
		System.out.println(banner);
		System.out.println("Starting XML parsing task testing...");
		try
		{
			int n = 1;
			List<JPPFTask> tasks = new ArrayList<JPPFTask>();
			for (int i=0; i<n; i++) tasks.add(new ParserTask("build.xml"));
			List<JPPFTask> results = jppfClient.submit(tasks, null);
			JPPFTask resultTask = results.get(0);
			if (resultTask.getException() != null)
			{
				System.out.println("Exception was caught:"+getStackTrace(resultTask.getException()));
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
			System.out.println("XML parsing task testing complete.");
		}
	}
	
	/**
	 * Check that correct results are returned by the framework.
	 * @throws JPPFException if an error is raised during the execution.
	 */
	static void performMyTaskTest() throws JPPFException
	{
		System.out.println(banner);
		System.out.println("Starting my task testing...");
		try
		{
			List<JPPFTask> tasks = new ArrayList<JPPFTask>();
			tasks.add(new MyTask());
			DataProvider dataProvider = new MemoryMapDataProvider();
			dataProvider.setValue("DATA", new SimpleData("Data and more data"));			
			List<JPPFTask> results = jppfClient.submit(tasks, dataProvider);
			JPPFTask resultTask = results.get(0);
			if (resultTask.getException() != null)
			{
				System.out.println("Exception was caught:"+getStackTrace(resultTask.getException()));
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
			System.out.println("My task testing complete.");
		}
	}
	
	/**
	 * Check that correct results are returned by the framework.
	 * @throws JPPFException if an error is raised during the execution.
	 */
	static void performTimeoutTaskTest() throws JPPFException
	{
		System.out.println(banner);
		System.out.println("Starting timeout testing...");
		try
		{
			List<JPPFTask> tasks = new ArrayList<JPPFTask>();
			tasks.add(new TimeoutTask());
			List<JPPFTask> results = jppfClient.submit(tasks, null);
			JPPFTask resultTask = results.get(0);
			if (resultTask.getException() != null)
			{
				System.out.println("Exception was caught:"+getStackTrace(resultTask.getException()));
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
			System.out.println("Timeout testing complete.");
		}
	}
	
	/**
	 * Return an exception stack trace as a string.
	 * @param t the throwable to get the stack trace from.
	 * @return a string.
	 */
	static String getStackTrace(Throwable t)
	{
		try
		{
			StringWriter sw = new StringWriter();
			PrintWriter writer = new PrintWriter(sw);
			t.printStackTrace(writer);
			return sw.toString();
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}
		return "";
	}

	/**
	 * Check that an anonymous inner class fails with a NotSerializableException on the client side.
	 * @throws JPPFException if an error is raised during the execution.
	 */
	static void performAnonymousInnerClassTaskTest() throws JPPFException
	{
		System.out.println(banner);
		System.out.println("Starting anonymous inner class task testing...");
		try
		{
			int n = 50;
			List<JPPFTask> tasks = new ArrayList<JPPFTask>();
			tasks.add(new AnonymousInnerClassTask());
			List<JPPFTask> results = jppfClient.submit(tasks, null);
			System.out.println("result is : "+results.get(0).getResult());
		}
		catch(Exception e)
		{
			throw new JPPFException(e);
		}
		finally
		{
			System.out.println("Anonymous inner class task testing complete.");
		}
	}

	/**
	 * Check that an anonymous inner class fails with a NotSerializableException on the client side.
	 * @throws JPPFException if an error is raised during the execution.
	 */
	static void performOutOfMemoryTest() throws JPPFException
	{
		System.out.println(banner);
		System.out.println("Starting OOM testing...");
		try
		{
			int n = 50;
			List<JPPFTask> tasks = new ArrayList<JPPFTask>();
			tasks.add(new OutOfMemoryTestTask());
			List<JPPFTask> results = jppfClient.submit(tasks, null);
			JPPFTask res = results.get(0);
			if (res.getException() != null) throw res.getException();
			System.out.println("result is : "+res.getResult());
		}
		catch(Exception e)
		{
			throw new JPPFException(e);
		}
		finally
		{
			System.out.println("OOM testing complete.");
		}
	}

	/**
	 * Check that an anonymous inner class fails with a NotSerializableException on the client side.
	 * @throws JPPFException if an error is raised during the execution.
	 */
	static void performLargeDataTest() throws JPPFException
	{
		System.out.println(banner);
		System.out.println("Starting OOM testing...");
		try
		{
			int n = 50;
			List<JPPFTask> tasks = new ArrayList<JPPFTask>();
			tasks.add(new ConstantTask(1));
			DataProvider dp = new MemoryMapDataProvider();
			byte[] data = new byte[128 * 1024 * 1204];
			dp.setValue("test", data);
			List<JPPFTask> results = jppfClient.submit(tasks, dp);
			JPPFTask res = results.get(0);
			if (res.getException() != null) throw res.getException();
			System.out.println("result is : " + res.getResult());
		}
		catch(Exception e)
		{
			throw new JPPFException(e);
		}
		finally
		{
			System.out.println("OOM testing complete.");
		}
	}
}
