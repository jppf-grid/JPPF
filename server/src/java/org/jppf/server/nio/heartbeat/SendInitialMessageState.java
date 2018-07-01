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
import org.jppf.utils.*;
import org.jppf.utils.configuration.*;
import org.slf4j.*;

/**
 *
 * @author Laurent Cohen
 */
public class SendInitialMessageState extends HeartbeatServerState {
  /**
   * Logger for this class.
   */
  private static final Logger log = LoggerFactory.getLogger(SendInitialMessageState.class);
  /**
   * Determines whether the debug level is enabled in the log configuration, without the cost of a method call.
   */
  private static final boolean debugEnabled = log.isDebugEnabled();
  /**
   * Initialize this state.
   * @param server the server that handles this state.
   */
  public SendInitialMessageState(final HeartbeatNioServer server) {
    super(server);
  }

  @Override
  public HeartbeatTransition performTransition(final ChannelWrapper<?> channel) throws Exception {
    final HeartbeatContext context = (HeartbeatContext) channel.getContext();
    if (context.getMessage() == null) {
      final HeartbeatMessage data = context.newHeartbeatMessage();
      final TypedProperties config = JPPFConfiguration.getProperties();
      final TypedProperties props = data.getProperties();
      props.set(JPPFProperties.RECOVERY_MAX_RETRIES, config.get(JPPFProperties.RECOVERY_MAX_RETRIES));
      props.set(JPPFProperties.RECOVERY_READ_TIMEOUT, config.get(JPPFProperties.RECOVERY_READ_TIMEOUT));
      props.set(JPPFProperties.RECOVERY_ENABLED, config.get(JPPFProperties.RECOVERY_ENABLED));
      context.createMessage(data);
    }
    if (context.writeMessage(channel)) {
      if (debugEnabled) log.debug("sent {}", context.getHeartbeatMessage());
      context.setHeartbeatMessage(null);
      context.setMessage(null);
      return HeartbeatTransition.TO_WAIT_INITIAL_RESPONSE;
    }
    return HeartbeatTransition.TO_SEND_INITIAL_MESSAGE;
  }
}
