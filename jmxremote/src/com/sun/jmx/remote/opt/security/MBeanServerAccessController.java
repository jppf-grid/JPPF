/*
 * JPPF.
 * Copyright (C) 2005-2014 JPPF Team.
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
/*
 * @(#)MBeanServerAccessController.java	1.3
 *
 *
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2007 Sun Microsystems, Inc. All Rights Reserved.
 *
 * The contents of this file are subject to the terms of either the GNU General
 * Public License Version 2 only ("GPL") or the Common Development and
 * Distribution License("CDDL")(collectively, the "License"). You may not use
 * this file except in compliance with the License. You can obtain a copy of the
 * License at http://opendmk.dev.java.net/legal_notices/licenses.txt or in the
 * LEGAL_NOTICES folder that accompanied this code. See the License for the
 * specific language governing permissions and limitations under the License.
 *
 * When distributing the software, include this License Header Notice in each
 * file and include the License file found at
 *     http://opendmk.dev.java.net/legal_notices/licenses.txt
 * or in the LEGAL_NOTICES folder that accompanied this code.
 * Sun designates this particular file as subject to the "Classpath" exception
 * as provided by Sun in the GPL Version 2 section of the License file that
 * accompanied this code.
 *
 * If applicable, add the following below the License Header, with the fields
 * enclosed by brackets [] replaced by your own identifying information:
 *
 *       "Portions Copyrighted [year] [name of copyright owner]"
 *
 * Contributor(s):
 *
 * If you wish your version of this file to be governed by only the CDDL or
 * only the GPL Version 2, indicate your decision by adding
 *
 *       "[Contributor] elects to include this software in this distribution
 *        under the [CDDL or GPL Version 2] license."
 *
 * If you don't indicate a single choice of license, a recipient has the option
 * to distribute your version of this file under either the CDDL or the GPL
 * Version 2, or to extend the choice of license to its licensees as provided
 * above. However, if you add GPL Version 2 code and therefore, elected the
 * GPL Version 2 license, then the option applies only if the new code is made
 * subject to such option by the copyright holder.
 *
 */

package com.sun.jmx.remote.opt.security;

import java.io.ObjectInputStream;
import java.util.Set;

import javax.management.*;
import javax.management.loading.ClassLoaderRepository;
import javax.management.remote.MBeanServerForwarder;

/**
 * <p>
 * An object of this class implements the MBeanServer interface and, for each of its methods, calls an appropriate checking method and then forwards the request to a wrapped MBeanServer object. The
 * checking method may throw a RuntimeException if the operation is not allowed; in this case the request is not forwarded to the wrapped object.
 * <p>
 * A typical use of this class is to insert it between a connector server such as the RMI connector and the MBeanServer with which the connector is associated. Requests from the connector client can
 * then be filtered and those operations that are not allowed, or not allowed in a particular context, can be rejected by throwing a <code>SecurityException</code> in the corresponding
 * <code>check*</code> method.
 * <p>
 * This is an abstract class, because in its implementation none of the checking methods does anything. To be useful, it must be subclassed and at least one of the checking methods overridden to do
 * some checking. Some or all of the MBeanServer methods may also be overridden, for instance if the default checking behaviour is inappropriate.
 * <p>
 * If there is no SecurityManager, then the access controller will refuse to create an MBean that is a ClassLoader, which includes MLets, or to execute the method addURL on an MBean that is an MLet.
 * This prevents people from opening security holes unintentionally. Otherwise, it would not be obvious that granting write access grants the ability to download and execute arbitrary code in the
 * target MBean server. Advanced users who do want the ability to use MLets are presumably advanced enough to handle policy files and security managers.
 */
public abstract class MBeanServerAccessController implements MBeanServerForwarder {
  /**
   * 
   */
  private MBeanServer mbs;

  @Override
  public MBeanServer getMBeanServer() {
    return mbs;
  }

  @Override
  public void setMBeanServer(final MBeanServer mbs) {
    if (mbs == null) throw new IllegalArgumentException("Null MBeanServer");
    if (this.mbs != null) throw new IllegalArgumentException("MBeanServer object already " + "initialized");
    this.mbs = mbs;
  }

  /**
   * Check if the caller can do read operations. This method does nothing if so, otherwise throws SecurityException.
   */
  protected abstract void checkRead();

  /**
   * Check if the caller can do write operations. This method does nothing if so, otherwise throws SecurityException.
   */
  protected abstract void checkWrite();

  //--------------------------------------------
  // Implementation of the MBeanServer interface
  //--------------------------------------------

