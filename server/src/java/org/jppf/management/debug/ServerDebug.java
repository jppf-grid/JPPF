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

package org.jppf.management.debug;

import java.nio.channels.*;
import java.util.*;

import org.apache.commons.logging.*;
import org.jppf.server.JPPFDriver;
import org.jppf.server.nio.StateTransitionManager;
import org.jppf.server.nio.nodeserver.*;


/**
 * MBean interface for server debugging utilities.
 * @author Laurent Cohen
 */
public class ServerDebug  implements ServerDebugMBean
{
	/**
	 * Logger for this class.
	 */
	private static Log log = LogFactory.getLog(ServerDebug.class);
	/**
	 * Determines whether DEBUG logging level is enabled.
	 */
	private static boolean debugEnabled = log.isDebugEnabled();

	/**
	 * {@inheritDoc}
	 */
	public String[] allIdleNodes()
	{
		List<SelectableChannel> idleChannels = JPPFDriver.getInstance().getNodeNioServer().getIdleChannels();
		String[] result = null;
		synchronized(idleChannels)
		{
			result = new String[idleChannels.size()];
			int i=0;
			for (SelectableChannel channel: idleChannels) result[i++] = channel.toString();
		}
		return result;
	}

	/**
	 * {@inheritDoc}
	 */
	public String[] allTaskProcessingKeys()
	{
		NodeNioServer server = JPPFDriver.getInstance().getNodeNioServer();
		StateTransitionManager mgr = server.getTransitionManager();
		return keysAsArray(mgr.getProcessingKeys());
	}

	/**
	 * {@inheritDoc}
	 */
	public String[] allTaskServerKeys()
	{
		String[] result = null;
		try
		{
			NodeNioServer server = JPPFDriver.getInstance().getNodeNioServer();
			result = keysAsArray(server.getSelector().keys());
		}
		catch(Exception e)
		{
			log.error(e.getMessage(), e);
		}
		return result;
	}

	/**
	 * Convert a set of keys into an array of readable strings. 
	 * @param set the keys to convert.
	 * @return an array of strings.
	 */
	private String[] keysAsArray(Set<SelectionKey> set)
	{
		String[] result = new String[set.size()];
		int i = 0;
		for (SelectionKey key: set)
		{
			SelectableChannel channel = key.channel();
			NodeContext ctx = (NodeContext) key.attachment();
			result[i++] = "state=" + (ctx == null ? "null_context" : ctx.getState()) + ", intOps=" + key.interestOps() + ",channel=" + channel;
		}
		return result;
	}
}
