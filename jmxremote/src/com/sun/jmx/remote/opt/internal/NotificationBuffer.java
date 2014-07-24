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
 * @(#)NotificationBuffer.java	1.3
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

package com.sun.jmx.remote.opt.internal;

import java.util.Set;

import javax.management.remote.*;

/** A buffer of notifications received from an MBean server. */
public interface NotificationBuffer {
  /**
   * <p>Fetch notifications that match the given listeners.</p>
   * <p>The operation only considers notifications with a sequence number at least <code>startSequenceNumber</code>.
   * It will take no longer than <code>timeout</code>, and will return no more than <code>maxNotifications</code> different notifications.</p>
   * <p>If there are no notifications matching the criteria, the operation will block until one arrives, subject to the timeout.</p>
   * @param listeners a Set of {@link ListenerInfo} that reflects the filters to be applied to notifications.
   * Accesses to this Set are synchronized on the Set object. The Set is consulted for selected notifications
   * that are present when the method starts, and for selected notifications that arrive while it is executing.
   * The contents of the Set can be modified, with appropriate synchronization, while the method is running.
   * @param startSequenceNumber the first sequence number to consider.
   * @param timeout the maximum time to wait. May be 0 to indicate not to wait if there are no notifications.
   * @param maxNotifications the maximum number of notifications to return.
   * May be 0 to indicate a wait for eligible notifications that will return a usable <code>nextSequenceNumber</code>.
   * The {@link TargetedNotification} array in the returned {@link NotificationResult} may contain more than this number
   * of elements but will not contain more than this number of different notifications.
   * @return .
   * @throws InterruptedException .
   */
  NotificationResult fetchNotifications(Set<ListenerInfo> listeners, long startSequenceNumber, long timeout, int maxNotifications) throws InterruptedException;

  /**
   * Discard this buffer.
   */
  void dispose();
}
