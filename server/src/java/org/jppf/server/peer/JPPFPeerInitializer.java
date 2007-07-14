/*
 * Java Parallel Processing Framework.
 * Copyright (C) 2005-2007 JPPF Team.
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
package org.jppf.server.peer;

import org.apache.commons.logging.*;


/**
 * Instances of this class are used to initialize the connections to a peer driver
 * in a separate thread.
 * @author Laurent Cohen
 */
public class JPPFPeerInitializer extends Thread
{
	/**
	 * Log4j logger for this class.
	 */
	private static Log log = LogFactory.getLog(JPPFPeerInitializer.class);
	/**
	 * Name of the peer in the configuration file.
	 */
	private String peerName = null;

	/**
	 * Initialize this peer initializer from a specified peerName.
	 * @param peerName the name of the peer in the configuration file.
	 */
	public JPPFPeerInitializer(String peerName)
	{
		this.peerName = peerName;
		setName("Peer Initializer ["+peerName+"]");
	}

	/**
	 * Perform the peer initialization.
	 * @see java.lang.Thread#run()
	 */
	public void run()
	{
		log.info("start initialization of peer ["+peerName+"]");
		try
		{
			new PeerResourceProvider(peerName).init();
			new PeerNode(peerName).run();
		}
		catch(Exception e)
		{
			log.error(e.getMessage(), e);
		}
		log.info("end initialization of peer ["+peerName+"]");
	}
}
