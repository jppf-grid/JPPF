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

package org.jppf.management;

import java.io.ObjectInputStream;
import java.util.Set;

import javax.management.*;
import javax.management.loading.ClassLoaderRepository;
import javax.management.remote.MBeanServerForwarder;

/**
 * This class is an adapter for the {@link MBeanServerForwarder} interface. It implements all th emethods of the interface
 * and merely delegates to the underlying {@link MBeanServer} if it has been set. When the {@link MBeanServer} is not set
 * the methods do nothing and/or return {@code null}.
 * <p>This class is provided as a convenience for when only a few methods need to be overriden in a subclass.
 * @author Laurent Cohen
 */
public class MBeanServerForwarderAdapter implements MBeanServerForwarder {
  /**
   * The underlyng {@link MBeanServer} this object delegates to.
   */
  private MBeanServer mbs;
  /**
   * Optional parameters that may be passed by the configuration.
   */
  protected String[] parameters;

  @Override
  public ObjectInstance createMBean(final String className, final ObjectName name)
    throws ReflectionException, InstanceAlreadyExistsException, MBeanRegistrationException, MBeanException, NotCompliantMBeanException {
    return (mbs == null) ? null : mbs.createMBean(className, name);
  }

  @Override
  public ObjectInstance createMBean(final String className, final ObjectName name, final ObjectName loaderName)
    throws ReflectionException, InstanceAlreadyExistsException, MBeanRegistrationException, MBeanException, NotCompliantMBeanException, InstanceNotFoundException {
    return (mbs == null) ? null : mbs.createMBean(className, name, loaderName);
  }

  @Override
  public ObjectInstance createMBean(final String className, final ObjectName name, final Object[] params, final String[] signature)
    throws ReflectionException, InstanceAlreadyExistsException, MBeanRegistrationException, MBeanException, NotCompliantMBeanException {
    return (mbs == null) ? null : mbs.createMBean(className, name, params, signature);
  }

  @Override
  public ObjectInstance createMBean(final String className, final ObjectName name, final ObjectName loaderName, final Object[] params, final String[] signature)
    throws ReflectionException, InstanceAlreadyExistsException, MBeanRegistrationException, MBeanException, NotCompliantMBeanException, InstanceNotFoundException {
    return (mbs == null) ? null : mbs.createMBean(className, name, loaderName, params, signature);
  }

  @Override
  public ObjectInstance registerMBean(final Object object, final ObjectName name) throws InstanceAlreadyExistsException, MBeanRegistrationException, NotCompliantMBeanException {
    return (mbs == null) ? null : mbs.registerMBean(object, name);
  }

  @Override
  public void unregisterMBean(final ObjectName name) throws InstanceNotFoundException, MBeanRegistrationException {
    if (mbs != null) mbs.unregisterMBean(name);
  }

  @Override
  public ObjectInstance getObjectInstance(final ObjectName name) throws InstanceNotFoundException {
    return (mbs == null) ? null : mbs.getObjectInstance(name);
  }

  @Override
  public Set<ObjectInstance> queryMBeans(final ObjectName name, final QueryExp query) {
    return (mbs == null) ? null : mbs.queryMBeans(name, query);
  }

  @Override
  public Set<ObjectName> queryNames(final ObjectName name, final QueryExp query) {
    return (mbs == null) ? null : mbs.queryNames(name, query);
  }

  @Override
  public boolean isRegistered(final ObjectName name) {
    return (mbs == null) ? null : mbs.isRegistered(name);
  }

  @Override
  public Integer getMBeanCount() {
    return (mbs == null) ? null : mbs.getMBeanCount();
  }

  @Override
  public Object getAttribute(final ObjectName name, final String attribute) throws MBeanException, AttributeNotFoundException, InstanceNotFoundException, ReflectionException {
    return (mbs == null) ? null : mbs.getAttribute(name, attribute);
  }

  @Override
  public AttributeList getAttributes(final ObjectName name, final String[] attributes) throws InstanceNotFoundException, ReflectionException {
    return (mbs == null) ? null : mbs.getAttributes(name, attributes);
  }

  @Override
  public void setAttribute(final ObjectName name, final Attribute attribute) throws InstanceNotFoundException, AttributeNotFoundException, InvalidAttributeValueException, MBeanException, ReflectionException {
    mbs.setAttribute(name, attribute);
  }

  @Override
  public AttributeList setAttributes(final ObjectName name, final AttributeList attributes) throws InstanceNotFoundException, ReflectionException {
    return (mbs == null) ? null : mbs.setAttributes(name, attributes);
  }

  @Override
  public Object invoke(final ObjectName name, final String operationName, final Object[] params, final String[] signature) throws InstanceNotFoundException, MBeanException, ReflectionException {
    return (mbs == null) ? null : mbs.invoke(name, operationName, params, signature);
  }

  @Override
  public String getDefaultDomain() {
    return (mbs == null) ? null : mbs.getDefaultDomain();
  }

