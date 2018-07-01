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
import org.slf4j.*;

/**
 *
 * @author Laurent Cohen
 */
public class WaitInitialResponseState extends HeartbeatServerState {
  /**
   * Logger for this class.
   */
  private static final Logger log = LoggerFactory.getLogger(WaitInitialResponseState.class);
  /**
   * Determines whether the debug level is enabled in the log configuration, without the cost of a method call.
   */
  private static final boolean debugEnabled = log.isDebugEnabled();
  /**
   * Initialize this state.
   * @param server the server that handles this state.
   */
  public WaitInitialResponseState(final HeartbeatNioServer server) {
    super(server);
  }

  @Override
  public HeartbeatTransition performTransition(final ChannelWrapper<?> channel) throws Exception {
    final HeartbeatContext context = (HeartbeatContext) channel.getContext();
    if (context.readMessage(channel)) {
      final HeartbeatMessage data = context.getHeartbeatMessage();
      if (debugEnabled) log.debug("got {}", data);
      final String uuid = data.getUuid();
      context.setUuid(uuid);
      context.setHeartbeatMessage(null);
      context.setMessage(null);
      return HeartbeatTransition.TO_IDLE;
    }
    return HeartbeatTransition.TO_WAIT_INITIAL_RESPONSE;
  }
}
