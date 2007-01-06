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

package sample.test.tilmanb;

import java.util.*;

import org.jppf.client.JPPFClient;
import org.jppf.client.event.*;
import org.jppf.server.protocol.JPPFTask;

/**
 * 
 * @author lcohen
 */
public class MyLongTaskRunner implements TaskResultListener
{
	/**
	 * Count of results received.
	 */
	private int count = 0;
	/**
	 * Number of tasks whose results ot wait for.
	 */
	private int nbTasks = 0;
	/**
	 * 
	 */
	private Map<Integer, List<JPPFTask>> resultMap = new TreeMap<Integer, List<JPPFTask>>();

	/**
	 * Run the test.
	 * @param args not used.
	 * @throws Exception if any error occurs.
	 */
	public static void main(String[] args) throws Exception
	{
		MyLongTaskRunner resultListener = new MyLongTaskRunner();
		JPPFClient client = new JPPFClient();

		//
		List<JPPFTask> taskList = new ArrayList<JPPFTask>();
		taskList.add(new MyLongTask("listtask1", 2000));
		taskList.add(new MyLongTask("listtask2", 2000));
		taskList.add(new MyLongTask("listtask3", 2000));
		taskList.add(new MyLongTask("listtask4", 2000));
		taskList.add(new MyLongTask("listtask5", 2000));
		client.submitNonBlocking(taskList, null, resultListener);

		/*
		client.submitNonBlocking(makeTask(new MyLongTask("short1", 2000)), null, resultListener);
		client.submitNonBlocking(makeTask(new MyLongTask("short2", 2000)), null, resultListener);
		client.submitNonBlocking(makeTask(new MyLongTask("short3", 2000)), null, resultListener);
		client.submitNonBlocking(makeTask(new MyLongTask("short4", 2000)), null, resultListener);
		client.submitNonBlocking(makeTask(new MyLongTask("short5", 2000)), null, resultListener);
		client.submitNonBlocking(makeTask(new MyLongTask("short6", 2000)), null, resultListener);
		client.submitNonBlocking(makeTask(new MyLongTask("short7", 2000)), null, resultListener);
		client.submitNonBlocking(makeTask(new MyLongTask("short8", 2000)), null, resultListener);
		client.submitNonBlocking(makeTask(new MyLongTask("short9", 2000)), null, resultListener);
		client.submitNonBlocking(makeTask(new MyLongTask("short10", 2000)), null, resultListener);
		*/

		resultListener.waitForResults();
	}

	/**
	 * Build a task list from an array of tasks.
	 * @param t an array of JPPFTAsks.
	 * @return a list containing all the tasks of the array, in the same order.
	 */
	private static List<JPPFTask> makeTask(JPPFTask...t)
	{
		List<JPPFTask> tasks = new ArrayList<JPPFTask>();
		for (JPPFTask task: t) tasks.add(task);
		return tasks;
	}

	/**
	 * Wait until all results of a request have been collected.
	 */
	private synchronized void waitForResults()
	{
		while (count < nbTasks)
		{
			try
			{
				wait();
			}
			catch(InterruptedException e)
			{
				e.printStackTrace();
			}
		}
	}
	/**
	 * 
	 * @param event notification that a set of tasks results have been received. 
	 * @see org.jppf.client.event.TaskResultListener#resultsReceived(org.jppf.client.event.TaskResultEvent)
	 */
	public synchronized void resultsReceived(TaskResultEvent event)
	{
		int idx = event.getStartIndex();
		List<JPPFTask> tasks = event.getTaskList();
		System.out.println("Received results for tasks " + idx + " - " + (idx + tasks.size() - 1));
		resultMap.put(idx, tasks);
		count += tasks.size();
		notify();
	}
	
}
