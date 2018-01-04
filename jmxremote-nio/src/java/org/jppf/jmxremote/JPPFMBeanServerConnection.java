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

package org.jppf.jmxremote;

import static org.jppf.jmxremote.message.JMXMessageType.*;

import java.io.*;
import java.util.*;

import javax.management.*;

import org.jppf.jmxremote.message.*;
import org.jppf.jmxremote.nio.*;
import org.jppf.jmxremote.notification.ClientListenerInfo;
import org.slf4j.*;

/**
 * Implementation of the {@link MBeanServerConnection} interface for JPPF JMX remote connectors.
 * @author Laurent Cohen
 */
public class JPPFMBeanServerConnection implements MBeanServerConnection, Closeable {
  /**
   * Logger for this class.
   */
  private static Logger log = LoggerFactory.getLogger(JPPFMBeanServerConnection.class);
  /**
   * Determines whether the debug level is enabled in the log configuration, without the cost of a method call.
   */
  private static boolean debugEnabled = log.isDebugEnabled();
  /**
   * The message handler.
   */
  private final JMXMessageHandler messageHandler;
  /**
   * The connection ID.
   */
  private String connectionID;
  /**
   * Mapping of notification listener ids to actual listeners.
   */
  private final Map<Integer, ClientListenerInfo> listenerMap = new HashMap<>();
  /**
   * The nio server.
   */
  private final JMXNioServer server;

  /**
   * Initialize with the specified message handler.
   * @param messageHandler performs the communication with the server.
   * @param server the nio server.
   */
  public JPPFMBeanServerConnection(final JMXMessageHandler messageHandler, final JMXNioServer server) {
    this.messageHandler = messageHandler;
    this.server = server;
  }

  @Override
  public ObjectInstance createMBean(final String className, final ObjectName name)
    throws ReflectionException, InstanceAlreadyExistsException, MBeanRegistrationException, MBeanException, NotCompliantMBeanException, IOException {
    try {
      return (ObjectInstance) messageHandler.sendRequestWithResponse(CREATE_MBEAN, className, name);
    } catch (final MBeanRegistrationException e) {
      throw e;
    } catch (final ReflectionException | InstanceAlreadyExistsException | MBeanException | NotCompliantMBeanException | IOException e) {
      throw e;
    } catch (final Exception e) {
      throw new IOException(e);
    }
  }

  @Override
  public ObjectInstance createMBean(final String className, final ObjectName name, final ObjectName loaderName)
    throws ReflectionException, InstanceAlreadyExistsException, MBeanRegistrationException, MBeanException, NotCompliantMBeanException, InstanceNotFoundException, IOException {
    try {
      return (ObjectInstance) messageHandler.sendRequestWithResponse(CREATE_MBEAN_LOADER, className, name, loaderName);
    } catch (final MBeanRegistrationException e) {
      throw e;
    } catch (final ReflectionException | InstanceAlreadyExistsException | MBeanException | NotCompliantMBeanException | InstanceNotFoundException | IOException e) {
      throw e;
    } catch (final Exception e) {
      throw new IOException(e);
    }
  }

  @Override
  public ObjectInstance createMBean(final String className, final ObjectName name, final Object[] params, final String[] signature)
    throws ReflectionException, InstanceAlreadyExistsException, MBeanRegistrationException, MBeanException, NotCompliantMBeanException, IOException {
    try {
      return (ObjectInstance) messageHandler.sendRequestWithResponse(CREATE_MBEAN_PARAMS, className, name, params, signature);
    } catch (final MBeanRegistrationException e) {
      throw e;
    } catch (final ReflectionException | InstanceAlreadyExistsException | MBeanException | NotCompliantMBeanException | IOException e) {
      throw e;
    } catch (final Exception e) {
      throw new IOException(e);
    }
  }

  @Override
  public ObjectInstance createMBean(final String className, final ObjectName name, final ObjectName loaderName, final Object[] params, final String[] signature)
    throws ReflectionException, InstanceAlreadyExistsException, MBeanRegistrationException, MBeanException, NotCompliantMBeanException, InstanceNotFoundException, IOException {
    try {
      return (ObjectInstance) messageHandler.sendRequestWithResponse(CREATE_MBEAN_LOADER_PARAMS, className, name, loaderName);
    } catch (final MBeanRegistrationException e) {
      throw e;
    } catch (final ReflectionException | InstanceAlreadyExistsException | MBeanException | NotCompliantMBeanException | InstanceNotFoundException | IOException e) {
      throw e;
    } catch (final Exception e) {
      throw new IOException(e);
    }
  }

