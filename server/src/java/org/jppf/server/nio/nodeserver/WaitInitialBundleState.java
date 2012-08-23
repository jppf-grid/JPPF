/*
 * JPPF.
 * Copyright (C) 2005-2012 JPPF Team.
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
import org.jppf.server.JPPFContextDriver;
import org.jppf.server.nio.ChannelWrapper;
import org.jppf.server.protocol.*;
import org.jppf.server.scheduler.bundle.*;
import org.jppf.utils.JPPFConfiguration;
import org.slf4j.*;

/**
 * This class implements the state of receiving a task bundle from the node as a
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
  public WaitInitialBundleState(final NodeNioServer server)
  {
    super(server);
  }

  /**
   * Execute the action associated with this channel state.
   * @param channel the selection key corresponding to the channel and selector for this state.
   * @return a state transition as an <code>NioTransition</code> instance.
   * @throws Exception if an error occurs while transitioning to another state.
   * @see org.jppf.server.nio.NioState#performTransition(java.nio.channels.SelectionKey)
   */
  @Override
  public NodeTransition performTransition(final ChannelWrapper<?> channel) throws Exception
  {
    AbstractNodeContext context = (AbstractNodeContext) channel.getContext();
    if (debugEnabled) log.debug("exec() for " + channel);
    if (context.getMessage() == null) context.setMessage(context.newMessage());
    if (context.readMessage(channel))
    {
      if (debugEnabled) log.debug("read bundle for " + channel + " done");
      ServerJob bundleWrapper = context.deserializeBundle();
      JPPFTaskBundle bundle = (JPPFTaskBundle) bundleWrapper.getJob();
      String uuid = (String) bundle.getParameter(BundleParameter.NODE_UUID_PARAM);
      context.setUuid(uuid);
      server.putUuid(uuid, channel);
      Bundler bundler = server.getBundler().copy();
      JPPFSystemInformation systemInfo = (JPPFSystemInformation) bundle.getParameter(BundleParameter.SYSTEM_INFO_PARAM);
      context.setNodeInfo(systemInfo);
      if (bundler instanceof NodeAwareness) ((NodeAwareness) bundler).setNodeConfiguration(systemInfo);
      if (debugEnabled) log.debug("processing threads for node " + channel + " = " + (systemInfo == null ? "?" : systemInfo.getJppf().getInt("processing.threads", -1)));
      if( bundler instanceof ContextAwareness) ((ContextAwareness) bundler).setJPPFContext(JPPFContextDriver.getInstance());
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
          boolean ssl = false;
          if (channel.isLocal()) ssl = JPPFConfiguration.getProperties().getBoolean("jppf.ssl.enabled", false);
          else ssl = context.getSSLHandler() != null;
          JPPFManagementInfo info = new JPPFManagementInfo(host, port, id, isPeer ? JPPFManagementInfo.DRIVER : JPPFManagementInfo.NODE, ssl);
          if (systemInfo != null) info.setSystemInfo(systemInfo);
          driver.getNodeHandler().addNodeInformation(channel, info);
          driver.getInitializer().getNodeConnectionEventHandler().fireNodeConnected(info);
        }
      }
      // make sure the context is reset so as not to resubmit the last bundle executed by the node.
      context.setMessage(null);
      context.setBundle(null);
      server.getTaskQueueChecker().addIdleChannel(channel);
      return TO_IDLE;
    }
    return TO_WAIT_INITIAL;
  }
}
