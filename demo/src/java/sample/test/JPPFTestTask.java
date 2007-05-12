/*
 * Java Parallel Processing Framework.
 * Copyright (C) 2005-2007 JPPF Team.
 * http://www.jppf.org
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

import java.io.*;
import java.lang.reflect.*;
import java.util.*;

import org.jppf.JPPFException;
import org.jppf.server.protocol.JPPFTask;

/**
 * This task is intended for testing the framework only.
 * @author Laurent Cohen
 */
public abstract class JPPFTestTask extends JPPFTask
{
	/**
	 * Holder for the execution results.
	 */
	protected List<ExecutionReport> reports = new ArrayList<ExecutionReport>();

	/**
	 * Initialize this task.
	 */
	public JPPFTestTask()
	{
	}

	/**
	 * Run the test.
	 * @see java.lang.Runnable#run()
	 */
	public void run()
	{
		runTestMethods();
	}

	/**
	 * Execute the test methods and generate an execution report for each. 
	 */
	protected void runTestMethods()
	{
		Class<?> thisClass = getClass();
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
			catch(Throwable e)
			{
				Throwable t = e.getCause() == null ? e : e.getCause(); 
				report.description = t.getMessage();
				StringWriter sw = new StringWriter();
				PrintWriter writer = new PrintWriter(sw);
				t.printStackTrace(writer);
				report.stackTrace = sw.toString();
				setException((t instanceof Exception) ? (Exception) t : new JPPFException(t));
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
	protected boolean isTestMethod(Method m)
	{
		if (m == null) return false;
		if (!m.getName().startsWith("test")) return false;
		int mod = m.getModifiers();
		if (Modifier.isStatic(mod) || !Modifier.isPublic(mod)) return false;
		if (!Void.TYPE.equals(m.getReturnType())) return false;
		Class<?>[] paramTypes = m.getParameterTypes();
		if ((paramTypes != null) && (paramTypes.length > 0)) return false;
		return true;
	}

	/**
	 * Get the holder for the execution results.
	 * @return a list of <code>ExecutionReport</code> instances.
	 */
	public List<ExecutionReport> getReports()
	{
		return reports;
	}

	/**
	 * Get a string representation of this task.
	 * @return a string describing the task execution result.
	 * @see java.lang.Object#toString()
	 */
	public String toString()
	{
		StringBuilder sb = new StringBuilder();
		for (ExecutionReport r: reports)
		{
			sb.append("----------------------------------------------------------\n");
			sb.append("method name: ").append(r.methodName).append("\n");
			sb.append("description: ").append(r.description).append("\n");
			sb.append("stack trace:\n").append(r.stackTrace).append("\n");
		}
		return sb.toString();
	}
}
