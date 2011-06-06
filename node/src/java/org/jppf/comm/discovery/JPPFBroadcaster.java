/*
 * JPPF.
 * Copyright (C) 2005-2011 JPPF Team.
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

package org.jppf.comm.discovery;

import java.net.*;
import java.nio.ByteBuffer;
import java.util.*;

import org.jppf.utils.*;
import org.slf4j.*;

/**
 * Instances of this class broadcast JPPF driver information at regular intervals,
 * to a configured UDP multicast group, to enable automatic discovery by clients,
 * nodes and peer drivers.
 * @author Laurent Cohen
 */
public class JPPFBroadcaster extends ThreadSynchronization implements Runnable
{
	/**
	 * Logger for this class.
	 */
	private static Logger log = LoggerFactory.getLogger(JPPFBroadcaster.class);
	/**
	 * Determines whether debug-level logging is enabled.
	 */
	private static boolean debugEnabled = log.isDebugEnabled();
	/**
	 * The UDP sockets to broadcast to, each bound to a different network interface.
	 */
	private List<Pair<MulticastSocket, DatagramPacket>> socketsInfo = new ArrayList<Pair<MulticastSocket, DatagramPacket>>();
	/**
	 * Frequency of the broadcast in milliseconds.
	 */
	private long broadcastInterval = 1000L;
	/**
	 * Holds the driver connection information to broadcast.
	 */
	private JPPFConnectionInformation info = null;

	/**
	 * Initialize this broadcaster using the server configuration information.
	 * @param info holds the driver connection information to broadcast.
	 */
	public JPPFBroadcaster(JPPFConnectionInformation info)
	{
		this.info = info;
	}

	/**
	 * Initialize the broadcast socket and data.
	 * @throws Exception if an error occurs while initializing the datagram packet or socket.
	 */
	private void init() throws Exception
	{
		TypedProperties props = JPPFConfiguration.getProperties();
		broadcastInterval = props.getLong("jppf.discovery.broadcast.interval", 1000L);
		String group = props.getString("jppf.discovery.group", "230.0.0.1");
		int port = props.getInt("jppf.discovery.port", 11111);

		List<InetAddress> addresses = NetworkUtils.getNonLocalIPV4Addresses();
		if (addresses.isEmpty()) addresses.add((Inet4Address) InetAddress.getByName("127.0.0.1"));
		if (debugEnabled)
		{
			StringBuilder sb = new StringBuilder();
			sb.append("Found ").append(addresses.size()).append(" address");
			if (addresses.size() > 1) sb.append("es");
			sb.append(":");
			for (InetAddress addr: addresses) sb.append(" ").append(addr.getHostAddress());
			log.debug(sb.toString());
		}
		for (InetAddress addr: addresses)
		{
			try
			{
				JPPFConnectionInformation ci = (JPPFConnectionInformation) info.clone();
				ci.host = addr.getHostAddress();
				ci.subnetMaskLength = NetworkUtils.getSubnetMaskLength(addr);
				byte[] infoBytes = JPPFConnectionInformation.toBytes(ci);
				ByteBuffer buffer = ByteBuffer.wrap(new byte[512]);
				buffer.putInt(infoBytes.length);
				buffer.put(infoBytes);
				DatagramPacket packet = new DatagramPacket(buffer.array(), 512, InetAddress.getByName(group), port);
				MulticastSocket socket = new MulticastSocket(port);
				socket.setInterface(addr);
				socketsInfo.add(new Pair<MulticastSocket, DatagramPacket>(socket, packet));
			}
			catch(Exception e)
			{
				log.error("Unable to bind to interface " + addr.getHostAddress() + " on port " + port, e);
			}
		}
	}

	/**
	 * 
	 * @see java.lang.Runnable#run()
	 */
	public void run()
	{
		try
		{
			init();
		}
		catch(Exception e)
		{
			log.error(e.getMessage(), e);
			setStopped(true);
		}
		while (!isStopped())
		{
			Iterator<Pair<MulticastSocket, DatagramPacket>> it = socketsInfo.iterator();
			while (it.hasNext())
			{
				try
				{
					Pair<MulticastSocket, DatagramPacket> socketInfo = it.next();
					socketInfo.first().send(socketInfo.second());
				}
				catch(Exception e)
				{
					log.error(e.getMessage(), e);
					it.remove();
				}
			}
			if (socketsInfo.isEmpty()) setStopped(true);
			if (!isStopped())
			{
				try
				{
					Thread.sleep(broadcastInterval);
				}
				catch(InterruptedException e)
				{
					log.error(e.getMessage(), e);
				}
			}
		}
		for (Pair<MulticastSocket, DatagramPacket> socketInfo: socketsInfo) socketInfo.first().close();
		socketsInfo.clear();
	}
}
