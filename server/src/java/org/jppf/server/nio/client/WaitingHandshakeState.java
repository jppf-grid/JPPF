/*
 * JPPF.
 * Copyright (C) 2005-2017 JPPF Team.
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

package org.jppf.server.nio.client;

import static org.jppf.server.nio.client.ClientTransition.*;

import java.util.List;

import org.jppf.management.JMXServer;
import org.jppf.nio.ChannelWrapper;
import org.jppf.node.protocol.*;
import org.jppf.server.nio.classloader.client.ClientClassNioServer;
import org.jppf.server.protocol.ServerTaskBundleClient;
import org.jppf.utils.LoggingUtils;
import org.slf4j.*;

/**
 * This class performs performs the work of reading a task bundle execution response from a node.
 * @author Laurent Cohen
 */
class WaitingHandshakeState extends ClientServerState
{
  /**
   * Logger for this class.
   */
  private static Logger log = LoggerFactory.getLogger(WaitingHandshakeState.class);
  /**
   * Determines whether DEBUG logging level is enabled.
   */
  private static boolean debugEnabled = LoggingUtils.isDebugEnabled(log);

  /**
   * Initialize this state.
   * @param server the server that handles this state.
   */
  public WaitingHandshakeState(final ClientNioServer server)
  {
    super(server);
  }

  /**
   * Execute the action associated with this channel state.
   * @param channel the selection key corresponding to the channel and selector for this state.
   * @return a state transition as an <code>NioTransition</code> instance.
   * @throws Exception if an error occurs while transitioning to another state.
   */
  @Override
  public ClientTransition performTransition(final ChannelWrapper<?> channel) throws Exception
  {
    ClientContext context = (ClientContext) channel.getContext();
    if (context.getClientMessage() == null) context.setClientMessage(context.newMessage());
    if (context.readMessage(channel))
    {
      ServerTaskBundleClient bundleWrapper = context.deserializeBundle();
      TaskBundle header = bundleWrapper.getJob();
      if (debugEnabled) log.debug("read handshake bundle " + header + " from client " + channel);
      context.setConnectionUuid((String) header.getParameter(BundleParameter.CONNECTION_UUID));
      header.getUuidPath().incPosition();
      String uuid = header.getUuidPath().getCurrentElement();
      context.setUuid(uuid);
      // wait until a class loader channel is up for the same client uuid
      ClientClassNioServer classServer = driver.getClientClassServer();
      List<ChannelWrapper<?>> list = classServer.getProviderConnections(uuid);
      while ((list == null) || list.isEmpty())
      {
        Thread.sleep(1L);
        list = classServer.getProviderConnections(uuid);
      }
      String driverUUID = driver.getUuid();
      header.getUuidPath().add(driverUUID);
      if (debugEnabled) log.debug("uuid path=" + header.getUuidPath());

      context.setClientMessage(null);
      context.setBundle(bundleWrapper);
      header.clear();
      // send system info (and more) back to the client
      header.setParameter(BundleParameter.SYSTEM_INFO_PARAM, driver.getSystemInformation());
      header.setParameter(BundleParameter.DRIVER_UUID_PARAM, driverUUID);
      JMXServer jmxServer = driver.getInitializer().getJmxServer(false);
      header.setParameter(BundleParameter.DRIVER_MANAGEMENT_PORT, jmxServer != null ? jmxServer.getManagementPort(): -1);
      jmxServer = driver.getInitializer().getJmxServer(true);
      header.setParameter(BundleParameter.DRIVER_MANAGEMENT_PORT_SSL, jmxServer != null ? jmxServer.getManagementPort(): -1);
      return TO_SENDING_HANDSHAKE_RESULTS;
    }
    return TO_WAITING_HANDSHAKE;
  }
}
