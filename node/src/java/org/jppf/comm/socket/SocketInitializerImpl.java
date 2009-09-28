/*
 * Java Parallel Processing Framework.
 * Copyright (C) 2005-2009 JPPF Team.
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
package org.jppf.comm.socket;

import java.util.*;
import java.util.concurrent.locks.*;

import org.apache.commons.logging.*;
import org.jppf.JPPFError;
import org.jppf.utils.*;

/**
 * Instances of this class attempt to connect a {@link org.jppf.comm.socket.SocketWrapper SocketWrapper} to a remote server.
 * The connection attempts are performed until a configurable amount of time has passed, and at a configurable time interval.
 * When no attempt succeeded, a <code>JPPFError</code> is thrown, and the application should normally exit.
 * @author Laurent Cohen
 */
public class SocketInitializerImpl extends AbstractSocketInitializer
{
	/**
	 * Logger for this class.
	 */
	private static Log log = LogFactory.getLog(SocketInitializerImpl.class);
	/**
	 * Determines whether the debug level is enabled in the logging configuration, without the cost of a method call.
	 */
	private boolean debugEnabled = log.isDebugEnabled();
	/**
	 * Date after which this task stop trying to connect the class loader.
	 */
	private Date latestAttemptDate = null;
	/**
	 * The locking lock used to block all class loaders while initializing the connection.
	 */
	private ReentrantLock lock = new ReentrantLock();
	/**
	 * The locking condition used to block all class loaders while initializing the connection.
	 */
	private Condition condition = lock.newCondition();
	/**
	 * The timer that periodically attempts the connection to the server.
	 */
	private Timer timer = null;

	/**
	 * Instantiate this SocketInitializer with a specified socket wrapper.
	 */
	public SocketInitializerImpl()
	{
	}

	/**
	 * Initialize the underlying socket client, by starting a <code>Timer</code> and a corresponding
	 * <code>TimerTask</code> until a specified amount of time has passed.
	 * @param socketWrapper the socket wrapper to initialize.
	 * @see org.jppf.comm.socket.SocketInitializer#initializeSocket(org.jppf.comm.socket.SocketWrapper)
	 */
	public void initializeSocket(SocketWrapper socketWrapper)
	{
		String errMsg = "SocketInitializer.initializeSocket(): Could not reconnect to the remote server";
		String fatalErrMsg = "FATAL: could not initialize the Socket Wrapper!";
		this.socketWrapper = socketWrapper;
		lock.lock();
		try
		{
			try
			{
				if (debugEnabled) log.debug(name + "about to close socket wrapper");
				socketWrapper.close();
			}
			catch(Exception e)
			{
			}
			// random delay between 0 and 1 second , to avoid overloading the server with simultaneous connection requests.
			TypedProperties props = JPPFConfiguration.getProperties();
			long delay = 1000L * props.getLong("reconnect.initial.delay", 0L);
			//if (delay == 0L) delay = rand.nextInt(1000);
			if (delay == 0L) delay = rand.nextInt(10);
			long maxTime = props.getLong("reconnect.max.time", 60L);
			long maxDuration = (maxTime <= 0) ? -1L : 1000L * maxTime;
			long period = 1000L * props.getLong("reconnect.interval", 1L);
			latestAttemptDate = (maxDuration > 0) ? new Date(System.currentTimeMillis() + maxDuration) : null;
			SocketInitializationTask task = new SocketInitializationTask();
			timer = new Timer("Socket initializer timer");
			timer.schedule(task, delay, period);
			try
			{
				condition.await();
			}
			catch(InterruptedException e)
			{
				if (debugEnabled) log.debug(name + e.getMessage(), e);
				if (!closed)
				{
					System.err.println(name + errMsg);
					if (debugEnabled) log.debug(name + errMsg);
					throw new JPPFError(name + fatalErrMsg, e);
				}
			}
			timer.cancel();
			timer.purge();
			if (!isSuccessfull() && !closed)
			{
				if (debugEnabled) log.debug(name + errMsg);
				System.err.println(name + errMsg);
				//throw new JPPFError(fatalErrMsg);
			}
		}
		finally
		{
			lock.unlock();
		}
	}

	/**
	 * Close this initializer.
	 * @see org.jppf.comm.socket.SocketInitializer#close()
	 */
	public void close()
	{
		if (!closed)
		{
			if (debugEnabled) log.debug(name + "closing socket initializer");
			closed = true;
			if (timer != null)
			{
				if (debugEnabled) log.debug(name + "timer not null");
				timer.cancel();
				timer.purge();
			}
		}
	}

	/**
	 * This timer task attempts to (re)connect a socket wrapper to its corresponding remote server.
	 * It also checks that the maximum duration for the attempts has not been reached, and cancels itself if it has.
	 */
	class SocketInitializationTask extends TimerTask
	{
		/**
		 * Attempt to connect to the remote server.
		 * @see java.util.TimerTask#run()
		 */
		public void run()
		{
			attemptCount++;
			try
			{
				socketWrapper.open();
				successfull = true;
				reset();
			}
			catch(Exception e)
			{
				if (latestAttemptDate != null)
				{
					Date now = new Date();
					if (now.after(latestAttemptDate))
					{
						successfull = false;
						reset();
					}
				}
			}
		}

		/**
		 * Reset the status of this task.
		 */
		private void reset()
		{
			lock.lock();
			try
			{
				cancel();
				condition.signalAll();
			}
			finally
			{
				lock.unlock();
			}
		}
	}
}
