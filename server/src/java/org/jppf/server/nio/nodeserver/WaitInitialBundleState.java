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

package org.jppf.server.nio.nodeserver;

import static org.jppf.server.protocol.BundleParameter.*;
import static org.jppf.server.nio.nodeserver.NodeTransition.*;
import static org.jppf.utils.StringUtils.getRemoteHost;

import java.net.InetAddress;
import java.nio.channels.*;

import org.apache.commons.logging.*;
import org.jppf.server.*;
import org.jppf.server.protocol.*;
import org.jppf.server.scheduler.bundle.BundlerFactory;

/**
 * This class implements the state of receiving information from the node as a
 * response to sending the initial bundle.
 * @author Laurent Cohen
 */
public class WaitInitialBundleState extends NodeServerState
{
	/**
	 * Log4j logger for this class.
	 */
	protected static final Log LOG = LogFactory.getLog(WaitInitialBundleState.class);
	/**
	 * Determines whether DEBUG logging level is enabled.
	 */
	protected static final boolean DEBUG_ENABLED = LOG.isDebugEnabled();

	/**
	 * Initialize this state.
	 * @param server the server that handles this state.
	 */
	public WaitInitialBundleState(NodeNioServer server)
	{
		super(server);
	}

	/**
	 * Execute the action associated with this channel state.
	 * @param key the selection key corresponding to the channel and selector for this state.
	 * @return a state transition as an <code>NioTransition</code> instance.
	 * @throws Exception if an error occurs while transitioning to another state.
	 * @see org.jppf.server.nio.NioState#performTransition(java.nio.channels.SelectionKey)
	 */
	public NodeTransition performTransition(SelectionKey key) throws Exception
	{
		SocketChannel channel = (SocketChannel) key.channel();
		NodeContext context = (NodeContext) key.attachment();
		if (DEBUG_ENABLED) LOG.debug("exec() for " + getRemoteHost(channel));
		if (context.readMessage(channel))
		{
			if (DEBUG_ENABLED) LOG.debug("read bundle for " + getRemoteHost(channel) + " done");
			JPPFTaskBundle bundle = context.deserializeBundle();
			context.setUuid(bundle.getBundleUuid());
			boolean override = bundle.getParameter(BUNDLE_TUNING_TYPE_PARAM) != null;
			if (override) context.setBundler(BundlerFactory.createBundler(bundle.getParametersMap(), true));
			else context.setBundler(server.getBundler().copy());
			Boolean isPeer = (Boolean) bundle.getParameter(IS_PEER);
			if ((isPeer == null) || !isPeer)
			{
				InetAddress addr = channel.socket().getInetAddress();
				if (addr != null)
				{
					String host = addr.getHostName();
					int port = (Integer) bundle.getParameter(NODE_MANAGEMENT_PORT_PARAM);
					NodeManagementInfo info = new NodeManagementInfo(host, port);
					JPPFDriver.getInstance().addNodeInformation(channel, info);
				}
			}
			// make sure the context is reset so as not to resubmit the last bundle executed by the node.
			context.setMessage(null);
			context.setBundle(null);
			server.addIdleChannel(channel);
			return TO_IDLE;
		}
		return TO_WAIT_INITIAL;
	}
}
