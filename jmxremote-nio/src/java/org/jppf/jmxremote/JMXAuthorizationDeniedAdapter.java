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

import javax.management.*;
import javax.security.auth.Subject;

/**
 * An adapter for the {@link JMXAuthorizationChecker} interface, to subclass when not all methods need be implemented.
 * <p>This particular implementation denies everythng by default: any of its methods that is not overriden will throw a {@code SecurityException}.
 * <p>The subject is available as a {@code protected} instance variable, as well as through its accessors {@link #getSubject() getSubject()} and {@link #setSubject(Subject) setSubject(Subject)}.
 * @author Laurent Cohen
 */
public class JMXAuthorizationDeniedAdapter implements JMXAuthorizationChecker {
  /**
   * The authenticated subject.
   */
  protected Subject subject;

  @Override
  public Subject getSubject() {
    return subject;
  }

  @Override
  public void setSubject(final Subject subject) {
    this.subject = subject;
  }

  @Override
  public void checkCreateMBean(final String className, final ObjectName name) throws Exception {
    throw new SecurityException(String.format("permission denied for method createMBean(%s, %s)", className, name));
  }

  @Override
  public void checkCreateMBean(final String className, final ObjectName name, final ObjectName loaderName) throws Exception {
    throw new SecurityException(String.format("permission denied for method createMBean(%s, %s, %s)", className, name, loaderName));
  }

  @Override
  public void checkCreateMBean(final String className, final ObjectName name, final Object[] params, final String[] signature) throws Exception {
    throw new SecurityException(String.format("permission denied for method createMBean(%s, %s, %s, %s)", className, name, params, signature));
  }

  @Override
  public void checkCreateMBean(final String className, final ObjectName name, final ObjectName loaderName, final Object[] params, final String[] signature) throws Exception {
    throw new SecurityException(String.format("permission denied for method createMBean(%s, %s, %s, %s, %s)", className, name, loaderName, params, signature));
  }

  @Override
  public void checkUnregisterMBean(final ObjectName name) throws Exception {
    throw new SecurityException(String.format("permission denied for method unregisterMBean(%s)", name));
  }

  @Override
  public void checkGetObjectInstance(final ObjectName name) throws Exception {
    throw new SecurityException(String.format("permission denied for method getObjectInstance(%s)", name));
  }

  @Override
  public void checkQueryMBeans(final ObjectName name, final QueryExp query) throws Exception {
    throw new SecurityException(String.format("permission denied for method queryMBeans(%s, %s)", name, query));
  }

  @Override
  public void checkQueryNames(final ObjectName name, final QueryExp query) throws Exception {
    throw new SecurityException(String.format("permission denied for method queryNames(%s, %s)", name, query));
  }

  @Override
  public void checkIsRegistered(final ObjectName name) throws Exception {
    throw new SecurityException(String.format("permission denied for method isRegistered(%s)", name));
  }

  @Override
  public void checkGetMBeanCount() throws Exception {
    throw new SecurityException(String.format("permission denied for method getMBeanCount()"));
  }

  @Override
  public void checkGetAttribute(final ObjectName name, final String attribute) throws Exception {
    throw new SecurityException(String.format("permission denied for method getAttribute(%s, %s)", name, attribute));
  }

  @Override
  public void checkGetAttributes(final ObjectName name, final String[] attributes) throws Exception {
    throw new SecurityException(String.format("permission denied for method getAttributes(%s, %s)", name, attributes));
  }

  @Override
  public void checkSetAttribute(final ObjectName name, final Attribute attribute) throws Exception {
    throw new SecurityException(String.format("permission denied for method setAttribute(%s, %s)", name, attribute));
  }

  @Override
  public void checkSetAttributes(final ObjectName name, final AttributeList attributes) throws Exception {
    throw new SecurityException(String.format("permission denied for method setAttributes(%s, %s)", name, attributes));
  }

  @Override
  public void checkInvoke(final ObjectName name, final String operationName, final Object[] params, final String[] signature) throws Exception {
    throw new SecurityException(String.format("permission denied for method invoke(%s, %s, %s, %s)", name, operationName, params, signature));
  }

  @Override
  public void checkGetDefaultDomain() throws Exception {
    throw new SecurityException(String.format("permission denied for method getDefaultDomain()"));
  }

  @Override
  public void checkGetDomains() throws Exception {
    throw new SecurityException(String.format("permission denied for method getDomains()"));
  }

  @Override
  public void checkAddNotificationListener(final ObjectName name, final NotificationListener listener, final NotificationFilter filter, final Object handback) throws Exception {
    throw new SecurityException(String.format("permission denied for method addNotificationListener(%s, %s, %s, %s)", name, listener, filter, handback));
  }

  @Override
  public void checkAddNotificationListener(final ObjectName name, final ObjectName listener, final NotificationFilter filter, final Object handback) throws Exception {
    throw new SecurityException(String.format("permission denied for method addNotificationListener(%s, %s, %s, %s)", name, listener, filter, handback));
  }

  @Override
  public void checkRemoveNotificationListener(final ObjectName name, final ObjectName listener) throws Exception {
    throw new SecurityException(String.format("permission denied for method removeNotificationListener(%s, %s)", name, listener));
  }

  @Override
  public void checkRemoveNotificationListener(final ObjectName name, final ObjectName listener, final NotificationFilter filter, final Object handback) throws Exception {
    throw new SecurityException(String.format("permission denied for method removeNotificationListener(%s, %s, %s, %s)", name, listener, filter, handback));
  }

  @Override
  public void checkRemoveNotificationListener(final ObjectName name, final NotificationListener listener) throws Exception {
    throw new SecurityException(String.format("permission denied for method removeNotificationListener(%s, %s)", name, listener));
  }

  @Override
  public void checkRemoveNotificationListener(final ObjectName name, final NotificationListener listener, final NotificationFilter filter, final Object handback) throws Exception {
    throw new SecurityException(String.format("permission denied for method removeNotificationListener(%s, %s, %s, %s)", name, listener, filter, handback));
  }

  @Override
  public void checkGetMBeanInfo(final ObjectName name) throws Exception {
    throw new SecurityException(String.format("permission denied for method getMBeanInfo(%s)", name));
  }

  @Override
  public void checkIsInstanceOf(final ObjectName name, final String className) throws Exception {
    throw new SecurityException(String.format("permission denied for method isInstanceOf(%s, %s)", name, className));
  }
}
