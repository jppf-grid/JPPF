/*
 * Java Parallel Processing Framework.
 *  Copyright (C) 2005-2009 JPPF Team. 
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
package sample.osgi;

import java.io.*;
import java.util.List;

import org.apache.commons.logging.*;
import org.jppf.client.*;
import org.jppf.server.protocol.JPPFTask;

/**
 * Runner class for the &quot;Long Task&quot; demo.
 * @author Laurent Cohen
 */
public class EclipseRunner
{
	/**
	 * Logger for this class.
	 */
	static Log log = LogFactory.getLog(EclipseRunner.class);
	/**
	 * JPPF client used to submit execution requests.
	 */
	private static JPPFClient jppfClient = null;

	/**
	 * Entry point for this class, submits the tasks with a set duration to the server.
	 * @param args not used.
	 */
	public static void main(String...args)
	{
		try
		{
			print("Running Eclipse Task");
			//JPPFTask t = new EclipseTask2();
			JPPFTask t = new JBossTask();
			JPPFJob job = new JPPFJob();
			job.addTask(t);
			jppfClient = new JPPFClient();
			List<JPPFTask> results = jppfClient.submit(job);
			JPPFTask task = results.get(0);
			/*
			t.run();
			JPPFTask task = t;
			*/
			if (task.getException() != null)
			{
				print("Exception occurred:\n");
				print(getStackTrace(task.getException()));
			}
			else
			{
				print("Execution OK, result:\n" + task.getResult());
			}
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
	 * Print a message tot he log and to the console.
	 * @param msg the message to print.
	 */
	private static void print(String msg)
	{
		log.info(msg);
		System.out.println(msg);
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
}
