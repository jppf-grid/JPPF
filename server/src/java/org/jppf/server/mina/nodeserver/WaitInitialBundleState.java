/*
 * JPPF.
 * Copyright (C) 2005-2009 JPPF Team.
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

package org.jppf.server.mina.nodeserver;

import static org.jppf.server.nio.nodeserver.NodeTransition.TO_IDLE;
import static org.jppf.utils.StringUtils.getRemoteHost;

import org.apache.commons.logging.*;
import org.apache.mina.core.session.IoSession;
import org.jppf.io.BundleWrapper;
import org.jppf.management.*;
import org.jppf.server.JPPFDriver;
import org.jppf.server.protocol.*;
import org.jppf.server.scheduler.bundle.Bundler;
import org.jppf.utils.JPPFConfiguration;

/**
 * This class implements the state of receiving information from the node as a
 * response to sending the initial bundle.
 * @author Laurent Cohen
 */
public class WaitInitialBundleState extends NodeServerState
{
	/**
	 * Logger for this class.
	 */
	protected static Log log = LogFactory.getLog(WaitInitialBundleState.class);
	/**
	 * Determines whether DEBUG logging level is enabled.
	 */
	protected static boolean debugEnabled = log.isDebugEnabled();

	/**
	 * Initialize this state.
	 * @param server the server that handles this state.
	 */
	public WaitInitialBundleState(MinaNodeServer server)
	{
		super(server);
	}

	/**
	 * Execute the action associated with this channel state.
	 * @param session the current session to which the state applies.
	 * @return true if the transition could be applied, false otherwise.
	 * @throws Exception if an error occurs while transitioning to another state.
	 * @see org.jppf.server.nio.NioState#performTransition(java.nio.channels.SelectionKey)
	 */
	public boolean startTransition(IoSession session) throws Exception
	{
		return true;
	}

	/**
	 * End the transition associated with this channel state.
	 * @param session the current session to which the state applies.
	 * @throws Exception if an error occurs while transitioning to another state.
	 * @see org.jppf.server.mina.nodeserver.NodeServerState#endTransition(org.apache.mina.core.session.IoSession)
	 */
	public void endTransition(IoSession session) throws Exception
	{
		if (debugEnabled) log.debug("read bundle for " + getRemoteHost(session.getRemoteAddress()) + " done");
		NodeContext context = getContext(session);
		BundleWrapper bundleWrapper = context.deserializeBundle();
		JPPFTaskBundle bundle = bundleWrapper.getBundle();
		context.setUuid(bundle.getBundleUuid());
		context.setNodeUuid((String) bundle.getParameter(BundleParameter.NODE_UUID_PARAM));
		Bundler bundler = server.getBundler().copy();
		bundler.setup();
		context.setBundler(bundler);
		Boolean b = (Boolean) bundle.getParameter(BundleParameter.IS_PEER);
		boolean isPeer = (b != null) && b;
		context.setPeer(isPeer);
		if (JPPFConfiguration.getProperties().getBoolean("jppf.management.enabled", true))
		{
			String id = (String) bundle.getParameter(BundleParameter.NODE_MANAGEMENT_ID_PARAM);
			if (id != null)
			{
				String host = (String) bundle.getParameter(BundleParameter.NODE_MANAGEMENT_HOST_PARAM);
				int port = (Integer) bundle.getParameter(BundleParameter.NODE_MANAGEMENT_PORT_PARAM);
				JPPFManagementInfo info = new JPPFManagementInfo(host, port, id, isPeer ? JPPFManagementInfo.DRIVER : JPPFManagementInfo.NODE);
				JPPFSystemInformation systemInfo = (JPPFSystemInformation) bundle.getParameter(BundleParameter.NODE_SYSTEM_INFO_PARAM);
				if (systemInfo != null) info.setSystemInfo(systemInfo);
				JPPFDriver.getInstance().addNodeInformation(new IoSessionWrapper(session), info);
			}
		}
		// make sure the context is reset so as not to resubmit the last bundle executed by the node.
		context.setNodeMessage(null);
		context.setBundle(null);
		server.addIdleChannel(session);
		server.transitionSession(session, TO_IDLE);
	}
}
