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
 * The subject is available as a {@code protected} instance variable, as well as via its accessors {@link #getSubject()} and {@link #setSubject(Subject)}.
 * @author Laurent Cohen
 */
public class JMXAuthorizationCheckerAdapter implements JMXAuthorizationChecker {
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
  }

  @Override
  public void checkCreateMBean(final String className, final ObjectName name, final ObjectName loaderName) throws Exception {
  }

  @Override
  public void checkCreateMBean(final String className, final ObjectName name, final Object[] params, final String[] signature) throws Exception {
  }

  @Override
  public void checkCreateMBean(final String className, final ObjectName name, final ObjectName loaderName, final Object[] params, final String[] signature) throws Exception {
  }

  @Override
  public void checkUnregisterMBean(final ObjectName name) throws Exception {
  }

  @Override
  public void checkGetObjectInstance(final ObjectName name) throws Exception {
  }

  @Override
  public void checkQueryMBeans(final ObjectName name, final QueryExp query) throws Exception {
  }

  @Override
  public void checkQueryNames(final ObjectName name, final QueryExp query) throws Exception {
  }

  @Override
  public void checkIsRegistered(final ObjectName name) throws Exception {
  }

  @Override
  public void checkGetMBeanCount() throws Exception {
  }

  @Override
  public void checkGetAttribute(final ObjectName name, final String attribute) throws Exception {
  }

  @Override
  public void checkGetAttributes(final ObjectName name, final String[] attributes) throws Exception {
  }

  @Override
  public void checkSetAttribute(final ObjectName name, final Attribute attribute) throws Exception {
  }

  @Override
  public void checkSetAttributes(final ObjectName name, final AttributeList attributes) throws Exception {
  }

  @Override
  public void checkInvoke(final ObjectName name, final String operationName, final Object[] params, final String[] signature) throws Exception {
  }

  @Override
  public void checkGetDefaultDomain() throws Exception {
  }

  @Override
  public void checkGetDomains() throws Exception {
  }

  @Override
  public void checkAddNotificationListener(final ObjectName name, final NotificationListener listener, final NotificationFilter filter, final Object handback) throws Exception {
  }

  @Override
  public void checkAddNotificationListener(final ObjectName name, final ObjectName listener, final NotificationFilter filter, final Object handback) throws Exception {
  }

  @Override
  public void checkRemoveNotificationListener(final ObjectName name, final ObjectName listener) throws Exception {
  }

  @Override
  public void checkRemoveNotificationListener(final ObjectName name, final ObjectName listener, final NotificationFilter filter, final Object handback) throws Exception {
  }

  @Override
  public void checkRemoveNotificationListener(final ObjectName name, final NotificationListener listener) throws Exception {
  }

  @Override
  public void checkRemoveNotificationListener(final ObjectName name, final NotificationListener listener, final NotificationFilter filter, final Object handback) throws Exception {
  }

  @Override
  public void checkGetMBeanInfo(final ObjectName name) throws Exception {
  }

  @Override
  public void checkIsInstanceOf(final ObjectName name, final String className) throws Exception {
  }
}
