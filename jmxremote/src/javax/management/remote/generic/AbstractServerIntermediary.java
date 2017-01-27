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
/*
 * @(#)file      ServerIntermediary.java
 * @(#)author    Sun Microsystems, Inc.
 * @(#)version   1.79
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
import java.security.*;
import java.util.Map;

import javax.management.MBeanServer;
import javax.management.remote.generic.ServerIntermediary.GenericServerCommunicatorAdmin;
import javax.security.auth.Subject;

import com.sun.jmx.remote.generic.ServerSynchroMessageConnection;
import com.sun.jmx.remote.opt.internal.ServerNotifForwarder;
import com.sun.jmx.remote.opt.security.*;
import com.sun.jmx.remote.opt.util.*;

/**
 * @exclude
 */
abstract class AbstractServerIntermediary {
  /** */
  final MBeanServer mbeanServer;
  /** */
  final GenericConnectorServer myServer;
  /** */
  final ServerSynchroMessageConnection connection;
  /** */
  final String clientId;
  /** */
  GenericServerCommunicatorAdmin serverCommunicatorAdmin;
  /** */
  final ObjectWrapping serialization;
  /** */
  final AccessControlContext acc;
  /** */
  final Subject subject;
  /** */
  final SubjectDelegator subjectDelegator;
  /** */
  final ClassLoader defaultClassLoader;
  /** */
  ServerNotifForwarder serverNotifForwarder;
  /** */
  Map<String, ?> env;
  /** */
  static final ClassLogger logger = new ClassLogger("javax.management.remote.generic", "ServerIntermediary");
  /** */
  static final int RUNNING = 0;
  /** */
  static final int FAILED = 1;
  /** */
  static final int TERMINATED = 2;
  /** */
  int state = RUNNING;
  /** */
  final int[] stateLock = new int[0];
  /**
   * compatible to RI 10: bug 4948444
   */
  final boolean isRI10;
  /** */
  static final Long ONE_LONG = new Long(1);

  /**
   * Note: it is necessary to pass the defaultClassLoader - because the defaultClassLoader is determined from the Map + context
   * class loader of the thread that calls GenericConnectorServer.start()
   * @param mbeanServer .
   * @param myServer .
   * @param connection .
   * @param wrapper .
   * @param subject .
   * @param defaultClassLoader .
   * @param env .
   */
  public AbstractServerIntermediary(final MBeanServer mbeanServer, final GenericConnectorServer myServer, final ServerSynchroMessageConnection connection, final ObjectWrapping wrapper, final Subject subject,
      final ClassLoader defaultClassLoader, final Map<String, ?> env) {
    if (logger.traceOn()) logger.trace("constructor", "Create a ServerIntermediary object.");
    if (mbeanServer == null) throw new NullPointerException("Null mbean server.");
    if (connection == null) throw new NullPointerException("Null connection.");
    this.mbeanServer = mbeanServer;
    this.myServer = myServer;
    this.connection = connection;
    this.clientId = connection.getConnectionId();
    this.serialization = wrapper;
    this.subjectDelegator = new SubjectDelegator();
    this.subject = subject;
    if (subject == null)this.acc = null;
    else this.acc = new AccessControlContext(AccessController.getContext(), new JMXSubjectDomainCombiner(subject));
    this.defaultClassLoader = defaultClassLoader;
    this.env = env;
    // compatible to RI 1.0: bug 4948444
    String s = (String) this.env.get("com.sun.jmx.remote.bug.compatible");
    if (s == null) {
      s = AccessController.doPrivileged(new PrivilegedAction<String>() {
        @Override public String run() {
          return System.getProperty("com.sun.jmx.remote.bug.compatible");
        }
      });
    }
    isRI10 = "RI1.0.0".equals(s);
    /*
    this.clr = AccessController.doPrivileged(new PrivilegedAction<ClassLoaderWithRepository>() {
      @Override
      public ClassLoaderWithRepository run() {
        return new ClassLoaderWithRepository(getClassLoaderRepository(), dcl);
      }
    });
    serverCommunicatorAdmin = new GenericServerCommunicatorAdmin(timeout);
    */
  }


  /**
   * Iterate until we extract the real exception from a stack of PrivilegedActionExceptions.
   * @param e .
   * @return .
   */
  Exception extractException(final Exception e) {
    Exception ex = e;
    while (ex instanceof PrivilegedActionException) ex = ((PrivilegedActionException) ex).getException();
    return ex;
  }

  /**
   * @param obj .
   * @param cl .
   * @return .
   * @throws IOException .
   * @throws ClassNotFoundException .
   */
  Object unwrapWithDefault(final Object obj, final ClassLoader cl) throws IOException, ClassNotFoundException {
    try {
      return AccessController.doPrivileged(new PrivilegedExceptionAction<Object>() {
        @Override public Object run() throws IOException, ClassNotFoundException {
          return serialization.unwrap(obj, new OrderClassLoaders(cl, defaultClassLoader));
        }
      });
    } catch (PrivilegedActionException pe) {
      Exception e = extractException(pe);
      if (e instanceof IOException) throw (IOException) e;
      if (e instanceof ClassNotFoundException) throw (ClassNotFoundException) e;
    }
    return null;
  }
}
