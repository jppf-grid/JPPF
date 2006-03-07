/*
 * Java Parallel Processing Framework.
 * Copyright (C) 2005-2006 Laurent Cohen.
 * lcohen@osp-chicago.com
 * 
 * This program is free software; you can redistribute it and/or modify it under the terms of
 * the GNU General Public License as published by the Free Software Foundation;
 * either version 2 of the License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License along with this program;
 * if not, write to the Free Software Foundation, Inc.,
 * 59 Temple Place, Suite 330,
 * Boston, MA 02111-1307 USA
 */
package org.jppf.server.app;

import java.io.*;
import java.net.*;
import java.util.*;
import org.apache.log4j.Logger;
import org.jppf.JPPFException;
import org.jppf.server.*;
import org.jppf.server.event.TaskCompletionListener;
import org.jppf.server.protocol.*;
import org.jppf.utils.*;
import static org.jppf.server.JPPFStatsUpdater.*;

/**
 * Instances of this class listen to incoming client application requests, so as
 * to dispatch them for execution.<br>
 * When the execution of a task is complete, this connection is automatically notified, through
 * an asynchronous event mechanism.
 * @author Laurent Cohen
 */
public class ApplicationConnection extends JPPFConnection implements TaskCompletionListener
{
	/**
	 * Log4j logger for this class.
	 */
	private static Logger log = Logger.getLogger(ApplicationConnection.class);
	/**
	 * Determines whether debug log statements are enabled.
	 */
	private static boolean debugEnabled = log.isDebugEnabled();

	/**
	 * Mapping of received tasks to a unique id within the context of their enclosing request. 
	 */
	private Map<String, JPPFTaskWrapper> wrapperMap = null;
	/**
	 * Mapping of task results to a unique id within the context of the enclosing request. 
	 */
	private Map<String, JPPFTaskWrapper> resultMap = null;
	/**
	 * A reference to the driver's tasks queue.
	 */
	private JPPFQueue queue = null;

	/**
	 * Initialize this connection with an open socket connection to a remote client.
	 * @param socket the socket connection from which requests are received and to which responses are sent.
	 * @param server the class server that created this connection.
	 * @throws JPPFException if this socket handler can't be initialized.
	 */
	public ApplicationConnection(JPPFServer server, Socket socket) throws JPPFException
	{
		super(server, socket);
		InetAddress addr = socket.getInetAddress();
		setName("appl ["+addr.getHostAddress()+":"+socket.getPort()+"]");
		if (isStatsEnabled()) newClientConnection();
	}

	/**
	 * Handle execution requests from a remote client application, and send the results back to the client.<br>
	 * The main flow is as follows:
	 * <ul>
	 * <li>receive an execution request</li>
	 * <li>extract the execution tasks and addd them to the execution queue</li>
	 * <li>wait until all tasks completion notifcations have been received</li>
	 * <li>recompose the tasks results in the same order as they were received</li>
	 * <li><send results back to the client/li>
	 * </ul>
	 * @throws Exception if an error is raised while processing an execution request.
	 * @see org.jppf.server.JPPFConnection#perform()
	 */
	public void perform() throws Exception
	{
		wrapperMap = new HashMap<String, JPPFTaskWrapper>();
		resultMap = new TreeMap<String, JPPFTaskWrapper>();

		JPPFBuffer buffer = socketClient.receiveBytes(0);
		SerializationHelper helper = new SerializationHelperImpl();
		byte[] bytes = buffer.getBuffer();
		DataInputStream dis = new DataInputStream(new ByteArrayInputStream(bytes));

		// Read the request header - with tasks count information
		JPPFRequestHeader header = (JPPFRequestHeader) helper.readNextObject(dis, false);
		if (JPPFRequestHeader.STATISTICS.equals(header.getRequestType()) && isStatsEnabled())
		{
			sendStats();
		}
		else if (JPPFRequestHeader.ADMIN.equals(header.getRequestType()) && isStatsEnabled())
		{
			performAdminOperation(header);
		}
		else if (JPPFRequestHeader.EXECUTION.equals(header.getRequestType()))
		{
			int count = header.getTaskCount();
			// Read the serialized data provider
			byte[] dataProvider = helper.readNextBytes(dis);
			// read each task
			for (int i=0; i<count; i++)
			{
				byte[] taskBytes = helper.readNextBytes(dis);
				if (debugEnabled)
				{
					log.debug("deserialized task in "+taskBytes.length+" bytes as : "+StringUtils.dumpBytes(taskBytes, 0, taskBytes.length));
				}
				String uuid = StringUtils.padLeft(""+i, '0', 10);
				synchronized(wrapperMap)
				{
					JPPFTaskWrapper wrapper = new JPPFTaskWrapper(this, uuid, header.getUuid(), taskBytes);
					wrapper.setAppUuid(header.getAppUuid());
					wrapper.setDataProvider(dataProvider);
					wrapperMap.put(uuid, wrapper);
					getQueue().addObject(wrapper);
				}
			}
			dis.close();
			waitForExecution();
			ByteArrayOutputStream baos = new ByteArrayOutputStream()
			{
		    public synchronized byte[] toByteArray()
				{
					return buf;
				}
			};
			DataOutputStream dos = new DataOutputStream(baos);
			for (String id: resultMap.keySet())
			{
				JPPFTaskWrapper wrapper = resultMap.get(id);
				helper.writeNextBytes(dos, wrapper.getBytes(), 0, wrapper.getBytes().length);
			}
			dos.flush();
			dos.close();
			buffer = new JPPFBuffer();
			buffer.setLength(baos.size());
			buffer.setBuffer(baos.toByteArray());
			socketClient.sendBytes(buffer);
		}
	}
	
