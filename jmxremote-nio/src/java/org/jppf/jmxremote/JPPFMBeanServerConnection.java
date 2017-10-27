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

import java.io.IOException;
import java.util.Set;

import javax.management.*;

import org.jppf.jmxremote.nio.JMXMessageHandler;

/**
 *
 * @author Laurent Cohen
 */
public class JPPFMBeanServerConnection implements MBeanServerConnection {
  /**
   * The message handler.
   */
  private final JMXMessageHandler messageHandler;

  /**
   *
   * @param messageHandler performs the communication with the server.
   */
  public JPPFMBeanServerConnection(final JMXMessageHandler messageHandler) {
    this.messageHandler = messageHandler;
  }

  @Override
  public ObjectInstance createMBean(final String className, final ObjectName name)
    throws ReflectionException, InstanceAlreadyExistsException, MBeanRegistrationException, MBeanException, NotCompliantMBeanException, IOException {
    try {
      return (ObjectInstance) messageHandler.sendRequest(CREATE_MBEAN, className, name);
    } catch (MBeanRegistrationException e) {
      throw e;
    } catch (ReflectionException | InstanceAlreadyExistsException | MBeanException | NotCompliantMBeanException | IOException e) {
      throw e;
    } catch (Exception e) {
      throw new IOException(e);
    }
  }

  @Override
  public ObjectInstance createMBean(final String className, final ObjectName name, final ObjectName loaderName)
    throws ReflectionException, InstanceAlreadyExistsException, MBeanRegistrationException, MBeanException, NotCompliantMBeanException, InstanceNotFoundException, IOException {
    try {
      return (ObjectInstance) messageHandler.sendRequest(CREATE_MBEAN_LOADER, className, name, loaderName);
    } catch (MBeanRegistrationException e) {
      throw e;
    } catch (ReflectionException | InstanceAlreadyExistsException | MBeanException | NotCompliantMBeanException | InstanceNotFoundException | IOException e) {
      throw e;
    } catch (Exception e) {
      throw new IOException(e);
    }
  }

  @Override
  public ObjectInstance createMBean(final String className, final ObjectName name, final Object[] params, final String[] signature)
    throws ReflectionException, InstanceAlreadyExistsException, MBeanRegistrationException, MBeanException, NotCompliantMBeanException, IOException {
    try {
      return (ObjectInstance) messageHandler.sendRequest(CREATE_MBEAN_PARAMS, className, name, params, signature);
    } catch (MBeanRegistrationException e) {
      throw e;
    } catch (ReflectionException | InstanceAlreadyExistsException | MBeanException | NotCompliantMBeanException | IOException e) {
      throw e;
    } catch (Exception e) {
      throw new IOException(e);
    }
  }

  @Override
  public ObjectInstance createMBean(final String className, final ObjectName name, final ObjectName loaderName, final Object[] params, final String[] signature)
    throws ReflectionException, InstanceAlreadyExistsException, MBeanRegistrationException, MBeanException, NotCompliantMBeanException, InstanceNotFoundException, IOException {
    try {
      return (ObjectInstance) messageHandler.sendRequest(CREATE_MBEAN_LOADER_PARAMS, className, name, loaderName);
    } catch (MBeanRegistrationException e) {
      throw e;
    } catch (ReflectionException | InstanceAlreadyExistsException | MBeanException | NotCompliantMBeanException | InstanceNotFoundException | IOException e) {
      throw e;
    } catch (Exception e) {
      throw new IOException(e);
    }
  }

  @Override
  public void unregisterMBean(final ObjectName name) throws InstanceNotFoundException, MBeanRegistrationException, IOException {
    try {
      messageHandler.sendRequest(UNREGISTER_MBEAN, name);
    } catch (InstanceNotFoundException | MBeanRegistrationException | IOException e) {
      throw e;
    } catch (Exception e) {
      throw new IOException(e);
    }
  }

  @Override
  public ObjectInstance getObjectInstance(final ObjectName name) throws InstanceNotFoundException, IOException {
    try {
      return (ObjectInstance) messageHandler.sendRequest(GET_OBJECT_INSTANCE, name);
    } catch (InstanceNotFoundException | IOException e) {
      throw e;
    } catch (Exception e) {
      throw new IOException(e);
    }
  }

  @SuppressWarnings("unchecked")
  @Override
  public Set<ObjectInstance> queryMBeans(final ObjectName name, final QueryExp query) throws IOException {
    try {
      return (Set<ObjectInstance>) messageHandler.sendRequest(QUERY_MBEANS, name, query);
    } catch (IOException e) {
      throw e;
    } catch (Exception e) {
      throw new IOException(e);
    }
  }

  @SuppressWarnings("unchecked")
  @Override
  public Set<ObjectName> queryNames(final ObjectName name, final QueryExp query) throws IOException {
    try {
      return (Set<ObjectName>) messageHandler.sendRequest(QUERY_NAMES, name, query);
    } catch (IOException e) {
      throw e;
    } catch (Exception e) {
      throw new IOException(e);
    }
  }

  @Override
  public boolean isRegistered(final ObjectName name) throws IOException {
    try {
      return (Boolean) messageHandler.sendRequest(IS_REGISTERED, name);
    } catch (IOException e) {
      throw e;
    } catch (Exception e) {
      throw new IOException(e);
    }
  }