  @Override
  public void unregisterMBean(final ObjectName name) throws InstanceNotFoundException, MBeanRegistrationException, IOException {
    try {
      messageHandler.sendRequestWithResponse(UNREGISTER_MBEAN, name);
    } catch (final InstanceNotFoundException | MBeanRegistrationException | IOException e) {
      throw e;
    } catch (final Exception e) {
      throw new IOException(e);
    }
  }

  @Override
  public ObjectInstance getObjectInstance(final ObjectName name) throws InstanceNotFoundException, IOException {
    try {
      return (ObjectInstance) messageHandler.sendRequestWithResponse(GET_OBJECT_INSTANCE, name);
    } catch (final InstanceNotFoundException | IOException e) {
      throw e;
    } catch (final Exception e) {
      throw new IOException(e);
    }
  }

  @SuppressWarnings("unchecked")
  @Override
  public Set<ObjectInstance> queryMBeans(final ObjectName name, final QueryExp query) throws IOException {
    try {
      return (Set<ObjectInstance>) messageHandler.sendRequestWithResponse(QUERY_MBEANS, name, query);
    } catch (final IOException e) {
      throw e;
    } catch (final Exception e) {
      throw new IOException(e);
    }
  }

  @SuppressWarnings("unchecked")
  @Override
  public Set<ObjectName> queryNames(final ObjectName name, final QueryExp query) throws IOException {
    try {
      return (Set<ObjectName>) messageHandler.sendRequestWithResponse(QUERY_NAMES, name, query);
    } catch (final IOException e) {
      throw e;
    } catch (final Exception e) {
      throw new IOException(e);
    }
  }

  @Override
  public boolean isRegistered(final ObjectName name) throws IOException {
    try {
      return (Boolean) messageHandler.sendRequestWithResponse(IS_REGISTERED, name);
    } catch (final IOException e) {
      throw e;
    } catch (final Exception e) {
      throw new IOException(e);
    }
  }

  @Override
  public Integer getMBeanCount() throws IOException {
    try {
      return (Integer) messageHandler.sendRequestWithResponse(GET_MBEAN_COUNT);
    } catch (final IOException e) {
      throw e;
    } catch (final Exception e) {
      throw new IOException(e);
    }
  }

  @Override
  public Object getAttribute(final ObjectName name, final String attribute) throws MBeanException, AttributeNotFoundException, InstanceNotFoundException, ReflectionException, IOException {
    try {
      return messageHandler.sendRequestWithResponse(GET_ATTRIBUTE, name, attribute);
    } catch (final MBeanException | AttributeNotFoundException | InstanceNotFoundException | ReflectionException | IOException e) {
      throw e;
    } catch (final Exception e) {
      throw new IOException(e);
    }
  }

  @Override
  public AttributeList getAttributes(final ObjectName name, final String[] attributes) throws InstanceNotFoundException, ReflectionException, IOException {
    try {
      return (AttributeList) messageHandler.sendRequestWithResponse(GET_ATTRIBUTES, name, attributes);
    } catch (final InstanceNotFoundException | ReflectionException | IOException e) {
      throw e;
    } catch (final Exception e) {
      throw new IOException(e);
    }
  }

  @Override
  public void setAttribute(final ObjectName name, final Attribute attribute)
    throws InstanceNotFoundException, AttributeNotFoundException, InvalidAttributeValueException, MBeanException, ReflectionException, IOException {
    try {
      messageHandler.sendRequestWithResponse(SET_ATTRIBUTE, name, attribute);
    } catch (final InstanceNotFoundException | AttributeNotFoundException | InvalidAttributeValueException | MBeanException | ReflectionException | IOException e) {
      throw e;
    } catch (final Exception e) {
      throw new IOException(e);
    }
  }

  @Override
  public AttributeList setAttributes(final ObjectName name, final AttributeList attributes) throws InstanceNotFoundException, ReflectionException, IOException {
    try {
      return (AttributeList) messageHandler.sendRequestWithResponse(SET_ATTRIBUTES, name, attributes);
    } catch (final InstanceNotFoundException | ReflectionException | IOException e) {
      throw e;
    } catch (final Exception e) {
      throw new IOException(e);
    }
  }

