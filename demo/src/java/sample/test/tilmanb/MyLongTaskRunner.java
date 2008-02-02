/*
 * Java Parallel Processing Framework.
 * Copyright (C) 2005-2008 JPPF Team.
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
