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

import java.util.concurrent.atomic.*;

import org.jppf.comm.recovery.HeartbeatMessage;
import org.jppf.io.IOHelper;
import org.jppf.nio.*;
import org.jppf.server.JPPFDriver;
import org.jppf.server.nio.nodeserver.AbstractNodeContext;
import org.jppf.utils.*;
import org.slf4j.*;

/**
 * 
 * @author Laurent Cohen
 */
class HeartbeatContext extends AbstractNioContext<HeartbeatState> {
  /**
   * Logger for this class.
   */
  private static final Logger log = LoggerFactory.getLogger(HeartbeatContext.class);
  /**
   * Determines whether the debug level is enabled in the log configuration, without the cost of a method call.
   */
  private static final boolean debugEnabled = log.isDebugEnabled();
  /**
   * Uuid of the remote peer.
   */
  final AtomicLong messageSequence = new AtomicLong(0L);
  /**
   * The current message to send or the last read one.
   */
  private HeartbeatMessage heartbeatMessage;
  /**
   * Whether this channel has been scheduled for emitting heartbeat messages.
   */
  private final AtomicBoolean submitted = new AtomicBoolean(false);
  /**
   * The server that handles this context.
   */
  private final HeartbeatNioServer server;

  /**
   * 
   * @param server the server that handles this context.
   */
  HeartbeatContext(final HeartbeatNioServer server) {
    this.server = server;
  }

  @Override
  public boolean readMessage(final ChannelWrapper<?> wrapper) throws Exception {
    if (message == null) message = new SimpleNioMessage(wrapper);
    final boolean b = message.read();
    if (b) deserializeData();
    return b;
  }

  @Override
  public boolean writeMessage(final ChannelWrapper<?> wrapper) throws Exception {
    if (message == null) createMessage(newHeartbeatMessage());
    return message.write();
  }

  /**
   * Deserialize the heartbeat data from the last read message.
   * @return the deserialized data.
   * @throws Exception if any error occurs.
   */
  HeartbeatMessage deserializeData() throws Exception {
    if (message == null) return null;
    if (heartbeatMessage == null) heartbeatMessage = (HeartbeatMessage) IOHelper.unwrappedData(((SimpleNioMessage) message).getCurrentDataLocation());
    return getHeartbeatMessage();
  }

  /**
   * Create a message to send with the specified data.
   * @param data the data to send.
   * @return the newly created message.
   * @throws Exception if any error occurs.
   */
  public NioMessage createMessage(final HeartbeatMessage data) throws Exception {
    heartbeatMessage = data;
    message = new SimpleNioMessage(this);
    data.setUuid(uuid);
    ((SimpleNioMessage) message).setCurrentDataLocation(IOHelper.serializeData(data));
    return message;
  }

  /**
   * Create a message to send with the specified data.
   * @return the newly created heartbeat message.
   */
  public HeartbeatMessage newHeartbeatMessage() {
    return new HeartbeatMessage(messageSequence.incrementAndGet());
  }

  /**
   * Called when the remote peer failed to respond to a heartbeat message.
   */
  void heartbeatFailed() {
    if (debugEnabled) log.debug("node {} failed to respond to heartbeat messages, closing the associated node channels", this);
    final JPPFDriver driver = JPPFDriver.getInstance();
    final AbstractNodeContext nodeContext = driver.getNodeNioServer().getConnection(uuid);
    if (nodeContext != null) driver.getNodeNioServer().connectionFailed(nodeContext.getChannel());
    final ChannelWrapper<?> nodeClassChannel = driver.getNodeClassServer().getNodeConnection(uuid);
    if (nodeClassChannel != null) driver.getNodeClassServer().connectionFailed(nodeClassChannel);
    handleException(getChannel(), null);
  }

  @Override
  public void handleException(final ChannelWrapper<?> channel, final Exception e) {
    if (e == null) log.info("closing heartbeat channel {}", this);
    else {
      if (debugEnabled) log.debug("closing heartbeat channel {} due to exception:\n{}", this, ExceptionUtils.getStackTrace(e));
      else log.warn("closing heartbeat channel {} due to exception: {}", this, ExceptionUtils.getMessage(e));
    }
    server.closeConnection(channel);
  }

  /**
   * @return the current message to send or the last read one.
   */
  public HeartbeatMessage getHeartbeatMessage() {
    return heartbeatMessage;
  }

  /**
   * Set the current message.
   * @param heartbeatMessage the current message to set.
   */
  void setHeartbeatMessage(final HeartbeatMessage heartbeatMessage) {
    this.heartbeatMessage = heartbeatMessage;
  }

  /**
   * @return whether this channel has been scheduled for emitting heartbeat messages.
   */
  public AtomicBoolean getSubmitted() {
    return submitted;
  }
}
