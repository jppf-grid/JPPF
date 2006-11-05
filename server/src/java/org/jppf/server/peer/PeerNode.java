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
package org.jppf.server.peer;

import java.io.*;
import java.net.Socket;
import java.util.*;
import org.apache.log4j.Logger;
import org.jppf.comm.socket.*;
import org.jppf.node.MonitoredNode;
import org.jppf.node.event.*;
import org.jppf.node.event.NodeEvent.EventType;
import org.jppf.server.*;
import org.jppf.utils.*;

/**
 * Instances of this class encapsulate execution nodes.
 * @author Laurent Cohen
 * @author Domingos Creado
 */
public class PeerNode implements MonitoredNode
{
	/**
	 * Log4j logger for this class.
	 */
	private static Logger log = Logger.getLogger(PeerNode.class);;
	/**
	 * Utility for deserialization and serialization.
	 */
	private SerializationHelper helper = new SerializationHelperImpl();
	/**
	 * Wrapper around the underlying server connection.
	 */
	private SocketWrapper socketClient = null;
	/**
	 * Used to synchronize access to the underlying socket from multiple threads.
	 */
	private SocketInitializer socketInitializer = new SocketInitializer();
	/**
	 * Determines whether the debug level is enabled in the log4j configuration, without the cost of a method call.
	 */
	private boolean debugEnabled = false;
	/**
	 * Used to programmatically stop this node.
	 */
	private boolean stopped = false;
	/**
	 * The list of listeners that receive notifications from this node.
	 */
	private List<NodeListener> listeners = new ArrayList<NodeListener>();
	/**
	 * This flag is true if there is at least one listener, and false otherwise.
	 */
	private boolean notifying = false;
	/**
	 * The socket used by this node's socket wrapper.
	 */
	private Socket socket = null;
	/**
	 * Total number of tasks executed.
	 */
	private int taskCount = 0;
	/**
	 * Used to send the task results back to the requester.
	 */
	private PeerNodeResultSender resultSender = null;
	/**
	 * The name of the peer in the configuration file.
	 */
	private String peerName = null;
	/**
	 * This node's universal identifier.
	 */
	private String uuid = new JPPFUuid().toString();
	/**
	 * Security credentials associated with this JPPF node.
	 */
	//private JPPFSecurityContext credentials = null;

	/**
	 * Initialize this peer node with the specified configuration name.
	 * @param peerName the name of the peer int he configuration file.
	 */
	public PeerNode(String peerName)
	{
		this.peerName = peerName;
	}

	/**
	 * Main processing loop of this node.
	 * @see java.lang.Runnable#run()
	 */
	public void run()
	{
		debugEnabled = log.isDebugEnabled();
		stopped = false;
		if (debugEnabled) log.debug("Start of peer node main loop");
		while (!stopped)
		{
			try
			{
				init();
				resultSender = new PeerNodeResultSender(socketClient);
				perform();
			}
			catch(Exception e)
			{
				log.error(e.getMessage(), e);
				try
				{
					socketClient.close();
					socket = null;
				}
				catch(Exception ex)
				{
					log.error(ex.getMessage(), ex);
				}
			}
			catch(Error e)
			{
				log.error(e.getMessage(), e);
				throw e;
			}
		}
		if (debugEnabled) log.debug("End of peer node main loop");
		if (notifying) fireNodeEvent(EventType.DISCONNECTED);
	}

	/**
	 * Perform the main execution loop for this node. At each iteration, this method listens for a task to execute,
	 * receives it, executes it and sends the results back.
	 * @throws Exception if an error was raised from the underlying socket connection or the class loader.
	 */
	public void perform() throws Exception
	{
		JPPFDriver driver = JPPFDriver.getInstance();
		if (debugEnabled) log.debug("Start of peer node secondary loop");
		while (!stopped)
		{
			JPPFTaskBundle bundle = readBundle();
			if (JPPFTaskBundle.State.INITIAL_BUNDLE.equals(bundle.getState()))
			{
				bundle.setBundleUuid(uuid);
			}
			if (notifying) fireNodeEvent(EventType.START_EXEC);
			boolean notEmpty = (bundle.getTasks() != null) && (bundle.getTaskCount() > 0);
			if (notEmpty)
			{
				bundle.getUuidPath().add(driver.getUuid());
				bundle.setCompletionListener(resultSender);
				JPPFDriver.getQueue().addBundle(bundle);
				resultSender.run(bundle.getTaskCount());
				taskCount += bundle.getTaskCount();
				if (debugEnabled) log.debug("tasks executed: "+taskCount);
			}
			else
			{
				resultSender.sendPartialResults(bundle);
			}
		}
		if (debugEnabled) log.debug("End of peer node secondary loop");
	}

	/**
	 * Initialize this node's resources.
	 * @throws Exception if an error is raised during initialization.
	 */
	public synchronized void init() throws Exception
	{
		if (debugEnabled) log.debug("initializing socket client");
		if (socketClient == null) initSocketClient();
		initCredentials();
		if (notifying) fireNodeEvent(EventType.START_CONNECT);
		if (socket == null)
		{
			if (debugEnabled) log.debug("initializing socket");
			System.out.println("PeerNode.init(): Attempting connection to the JPPF driver");
			socketInitializer.initializeSocket(socketClient);
			socket = socketClient.getSocket();
			System.out.println("PeerNode.init(): Reconnected to the JPPF driver");
		}
		if (notifying) fireNodeEvent(EventType.END_CONNECT);
	}

