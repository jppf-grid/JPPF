/*
 * JPPF.
 * Copyright (C) 2005-2019 JPPF Team.
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

package org.jppf.management.doc;

import javax.management.*;

import org.jppf.discovery.DriverConnectionInfo;

/**
 * Visitor interface for exploring the information on all JPPF MBeans in a specified remote MBean server.
 * @author Laurent Cohen
 */
public interface MBeanInfoVisitor {
  /**
   * Start the visit for the specified remote MBean server.
   * @param connectionInfo represent the information to connect the remote JMX server.
   * @throws Exception if any error occurs.
   */
  void start(DriverConnectionInfo connectionInfo) throws Exception;

  /**
   * End the visit for the specified remote MBean server.
   * @param connectionInfo information of the remote JVM to connect to.
   * @throws Exception if any error occurs.
   */
  void end(DriverConnectionInfo connectionInfo) throws Exception;

  /**
   * Start the visit for the specified mbean.
   * @param name the object name of the mbea,.
   * @param info full information on the mbena.
   * @throws Exception if any error occurs.
   */
  void startMBean(final ObjectName name, final MBeanInfo info) throws Exception;

  /**
   * End the visit for the specified mbean.
   * @param name the object name of the mbea,.
   * @param info full information on the mbena.
   * @throws Exception if any error occurs.
   */
  void endMBean(final ObjectName name, final MBeanInfo info) throws Exception;

  /**
   * Start visiting the attributes of the specified mbean.
   * @param attributes the attributes to visit.
   * @throws Exception if any error occurs.
   */
  void startAttributes(final MBeanAttributeInfo[] attributes) throws Exception;

  /**
   * End visiting the attributes of the specified mbean.
   * @param attributes the attributes to visit.
   * @throws Exception if any error occurs.
   */
  void endAttributes(final MBeanAttributeInfo[] attributes) throws Exception;

  /**
   * Visit the specified mbean attribute.
   * @param attribute info on the mbean attribute
   * @throws Exception if any error occurs.
   */
  void visitAttribute(final MBeanAttributeInfo attribute) throws Exception;

  /**
   * Start visiting the operations of the specified mbean.
   * @param operations the mbean operations to visit.
   * @throws Exception if any error occurs.
   */
  void startOperations(final MBeanOperationInfo[] operations) throws Exception;

  /**
   * End visiting the operations of the specified mbean.
   * @param operations the mbean operations to visit.
   * @throws Exception if any error occurs.
   */
  void endOperations(final MBeanOperationInfo[] operations) throws Exception;

  /**
   * Visit the specified mbean operation.
   * @param operation info on the mbean operation.
   * @throws Exception if any error occurs.
   */
  void visitOperation(final MBeanOperationInfo operation) throws Exception;

  /**
   * Functional interface to filter the MBean elements (attributes or operations) to visit.
   */
  @FunctionalInterface
  interface MBeanFeatureFilter {
    /**
     * Determine whether the specified MBean feature should be accepted.
     * @param feature the feature to validate.
     * @return {@code true} if the feature is accepted, {@code false} otherwise.
     */
    boolean accepts(MBeanFeatureInfo feature);
  }

  /**
   * Functional interface to filter the MBeans to visit.
   */
  @FunctionalInterface
  interface MBeanFilter {
    /**
     * Determine whether the specified MBean should be accepted.
     * @param info information on the MBean to validate.
     * @return {@code true} if the feature is accepted, {@code false} otherwise.
     */
    boolean accepts(MBeanInfo info);
  }

  /**
   * Perform the visit.
   * @param visitor the visitor to apply.
   * @param connectionInfo information of the remote JVM to connect to.
   * @param mbeanFilter an optional MBean filter, may be {@code null}.
   * @param featureFilter an optional MBean feature filter, may be {@code null}.
   * @throws Exception if any error occurs.
   */
  public static void visit(final MBeanInfoVisitor visitor, final DriverConnectionInfo connectionInfo, final MBeanFilter mbeanFilter, final MBeanFeatureFilter featureFilter) throws Exception {
    MBeanInfoExplorer.visit(visitor, connectionInfo, mbeanFilter, featureFilter);
  }

  /**
   * Perform the visit.
   * @param visitor the visitor to apply.
   * @param connectionInfo information of the remote JVM to connect to.
   * @throws Exception if any error occurs.
   */
  public static void visit(final MBeanInfoVisitor visitor, final DriverConnectionInfo connectionInfo) throws Exception {
    MBeanInfoExplorer.visit(visitor, connectionInfo, null, null);
  }
}
