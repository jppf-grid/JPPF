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

import static org.jppf.jmxremote.nio.JMXState.RECEIVING_MESSAGE;
import static org.jppf.jmxremote.message.JMXMessageType.*;

import java.io.IOException;
import java.nio.channels.*;

import javax.management.*;

import org.jppf.jmxremote.JMXEnvHelper;
import org.jppf.jmxremote.message.*;
import org.jppf.nio.ChannelWrapper;
import org.jppf.utils.LoggingUtils;
import org.slf4j.Logger;

/**
 * Reads the next message, if any, or the current uncomplete message.
 * @author Laurent Cohen
 */
public class ReceivingMessageState extends JMXNioState {
  /**
   * Logger for this class.
   */
  private static Logger log = LoggingUtils.getLogger(ReceivingMessageState.class, JMXEnvHelper.isAsyncLoggingEnabled());
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
    JMXContext context = ((JMXChannelWrapper) channel).context;
    ChannelsPair pair = context.getChannels();
    //if (pair.isClosed() || pair.isClosing()) return null;
    while (true) {
      boolean b = false;
      try {
        b = context.readMessage(channel);
      } catch (IOException e) {
        if (pair.isClosed() || pair.isClosing()) return null;
        else throw e;
      }
      if (b) {
        JMXMessage msg = context.deserializeMessage();
        context.setMessage(null);
        if (debugEnabled) log.debug("read message = {} from context = {}", msg, context);
        if (msg instanceof JMXRequest) handleRequest(context, (JMXRequest) msg);
        else if (msg instanceof JMXResponse) handleResponse(context, (JMXResponse) msg);
        else if (msg instanceof JMXNotification) handleNotification(context, (JMXNotification) msg);
      } else if (context.byteCount <= 0L) break;
    }
    return context.transitionChannel(RECEIVING_MESSAGE, SelectionKey.OP_READ, true);
  }

  /**
   * Handle a reqUest received from a client.
   * @param context the JMX nio context.
   * @param request the request to process.
   * @throws Exception if any error occurs.
   */
  private void handleRequest(final JMXContext context, final JMXRequest request) throws Exception {
    if (debugEnabled) log.debug("handling request = {} from {}", request, context);
    Exception exception = null;
    Object result = null;
    Object[] p = request.getParams();
    MBeanServer mbs = context.getChannels().getMbeanServer();
    try {
      switch(request.getMessageType()) {
        case CONNECT: result = context.getConnectionID();
          break;
        case CLOSE: context.getChannels().disableReadWrite();
          jmxServer.closeConnection(context.getConnectionID(), null);
          break;
        case INVOKE: result = mbs.invoke((ObjectName) p[0], (String) p[1], (Object[]) p[2], (String[]) p[3]);
          break;
        case GET_ATTRIBUTE: result = mbs.getAttribute((ObjectName) p[0], (String) p[1]);
          break;
        case GET_ATTRIBUTES: result = mbs.getAttributes((ObjectName) p[0], (String[]) p[1]);
          break;
        case SET_ATTRIBUTE: mbs.setAttribute((ObjectName) p[0], (Attribute) p[1]);
          break;
        case SET_ATTRIBUTES: result = mbs.setAttributes((ObjectName) p[0], (AttributeList) p[1]);
          break;
        case CREATE_MBEAN: result = mbs.createMBean((String) p[0], (ObjectName) p[1]);
          break;
        case CREATE_MBEAN_PARAMS: result = mbs.createMBean((String) p[0], (ObjectName) p[1], (Object[]) p[2], (String[]) p[3]);
          break;
        case CREATE_MBEAN_LOADER: result = mbs.createMBean((String) p[0], (ObjectName) p[1], (ObjectName) p[2]);
          break;
        case CREATE_MBEAN_LOADER_PARAMS: result = mbs.createMBean((String) p[0], (ObjectName) p[1], (ObjectName) p[2], (Object[]) p[3], (String[]) p[4]);
          break;
        case GET_DEFAULT_DOMAIN: result = mbs.getDefaultDomain();
          break;
        case GET_DOMAINS: result = mbs.getDomains();
          break;
        case GET_MBEAN_COUNT: result = mbs.getMBeanCount();
          break;
        case GET_MBEAN_INFO: result = mbs.getMBeanInfo((ObjectName) p[0]);
          break;
        case GET_OBJECT_INSTANCE: result = mbs.getObjectInstance((ObjectName) p[0]);
          break;
        case IS_INSTANCE_OF: result = mbs.isInstanceOf((ObjectName) p[0], (String) p[1]);
          break;
        case IS_REGISTERED: result = mbs.isRegistered((ObjectName) p[0]);
          break;
        case QUERY_MBEANS: result = mbs.queryMBeans((ObjectName) p[0], (QueryExp) p[1]);
          break;
        case QUERY_NAMES: result = mbs.queryNames((ObjectName) p[0], (QueryExp) p[1]);
          break;
        case UNREGISTER_MBEAN: mbs.unregisterMBean((ObjectName) p[0]);
          break;
        case ADD_NOTIFICATION_LISTENER: result = jmxServer.getServerNotificationHandler().addNotificationListener(mbs, context.getConnectionID(), (ObjectName) p[0], (NotificationFilter) p[1]);
          break;
        case ADD_NOTIFICATION_LISTENER_OBJECTNAME: mbs.addNotificationListener((ObjectName) p[0], (ObjectName) p[1], (NotificationFilter) p[2], p[3]);
          break;
        case REMOVE_NOTIFICATION_LISTENER: jmxServer.getServerNotificationHandler().removeNotificationListeners(mbs, (ObjectName) p[0], (int[]) p[1]);
          break;
        case REMOVE_NOTIFICATION_LISTENER_FILTER_HANDBACK: jmxServer.getServerNotificationHandler().removeNotificationListeners(mbs, (ObjectName) p[0], new int[] { (Integer) p[1] });
          break;
        case REMOVE_NOTIFICATION_LISTENER_OBJECTNAME: mbs.removeNotificationListener((ObjectName) p[0], (ObjectName) p[1]);
          break;
        case REMOVE_NOTIFICATION_LISTENER_OBJECTNAME_FILTER_HANDBACK: mbs.removeNotificationListener((ObjectName) p[0], (ObjectName) p[1], (NotificationFilter) p[2], p[3]);
          break;
      }
    } catch (Exception e) {
      exception = e;
    }
    respond(context, request, result, exception);
  }

  /**
   * Handle a received reqUest.
   * @param context the JMX nio context.
   * @param response the response to handle.
   * @throws Exception if any error occurs.
   */
  private void handleResponse(final JMXContext context, final JMXResponse response) throws Exception {
    context.getMessageHandler().responseReceived(response);
  }

  /**
   * Handle a received notification.
   * @param context the JMX nio context.
   * @param jmxNotification the notification message to process.
   * @throws Exception if any error occurs.
   */
  private void handleNotification(final JMXContext context, final JMXNotification jmxNotification) throws Exception {
    if (debugEnabled) log.debug("received notification {} from context = {}", jmxNotification, context);
    context.getChannels().getMbeanServerConnection().handleNotification(jmxNotification);
  }

  /**
   * Send the response to the specified request.
   * @param context the JMX context that received the request.
   * @param request the request to respond to.
   * @param result the result of the requets, if any.
   * @param exception an exception that may have occurred if the request failed.
   * @throws Exception if any error occurs.
   */
  private void respond(final JMXContext context, final JMXRequest request, final Object result, final Exception exception) throws Exception {
    if (request.getMessageType() == JMXMessageType.CLOSE) {
      if (exception != null) throw exception;
    } else {
      JMXResponse response = new JMXResponse(request.getMessageID(), request.getMessageType(), result, exception);
      context.getMessageHandler().sendMessage(response);
    }
  }
}
