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
package org.jppf.server.peer;

import org.apache.commons.logging.*;
import org.jppf.comm.socket.SocketWrapper;
import org.jppf.server.*;
import org.jppf.server.protocol.JPPFTaskBundle;
import org.jppf.utils.JPPFBuffer;

/**
 * Result sender for a peer driver.<br>
 * Instances of this class are used by a driver to receive task bundles from another driver
 * and send the results back to this driver.
 * @author Laurent Cohen
 */
public class PeerNodeResultSender extends AbstractResultSender
{
	/**
	 * Log4j logger for this class.
	 */
	private static Log log = LogFactory.getLog(PeerNodeResultSender.class);
	/**
	 * Determines whether debug log statements are enabled.
	 */
	private static boolean debugEnabled = log.isDebugEnabled();
	/**
	 * Initialize this result sender with a specified socket client.
	 * @param socketClient the socket client used to send results back.
	 */
	public PeerNodeResultSender(SocketWrapper socketClient)
	{
		super(socketClient, false);
	}

	/**
	 * Send the results of the tasks in a bundle back to the client who
	 * submitted the request.
	 * @param bundle the bundle to get the task results from.
	 * @throws Exception if an IO exception occurred while sending the results back.
	 */
	public void sendPartialResults(JPPFTaskBundle bundle) throws Exception
	{
		if (debugEnabled) log.debug("Sending bundle with "+bundle.getTaskCount()+" tasks");
		long elapsed = System.currentTimeMillis() - bundle.getNodeExecutionTime();
		bundle.setNodeExecutionTime(elapsed);

		JPPFBuffer buf = helper.toBytes(bundle, false);
		int size = 4 + buf.getLength();
		for (int i = 0; i < bundle.getTaskCount(); i++) size += 4 + bundle.getTasks().get(i).length;
		byte[] data = new byte[size];
		int pos = helper.copyToBuffer(buf.getBuffer(), data, 0, buf.getLength());
		for (int i = 0; i < bundle.getTaskCount(); i++) 
		{
			byte[] task = bundle.getTasks().get(i);
			pos = helper.copyToBuffer(task, data, pos, task.length);
		}
		JPPFBuffer buffer = new JPPFBuffer(data, size);
		socketClient.sendBytes(buffer);
	}
}
