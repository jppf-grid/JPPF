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
package sample.test.profiling;

import java.util.*;

import org.jppf.client.*;
import org.jppf.server.protocol.JPPFTask;
import org.jppf.utils.*;
import org.slf4j.*;

/**
 * Runner class for the &quot;Long Task&quot; demo.
 * @author Laurent Cohen
 */
public class ProfilingRunner
{
	/**
	 * Logger for this class.
	 */
	static Logger log = LoggerFactory.getLogger(ProfilingRunner.class);
	/**
	 * JPPF client used to submit execution requests.
	 */
	private static JPPFClient jppfClient = null;
	/**
	 * Size of the data in each task, in KB.
	 */
	private static int dataSize = JPPFConfiguration.getProperties().getInt("profiling.data.size");

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
			int nbTask = props.getInt("profiling.nbTasks");
			int iterations = props.getInt("profiling.iterations");
			System.out.println("Running with " + nbTask + " tasks of size=" + dataSize + " for " + iterations + " iterations");
			performSequential(nbTask, true);
			performSequential(nbTask, false);
			perform(nbTask, iterations);
			System.exit(0);
		}
		catch(Exception e)
		{
			e.printStackTrace();
			System.exit(1);
		}
	}
	
	/**
	 * Execute the specified number of tasks for the specified number of iterations.
	 * @param nbTask the number of tasks to send at each iteration.
	 * @param iterations the number of times the the tasks will be sent.
	 * @throws Exception if an error is raised during the execution.
	 */
	private static void perform(int nbTask, int iterations) throws Exception
	{
		// perform "iteration" times
		for (int iter=0; iter<iterations; iter++)
		{
			long start = System.currentTimeMillis();
			JPPFJob job = new JPPFJob();
			for (int i=0; i<nbTask; i++) job.addTask(new EmptyTask(dataSize));
			// submit the tasks for execution
			List<JPPFTask> results = jppfClient.submit(job);
			long elapsed = System.currentTimeMillis() - start;
			System.out.println("Iteration #"+(iter+1)+" performed in "+StringUtils.toStringDuration(elapsed));
		}
		/*
		JPPFStats stats = jppfClient.requestStatistics();
		System.out.println("End statistics :\n"+stats.toString());
		*/
	}

	/**
	 * Execute the specified number of tasks for the specified number of iterations.
	 * @param nbTask the number of tasks to send at each iteration.
	 * @param silent determines whether resuls should be displayed on the console.
	 * @throws Exception if an error is raised during the execution.
	 */
	private static void performSequential(int nbTask, boolean silent) throws Exception
	{
		long start = System.currentTimeMillis();
		List<JPPFTask> tasks = new ArrayList<JPPFTask>();
		for (int i=0; i<nbTask; i++) tasks.add(new EmptyTask(dataSize));
		// submit the tasks for execution
		for (JPPFTask task: tasks) task.run();
		long elapsed = System.currentTimeMillis() - start;
		if (!silent)
			System.out.println("Sequential iteration performed in "+StringUtils.toStringDuration(elapsed));
	}
}
