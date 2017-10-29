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

package org.jppf.jmxremote.nio;

import java.util.Queue;
import java.util.concurrent.LinkedBlockingQueue;

import javax.management.*;

import org.jppf.io.*;
import org.jppf.jmxremote.JPPFMBeanServerConnection;
import org.jppf.jmxremote.message.*;
import org.jppf.nio.*;
import org.slf4j.*;

/**
 *
 * @author Laurent Cohen
 */
public class JMXContext extends SimpleNioContext<JMXState> {
  /**
   * Logger for this class.
   */
  private static Logger log = LoggerFactory.getLogger(JMXContext.class);
  /**
   * Determines whether the debug level is enabled in the log configuration, without the cost of a method call.
   */
  //private static boolean debugEnabled = log.isDebugEnabled();
  /**
   * Whether the associated channel performs read operations ({@code true}) or write operations ({@code false}).
   */
  private boolean reading;
  /**
   * The last read or written non-serialized JMX message.
   */
  private JMXMessage currentJmxMessage;
  /**
   * The queue of pending messages to send.
   */
  private final Queue<JMXMessage> pendingJmxMessages = new LinkedBlockingQueue<>();
  /**
   * The JMX nio server to use.
   */
  private final JMXNioServer server;
  /**
   * Server port on which the connection was established.
   */
  private int serverPort = -1;
  /**
   * The object that handles messages correlations.
   */
  private JMXMessageHandler messageHandler;
  /**
   * The associated MBeanServer (server-side).
   */
  private MBeanServer mbeanServer;
  /**
   * The associated MBeanServerConnection (clientr-side).
   */
  private JPPFMBeanServerConnection mbeanServerConnection;

  /**
   * Initializewitht he specified server.
   * @param server the JMX nio server to use.
   */
  public JMXContext(final JMXNioServer server) {
    this.server = server;
  }

  @Override
  public void handleException(final ChannelWrapper<?> channel, final Exception exception) {
    try {
      server.closeConnection(channel);
    } catch (Exception e) {
      log.error(e.getMessage(), e);
    }
  }

  /**
   * @return whether the associated channel performs read operations ({@code true}) or write operations ({@code false}).
   */
  public boolean isReading() {
    return reading;
  }

  /**
   * Specify whether the associated channel performs read operations ({@code true}) or write operations ({@code false}).
   * @param reading {@code true} for read operations, {@code false} otherwise.
   * @return this context, for method chaining.
   */
  public JMXContext setReading(final boolean reading) {
    this.reading = reading;
    return this;
  }

  /**
   * @return the connection id.
   */
  public String getConnectionID() {
    return getConnectionUuid();
  }

  /**
   * Set the connection id.
   * @param connectionID the connection id to set.
   * @return this context, for method chaining.
   */
  public JMXContext setConnectionID(final String connectionID) {
    setConnectionUuid(connectionID);
    return this;
  }

  /**
   * Deserialize the last read message.
   * @return a deserialzed message.
   * @throws Exception if any error occurs.
   */
  public JMXMessage deserializeMessage() throws Exception {
    currentJmxMessage = null;
    if (message != null) {
      BaseNioMessage msg = (BaseNioMessage) message;
      DataLocation dl = msg.getLocations().get(0);
      currentJmxMessage = (JMXMessage) IOHelper.unwrappedData(dl);
    }
    return currentJmxMessage;
  }

  /**
   * Serialize the current message message.
   * @param channel the channel to which the serialized message is written.
   * @throws Exception if any error occurs.
   */
  public void serializeMessage(final ChannelWrapper<?> channel) throws Exception {
    BaseNioMessage msg = (BaseNioMessage) (message = new BaseNioMessage(channel));
    DataLocation dl = IOHelper.serializeData(currentJmxMessage);
    msg.addLocation(dl);
  }

  /**
   * @return the last read or written non-serialized JMX message.
   */
  public JMXMessage getCurrentJmxMessage() {
    return currentJmxMessage;
  }

  /**
   * Set the next to read or write non-serialized JMX message.
   * @param jmxMessage the next to read or write non-serialized JMX message.
   */
  public void setCurrentJmxMessage(final JMXMessage jmxMessage) {
    this.currentJmxMessage = jmxMessage;
  }

  /**
   * Add a JMX message to the pending queue.
   * @param msg the JMX message to offer.
   * @throws Exception if any error occurs.
   */
  public void offerJmxMessage(final JMXMessage msg) throws Exception {
    pendingJmxMessages.offer(msg);
  }

  /**
   * Get the next JMX message from the pending queue.
   * @return a {@link JMXMessage} instance.
   */
  public JMXMessage pollJmxMessage() {
    return pendingJmxMessages.poll();
  }

  /**
   * @return the server port on which the connection was established.
   */
  public int getServerPort() {
    return serverPort;
  }

  /**
   * Set the server port on which the connection was established.
   * @param serverPort the port number.
   * @return this context, for method chaining.
   */
  public JMXContext setServerPort(final int serverPort) {
    this.serverPort = serverPort;
    return this;
  }

  /**
   *
   * @return the object that handles messages correlations.
   */
  public JMXMessageHandler getMessageHandler() {
    return messageHandler;
  }

  /**
   * Set the object that handles messages correlations.
   * @param messageHandler a {@link JMXMessageHandler} instance.
   * @return this context, for method chaining.
   */
  public JMXContext setMessageHandler(final JMXMessageHandler messageHandler) {
    this.messageHandler = messageHandler;
    return this;
  }

  /**
   * @return the associated MBeanServer (server-side), if any.
   */
  public MBeanServer getMbeanServer() {
    return mbeanServer;
  }

  /**
   * Set the associated MBeanServer (server-side).
   * @param mbeanServer a {@link MBeanServer} instance.
   * @return this context, for method chaining.
   */
  public JMXContext setMbeanServer(final MBeanServer mbeanServer) {
    this.mbeanServer = mbeanServer;
    return this;
  }

  /**
   * @return the associated MBeanServerConnection (client-side).
   */
  public JPPFMBeanServerConnection getMbeanServerConnection() {
    return mbeanServerConnection;
  }

  /**
   * Set the associated MBeanServerConnection (client-side).
   * @param mbeanServerConnection the {@link MBeanServerConnection} to set.
   * @return this context, for method chaining.
   */
  public JMXContext setMbeanServerConnection(final JPPFMBeanServerConnection mbeanServerConnection) {
    this.mbeanServerConnection = mbeanServerConnection;
    return this;
  }
}
