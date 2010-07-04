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

package org.jppf.scheduling;

import java.text.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.commons.logging.*;

/**
 * This class handles a timer.
 * @author Laurent Cohen
 */
public class JPPFScheduleHandler
{
	/**
	 * Logger for this class.
	 */
	private static Log log = LogFactory.getLog(JPPFScheduleHandler.class);
	/**
	 * Determines whether the debug level is enabled in the logging configuration, without the cost of a method call.
	 */
	private static boolean debugEnabled = log.isDebugEnabled();
	/**
	 * Timer that will trigger an action when a schedule date is reached.
	 */
	private Timer timer = null;
	/**
	 * Count of the instances of this class, added as a suffix to the timer's name.
	 */
	private static AtomicInteger instanceCount = new AtomicInteger(0);
	/**
	 * Mapping of timer tasks to a key.
	 */
	private Map<Object, TimerTask> timerTaskMap = new Hashtable<Object, TimerTask>();
	/**
	 * The name given to this schedule handler's internal timer.
	 */
	private String name = null;
	/**
	 * Used to debug date information.
	 */
	private SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");

	/**
	 * Initialize this schedule handler with a default name.
	 */
	public JPPFScheduleHandler()
	{
		this("JPPFScheduleHandler timer - " + instanceCount.incrementAndGet());
	}

	/**
	 * Initialize this schedule handler with the specified name.
	 * @param name the name given to this schedule handler.
	 */
	public JPPFScheduleHandler(String name)
	{
		this.name = name;
		timer = new Timer(name);
	}

	/**
	 * Schedule an action.
	 * @param key key used to retrieve or cancel the action at a later time.
	 * @param schedule the schedule at which the action is triggered.
	 * @param action the action to perform when the schedule date is reached.
	 * @throws ParseException if the schedule date could not be parsed
	 */
	public void scheduleAction(Object key, JPPFSchedule schedule, Runnable action) throws ParseException
	{
		scheduleAction(key, schedule, action, System.currentTimeMillis());
	}

	/**
	 * Schedule an action.
	 * @param key key used to retrieve or cancel the action at a later time.
	 * @param schedule the schedule at which the action is triggered.
	 * @param action the action to perform when the schedule date is reached.
	 * @param start the start time to use if the schedule is expressed as a durartion.
	 * @throws ParseException if the schedule date could not be parsed
	 */
	public void scheduleAction(Object key, JPPFSchedule schedule, Runnable action, long start) throws ParseException
	{
		if (debugEnabled)
		{
			synchronized(sdf)
			{
				log.debug(name + " : scheduling action[key=" + key + ", " + schedule + ", action=" + action + ", start=" + sdf.format(new Date(start)));
			}
		}
		Date date = schedule.toDate(start);
		ScheduleHandlerTask task = new ScheduleHandlerTask(key, action);
		timerTaskMap.put(key, task);
		if (debugEnabled)
		{
			synchronized(sdf)
			{
				log.debug(name + " : date=" + sdf.format(date) + ", key=" + key + ", timerTaskMap=" + timerTaskMap);
			}
		}
		timer.schedule(task, date);
	}

	/**
	 * Cancel the scheduled action identified by the specified key.
	 * @param key the key associated with the action.
	 */
	public void cancelAction(Object key)
	{
		if (key == null) return;
		TimerTask task = null;
		task = timerTaskMap.remove(key);
		if (debugEnabled) log.debug(name + " : cancelling action for key=" + key + ", task=" + task);
		if (task != null) task.cancel();
	}

	/**
	 * Timer task that triggers an action when the corresponding schedule date is reached.
	 */
	public class ScheduleHandlerTask extends TimerTask
	{
		/**
		 * Runnable action to perform.
		 */
		private Runnable action = null;
		/**
		 * The key associated witht this action.
		 */
		private Object key = null;

		/**
		 * Timer task wrapping a scheduled action.
		 * @param key the key associated with the action.
		 * @param action the action to perform when the schedule date is reached.
		 */
		public ScheduleHandlerTask(Object key, Runnable action)
		{
			this.key = key;
			this.action = action;
		}

		/**
		 * Check if the scheduled date has been reached and execute the corresponding action 
		 * @see java.util.TimerTask#run()
		 */
		public void run()
		{
			timerTaskMap.remove(key);
			action.run();
		}
	}

	/**
	 * Cleanup this schedule handler.
	 */
	public void clear()
	{
		clear(false);
	}

	/**
	 * Shutdown this schedule handler.
	 * @param shutdown flag indicating whether this scehdule handler should be shutdown.
	 */
	public void clear(boolean shutdown)
	{
		timer.cancel();
		timer.purge();
		timerTaskMap.clear();
		if (!shutdown) timer = new Timer(name);
	}
}
