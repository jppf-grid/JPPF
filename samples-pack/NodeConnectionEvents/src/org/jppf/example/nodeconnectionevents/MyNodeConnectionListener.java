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

package org.jppf.example.nodeconnectionevents;

import org.jppf.management.*;
import org.jppf.server.event.*;



/**
 * This is a test of a driver startup class.
 * @author Laurent Cohen
 */
public class MyNodeConnectionListener implements NodeConnectionListener
{
	/**
	 * Default no-arg constructor.
	 */
	public MyNodeConnectionListener()
	{
		System.out.println("*** MyNodeConnectionListener instance successfully loaded ***");
	}

	/**
	 * {@inheritDoc}
	 */
	public void nodeConnected(NodeConnectionEvent event)
	{
		System.out.println("*** Node " + computeIdentifier(event.getNodeInformation()) + " is now connected ***");
	}

	/**
	 * {@inheritDoc}
	 */
	public void nodeDisconnected(NodeConnectionEvent event)
	{
		System.out.println("*** Node " + computeIdentifier(event.getNodeInformation()) + " is disconnected ***");
	}

	/**
	 * Build a node identifier based on its manageent host and port.
	 * @param info the inforamtion from which to extract the identifier segments.
	 * @return a string in the format 'management_host:management_port'.
	 */
	private String computeIdentifier(JPPFManagementInfo info)
	{
		JPPFSystemInformation systemInfo = info.getSystemInfo();
		String host = info.getHost();
		int port = info.getPort();
		return host + ':' + port;
	}
}
