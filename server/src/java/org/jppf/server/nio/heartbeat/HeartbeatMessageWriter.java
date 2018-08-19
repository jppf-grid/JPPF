/*
 * JPPF.
 * Copyright (C) 2005-2018 JPPF Team.
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
import org.jppf.jmx.JMXEnvHelper;
import org.jppf.utils.*;
import org.jppf.utils.configuration.JPPFProperties;
import org.slf4j.Logger;

/**
 * Writes all messages in the channel's context queue, if any, or keeps writing the current message until the socket send buffer is full.
 * @author Laurent Cohen
 */
public class HeartbeatMessageWriter {
  /**
   * Logger for this class.
   */
  private static final Logger log = LoggingUtils.getLogger(HeartbeatMessageWriter.class, JMXEnvHelper.isAsyncLoggingEnabled());
  /**
   * Determines whether the debug level is enabled in the log configuration, without the cost of a method call.
   */
  private static final boolean debugEnabled = log.isDebugEnabled();

  /**
   * Write to the specified channel.
   * @param context the context to write to.
   * @return {@code true} if there is something left to write, {@code false} otherwise. 
   * @throws Exception if any errort occurs.
   */
  static boolean write(final HeartbeatContext context) throws Exception {
    if (context.isSsl()) {
      synchronized(context.getSocketChannel()) {
        return doWrite(context);
      }
    }
    return doWrite(context);
  }

  /**
   * Write to the specified channel.
   * @param context the context to write to.
   * @return {@code true} if there is something left to write, {@code false} otherwise. 
   * @throws Exception if any errort occurs.
   */
  private static boolean doWrite(final HeartbeatContext context) throws Exception {
    if (context.getMessage() == null) {
      final HeartbeatMessage data = context.newHeartbeatMessage();
      if (context.getUuid() == null) {
        final TypedProperties config = JPPFConfiguration.getProperties();
        final TypedProperties props = data.getProperties();
        props.set(JPPFProperties.RECOVERY_MAX_RETRIES, config.get(JPPFProperties.RECOVERY_MAX_RETRIES));
        props.set(JPPFProperties.RECOVERY_READ_TIMEOUT, config.get(JPPFProperties.RECOVERY_READ_TIMEOUT));
        props.set(JPPFProperties.RECOVERY_ENABLED, config.get(JPPFProperties.RECOVERY_ENABLED));
      }
      context.createMessage(data);
    }
    if (context.writeMessage(null)) {
      if (debugEnabled) log.debug("fully sent message {} from context {}", context.getHeartbeatMessage(), context);
      context.setHeartbeatMessage(null);
      context.setMessage(null);
      return false;
    }
    return true;
  }
}
