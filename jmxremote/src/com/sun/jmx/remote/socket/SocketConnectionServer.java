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
 * @(#)file      SocketConnectionServer.java
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

package com.sun.jmx.remote.socket;

import java.io.IOException;
import java.lang.reflect.*;
import java.net.*;
import java.util.*;

import javax.management.remote.JMXServiceURL;
import javax.management.remote.generic.*;
import javax.management.remote.jmxmp.JMXMPConnectorServer;

import com.sun.jmx.remote.generic.DefaultConfig;
import com.sun.jmx.remote.opt.util.*;

/**
 * This class uses a Tcp Server Socket to realize a JMX connection server
 */
public class SocketConnectionServer implements MessageConnectionServer {
  /**
   * 
   */
  private ServerSocket ss;
  /**
   * 
   */
  private JMXServiceURL addr;
  /**
   * 
   */
  private boolean wildcard;
  /**
   * 
   */
  private Map<String, ?> env;
  /**
   * 
   */
  private static final String DEFAULT_PROTOCOL = "jmxmp";
  /**
   * 
   */
  private static final int DEFAULT_BACKLOG = 100;
  /**
   * 
   */
  private final ClassLogger logger = new ClassLogger("javax.management.remote.misc", "SocketConnectionServer");

  /**
   * Initialize this connection serevr.
   * @param addr the JMX service URL of the remote connector.
   * @param env the environment of the remote connector.
   * @throws IOException if any I/O error occurs.
   */
  public SocketConnectionServer(final JMXServiceURL addr, final Map<String, ?> env) throws IOException {
    if (logger.traceOn()) logger.trace("constructor", "Constructs a SocketConnectionServer on " + addr);
    if (addr == null) throw new NullPointerException("Null address.");
    if (!DEFAULT_PROTOCOL.equalsIgnoreCase(addr.getProtocol())) throw new MalformedURLException("Unknown protocol: " + addr.getProtocol());
    String wildcardS = null;
    if (env != null) wildcardS = (String) env.get(JMXMPConnectorServer.SERVER_ADDRESS_WILDCARD);
    wildcard = (wildcardS == null) ? true : wildcardS.equalsIgnoreCase("true");
    this.addr = addr;
  }

  // implements MessageConnectionServer interface
  @Override
  public void start(final Map<String, ?> env) throws IOException {
    if (logger.traceOn()) logger.trace("start", "Starts the server now.");
    Map<String, Object> newEnv = new HashMap<>();
    if (this.env != null) newEnv.putAll(this.env);
    if (env != null) newEnv.putAll(env);
    final int port = addr.getPort();
    String host = addr.getHost();
    if (host.equals("")) host = InetAddress.getLocalHost().getHostName();
    // In the wildcard case, the following socket creation just serves to check that the address in the URL is a valid local address.
    if (wildcard) {
      ss = new ServerSocket(0, DEFAULT_BACKLOG, InetAddress.getByName(host));
      ss.close();
    }
    Object o = null;
    try {
      Class<?> c = Class.forName("java.net.InetSocketAddress");
      if (wildcard) {
        Constructor<?> ct = c.getDeclaredConstructor(new Class[] { int.class });
        o = ct.newInstance(new Object[] { new Integer(port) });
      } else {
        Constructor<?> ct = c.getDeclaredConstructor(new Class[] { String.class, int.class });
        o = ct.newInstance(new Object[] { host, new Integer(port) });
      }
    } catch (@SuppressWarnings("unused") Exception ee) {
      // OK. we are using JDK1.3 or earlier
    }
    if (o != null && DefaultConfig.getServerReuseAddress(newEnv)) {
      try {
        Class<?> cc = ServerSocket.class;
        Method m1 = cc.getMethod("setReuseAddress", new Class[] { boolean.class });
        Method m2 = cc.getMethod("bind", new Class[] { Class.forName("java.net.SocketAddress"), int.class });
        ss = (ServerSocket) cc.newInstance();
        // setReusAddress
        m1.invoke(ss, new Object[] { Boolean.TRUE });
        // bind
        m2.invoke(ss, new Object[] { o, new Integer(DEFAULT_BACKLOG) });
      } catch (RuntimeException re) {
        throw re;
      } catch (Exception e) {
        if (e instanceof InvocationTargetException) {
          Throwable t = ((InvocationTargetException) e).getTargetException();
          if (t instanceof IOException) throw (IOException) t;
          else if (t instanceof RuntimeException) throw (RuntimeException) t;
          else if (t instanceof Exception) e = (Exception) t;
        }
        // possible: ClassNotFoundException, NoSuchMethodException, IllegalAccessException, InstantiationException, InvocationTargetException (getCause() == null or == Error)
        IOException ioe = new IOException(e.toString());
        EnvHelp.initCause(ioe, e);
        throw ioe;
      }
    } else { // 1.3 or earlier or not reuse address
      if (wildcard) ss = new ServerSocket(port, DEFAULT_BACKLOG);
      else ss = new ServerSocket(port, DEFAULT_BACKLOG, InetAddress.getByName(host));
    }
    addr = new JMXServiceURL(DEFAULT_PROTOCOL, host, ss.getLocalPort());
    this.env = newEnv;
  }

  @Override
  public MessageConnection accept() throws IOException {
    if (logger.traceOn()) {
      logger.trace("accept", "Waiting a new connection...");
    }
    Socket sock = ss.accept();
    if (!InterceptorHandlerProxy.invokeOnAccept(sock)) throw new IOException("Connection denied by interceptor: " + sock);
    MessageConnection mc = new SocketConnection(sock);
    return mc;
  }

  @Override
  public void stop() throws IOException {
    if (logger.traceOn()) logger.trace("stop", "Stops the server now.");
    if (ss != null) ss.close();
  }

  @Override
  public JMXServiceURL getAddress() {
    return addr;
  }
}
