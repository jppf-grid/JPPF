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
 * @(#)file      SASLClientHandler.java
 * @(#)author    Sun Microsystems, Inc.
 * @(#)version   1.25
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
import javax.security.auth.callback.*;
import javax.security.sasl.*;

import com.sun.jmx.remote.generic.ProfileClient;
import com.sun.jmx.remote.opt.util.ClassLogger;
import com.sun.jmx.remote.socket.SocketConnectionIf;

/**
 * This class implements the client side SASL profile.
 */
public class SASLClientHandler implements ProfileClient {
  /**
   * 
   */
  private SaslClient saslClnt = null;
  /**
   * 
   */
  private boolean completed = false;
  /**
   * 
   */
  private boolean initialResponse = true;
  /**
   * 
   */
  private byte[] blob = null;
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
  private Socket socket = null;
  /**
   * 
   */
  private String mechanism = null;
  /**
   * 
   */
  private String profile = null;
  /**
   * 
   */
  private static final byte[] EMPTY = new byte[0];
  /**
   * 
   */
  private static final ClassLogger logger = new ClassLogger("javax.management.remote.misc", "SASLClientHandler");

  /**
   * 
   * @param profile .
   * @param env .
   */
  public SASLClientHandler(final String profile, final Map<String, Object> env) {
    this.env = env;
    this.profile = profile;
  }

  //---------------------------------------
  // ProfileClient interface implementation
  //---------------------------------------

  @Override
  @SuppressWarnings("unchecked")
  public void initialize(final MessageConnection mc) throws IOException {
    logger.trace("initialize", "starts");
    this.mc = mc;
    // Check if instance of SocketConnectionIf and retrieve underlying socket
    if (mc instanceof SocketConnectionIf) socket = ((SocketConnectionIf) mc).getSocket();
    else throw new IOException("Not an instance of SocketConnectionIf");

    // Prepare parameters for creating SASL client
    String mech = profile.substring(profile.indexOf("SASL/") + 5);
    String[] mechs = getSaslMechanismNames(mech);
    String authzId = (String) env.get("jmx.remote.sasl.authorization.id");
    String server = (String) env.get("jmx.remote.x.sasl.server.name");
    if (server == null) server = socket.getInetAddress().getHostName();
    if (logger.traceOn()) logger.trace("initialize", "mech=" + mech + "; mechs=" + Arrays.asList(mechs) + "; authzId=" + authzId + "; server=" + server);
    CallbackHandler cbh = null;
    if (env.containsKey("jmx.remote.sasl.callback.handler")) {
      cbh = (CallbackHandler) env.get("jmx.remote.sasl.callback.handler");
      if (logger.traceOn()) logger.trace("initialize", "found callback.handler property: " + cbh);
    } else {
      if (env.containsKey("jmx.remote.credentials")) {
        logger.trace("initialize", "found jmx.remote.credentials property");
        Object credso = env.get("jmx.remote.credentials");
        if (!(credso instanceof String[])) {
          if (logger.traceOn()) logger.trace("initialize", "...but it is not a String[]: " + credso);
        } else {
          String[] creds = (String[]) credso;
          if (creds.length != 2) {
            if (logger.traceOn()) logger.trace("initialize", "...but it does not have 2 " + "elements: " + Arrays.asList(creds));
          } else cbh = new UserPasswordCallbackHandler(creds[0], creds[1]);
        }
      }
    }
    // Create SASL client to use using SASL package
    saslClnt = Sasl.createSaslClient(mechs, authzId, "jmxmp", server, env, cbh);
    if (saslClnt == null) throw new IOException("Unable to create SASL client connection for " + "authentication mechanism [" + mech + "]");
    // Retrieve the SASL mechanism in use
    mechanism = saslClnt.getMechanismName();
  }

  @Override
  public ProfileMessage produceMessage() throws IOException {
    if (initialResponse) {
      blob = saslClnt.hasInitialResponse() ? saslClnt.evaluateChallenge(EMPTY) : EMPTY;
      initialResponse = false;
    }
    SASLMessage response = new SASLMessage(mechanism, SASLMessage.CONTINUE, blob);
    if (logger.traceOn()) {
      logger.trace("produceMessage", ">>>>> SASL client message <<<<<");
      logger.trace("produceMessage", "Profile Name : " + response.getProfileName());
      logger.trace("produceMessage", "Status : " + response.getStatus());
    }
    return response;
  }

  @Override
  public void consumeMessage(final ProfileMessage pm) throws IOException {
    if (!(pm instanceof SASLMessage)) throw new IOException("Unexpected profile message type: " + pm.getClass().getName());
    SASLMessage challenge = (SASLMessage) pm;
    if (logger.traceOn()) {
      logger.trace("consumeMessage", ">>>>> SASL server message <<<<<");
      logger.trace("consumeMessage", "Profile Name : " + challenge.getProfileName());
      logger.trace("consumeMessage", "Status : " + challenge.getStatus());
    }
    if (challenge.getStatus() != SASLMessage.CONTINUE && challenge.getStatus() != SASLMessage.COMPLETE)
      throw new IOException("Unexpected SASL status [" + challenge.getStatus() + "]");
    if (saslClnt.isComplete() && challenge.getStatus() == SASLMessage.COMPLETE) {
      completed = true;
      return;
    }
    if (saslClnt.isComplete() && challenge.getStatus() != SASLMessage.COMPLETE)
      throw new IOException("SASL authentication complete despite " + "the server claim for non-completion");
    if (!saslClnt.isComplete() && challenge.getStatus() == SASLMessage.COMPLETE) {
      blob = saslClnt.evaluateChallenge(challenge.getBlob());
      if (saslClnt.isComplete()) {
        completed = true;
        return;
      } else throw new IOException("SASL authentication not complete " + "despite the server claim for " + "completion");
    }
    if (!saslClnt.isComplete() && challenge.getStatus() != SASLMessage.COMPLETE) blob = saslClnt.evaluateChallenge(challenge.getBlob());
  }

  @Override
  public boolean isComplete() {
    return completed;
  }

  @Override
  public void activate() throws IOException {
    // If negotiated integrity or privacy
    //
    String qop = (String) saslClnt.getNegotiatedProperty(Sasl.QOP);
    if (qop != null && (qop.equalsIgnoreCase("auth-int") || qop.equalsIgnoreCase("auth-conf"))) {
      // Replace the current input/output streams in
      // MessageConnection by the SASL input/output streams
      //
      SASLInputStream saslis = new SASLInputStream(saslClnt, socket.getInputStream());
      SASLOutputStream saslos = new SASLOutputStream(saslClnt, socket.getOutputStream());
      ((SocketConnectionIf) mc).replaceStreams(saslis, saslos);
    }
  }

  @Override
  public void terminate() throws IOException {
    saslClnt.dispose();
  }

  @Override
  public String getName() {
    return profile;
  }

  /**
   * Returns an array of SASL mechanisms given a string of space separated SASL mechanism names.
   * @param str The non-null string containing the mechanism names
   * @return A non-null array of String; each element of the array contains a single mechanism name.
   */
  private static String[] getSaslMechanismNames(final String str) {
    StringTokenizer parser = new StringTokenizer(str);
    Vector<String> mechanisms = new Vector<>(10);
    while (parser.hasMoreTokens()) {
      mechanisms.addElement(parser.nextToken());
    }
    return mechanisms.toArray(new String[mechanisms.size()]);
  }

  /**
   * Predefined CallbackHandler for when we get jmx.remote.credentials instead of jmx.remote.sasl.callback.handler. We make a valiant attempt not to hold on to the password any longer than we have to,
   * so that if someone can snoop our memory (perhaps by examining a core dump or the like) they can't fish out passwords. This probably isn't very successful, because the password is there in the
   * user's Map for at least as long as the handshake sequence lasts, and maybe for the entire lifetime of the connection.
   */
  private static class UserPasswordCallbackHandler implements CallbackHandler {
    /**
     * 
     */
    private String user;
    /**
     * 
     */
    private char[] pwchars;

    /**
     * 
     * @param user .
     * @param password .
     */
    UserPasswordCallbackHandler(final String user, final String password) {
      this.user = user;
      this.pwchars = password.toCharArray();
    }

    @Override
    public void handle(final Callback[] callbacks) throws IOException, UnsupportedCallbackException {
      for (int i = 0; i < callbacks.length; i++) {
        if (callbacks[i] instanceof NameCallback) {
          NameCallback ncb = (NameCallback) callbacks[i];
          ncb.setName(user);
        } else if (callbacks[i] instanceof PasswordCallback) {
          PasswordCallback pcb = (PasswordCallback) callbacks[i];
          pcb.setPassword(pwchars);
        } else {
          throw new UnsupportedCallbackException(callbacks[i]);
        }
      }
    }

    /**
     * 
     */
    private void clearPassword() {
      if (pwchars != null) {
        for (int i = 0; i < pwchars.length; i++)
          pwchars[i] = 0;
        pwchars = null;
      }
    }

    @Override
    protected void finalize() {
      clearPassword();
    }
  }
}
