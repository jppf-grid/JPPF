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

import org.apache.commons.logging.*;
import org.jppf.client.*;
import org.jppf.server.JPPFStats;
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
	 * One kilobyte.
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
		TypedProperties props = JPPFConfiguration.getProperties();
		int datasize = props.getInt("datasize.size", 1);
		int nbTasks = props.getInt("datasize.nbTasks", 10);
		String unit = props.getString("datasize.unit", "b").toLowerCase();
		if ("k".equals(unit)) datasize *= KILO;
		else if ("m".equals(unit)) datasize *= MEGA;
		
		output("Running datasize demo with data size = " + datasize + " with " + nbTasks + " tasks");
		long totalTime = System.currentTimeMillis();
		JPPFJob job = new JPPFJob();
		for (int i=0; i<nbTasks; i++) job.addTask(new DataTask(datasize));
		List<JPPFTask> results = jppfClient.submit(job);
		for (JPPFTask t: results)
		{
			if (t.getException() != null) throw t.getException();
		}
		totalTime = System.currentTimeMillis() - totalTime;
		output("Computation time: " + StringUtils.toStringDuration(totalTime));
		JPPFStats stats = jppfClient.requestStatistics();
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