  @Override
  public String[] getDomains() {
    return (mbs == null) ? null : mbs.getDomains();
  }

  @Override
  public void addNotificationListener(final ObjectName name, final NotificationListener listener, final NotificationFilter filter, final Object handback) throws InstanceNotFoundException {
    if (mbs != null) mbs.addNotificationListener(name, listener, filter, handback);
  }

  @Override
  public void addNotificationListener(final ObjectName name, final ObjectName listener, final NotificationFilter filter, final Object handback) throws InstanceNotFoundException {
    if (mbs != null) mbs.addNotificationListener(name, listener, filter, handback);
  }

  @Override
  public void removeNotificationListener(final ObjectName name, final ObjectName listener) throws InstanceNotFoundException, ListenerNotFoundException {
    if (mbs != null) mbs.removeNotificationListener(name, listener);
  }

  @Override
  public void removeNotificationListener(final ObjectName name, final ObjectName listener, final NotificationFilter filter, final Object handback) throws InstanceNotFoundException, ListenerNotFoundException {
    if (mbs != null) mbs.removeNotificationListener(name, listener, filter, handback);
  }

  @Override
  public void removeNotificationListener(final ObjectName name, final NotificationListener listener) throws InstanceNotFoundException, ListenerNotFoundException {
    if (mbs != null) mbs.removeNotificationListener(name, listener);
  }

  @Override
  public void removeNotificationListener(final ObjectName name, final NotificationListener listener, final NotificationFilter filter, final Object handback) throws InstanceNotFoundException, ListenerNotFoundException {
    mbs.removeNotificationListener(name, listener, filter, handback);
  }

  @Override
  public MBeanInfo getMBeanInfo(final ObjectName name) throws InstanceNotFoundException, IntrospectionException, ReflectionException {
    return (mbs == null) ? null : mbs.getMBeanInfo(name);
  }

  @Override
  public boolean isInstanceOf(final ObjectName name, final String className) throws InstanceNotFoundException {
    return (mbs == null) ? null : mbs.isInstanceOf(name, className);
  }

  @Override
  public Object instantiate(final String className) throws ReflectionException, MBeanException {
    return (mbs == null) ? null : mbs.instantiate(className);
  }

  @Override
  public Object instantiate(final String className, final ObjectName loaderName) throws ReflectionException, MBeanException, InstanceNotFoundException {
    return (mbs == null) ? null : mbs.instantiate(className, loaderName);
  }

  @Override
  public Object instantiate(final String className, final Object[] params, final String[] signature) throws ReflectionException, MBeanException {
    return (mbs == null) ? null : mbs.instantiate(className, params, signature);
  }

  @Override
  public Object instantiate(final String className, final ObjectName loaderName, final Object[] params, final String[] signature) throws ReflectionException, MBeanException, InstanceNotFoundException {
    return (mbs == null) ? null : mbs.instantiate(className, loaderName, params, signature);
  }

  @Override
  @SuppressWarnings("deprecation")
  public ObjectInputStream deserialize(final ObjectName name, final byte[] data) throws InstanceNotFoundException, OperationsException {
    return (mbs == null) ? null : mbs.deserialize(name, data);
  }

  @Override
  @SuppressWarnings("deprecation")
  public ObjectInputStream deserialize(final String className, final byte[] data) throws OperationsException, ReflectionException {
    return (mbs == null) ? null : mbs.deserialize(className, data);
  }

  @Override
  @SuppressWarnings("deprecation")
  public ObjectInputStream deserialize(final String className, final ObjectName loaderName, final byte[] data) throws InstanceNotFoundException, OperationsException, ReflectionException {
    return (mbs == null) ? null : mbs.deserialize(className, loaderName, data);
  }

  @Override
  public ClassLoader getClassLoaderFor(final ObjectName mbeanName) throws InstanceNotFoundException {
    return (mbs == null) ? null : mbs.getClassLoaderFor(mbeanName);
  }

  @Override
  public ClassLoader getClassLoader(final ObjectName loaderName) throws InstanceNotFoundException {
    return (mbs == null) ? null : mbs.getClassLoader(loaderName);
  }

  @Override
  public ClassLoaderRepository getClassLoaderRepository() {
    return (mbs == null) ? null : mbs.getClassLoaderRepository();
  }

  @Override
  public MBeanServer getMBeanServer() {
    return mbs;
  }

  @Override
  public void setMBeanServer(final MBeanServer mbs) {
    this.mbs = mbs;
  }

  /**
   * Set the parameters defined in the configuration, if any.
   * @param parameters the parameters as an array of strings.
   */
  public void setParameters(final String[] parameters) {
    this.parameters = parameters;
  }

  /**
   * Get the parameters defined in the configuration, if any.
   * @return the parameters as an array of strings.
   */
  public String[] getParameters() {
    return parameters;
  }
}
