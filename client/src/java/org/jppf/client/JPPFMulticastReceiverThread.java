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
package org.jppf.client;

import java.net.InetAddress;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

import org.jppf.comm.discovery.*;
import org.jppf.utils.*;
import org.slf4j.*;

/**
 * This class listens to information broadcast by JPPF servers on the network and uses it
 * to establish a connection with one or more servers. 
 */
class JPPFMulticastReceiverThread extends ThreadSynchronization implements Runnable
{
	/**
	 * Logger for this class.
	 */
	private static Logger log = LoggerFactory.getLogger(JPPFMulticastReceiverThread.class);
	/**
	 * Determines whether debug-level logging is enabled.
	 */
	private static boolean debugEnabled = log.isDebugEnabled();
	/**
	 * Contains the set of retrieved connection information objects.
	 */
	private Map<String, Set<JPPFConnectionInformation>> infoMap = new HashMap<String, Set<JPPFConnectionInformation>>();
	/**
	 * Count of distinct retrieved connection information objects.
	 */
	private AtomicInteger count = new AtomicInteger(0);
	/**
	 * The JPPF client for which servers are discovered.
	 */
	private AbstractGenericClient client = null;
	/**
	 * Determines whether we keep the addresses of all discovered network interfaces for the ssame driver,
	 * or if we only use the first one that is discovered.
	 */
	private boolean acceptMultipleInterfaces = JPPFConfiguration.getProperties().getBoolean("jppf.discovery.acceptMultipleInterfaces", false);

	/**
	 * Initialize this discovery thread with the specified JPPF client.
	 * @param client the JPPF client for which servers are discovered.
	 */
	JPPFMulticastReceiverThread(AbstractGenericClient client)
	{
		this.client = client;
	}

	/**
	 * Lookup server configurations from UDP multicasts.
	 * @see java.lang.Runnable#run()
	 */
	public void run()
	{
		TypedProperties config = client.getConfig();
		JPPFMulticastReceiver receiver = new JPPFMulticastReceiver(new IPFilter(config));
		try
		{
			while (!isStopped())
			{
				JPPFConnectionInformation info = receiver.receive();
				if (info == null)
				{
					//setStopped(true);
					//log.error("Abnormal situation: connection information should not be null");
					//break;
					continue;
				}
				InetAddress ip = InetAddress.getByName(info.host);
				if (!hasConnectionInformation(info))
				{
					if (debugEnabled) log.debug("Found connection information: " + info);
					addConnectionInformation(info);
					int n = config.getInt("jppf.pool.size", 1);
					if (n < 1) n = 1;
					int currentCount = count.incrementAndGet();
					for (int i=1; i<=n; i++)
					{
						String name = "driver-" + currentCount  + (n == 1 ? "" : "-" + i);
						AbstractJPPFClientConnection c = client.createConnection(info.uuid, name, info);
						client.newConnection(c);
					}
				}
			}
		}
		catch(Exception e)
		{
			log.error(e.getMessage(), e);
		}
		finally
		{
			if (receiver != null) receiver.setStopped(true);
		}
	}

	/**
	 * Detrmine whether a connection information object is already present in the map.
	 * @param info the connection information to lookup.
	 * @return true if the connection information is in the map, false otherwise.
	 */
	private boolean hasConnectionInformation(JPPFConnectionInformation info)
	{
		if (acceptMultipleInterfaces)
		{
			Set<JPPFConnectionInformation> set = infoMap.get(info.uuid);
			if (set == null) return false;
			return set.contains(info);
		}
		return infoMap.get(info.uuid) != null;
	}

	/**
	 * Add the specified connection information to the map.
	 * @param info a {@link JPPFConnectionInformation} instance.
	 */
	private void addConnectionInformation(JPPFConnectionInformation info)
	{
		Set<JPPFConnectionInformation> set = infoMap.get(info.uuid);
		if (set == null)
		{
			set = new HashSet<JPPFConnectionInformation>();
			infoMap.put(info.uuid, set);
		}
		set.add(info);
	}
}
