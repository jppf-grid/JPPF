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

import static org.jppf.jmxremote.message.JMXMessageType.*;

import java.io.IOException;

import javax.management.*;
import javax.management.remote.JMXAuthenticator;
import javax.security.auth.Subject;

import org.jppf.jmxremote.JMXAuthorizationChecker;
import org.jppf.jmxremote.message.*;
import org.jppf.nio.*;
import org.jppf.utils.*;
import org.slf4j.*;

/**
 * Reads from the channel until no more data is available. Each fully read message is handed off
 * to a global thread pool for deserialization and processing.
 * @author Laurent Cohen
 */
class JMXMessageReader {
  /**
   * Logger for this class.
   */
  private static final Logger log = LoggerFactory.getLogger(JMXMessageReader.class);
  /**
   * Determines whether the debug level is enabled in the log configuration, without the cost of a method call.
   */
  private static final boolean debugEnabled = log.isDebugEnabled();

  /**
   * Read from the channel until no more data is available (i.e. socket receive buffer is empty).
   * @param context the JMX context that reads the data.
   * @throws Exception if any error occurs.
   */
  static void read(final JMXContext context) throws Exception {
    if (context.isSsl()) {
      synchronized(context.getSocketChannel()) {
        doRead(context);
      }
    } else doRead(context);
  }

  /**
   * Read from the channel until no more data is available (i.e. socket receive buffer is empty).
   * @param context the JMX context that reads the data.
   * @throws Exception if any error occurs.
   */
  private static void doRead(final JMXContext context) throws Exception {
    final StateTransitionManager<EmptyEnum, EmptyEnum> mgr = context.getServer().getTransitionManager();
    while (true) {
      boolean b = false;
      try {
        b = context.readMessage(context.getChannel());
      } catch (final IOException e) {
        final ChannelsPair pair = context.getChannels();
        if (pair.isClosed() || pair.isClosing()) return;
        else throw e;
      }
      if (b) {
        final SimpleNioMessage message = (SimpleNioMessage) context.getMessage();
        if (debugEnabled) log.debug("read message from {}", context);
        context.setMessage(null);
        mgr.execute(new HandlingTask(context, message));
      } else if (context.byteCount <= 0L) break;
    }
  }

  /**
   * Deserialize the specified message and route it to the specialized handling method.
   * @param context the context associated with the channel.
   * @param message the message to handle.
   * @throws Exception if any error occurs.
   */
  private static void handleMessage(final JMXContext context, final SimpleNioMessage message) throws Exception {
    final JMXMessage msg = context.deserializeMessage(message);
    if (debugEnabled) log.debug("read message = {} from context = {}", msg, context);
    if (msg instanceof JMXRequest) handleRequest(context, (JMXRequest) msg);
    else if (msg instanceof JMXResponse) handleResponse(context, (JMXResponse) msg);
    else if (msg instanceof JMXNotification) handleNotification(context, (JMXNotification) msg);
  }

