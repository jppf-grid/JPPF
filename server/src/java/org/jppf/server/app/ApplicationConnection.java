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

import static org.jppf.server.JPPFStatsUpdater.*;
import static org.jppf.server.protocol.JPPFTaskBundle.Type.EXECUTION;

import java.net.*;
import java.util.*;

import org.apache.commons.logging.*;
import org.jppf.JPPFException;
import org.jppf.server.*;
import org.jppf.server.protocol.JPPFTaskBundle;
import org.jppf.utils.*;

/**
 * Instances of this class listen to incoming client application requests, so as
 * to dispatch them for execution.<br>
 * When the execution of a task is complete, this connection is automatically
 * notified, through an asynchronous event mechanism.
 * @author Laurent Cohen
 */
public class ApplicationConnection extends JPPFConnection
{
	/**
	 * Logger for this class.
	 */
	private static Log log = LogFactory.getLog(ApplicationConnection.class);

	/**
	 * Base name used for localization lookups";
	 */
	private static final String I18N_BASE = "org.jppf.server.i18n.messages";

	/**
	 * Determines whether debug log statements are enabled.
	 */
	private static boolean debugEnabled = log.isDebugEnabled();

	/**
	 * Determines whether dumping byte arrays in the log is enabled.
	 */
	private boolean dumpEnabled = JPPFConfiguration.getProperties().getBoolean("byte.array.dump.enabled", false);

	/**
	 * A reference to the driver's tasks queue.
	 */
	private JPPFQueue queue = null;

	/**
	 * Used to serialize and deserialize the tasks data.
	 */
	private SerializationHelper helper = new SerializationHelperImpl();

	/**
	 * The header describing the current client request.
	 */
	private JPPFTaskBundle header = null;

	/**
	 * Total number of tasks submitted to this application connection.
	 */
	private int totalTaskCount = 0;

	/**
	 * Used to send the task results back to the requester.
	 */
	private ApplicationResultSender resultSender = null;

	/**
	 * Initialize this connection with an open socket connection to a remote client.
	 * @param server the server that created this connection.
	 * @param socket the socket connection from which requests are received and to
	 * which responses are sent.
	 * @throws JPPFException if this socket handler can't be initialized.
	 */
	public ApplicationConnection(JPPFServer server, Socket socket)
			throws JPPFException
	{
		super(server, socket);
		resultSender = new ApplicationResultSender(socketClient);
		InetAddress addr = socket.getInetAddress();
		setName("appl [" + addr.getHostAddress() + ":" + socket.getPort() + "]");
		if (isStatsEnabled()) newClientConnection();
	}

	/**
	 * Handle execution requests from a remote client application, and send the
	 * results back to the client.<br>
	 * The main flow is as follows:
	 * <ul>
	 * <li>receive an execution request</li>
	 * <li>extract the execution tasks and addd them to the execution queue</li>
	 * <li>wait until all tasks completion notifcations have been received</li>
	 * <li>recompose the tasks results in the same order as they were received</li>
	 * <li><send results back to the client/li>
	 * </ul>
	 * 
	 * @throws Exception if an error is raised while processing an execution request.
	 * @see org.jppf.server.JPPFConnection#perform()
	 */
	public void perform() throws Exception
	{
		JPPFBuffer buffer = socketClient.receiveBytes(0);
		// Read the request header - with tasks count information
		List<JPPFTaskBundle> list = new ArrayList<JPPFTaskBundle>();
		int pos = helper.fromBytes(buffer.getBuffer(), 0, false, list, 1);
		header = list.get(0);
		JPPFTaskBundle.Type type = header.getRequestType();
		if (EXECUTION.equals(type))
		{
			executeTasks(buffer.getBuffer(), pos);
		}
	}

	/**
	 * Execute the tasks received from a client.
	 * @param data the source data from where to read the data provider and task data.
	 * @param offset the position at which to start reading in the source data.
	 * @throws Exception if the tasks could not be read.
	 */
	protected void executeTasks(byte[] data, int offset) throws Exception
	{
		int count = header.getTaskCount();
		if (debugEnabled) log.debug("Received " + count + " tasks");
		
		int pos = offset;
		byte[] dataProvider = helper.copyFromBuffer(data, pos);
		pos += 4 + dataProvider.length;
		List<byte[]> taskList = new ArrayList<byte[]>();
		for (int i = 0; i < count; i++)
		{
			byte[] taskBytes = helper.copyFromBuffer(data, pos);
			pos += 4 + taskBytes.length; 
			if (debugEnabled)
			{
				log.debug(new StringBuilder("deserialized task in ").append(taskBytes.length).append(" bytes").toString());
				if (dumpEnabled)
					log.debug(new StringBuilder("bytes: ").append(StringUtils.dumpBytes(taskBytes, 0, taskBytes.length)).toString());
			}
			taskList.add(taskBytes);
		}
		header.setDataProvider(dataProvider);
		header.getUuidPath().add(JPPFDriver.getInstance().getUuid());
		header.setTasks(taskList);
		header.setCompletionListener(resultSender);
		getQueue().addBundle(header);
		if (count > 0)
		{
			totalTaskCount += count;
			if (debugEnabled) log.debug("Queued " + totalTaskCount + " tasks");
		}
		if (count <= 0) resultSender.sendPartialResults(header);
		else resultSender.run(count);
		return;
	}

	/**
	 * Get a reference to the driver's tasks queue.
	 * @return a <code>JPPFQueue</code> instance.
	 */
	protected JPPFQueue getQueue()
	{
		if (queue == null) queue = JPPFDriver.getQueue();
		return queue;
	}

	/**
	 * Close this application connection.
	 * @see org.jppf.server.JPPFConnection#close()
	 */
	public void close()
	{
		super.close();
		if (isStatsEnabled()) clientConnectionClosed();
	}

	/**
	 * Get a string representation of this connection.
	 * 
	 * @return a string representation of this connection.
	 * @see org.jppf.server.JPPFConnection#toString()
	 */
	public String toString()
	{
		return "Application connection : " + super.toString();
	}
}
