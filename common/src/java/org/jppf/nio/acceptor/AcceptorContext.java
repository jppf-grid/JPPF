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

package org.jppf.nio.acceptor;

import org.jppf.nio.*;
import org.jppf.serialization.SerializationUtils;
import org.jppf.utils.JPPFIdentifiers;
import org.jppf.utils.stats.*;

/**
 * Context associated with a channel serving tasks to a node.
 * @author Laurent Cohen
 */
public class AcceptorContext extends SimpleNioContext<AcceptorState> {
  /**
   * Identifier for the channel.
   */
  private int id = JPPFIdentifiers.UNKNOWN;
  /**
   * Contains the data read from the socket channel.
   */
  private NioObject nioObject = null;
  /**
   * The acceptor server.
   */
  private final AcceptorNioServer server;
  /**
   * The statsistics to update, if any.
   */
  private final JPPFStatistics stats;

  /**
   * 
   * @param server the acceptor server.
   */
  public AcceptorContext(final AcceptorNioServer server) {
    this(server, null);
  }

  /**
   * 
   * @param server the acceptor server.
   * @param stats the statsistics to update, if any.
   */
  public AcceptorContext(final AcceptorNioServer server, final JPPFStatistics stats) {
    this.server = server;
    this.stats = stats;
  }

  /**
   * Read data from a channel. This method reads a single integer which identifies the type of the channel.
   * @param wrapper the channel to read the data from.
   * @return true if all the data has been read, false otherwise.
   * @throws Exception if an error occurs while reading the data.
   * @see org.jppf.utils.JPPFIdentifiers
   */
  @Override
  public boolean readMessage(final ChannelWrapper<?> wrapper) throws Exception {
    if (nioObject == null) nioObject = new PlainNioObject(wrapper, 4);
    boolean b = false;
    try {
      b = nioObject.read();
    } catch (Exception e) {
      if (stats != null) stats.addValue(JPPFStatisticsHelper.UNIDENTIFIED_IN_TRAFFIC, nioObject.getChannelCount());
      throw e;
    }
    if (b) {
      id = SerializationUtils.readInt(nioObject.getData().getInputStream());
      if (stats != null) stats.addValue(JPPFStatisticsHelper.UNIDENTIFIED_IN_TRAFFIC, nioObject.getChannelCount());
      nioObject = null;
    }
    return b;
  }

  @Override
  public void handleException(final ChannelWrapper<?> channel, final Exception e) {
    server.closeChannel(channel);
  }

  /**
   * get the identifier for the channel.
   * @return the identifier as an int value.
   */
  public int getId() {
    return id;
  }
}
