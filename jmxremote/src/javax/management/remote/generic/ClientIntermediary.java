/*
 * JPPF.
 * Copyright (C) 2005-2015 JPPF Team.
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
 * @(#)file      ClientIntermediary.java
 * @(#)author    Sun Microsystems, Inc.
 * @(#)version   1.45
 * @(#)lastedit  07/03/08
 * @(#)build     @BUILD_TAG_PLACEHOLDER@
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

package javax.management.remote.generic;

import java.io.IOException;
import java.util.*;

import javax.management.*;
import javax.management.remote.message.MBeanServerRequestMessage;
import javax.security.auth.Subject;

import com.sun.jmx.remote.generic.*;
import com.sun.jmx.remote.opt.util.*;

/**
 *
 */
class ClientIntermediary extends AbstractClientIntermediary {
  /** */
  private static final ClassLogger logger = new ClassLogger("javax.management.remote.generic", "ClientIntermediary");

  /**
   * @param connection .
   * @param wrap .
   * @param client .
   * @param env .
   */
  public ClientIntermediary(final ClientSynchroMessageConnection connection, final ObjectWrapping wrap, final GenericConnector client, final Map<String, ?> env) {
    logger.trace("constructor", "Create a ClientIntermediary object.");
    if (connection == null) throw new NullPointerException("Null connection.");
    this.connection = connection;
    if (wrap == null) {
      logger.trace("constructor", "Use a default ObjectWrapping implementation.");
      this.serialization = new ObjectWrappingImpl();
    } else this.serialization = wrap;
    myloader = EnvHelp.resolveClientClassLoader(env);
    this.client = client;
    communicatorAdmin = new GenericClientCommunicatorAdmin(this, EnvHelp.getConnectionCheckPeriod(env));
    notifForwarder = new GenericClientNotifForwarder(this, env);
    requestTimeoutReconn = DefaultConfig.getTimeoutReconnection(env);
  }

  //-------------------------------------------------------------
  // Implementation of MBeanServerConnection + Delegation Subject
  //-------------------------------------------------------------
  /**
   * @param className .
   * @param name .
   * @param delegationSubject .
   * @return .
   * @throws ReflectionException .
   * @throws InstanceAlreadyExistsException .
   * @throws MBeanRegistrationException .
   * @throws MBeanException .
   * @throws NotCompliantMBeanException .
   * @throws IOException .
   */
  public ObjectInstance createMBean(final String className, final ObjectName name, final Subject delegationSubject) throws ReflectionException, InstanceAlreadyExistsException, MBeanRegistrationException,
    MBeanException, NotCompliantMBeanException, IOException {
    logger.trace("createMBean", "called");
    try {
      return (ObjectInstance) mBeanServerRequest(MBeanServerRequestMessage.CREATE_MBEAN, new Object[] { className, name }, delegationSubject);
    } catch (ReflectionException|InstanceAlreadyExistsException|MBeanException|NotCompliantMBeanException e) {
      throw e;
    } catch (Exception e) {
      throw appropriateException(e);
    }
  }

  /**
   * @param className .
   * @param name .
   * @param params .
   * @param signature .
   * @param delegationSubject .
   * @return .
   * @throws ReflectionException .
   * @throws InstanceAlreadyExistsException .
   * @throws MBeanRegistrationException .
   * @throws MBeanException .
   * @throws NotCompliantMBeanException .
   * @throws IOException .
   */
  public ObjectInstance createMBean(final String className, final ObjectName name, final Object params[], final String signature[], final Subject delegationSubject) throws ReflectionException, InstanceAlreadyExistsException,
    MBeanRegistrationException, MBeanException, NotCompliantMBeanException, IOException {
    logger.trace("createMBean", "called");
    try {
      return (ObjectInstance) mBeanServerRequest(MBeanServerRequestMessage.CREATE_MBEAN_PARAMS, new Object[] { className, name, serialization.wrap(params), signature }, delegationSubject);
    } catch (ReflectionException|InstanceAlreadyExistsException|MBeanException|NotCompliantMBeanException e) {
      throw e;
    } catch (Exception e) {
      throw appropriateException(e);
    }
  }

