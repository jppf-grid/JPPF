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

import static org.jppf.server.JPPFStatsUpdater.*;
import static org.jppf.server.protocol.AdminRequest.*;
import java.io.*;
import java.net.*;
import java.util.*;
import javax.crypto.SecretKey;
import org.apache.log4j.Logger;
import org.jppf.JPPFException;
import org.jppf.security.*;
import org.jppf.server.*;
import org.jppf.server.event.TaskCompletionListener;
import org.jppf.server.node.JPPFNodeServer;
import org.jppf.server.protocol.*;
import org.jppf.server.scheduler.bundle.*;
import org.jppf.utils.*;

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
	 * A reference to the sever for the JPPF nodes.
	 */
	private JPPFNodeServer nodeServer = null;
	/**
	 * Used to serialize and deserialize the tasks data.
	 */
	private SerializationHelper helper = new SerializationHelperImpl();
	/**
	 * The header describing the current client request.
	 */
	private JPPFRequestHeader header = null;

	/**
	 * Initialize this connection with an open socket connection to a remote client.
	 * @param server the server that created this connection.
	 * @param socket the socket connection from which requests are received and to which responses are sent.
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
		byte[] bytes = buffer.getBuffer();
		DataInputStream dis = new DataInputStream(new ByteArrayInputStream(bytes));

		// Read the request header - with tasks count information
		header = (JPPFRequestHeader) helper.readNextObject(dis, false);
		String type = header.getRequestType();
		if (STATISTICS.equals(type) && isStatsEnabled())
		{
			sendStats();
		}
		else if (ADMIN.equals(type))
		{
			performAdminOperation(header);
		}
		else if (EXECUTION.equals(type) || NON_BLOCKING_EXECUTION.equals(type))
		{
			executeTasks(dis);
		}
	}

	/**
	 * Execute the tasks received from a client.
	 * @param dis the stream from which the task data are read.
	 * @throws Exception if the tasks could not be read.
	 */
	protected void executeTasks(DataInputStream dis) throws Exception
	{
		bundleMap = new HashMap<String, JPPFTaskBundle>();
		resultMap = new TreeMap<String, JPPFTaskBundle>();
		int count = header.getTaskCount();
		boolean blocking = EXECUTION.equals(header.getRequestType());
		byte[] dataProvider = helper.readNextBytes(dis);
		int bundleCount = 0;
		int n = 0;
		List<byte[]> taskList = new ArrayList<byte[]>();
		
		int bundleSize = getBundler().getBundleSize();
		int startIdx = -1;
		for (int i=0; i<count; i++)
		{
			if (startIdx < 0) startIdx = i;
			n++;
			byte[] taskBytes = helper.readNextBytes(dis);
			if (debugEnabled)
			{
				log.debug("deserialized task in " + taskBytes.length+" bytes as : "
					+ StringUtils.dumpBytes(taskBytes, 0, taskBytes.length));
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
					bundle.setStartTaskNumber(startIdx);
					bundleMap.put(uuid, bundle);
					getQueue().addBundle(bundle);
					startIdx = -1;
				}
				n = 0;
				bundleCount++;
				taskList = new ArrayList<byte[]>();
			}
		}
		dis.close();
		waitForExecution(blocking);
		if (!blocking) return;
		ByteArrayOutputStream baos = new ByteArrayOutputStream()
		{
	    public synchronized byte[] toByteArray()
			{
				return buf;
			}
		};
		DataOutputStream dos = new DataOutputStream(baos);
		dos.writeInt(header.getTaskCount());
		// start task index is -1 to indicate all task results are being sent
		dos.writeInt(-1);
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
	 * Send the results of the tasks in a bundle back to the client who submitted the request.
	 * @param bundle the bundle to get the task results from.
	 * @throws Exception if an IO exception occurred while sending the results back.
	 */
	private void sendPartialResults(JPPFTaskBundle bundle) throws Exception
	{
		ByteArrayOutputStream baos = new ByteArrayOutputStream()
		{
	    public synchronized byte[] toByteArray()
			{
				return buf;
			}
		};
		DataOutputStream dos = new DataOutputStream(baos);
		dos.writeInt(bundle.getTaskCount());
		dos.writeInt(bundle.getStartTaskNumber());
		for (int i=0; i<bundle.getTaskCount(); i++)
		{
			byte[] task = bundle.getTasks().get(i);
			helper.writeNextBytes(dos, task, 0, task.length);
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

		if (!localPwd.equals(remotePwd)) response = "Invalid password";
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
				response = performChangeSettings(request);
			}
		}
		sendAdminResponse(request, response);
	}

	/**
	 * Perform the action to change the settings for bundle size tuning.
	 * @param request the request holding the parametrs to change.
	 * @return a message to report the change status.
	 * @throws Exception if the changes could not be applied.
	 */
	private String performChangeSettings(AdminRequest request) throws Exception
	{
		boolean manual = "manual".equalsIgnoreCase((String) request.getParameter(BUNDLE_TUNING_TYPE_PARAM));
		String response = null;
		if (manual)
		{
			Number n = (Number) request.getParameter(BUNDLE_SIZE_PARAM);
			if (n != null) JPPFStatsUpdater.setStaticBundleSize(n.intValue());
			nodeServer.setBundler(BundlerFactory.createFixedSizeBundler());
			response = "Manual bundle size settings changed";
		}
		else
		{
			AnnealingTuneProfile prof = new AnnealingTuneProfile();
			Number n = (Number) request.getParameter("MinSamplesToAnalyse");
			prof.setMinSamplesToAnalyse(n.longValue());
			n = (Number) request.getParameter("MinSamplesToCheckConvergence");
			prof.setMinSamplesToCheckConvergence(n.longValue());
			n = (Number) request.getParameter("MaxDeviation");
			prof.setMaxDeviation(n.doubleValue());
			n = (Number) request.getParameter("MaxGuessToStable");
			prof.setMaxGuessToStable(n.intValue());
			n = (Number) request.getParameter("SizeRatioDeviation");
			prof.setSizeRatioDeviation(n.floatValue());
			n = (Number) request.getParameter("DecreaseRatio");
			prof.setDecreaseRatio(n.floatValue());
			setBundler(BundlerFactory.createBundler(prof));
			response = "Automatic bundle size settings changed";
		}
		return response;
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
	 * @param blocking indicates whether we should wait for all execution results, or send
	 * them back to the client as soon as they are received.
	 * @throws Exception if handing of the results fails.
	 */
	private synchronized void waitForExecution(boolean blocking)
		throws Exception
	{
		while (bundleMap.size() > 0)
		{
			try
			{
				wait();
				if (!blocking)
				{
					synchronized(resultMap)
					{
						for (String uuid: resultMap.keySet())
						{
							sendPartialResults((resultMap.get(uuid)));
						}
						resultMap.clear();
					}
				}
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
	 * Get the algorithm that dynamically computes the task bundle size.
	 * @return a <code>Bundler</code> instance.
	 */
	public Bundler getBundler()
	{
		if (nodeServer == null) nodeServer = JPPFDriver.getInstance().getNodeServer();
		return nodeServer.getBundler();
	}

	/**
	 * Set the algorithm that dynamically computes the task bundle size.
	 * @param bundler a <code>Bundler</code> instance.
	 */
	public void setBundler(Bundler bundler)
	{
		if (nodeServer == null) nodeServer = JPPFDriver.getInstance().getNodeServer();
		nodeServer.setBundler(bundler);
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
