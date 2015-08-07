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
 * @(#)file      VersionMessage.java
 * @(#)author    Sun Microsystems, Inc.
 * @(#)version   1.14
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
 * This class represents the handshake messages exchanged between the client and the server to agree on the protocol version.
 * <p>The protocol version is implicitly negotiated by the client and the server when necessary at the beginning of the handshake phase.
 * <p>Refer to the {@link HandshakeBeginMessage HandshakeBeginMessage} documentation for a full description of how the protocol version is negotiated.
 * <p>The textual representation of the protocol version must be a series of non-negative decimal integers each separated by a period from the one that precedes it.
 * For example, "1.0" is earlier than "1.0.1" and "2.0".
 * @see HandshakeBeginMessage
 */
public class VersionMessage implements Message {
  /**
   * Explicit serialVersionUID.
   */
  private static final long serialVersionUID = 1820574193954947740L;
  /**
   * @serial The latest version of the JMXMP protocol acceptable for use.
   * @see #getVersion()
   **/
  private String version;

  /**
   * Constructs a new VersionMessage with the specified protocol version.
   * @param version the latest protocol version acceptable for use.
   */
  public VersionMessage(final String version) {
    this.version = version;
  }

  /**
   * The latest protocol version acceptable for use.
   * <p>The textual representation of the protocol version must be a series of non-negative decimal integers each separated by a period from the one that precedes it.
   * For example, "1.0" is earlier than "1.0.1" and "2.0".
   * @return The latest protocol version acceptable for use.
   */
  public String getVersion() {
    return version;
  }
}
