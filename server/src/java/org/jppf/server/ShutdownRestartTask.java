/*
 * Java Parallel Processing Framework.
 *  Copyright (C) 2005-2009 JPPF Team. 
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
package org.jppf.server;

import java.util.*;

import org.apache.commons.logging.*;
import org.jppf.JPPFError;

/**
 * Task used by a timer to shutdown, and eventually restart, this server.<br>
 * Both shutdown and restart operations can be performed with a specified delay.
 */
public class ShutdownRestartTask extends TimerTask
{
	/**
	 * Logger for this class.
	 */
	static Log log = LogFactory.getLog(ShutdownRestartTask.class);
	/**
	 * Determines whether the server should restart after shutdown is complete.
	 */
	private boolean restart = true;
	/**
	 * Delay, starting from shutdown completion, after which the server is restarted.
	 */
	private long restartDelay = 0L;
	/**
	 * The timer used to schedule this task, and eventually the restart operation.
	 */
	private Timer timer = null;

	/**
	 * Initialize this task with the specified parameters.<br>
	 * The shutdown is initiated after the specified shutdown delay has expired.<br>
	 * If the restart parameter is set to false then the JVM exits after the shutdown is complete.
	 * @param timer the timer used to schedule this task, and eventually the restart operation.
	 * @param restart determines whether the server should restart after shutdown is complete.
	 * If set to false, then the JVM will exit.
	 * @param restartDelay delay, starting from shutdown completion, after which the server is restarted.
	 * A value of 0 or less means the server is restarted immediately after the shutdown is complete. 
	 */
	public ShutdownRestartTask(Timer timer, boolean restart, long restartDelay)
	{
		this.timer = timer;
		this.restart = restart;
		this.restartDelay = restartDelay;
	}

	/**
	 * Perform the actual shutdown, and eventually restart, as specified in the constructor.
	 * @see java.util.TimerTask#run()
	 */
	public void run()
	{
		log.info("Initiating shutdown");
		JPPFDriver.getInstance().shutdown();
		if (!restart)
		{
			log.info("Performing requested exit");
			System.exit(0);
		}
		else
		{
			TimerTask task = new TimerTask()
			{
				public void run()
				{
					try
					{
						log.info("Initiating restart");
						System.exit(2);
					}
					catch(Exception e)
					{
						log.fatal(e.getMessage(), e);
						throw new JPPFError("Could not restart the JPPFDriver");
					}
				}
			};
			cancel();
			timer.schedule(task, restartDelay);
		}
	}
}