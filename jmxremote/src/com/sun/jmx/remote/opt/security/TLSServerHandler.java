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
 * @(#)file      TLSServerHandler.java
 * @(#)author    Sun Microsystems, Inc.
 * @(#)version   1.27
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
import java.lang.reflect.*;
import java.net.Socket;
import java.security.*;
import java.util.*;

import javax.management.remote.JMXPrincipal;
import javax.management.remote.generic.MessageConnection;
import javax.management.remote.message.*;
import javax.net.ssl.*;
import javax.security.auth.Subject;
import javax.security.cert.X509Certificate;

import com.sun.jmx.remote.generic.ProfileServer;
import com.sun.jmx.remote.opt.util.*;
import com.sun.jmx.remote.socket.SocketConnectionIf;

/**
 * This class implements the server side TLS profile.
 */
public class TLSServerHandler implements ProfileServer {
  /**
   * 
   */
  static final boolean bundledJSSE;
  /**
   * 
   */
  static Method getProtocol;
  /**
   * 
   */
  static Method getEnabledProtocols;
  /**
   * 
   */
  static Method setEnabledProtocols;
  /**
   * 
   */
  static Method getWantClientAuth;
  /**
   * 
   */
  static Method setWantClientAuth;
  static {
    /*
     * We attempt to work even if we are running in J2SE 1.3, where the unbundled JSSE libraries do not contain the following methods:
     * SSLSession.getProtocol() SSLSocket.getEnabledProtocols() SSLSocket.setEnabledProtocols(String[]) SSLSocket.getWantClientAuth() SSLSocket.setWantClientAuth(boolean)
     */
    boolean error = false;
    try {
      getProtocol = SSLSession.class.getMethod("getProtocol", new Class[0]);
      getEnabledProtocols = SSLSocket.class.getMethod("getEnabledProtocols", new Class[0]);
      setEnabledProtocols = SSLSocket.class.getMethod("setEnabledProtocols", new Class[] { String[].class });
      getWantClientAuth = SSLSocket.class.getMethod("getWantClientAuth", new Class[0]);
      setWantClientAuth = SSLSocket.class.getMethod("setWantClientAuth", new Class[] { Boolean.TYPE });
    } catch (Throwable t) {
      // Running with a J2SE prior to J2SE 1.4
      error = true;
    }
    bundledJSSE = !error;
  }
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
  private Map<String, ?> env = null;
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
  private Subject subject = null;
  /**
   * 
   */
  private static final String X500_PRINCIPAL = "javax.security.auth.x500.X500Principal";
  /**
   * 
   */
  private static final ClassLogger logger = new ClassLogger("javax.management.remote.misc", "TLSServerHandler");

  /**
   * 
   * @param s .
   * @return .
   * @throws IOException .
   */
  static String getProtocol(final SSLSession s) throws IOException {
    try {
      return (String) getProtocol.invoke(s, new Object[0]);
    } catch (InvocationTargetException e) {
      throw (RuntimeException) e.getTargetException();
    } catch (Throwable t) {
      // Should never happen as this code is only executed when
      // running with the bundled JSSE, i.e. J2SE 1.4 or later.
      throw (IOException) EnvHelp.initCause(new IOException(t.getMessage()), t);
    }
  }

  /**
   * 
   * @param s .
   * @return .
   * @throws IOException .
   */
  static String[] getEnabledProtocols(final SSLSocket s) throws IOException {
    try {
      return (String[]) getEnabledProtocols.invoke(s, new Object[0]);
    } catch (InvocationTargetException e) {
      throw (RuntimeException) e.getTargetException();
    } catch (Throwable t) {
      // Should never happen as this code is only executed when
      // running with the bundled JSSE, i.e. J2SE 1.4 or later.
      throw (IOException) EnvHelp.initCause(new IOException(t.getMessage()), t);
    }
  }

  /**
   * 
   * @param s .
   * @param p .
   * @throws IOException .
   */
  static void setEnabledProtocols(final SSLSocket s, final String[] p) throws IOException {
    try {
      setEnabledProtocols.invoke(s, new Object[] { p });
    } catch (InvocationTargetException e) {
      throw (RuntimeException) e.getTargetException();
    } catch (Throwable t) {
      // Should never happen as this code is only executed when
      // running with the bundled JSSE, i.e. J2SE 1.4 or later.
      throw (IOException) EnvHelp.initCause(new IOException(t.getMessage()), t);
    }
  }

  /**
   * 
   * @param s .
   * @return .
   * @throws IOException .
   */
  static Boolean getWantClientAuth(final SSLSocket s) throws IOException {
    try {
      return (Boolean) getWantClientAuth.invoke(s, new Object[0]);
    } catch (InvocationTargetException e) {
      throw (RuntimeException) e.getTargetException();
    } catch (Throwable t) {
      // Should never happen as this code is only executed when
      // running with the bundled JSSE, i.e. J2SE 1.4 or later.
      throw (IOException) EnvHelp.initCause(new IOException(t.getMessage()), t);
    }
  }

  /**
   * 
   * @param s . 
   * @param b .
   * @throws IOException .
   */
  static void setWantClientAuth(final SSLSocket s, final Boolean b) throws IOException {
    try {
      setWantClientAuth.invoke(s, new Object[] { b });
    } catch (InvocationTargetException e) {
      throw (RuntimeException) e.getTargetException();
    } catch (Throwable t) {
      // Should never happen as this code is only executed when
      // running with the bundled JSSE, i.e. J2SE 1.4 or later.
      throw (IOException) EnvHelp.initCause(new IOException(t.getMessage()), t);
    }
  }

  /**
   * 
   * @param profile .
   * @param env .
   */
  public TLSServerHandler(final String profile, final Map<String, ?> env) {
    this.profile = profile;
    this.env = env;
  }

  //---------------------------------------
  // ProfileServer interface implementation
  //---------------------------------------

  @Override
  public void initialize(final MessageConnection mc, final Subject s) throws IOException {
    this.mc = mc;
    this.subject = s;
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
    ts.setUseClientMode(false);
    if (logger.traceOn()) logger.trace("initialize", "TLS: Socket Client Mode = " + ts.getUseClientMode());
    // Set the SSLSocket Enabled Protocols
    if (bundledJSSE) {
      String enabledProtocols = (String) env.get("jmx.remote.tls.enabled.protocols");
      if (enabledProtocols != null) {
        StringTokenizer st = new StringTokenizer(enabledProtocols, " ");
        int tokens = st.countTokens();
        String enabledProtocolsList[] = new String[tokens];
        for (int i = 0; i < tokens; i++) enabledProtocolsList[i] = st.nextToken();
        setEnabledProtocols(ts, enabledProtocolsList);
      }
      if (logger.traceOn()) {
        logger.trace("initialize", "TLS: Enabled Protocols");
        String[] enabled_p = getEnabledProtocols(ts);
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

    // Configures the socket to require client authentication
    String needClientAuth = (String) env.get("jmx.remote.tls.need.client.authentication");
    if (needClientAuth != null) ts.setNeedClientAuth(Boolean.valueOf(needClientAuth).booleanValue());
    if (logger.traceOn()) logger.trace("initialize", "TLS: Socket Need Client Authentication = " + ts.getNeedClientAuth());
    // Configures the socket to request client authentication
    if (bundledJSSE) {
      String wantClientAuth = (String) env.get("jmx.remote.tls.want.client.authentication");
      if (wantClientAuth != null) setWantClientAuth(ts, Boolean.valueOf(wantClientAuth));
      if (logger.traceOn()) logger.trace("initialize", "TLS: Socket Want Client Authentication = " + getWantClientAuth(ts));
    }
  }

  @Override
  public ProfileMessage produceMessage() throws IOException {
    TLSMessage tlspm = new TLSMessage(TLSMessage.PROCEED);
    if (logger.traceOn()) {
      logger.trace("produceMessage", ">>>>> TLS server message <<<<<");
      logger.trace("produceMessage", "Profile Name : " + tlspm.getProfileName());
      logger.trace("produceMessage", "Status : " + tlspm.getStatus());
    }
    completed = true;
    return tlspm;
  }

  @Override
  public void consumeMessage(final ProfileMessage pm) throws IOException {
    if (!(pm instanceof TLSMessage)) {
      throw new IOException("Unexpected profile message type: " + pm.getClass().getName());
    }
    TLSMessage tlspm = (TLSMessage) pm;
    if (logger.traceOn()) {
      logger.trace("consumeMessage", ">>>>> TLS client message <<<<<");
      logger.trace("consumeMessage", "Profile Name : " + tlspm.getProfileName());
      logger.trace("consumeMessage", "Status : " + tlspm.getStatus());
    }
    if (tlspm.getStatus() != TLSMessage.READY) {
      throw new IOException("Unexpected TLS status [" + tlspm.getStatus() + "]");
    }
  }

  @Override
  public boolean isComplete() {
    return completed;
  }

  @Override
  public Subject activate() throws IOException {
    if (logger.traceOn()) {
      logger.trace("activate", ">>>>> TLS handshake <<<<<");
      logger.trace("activate", "TLS: Start TLS Handshake");
    }
    ts.startHandshake();
    SSLSession session = ts.getSession();
    if (session != null) {
      if (logger.traceOn()) {
        logger.trace("activate", "TLS: getCipherSuite = " + session.getCipherSuite());
        logger.trace("activate", "TLS: getPeerHost = " + session.getPeerHost());
        if (bundledJSSE) logger.trace("activate", "TLS: getProtocol = " + getProtocol(session));
      }
      // Retrieve the subject distinguished name from the client's certificate, if client authentication was carried out.
      try {
        final X509Certificate[] certificate = session.getPeerCertificateChain();
        if (certificate != null && certificate[0] != null) {
          Principal p = certificate[0].getSubjectDN();
          final String pn = p.getName();
          if (bundledJSSE) {
            try {
              Class<?> cl = Class.forName(X500_PRINCIPAL);
              Constructor<?> co = cl.getConstructor(new Class[] { String.class });
              p = (Principal) co.newInstance(new Object[] { pn });
            } catch (Exception e) {
              final String mh = "TLS: Client Authentication: ";
              logger.trace("activate", mh + e.getMessage());
              logger.debug("activate", e);
              logger.trace("activate", mh + "Got exception building the " + X500_PRINCIPAL + " from the " + "principal stored in the " + "client's certificate.");
              logger.trace("activate", mh + "Subject DN = [" + pn + "]");
              logger.trace("activate", mh + "Default to JMXPrincipal[" + pn + "]");
              p = new JMXPrincipal(pn);
            }
          } else p = new JMXPrincipal(pn);
          final Principal principal = p;
          if (subject == null) subject = new Subject();
          AccessController.doPrivileged(new PrivilegedAction<Object>() {
            @Override
            public Object run() {
              subject.getPrincipals().add(principal);
              return null;
            }
          });
          logger.trace("activate", "TLS: Client Authentication OK!" + " SubjectDN = " + principal);
        } else logger.trace("activate", "TLS: No Client Authentication");
      } catch (SSLPeerUnverifiedException e) {
        logger.trace("activate", "TLS: No Client Authentication: " + e.getMessage());
      }
      logger.trace("activate", "TLS: Finish TLS Handshake");
    }
    // Set new TLS socket in MessageConnection
    ((SocketConnectionIf) mc).setSocket(ts);
    // Return given Subject
    return subject;
  }

  @Override
  public void terminate() throws IOException {
  }

  @Override
  public String getName() {
    return profile;
  }
}