  /**
   *
   * @param className .
   * @param name .
   * @param loaderName .
   * @param delegationSubject .
   * @return .
   * @throws ReflectionException .
   * @throws InstanceAlreadyExistsException .
   * @throws MBeanRegistrationException .
   * @throws MBeanException .
   * @throws NotCompliantMBeanException .
   * @throws InstanceNotFoundException .
   * @throws IOException .
   */
  public ObjectInstance createMBean(final String className, final ObjectName name, final ObjectName loaderName, final Subject delegationSubject) throws ReflectionException, InstanceAlreadyExistsException,
    MBeanRegistrationException, MBeanException, NotCompliantMBeanException, InstanceNotFoundException, IOException {
    logger.trace("createMBean", "called");
    try {
      return (ObjectInstance) mBeanServerRequest(MBeanServerRequestMessage.CREATE_MBEAN_LOADER, new Object[] { className, name, loaderName }, delegationSubject);
    } catch (ReflectionException|InstanceAlreadyExistsException|MBeanException|NotCompliantMBeanException|InstanceNotFoundException e) {
      throw e;
    } catch (Exception e) {
      throw appropriateException(e);
    }
  }

  /**
   * @param className .
   * @param name .
   * @param loaderName .
   * @param params .
   * @param signature .
   * @param delegationSubject .
   * @return .
   * @throws ReflectionException .
   * @throws InstanceAlreadyExistsException .
   * @throws MBeanRegistrationException .
   * @throws MBeanException .
   * @throws NotCompliantMBeanException .
   * @throws InstanceNotFoundException .
   * @throws IOException .
   */
  public ObjectInstance createMBean(final String className, final ObjectName name, final ObjectName loaderName, final Object params[], final String signature[], final Subject delegationSubject) throws ReflectionException,
  InstanceAlreadyExistsException, MBeanRegistrationException, MBeanException, NotCompliantMBeanException, InstanceNotFoundException, IOException {
    logger.trace("createMBean", "called");
    try {
      return (ObjectInstance) mBeanServerRequest(MBeanServerRequestMessage.CREATE_MBEAN_LOADER_PARAMS, new Object[] { className, name, loaderName, serialization.wrap(params), signature }, delegationSubject);
    } catch (ReflectionException|InstanceAlreadyExistsException|MBeanException|NotCompliantMBeanException|InstanceNotFoundException e) {
      throw e;
    } catch (Exception e) {
      throw appropriateException(e);
    }
  }

  /**
   * @param name .
   * @param delegationSubject .
   * @throws InstanceNotFoundException .
   * @throws MBeanRegistrationException .
   * @throws IOException .
   */
  public void unregisterMBean(final ObjectName name, final Subject delegationSubject) throws InstanceNotFoundException, MBeanRegistrationException, IOException {
    logger.trace("unregisterMBean", "called");
    try {
      mBeanServerRequest(MBeanServerRequestMessage.UNREGISTER_MBEAN, new Object[] { name }, delegationSubject);
    } catch (InstanceNotFoundException|MBeanRegistrationException e) {
      throw e;
    } catch (Exception e) {
      throw appropriateException(e);
    }
  }

  /**
   * @param name .
   * @param delegationSubject .
   * @return .
   * @throws InstanceNotFoundException .
   * @throws IOException .
   */
  public ObjectInstance getObjectInstance(final ObjectName name, final Subject delegationSubject) throws InstanceNotFoundException, IOException {
    logger.trace("getObjectInstance", "called");
    try {
      return (ObjectInstance) mBeanServerRequest(MBeanServerRequestMessage.GET_OBJECT_INSTANCE, new Object[] { name }, delegationSubject);
    } catch (InstanceNotFoundException e) {
      throw e;
    } catch (Exception e) {
      throw appropriateException(e);
    }
  }

  /**
   * @param name .
   * @param query .
   * @param delegationSubject .
   * @return .
   * @throws IOException .
   */
  @SuppressWarnings("all")
  public Set<ObjectInstance> queryMBeans(final ObjectName name, final QueryExp query, final Subject delegationSubject) throws IOException {
    logger.trace("queryMBeans", "called");
    try {
      return (Set<ObjectInstance>) mBeanServerRequest(MBeanServerRequestMessage.QUERY_MBEANS, new Object[] { name, serialization.wrap(query) }, delegationSubject);
    } catch (Exception e) {
      throw appropriateException(e);
    }
  }