  /*
   * Call <code>checkRead()</code>, then forward this method to the wrapped object.
   */
  @Override
  public void addNotificationListener(final ObjectName name, final NotificationListener listener, final NotificationFilter filter, final Object handback) throws InstanceNotFoundException {
    checkRead();
    getMBeanServer().addNotificationListener(name, listener, filter, handback);
  }

  /*
   * Call <code>checkRead()</code>, then forward this method to the wrapped object.
   */
  @Override
  public void addNotificationListener(final ObjectName name, final ObjectName listener, final NotificationFilter filter, final Object handback) throws InstanceNotFoundException {
    checkRead();
    getMBeanServer().addNotificationListener(name, listener, filter, handback);
  }

  /*
   * Call <code>checkWrite()</code>, then forward this method to the wrapped object.
   */
  @Override
  public ObjectInstance createMBean(final String className, final ObjectName name) throws ReflectionException, InstanceAlreadyExistsException, MBeanRegistrationException, MBeanException,
  NotCompliantMBeanException {
    checkWrite();
    SecurityManager sm = System.getSecurityManager();
    if (sm == null) {
      Object object = getMBeanServer().instantiate(className);
      checkClassLoader(object);
      return getMBeanServer().registerMBean(object, name);
    } else {
      return getMBeanServer().createMBean(className, name);
    }
  }

  /*
   * Call <code>checkWrite()</code>, then forward this method to the wrapped object.
   */
  @Override
  public ObjectInstance createMBean(final String className, final ObjectName name, final Object params[], final String signature[]) throws ReflectionException, InstanceAlreadyExistsException, MBeanRegistrationException,
  MBeanException, NotCompliantMBeanException {
    checkWrite();
    SecurityManager sm = System.getSecurityManager();
    if (sm == null) {
      Object object = getMBeanServer().instantiate(className, params, signature);
      checkClassLoader(object);
      return getMBeanServer().registerMBean(object, name);
    } else {
      return getMBeanServer().createMBean(className, name, params, signature);
    }
  }

  /*
   * Call <code>checkWrite()</code>, then forward this method to the wrapped object.
   */
  @Override
  public ObjectInstance createMBean(final String className, final ObjectName name, final ObjectName loaderName) throws ReflectionException, InstanceAlreadyExistsException, MBeanRegistrationException, MBeanException,
  NotCompliantMBeanException, InstanceNotFoundException {
    checkWrite();
    SecurityManager sm = System.getSecurityManager();
    if (sm == null) {
      Object object = getMBeanServer().instantiate(className, loaderName);
      checkClassLoader(object);
      return getMBeanServer().registerMBean(object, name);
    } else {
      return getMBeanServer().createMBean(className, name, loaderName);
    }
  }

  /*
   * Call <code>checkWrite()</code>, then forward this method to the wrapped object.
   */
  @Override
  public ObjectInstance createMBean(final String className, final ObjectName name, final ObjectName loaderName, final Object params[], final String signature[]) throws ReflectionException, InstanceAlreadyExistsException,
  MBeanRegistrationException, MBeanException, NotCompliantMBeanException, InstanceNotFoundException {
    checkWrite();
    SecurityManager sm = System.getSecurityManager();
    if (sm == null) {
      Object object = getMBeanServer().instantiate(className, loaderName, params, signature);
      checkClassLoader(object);
      return getMBeanServer().registerMBean(object, name);
    } else {
      return getMBeanServer().createMBean(className, name, loaderName, params, signature);
    }
  }

  /*
   * Call <code>checkRead()</code>, then forward this method to the wrapped object.
   */
  @Override
  @SuppressWarnings("deprecation")
  public ObjectInputStream deserialize(final ObjectName name, final byte[] data) throws InstanceNotFoundException, OperationsException {
    checkRead();
    return getMBeanServer().deserialize(name, data);
  }

  /*
   * Call <code>checkRead()</code>, then forward this method to the wrapped object.
   */
  @Override
  @SuppressWarnings("deprecation")
  public ObjectInputStream deserialize(final String className, final byte[] data) throws OperationsException, ReflectionException {
    checkRead();
    return getMBeanServer().deserialize(className, data);
  }

  /*
   * Call <code>checkRead()</code>, then forward this method to the wrapped object.
   */
  @Override
  @SuppressWarnings("deprecation")
  public ObjectInputStream deserialize(final String className, final ObjectName loaderName, final byte[] data) throws InstanceNotFoundException, OperationsException, ReflectionException {
    checkRead();
    return getMBeanServer().deserialize(className, loaderName, data);
  }

