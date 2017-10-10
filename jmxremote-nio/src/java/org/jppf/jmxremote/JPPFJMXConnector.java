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

import java.io.IOException;
import java.util.Map;

import javax.management.*;
import javax.management.remote.*;
import javax.security.auth.Subject;

/**
 * 
 * @author Laurent Cohen
 */
public class JPPFJMXConnector implements JMXConnector {
  /**
   * The environment for this connector.
   */
  private final Map<String, ?> environment;
  /**
   * The address of this connector.
   */
  private final JMXServiceURL address;

  /**
   * 
   * @param serviceURL the address of this connector.
   * @param environment the environment for this connector.
   */
  public JPPFJMXConnector(final JMXServiceURL serviceURL, final Map<String, ?> environment) {
    this.environment = environment;
    this.address = serviceURL;
  }

  @Override
  public void connect() throws IOException {
  }

  @Override
  public void connect(Map<String, ?> env) throws IOException {
  }

  @Override
  public MBeanServerConnection getMBeanServerConnection() throws IOException {
    return null;
  }

  @Override
  public MBeanServerConnection getMBeanServerConnection(Subject delegationSubject) throws IOException {
    return null;
  }

  @Override
  public void close() throws IOException {
  }

  @Override
  public void addConnectionNotificationListener(NotificationListener listener, NotificationFilter filter, Object handback) {
  }

  @Override
  public void removeConnectionNotificationListener(NotificationListener listener) throws ListenerNotFoundException {
  }

  @Override
  public void removeConnectionNotificationListener(NotificationListener l, NotificationFilter f, Object handback) throws ListenerNotFoundException {
  }

  @Override
  public String getConnectionId() throws IOException {
    return null;
  }

  /**
   * @return the environment for this connector.
   */
  public Map<String, ?> getEnvironment() {
    return environment;
  }

  /**
   * @return the address of this connector.
   */
  public JMXServiceURL getAddress() {
    return address;
  }
}