  @Override
  public Integer getMBeanCount() throws IOException {
    try {
      return (Integer) messageHandler.sendRequest(GET_MBEAN_COUNT);
    } catch (IOException e) {
      throw e;
    } catch (Exception e) {
      throw new IOException(e);
    }
  }

  @Override
  public Object getAttribute(final ObjectName name, final String attribute) throws MBeanException, AttributeNotFoundException, InstanceNotFoundException, ReflectionException, IOException {
    try {
      return messageHandler.sendRequest(GET_ATTRIBUTE, name, attribute);
    } catch (MBeanException | AttributeNotFoundException | InstanceNotFoundException | ReflectionException | IOException e) {
      throw e;
    } catch (Exception e) {
      throw new IOException(e);
    }
  }

  @Override
  public AttributeList getAttributes(final ObjectName name, final String[] attributes) throws InstanceNotFoundException, ReflectionException, IOException {
    try {
      return (AttributeList) messageHandler.sendRequest(GET_ATTRIBUTES, name, attributes);
    } catch (InstanceNotFoundException | ReflectionException | IOException e) {
      throw e;
    } catch (Exception e) {
      throw new IOException(e);
    }
  }

  @Override
  public void setAttribute(final ObjectName name, final Attribute attribute)
    throws InstanceNotFoundException, AttributeNotFoundException, InvalidAttributeValueException, MBeanException, ReflectionException, IOException {
    try {
      messageHandler.sendRequest(SET_ATTRIBUTE, name, attribute);
    } catch (InstanceNotFoundException | AttributeNotFoundException | InvalidAttributeValueException | MBeanException | ReflectionException | IOException e) {
      throw e;
    } catch (Exception e) {
      throw new IOException(e);
    }
  }

  @Override
  public AttributeList setAttributes(final ObjectName name, final AttributeList attributes) throws InstanceNotFoundException, ReflectionException, IOException {
    try {
      return (AttributeList) messageHandler.sendRequest(SET_ATTRIBUTES, name, attributes);
    } catch (InstanceNotFoundException | ReflectionException | IOException e) {
      throw e;
    } catch (Exception e) {
      throw new IOException(e);
    }
  }

  @Override
  public Object invoke(final ObjectName name, final String operationName, final Object[] params, final String[] signature)
    throws InstanceNotFoundException, MBeanException, ReflectionException, IOException {
    try {
      return messageHandler.sendRequest(INVOKE, name, operationName, params, signature);
    } catch (InstanceNotFoundException | MBeanException | ReflectionException | IOException e) {
      throw e;
    } catch (Exception e) {
      throw new IOException(e);
    }
  }

  @Override
  public String getDefaultDomain() throws IOException {
    try {
      return (String) messageHandler.sendRequest(GET_DEFAULT_DOMAIN);
    } catch (IOException e) {
      throw e;
    } catch (Exception e) {
      throw new IOException(e);
    }
  }

  @Override
  public String[] getDomains() throws IOException {
    try {
      return (String[]) messageHandler.sendRequest(GET_DOMAINS);
    } catch (IOException e) {
      throw e;
    } catch (Exception e) {
      throw new IOException(e);
    }
  }

  @Override
  public void addNotificationListener(final ObjectName name, final NotificationListener listener, final NotificationFilter filter, final Object handback)
    throws InstanceNotFoundException, IOException {
  }

  @Override
  public void addNotificationListener(final ObjectName name, final ObjectName listener, final NotificationFilter filter, final Object handback) throws InstanceNotFoundException, IOException {
  }

  @Override
  public void removeNotificationListener(final ObjectName name, final ObjectName listener) throws InstanceNotFoundException, ListenerNotFoundException, IOException {
  }

  @Override
  public void removeNotificationListener(final ObjectName name, final NotificationListener listener) throws InstanceNotFoundException, ListenerNotFoundException, IOException {
  }

  @Override
  public void removeNotificationListener(final ObjectName name, final ObjectName listener, final NotificationFilter filter, final Object handback)
    throws InstanceNotFoundException, ListenerNotFoundException, IOException {
  }

  @Override
  public void removeNotificationListener(final ObjectName name, final NotificationListener listener, final NotificationFilter filter, final Object handback)
    throws InstanceNotFoundException, ListenerNotFoundException, IOException {
  }

  @Override
  public MBeanInfo getMBeanInfo(final ObjectName name) throws InstanceNotFoundException, IntrospectionException, ReflectionException, IOException {
    try {
      return (MBeanInfo) messageHandler.sendRequest(GET_MBEAN_INFO, name);
    } catch (InstanceNotFoundException | IntrospectionException | ReflectionException | IOException e) {
      throw e;
    } catch (Exception e) {
      throw new IOException(e);
    }
  }

  @Override
  public boolean isInstanceOf(final ObjectName name, final String className) throws InstanceNotFoundException, IOException {
    try {
      return (Boolean) messageHandler.sendRequest(IS_INSTANCE_OF, name, className);
    } catch (InstanceNotFoundException | IOException e) {
      throw e;
    } catch (Exception e) {
      throw new IOException(e);
    }
  }

  /**
   * @return the message handler.
   */
  public JMXMessageHandler getMessageHandler() {
    return messageHandler;
  }
}
