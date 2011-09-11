/*
 * JPPF.
 * Copyright (C) 2005-2011 JPPF Team.
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
