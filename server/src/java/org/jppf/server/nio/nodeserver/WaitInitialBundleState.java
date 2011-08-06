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

package org.jppf.server.nio.nodeserver;

import static org.jppf.server.nio.nodeserver.NodeTransition.*;

import org.jppf.management.*;
import org.jppf.server.nio.ChannelWrapper;
import org.jppf.server.protocol.*;
import org.jppf.server.scheduler.bundle.*;
import org.jppf.utils.*;
import org.slf4j.*;

/**
 * This class implements the state of receiving information from the node as a
 * response to sending the initial bundle.
 * @author Laurent Cohen
 */
class WaitInitialBundleState extends NodeServerState
{
	/**
	 * Logger for this class.
	 */
	protected static Logger log = LoggerFactory.getLogger(WaitInitialBundleState.class);
	/**
	 * Determines whether DEBUG logging level is enabled.
	 */
	protected static boolean debugEnabled = log.isDebugEnabled();

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
	 * @param wrapper the selection key corresponding to the channel and selector for this state.
	 * @return a state transition as an <code>NioTransition</code> instance.
	 * @throws Exception if an error occurs while transitioning to another state.
	 * @see org.jppf.server.nio.NioState#performTransition(java.nio.channels.SelectionKey)
	 */
	public NodeTransition performTransition(ChannelWrapper<?> wrapper) throws Exception
	{
		AbstractNodeContext context = (AbstractNodeContext) wrapper.getContext();
		if (debugEnabled) log.debug("exec() for " + wrapper);
		if (context.getNodeMessage() == null) context.setNodeMessage(context.newMessage(), wrapper);
		if (context.readMessage(wrapper))
		{
			if (debugEnabled) log.debug("read bundle for " + wrapper + " done");
			BundleWrapper bundleWrapper = context.deserializeBundle();
			JPPFTaskBundle bundle = bundleWrapper.getBundle();
			context.setUuid(bundle.getBundleUuid());
			String uuid = (String) bundle.getParameter(BundleParameter.NODE_UUID_PARAM);
			context.setNodeUuid(uuid);
			server.putUuid(uuid, wrapper);
			Bundler bundler = server.getBundler().copy();
			JPPFSystemInformation systemInfo = (JPPFSystemInformation) bundle.getParameter(BundleParameter.NODE_SYSTEM_INFO_PARAM);
			if (bundler instanceof NodeAwareness) ((NodeAwareness) bundler).setNodeConfiguration(systemInfo);
			if (debugEnabled)
			{
				if (systemInfo == null) log.debug("system info for node is null");
				else
				{
					TypedProperties jppf = systemInfo.getJppf();
					if (jppf == null) log.debug("jppf config for node is not available");
					else log.debug("processing threads for node " + wrapper + " = " + jppf.getInt("processing.threads", -1));
				}
			}
			bundler.setup();
			context.setBundler(bundler);
			boolean isPeer = (Boolean) bundle.getParameter(BundleParameter.IS_PEER, Boolean.FALSE);
			context.setPeer(isPeer);
			if (JPPFConfiguration.getProperties().getBoolean("jppf.management.enabled", true))
			{
				String id = (String) bundle.getParameter(BundleParameter.NODE_MANAGEMENT_ID_PARAM);
				if (id != null)
				{
					String host = (String) bundle.getParameter(BundleParameter.NODE_MANAGEMENT_HOST_PARAM);
					int port = (Integer) bundle.getParameter(BundleParameter.NODE_MANAGEMENT_PORT_PARAM);
					JPPFManagementInfo info = new JPPFManagementInfo(host, port, id, isPeer ? JPPFManagementInfo.DRIVER : JPPFManagementInfo.NODE);
					if (systemInfo != null) info.setSystemInfo(systemInfo);
					driver.getNodeHandler().addNodeInformation(wrapper, info);
				}
			}
			// make sure the context is reset so as not to resubmit the last bundle executed by the node.
			context.setNodeMessage(null, wrapper);
			context.setBundle(null);
			server.addIdleChannel(wrapper);
			return TO_IDLE;
		}
		return TO_WAIT_INITIAL;
	}
}
