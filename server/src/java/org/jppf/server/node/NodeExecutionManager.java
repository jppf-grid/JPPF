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

package org.jppf.server.node;

import java.text.*;
import java.util.*;
import java.util.concurrent.*;

import org.apache.commons.logging.*;
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
	 * Determines whether the debug level is enabled in the log configuration, without the cost of a method call.
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
	 * List of tasks to restart.
	 */
	private List<JPPFTask> toResubmit = new ArrayList<JPPFTask>();
	/**
	 * Mapping of tasks to their id.
	 */
	private Map<String, List<Pair<Future<?>, JPPFTask>>> idMap =
		new HashMap<String, List<Pair<Future<?>, JPPFTask>>>();

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
		LinkedBlockingQueue queue = new LinkedBlockingQueue();
		//threadPool = Executors.newFixedThreadPool(poolSize);
		threadPool = new ThreadPoolExecutor(poolSize, poolSize, Long.MAX_VALUE, TimeUnit.MICROSECONDS, queue);
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
		perform(uuidList, taskList);
		List<JPPFTask> tempList = new ArrayList<JPPFTask>();
		boolean done = false;
		while (!done)
		{
			synchronized(this)
			{
				idMap.clear();
				done = toResubmit.isEmpty();
				if (!done)
				{
					tempList = toResubmit;
					if (debugEnabled) log.debug("restarting " + tempList.size() + " tasks");
				}
				toResubmit = new ArrayList<JPPFTask>();
			}
			if (!done) perform(uuidList, tempList);
		}
	}

	/**
	 * Execute the specified tasks.
	 * @param uuidList the uuid path for the classloader providers.
	 * @param taskList the lsit of tasks to execute.
	 * @throws Exception if the execution failed.
	 */
	private void perform(List<String> uuidList, List<JPPFTask> taskList) throws Exception
	{
		if (debugEnabled) log.debug("executing " + taskList.size() + " tasks");
		List<Future<?>> futureList = new ArrayList<Future<?>>(taskList.size());
		synchronized(this)
		{
			for (JPPFTask task : taskList)
			{
				Future<?> f = threadPool.submit(new NodeTaskWrapper(node, task, uuidList));
				futureList.add(f);
				String id = task.getId();
				if (id != null)
				{
					List<Pair<Future<?>, JPPFTask>> pairList = idMap.get(id);
					if (pairList == null)
					{
						pairList = new ArrayList<Pair<Future<?>, JPPFTask>>();
						idMap.put(id, pairList);
					}
					pairList.add(new Pair<Future<?>, JPPFTask>(f, task));
				}
				if ((task.getTimeout() > 0L) || (task.getTimeoutDate() != null))
				{
					processTaskTimeout(task, f);
				}
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
	 * Cancel the execution of the tasks with the specified id.
	 * @param id the id of the tasks to cancel.
	 */
	public void cancelTask(String id)
	{
		synchronized(this)
		{
			if (debugEnabled) log.debug("cancelling tasks with id = " + id);
			List<Pair<Future<?>, JPPFTask>> pairList = idMap.get(id);
			if (pairList == null) return;
			idMap.remove(id);
			if (debugEnabled) log.debug("number of tasks to cancel: " + pairList.size());
			for (Pair<Future<?>, JPPFTask> pair: pairList)
			{
				Future<?> future = pair.first();
				if (!future.isDone())
				{
					future.cancel(true);
					pair.second().onCancel();
					//pair.second().setException(new JPPFException("This task was cancelled"));
				}
			}
		}
	}

	/**
	 * Restart the execution of the tasks with the specified id.<br>
	 * The task(s) will be restarted even if their execution has already completed.
	 * @param id the id of the task or tasks to restart.
	 */
	public void restartTask(String id)
	{
		synchronized(this)
		{
			if (debugEnabled) log.debug("restarting tasks with id = " + id);
			List<Pair<Future<?>, JPPFTask>> pairList = idMap.get(id);
			if (pairList == null) return;
			idMap.remove(id);
			if (debugEnabled) log.debug("number of tasks to restart: " + pairList.size());
			for (Pair<Future<?>, JPPFTask> pair: pairList)
			{
				Future<?> future = pair.first();
				if (!future.isDone()) future.cancel(true);
				toResubmit.add(pair.second());
				pair.second().onRestart();
			}
		}
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
				log.error("Unparseable timeout date: " + date + ", format = " + sdf.toPattern(), e);
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
	 * Set the size of the node's thread pool.
	 * @param size the size as an int.
	 */
	public void setThreadPoolSize(int size)
	{
		if (size <= 0) return;
		ThreadPoolExecutor tpe = (ThreadPoolExecutor) threadPool;
		if (size > tpe.getCorePoolSize())
		{
			tpe.setMaximumPoolSize(size);
			tpe.setCorePoolSize(size);
			log.info("Node thread pool size increased to " + size);
		}
		else if (size < tpe.getCorePoolSize())
		{
			tpe.setCorePoolSize(size);
			tpe.setMaximumPoolSize(size);
			log.info("Node thread pool size reduced to " + size);
		}
		JPPFConfiguration.getProperties().setProperty("processing.threads", "" + size);
	}

	/**
	 * Get the size of the node's thread pool.
	 * @return the size as an int.
	 */
	public int getThreadPoolSize()
	{
		if (threadPool == null) return 0;
		return ((ThreadPoolExecutor) threadPool).getCorePoolSize();
	}
}
