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

import org.apache.log4j.Logger;


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
	private static Logger log = Logger.getLogger(JPPFPeerInitializer.class);
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
