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
package sample.tasklength;

import java.util.*;
import org.apache.log4j.Logger;
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
	static Logger log = Logger.getLogger(LongTaskRunner.class);
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
			for (int iter=0; iter<iterations; iter++)
			{
				long start = System.currentTimeMillis();
				// create a task for each row in matrix a
				List<JPPFTask> tasks = new ArrayList<JPPFTask>();
				for (int i=0; i<nbTask; i++) tasks.add(new LongTask(length));
				// submit the tasks for execution
				List<JPPFTask> results = jppfClient.submit(tasks, null);
				long elapsed = System.currentTimeMillis() - start;
				System.out.println("Iteration #"+(iter+1)+" performed in "+StringUtils.toStringDuration(elapsed));
			}
			JPPFStats stats = jppfClient.requestStatistics();
			System.out.println("End statistics :\n"+stats.toString());
		}
		catch(Exception e)
		{
			throw new JPPFException(e.getMessage(), e);
		}
	}

}
