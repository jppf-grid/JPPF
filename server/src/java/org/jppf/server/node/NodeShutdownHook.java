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

package org.jppf.server.node;

import org.apache.commons.logging.*;
import org.jppf.comm.socket.SocketWrapper;

/**
 * This thread is run when the node process is killed brutally, either through System.exit()
 * or via a Ctrl-C or a kill process command.
 * Its goal is to properly shutdown the connection to the server before exiting. 
 * @author Laurent Cohen
 */
public class NodeShutdownHook extends Thread
{
	/**
	 * Logger for this class.
	 */
	private static Log log = LogFactory.getLog(NodeShutdownHook.class);
	/**
	 * Determines whether the debug level is enabled in the logging configuration, without the cost of a method call.
	 */
	private static boolean debugEnabled = log.isDebugEnabled();
	/**
	 * The node for which to close the socket.
	 */
	private JPPFNode node = null;
	
	/**
	 * Initiialize this hook with the specified node.
	 * @param node the node for which to close the socket.
	 */
	public NodeShutdownHook(JPPFNode node)
	{
		super("Node Shutdown Hook");
		this.node = node;
	}

	/**
	 * Run this hook.
	 * @see java.lang.Runnable#run()
	 */
	public void run()
	{
		try
		{
			log.info("Executing shutdown hook");
			SocketWrapper wrapper = node.getSocketWrapper();
			if ((wrapper != null) && wrapper.isOpened())
			{
				try
				{
					wrapper.write(new byte[] { (byte) 0xFF }, 0, 1);
					wrapper.flush();
				}
				catch(Exception e)
				{
					if (debugEnabled) log.debug(e.getMessage(), e);
				}
				wrapper.close();
			}
		}
		catch(Exception e)
		{
			log.error(e.getMessage(), e);
		}
	}
}
