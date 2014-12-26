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
 * @(#)ArrayNotificationBuffer.java 1.3
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
 *
 */

package com.sun.jmx.remote.opt.internal;

import java.util.Set;

import javax.management.*;
import javax.management.remote.NotificationResult;

import com.sun.jmx.remote.opt.util.ClassLogger;

/**
 * 
 * @author Laurent Cohen
 */
class ArrayNotificationBufferAux {

  /**
   */
  static class ShareBuffer implements NotificationBuffer {
    /** */
    private final int size;
    /** */
    private final ArrayNotificationBuffer buffer;
  
    /**
     * @param buffer .
     * @param size .
     */
    ShareBuffer(final ArrayNotificationBuffer buffer, final int size) {
      this.size = size;
      this.buffer = buffer;
      buffer.addSharer(this);
    }
  
    @Override
    public NotificationResult fetchNotifications(final Set<ListenerInfo> listeners, final long startSequenceNumber, final long timeout, final int maxNotifications) throws InterruptedException {
      NotificationBuffer buf = buffer;
      return buf.fetchNotifications(listeners, startSequenceNumber, timeout, maxNotifications);
    }
  
    @Override
    public void dispose() {
      buffer.removeSharer(this);
    }
  
    /**
     * @return .
     */
    int getSize() {
      return size;
    }
  }

  /**
   */
  static class NamedNotification {
    /**
     */
    private final ObjectName sender;
    /**
     */
    private final Notification notification;
  
    /**
     * @param sender .
     * @param notif .
     */
    NamedNotification(final ObjectName sender, final Notification notif) {
      this.sender = sender;
      this.notification = notif;
    }
  
    /**
     * @return .
     */
    ObjectName getObjectName() {
      return sender;
    }
  
    /**
     * @return .
     */
    Notification getNotification() {
      return notification;
    }
  
    @Override
    public String toString() {
      return "NamedNotification(" + sender + ", " + notification + ")";
    }
  }

  /**
   *
   */
  static class BufferListener implements NotificationListener {
    /** */
    private static final ClassLogger logger = new ClassLogger("javax.management.remote.misc", "ArrayNotificationBuffer");
    /** */
    final ArrayNotificationBuffer buffer;
  
    /**
     * @param buffer .
     */
    public BufferListener(final ArrayNotificationBuffer buffer) {
      this.buffer = buffer;
    }
    @Override
    public void handleNotification(final Notification notif, final Object handback) {
      if (logger.debugOn()) logger.debug("BufferListener.handleNotification", "notif=" + notif + "; handback=" + handback);
      ObjectName name = (ObjectName) handback;
      buffer.addNotification(new NamedNotification(name, notif));
    }
  }

  /**
   *
   */
  static class BroadcasterQuery extends QueryEval implements QueryExp {
    @Override
    public boolean apply(final ObjectName name) {
      final MBeanServer mbs = QueryEval.getMBeanServer();
      return ArrayNotificationBuffer.isInstanceOf(mbs, name, ArrayNotificationBuffer.broadcasterClass);
    }
  }

}
