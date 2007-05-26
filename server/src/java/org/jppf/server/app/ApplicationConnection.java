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

import static org.jppf.server.JPPFStatsUpdater.*;
import static org.jppf.server.protocol.BundleParameter.*;
import static org.jppf.server.protocol.JPPFTaskBundle.Type.*;

import java.net.*;
import java.util.*;

import javax.crypto.SecretKey;

import org.apache.log4j.Logger;
import org.jppf.JPPFException;
import org.jppf.security.*;
import org.jppf.server.*;
import org.jppf.server.protocol.*;
import org.jppf.server.scheduler.bundle.*;
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
	 * Log4j logger for this class.
	 */
	private static Logger log = Logger.getLogger(ApplicationConnection.class);

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
		byte[] bytes = buffer.getBuffer();
		// Read the request header - with tasks count information
		List<JPPFTaskBundle> list = new ArrayList<JPPFTaskBundle>();
		int pos = helper.fromBytes(buffer.getBuffer(), 0, false, list, 1);
		header = list.get(0);
		JPPFTaskBundle.Type type = header.getRequestType();
		if (STATISTICS.equals(type) && isStatsEnabled())
		{
			sendStats();
		}
		else if (ADMIN.equals(type))
		{
			performAdminOperation(header);
		}
		else if (EXECUTION.equals(type))
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
				StringBuilder sb = new StringBuilder("deserialized task in ").append(taskBytes.length).append(" bytes");
				// log.debug(sb.toString());
				if (dumpEnabled)
				{
					sb = new StringBuilder("bytes: ").append(StringUtils.dumpBytes(taskBytes, 0, taskBytes.length));
					log.debug(sb.toString());
				}
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
	 * Send the collected statistics in response to a stats request.
	 * @throws Exception if the statistics could not be sent to the requester.
	 */
	private void sendStats() throws Exception
	{
		JPPFStats stats = getStats();
		SerializationHelper helper = new SerializationHelperImpl();
		JPPFBuffer buffer = helper.toBytes(stats, false);
		socketClient.sendBytes(buffer);
	}

	/**
	 * Perform a requested administrative function.
	 * @param header the header for the request.
	 * @throws Exception if the function could not be performed.
	 */
	private void performAdminOperation(JPPFTaskBundle header) throws Exception
	{
		String response = LocalizationUtils.getLocalized(I18N_BASE, "request.executed");
		byte[] b = (byte[]) header.getParameter(KEY_PARAM);
		b = CryptoUtils.decrypt(b);
		SecretKey tmpKey = CryptoUtils.getSecretKeyFromEncoded(b);
		b = (byte[]) header.getParameter(PASSWORD_PARAM);
		String remotePwd = new String(CryptoUtils.decrypt(tmpKey, b));
		PasswordManager pm = new PasswordManager();
		b = pm.readPassword();
		String localPwd = new String(CryptoUtils.decrypt(b));

		if (!localPwd.equals(remotePwd)) response = LocalizationUtils.getLocalized(I18N_BASE, "invalid.password");
		else
		{
			BundleParameter command = (BundleParameter) header.getParameter(COMMAND_PARAM);
			if (SHUTDOWN.equals(command) || SHUTDOWN_RESTART.equals(command))
			{
				long shutdownDelay = (Long) header.getParameter(SHUTDOWN_DELAY_PARAM);
				boolean restart = !SHUTDOWN.equals(command);
				long restartDelay = (Long) header.getParameter(RESTART_DELAY_PARAM);
				sendAdminResponse(header, LocalizationUtils.getLocalized(I18N_BASE, "request.acknowledged"));
				JPPFDriver.getInstance().initiateShutdownRestart(shutdownDelay, restart, restartDelay);
				return;
			}
			else if (CHANGE_PASSWORD.equals(command))
			{
				b = (byte[]) header.getParameter(NEW_PASSWORD_PARAM);
				String newPwd = new String(CryptoUtils.decrypt(tmpKey, b));
				pm.savePassword(CryptoUtils.encrypt(newPwd.getBytes()));
				response = "Password changed";
			}
			else if (CHANGE_SETTINGS.equals(command))
			{
				response = performChangeSettings(header);
			}
		}
		sendAdminResponse(header, response);
	}

	/**
	 * Perform the action to change the settings for bundle size tuning.
	 * @param request the request holding the parameters to change.
	 * @return a message to report the change status.
	 * @throws Exception if the changes could not be applied.
	 */
	private String performChangeSettings(JPPFTaskBundle request) throws Exception
	{
		String response = null;
		Bundler bundler = BundlerFactory.createBundler(request.getParametersMap(), false);
		JPPFDriver.getInstance().getNodeNioServer().setBundler(bundler);
		boolean manual =
			"manual".equalsIgnoreCase((String) request.getParameter(BUNDLE_TUNING_TYPE_PARAM));
		if (manual) JPPFStatsUpdater.setStaticBundleSize((Integer) request.getParameter(BUNDLE_SIZE_PARAM));
		response = LocalizationUtils.getLocalized(I18N_BASE, (manual ? "manual" : "automatic") + ".settings.changed");
		return response;
	}

	/**
	 * Send the response to an admin request.
	 * @param request the admin request that holds the response.
	 * @param msg the response messages.
	 * @throws Exception if the response could not be sent.
	 */
	private void sendAdminResponse(JPPFTaskBundle request, String msg) throws Exception
	{
		request.setParameter(RESPONSE_PARAM, msg);
		JPPFBuffer buf = helper.toBytes(request, false);
		byte[] data = new byte[buf.getLength() + 4];
		helper.copyToBuffer(buf.getBuffer(), data, 0, buf.getLength());
		buf.setLength(data.length);
		buf.setBuffer(data);
		socketClient.sendBytes(buf);
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