	/**
	 * Initialize this node's resources.
	 * @throws Exception if an error is raised during initialization.
	 */
	public void initSocketClient() throws Exception
	{
		if (socket != null) socketClient = new SocketClient(socket);
		else
		{
			if (debugEnabled) log.debug("initializing socket client");
			TypedProperties props = JPPFConfiguration.getProperties();
			String host = props.getString("jppf.peer."+peerName+".server.host", "localhost");
			int port = props.getInt("node.peer."+peerName+".server.port", 11113);
			socketClient = new SocketClient();
			socketClient.setHost(host);
			socketClient.setPort(port);
		}
		socketClient.setSerializer(helper.getSerializer());
	}

	/**
	 * Initialize the security credentials associated with this JPPF node.
	 */
	private void initCredentials()
	{
		/*
		String uuid = new JPPFUuid().toString();
		StringBuilder sb = new StringBuilder("Node:");
		sb.append(VersionUtils.getLocalIpAddress()).append(":");
		sb.append(socketClient.getPort());
		// testing that the server throws a JPPFSecurityException
		credentials = new JPPFSecurityContext(uuid, sb.toString(), new JPPFCredentials());
		*/
	}
	
	/**
	 * Perform the deserialization of the objects received through the socket connection.
	 * @return an array of deserialized objects.
	 * @throws Exception if an error occurs while deserializing.
	 */
	private JPPFTaskBundle readBundle() throws Exception
	{
		JPPFBuffer buf = socketClient.receiveBytes(0);
		if (debugEnabled) log.debug("read  "+buf.getLength()+" bytes");
		ByteArrayInputStream bais = new ByteArrayInputStream(buf.getBuffer());
		DataInputStream dis = new DataInputStream(bais);
		JPPFTaskBundle bundle = (JPPFTaskBundle) helper.readNextObject(dis, false);
		bundle.setNodeExecutionTime(System.currentTimeMillis());
		int count = bundle.getTaskCount();
		if (!JPPFTaskBundle.State.INITIAL_BUNDLE.equals(bundle.getState()))
		{
			List<byte[]> taskList = new ArrayList<byte[]>();
			bundle.setDataProvider(helper.readNextBytes(dis));
			for (int i = 0; i < count; i++)
			{
				taskList.add(helper.readNextBytes(dis));
			}
			bundle.setTasks(taskList);
		}
		dis.close();
		if (debugEnabled) log.debug("read bundle with "+bundle.getTaskCount()+" tasks");
		return bundle;
	}

	/**
	 * Add a listener to the list of listener for this node.
	 * @param listener the listener to add.
	 * @see org.jppf.node.MonitoredNode#addNodeListener(org.jppf.node.event.NodeListener)
	 */
	public void addNodeListener(NodeListener listener)
	{
		if (listener == null) return;
		listeners.add(listener);
		notifying = true;
	}

	/**
	 * Remove a listener from the list of listener for this node.
	 * @param listener the listener to remove.
	 * @see org.jppf.node.MonitoredNode#removeNodeListener(org.jppf.node.event.NodeListener)
	 */
	public void removeNodeListener(NodeListener listener)
	{
		if (listener == null) return;
		listeners.remove(listener);
		if (listeners.size() <= 0) notifying = false;
	}

	/**
	 * Notify all listeners that an event has occurred.
	 * @param eventType the type of the event as an enumerated value.
	 * @see org.jppf.node.MonitoredNode#fireNodeEvent(org.jppf.node.event.NodeEvent.EventType)
	 */
	public void fireNodeEvent(EventType eventType)
	{
		NodeEvent event = new NodeEvent(eventType);
		for (NodeListener listener : listeners) listener.eventOccurred(event);
	}

	/**
	 * Stop this node and release the resources it is using.
	 * @param closeSocket determines whether the underlying socket should be closed.
	 * @see org.jppf.node.MonitoredNode#stopNode(boolean)
	 */
	public void stopNode(boolean closeSocket)
	{
		if (debugEnabled) log.debug("closing node");
		stopped = true;
		if (closeSocket)
		{
			try
			{
				if (debugEnabled) log.debug("closing socket: "+socket);
				socketClient.close();
			}
			catch(Exception ex)
			{
				log.error(ex.getMessage(), ex);
			}
			socket = null;
		}
		socketClient.setSocket(null);
		socketClient = null;
	}

	/**
	 * Get the underlying socket used by this socket wrapper.
	 * @return a Socket instance.
	 * @see org.jppf.node.MonitoredNode#getSocket()
	 */
	public Socket getSocket()
	{
		return socket;
	}
	
	/**
	 * Set the underlying socket to be used by this socket wrapper.
	 * @param socket a Socket instance.
	 * @see org.jppf.node.MonitoredNode#setSocket(java.net.Socket)
	 */
	public void setSocket(Socket socket)
	{
		this.socket = socket;
	}

	/**
	 * Get the underlying socket wrapper used by this node.
	 * @return a <code>SocketWrapper</code> instance.
	 */
	public SocketWrapper getSocketWrapper()
	{
		return socketClient;
	}
}