	/**
	 * Send the collected statitics in response to a stats request.
	 * @throws Exception if the statistics could not be sent to the requester.
	 */
	private void sendStats() throws Exception
	{
		JPPFStats stats = getStats();
		ByteArrayOutputStream baos = new ByteArrayOutputStream()
		{
	    public synchronized byte[] toByteArray()
			{
				return buf;
			}
		};
		DataOutputStream dos = new DataOutputStream(baos);
		SerializationHelper helper = new SerializationHelperImpl();
		helper.writeNextObject(stats, dos, false);

		dos.flush();
		dos.close();
		JPPFBuffer buffer = new JPPFBuffer();
		buffer.setLength(baos.size());
		buffer.setBuffer(baos.toByteArray());
		socketClient.sendBytes(buffer);
	}

	/**
	 * Perform a requested administative function.
	 * @param header the header for the request.
	 * @throws Exception if the function could not be performed.
	 */
	private void performAdminOperation(JPPFRequestHeader header) throws Exception
	{
		AdminRequestHeader adminHeader = (AdminRequestHeader) header;
		long shutdownDelay = adminHeader.getShutdownDelay();
		boolean restart = !AdminRequestHeader.ADMIN_SHUTDOWN.equals(adminHeader.getCommand());
		long restartDelay = adminHeader.getRestartDelay();
		JPPFDriver.getInstance().initiateShutdownRestart(shutdownDelay, restart, restartDelay);
	}

	/**
	 * Get a reference to the driver's tasks queue.
	 * @return a <code>JPPFQueue</code> instance.
	 */
	private JPPFQueue getQueue()
	{
		if (queue == null) queue = JPPFDriver.getInstance().getTaskQueue();
		return queue;
	}

	/**
	 * This method waits until all tasks of a request have been completed.
	 */
	private synchronized void waitForExecution()
	{
		while (wrapperMap.size() > 0)
		{
			try
			{
				wait();
			}
			catch(InterruptedException e)
			{
				log.error(e.getMessage(), e);
			}
		}
	}
	
	/**
	 * Callback method invoked when the execution of a task has completed.
	 * This method triggers a check of the request completion status.
	 * When all tasks have completed, this connection sends all results back.
	 * @param result the result of the task's execution.
	 */
	public synchronized void taskCompleted(JPPFTaskWrapper result)
	{
		String uuid = result.getUuid();
		synchronized(resultMap)
		{
			resultMap.put(uuid, result);
		}
		synchronized(wrapperMap)
		{
			wrapperMap.remove(uuid);
		}
		notify();
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
	 * @return a string representation of this connection.
	 * @see org.jppf.server.JPPFConnection#toString()
	 */
	public String toString()
	{
		return "Application connection : " + super.toString();
	}
}
