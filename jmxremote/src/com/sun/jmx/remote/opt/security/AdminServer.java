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
 * @(#)file      AdminServer.java
 * @(#)author    Sun Microsystems, Inc.
 * @(#)version   1.37
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
import java.util.*;

import javax.management.remote.JMXAuthenticator;
import javax.management.remote.generic.MessageConnection;
import javax.management.remote.message.*;
import javax.security.auth.Subject;

import com.sun.jmx.remote.generic.*;
import com.sun.jmx.remote.opt.util.ClassLogger;
import com.sun.jmx.remote.socket.SocketConnectionIf;

/**
 *
 */
public class AdminServer implements ServerAdmin {
  /**
   * 
   */
  private Map<String, ?> env = null;
  /**
   * 
   */
  private Map<MessageConnection, Subject> subjectsTable = new WeakHashMap<>();
  /**
   * 
   */
  private Map<MessageConnection, List<ProfileServer>> profilesTable = new WeakHashMap<>();
  /**
   * 
   */
  private static final ClassLogger logger = new ClassLogger("javax.management.remote.misc", "AdminServer");

  /**
   * 
   * @param env environment attributes.
   */
  public AdminServer(final Map<String, ?> env) {
    //this.env = (env != null) ? env : Collections.emptyMap(); // ==> why compile error here?
    if (env != null) this.env = env;
    else this.env = Collections.emptyMap();
  }

  @Override
  public MessageConnection connectionOpen(final MessageConnection mc) throws IOException {
    boolean sendError = true;
    try {
      Subject subject = null; // Initialize the Subject that will be passed to all the negotiated profiles
      String serverProfiles = (String) env.get("jmx.remote.profiles");
      String serverVersion = "1.0";
      if (logger.traceOn()) logger.trace("connectionOpen", ">>>>> Handshake Begin <<<<< - Server Supported Profiles [ " + serverProfiles + " ] - Server JMXMP Version [ " + serverVersion + " ]");
      HandshakeBeginMessage begin = new HandshakeBeginMessage(serverProfiles, serverVersion); // Begin Handshake
      mc.writeMessage(begin);
      while (true) {
        Message msg = mc.readMessage();
        if (msg instanceof HandshakeErrorMessage) {
          sendError = false;
          AdminClient.throwExceptionOnError((HandshakeErrorMessage) msg); // Throw exception and let GenericConnectorServer close the connection
        } else if (msg instanceof HandshakeEndMessage) {
          HandshakeEndMessage cend = (HandshakeEndMessage) msg;
          Object ccontext = cend.getContext();
          if (logger.traceOn()) logger.trace("connectionOpen", ">>>>> Handshake End <<<<< - Client Context Object [ " + ccontext + " ]");
          Object scontext = env.get("jmx.remote.context");
          // If MessageConnection is an instance of SocketConnectionIf then set the authenticated subject.
          if (mc instanceof SocketConnectionIf) ((SocketConnectionIf) mc).setSubject(subject);
          String connectionId = mc.getConnectionId();
          // If the environment includes an authenticator, check that it accepts the connection id, and replace the Subject by whatever it returns.
          JMXAuthenticator authenticator = (JMXAuthenticator) env.get("jmx.remote.authenticator");
          if (authenticator != null) {
            Object[] credentials = { connectionId, subject };
            subject = authenticator.authenticate(credentials);
            if (mc instanceof SocketConnectionIf) ((SocketConnectionIf) mc).setSubject(subject);
            connectionId = mc.getConnectionId();
          }
          if (logger.traceOn()) logger.trace("connectionOpen", "Server Context Object [ " + scontext + " ] - Server Connection Id [ " + connectionId + " ]");
          // Check that the negotiated profiles are acceptable for the server's defined security policy. This method is called just before the initial handshake is completed
          // with a HandshakeEndMessage sent from the server to theclient. If the method throws an exception, then aHandshakeErrorMessage will be sent instead.
          List<String> profileNames = getProfilesByName(mc);
          CheckProfiles np = (CheckProfiles) env.get("com.sun.jmx.remote.profile.checker");
          if (np != null) np.checkProfiles(env, profileNames, ccontext, connectionId);
          else checkProfilesForEquality(serverProfiles, profileNames);
          HandshakeEndMessage send = new HandshakeEndMessage(scontext, connectionId);
          mc.writeMessage(send);
          break;
        } else if (msg instanceof VersionMessage) {
          VersionMessage cjmxmp = (VersionMessage) msg;
          String clientVersion = cjmxmp.getVersion();
          if (clientVersion.equals(serverVersion)) {
            VersionMessage sjmxmp = new VersionMessage(serverVersion);
            mc.writeMessage(sjmxmp);
          } else throw new IOException("Protocol version " + "mismatch: Client [" + clientVersion + "] vs. Server [" + serverVersion + "]");
        } else if (msg instanceof ProfileMessage) {
          ProfileMessage pm = (ProfileMessage) msg;
          String pn = pm.getProfileName();
          ProfileServer p = getProfile(mc, pn);
          if (p == null) {
            p = ProfileServerFactory.createProfile(pn, env);
            if (logger.traceOn()) logger.trace("connectionOpen", ">>>>> Profile " + p.getClass().getName() + " <<<<<");
            p.initialize(mc, subject);
            putProfile(mc, p);
          }
          p.consumeMessage(pm);
          pm = p.produceMessage();
          mc.writeMessage(pm);
          if (p.isComplete()) subject = p.activate();
        } else throw new IOException("Unexpected message: " + msg.getClass().getName());
      }
      putSubject(mc, subject);
    } catch (Exception e) {
      if (sendError) {
        try {
          mc.writeMessage(new HandshakeErrorMessage(e.toString()));
        } catch (Exception hsem) {
          if (logger.debugOn()) logger.debug("connectionOpen", "Could not send HandshakeErrorMessage to the client", hsem);
        }
      }
      if (e instanceof RuntimeException) throw (RuntimeException) e;
      else if (e instanceof IOException) throw (IOException) e;
      else throw new IOException(e.getMessage());
    }
    return mc;
  }

  @Override
  public void connectionClosed(final MessageConnection mc) {
    removeSubject(mc);
    removeProfiles(mc);
  }

  @Override
  public Subject getSubject(final MessageConnection mc) {
    synchronized (subjectsTable) {
      return subjectsTable.get(mc);
    }
  }

  /**
   * 
   * @param mc .
   * @param s .
   */
  private void putSubject(final MessageConnection mc, final Subject s) {
    synchronized (subjectsTable) {
      subjectsTable.put(mc, s);
    }
  }

  /**
   * 
   * @param mc .
   */
  private void removeSubject(final MessageConnection mc) {
    synchronized (subjectsTable) {
      subjectsTable.remove(mc);
    }
  }

  /**
   * 
   * @param mc .
   * @param pn .
   * @return .
   */
  private ProfileServer getProfile(final MessageConnection mc, final String pn) {
    synchronized (profilesTable) {
      List<ProfileServer> list = profilesTable.get(mc);
      if (list == null) return null;
      for (ProfileServer p: list) {
        if (p.getName().equals(pn)) return p;
      }
      return null;
    }
  }

  /**
   * 
   * @param mc .
   * @param p .
   */
  private synchronized void putProfile(final MessageConnection mc, final ProfileServer p) {
    synchronized (profilesTable) {
      List<ProfileServer> list = profilesTable.get(mc);
      if (list == null) {
        list = new ArrayList<>();
        profilesTable.put(mc, list);
      }
      if (!list.contains(p)) {
        list.add(p);
      }
    }
  }

  /**
   * 
   * @param mc .
   * @return .
   */
  private List<ProfileServer> getProfiles(final MessageConnection mc) {
    synchronized (profilesTable) {
      return profilesTable.get(mc);
    }
  }

  /**
   * 
   * @param mc .
   * @return .
   */
  private List<String> getProfilesByName(final MessageConnection mc) {
    List<ProfileServer> profiles = getProfiles(mc);
    if (profiles == null) return null;
    List<String> profileNames = new ArrayList<>(profiles.size());
    for (ProfileServer p: profiles) profileNames.add(p.getName());
    return profileNames;
  }

  /**
   * 
   * @param mc .
   */
  private synchronized void removeProfiles(final MessageConnection mc) {
    synchronized (profilesTable) {
      List<ProfileServer> list = profilesTable.get(mc);
      if (list != null) {
        for (ProfileServer p: list) {
          try {
            p.terminate();
          } catch (Exception e) {
            if (logger.debugOn()) {
              logger.debug("removeProfiles", "Got an exception to terminate a ProfileServer: " + p.getName(), e);
            }
          }
        }
        list.clear();
      }
      profilesTable.remove(mc);
    }
  }

  /**
   * 
   * @param serverProfiles .
   * @param clientProfilesList .
   * @throws IOException .
   */
  private void checkProfilesForEquality(final String serverProfiles, final List<String> clientProfilesList) throws IOException {
    // Check for null values. Both the server and the client
    // environment maps did not specified any profile.
    boolean serverFlag = (serverProfiles == null || serverProfiles.equals(""));
    boolean clientFlag = (clientProfilesList == null || clientProfilesList.isEmpty());
    if (serverFlag && clientFlag) return;
    if (serverFlag) throw new IOException("The server does not support any " + "profile but the client requires one");
    if (clientFlag) throw new IOException("The client does not require any " + "profile but the server mandates one");
    // Build ArrayList<String> from server profiles string.
    StringTokenizer sst = new StringTokenizer(serverProfiles, " ");
    List<String> serverProfilesList = new ArrayList<>(sst.countTokens());
    while (sst.hasMoreTokens()) {
      String serverToken = sst.nextToken();
      serverProfilesList.add(serverToken);
    }
    // Check for size equality.
    if (serverProfilesList.size() != clientProfilesList.size()) throw new IOException("The client negotiated profiles do not " + "match the server required profiles.");
    // Check for content equality.
    if (!clientProfilesList.containsAll(serverProfilesList))
      throw new IOException("The client negotiated profiles " + clientProfilesList + " do not match " + "the server required profiles " + serverProfilesList + ".");
  }
}
