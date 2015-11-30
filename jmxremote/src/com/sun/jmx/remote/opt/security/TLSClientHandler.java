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
 * @(#)file      TLSClientHandler.java
 * @(#)author    Sun Microsystems, Inc.
 * @(#)version   1.21
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

package com.sun.jmx.remote.opt.security;

import java.io.IOException;
import java.net.Socket;
import java.util.*;

import javax.management.remote.generic.MessageConnection;
import javax.management.remote.message.*;
import javax.net.ssl.*;

import com.sun.jmx.remote.generic.ProfileClient;
import com.sun.jmx.remote.opt.util.ClassLogger;
import com.sun.jmx.remote.socket.SocketConnectionIf;

/**
 * This class implements the client side TLS profile.
 */
public class TLSClientHandler implements ProfileClient {
  /**
   * 
   */
  protected SSLSocket ts = null;
  /**
   * 
   */
  private boolean completed = false;
  /**
   * 
   */
  private Map<String, Object> env = null;
  /**
   * 
   */
  private MessageConnection mc = null;
  /**
   * 
   */
  private String profile = null;
  /**
   * 
   */
  private static final ClassLogger logger = new ClassLogger("javax.management.remote.misc", "TLSClientHandler");

  /**
   * 
   * @param profile .
   * @param env .
   */
  public TLSClientHandler(final String profile, final Map<String, Object> env) {
    this.profile = profile;
    this.env = env;
  }

  //---------------------------------------
  // ProfileClient interface implementation
  //---------------------------------------

  @Override
  public void initialize(final MessageConnection mc) throws IOException {
    this.mc = mc;
    // Check if instance of SocketConnectionIf and retrieve underlying socket
    Socket socket = null;
    if (mc instanceof SocketConnectionIf) socket = ((SocketConnectionIf) mc).getSocket();
    else throw new IOException("Not an instance of SocketConnectionIf");
    // Get SSLSocketFactory
    SSLSocketFactory ssf = (SSLSocketFactory) env.get("jmx.remote.tls.socket.factory");
    if (ssf == null) ssf = (SSLSocketFactory) SSLSocketFactory.getDefault();
    String hostname = socket.getInetAddress().getHostName();
    int port = socket.getPort();
    if (logger.traceOn()) {
      logger.trace("initialize", "TLS: Hostname = " + hostname);
      logger.trace("initialize", "TLS: Port = " + port);
    }
    ts = (SSLSocket) ssf.createSocket(socket, hostname, port, true);
    // Set the SSLSocket Client Mode
    ts.setUseClientMode(true);
    if (logger.traceOn()) logger.trace("initialize", "TLS: Socket Client Mode = " + ts.getUseClientMode());
    // Set the SSLSocket Enabled Protocols
    if (TLSServerHandler.bundledJSSE) {
      String enabledProtocols = (String) env.get("jmx.remote.tls.enabled.protocols");
      if (enabledProtocols != null) {
        StringTokenizer st = new StringTokenizer(enabledProtocols, " ");
        int tokens = st.countTokens();
        String enabledProtocolsList[] = new String[tokens];
        for (int i = 0; i < tokens; i++) enabledProtocolsList[i] = st.nextToken();
        TLSServerHandler.setEnabledProtocols(ts, enabledProtocolsList);
      }
      if (logger.traceOn()) {
        logger.trace("initialize", "TLS: Enabled Protocols");
        String[] enabled_p = TLSServerHandler.getEnabledProtocols(ts);
        if (enabled_p != null) {
          StringBuffer str_buffer = new StringBuffer();
          for (int i = 0; i < enabled_p.length; i++) {
            str_buffer.append(enabled_p[i]);
            if (i + 1 < enabled_p.length) str_buffer.append(", ");
          }
          logger.trace("initialize", "TLS: [" + str_buffer + "]");
        } else logger.trace("initialize", "TLS: []");
      }
    }

    // Set the SSLSocket Enabled Cipher Suites
    String enabledCipherSuites = (String) env.get("jmx.remote.tls.enabled.cipher.suites");
    if (enabledCipherSuites != null) {
      StringTokenizer st = new StringTokenizer(enabledCipherSuites, " ");
      int tokens = st.countTokens();
      String enabledCipherSuitesList[] = new String[tokens];
      for (int i = 0; i < tokens; i++) enabledCipherSuitesList[i] = st.nextToken();
      ts.setEnabledCipherSuites(enabledCipherSuitesList);
    }
    if (logger.traceOn()) {
      logger.trace("initialize", "TLS: Enabled Cipher Suites");
      String[] enabled_cs = ts.getEnabledCipherSuites();
      if (enabled_cs != null) {
        StringBuffer str_buffer = new StringBuffer();
        for (int i = 0; i < enabled_cs.length; i++) {
          str_buffer.append(enabled_cs[i]);
          if (i + 1 < enabled_cs.length) str_buffer.append(", ");
        }
        logger.trace("initialize", "TLS: [" + str_buffer + "]");
      } else logger.trace("initialize", "TLS: []");
    }
  }

  @Override
  public ProfileMessage produceMessage() throws IOException {
    TLSMessage tlspm = new TLSMessage(TLSMessage.READY);
    if (logger.traceOn()) {
      logger.trace("produceMessage", ">>>>> TLS client message <<<<<");
      logger.trace("produceMessage", "Profile Name : " + tlspm.getProfileName());
      logger.trace("produceMessage", "Status : " + tlspm.getStatus());
    }
    return tlspm;
  }

  @Override
  public void consumeMessage(final ProfileMessage pm) throws IOException {
    if (!(pm instanceof TLSMessage)) {
      throw new IOException("Unexpected profile message type: " + pm.getClass().getName());
    }
    TLSMessage tlspm = (TLSMessage) pm;
    if (logger.traceOn()) {
      logger.trace("consumeMessage", ">>>>> TLS server message <<<<<");
      logger.trace("consumeMessage", "Profile Name : " + tlspm.getProfileName());
      logger.trace("consumeMessage", "Status : " + tlspm.getStatus());
    }
    if (tlspm.getStatus() != TLSMessage.PROCEED) {
      throw new IOException("Unexpected TLS status [" + tlspm.getStatus() + "]");
    }
    completed = true;
  }

  @Override
  public boolean isComplete() {
    return completed;
  }

  @Override
  public void activate() throws IOException {
    if (logger.traceOn()) {
      logger.trace("activate", ">>>>> TLS handshake <<<<<");
      logger.trace("activate", "TLS: Start TLS Handshake");
    }
    ts.startHandshake();
    if (logger.traceOn()) {
      SSLSession session = ts.getSession();
      if (session != null) {
        logger.trace("activate", "TLS: getCipherSuite = " + session.getCipherSuite());
        logger.trace("activate", "TLS: getPeerHost = " + session.getPeerHost());
        if (TLSServerHandler.bundledJSSE) logger.trace("activate", "TLS: getProtocol = " + TLSServerHandler.getProtocol(session));
      }
      logger.trace("activate", "TLS: Finish TLS Handshake");
    }

    // Set new TLS socket in MessageConnection
    ((SocketConnectionIf) mc).setSocket(ts);
  }

  @Override
  public void terminate() throws IOException {
  }

  @Override
  public String getName() {
    return profile;
  }
}