  @Override
  public Object invoke(final ObjectName name, final String operationName, final Object[] params, final String[] signature)
    throws InstanceNotFoundException, MBeanException, ReflectionException, IOException {
    try {
      return messageHandler.sendRequestWithResponse(INVOKE, name, operationName, params, signature);
    } catch (final InstanceNotFoundException | MBeanException | ReflectionException | IOException e) {
      throw e;
    } catch (final Exception e) {
      throw new IOException(e);
    }
  }

  @Override
  public String getDefaultDomain() throws IOException {
    try {
      return (String) messageHandler.sendRequestWithResponse(GET_DEFAULT_DOMAIN);
    } catch (final IOException e) {
      throw e;
    } catch (final Exception e) {
      throw new IOException(e);
    }
  }

  @Override
  public String[] getDomains() throws IOException {
    try {
      return (String[]) messageHandler.sendRequestWithResponse(GET_DOMAINS);
    } catch (final IOException e) {
      throw e;
    } catch (final Exception e) {
      throw new IOException(e);
    }
  }

  @Override
  public void addNotificationListener(final ObjectName name, final NotificationListener listener, final NotificationFilter filter, final Object handback)
    throws InstanceNotFoundException, IOException {
    try {
      final int listenerID = (Integer) messageHandler.sendRequestWithResponse(ADD_NOTIFICATION_LISTENER, name, filter);
      synchronized(listenerMap) {
        listenerMap.put(listenerID, new ClientListenerInfo(listenerID, name, listener, filter, handback));
      }
    } catch (final IOException e) {
      throw e;
    } catch (final Exception e) {
      throw new IOException(e);
    }
  }

  @Override
  public void addNotificationListener(final ObjectName name, final ObjectName listener, final NotificationFilter filter, final Object handback) throws InstanceNotFoundException, IOException {
    try {
      messageHandler.sendRequestWithResponse(ADD_NOTIFICATION_LISTENER_OBJECTNAME, name, listener, filter, handback);
    } catch (final InstanceNotFoundException | IOException e) {
      throw e;
    } catch (final Exception e) {
      throw new IOException(e);
    }
  }

  @Override
  public void removeNotificationListener(final ObjectName name, final NotificationListener listener) throws InstanceNotFoundException, ListenerNotFoundException, IOException {
    try {
      final List<ClientListenerInfo> toRemove = new ArrayList<>();
      synchronized(listenerMap) {
        for (final Map.Entry<Integer, ClientListenerInfo> entry: listenerMap.entrySet()) {
          final ClientListenerInfo info = entry.getValue();
          if (info.getMbeanName().equals(name) && (info.getListener() == listener)) toRemove.add(info);
        }
      }
      if (toRemove.isEmpty()) throw new ListenerNotFoundException("no matching listener");
      final int[] ids = new int[toRemove.size()];
      for (int i=0; i<ids.length; i++) ids[i] = toRemove.get(i).getListenerID();
      messageHandler.sendRequestWithResponse(REMOVE_NOTIFICATION_LISTENER, name, ids);
      synchronized(listenerMap) {
        for (final int id: ids) listenerMap.remove(id);
      }
    } catch (final InstanceNotFoundException | ListenerNotFoundException | IOException e) {
      throw e;
    } catch (final Exception e) {
      throw new IOException(e);
    }
  }

  @Override
  public void removeNotificationListener(final ObjectName name, final NotificationListener listener, final NotificationFilter filter, final Object handback)
    throws InstanceNotFoundException, ListenerNotFoundException, IOException {
    try {
      ClientListenerInfo toRemove = null;
      synchronized(listenerMap) {
        for (Map.Entry<Integer, ClientListenerInfo> entry: listenerMap.entrySet()) {
          final ClientListenerInfo info = entry.getValue();
          if (info.getMbeanName().equals(name) && (info.getListener() == listener) && (info.getFilter() == filter) && (info.getHandback() == handback)) {
            toRemove = info;
            break;
          }
        }
      }
      if (toRemove == null) throw new ListenerNotFoundException("no matching listener");
      messageHandler.sendRequestWithResponse(REMOVE_NOTIFICATION_LISTENER_FILTER_HANDBACK, name, toRemove.getListenerID());
      synchronized(listenerMap) {
        listenerMap.remove(toRemove.getListenerID());
      }
    } catch (final InstanceNotFoundException | ListenerNotFoundException | IOException e) {
      throw e;
    } catch (final Exception e) {
      throw new IOException(e);
    }
  }

