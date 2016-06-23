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
 * @(#)file      MessageConnection.java
 * @(#)author    Sun Microsystems, Inc.
 * @(#)version   1.11
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


package javax.management.remote.generic;

import java.io.IOException;
import java.util.Map;

import javax.management.remote.message.Message;

/**
 * <p>Interface specifying the full-duplex transport used by each end
 * of a Generic Connector connection to communicate with the other
 * end.</p>
 *
 * <p>An instance of this interface can be communicated to the Generic
 * Connector using the attribute {@link
 * GenericConnector#MESSAGE_CONNECTION} in the <code>Map</code> passed
 * to the constructor or the {@link GenericConnector#connect(Map)
 * connect} method.</p>
 * @exclude
 */
public interface MessageConnection {
  /**
   * Establish the connection.  This method must be called before any other method of this interface.  The behavior is unspecified if not.
   * @param env the properties of the connection.
   * @exception IOException if the connection cannot be made.
   */
  void connect(Map<String, ?> env) throws IOException;

  /**
   * Reads a <code>Message</code> object from the other end of the connection.
   * @return the message got from the other end of the connection.
   * @exception IOException if a message could not be read because of a communication problem.
   * @exception ClassNotFoundException If the class of a serialized object cannot be found.
   */
  Message readMessage() throws IOException, ClassNotFoundException;

  /**
   * Writes a <code>Message</code> object to the other end of the connection.
   * @param msg the message to be written.
   * @exception IOException if the message could not be written because of a communication problem.
   */
  void writeMessage(Message msg) throws IOException;

  /**
   * Terminates this object connection.  After calling this method, any current or new call to
   * {@link #readMessage readMessage} or {@link #writeMessage(Message)} should produce an <code>IOException</code>.</p>
   * @exception IOException if an I/O error occurs when closing the connection.  A best effort will have been made to clean up the
   * connection's resources.  The caller will not call any other methods of this object after {@code close()}, whether ornot it gets {@code IOException}.
   */
  void close() throws IOException;

  /**
   * The ID for this connection.
   * @return the ID for this connection. This method can return null if the connection handshake is not yet complete.
   */
  String getConnectionId();
}
