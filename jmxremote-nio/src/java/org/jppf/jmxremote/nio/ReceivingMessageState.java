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

import static org.jppf.jmxremote.nio.JMXTransition.*;

import javax.management.*;

import org.jppf.jmxremote.message.*;
import org.jppf.nio.ChannelWrapper;
import org.slf4j.*;

/**
 *
 * @author Laurent Cohen
 */
public class ReceivingMessageState extends JMXNioState {
  /**
   * Logger for this class.
   */
  private static Logger log = LoggerFactory.getLogger(ReceivingMessageState.class);
  /**
   * Determines whether the debug level is enabled in the log configuration, without the cost of a method call.
   */
  private static boolean debugEnabled = log.isDebugEnabled();
  /**
   *
   * @param server the server which handles the channels states and transitions.
   */
  public ReceivingMessageState(final JMXNioServer server) {
    super(server);
  }

  @Override
  public JMXTransition performTransition(final ChannelWrapper<?> channel) throws Exception {
    JMXContext context = (JMXContext) channel.getContext();
    if (context.readMessage(channel)) {
      JMXMessage msg = context.deserializeMessage();
      context.setMessage(null);
      if (debugEnabled) log.debug("read message = {} from context = {}", msg, context);
      if (msg instanceof JMXRequest) handleRequest(context, (JMXRequest) msg);
      else if (msg instanceof JMXResponse) handleResponse(context, (JMXResponse) msg);
    }
    return TO_RECEIVING_MESSAGE;
  }

  /**
   * Handle a received reqUest.
   * @param the JMX nio context.
   * @param requests the request to process.
   * @throws Exception if any error occurs.
   */
  private void handleRequest(final JMXContext context, final JMXRequest request) throws Exception {
    if (debugEnabled) log.debug("handling request = {} on context = {}", request, context);
    Exception exception = null;
    Object result = null;
    Object[] params = request.getParams();
    MBeanServer mbs = context.getMbeanServer();
    try {
      switch(request.getMessageType()) {
        case CONNECT:
          result = context.getConnectionID();
          break;
        case INVOKE:
          result = mbs.invoke((ObjectName) params[0], (String) params[1], (Object[]) params[2], (String[]) params[3]);
          break;
        case GET_ATTRIBUTE:
          result = mbs.getAttribute((ObjectName) params[0], (String) params[1]);
          break;
        case GET_ATTRIBUTES:
          result = mbs.getAttributes((ObjectName) params[0], (String[]) params[1]);
          break;
        case SET_ATTRIBUTE:
          mbs.setAttribute((ObjectName) params[0], (Attribute) params[1]);
          break;
        case SET_ATTRIBUTES:
          result = mbs.setAttributes((ObjectName) params[0], (AttributeList) params[1]);
          break;
        case CREATE_MBEAN:
          result = mbs.createMBean((String) params[0], (ObjectName) params[1]);
          break;
        case CREATE_MBEAN_PARAMS:
          result = mbs.createMBean((String) params[0], (ObjectName) params[1], (Object[]) params[2], (String[]) params[3]);
          break;
        case CREATE_MBEAN_LOADER:
          result = mbs.createMBean((String) params[0], (ObjectName) params[1], (ObjectName) params[2]);
          break;
        case CREATE_MBEAN_LOADER_PARAMS:
          result = mbs.createMBean((String) params[0], (ObjectName) params[1], (ObjectName) params[2], (Object[]) params[3], (String[]) params[4]);
          break;
        case GET_DEFAULT_DOMAIN:
          result = mbs.getDefaultDomain();
          break;
        case GET_DOMAINS:
          result = mbs.getDomains();
          break;
        case GET_MBEAN_COUNT:
          result = mbs.getMBeanCount();
          break;
        case GET_MBEAN_INFO:
          result = mbs.getMBeanInfo((ObjectName) params[0]);
          break;
        case GET_OBJECT_INSTANCE:
          result = mbs.getObjectInstance((ObjectName) params[0]);
          break;
        case IS_INSTANCE_OF:
          result = mbs.isInstanceOf((ObjectName) params[0], (String) params[1]);
          break;
        case IS_REGISTERED:
          result = mbs.isRegistered((ObjectName) params[0]);
          break;
        case QUERY_MBEANS:
          result = mbs.queryMBeans((ObjectName) params[0], (QueryExp) params[1]);
          break;
        case QUERY_NAMES:
          result = mbs.queryNames((ObjectName) params[0], (QueryExp) params[1]);
          break;
        case UNREGISTER_MBEAN:
          mbs.unregisterMBean((ObjectName) params[0]);
          break;
        case ADD_NOTIFICATION_LISTENERS:
          break;
        case ADD_NOTIFICATION_LISTENER_OBJECTNAME:
          break;
        case REMOVE_NOTIFICATION_LISTENER:
          break;
        case REMOVE_NOTIFICATION_LISTENER_FILTER_HANDBACK:
          break;
        case REMOVE_NOTIFICATION_LISTENER_OBJECTNAME:
          break;
        case REMOVE_NOTIFICATION_LISTENER_OBJECTNAME_FILTER_HANDBACK:
          break;
      }
    } catch (Exception e) {
      exception = e;
    }
    ChannelWrapper<?> writingChannel = context.getMessageHandler().getChannels().writingChannel();
    JMXContext writingContext = (JMXContext) writingChannel.getContext();
    JMXResponse response = new JMXResponse(request.getMessageID(), request.getMessageType(), result, exception);
    if (debugEnabled) log.debug("sending response = {} on context = {}", response, context);
    writingContext.offerJmxMessage(response);
    synchronized(writingChannel) {
      if (writingContext.getState() == JMXState.IDLE) {
        server.getTransitionManager().transitionChannel(writingChannel, JMXTransition.TO_SENDING_MESSAGE);
        transitionChannel(writingChannel, JMXTransition.TO_SENDING_MESSAGE);
      }
    }
  }

  /**
   * Handle a received reqUest.
   * @param the JMX nio context.
   * @param requests the request to process.
   * @throws Exception if any error occurs.
   */
  private void handleResponse(final JMXContext context, final JMXResponse response) throws Exception {
    context.getMessageHandler().responseReceived(response);
  }
}
