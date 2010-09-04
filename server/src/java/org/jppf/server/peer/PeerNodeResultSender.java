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
package org.jppf.server.peer;

import org.jppf.comm.socket.SocketWrapper;
import org.jppf.io.*;
import org.jppf.server.AbstractResultSender;
import org.jppf.server.protocol.*;
import org.jppf.utils.JPPFBuffer;
import org.slf4j.*;

/**
 * Result sender for a peer driver.<br>
 * Instances of this class are used by a driver to receive task bundles from another driver
 * and send the results back to this driver.
 * @author Laurent Cohen
 */
class PeerNodeResultSender extends AbstractResultSender
{
	/**
	 * Logger for this class.
	 */
	private static Logger log = LoggerFactory.getLogger(PeerNodeResultSender.class);
	/**
	 * Determines whether debug log statements are enabled.
	 */
	private static boolean debugEnabled = log.isDebugEnabled();
	/**
	 * Output destination wrapping all write operations on the socket client.
	 */
	private OutputDestination destination = null;

	/**
	 * Initialize this result sender with a specified socket client.
	 * @param socketClient the socket client used to send results back.
	 */
	public PeerNodeResultSender(SocketWrapper socketClient)
	{
		super(socketClient, false);
		destination = new SocketWrapperOutputDestination(socketClient);
	}

	/**
	 * This method waits until all tasks of a request have been completed.
	 * @throws Exception if handing of the results fails.
	 * @see org.jppf.server.AbstractResultSender#waitForExecution()
	 */
	public synchronized void waitForExecution() throws Exception
	{
		long start = System.currentTimeMillis();
		while (getPendingTasksCount() > 0)
		{
			try
			{
				wait();
				if (debugEnabled) log.debug(""+getResultList().size()+" in result list");
				if (!getResultList().isEmpty())
				{
					BundleWrapper first = getResultList().remove(0);
					int count = first.getTasks().size();
					int size = getResultList().size();
					for (int i=0; i<size; i++)
					{
						BundleWrapper bundle = getResultList().remove(0);
						for (DataLocation task: bundle.getTasks())
						{
							first.addTask(task);
							count++;
						}
						bundle.getTasks().clear();
					}
					first.getBundle().setTaskCount(count);
					long elapsed = System.currentTimeMillis() - start;
					first.getBundle().setNodeExecutionTime(elapsed);
					sendPartialResults(first);
				}
				getResultList().clear();
			}
			catch (InterruptedException e)
			{
				log.error(e.getMessage(), e);
			}
		}
	}

	/**
	 * Send the results of the tasks in a bundle back to the client who
	 * submitted the request.
	 * @param bundleWrapper the bundle to get the task results from.
	 * @throws Exception if an IO exception occurred while sending the results back.
	 */
	public void sendPartialResults(BundleWrapper bundleWrapper) throws Exception
	{
		JPPFTaskBundle bundle = bundleWrapper.getBundle();
		if (debugEnabled) log.debug("Sending bundle with "+bundle.getTaskCount()+" tasks");
		//long elapsed = System.currentTimeMillis() - bundle.getNodeExecutionTime();
		//bundle.setNodeExecutionTime(elapsed);

		JPPFBuffer buf = helper.getSerializer().serialize(bundle);
		socketClient.sendBytes(buf);
		for (DataLocation task : bundleWrapper.getTasks())
		{
			destination.writeInt(task.getSize());
			task.transferTo(destination, true);
		}
		socketClient.flush();
	}
}