  /*
   * Call <code>checkRead()</code>, then forward this method to the wrapped object.
   */
  @Override
  public Object getAttribute(final ObjectName name, final String attribute) throws MBeanException, AttributeNotFoundException, InstanceNotFoundException, ReflectionException {
    checkRead();
    return getMBeanServer().getAttribute(name, attribute);
  }

  /*
   * Call <code>checkRead()</code>, then forward this method to the wrapped object.
   */
  @Override
  public AttributeList getAttributes(final ObjectName name, final String[] attributes) throws InstanceNotFoundException, ReflectionException {
    checkRead();
    return getMBeanServer().getAttributes(name, attributes);
  }

  /*
   * Call <code>checkRead()</code>, then forward this method to the wrapped object.
   */
  @Override
  public ClassLoader getClassLoader(final ObjectName loaderName) throws InstanceNotFoundException {
    checkRead();
    return getMBeanServer().getClassLoader(loaderName);
  }

  /*
   * Call <code>checkRead()</code>, then forward this method to the wrapped object.
   */
  @Override
  public ClassLoader getClassLoaderFor(final ObjectName mbeanName) throws InstanceNotFoundException {
    checkRead();
    return getMBeanServer().getClassLoaderFor(mbeanName);
  }

  /*
   * Call <code>checkRead()</code>, then forward this method to the wrapped object.
   */
  @Override
  public ClassLoaderRepository getClassLoaderRepository() {
    checkRead();
    return getMBeanServer().getClassLoaderRepository();
  }

  /*
   * Call <code>checkRead()</code>, then forward this method to the wrapped object.
   */
  @Override
  public String getDefaultDomain() {
    checkRead();
    return getMBeanServer().getDefaultDomain();
  }

  /*
   * Call <code>checkRead()</code>, then forward this method to the wrapped object.
   */
  @Override
  public String[] getDomains() {
    checkRead();
    return getMBeanServer().getDomains();
  }

  /*
   * Call <code>checkRead()</code>, then forward this method to the wrapped object.
   */
  @Override
  public Integer getMBeanCount() {
    checkRead();
    return getMBeanServer().getMBeanCount();
  }

  /*
   * Call <code>checkRead()</code>, then forward this method to the wrapped object.
   */
  @Override
  public MBeanInfo getMBeanInfo(final ObjectName name) throws InstanceNotFoundException, IntrospectionException, ReflectionException {
    checkRead();
    return getMBeanServer().getMBeanInfo(name);
  }

  /*
   * Call <code>checkRead()</code>, then forward this method to the wrapped object.
   */
  @Override
  public ObjectInstance getObjectInstance(final ObjectName name) throws InstanceNotFoundException {
    checkRead();
    return getMBeanServer().getObjectInstance(name);
  }

  /*
   * Call <code>checkWrite()</code>, then forward this method to the wrapped object.
   */
  @Override
  public Object instantiate(final String className) throws ReflectionException, MBeanException {
    checkWrite();
    return getMBeanServer().instantiate(className);
  }

  /*
   * Call <code>checkWrite()</code>, then forward this method to the wrapped object.
   */
  @Override
  public Object instantiate(final String className, final Object params[], final String signature[]) throws ReflectionException, MBeanException {
    checkWrite();
    return getMBeanServer().instantiate(className, params, signature);
  }

  /*
   * Call <code>checkWrite()</code>, then forward this method to the wrapped object.
   */
  @Override
  public Object instantiate(final String className, final ObjectName loaderName) throws ReflectionException, MBeanException, InstanceNotFoundException {
    checkWrite();
    return getMBeanServer().instantiate(className, loaderName);
  }

  /*
   * Call <code>checkWrite()</code>, then forward this method to the wrapped object.
   */
  @Override
  public Object instantiate(final String className, final ObjectName loaderName, final Object params[], final String signature[]) throws ReflectionException, MBeanException, InstanceNotFoundException {
    checkWrite();
    return getMBeanServer().instantiate(className, loaderName, params, signature);
  }

  /*
   * Call <code>checkWrite()</code>, then forward this method to the wrapped object.
   */
  @Override
  public Object invoke(final ObjectName name, final String operationName, final Object params[], final String signature[]) throws InstanceNotFoundException, MBeanException, ReflectionException {
    checkWrite();
    checkMLetAddURL(name, operationName);
    return getMBeanServer().invoke(name, operationName, params, signature);
  }

  /*
   * Call <code>checkRead()</code>, then forward this method to the wrapped object.
   */
  @Override
  public boolean isInstanceOf(final ObjectName name, final String className) throws InstanceNotFoundException {
    checkRead();
    return getMBeanServer().isInstanceOf(name, className);
  }

