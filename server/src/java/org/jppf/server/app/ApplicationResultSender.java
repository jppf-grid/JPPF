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
package org.jppf.server.app;

import org.apache.log4j.Logger;
import org.jppf.comm.socket.SocketWrapper;
import org.jppf.server.*;
import org.jppf.server.protocol.JPPFTaskBundle;
import org.jppf.utils.JPPFBuffer;

/**
 * 
 * @author Laurent Cohen
 */
public class ApplicationResultSender extends AbstractResultSender
{
	/**
	 * Log4j logger for this class.
	 */
	private static Logger log = Logger.getLogger(ApplicationResultSender.class);
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
		int size = 4;
		for (byte[] task : bundle.getTasks())
		{
			size += 4 + task.length;
		}
		byte[] data = new byte[size];
		int pos = helper.writeInt(bundle.getTaskCount(), data, 0);
		for (byte[] task : bundle.getTasks())
		{
			pos = helper.copyToBuffer(task, data, pos, task.length);
		}
		JPPFBuffer buffer = new JPPFBuffer(data, size);
		socketClient.sendBytes(buffer);
	}
}
