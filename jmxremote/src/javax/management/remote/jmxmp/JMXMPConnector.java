/*
 * JPPF.
 * Copyright (C) 2005-2016 JPPF Team.
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
 * @(#)file      JMXMPConnector.java
 * @(#)author    Sun Microsystems, Inc.
 * @(#)version   1.28
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

import java.io.*;
import java.net.MalformedURLException;
import java.util.*;

import javax.management.remote.*;
import javax.management.remote.generic.*;

import com.sun.jmx.remote.opt.util.EnvHelp;
import com.sun.jmx.remote.socket.SocketConnection;

/**
 * <p> The client end of a JMXMP Connector. An object of this type can be used to establish a connection to a connector server.
 * Rather than instantiate this class directly, it is recommended to use the {@link JMXConnectorFactory} with a {@link JMXServiceURL} that has protocol type <code>jmxmp</code>.
 */
public class JMXMPConnector extends GenericConnector implements Serializable {
  /**
   * Explicit serialVersionUID.
   */
  private static final long serialVersionUID = 7098019344073706637L;
  /**
   * @serial The JMX Service URL of the peer JMXMP Connector Server.
   * @see #JMXMPConnector(JMXServiceURL)
   * @see #JMXMPConnector(JMXServiceURL, Map)
   */
  private JMXServiceURL address;
  /**
   *
   */
  private transient Map<String, ?> env;
  /**
   *
   */
  private static final String protocolName = "jmxmp";

  /**
   * <p>Constructs a JMXMP Connector client that can make a connection to the connector server at the given address.
   * This constructor is equivalent to {@link #JMXMPConnector(JMXServiceURL, Map) JMXMPConnector(address, null)}.
   * @param address the address of the connector server to connect to.
   * @exception IllegalArgumentException if <code>address</code> is null.
   * @exception IOException if <code>address</code> is not a valid URL for the JMXMP connector or the connector cannot work for another reason.
   */
  public JMXMPConnector(final JMXServiceURL address) throws IOException {
    this(address, null);
  }

  /**
   * <p> Constructs a JMXMP Connector client that can make a connection to the connector server at the given address.
   * @param address the address of the connector server to connect to.
   * @param env the environment parameters controlling the connection. This parameter can be null, which is equivalent to an empty map. The provided Map will not be modified.
   * @exception IllegalArgumentException if <code>address</code> is null.
   * @exception IOException if <code>address</code> is not a valid URL for the JMXMP connector or the connector cannot work for another reason.
   */
  public JMXMPConnector(final JMXServiceURL address, final Map<String, ?> env) throws IOException {
    super(env);
    if (env == null) this.env = Collections.emptyMap();
    else this.env = new HashMap<>(env);
    this.address = address;
    validateAddress();
  }

  /**
   * Validate the service URL for this connector.
   * @throws MalformedURLException if the service URL is not a valid JMXMP URL.
   */
  private void validateAddress() throws MalformedURLException {
    if (address == null) throw new IllegalArgumentException("JMXServiceURL must not be null");
    if (!protocolName.equalsIgnoreCase(address.getProtocol())) throw new MalformedURLException("Unknown protocol: " + address.getProtocol());
  }

  @Override
  public void connect(final Map<String, ?> env) throws IOException {
    // Call validateAddress again in case we've deserialized a bogus address (so we didn't call the constructor).
    validateAddress();
    // We have to be careful not to overwrite params in the constructor env with default params.
    Map<String, Object> newEnv = new HashMap<>();
    if (this.env != null) newEnv.putAll(this.env);
    if (env != null) {
      EnvHelp.checkAttributes(env);
      newEnv.putAll(env);
    }
    final ClassLoader defaultClassLoader = EnvHelp.resolveClientClassLoader(newEnv);
    newEnv.put(JMXConnectorFactory.DEFAULT_CLASS_LOADER, defaultClassLoader);
    if (!newEnv.containsKey(MESSAGE_CONNECTION)) {
      MessageConnection conn = new SocketConnection(address.getHost(), address.getPort());
      newEnv.put(MESSAGE_CONNECTION, conn);
    }
    super.connect(newEnv);
  }

  /**
   * Returns a string representation of this object. In general, the <code>toString</code> method returns a string that "textually represents" this object.
   * The result should be a concise but informative representation that is easy for a person to read.
   * @return a String representation of this object.
   **/
  @Override
  public String toString() {
    return this.getClass().getName() + ": JMXServiceURL=" + address;
  }
}
