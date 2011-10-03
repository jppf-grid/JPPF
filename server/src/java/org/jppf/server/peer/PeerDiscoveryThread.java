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

package org.jppf.server.peer;

import java.net.InetAddress;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

import org.jppf.comm.discovery.*;
import org.jppf.server.nio.classloader.ClassNioServer;
import org.jppf.utils.*;
import org.slf4j.*;

/**
 * Instances of this class discover peer drivers over the network.
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
	private final Set<JPPFConnectionInformation> infoSet = new HashSet<JPPFConnectionInformation>();
	/**
	 * Count of distinct retrieved connection informaiton objects.
	 */
	private final AtomicInteger count = new AtomicInteger(0);
	/**
	 * Connection information for this JPPF driver.
	 */
	private final JPPFConnectionInformation localInfo;
    /**
     * JPPF class server
     */
    private final ClassNioServer classServer;

    /**
	 * Default constructor.
     * @param localInfo Connection information for this JPPF driver.
     * @param classServer JPPF class server
     */
	public PeerDiscoveryThread(final JPPFConnectionInformation localInfo, final ClassNioServer classServer)
	{
        if(localInfo == null) throw new IllegalArgumentException("localInfo is null");
        if(classServer == null) throw new IllegalArgumentException("classServer is null");

        this.localInfo = localInfo;
        this.classServer = classServer;
	}

	/**
	 * Lookup server configurations from UDP multicasts.
	 * @see java.lang.Runnable#run()
	 */
	@Override
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
	public synchronized void addPeer(final JPPFConnectionInformation info)
	{
		if (debugEnabled) log.debug("Found peer connection information: " + info);
		infoSet.add(info);
        String name = "Peer-" + count.incrementAndGet();

		new JPPFPeerInitializer(name, info, classServer).start();
	}

	/**
	 * Determine whether the specified connection information refers to this driver.
	 * This situation may arise if the host has multiple network interfaces, each with its own IP address.
	 * Making thios distinction is important to prevent a driver from connecting to itself.
	 * @param info the peer's connection information.
	 * @return true if the host/port combination in the connection information can be resolved
	 * as the configuration for this driver.
	 */
	private boolean isSelf(final JPPFConnectionInformation info)
	{
		List<InetAddress> ipv4Addresses = NetworkUtils.getIPV4Addresses();
		for (InetAddress addr: ipv4Addresses)
		{
			String ip = addr.getHostAddress();
			if (info.host.equals(ip) && Arrays.equals(info.serverPorts, localInfo.serverPorts))
			{
				return true;
			}
		}
		return false;
	}

	/**
	 * Remove a disconnected peer.
     * @param connectionInfo connection info of the peer to remove
	 */
	public synchronized void removePeer(final JPPFConnectionInformation connectionInfo)
	{
        infoSet.remove(connectionInfo);
	}
}
