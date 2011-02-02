/*
 * JPPF.
 * Copyright (C) 2005-2010 JPPF Team.
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
package sample.datasize;

import java.util.*;
import java.util.concurrent.*;

import org.apache.commons.logging.*;
import org.jppf.client.*;
import org.jppf.client.concurrent.JPPFExecutorService;
import org.jppf.server.protocol.JPPFTask;
import org.jppf.utils.*;

/**
 * Runner class for the matrix multiplication demo.
 * @author Laurent Cohen
 */
public class DataSizeRunner
{
	/**
	 * Logger for this class.
	 */
	static Log log = LogFactory.getLog(DataSizeRunner.class);
	/**
	 * JPPF client used to submit execution requests.
	 */
	private static JPPFClient jppfClient = null;
	/**
	 * One kilobyte.
	 */
	private static final int KILO = 1024;
	/**
	 * One megabyte.
	 */
	private static final int MEGA = 1024 * KILO;

	/**
	 * Entry point for this class, performs a matrix multiplication a number of times.,<br>
	 * The number of times is specified as a configuration property named &quot;matrix.iterations&quot;.<br>
	 * The size of the matrices is specified as a configuration property named &quot;matrix.size&quot;.<br>
	 * @param args not used.
	 */
	public static void main(String...args)
	{
		try
		{
			jppfClient = new JPPFClient();
			perform();
			//perform2();
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
	 * Perform the test.
	 * @throws Exception if an error is raised during the execution.
	 */
	private static void perform() throws Exception
	{
		TypedProperties config = JPPFConfiguration.getProperties();
		boolean inNodeOnly = config.getBoolean("datasize.inNodeOnly", false);
		int datasize = config.getInt("datasize.size", 1);
		int nbTasks = config.getInt("datasize.nbTasks", 10);
		String unit = config.getString("datasize.unit", "b").toLowerCase();
		int size = datasize;
		if ("k".equals(unit)) size *= KILO;
		else if ("m".equals(unit)) size *= MEGA;
		
		String s = StringUtils.buildString("JPPFJob, data size = ", datasize, unit, ", nbTasks = ", nbTasks, ", inNodeOnly = ", inNodeOnly);
		output("Running datasize demo with " + s);
		long totalTime = System.currentTimeMillis();
		JPPFJob job = new JPPFJob();
		job.setId("Datasize job [" + s + "]");
		for (int i=0; i<nbTasks; i++)
		{
			JPPFTask task = new DataTask(size, inNodeOnly);
			task.setId("task " + (i+1));
			job.addTask(task);
		}
		List<JPPFTask> results = jppfClient.submit(job);
		for (JPPFTask t: results)
		{
			if (t.getException() != null) System.out.println("error for " +t.getId() + " : " +  t.getException().getMessage());
			else System.out.println("result for " + t.getId() + " : " + t.getResult());
		}
		totalTime = System.currentTimeMillis() - totalTime;
		output("Computation time: " + StringUtils.toStringDuration(totalTime));
	}

	/**
	 * Perform the test.
	 * @throws Exception if an error is raised during the execution.
	 */
	private static void perform2() throws Exception
	{
		TypedProperties config = JPPFConfiguration.getProperties();
		boolean inNodeOnly = config.getBoolean("datasize.inNodeOnly", false);
		int datasize = config.getInt("datasize.size", 1);
		int nbTasks = config.getInt("datasize.nbTasks", 10);
		String unit = config.getString("datasize.unit", "b").toLowerCase();
		int size = datasize;
		if ("k".equals(unit)) size *= KILO;
		else if ("m".equals(unit)) size *= MEGA;
		
		String s = StringUtils.buildString("JPFFExecutor, data size = ", datasize, unit, ", nbTasks = ", nbTasks, ", inNodeOnly = ", inNodeOnly);
		output("Running datasize demo with " + s);
		long totalTime = System.currentTimeMillis();
		ExecutorService executor = new JPPFExecutorService(jppfClient);
		List<Future<String>> futureList = new ArrayList<Future<String>>();
		for (int i=0; i<nbTasks; i++)
		{
			Callable<String> c = new CallableDataTask(size, inNodeOnly);
			futureList.add(executor.submit(c));
		}
		for (Future<String> f: futureList)
		{
			System.out.println("task result: " + f.get());
		}
		totalTime = System.currentTimeMillis() - totalTime;
		output("Computation time: " + StringUtils.toStringDuration(totalTime));
	}

	/**
	 * Print a message to the console and/or log file.
	 * @param message the message to print.
	 */
	private static void output(String message)
	{
		System.out.println(message);
		log.info(message);
	}
}
