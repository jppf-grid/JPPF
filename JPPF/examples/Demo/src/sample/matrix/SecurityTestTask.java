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
package sample.matrix;

import java.io.*;
import java.lang.reflect.*;
import java.net.Socket;
import java.util.*;
import org.jppf.server.protocol.JPPFTask;
import org.jppf.utils.JPPFConfiguration;

/**
 * Thnis task is intended for testing the framework only.
 * @author Laurent Cohen
 */
public class SecurityTestTask extends JPPFTask
{
	/**
	 * Holder for the execution results.
	 */
	private List<ExecutionReport> reports = new ArrayList<ExecutionReport>();

	/**
	 * Initialize this task.
	 */
	public SecurityTestTask()
	{
	}
	
	/**
	 * Run the test.
	 * @see java.lang.Runnable#run()
	 */
	public void run()
	{
		System.out.println("Task Executing");
		runTestMethods();
	}

	/**
	 * Execute the test methods and generate an execution report for each. 
	 */
	private void runTestMethods()
	{
		Class thisClass = getClass();
		Method[] methods = thisClass.getMethods();
		for (Method m: methods)
		{
			if (!isTestMethod(m)) continue;
			ExecutionReport report = new ExecutionReport();
			report.methodName = m.getName();
			try
			{
				m.invoke(this, (Object[]) null);
				report.description = "No exception was raised";
			}
			catch(Exception e)
			{
				Throwable t = e.getCause() == null ? e : e.getCause(); 
				report.description = t.getMessage();
				StringWriter sw = new StringWriter();
				PrintWriter writer = new PrintWriter(sw);
				t.printStackTrace(writer);
				report.stackTrace = sw.toString();
			}
			reports.add(report);
		}
	}
	
	/**
	 * Determine whether a method is a test method.
	 * The method must:
	 * <ul>
	 * <li>have a name starting with &quot;test&quot;</li>
	 * <li>be non-static</li>
	 * <li>be public</li>
	 * <li>have a void return type</li>
	 * <li>have no parameters</li>
	 * </ul>
	 * @param m the method to check.
	 * @return true if the method is a test method, false otherwise.
	 */
	private boolean isTestMethod(Method m)
	{
		if (m == null) return false;
		if (!m.getName().startsWith("test")) return false;
		int mod = m.getModifiers();
		if (Modifier.isStatic(mod) || !Modifier.isPublic(mod)) return false;
		if (!Void.TYPE.equals(m.getReturnType())) return false;
		Class[] paramTypes = m.getParameterTypes();
		if ((paramTypes != null) && (paramTypes.length > 0)) return false;
		return true;
	}
	
	/**
	 * Try exiting the JVM through a <code>System.exit(int)</code> call.
	 * @throws SecurityException if the security manager prevents from exiting the JVM.
	 */
	public void testExitVM() throws SecurityException
	{
		System.exit(0);
	}

	/**
	 * Try connecting to a non authorized host through a TCP/IP socket.
	 * @throws SecurityException if the security manager prevents from connecting to the host.
	 */
	public void testConnectForbiddenHost() throws SecurityException
	{
		try
		{
			Socket s = new Socket("www.jppf.org", 8000);
			s.close();
		}
		catch(IOException e)
		{
		}
	}

	/**
	 * Try connecting to a non authorized port on the JPPF server.
	 * @throws SecurityException if the security manager prevents from connecting on the specified port.
	 */
	public void testConnectForbiddenPort() throws SecurityException
	{
		try
		{
			String host = JPPFConfiguration.getProperties().getString("jppf.server.host", "localhost");
			Socket s = new Socket(host, 8000);
			s.close();
		}
		catch(IOException e)
		{
		}
	}

	/**
	 * Try writing a dummy file.
	 * @throws SecurityException if the security manager prevents from writing the file.
	 */
	public void testWriteFile() throws SecurityException
	{
		try
		{
			FileWriter writer = new FileWriter("foo.bar");
			writer.write("Hello");
			writer.close();
		}
		catch(IOException e)
		{
		}
	}

	/**
	 * Try reading a non-authorized file.
	 * @throws SecurityException if the security manager prevents from writing the file.
	 */
	public void testReadFile() throws SecurityException
	{
		try
		{
			File file = new File("/");
			File[] dirList = file.listFiles();
			for (File f: dirList)
			{
				if (!f.isDirectory())
				{
					FileInputStream fis = new FileInputStream(f);
					fis.read();
					fis.close();
					break;
				}
			}
		}
		catch(IOException e)
		{
		}
	}
	
	/**
	 * Holds the result of the execution of a test method.
	 */
	public class ExecutionReport implements Serializable
	{
		/**
		 * Name of the method executed.
		 */
		public String methodName = "";
		/**
		 * Stack trace of the resulting exception, if any.
		 */
		public String stackTrace = "";
		/**
		 * Description of the test.
		 */
		public String description = "";
	}

	/**
	 * Get the holder for the execution results.
	 * @return a list of <code>ExecutionReport</code> instances.
	 */
	public List<ExecutionReport> getReports()
	{
		return reports;
	}
}
