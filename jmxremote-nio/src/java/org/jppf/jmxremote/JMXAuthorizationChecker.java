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
 * <p>This interface represents a set of authorization checks for a given authenticated subject.
 * <p>A check is available for each method available in the {@link MBeanServerConnection} interface
 * and is implemented as a method whose name is made of the "check" prefix, followed by the name of the method to check.
 * <p>An authorization failure is raised by throwing an exception in a {@code checkXXX()} method.
 * @author Laurent Cohen
 */
public interface JMXAuthorizationChecker {
  /**
   * Get the authenticated subject to check for authorization.
   * @return the authenticated {@link Subject}, if any.
   */
  Subject getSubject();

  /**
   * Set the authenticated subject to check for authorization.
   * @param subject the authenticated {@link Subject} to set.
   */
  void setSubject(Subject subject);

  /**
   * Check that the subject can invoke the {@link MBeanServerConnection#createMBean(String, ObjectName)} method on the mbean server.
   * @param className the class name of the MBean to instantiate. 
   * @param name the object name of the MBean. May be null.
   * @throws Exception if the subject is not authorized.
   */
  void checkCreateMBean(String className, ObjectName name) throws Exception;

  /**
   * Check that the subject can invoke the {@link MBeanServerConnection#createMBean(String, ObjectName, ObjectName)}  method on the mbean server.
   * @param className the class name of the MBean to be instantiated. 
   * @param name the object name of the MBean. May be null.
   * @param loaderName the object name of the class loader to be used.
   * @throws Exception if the subject is not authorized.
   */
  void checkCreateMBean(String className, ObjectName name, ObjectName loaderName) throws Exception;

  /**
   * Check that the subject can invoke the {@link MBeanServerConnection#createMBean(String, ObjectName, Object[], String[])}  method on the mbean server.
   * @param className the class name of the MBean to instantiate. 
   * @param name the object name of the MBean. May be null.
   * @param params an array containing the parameters of the constructor to invoke.
   * @param signature an array containing the signature of the constructor to invoke.
   * @throws Exception if the subject is not authorized.
   */
  void checkCreateMBean(String className, ObjectName name, Object[] params, String[] signature) throws Exception;

  /**
   * Check that the subject can invoke the {@link MBeanServerConnection#createMBean(String, ObjectName, ObjectName, Object[], String[])}  method on the mbean server.
   * @param className The class name of the MBean to instantiate. 
   * @param name the object name of the MBean. May be null.
   * @param loaderName the object name of the class loader to use.
   * @param params an array containing the parameters of the constructor to invoke.
   * @param signature an array containing the signature of the constructor to invoke.
   * @throws Exception if the subject is not authorized.
   */
  void checkCreateMBean(String className, ObjectName name, ObjectName loaderName, Object[] params, String[] signature) throws Exception;

  /**
   * Check that the subject can invoke the {@link MBeanServerConnection#unregisterMBean(ObjectName)}  method on the mbean server.
   * @param name the object name of the MBean to unregister.
   * @throws Exception if the subject is not authorized.
   */
  void checkUnregisterMBean(ObjectName name) throws Exception;

  /**
   * Check that the subject can invoke the {@link MBeanServerConnection#getObjectInstance(ObjectName)}  method on the mbean server.
   * @param name the object name of the MBean.
   * @throws Exception if the subject is not authorized.
   */
  void checkGetObjectInstance(ObjectName name) throws Exception;

  /**
   * Check that the subject can invoke the {@link MBeanServerConnection#queryMBeans(ObjectName, QueryExp)}  method on the mbean server.
   * @param name the object name pattern identifying the MBeans to retrieve.
   * @param query the query expression to apply for selecting MBeans.
   * @throws Exception if the subject is not authorized.
   */
  void checkQueryMBeans(ObjectName name, QueryExp query) throws Exception;

  /**
   * Check that the subject can invoke the {@link MBeanServerConnection#queryNames(ObjectName, QueryExp)}  method on the mbean server.
   * @param name the object name pattern identifying the MBean names to retrieve.
   * @param query the query expression to apply for selecting MBeans.
   * @throws Exception if the subject is not authorized.
   */
  void checkQueryNames(ObjectName name, QueryExp query) throws Exception;

  /**
   * Check that the subject can invoke the {@link MBeanServerConnection#isRegistered(ObjectName)}  method on the mbean server.
   * @param name the object name of the MBean to check.
   * @throws Exception if the subject is not authorized.
   */
  void checkIsRegistered(ObjectName name) throws Exception;

  /**
   * Check that the subject can invoke the {@link MBeanServerConnection#getMBeanCount()}  method on the mbean server.
   * @throws Exception if the subject is not authorized.
   */
  void checkGetMBeanCount() throws Exception;

  /**
   * Check that the subject can invoke the {@link MBeanServerConnection#getAttribute(ObjectName, String)}  method on the mbean server.
   * @param name the object name of the MBean from which the attribute is to be retrieved.
   * @param attribute A String specifying the name of the attribute to retrieve.
   * @throws Exception if the subject is not authorized.
   */
  void checkGetAttribute(ObjectName name, String attribute) throws Exception;

  /**
   * Check that the subject can invoke the {@link MBeanServerConnection#getAttributes(ObjectName, String[])}  method on the mbean server.
   * @param name the object name of the MBean from which the attributes are retrieved.
   * @param attributes a list of the attributes to retrieve.
   * @throws Exception if the subject is not authorized.
   */
  void checkGetAttributes(ObjectName name, String[] attributes) throws Exception;

