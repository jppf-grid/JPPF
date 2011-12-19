/*
 * JPPF.
 * Copyright (C) 2005-2011 JPPF Team.
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
package org.jppf.management;

import org.jppf.utils.ThreadSynchronization;

/**
 * This class is intended to be used as a thread that attempts to (re-)connect to
 * the management server.
 */
class JMXConnectionThread extends ThreadSynchronization implements Runnable
{
	/**
	 * Determines the suspended state of this connection thread.
	 */
	private boolean suspended = false;
	/**
	 * Determines the connecting state of this connection thread.
	 */
	private boolean connecting = true;
	/**
	 * The holder of the jmx connection to initialize.
	 */
	private final JMXConnectionWrapper wrapper;

	/**
	 * Initialize this thread with the specified jmx connection wrapper.
	 * @param wrapper the holder of the jmx connection to initialize.
	 */
	JMXConnectionThread(JMXConnectionWrapper wrapper)
	{
		this.wrapper = wrapper;
	}

	/**
	 * {@inheritDoc}
	 */
	public void run()
	{
		while (!isStopped())
		{
			if (isSuspended())
			{
				if (JMXConnectionWrapper.debugEnabled) JMXConnectionWrapper.log.debug(wrapper.getId() + " about to go to sleep");
				goToSleep();
				continue;
			}
			if (isConnecting())
			{
				try
				{
					if (JMXConnectionWrapper.debugEnabled) JMXConnectionWrapper.log.debug(wrapper.getId() + " about to perform RMI connection attempts");
					wrapper.performConnection();
					if (JMXConnectionWrapper.debugEnabled) JMXConnectionWrapper.log.debug(wrapper.getId() + " about to suspend RMI connection attempts");
					suspend();
					wakeUp();
					wrapper.wakeUp();
				}
				catch(Exception ignored)
				{
					if (JMXConnectionWrapper.debugEnabled) JMXConnectionWrapper.log.debug(wrapper.getId()+ " JMX URL = "+wrapper.url, ignored);
					try
					{
						Thread.sleep(100);
					}
					catch(InterruptedException e)
					{
						JMXConnectionWrapper.log.error(e.getMessage(), e);
					}
				}
			}
		}
	}

	/**
	 * Suspend the current thread.
	 */
	public synchronized void suspend()
	{
		if (JMXConnectionWrapper.debugEnabled) JMXConnectionWrapper.log.debug(wrapper.getId() + " suspending RMI connection attempts");
		setConnecting(false);
		setSuspended(true);
	}

	/**
	 * Resume the current thread's execution.
	 */
	public synchronized void resume()
	{
		if (JMXConnectionWrapper.debugEnabled) JMXConnectionWrapper.log.debug(wrapper.getId() + " resuming RMI connection attempts");
		setConnecting(true);
		setSuspended(false);
		wakeUp();
	}

	/**
	 * Stop this thread.
	 */
	public synchronized void close()
	{
		setConnecting(false);
		setStopped(true);
		wakeUp();
	}

	/**
	 * Get the connecting state of this connection thread.
	 * @return true if the connection is established, false otherwise.
	 */
	public synchronized boolean isConnecting()
	{
		return connecting;
	}

	/**
	 * Get the connecting state of this connection thread.
	 * @param connecting true if the connection is established, false otherwise.
	 */
	public synchronized void setConnecting(boolean connecting)
	{
		this.connecting = connecting;
	}

	/**
	 * Determines the suspended state of this connection thread.
	 * @return true if the thread is suspended, false otherwise. 
	 */
	public synchronized boolean isSuspended()
	{
		return suspended;
	}

	/**
	 * Set the suspended state of this connection thread.
	 * @param suspended true if the connection is suspended, false otherwise.
	 */
	public synchronized void setSuspended(boolean suspended)
	{
		this.suspended = suspended;
	}
}