/*
 * Java Parallel Processing Framework.
 * Copyright (C) 2005-2006 Laurent Cohen.
 * lcohen@osp-chicago.com
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

import static org.jppf.server.JPPFStatsUpdater.clientConnectionClosed;
import static org.jppf.server.JPPFStatsUpdater.getStats;
import static org.jppf.server.JPPFStatsUpdater.isStatsEnabled;
import static org.jppf.server.JPPFStatsUpdater.newClientConnection;
import static org.jppf.server.protocol.AdminRequest.BUNDLE_SIZE_PARAM;
import static org.jppf.server.protocol.AdminRequest.CHANGE_PASSWORD;
import static org.jppf.server.protocol.AdminRequest.CHANGE_SETTINGS;
import static org.jppf.server.protocol.AdminRequest.COMMAND_PARAM;
import static org.jppf.server.protocol.AdminRequest.KEY_PARAM;
import static org.jppf.server.protocol.AdminRequest.NEW_PASSWORD_PARAM;
import static org.jppf.server.protocol.AdminRequest.PASSWORD_PARAM;
import static org.jppf.server.protocol.AdminRequest.RESPONSE_PARAM;
import static org.jppf.server.protocol.AdminRequest.RESTART_DELAY_PARAM;
import static org.jppf.server.protocol.AdminRequest.SHUTDOWN;
import static org.jppf.server.protocol.AdminRequest.SHUTDOWN_DELAY_PARAM;
import static org.jppf.server.protocol.AdminRequest.SHUTDOWN_RESTART;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.net.InetAddress;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import javax.crypto.SecretKey;

import org.apache.log4j.Logger;
import org.jppf.JPPFException;
import org.jppf.security.CryptoUtils;
import org.jppf.security.PasswordManager;
import org.jppf.server.JPPFConnection;
import org.jppf.server.JPPFDriver;
import org.jppf.server.JPPFQueue;
import org.jppf.server.JPPFServer;
import org.jppf.server.JPPFStats;
import org.jppf.server.JPPFStatsUpdater;
import org.jppf.server.event.TaskCompletionListener;
import org.jppf.server.protocol.AdminRequest;
import org.jppf.server.protocol.JPPFRequestHeader;
import org.jppf.server.protocol.JPPFTaskBundle;
import org.jppf.utils.JPPFBuffer;
import org.jppf.utils.SerializationHelper;
import org.jppf.utils.SerializationHelperImpl;
import org.jppf.utils.StringUtils;

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
	private Map<String, JPPFTaskBundle> bundleMap = null;
	/**
	 * Mapping of task results to a unique id within the context of the enclosing request. 
	 */
	private Map<String, JPPFTaskBundle> resultMap = null;
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
		else if (JPPFRequestHeader.ADMIN.equals(header.getRequestType()))
		{
			performAdminOperation(header);
		}
		else if (JPPFRequestHeader.EXECUTION.equals(header.getRequestType()))
		{
			executeTasks(header, helper, dis);
		}
	}

	/**
	 * Execute the tasks receive from a client.
	 * @param header the header describing the client request.
	 * @param helper used to read the tasks data.
	 * @param dis the stream from which the task data are read.
	 * @throws Exception if the tasks could not be read.
	 */
	protected void executeTasks(JPPFRequestHeader header, SerializationHelper helper, DataInputStream dis)
		throws Exception
	{
		bundleMap = new HashMap<String, JPPFTaskBundle>();
		resultMap = new TreeMap<String, JPPFTaskBundle>();
		int count = header.getTaskCount();
		byte[] dataProvider = helper.readNextBytes(dis);
		int bundleCount = 0;
		int n = 0;
		List<byte[]> taskList = new ArrayList<byte[]>();
		int bundleSize = JPPFStatsUpdater.getStaticBundleSize();
		for (int i=0; i<count; i++)
		{
			n++;
			byte[] taskBytes = helper.readNextBytes(dis);
			if (debugEnabled)
			{
				log.debug("deserialized task in "+taskBytes.length+" bytes as : "+StringUtils.dumpBytes(taskBytes, 0, taskBytes.length));
			}
			taskList.add(taskBytes);
			if ((n == bundleSize) || (i == count - 1))
			{
				String uuid = StringUtils.padLeft(""+bundleCount, '0', 10);
				synchronized(bundleMap)
				{
					JPPFTaskBundle bundle = new JPPFTaskBundle();
					bundle.setUuid(uuid);
					bundle.setRequestUuid(header.getUuid());
					bundle.setAppUuid(header.getAppUuid());
					bundle.setDataProvider(dataProvider);
					bundle.setTaskCount(n);
					bundle.setTasks(taskList);
					bundle.setCompletionListener(this);
					bundleMap.put(uuid, bundle);
					getQueue().addBundle(bundle);
				}
				n = 0;
				bundleCount++;
				taskList = new ArrayList<byte[]>();
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
			JPPFTaskBundle bundle = resultMap.get(id);
			for (int i=0; i<bundle.getTaskCount(); i++)
			{
				byte[] task = bundle.getTasks().get(i);
				helper.writeNextBytes(dos, task, 0, task.length);
			}
		}
		dos.flush();
		dos.close();
		JPPFBuffer buffer = new JPPFBuffer();
		buffer.setLength(baos.size());
		buffer.setBuffer(baos.toByteArray());
		socketClient.sendBytes(buffer);
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
		String response = "Request executed";
		AdminRequest request = (AdminRequest) header;
		byte[] b = (byte[]) request.getParameter(KEY_PARAM);
		b = CryptoUtils.decrypt(b);
		SecretKey tmpKey = CryptoUtils.getSecretKeyFromEncoded(b);
		b = (byte[]) request.getParameter(PASSWORD_PARAM);
		String remotePwd = new String(CryptoUtils.decrypt(tmpKey, b));
		PasswordManager pm = new PasswordManager();
		b = pm.readPassword();
		String localPwd = new String(CryptoUtils.decrypt(b));
		if (!localPwd.equals(remotePwd))
		{
			response = "Invalid password";
		}
		else
		{
			String command = (String) request.getParameter(COMMAND_PARAM);
			if (SHUTDOWN.equals(command) || SHUTDOWN_RESTART.equals(command))
			{
				long shutdownDelay = (Long) request.getParameter(SHUTDOWN_DELAY_PARAM);
				boolean restart = !SHUTDOWN.equals(command);
				long restartDelay = (Long) request.getParameter(RESTART_DELAY_PARAM);
				sendAdminResponse(request, "Request acknowledged");
				JPPFDriver.getInstance().initiateShutdownRestart(shutdownDelay, restart, restartDelay);
				return;
			}
			else if (CHANGE_PASSWORD.equals(command))
			{
				b = (byte[]) request.getParameter(NEW_PASSWORD_PARAM);
				String newPwd = new String(CryptoUtils.decrypt(tmpKey, b));
				pm.savePassword(CryptoUtils.encrypt(newPwd.getBytes()));
				response = "Password changed";
			}
			else if (CHANGE_SETTINGS.equals(command))
			{
				Number n = (Number) request.getParameter(BUNDLE_SIZE_PARAM);
				if (n != null) JPPFStatsUpdater.setStaticBundleSize(n.intValue());
				response = "Settings changed";
			}
		}
		sendAdminResponse(request, response);
	}
	
	/**
	 * Send the response to an admin request.
	 * @param request the admin request that holds the response.
	 * @param msg the response messages.
	 * @throws Exception if the response could not be sent.
	 */
	private void sendAdminResponse(AdminRequest request, String msg) throws Exception
	{
		request.setParameter(RESPONSE_PARAM, msg);
		ByteArrayOutputStream baos = new ByteArrayOutputStream()
		{
	    public synchronized byte[] toByteArray()
			{
				return buf;
			}
		};
		DataOutputStream dos = new DataOutputStream(baos);
		SerializationHelper helper = new SerializationHelperImpl();
		helper.writeNextObject(request, dos, false);

		dos.flush();
		dos.close();
		JPPFBuffer buffer = new JPPFBuffer();
		buffer.setLength(baos.size());
		buffer.setBuffer(baos.toByteArray());
		socketClient.sendBytes(buffer);
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
		while (bundleMap.size() > 0)
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
	public synchronized void taskCompleted(JPPFTaskBundle result)
	{
		String uuid = result.getUuid();
		synchronized(bundleMap)
		{
			bundleMap.remove(uuid);
		}
		synchronized(resultMap)
		{
			resultMap.put(uuid, result);
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