  /**
   * @param name .
   * @param query .
   * @param delegationSubject .
   * @return .
   * @throws IOException .
   */
  @SuppressWarnings("all")
  public Set<ObjectName> queryNames(final ObjectName name, final QueryExp query, final Subject delegationSubject) throws IOException {
    logger.trace("queryNames", "called");
    try {
      return (Set<ObjectName>) mBeanServerRequest(MBeanServerRequestMessage.QUERY_NAMES, new Object[] { name, serialization.wrap(query) }, delegationSubject);
    } catch (Exception e) {
      throw appropriateException(e);
    }
  }

  /**
   * @param name .
   * @param delegationSubject .
   * @return .
   * @throws IOException .
   */
  public boolean isRegistered(final ObjectName name, final Subject delegationSubject) throws IOException {
    logger.trace("isRegistered", "called");
    try {
      return (Boolean) mBeanServerRequest(MBeanServerRequestMessage.IS_REGISTERED, new Object[] { name }, delegationSubject);
    } catch (Exception e) {
      throw appropriateException(e);
    }
  }

  /**
   * @param delegationSubject .
   * @return .
   * @throws IOException .
   */
  public Integer getMBeanCount(final Subject delegationSubject) throws IOException {
    logger.trace("getMBeanCount", "called");
    try {
      return (Integer) mBeanServerRequest(MBeanServerRequestMessage.GET_MBEAN_COUNT, null, delegationSubject);
    } catch (Exception e) {
      throw appropriateException(e);
    }
  }

  /**
   * @param name .
   * @param attribute .
   * @param delegationSubject .
   * @return .
   * @throws MBeanException .
   * @throws AttributeNotFoundException .
   * @throws InstanceNotFoundException .
   * @throws ReflectionException .
   * @throws IOException .
   */
  public Object getAttribute(final ObjectName name, final String attribute, final Subject delegationSubject)
      throws MBeanException, AttributeNotFoundException, InstanceNotFoundException, ReflectionException, IOException {
    logger.trace("getAttribute", "called");
    try {
      return mBeanServerRequest(MBeanServerRequestMessage.GET_ATTRIBUTE, new Object[] { name, attribute }, delegationSubject);
    } catch (MBeanException|AttributeNotFoundException|InstanceNotFoundException|ReflectionException e) {
      throw e;
    } catch (Exception e) {
      throw appropriateException(e);
    }
  }

  /**
   * @param name .
   * @param attributes .
   * @param delegationSubject .
   * @return .
   * @throws InstanceNotFoundException .
   * @throws ReflectionException .
   * @throws IOException .
   */
  public AttributeList getAttributes(final ObjectName name, final String[] attributes, final Subject delegationSubject) throws InstanceNotFoundException, ReflectionException, IOException {
    logger.trace("getAttributes", "called");
    try {
      return (AttributeList) mBeanServerRequest(MBeanServerRequestMessage.GET_ATTRIBUTES, new Object[] { name, attributes }, delegationSubject);
    } catch (InstanceNotFoundException|ReflectionException e) {
      throw e;
    } catch (Exception e) {
      throw appropriateException(e);
    }
  }

  /**
   * @param name .
   * @param attribute .
   * @param delegationSubject .
   * @throws InstanceNotFoundException .
   * @throws AttributeNotFoundException .
   * @throws InvalidAttributeValueException .
   * @throws MBeanException .
   * @throws ReflectionException .
   * @throws IOException .
   */
  public void setAttribute(final ObjectName name, final Attribute attribute, final Subject delegationSubject)
      throws InstanceNotFoundException, AttributeNotFoundException, InvalidAttributeValueException, MBeanException, ReflectionException, IOException {
    logger.trace("setAttribute", "called");
    try {
      mBeanServerRequest(MBeanServerRequestMessage.SET_ATTRIBUTE, new Object[] { name, serialization.wrap(attribute) }, delegationSubject);
    } catch (InstanceNotFoundException|AttributeNotFoundException|InvalidAttributeValueException|MBeanException|ReflectionException e) {
      throw e;
    } catch (Exception e) {
      throw appropriateException(e);
    }
  }