  /**
   * Check that the subject can invoke the {@link MBeanServerConnection#setAttribute(ObjectName, Attribute)}  method on the mbean server.
   * @param name the name of the MBean within which the attribute is to be set.
   * @param attribute The identification of the attribute to set and the value to set it to.
   * @throws Exception if the subject is not authorized.
   */
  void checkSetAttribute(ObjectName name, Attribute attribute) throws Exception;

  /**
   * Check that the subject can invoke the {@link MBeanServerConnection#setAttributes(ObjectName, AttributeList)}  method on the mbean server.
   * @param name The object name of the MBean within which the attributes are to be set.
   * @param attributes A list of attributes: the identification of the attributes to set and the values to set them to.
   * @throws Exception if the subject is not authorized.
   */
  void checkSetAttributes(ObjectName name, AttributeList attributes) throws Exception;

  /**
   * Check that the subject can invoke the {@link MBeanServerConnection#invoke(ObjectName, String, Object[], String[])}  method on the mbean server.
   * @param name the object name of the MBean on which the method is invoked.
   * @param operationName the name of the operation to invoke.
   * @param params an array containing the parameters to set when the operation is invoked.
   * @param signature an array containing the signature of the operation, an array of class names in the format returned by {@link Class#getName()}.
   * @throws Exception if the subject is not authorized.
   */
  void checkInvoke(ObjectName name, String operationName, Object[] params, String[] signature) throws Exception;

  /**
   * Check that the subject can invoke the {@link MBeanServerConnection#getDefaultDomain()}  method on the mbean server.
   * @throws Exception if the subject is not authorized.
   */
  void checkGetDefaultDomain() throws Exception;

  /**
   * Check that the subject can invoke the {@link MBeanServerConnection#getDomains()}  method on the mbean server.
   * @throws Exception if the subject is not authorized.
   */
  void checkGetDomains() throws Exception;

  /**
   * Check that the subject can invoke the {@link MBeanServerConnection#addNotificationListener(ObjectName, NotificationListener, NotificationFilter, Object)}  method on the mbean server.
   * @param name the name of the MBean on which the listener should be added.
   * @param listener the listener object which will handle the notifications emitted by the registered MBean.
   * @param filter the filter object.
   * @param handback the context to send to the listener when a notification is emitted.
   * @throws Exception if the subject is not authorized.
   */
  void checkAddNotificationListener(ObjectName name, NotificationListener listener, NotificationFilter filter, Object handback) throws Exception;

  /**
   * Check that the subject can invoke the {@link MBeanServerConnection#addNotificationListener(ObjectName, ObjectName, NotificationFilter, Object)}  method on the mbean server.
   * @param name the name of the MBean on which the listener should be added.
   * @param listener the object name of the listener object which will handle the notifications emitted by the registered MBean.
   * @param filter the filter object.
   * @param handback the context to send to the listener when a notification is emitted.
   * @throws Exception if the subject is not authorized.
   */
  void checkAddNotificationListener(ObjectName name, ObjectName listener, NotificationFilter filter, Object handback) throws Exception;

  /**
   * Check that the subject can invoke the {@link MBeanServerConnection#removeNotificationListener(ObjectName, ObjectName)}  method on the mbean server.
   * @param name the name of the MBean on which the listener should be added.
   * @param listener the object name of the listener object which will handle the notifications emitted by the registered MBean.
   * @throws Exception if the subject is not authorized.
   */
  void checkRemoveNotificationListener(ObjectName name, ObjectName listener) throws Exception;

  /**
   * Check that the subject can invoke the {@link MBeanServerConnection#removeNotificationListener(ObjectName, ObjectName, NotificationFilter, Object)}  method on the mbean server.
   * @param name the name of the MBean on which the listener should be removed.
   * @param listener the object name of the listener to remove.
   * @param filter the filter that was specified when the listener was added.
   * @param handback the handback that was specified when the listener was added.
   * @throws Exception if the subject is not authorized.
   */
  void checkRemoveNotificationListener(ObjectName name, ObjectName listener, NotificationFilter filter, Object handback) throws Exception;

  /**
   * Check that the subject can invoke the {@link MBeanServerConnection#removeNotificationListener(ObjectName, NotificationListener)}  method on the mbean server.
   * @param name the name of the MBean on which the listener should be removed.
   * @param listener the listener object to remove.
   * @throws Exception if the subject is not authorized.
   */
  void checkRemoveNotificationListener(ObjectName name, NotificationListener listener) throws Exception;

  /**
   * Check that the subject can invoke the {@link MBeanServerConnection#removeNotificationListener(ObjectName, NotificationListener, NotificationFilter, Object)}  method on the mbean server.
   * @param name the name of the MBean on which the listener should be removed.
   * @param listener the listener object to remove.
   * @param filter the filter that was specified when the listener was added.
   * @param handback the handback that was specified when the listener was added.
   * @throws Exception if the subject is not authorized.
   */
  void checkRemoveNotificationListener(ObjectName name, NotificationListener listener, NotificationFilter filter, Object handback) throws Exception;

  /**
   * Check that the subject can invoke the {@link MBeanServerConnection#getMBeanInfo(ObjectName)}  method on the mbean server.
   * @param name the name of the MBean to analyze
   * @throws Exception if the subject is not authorized.
   */
  void checkGetMBeanInfo(ObjectName name) throws Exception;

  /**
   * Check that the subject can invoke the {@link MBeanServerConnection#isInstanceOf(ObjectName, String)}  method on the mbean server.
   * @param name the object name of the MBean.
   * @param className the name of the class.
   * @throws Exception if the subject is not authorized.
   */
  void checkIsInstanceOf(ObjectName name, String className) throws Exception;
}
