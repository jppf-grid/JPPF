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
 * @(#)file      NotificationRequestMessage.java
 * @(#)author    Sun Microsystems, Inc.
 * @(#)version   1.7
 * @(#)date      07/03/08
 * @(#)build	@BUILD_TAG_PLACEHOLDER@
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
 * <p>Message that requests received notifications.
 * <p>This message represents the <em>fetch-notifications</em> protocol operation described in the JMX Remote API specification.
 * It is sent from a client to a server to solicit a {@link NotificationResponseMessage}.
 * The server need not reply immediately if it does not have notifications that match the client's criteria.
 */
public class NotificationRequestMessage implements Message {
  /**
   * Explicit serialVersionUID.
   */
  private static final long serialVersionUID = -4639266981029744819L;
  /**
   * The client sequence number.
   */
  private final long clientSequenceNumber;
  /**
   * The maximum number of notifications requested.
   */
  private final int maxNotifications;
  /**
   * The maximum time to wait for notifications.
   */
  private final long timeout;

  /**
   * Constructs a <code>NotificationRequestMessage</code> object. The behavior is unspecified if any of the parameters is negative.
   * @param clientSequenceNumber the first sequence number that the client is interested in. If negative, it is interpreted as meaning the sequence number that the next notification will have.
   * @param maxNotifications the maximum number of different notifications to return.
   * The <code>TargetedNotification</code> array in the {@link NotificationResponseMessage} can have more elements thanthis if the same notification appears more than once.
   * @param timeout the maximum time in milliseconds to wait for a notification to arrive.
   * This can be 0 to indicate that the method should not wait if there are no notifications, but should return atonce. It can be <code>Long.MAX_VALUE</code> to indicate that there is no timeout.
   */
  public NotificationRequestMessage(final long clientSequenceNumber, final int maxNotifications, final long timeout) {
    //	if (clientSequenceNumber < 0
    if (maxNotifications < 0 || timeout < 0)throw new IllegalArgumentException("Bad params");
    this.clientSequenceNumber = clientSequenceNumber;
    this.maxNotifications = maxNotifications;
    this.timeout = timeout;
  }

  /**
   * Returns the client sequence number.
   * @return the client sequence number.
   */
  public long getClientSequenceNumber() {
    return clientSequenceNumber;
  }

  /**
   * Returns the maximum number of notifications requested.
   * @return the maximum number of notifications requested.
   */
  public int getMaxNotifications() {
    return maxNotifications;
  }

  /**
   * Returns the maximum time to wait for notifications.
   * @return the maximum time to wait for notifications.
   */
  public long getTimeout() {
    return timeout;
  }
}
