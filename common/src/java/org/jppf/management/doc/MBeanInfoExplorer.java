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

import java.util.*;
import java.util.stream.Collectors;

import javax.management.*;

import org.jppf.JPPFTimeoutException;
import org.jppf.discovery.DriverConnectionInfo;
import org.jppf.management.*;
import org.jppf.management.doc.MBeanInfoVisitor.*;

/**
 * 
 * @author Laurent Cohen
 */
class MBeanInfoExplorer {
  /**
   * Descriptor field name for the textual description of an mbean or mbean element.
   */
  static final String DESCRIPTION_FIELD = "mbean.description";
  /**
   * Descriptor field name for the textual description of a notification emitted by an mbean.
   */
  static final String NOTIF_DESCRIPTION_FIELD = "mbean.notif.description";
  /**
   * Descriptor field name for the class of a notification emitted by an mbean.
   */
  static final String NOTIF_CLASS_FIELD = "mbean.notif.class";
  /**
   * Descriptor field name for the class of the {@code userData} attribute of a notification emitted by an mbean.
   */
  static final String NOTIF_USER_DATA_CLASS_FIELD = "mbean.notif.user.data.class";
  /**
   * Descriptor field name for the textual description of the {@code userData} attribute of a notification emitted by an mbean.
   */
  static final String NOTIF_USER_DATA_DESCRIPTION_FIELD = "mbean.notif.user.data.description";
  /**
   * Descriptor field name for the name to give to a parameter in an mbean cosntructor or method.
   */
  static final String PARAM_NAME_FIELD = "mbean.param.name";
  /**
   * Descriptor field name for whether to exclude an mbean or mbean element when visiting an mbean info tree.
   */
  static final String EXCLUDE_FIELD = "mbean.exclude";
  /**
   * Descriptor field name for the raw type of a field or of a method return type.
   */
  static final String RAW_TYPE_FIELD = "mbean.raw.type";
  /**
   * Descriptor field name for the raw types of a a generic declaration instance.
   */
  static final String RAW_TYPE_PARAMS_FIELD = "mbean.raw.type.params";

  /**
   * Perform the visit.
   * @param visitor the visitor to apply.
   * @param connectionInfo information of the remote JVM to connect to.
   * @param mbeanFilter an optional MBean filter, may be {@code null}.
   * @param featureFilter an optional MBean feature filter, may be {@code null}.
   * @throws Exception if any error occurs.
   */
  static void visit(final MBeanInfoVisitor visitor, final DriverConnectionInfo connectionInfo, final MBeanFilter mbeanFilter, final MBeanFeatureFilter featureFilter) throws Exception {
    visitor.start(connectionInfo);
    try (final JMXConnectionWrapper jmx = new JMXConnectionWrapper(connectionInfo.getHost(), connectionInfo.getPort(), false)) {
      if (!jmx.connectAndWait(5000L)) throw new JPPFTimeoutException("could not connect to remote JVM " + connectionInfo);
      final MBeanServerConnection mbsc = jmx.getMbeanConnection();
      final Set<ObjectName> names = mbsc.queryNames(ObjectNameCache.getObjectName("org.jppf:*"), null);
      for (final ObjectName name: names) {
        final MBeanInfo info = mbsc.getMBeanInfo(name);
        if (((mbeanFilter != null) && !mbeanFilter.accepts(info)) || MBeanInfoExplorer.isExcluded(info.getDescriptor())) continue;
  
        visitor.startMBean(name, info);
  
        MBeanAttributeInfo[] attributes = info.getAttributes();
        final List<MBeanAttributeInfo> attrList = Arrays.stream(attributes)
          .filter(attr -> ((featureFilter == null) || featureFilter.accepts(attr)) && !MBeanInfoExplorer.isExcluded(attr.getDescriptor()))
          .collect(Collectors.toList());
        attributes = attrList.toArray(new MBeanAttributeInfo[attrList.size()]);
        visitor.startAttributes(attributes);
        for (final MBeanAttributeInfo attribute: attributes) visitor.visitAttribute(attribute);
        visitor.endAttributes(attributes);
  
        MBeanOperationInfo[] operations = info.getOperations();
        final List<MBeanOperationInfo> opList = Arrays.stream(operations)
          .filter(op -> ((featureFilter == null) || featureFilter.accepts(op)) && !MBeanInfoExplorer.isExcluded(op.getDescriptor()))
          .collect(Collectors.toList());
        operations = opList.toArray(new MBeanOperationInfo[opList.size()]);
        visitor.startOperations(operations);
        for (final MBeanOperationInfo operation: operations) visitor.visitOperation(operation);
        visitor.endOperations(operations);
  
        visitor.endMBean(name, info);
      }
    } finally {
      visitor.end(connectionInfo);
    }
  }

  /**
   * Determine whether the specified descriptor has an exlcude flag set.
   * @param descriptor the descriptor to evaluate.
   * @return {@code true} if the corresponding mbean element should beexcluded, {@code false} otherwise.
   */
  private static boolean isExcluded(final Descriptor descriptor) {
    final Boolean excluded = (Boolean) descriptor.getFieldValue(MBeanInfoExplorer.EXCLUDE_FIELD);
    return (excluded != null) && excluded;
  }
}
