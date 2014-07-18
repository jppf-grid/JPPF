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
 * @(#)file      HandshakeBeginMessage.java
 * @(#)author    Sun Microsystems, Inc.
 * @(#)version   1.17
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

package javax.management.remote.message;

/**
 * <p>Handshake begin message exchanged between the server and the client.
 * <p> First of all, when the connection between the client and the server is established the server sends this message to the client
 * with all the server's supported profiles and the server version of the JMXMP protocol.
 * <p> The first thing the client does is to compare the version of the JMXMP protocol supported by the server against the version supported by the client.
 * As a result of this comparison the following things can happen:
 * <ul>
 * <li>Both versions are equal, in which case the client continues with the profiles' negotiation.</li>
 * <li>The client version is greater than the server version and:</li>
 * <ul>
 * <li>The client can work with a lower version that matches the server's version, in which case it switches to the server's version and continues with the profiles' negotiation.</li>
 * <li>The client cannot work with a lower version that matches the server's version, in which case it sends a {@link HandshakeErrorMessage HandshakeErrorMessage} and closes the connection.</li>
 * </ul>
 * <li>The server version is greater than the client version so the client sends a {@link VersionMessage VersionMessage} specifying the client version and:</li>
 * <ul>
 * <li>The server can work with a lower version that matches the client's version, in which case it sends a {@link VersionMessage VersionMessage} specifying the client's version. Upon reception of
 * this message the client continues with the profiles' negotiation.</li>
 * <li>The server cannot work with a lower version that matches the client's version, in which case it sends a {@link HandshakeErrorMessage HandshakeErrorMessage} and closes the connection. Upon
 * reception of this message the client should close the connection immediately.</li>
 * </ul>
 * </ul>
 * <p> Once the JMXMP protocol versions to use have been negotiated, the client and the server start the profile negotiation phase.
 * The server's supported profiles are configured by specifying the {@code jmx.remote.profiles} property in the environment map passed
 * into the {@link javax.management.remote.JMXConnectorServer JMXConnectorServer}. Upon reception of the {@link HandshakeBeginMessage HandshakeBeginMessage}
 * the client verifies that the profiles it wants to use, namely the ones specified through the <code>jmx.remote.profiles</code> property in the
 * environment map passed into the {@link javax.management.remote.JMXConnector JMXConnector} are all present in the server's supported profile list.
 * If false, the client sends a {@link HandshakeErrorMessage HandshakeErrorMessage} to the server and closes the connection.
 * Otherwise, the client starts exchanging profile messages with the server for the selected profiles following the order specified in the client's profile list.
 * Once the profile exchanges between the client and the server are completed the client sends a {@link HandshakeEndMessage HandshakeEndMessage} to notify the server
 * that the handshake exchanges have been successfully completed with regards to the client. Then the server verifies that the negotiated profiles do not
 * compromise the server's minimum required security level and if the server agrees it sends a {@link HandshakeEndMessage HandshakeEndMessage} to notify the client
 * that the handshake exchanges have been successfully completed with regards to the server.
 * If the server does not agree on the negotiated profiles it sends a {@link HandshakeErrorMessage HandshakeErrorMessage} to the client and closes the connection.
 * <p> If an error is encountered at any time, either on the client or the server side, either peer can then send an {@link HandshakeErrorMessage indication} as to why the operation failed.
 * <p> The server's supported profiles contained in this message are formatted as a space-separated list.
 * <p> The server's JMXMP protocol version describes the version of the JMXMP protocol supported by the server.
 */
public class HandshakeBeginMessage implements Message {
  /**
   * Explicit serialVersionUID.
   */
  private static final long serialVersionUID = 293823783004524086L;
  /**
   * @serial The space-separated list of the server's supported profile names.
   * @see #getProfiles()
   */
  private String profiles;
  /**
   * @serial The server version of the JMXMP protocol.
   * @see #getVersion()
   */
  private String version;
  /**
   * Constructs a new HandshakeBeginMessage with the space-separated list of server's supported profile names and the server version of the JMXMP protocol.
   * @param profiles a space-separated list of the server's supported profile names.
   * @param version the server's version of the JMXMP protocol.
   */
  public HandshakeBeginMessage(final String profiles, final String version) {
    this.profiles = profiles;
    this.version = version;
  }

  /**
   * A space-separated list containing the server's supported profile names.
   * @return The space-separated list of the server's supported profile names.
   */
  public String getProfiles() {
    return profiles;
  }

  /**
   * The version of the JMXMP protocol supported by the server.
   * @return The server version of the JMXMP protocol.
   */
  public String getVersion() {
    return version;
  }
}
