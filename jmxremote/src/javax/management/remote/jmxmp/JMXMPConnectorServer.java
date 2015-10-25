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
 * @(#)file      JMXMPConnectorServer.java
 * @(#)author    Sun Microsystems, Inc.
 * @(#)version   1.18
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

package javax.management.remote.jmxmp;

import java.io.IOException;
import java.util.*;

import javax.management.MBeanServer;
import javax.management.remote.*;
import javax.management.remote.generic.GenericConnectorServer;

import com.sun.jmx.remote.socket.SocketConnectionServer;

/**
 * <p> A JMX API connector server that creates connections using the JMX Messaging Protocol over TCP.
 * Rather than instantiate this class directly, it is recommended to use the {@link JMXConnectorServerFactory}
 * with a {@link JMXServiceURL} that has protocol type <code>jmxmp</code>.
 *
 * <p>The address that the connector server should listen for connections on is specified with a
 * {@link JMXServiceURL} provided to the constructor. If the host is omitted from this address, the local
 * host is understood. If the port is omitted from this address, the connector server will listen on an
 * unspecified available port. The {@link #getAddress() getAddress} method can be used to discover which port it is.
 *
 * <p> If the local machine has more than one network interface, it is possible to listen for connections on all of
 * the interfaces, or on just one interface. By default, a connector server listens on all of the interfaces.
 * To listen on just one, specify the address of the interface in the <code>JMXServiceURL</code> and supply the
 * attribute {@link #SERVER_ADDRESS_WILDCARD} with the value <code>"false"</code> when constructing the connector server.
 *
 * <p>In addition to any protocol-specific authentication, this connector server can verify new connections and/or modify
 * the authenticated {@link javax.security.auth.Subject} by supplying a {@link JMXAuthenticator} using the attribute {@link #AUTHENTICATOR}.
 * The {@link JMXAuthenticator#authenticate authenticate} method will be called with a two-element <code>Object[]</code>.
 * The first element is a String that is the connection id of the new connection. The second element is either null or the
 * authenticated {@link javax.security.auth.Subject}. If the connection is accepted, the method returns the <code>Subject</code> to use
 * for received requests, or null if there is none. If the connection is rejected, the method throws an exception, typically {@link SecurityException}.
 * </p>
 */
public class JMXMPConnectorServer extends GenericConnectorServer {
  /**
   * Name of the attribute that specifies whether the connector server should listen for connections on all connected network
   * interfaces, or just on the interface whose address is specified in the {@link JMXServiceURL}.
   * The value associated with this attribute, if any, should be a string that is equal, ignoring case, to <code>"true"</code> or <code>"false"</code>.
   * The default value is {@code true}.
   */
  public static final String SERVER_ADDRESS_WILDCARD = "jmx.remote.server.address.wildcard";

  /**
   * <p>Creates a connector server that listens for connection requests on an unspecified port.
   * A connector server created in this way must be registered as an MBean in the MBean server that is being
   * made available remotely.
   * <p>This constructor is equivalent to {@link #JMXMPConnectorServer(JMXServiceURL,Map,MBeanServer) JMXMPConnectorServer(null,null,null)}.
   * @exception IOException if the connector server cannot be created for some reason.
   */
  public JMXMPConnectorServer() throws IOException {
    this((MBeanServer) null);
  }

  /**
   * <p>Creates a connector server that listens for connection requests on an unspecified port.
   * The MBean server that is being made available remotely is specified by the <code>mbs</code> parameter.
   * <p>This constructor is equivalent to {@link #JMXMPConnectorServer(JMXServiceURL,Map,MBeanServer) JMXMPConnectorServer(null,null,mbs)}.
   * @param mbs the MBean server that is being made available remotely.
   * @exception IOException if the connector server cannot be created for some reason.
   */
  public JMXMPConnectorServer(final MBeanServer mbs) throws IOException {
    this(null, null, mbs);
  }

  /**
   * <p>Creates a connector server that listens for connection requests on the given address with the given parameters.
   * A connector server created in this way must be registered as an MBean in the MBean server that is being made available remotely.
   * <p>This constructor is equivalent to {@link #JMXMPConnectorServer(JMXServiceURL,Map,MBeanServer) JMXMPConnectorServer(address,env,null)}.
   * @param address the address that the connector server will listen for connections on. If null, the connector server will listen for connections on an unspecified port of the local host name.
   * @param env the properties of the connector server. This parameter can be null, which is equivalent to an empty Map. The supplied Map is not modified.
   * @exception IllegalArgumentException if <var>env</var> contains some invalid values.
   * @exception IOException if the connector server cannot be created for some reason.
   */
  public JMXMPConnectorServer(final JMXServiceURL address, final Map<String, ?> env) throws IOException {
    this(address, env, (MBeanServer) null);
  }

  /**
   * <p>Creates a connector server that listens for connection requests on the given address with the given parameters.
   * The MBean server that is being made available remotely is specified by the <code>mbs</code> parameter.
   * @param address the address that the connector server will listen for connections on. If null, the connector server will listen for connections on an unspecified port of the local host name.
   * @param env the properties of the connector server. This parameter can be null, which is equivalent to an empty Map. The supplied Map is not modified.
   * @param mbs the MBean server that this connector server is making available remotely. Null if this connector server will be registered as an MBean in the MBean server to be made available.
   * @exception IllegalArgumentException if <var>env</var> contains some invalid values.
   * @exception IOException if the connector server cannot be created for some reason.
   */
  public JMXMPConnectorServer(final JMXServiceURL address, final Map<String, ?> env, final MBeanServer mbs) throws IOException {
    super(completeEnv(address, env), mbs);
  }

  /**
   * Add missing properties to the environment map.
   * @param address the srvice URL for the connector server.
   * @param env the environment map to complete.
   * @return a new environment map with added properties.
   * @throws IOException if the environment cannot be completed for any reason.
   */
  private static Map<String, ?> completeEnv(final JMXServiceURL address, final Map<String, ?> env) throws IOException {
    if (env != null && env.containsKey(MESSAGE_CONNECTION_SERVER)) return env;
    JMXServiceURL addr = address;
    if (addr == null) addr = new JMXServiceURL("jmxmp", null, 0);
    Map<String, Object> newEnv = null;
    if (env == null) newEnv =  new HashMap<>();
    else newEnv = new HashMap<>(env);
    newEnv.put(MESSAGE_CONNECTION_SERVER, new SocketConnectionServer(addr, newEnv));
    return newEnv;
  }
}
