/*
 * Java Parallel Processing Framework.
 * Copyright (C) 2005-2006 Laurent Cohen.
 * lcohen@osp-chicago.com
 *
 * This library is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as published by the
 * Free Software Foundation; either version 2.1 of the License, or (at your
 * option) any later version.
 *
 * This library is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this library; if not, write to the Free Software Foundation,
 * Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA
 */
package sample.test;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import org.jppf.JPPFException;
import org.jppf.client.JPPFClient;
import org.jppf.server.protocol.JPPFTask;
import org.jppf.task.storage.CompositeDataProvider;

/**
 * Runner class used for testing the framework.
 * @author Laurent Cohen
 */
public class TestTaskRunner
{
	/**
	 * Log4j logger for this class.
	 */
	static Logger log = Logger.getLogger(TestTaskRunner.class);
	/**
	 * JPPF client used to submit execution requests.
	 */
	private static JPPFClient jppfClient = null;

	/**
	 * Entry point for this class, performs a matrix multiplication a number of times.
	 * @param args not used.
	 */
	public static void main(String...args)
	{
		try
		{
			jppfClient = new JPPFClient();
			performEmptyTaskListTest();
			performExceptionTest();
			performURLTest();
			performSecurityTest();
			performEmptyConstantTaskTest();
			performClassNotFoundTaskTest();
			System.exit(0);
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}
	}
	
	/**
	 * Perform the test.
	 * @throws JPPFException if an error is raised during the execution.
	 */
	static void performExceptionTest() throws JPPFException
	{
		System.out.println("Starting exception testing...");
		try
		{
			List<JPPFTask> tasks = new ArrayList<JPPFTask>();
			JPPFTask task = new ExceptionTestTask(2);
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
		System.out.println("Starting constant tasks testing...");
		try
		{
			List<JPPFTask> tasks = new ArrayList<JPPFTask>();
			for (int i=0; i<15; i++) tasks.add(new ConstantTask(i));
			List<JPPFTask> results = jppfClient.submit(tasks, null);
			for (int i=0; i<15; i++)
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
			System.out.println("ClassNotFound task testing complete.");
		}
	}
	
	/**
	 * Return an exception stack trace as a string.
	 * @param t the throwable toget the stack trace from.
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
}
