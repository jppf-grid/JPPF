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
package org.jppf.server.app;

import org.apache.commons.logging.*;
import org.jppf.comm.socket.SocketWrapper;
import org.jppf.server.AbstractResultSender;
import org.jppf.server.protocol.JPPFTaskBundle;
import org.jppf.utils.JPPFBuffer;

/**
 * Instances of this class are used to send task bundle execution results back to a JPPF client.
 * @author Laurent Cohen
 */
public class ApplicationResultSender extends AbstractResultSender
{
	/**
	 * Logger for this class.
	 */
	private static Log log = LogFactory.getLog(ApplicationResultSender.class);
	/**
	 * Determines whether debug log statements are enabled.
	 */
	private static boolean debugEnabled = log.isDebugEnabled();
	/**
	 * Initialize this result sender with a specified socket client.
	 * @param socketClient the socket client used to send results back.
	 */
	public ApplicationResultSender(SocketWrapper socketClient)
	{
		super(socketClient, true);
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
		JPPFBuffer bundleBuffer = helper.toBytes(bundle, false);
		int size = 4 + bundleBuffer.getLength();
		for (byte[] task : bundle.getTasks()) size += 4 + task.length;

		byte[] intBytes = new byte[4];
		helper.writeInt(size, intBytes, 0);
		socketClient.write(intBytes);
		socketClient.sendBytes(bundleBuffer);
		for (byte[] task : bundle.getTasks())
		{
			socketClient.sendBytes(new JPPFBuffer(task, task.length));
		}
	}
}
