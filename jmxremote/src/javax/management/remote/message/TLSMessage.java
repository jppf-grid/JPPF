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
 * @(#)file      TLSMessage.java
 * @(#)author    Sun Microsystems, Inc.
 * @(#)version   1.10
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
 * <p>Handshake message between client and server to set up the TLShandshake.
 * <p>This class represents the handshake messages exchanged between the client and the server to agree on the initiation of the TLS handshake.
 * <p>When the client sends the TLSMessage(READY), it must not send any further traffic on the underlying transport service until a corresponding
 * reply message, either TLSMessage(PROCEED) or an {@link HandshakeErrorMessage indication} message, is received.
 * When the client receives a TLSMessage(PROCEED) in reply to its TLSMessage(READY) it immediately begins the underlying negotiationprocess for TLS transport security.
 * <p>When the server receives the TLSMessage(READY), it must not send any further traffic on the underlying transport service until it generates a corresponding reply.
 * If the server decides to allow TLS transport security negotiation it sends a TLSMessage(PROCEED) and awaits the underlying negotiation process for TLS transport security.
 * Otherwise, the server sends an {@link HandshakeErrorMessage indication} as to why the operation failed.
 * <p>The status attribute takes one of the two values:
 * <ul>
 * <li><b>READY</b> : used by a client to indicate that it is ready to start the TLS handshake.</li>
 * <li><b>PROCEED</b> : used by a server to indicate that it allows the client to proceed with the TLS handshake.</li>
 * </ul>
 * The profile name in this profile message is "TLS".
 * @see HandshakeBeginMessage
 */
public class TLSMessage implements ProfileMessage {
  /**
   * Explicit serialVersionUID.
   */
  private static final long serialVersionUID = -1560560030756307658L;
  /**
   * @serial The status of the current TLS transport security negotiation.
   * @see #getStatus()
   */
  private int status;
  /**
   * This status code is used by a client to indicate that it is ready to start the TLS handshake.
   */
  public static final int READY = 1;
  /**
   * This status code is used by a server to indicate that it allows the client to proceed with the TLS handshake.
   */
  public static final int PROCEED = 2;

  /**
   * Constructs a new TLSMessage with the specified status.
   * @param status the status of the current TLS transport security negotiation.
   */
  public TLSMessage(final int status) {
    this.status = status;
  }

  /**
   * The status of the current TLS transport security negotiation.
   * @return the status of the current TLS transport security negotiation.
   */
  public int getStatus() {
    return status;
  }

  @Override
  public String getProfileName() {
    return "TLS";
  }
}
