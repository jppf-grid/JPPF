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

package org.jppf.server.nio.heartbeat;

import org.jppf.comm.recovery.HeartbeatMessage;
import org.jppf.nio.ChannelWrapper;

/**
 * In this state, the server is reading a heartbeat response message from the node.
 * @author Laurent Cohen
 */
class WaitResponseState extends HeartbeatServerState {
  /**
   * Initialize this state.
   * @param server the server that handles this state.
   */
  public WaitResponseState(final HeartbeatNioServer server) {
    super(server);
  }

  @Override
  public HeartbeatTransition performTransition(final ChannelWrapper<?> channel) throws Exception {
    final HeartbeatContext context = (HeartbeatContext) channel.getContext();
    if (context.readMessage(channel)) {
      final HeartbeatMessage data = context.getHeartbeatMessage();
      context.setHeartbeatMessage(null);
      context.setMessage(null);
      server.getMessageHandler().responseReceived(data);
      return HeartbeatTransition.TO_IDLE;
    }
    return HeartbeatTransition.TO_WAIT_RESPONSE;
  }
}
