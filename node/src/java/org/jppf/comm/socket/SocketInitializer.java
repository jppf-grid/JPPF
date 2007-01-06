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
package org.jppf.comm.socket;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.locks.*;
import org.jppf.JPPFError;
import org.jppf.utils.*;

/**
 * Instances of this class attempt to connect a {@link org.jppf.comm.socket.SocketWrapper SocketWrapper} to a remote server.
 * The connection attempts are performed until a configurable amount of time has passed, and at a configurable time interval.
 * When no attempt succeeded, a <code>JPPFError</code> is thrown, and the application should normally exit.
 * @author Laurent Cohen
 */
public class SocketInitializer
{
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
	 * Determines whether any connection attempt succeeded.
	 */
	private boolean successfull = false;
	/**
	 * Current number of connection attempts.
	 */
	private int attemptCount = 0;
	/**
	 * The socket wrapper to initialize.
	 */
	private SocketWrapper socketWrapper = null;
	/**
	 * Used to compute a random start delay for this node.
	 */
	private static Random rand = new Random(System.currentTimeMillis());

	/**
	 * Instantiate this SocketInitializer with a specified socket wrapper.
	 */
	public SocketInitializer()
	{
	}

	/**
	 * Initialize the underlying socket client, by starting a <code>Timer</code> and a corresponding
	 * <code>TimerTask</code> until a specified amount of time has passed.
	 * @param socketWrapper the socket wrapper to initialize.
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
				socketWrapper.close();
			}
			catch(IOException e)
			{
			}
			// random delay between 0 and 1 second , to avoid overloading the server with simultaneous connection requests.
			TypedProperties props = JPPFConfiguration.getProperties();
			long delay = 1000L * props.getLong("reconnect.initial.delay", 0L);
			if (delay == 0L) delay = rand.nextInt(1000);
			long maxTime = props.getLong("reconnect.max.time", 60L);
			long maxDuration = (maxTime <= 0) ? -1L : 1000L * maxTime;
			long period = 1000L * props.getLong("reconnect.interval", 1L);
			latestAttemptDate = (maxDuration > 0) ? new Date(System.currentTimeMillis() + maxDuration) : null;
			SocketInitializationTask task = new SocketInitializationTask();
			Timer timer = new Timer("Socket initializer timer");
			timer.schedule(task, delay, period);
			try
			{
				condition.await();
			}
			catch(InterruptedException e)
			{
				System.err.println(errMsg);
				throw new JPPFError(fatalErrMsg, e);
			}
			timer.cancel();
			timer.purge();
			if (!isSuccessfull())
			{
				System.err.println(errMsg);
				//throw new JPPFError(fatalErrMsg);
			}
		}
		finally
		{
			lock.unlock();
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

	/**
	 * Determine whether any connection attempt succeeded.
	 * @return true if any attempt was successfull, false otherwise.
	 */
	public boolean isSuccessfull()
	{
		return successfull;
	}
}
