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
 * 
 * @author Laurent Cohen
 */
public class MBeanInfoVisitorAdapter implements MBeanInfoVisitor {
  @Override
  public void start(final DriverConnectionInfo connectionInfo) throws Exception {
  }

  @Override
  public void end(final DriverConnectionInfo connectionInfo) throws Exception {
  }

  @Override
  public void startMBean(final ObjectName name, final MBeanInfo info) throws Exception {
  }

  @Override
  public void endMBean(final ObjectName name, final MBeanInfo info) throws Exception {
  }

  @Override
  public void startAttributes(final MBeanAttributeInfo[] attributes) throws Exception {
  }

  @Override
  public void endAttributes(final MBeanAttributeInfo[] attributes) throws Exception {
  }

  @Override
  public void visitAttribute(final MBeanAttributeInfo attribute) throws Exception {
  }

  @Override
  public void startOperations(final MBeanOperationInfo[] operations) throws Exception {
  }

  @Override
  public void endOperations(final MBeanOperationInfo[] operations) throws Exception {
  }

  @Override
  public void visitOperation(final MBeanOperationInfo operation) throws Exception {
  }
}