  /**
   * @param name .
   * @param attributes .
   * @param delegationSubject .
   * @return .
   * @throws InstanceNotFoundException .
   * @throws ReflectionException .
   * @throws IOException .
   */
  public AttributeList setAttributes(final ObjectName name, final AttributeList attributes, final Subject delegationSubject) throws InstanceNotFoundException, ReflectionException, IOException {
    logger.trace("setAttributes", "called");
    try {
      return (AttributeList) mBeanServerRequest(MBeanServerRequestMessage.SET_ATTRIBUTES, new Object[] { name, serialization.wrap(attributes) }, delegationSubject);
    } catch (InstanceNotFoundException|ReflectionException e) {
      throw e;
    } catch (Exception e) {
      throw appropriateException(e);
    }
  }

  /**
   * @param name .
   * @param operationName .
   * @param params .
   * @param signature .
   * @param delegationSubject .
   * @return .
   * @throws InstanceNotFoundException .
   * @throws MBeanException .
   * @throws ReflectionException .
   * @throws IOException .
   */
  public Object invoke(final ObjectName name, final String operationName, final Object params[], final String signature[], final Subject delegationSubject)
      throws InstanceNotFoundException, MBeanException, ReflectionException, IOException {
    logger.trace("invoke", "called");
    try {
      return mBeanServerRequest(MBeanServerRequestMessage.INVOKE, new Object[] { name, operationName, serialization.wrap(params), signature }, delegationSubject);
    } catch (InstanceNotFoundException|MBeanException|ReflectionException e) {
      throw e;
    } catch (Exception e) {
      throw appropriateException(e);
    }
  }

  /**
   * @param delegationSubject .
   * @return .
   * @throws IOException .
   */
  public String getDefaultDomain(final Subject delegationSubject) throws IOException {
    logger.trace("getDefaultDomain", "called");
    try {
      return (String) mBeanServerRequest(MBeanServerRequestMessage.GET_DEFAULT_DOMAIN, null, delegationSubject);
    } catch (Exception e) {
      throw appropriateException(e);
    }
  }

  /**
   * @param delegationSubject .
   * @return .
   * @throws IOException .
   */
  public String[] getDomains(final Subject delegationSubject) throws IOException {
    logger.trace("getDomains", "called");
    try {
      return (String[]) mBeanServerRequest(MBeanServerRequestMessage.GET_DOMAINS, null, delegationSubject);
    } catch (Exception e) {
      throw appropriateException(e);
    }
  }

  /**
   * @param name .
   * @param delegationSubject .
   * @return .
   * @throws InstanceNotFoundException .
   * @throws IntrospectionException .
   * @throws ReflectionException .
   * @throws IOException .
   */
  public MBeanInfo getMBeanInfo(final ObjectName name, final Subject delegationSubject) throws InstanceNotFoundException, IntrospectionException, ReflectionException, IOException {
    logger.trace("getMBeanInfo", "called");
    try {
      return (MBeanInfo) mBeanServerRequest(MBeanServerRequestMessage.GET_MBEAN_INFO, new Object[] { name }, delegationSubject);
    } catch (InstanceNotFoundException|IntrospectionException|ReflectionException e) {
      throw e;
    } catch (Exception e) {
      throw appropriateException(e);
    }
  }

  /**
   * @param name .
   * @param className .
   * @param delegationSubject .
   * @return .
   * @throws InstanceNotFoundException .
   * @throws IOException .
   */
  public boolean isInstanceOf(final ObjectName name, final String className, final Subject delegationSubject) throws InstanceNotFoundException, IOException {
    logger.trace("isInstanceOf", "called");
    try {
      return (Boolean) mBeanServerRequest(MBeanServerRequestMessage.IS_INSTANCE_OF, new Object[] { name, className }, delegationSubject);
    } catch (InstanceNotFoundException e) {
      throw e;
    } catch (Exception e) {
      throw appropriateException(e);
    }
  }
}