  /**
   * Handle a reqUest received from a client.
   * @param context the JMX nio context.
   * @param request the request to process.
   * @throws Exception if any error occurs.
   */
  private static void handleRequest(final JMXContext context, final JMXRequest request) throws Exception {
    if (debugEnabled) log.debug("handling request = {} from {}", request, context);
    boolean isException = false;
    Object result = null;
    final Object[] p = request.getParams();
    final MBeanServer mbs = context.getChannels().getMbeanServer();
    final JMXNioServer jmxServer = context.getServer();
    try {
      checkRequestAuthorization(context, request);
      switch(request.getMessageType()) {
        case CONNECT: result = handleConnect(context, request);
          break;
        case CLOSE: handleClose(context);
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
    } catch (final Exception e) {
      isException = true;
      result = e;
    }
    respond(context, request, result, isException);
  }

  /**
   * Check that a request is authorized for the authenticated subject of the specified context.
   * @param context the JMX nio context.
   * @param request the request to check for authorization.
   * @throws Exception if any error occurs.
   */
  private static void checkRequestAuthorization(final JMXContext context, final JMXRequest request) throws Exception {
    final JMXAuthorizationChecker checker = context.getChannels().getAuhtorizationChecker();
    if (checker == null) return;
    if (debugEnabled) log.debug("checking autorization for request = {}, with context {}", request, context);
    final Object[] p = request.getParams();
    switch(request.getMessageType()) {
      case INVOKE: checker.checkInvoke((ObjectName) p[0], (String) p[1], (Object[]) p[2], (String[]) p[3]);
        break;
      case GET_ATTRIBUTE: checker.checkGetAttribute((ObjectName) p[0], (String) p[1]);
        break;
      case GET_ATTRIBUTES: checker.checkGetAttributes((ObjectName) p[0], (String[]) p[1]);
        break;
      case SET_ATTRIBUTE: checker.checkSetAttribute((ObjectName) p[0], (Attribute) p[1]);
        break;
      case SET_ATTRIBUTES: checker.checkSetAttributes((ObjectName) p[0], (AttributeList) p[1]);
        break;
      case CREATE_MBEAN: checker.checkCreateMBean((String) p[0], (ObjectName) p[1]);
        break;
      case CREATE_MBEAN_PARAMS: checker.checkCreateMBean((String) p[0], (ObjectName) p[1], (Object[]) p[2], (String[]) p[3]);
        break;
      case CREATE_MBEAN_LOADER: checker.checkCreateMBean((String) p[0], (ObjectName) p[1], (ObjectName) p[2]);
        break;
      case CREATE_MBEAN_LOADER_PARAMS: checker.checkCreateMBean((String) p[0], (ObjectName) p[1], (ObjectName) p[2], (Object[]) p[3], (String[]) p[4]);
        break;
      case GET_DEFAULT_DOMAIN: checker.checkGetDefaultDomain();
        break;
      case GET_DOMAINS: checker.checkGetDomains();
        break;
      case GET_MBEAN_COUNT: checker.checkGetMBeanCount();
        break;
      case GET_MBEAN_INFO: checker.checkGetMBeanInfo((ObjectName) p[0]);
        break;
      case GET_OBJECT_INSTANCE: checker.checkGetObjectInstance((ObjectName) p[0]);
        break;
      case IS_INSTANCE_OF: checker.checkIsInstanceOf((ObjectName) p[0], (String) p[1]);
        break;
      case IS_REGISTERED: checker.checkIsRegistered((ObjectName) p[0]);
        break;
      case QUERY_MBEANS: checker.checkQueryMBeans((ObjectName) p[0], (QueryExp) p[1]);
        break;
      case QUERY_NAMES: checker.checkQueryNames((ObjectName) p[0], (QueryExp) p[1]);
        break;
      case UNREGISTER_MBEAN: checker.checkUnregisterMBean((ObjectName) p[0]);
        break;
      case ADD_NOTIFICATION_LISTENER: checker.checkAddNotificationListener((ObjectName) p[0], (NotificationListener) null, (NotificationFilter) p[2], null);
        break;
      case ADD_NOTIFICATION_LISTENER_OBJECTNAME: checker.checkAddNotificationListener((ObjectName) p[0], (ObjectName) p[1], (NotificationFilter) p[2], p[3]);
        break;
      case REMOVE_NOTIFICATION_LISTENER: checker.checkRemoveNotificationListener((ObjectName) p[0], (NotificationListener) null);
        break;
      case REMOVE_NOTIFICATION_LISTENER_FILTER_HANDBACK: checker.checkRemoveNotificationListener((ObjectName) p[0], (NotificationListener) null, null, null);
        break;
      case REMOVE_NOTIFICATION_LISTENER_OBJECTNAME: checker.checkRemoveNotificationListener((ObjectName) p[0], (ObjectName) p[1]);
        break;
      case REMOVE_NOTIFICATION_LISTENER_OBJECTNAME_FILTER_HANDBACK: checker.checkRemoveNotificationListener((ObjectName) p[0], (ObjectName) p[1], (NotificationFilter) p[2], p[3]);
        break;
    }
  }

  /**
   * Handle a connection reqUest.
   * @param context the JMX nio context.
   * @param request the connection request to handle.
   * @return the connection ID.
   * @throws Exception if any error occurs.
   */
  private static String handleConnect(final JMXContext context, final JMXRequest request) throws Exception {
    final JMXAuthenticator authenticator = context.getChannels().getAuthenticator();
    if (authenticator != null) {
      final Subject subject = authenticator.authenticate(request.getParams()[0]);
      context.getChannels().setSubject(subject);
      final JMXAuthorizationChecker checker = context.getChannels().getAuhtorizationChecker();
      if (checker != null) checker.setSubject(subject);
    }
    return context.getConnectionID();
  }

  /**
   * Handle a connection reqUest.
   * @param context the JMX nio context.
   * @throws Exception if any error occurs.
   */
  private static void handleClose(final JMXContext context) throws Exception {
    context.getChannels().requestClose();
    context.getChannels().close(null);
    context.getServer().closeConnection(context.getChannels(), null, true);
  }

  /**
   * Handle a received reqUest.
   * @param context the JMX nio context.
   * @param response the response to handle.
   * @throws Exception if any error occurs.
   */
  private static void handleResponse(final JMXContext context, final JMXResponse response) throws Exception {
    context.getMessageHandler().responseReceived(response);
  }

  /**
   * Handle a received notification.
   * @param context the JMX nio context.
   * @param jmxNotification the notification message to process.
   * @throws Exception if any error occurs.
   */
  private static void handleNotification(final JMXContext context, final JMXNotification jmxNotification) throws Exception {
    if (debugEnabled) log.debug("received notification {} from context = {}", jmxNotification, context);
    context.getChannels().getJMXConnector().handleNotification(jmxNotification);
  }

  /**
   * Send the response to the specified request.
   * @param context the JMX context that received the request.
   * @param request the request to respond to.
   * @param result the result of the requets, if any.
   * @param isException whether the result is an exception or a normal result.
   * @throws Exception if any error occurs.
   */
  private static void respond(final JMXContext context, final JMXRequest request, final Object result, final boolean isException) throws Exception {
    if (request.getMessageType() == JMXMessageType.CLOSE) {
      if (isException) throw (Exception) result;
    } else {
      final JMXResponse response = new JMXResponse(request.getMessageID(), request.getMessageType(), result, isException);
      context.getMessageHandler().sendMessage(response);
    }
  }

  /**
   * Instances of this task deserialize and porcess a NioMessage that was read from the network channel.
   */
  private final static class HandlingTask implements Runnable {
    /**
     * The context associated with the channel.
     */
    private final JMXContext context;
    /**
     * The message to handle.
     */
    private final SimpleNioMessage message;

    /**
     * Initialize with the specified context and message.
     * @param context the context associated with the channel.
     * @param message the message to handle.
     */
    private HandlingTask(final JMXContext context, final SimpleNioMessage message) {
      this.context = context;
      this.message = message;
    }

    @Override
    public void run() {
      try {
        JMXMessageReader.handleMessage(context, message);
      } catch(final Exception|Error e) {
        try {
          if (debugEnabled) log.debug("error on channel {} :\n{}", context, ExceptionUtils.getStackTrace(e));
          else log.warn("error on channel {} : {}", context, ExceptionUtils.getMessage(e));
        } catch (final Exception e2) {
          if (debugEnabled) log.debug("error on channel: {}", ExceptionUtils.getStackTrace(e2));
          else log.warn("error on channel: {}", ExceptionUtils.getMessage(e2));
        }
        if (e instanceof Exception) context.handleException(context.getChannel(), (Exception) e);
        else throw (Error) e;
      }
    }
  }
}
