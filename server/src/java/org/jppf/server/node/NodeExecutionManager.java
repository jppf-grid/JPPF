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

package org.jppf.server.node;

import java.text.*;
import java.util.*;
import java.util.concurrent.*;

import org.apache.commons.logging.*;
import org.jppf.JPPFException;
import org.jppf.server.protocol.*;
import org.jppf.utils.*;

/**
 * Instances of this class manage the execution of JPPF tasks by a node.
 * @author Laurent Cohen
 */
public class NodeExecutionManager
{
	/**
	 * Logger for this class.
	 */
	private static Log log = LogFactory.getLog(NodeExecutionManager.class);
	/**
	 * Determines whether the debug level is enabled in the log4j configuration, without the cost of a method call.
	 */
	private static boolean debugEnabled = log.isDebugEnabled();
	/**
	 * The Thread Pool that really process the tasks
	 */
	private ExecutorService threadPool = null;
	/**
	 * The node that uses this excecution manager.
	 */
	private JPPFNode node = null;
	/**
	 * Timer managing the tasks timeout.
	 */
	private Timer timeoutTimer = null;
	/**
	 * Map of futures to corresponding timeout timer tasks.
	 */
	private Map<Future<?>, TimerTask> timerTaskMap = new HashMap<Future<?>, TimerTask>();

	/**
	 * Initialize this execution manager with the specified node.
	 * @param node the node that uses this excecution manager.
	 */
	public NodeExecutionManager(JPPFNode node)
	{
		this.node = node;
		TypedProperties props = JPPFConfiguration.getProperties();
		int poolSize = props.getInt("processing.threads", 1);
		log.info("Node running " + poolSize + " processing thread" + (poolSize > 1 ? "s" : ""));
		threadPool = Executors.newFixedThreadPool(poolSize);
		timeoutTimer = new Timer("Node Task Timeout Timer");
	}

	/**
	 * Execute the specified tasks of the specified tasks bundle.
	 * @param bundle the bundle to which the tasks are associated.
	 * @param taskList the lsit of tasks to execute.
	 * @throws Exception if the execution failed.
	 */
	public void execute(JPPFTaskBundle bundle, List<JPPFTask> taskList) throws Exception
	{
		if (debugEnabled) log.debug("executing " + taskList.size() + " tasks");
		List<String> uuidList = bundle.getUuidPath().getList();
		List<Future<?>> futureList = new ArrayList<Future<?>>(taskList.size());
		for (JPPFTask task : taskList)
		{
			NodeTaskWrapper taskWrapper = new NodeTaskWrapper(node, task, uuidList);
			Future<?> f = threadPool.submit(taskWrapper);
			futureList.add(f);
			if ((task.getTimeout() > 0L) || (task.getTimeoutDate() != null))
			{
				processTaskTimeout(task, f);
			}
		}
		for (Future<?> future : futureList)
		{
			try
			{
				if (!future.isDone()) future.get();
			}
			catch(CancellationException ignored)
			{
			}
			TimerTask tt = timerTaskMap.remove(future);
			if (tt != null) tt.cancel();
		}
		timerTaskMap.clear();
	}

	/**
	 * Notify the timer that a task must be aborted if its timeout period expired.
	 * @param task the JPPF task for which to set the timeout.
	 * @param future the corresponding execution result return by the executor service.
	 */
	private void processTaskTimeout(JPPFTask task, final Future<?> future)
	{
		long time = 0L;
		if (task.getTimeout() > 0) time = task.getTimeout();
		else
		{
			String date = task.getTimeoutDate();
			SimpleDateFormat sdf = task.getTimeoutDateFormat();
			try
			{
				time = sdf.parse(date).getTime() - System.currentTimeMillis();
			}
			catch(ParseException e)
			{
				log.error("Unparseable timeout date: " + date + ", format = " + sdf.toPattern());
			}
		}
		if (time > 0L)
		{
			TimerTask tt = new TimeoutTimerTask(future, task);
			timerTaskMap.put(future, tt);
			timeoutTimer.schedule(tt, time);
		}
	}

	/**
	 * Shutdown this execution manager.
	 */
	public void shutdown()
	{
		threadPool.shutdownNow();
		if (timeoutTimer != null) timeoutTimer.cancel();
		timerTaskMap.clear();
	}

	/**
	 * Instances of this class are scheduled by a timer to execute one time, check whether the corresponding
	 * JPPF task timeout has been reached, and asbort the task if necessary.
	 */
	public class TimeoutTimerTask extends TimerTask
	{
		/**
		 * The future on which to call the cancel() method.
		 */
		private Future<?> future = null;
		/**
		 * The task to cancel.
		 */
		private JPPFTask task = null;

		/**
		 * Initialize this timer task with the specified future.
		 * @param future the future used to cancel the underlying JPPF task.
		 * @param task the task to cancel.
		 */
		public TimeoutTimerTask(Future<?> future, JPPFTask task)
		{
			this.future = future;
			this.task = task;
		}

		/**
		 * Execute this task.
		 * @see java.util.TimerTask#run()
		 */
		public void run()
		{
			if (!future.isDone())
			{
				future.cancel(true);
				task.setException(new JPPFException("This task has timed out"));
			}
		}
	}
}
