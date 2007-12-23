/*
 * Java Parallel Processing Framework.
 * Copyright (C) 2005-2007 JPPF Team.
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
package sample.tasklength;

import java.util.*;

import org.apache.commons.logging.*;
import org.jppf.JPPFException;
import org.jppf.client.JPPFClient;
import org.jppf.server.JPPFStats;
import org.jppf.server.protocol.JPPFTask;
import org.jppf.utils.*;

/**
 * Runner class for the &quot;Long Task&quot; demo.
 * @author Laurent Cohen
 */
public class LongTaskRunner
{
	/**
	 * Log4j logger for this class.
	 */
	static Log log = LogFactory.getLog(LongTaskRunner.class);
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
			jppfClient = new JPPFClient();
			TypedProperties props = JPPFConfiguration.getProperties();
			int length = props.getInt("longtask.length");
			int nbTask = props.getInt("longtask.number");
			int iterations = props.getInt("longtask.iterations");
			System.out.println("Running Long Task demo with "+nbTask+" tasks of length = "+length+" ms for "+iterations+" iterations");
			perform(nbTask, length, iterations);
			//performLong(size, iterations);
			System.exit(0);
		}
		catch(Exception e)
		{
			e.printStackTrace();
			System.exit(1);
		}
	}
	
	/**
	 * Perform the multiplication of 2 matrices with the specified size, for a specified number of times.
	 * @param nbTask the number of tasks to send at each iteration.
	 * @param length the executionlength of each task.
	 * @param iterations the number of times the the tasks will be sent.
	 * @throws JPPFException if an error is raised during the execution.
	 */
	@SuppressWarnings("unused")
	private static void perform(int nbTask, int length, int iterations) throws JPPFException
	{
		try
		{
			// perform "iteration" times
			long totalTime = 0L;
			for (int iter=0; iter<iterations; iter++)
			{
				long start = System.currentTimeMillis();
				// create a task for each row in matrix a
				List<JPPFTask> tasks = new ArrayList<JPPFTask>();
				for (int i=0; i<nbTask; i++) tasks.add(new LongTask(length));
				// submit the tasks for execution
				List<JPPFTask> results = jppfClient.submit(tasks, null);
				for (JPPFTask task: results)
				{
					Exception e = task.getException();
					if (e != null) throw e;
				}
				long elapsed = System.currentTimeMillis() - start;
				System.out.println("Iteration #"+(iter+1)+" performed in "+StringUtils.toStringDuration(elapsed));
				totalTime += elapsed;
			}
			System.out.println("Average iteration time: "+StringUtils.toStringDuration(totalTime/iterations));
			JPPFStats stats = jppfClient.requestStatistics();
			System.out.println("End statistics :\n"+stats.toString());
		}
		catch(Exception e)
		{
			throw new JPPFException(e.getMessage(), e);
		}
	}

}
