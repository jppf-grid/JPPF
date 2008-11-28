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

package org.jppf.comm.discovery;

import java.net.*;
import java.nio.ByteBuffer;
import java.util.*;

import org.apache.commons.logging.*;
import org.jppf.utils.*;

/**
 * An instance of this class listens to messages broadcast by the driver
 * to get the driver connection host and ports.
 * @author Laurent Cohen
 */
public class JPPFMulticastReceiver
{
	/**
	 * Logger for this class.
	 */
	private static Log log = LogFactory.getLog(JPPFMulticastReceiver.class);
	/**
	 * Determines whether debug-level logging is enabled.
	 */
	private static boolean debugEnabled = log.isDebugEnabled();
	/**
	 * Reference to the JPPF configuration.
	 */
	private static TypedProperties props = JPPFConfiguration.getProperties();
	/**
	 * Multicast group to join.
	 */
	private	static String group = props.getString("jppf.broadcast.group", "230.0.0.1");
	/**
	 * Multicast port to listen to.
	 */
	private	static int port = props.getInt("jppf.broadcast.port", 44446);
	/**
	 * Multicast group to join.
	 */
	private static InetAddress groupInetAddress = null;

	/**
	 * Retrieve the driver connection information broadcast by the driver.
	 * @return a <code>DriverConnectionInformation</code> instance, or null
	 * if no broadcast information could be retrieved.
	 */
	public JPPFConnectionInformation receive()
	{
		JPPFConnectionInformation info = null;
		try
		{
			groupInetAddress = InetAddress.getByName(group);
			List<Inet4Address> addresses = NetworkUtils.getNonLocalIPV4Addresses();
			//List<Inet4Address> addresses = NetworkUtils.getIPV4Addresses();
			if (addresses.isEmpty()) addresses.add((Inet4Address) InetAddress.getByName("127.0.0.1"));
			int len = addresses.size();
			if (debugEnabled)
			{
				StringBuilder sb = new StringBuilder();
				sb.append("Found ").append(len).append(" address");
				if (len > 1) sb.append("es");
				sb.append(":");
				for (Inet4Address addr: addresses) sb.append(" ").append(addr.getHostAddress());
				log.debug(sb.toString());
			}
			Receiver[] receivers = new Receiver[len];
			for (int i=0; i<len; i++) receivers[i] = new Receiver(addresses.get(i), port);
			for (Receiver r: receivers) r.start();
			for (Receiver r: receivers)
			{
				r.join();
				if (r.getInfo() != null)
				{
					info = r.getInfo();
					break;
				}
			}
		}
		catch(Exception e)
		{
			log.error(e.getMessage(), e);
		}
		if (debugEnabled) log.debug("Auto-discovery of the driver connection information: " + info);
		return info;
	}

	/**
	 * Instances of this class attempt to receive broadcast connection information
	 * from a multicast socket bound to a specified address.
	 */
	public static class Receiver extends Thread
	{
		/**
		 * Address the multicast socket is bound to.
		 */
		private InetAddress addr = null;
		/**
		 * Port the multicast socket listens to.
		 */
		private int port = 0;
		/**
		 * Connection information retrieved by the multicast socket.
		 */
		private JPPFConnectionInformation info = null;

		/**
		 * Initialize this Receiver with the specified address and port.
		 * @param addr address the multicast socket is bound to.
		 * @param port port the multicast socket listens to.
		 */
		public Receiver(InetAddress addr, int port)
		{
			this.addr = addr;
			this.port = port;
		}

		/**
		 * 
		 * @see java.lang.Runnable#run()
		 */
		public void run()
		{
			MulticastSocket socket = null;
			try
			{
				socket = new MulticastSocket(new InetSocketAddress(addr, port));
				socket.setInterface(addr);
				socket.joinGroup(groupInetAddress);
				byte[] buf = new byte[512];
				DatagramPacket packet = new DatagramPacket(buf, buf.length);
				int timeout = props.getInt("jppf.broadcast.timeout", 5000);
				socket.setSoTimeout(timeout);
				socket.receive(packet);
				ByteBuffer buffer = ByteBuffer.wrap(buf);
				int len = buffer.getInt();
				byte[] bytes = new byte[len];
				buffer.get(bytes);
				info = JPPFConnectionInformation.fromBytes(bytes);
				socket.leaveGroup(groupInetAddress);
			}
			catch(SocketTimeoutException e)
			{
				if (debugEnabled) log.debug(e.getMessage(), e);
			}
			catch(Exception e)
			{
				log.error(e.getMessage(), e);
			}
			if (socket != null) socket.close();
		}

		/**
		 * Get the connection information retrieved by the multicast socket.
		 * @return a <code>JPPFConnectionInformation</code> instance or null if no information could be retrieved.
		 */
		public JPPFConnectionInformation getInfo()
		{
			return info;
		}
	}
}