  @Override
  public void removeNotificationListener(final ObjectName name, final ObjectName listener) throws InstanceNotFoundException, ListenerNotFoundException, IOException {
    try {
      messageHandler.sendRequestWithResponse(REMOVE_NOTIFICATION_LISTENER_OBJECTNAME, name, listener);
    } catch (final InstanceNotFoundException | ListenerNotFoundException | IOException e) {
      throw e;
    } catch (final Exception e) {
      throw new IOException(e);
    }
  }

  @Override
  public void removeNotificationListener(final ObjectName name, final ObjectName listener, final NotificationFilter filter, final Object handback)
    throws InstanceNotFoundException, ListenerNotFoundException, IOException {
    try {
      messageHandler.sendRequestWithResponse(REMOVE_NOTIFICATION_LISTENER_OBJECTNAME_FILTER_HANDBACK, name, listener, filter, handback);
    } catch (final InstanceNotFoundException | ListenerNotFoundException | IOException e) {
      throw e;
    } catch (final Exception e) {
      throw new IOException(e);
    }
  }

  @Override
  public MBeanInfo getMBeanInfo(final ObjectName name) throws InstanceNotFoundException, IntrospectionException, ReflectionException, IOException {
    try {
      return (MBeanInfo) messageHandler.sendRequestWithResponse(GET_MBEAN_INFO, name);
    } catch (final InstanceNotFoundException | IntrospectionException | ReflectionException | IOException e) {
      throw e;
    } catch (final Exception e) {
      throw new IOException(e);
    }
  }

  @Override
  public boolean isInstanceOf(final ObjectName name, final String className) throws InstanceNotFoundException, IOException {
    try {
      return (Boolean) messageHandler.sendRequestWithResponse(IS_INSTANCE_OF, name, className);
    } catch (final InstanceNotFoundException | IOException e) {
      throw e;
    } catch (final Exception e) {
      throw new IOException(e);
    }
  }

  @Override
  public void close() throws IOException {
    final ChannelsPair channels = messageHandler.getChannels();
    if (channels.isClosing() || channels.isClosed()) return;
    try {
      if (debugEnabled) log.debug("closing {}", channels);
      channels.requestClose();
      channels.disableRead();
      messageHandler.sendRequestNoResponse(CLOSE);
      server.closeConnection(connectionID, null, true);
    } catch (final IOException e) {
      throw e;
    } catch (final Exception e) {
      throw new IOException(e);
    }
  }

  /**
   * Obtain the connection ID from the remote server. This method should opnly be called once, after the JPPF identifier has been sent.
   * @return the connection ID string.
   * @throws IOException if any error occurs.
   */
  String receiveConnectionID() throws IOException {
    try {
      return connectionID = messageHandler.receiveConnectionID();
    } catch (final IOException e) {
      throw e;
    } catch (final Exception e) {
      throw new IOException(e);
    }
  }

  /**
   * @return the message handler.
   */
  public JMXMessageHandler getMessageHandler() {
    return messageHandler;
  }

  /**
   * @return the connection ID.
   */
  public String getConnectionID() {
    return connectionID;
  }

  /**
   * Set the connection ID.
   * @param connectionID the connection id to set.
   */
  public void setConnectionID(final String connectionID) {
    this.connectionID = connectionID;
  }

  /**
   * Get the listener information for the specified listener id.
   * @param listenerID the id of the listener for which to get information.
   * @return a {@link ClientListenerInfo} object, or {@code null} if no listener is registered witht he specified id.
   */
  public ClientListenerInfo getListenerInfo(final int listenerID) {
    synchronized(listenerMap) {
      return listenerMap.get(listenerID);
    }
  }

  /**
   * Handle a new received notification.
   * @param jmxNotification the notification message to process.
   * @throws Exception if any error occurs.
   */
  public void handleNotification(final JMXNotification jmxNotification) throws Exception {
    for (final Integer listenerID: jmxNotification.getListenerIDs()) {
      final ClientListenerInfo info = getListenerInfo(listenerID);
      if (info != null) info.getListener().handleNotification(jmxNotification.getNotification(), info.getHandback());
    }
  }
}
