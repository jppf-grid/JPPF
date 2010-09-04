/*
 * JPPF.
 * Copyright (C) 2005-2010 JPPF Team.
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

package org.jppf.server.peer;

import java.net.Inet4Address;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

import org.jppf.comm.discovery.*;
import org.jppf.server.JPPFDriver;
import org.jppf.utils.*;
import org.slf4j.*;

/**
 * 
 * @author Laurent Cohen
 */
public class PeerDiscoveryThread extends ThreadSynchronization implements Runnable
{
	/**
	 * Logger for this class.
	 */
	private static Logger log = LoggerFactory.getLogger(PeerNode.class);
	/**
	 * Determines whether the debug level is enabled in the logging configuration, without the cost of a method call.
	 */
	private static boolean debugEnabled = log.isDebugEnabled();
	/**
	 * Contains the set of retrieved connection information objects.
	 */
	private Set<JPPFConnectionInformation> infoSet = new HashSet<JPPFConnectionInformation>();
	/**
	 * Count of distinct retrieved connection informaiton objects.
	 */
	private AtomicInteger count = new AtomicInteger(0);
	/**
	 * Connection information for this JPPF driver.
	 */
	private JPPFConnectionInformation localInfo = null;
	/**
	 * Map of peer server conneciton information to their name.
	 */
	private Map<String, JPPFConnectionInformation> peersMap = new HashMap<String, JPPFConnectionInformation>();

	/**
	 * Default constructor.
	 */
	public PeerDiscoveryThread()
	{
		localInfo = JPPFDriver.getInstance().createConnectionInformation();
	}

	/**
	 * Lookup server configurations from UDP multicasts.
	 * @see java.lang.Runnable#run()
	 */
	public void run()
	{
		try
		{
			JPPFMulticastReceiver receiver = new JPPFMulticastReceiver();
			while (!isStopped())
			{
				JPPFConnectionInformation info = receiver.receive();
				if ((info != null) && !infoSet.contains(info) && !info.equals(localInfo) && !isSelf(info)) addPeer(info);
			}
		}
		catch(Exception e)
		{
			log.error(e.getMessage(), e);
		}
	}

	/**
	 * Add a newly found peer.
	 * @param info the peer's connection information.
	 */
	public synchronized void addPeer(JPPFConnectionInformation info)
	{
		if (debugEnabled) log.debug("Found peer connection information: " + info);
		infoSet.add(info);
		String name = "Peer-" + count.incrementAndGet();
		peersMap.put(name, info);
		int n = count.get();
		TypedProperties props = JPPFConfiguration.getProperties();
		String peerNames = (n <= 0) ? "" : props.getString("jppf.peers", "").trim();
		String[] allNames = peerNames.split("\\s");
		StringBuilder sb = new StringBuilder();
		if (allNames.length > 0)
		{
			for (String s: allNames)
			{
				if (!name.equals(s)) sb.append(s).append(" ");
			}
		}
		sb.append(name);
		props.setProperty("jppf.peers", sb.toString());
		props.setProperty("jppf.peer." + name + ".server.host", info.host);
		props.setProperty("class.peer."+name+".server.port", "" + info.classServerPorts[0]);
		props.setProperty("node.peer."+name+".server.port", "" + info.nodeServerPorts[0]);
		new JPPFPeerInitializer(name).start();
	}

	/**
	 * Determine whether the specified connection information refers to this driver.
	 * This situation may arise if the host has multiple network interfaces, each with its own IP address.
	 * Making thios distinction is important to prevent a driver from connecting to itself.
	 * @param info the peer's connection information.
	 * @return true if the host/port combination in the connection information can be resolved
	 * as the configuration for this driver.
	 */
	private boolean isSelf(JPPFConnectionInformation info)
	{
		List<Inet4Address> ipv4Addresses = NetworkUtils.getIPV4Addresses();
		for (Inet4Address addr: ipv4Addresses)
		{
			String ip = addr.getHostAddress();
			if (info.host.equals(ip) && Arrays.equals(info.classServerPorts, localInfo.classServerPorts))
			{
				return true;
			}
		}
		return false;
	}

	/**
	 * Remove a disconnected peer.
	 * @param name the name of the peer to remove.
	 */
	public synchronized void removePeer(String name)
	{
		JPPFConnectionInformation info = peersMap.get(name);
		if (info != null)
		{
			infoSet.remove(info);
			peersMap.remove(name);
		}
	}
}
