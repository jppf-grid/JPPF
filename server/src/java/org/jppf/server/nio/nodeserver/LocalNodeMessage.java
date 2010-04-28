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

package org.jppf.server.nio.nodeserver;

import org.jppf.server.nio.ChannelWrapper;

/**
 * 
 * @author Laurent Cohen
 */
public class LocalNodeMessage extends AbstractNodeMessage
{
	/**
	 * Read the next serializable object from the specified channel.
	 * @param wrapper the channel to read from.
	 * @return true if the object has been completely read from the channel, false otherwise.
	 * @throws Exception if an IO error occurs.
	 */
	protected synchronized boolean readNextObject(ChannelWrapper<?> wrapper) throws Exception
	{
		if (locations.size() <= position) goToSleep();
		position++;
		return true;
	}

	/**
	 * Write the next object to the specified channel.
	 * @param wrapper the channel to write to.
	 * @return true if the object has been completely written the channel, false otherwise.
	 * @throws Exception if an IO error occurs.
	 */
	protected boolean writeNextObject(ChannelWrapper<?> wrapper) throws Exception
	{
		return true;
	}

	/**
	 * Cause the current thread to wait until notified.
	 */
	public synchronized void goToSleep()
	{
		try
		{
			wait();
		}
		catch(InterruptedException ignored)
		{
		}
	}

	/**
	 * Notify the threads currently waiting on this object that they can resume.
	 */
	public synchronized void wakeUp()
	{
		notifyAll();
	}
}
