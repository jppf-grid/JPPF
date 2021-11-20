/*
 * JPPF.
 * Copyright (C) 2005-2019 JPPF Team.
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
package org.jppf.server.node.remote;

import org.jppf.JPPFNodeReconnectionNotification;
import org.jppf.comm.recovery.*;
import org.jppf.node.connection.*;
import org.jppf.server.node.*;
import org.jppf.utils.*;
import org.jppf.utils.concurrent.ThreadUtils;
import org.jppf.utils.configuration.JPPFProperties;
import org.jppf.utils.hooks.HookFactory;
import org.slf4j.*;

/**
 * Instances of this class encapsulate execution nodes.
 * @since 5.1
 * @author Laurent Cohen
 */
public abstract class AbstractRemoteNode extends JPPFNode implements HeartbeatConnectionListener {
  /**
   * Logger for this class.
   */
  private static Logger log = LoggerFactory.getLogger(AbstractRemoteNode.class);
  /**
   * Determines whether the debug level is enabled in the logging configuration, without the cost of a method call.
   */
  private static boolean debugEnabled = log.isDebugEnabled();
  /**
   * Connection to the recovery server.
   */
  private HeartbeatConnection recoveryConnection;
  /**
   * Server connection information.
   */
  private final DriverConnectionInfo connectionInfo;

  /**
   * Default constructor.
   * @param uuid this node's uuid.
   * @param configuration the configuration of this node.
   * @param connectionInfo the server connection information.
   * @param hookFactory used to create and invoke hook instances.
   */
  public AbstractRemoteNode(final String uuid, final TypedProperties configuration, final DriverConnectionInfo connectionInfo, final HookFactory hookFactory) {
    super(uuid, configuration, hookFactory);
    this.connectionInfo = connectionInfo;
    initClassLoaderManager();
  }

  /**
   * Create and initialize the {@link AbstractClassLoaderManager class loader manager}.
   */
  protected abstract void initClassLoaderManager();

  @Override
  public void initDataChannel() throws Exception {
    (nodeConnection = new RemoteNodeConnection(connectionInfo, serializer, this)).init();
    if (nodeIO == null) nodeIO = new RemoteNodeIO(this);
    if (configuration.get(JPPFProperties.RECOVERY_ENABLED)) {
      if (recoveryConnection == null) {
        if (debugEnabled) log.debug("Initializing recovery");
        recoveryConnection = new HeartbeatConnection(JPPFIdentifiers.NODE_HEARTBEAT_CHANNEL, uuid, connectionInfo.getHost(), connectionInfo.getPort(), connectionInfo.isSecure(), configuration);
        recoveryConnection.addClientConnectionListener(this);
        ThreadUtils.startThread(recoveryConnection, "HeartbeatConnection");
      }
    }
  }

  @Override
  public void closeDataChannel() throws Exception {
    if (debugEnabled) log.debug("closing data channel: nodeConnection=" + nodeConnection + ", recoveryConnection=" + recoveryConnection);
    if (nodeConnection != null) nodeConnection.close();
    if (recoveryConnection != null) {
      final HeartbeatConnection tmp = recoveryConnection;
      if (tmp != null) {
        recoveryConnection = null;
        tmp.close();
      }
    }
  }

  @Override
  public void heartbeatConnectionFailed(final HeartbeatConnectionEvent event) {
    try {
      if (debugEnabled) log.debug("recovery connection failed, attempting to reconnect this node");
      reconnectionNotification = new JPPFNodeReconnectionNotification("The heartbeat mechanism failed to receive a message from the server", null, ConnectionReason.HEARTBEAT_FAILURE);
      getExecutionManager().cancelAllTasks(false, false);
      closeDataChannel();
    } catch (final Exception e) {
      log.error(e.getMessage(), e);
    }
  }

  @Override
  public boolean isLocal() {
    return false;
  }
}