  /*
   * Call <code>checkRead()</code>, then forward this method to the wrapped object.
   */
  @Override
  public boolean isRegistered(final ObjectName name) {
    checkRead();
    return getMBeanServer().isRegistered(name);
  }

  /*
   * Call <code>checkRead()</code>, then forward this method to the wrapped object.
   */
  @Override
  public Set<ObjectInstance> queryMBeans(final ObjectName name, final QueryExp query) {
    checkRead();
    return getMBeanServer().queryMBeans(name, query);
  }

  /*
   * Call <code>checkRead()</code>, then forward this method to the wrapped object.
   */
  @Override
  public Set<ObjectName> queryNames(final ObjectName name, final QueryExp query) {
    checkRead();
    return getMBeanServer().queryNames(name, query);
  }

  /*
   * Call <code>checkWrite()</code>, then forward this method to the wrapped object.
   */
  @Override
  public ObjectInstance registerMBean(final Object object, final ObjectName name) throws InstanceAlreadyExistsException, MBeanRegistrationException, NotCompliantMBeanException {
    checkWrite();
    return getMBeanServer().registerMBean(object, name);
  }

  /*
   * Call <code>checkRead()</code>, then forward this method to the wrapped object.
   */
  @Override
  public void removeNotificationListener(final ObjectName name, final NotificationListener listener) throws InstanceNotFoundException, ListenerNotFoundException {
    checkRead();
    getMBeanServer().removeNotificationListener(name, listener);
  }

  /*
   * Call <code>checkRead()</code>, then forward this method to the wrapped object.
   */
  @Override
  public void removeNotificationListener(final ObjectName name, final NotificationListener listener, final NotificationFilter filter, final Object handback) throws InstanceNotFoundException, ListenerNotFoundException {
    checkRead();
    getMBeanServer().removeNotificationListener(name, listener, filter, handback);
  }

  /*
   * Call <code>checkRead()</code>, then forward this method to the wrapped object.
   */
  @Override
  public void removeNotificationListener(final ObjectName name, final ObjectName listener) throws InstanceNotFoundException, ListenerNotFoundException {
    checkRead();
    getMBeanServer().removeNotificationListener(name, listener);
  }

  /*
   * Call <code>checkRead()</code>, then forward this method to the wrapped object.
   */
  @Override
  public void removeNotificationListener(final ObjectName name, final ObjectName listener, final NotificationFilter filter, final Object handback) throws InstanceNotFoundException, ListenerNotFoundException {
    checkRead();
    getMBeanServer().removeNotificationListener(name, listener, filter, handback);
  }

  /*
   * Call <code>checkWrite()</code>, then forward this method to the wrapped object.
   */
  @Override
  public void setAttribute(final ObjectName name, final Attribute attribute) throws InstanceNotFoundException, AttributeNotFoundException, InvalidAttributeValueException, MBeanException, ReflectionException {
    checkWrite();
    getMBeanServer().setAttribute(name, attribute);
  }

  /*
   * Call <code>checkWrite()</code>, then forward this method to the wrapped object.
   */
  @Override
  public AttributeList setAttributes(final ObjectName name, final AttributeList attributes) throws InstanceNotFoundException, ReflectionException {
    checkWrite();
    return getMBeanServer().setAttributes(name, attributes);
  }

  /*
   * Call <code>checkWrite()</code>, then forward this method to the wrapped object.
   */
  @Override
  public void unregisterMBean(final ObjectName name) throws InstanceNotFoundException, MBeanRegistrationException {
    checkWrite();
    getMBeanServer().unregisterMBean(name);
  }

  /**
   * 
   * @param object .
   */
  private void checkClassLoader(final Object object) {
    if (object instanceof ClassLoader) throw new SecurityException("Access denied! Creating an " + "MBean that is a ClassLoader " + "is forbidden unless a security " + "manager is installed.");
  }

  /**
   * 
   * @param name .
   * @param operationName .
   * @throws InstanceNotFoundException .
   */
  private void checkMLetAddURL(final ObjectName name, final String operationName) throws InstanceNotFoundException {
    SecurityManager sm = System.getSecurityManager();
    if (sm == null) {
      if (operationName.equals("addURL") && getMBeanServer().isInstanceOf(name, "javax.management.loading.MLet")) throw new SecurityException("Access denied! MLet method "
          + "addURL cannot be invoked " + "unless a security manager " + "is installed.");
    }
  }
}
