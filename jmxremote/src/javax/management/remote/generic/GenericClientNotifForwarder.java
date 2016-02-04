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

package javax.management.remote.generic;

import java.io.IOException;
import java.util.Map;

import javax.management.*;
import javax.management.remote.*;
import javax.management.remote.message.*;

import com.sun.jmx.remote.opt.internal.ClientNotifForwarder;
import com.sun.jmx.remote.opt.util.ClassLogger;

/**
 * 
 * @author Laurent Cohen
 */
class GenericClientNotifForwarder extends ClientNotifForwarder {
  /** */
  private static final ClassLogger logger = new ClassLogger("javax.management.remote.generic", "GenericClientCommunicatorAdmin");
  /** */
  private final ClientIntermediary intermediary;

  /**
   * Initialize this notification forwarder witht he specified environment.
   * @param intermediary .
   * @param env the configuration environment to use.
   */
  public GenericClientNotifForwarder(final ClientIntermediary intermediary, final Map<String, ?> env) {
    super(env);
    this.intermediary = intermediary;
  }

  @Override
  protected NotificationResult fetchNotifs(final long clientSequenceNumber, final int maxNotifications, final long timeout) throws IOException, ClassNotFoundException {
    logger.trace("GenericClientNotifForwarder-fetchNotifs", "fetching notifs...");
    final NotificationRequestMessage nreq = new NotificationRequestMessage(clientSequenceNumber, maxNotifications, timeout);
    final NotificationResponseMessage nresp = (NotificationResponseMessage) intermediary.connection.sendWithReturn(nreq);
    Object wrapped = nresp.getWrappedNotificationResult();
    Object unwrapped = intermediary.serialization.unwrap(wrapped, intermediary.myloader);
    if (!(unwrapped instanceof NotificationResult)) {
      // This is a protocol error, so we close the client.
      final String msg = "Not a NotificationResult: " + unwrapped.getClass();
      logger.warning("Forwarder.fetchNotifs", msg);
      logger.warning("Forwarder.fetchNotifs", "closing connector");
      intermediary.client.close();
      // Cast below will generate a ClassCastException, but anyway this thread is going to die.
    }
    return (NotificationResult) unwrapped;
  }

  @Override
  protected Integer addListenerForMBeanRemovedNotif() throws IOException, InstanceNotFoundException {
    logger.trace("GenericClientNotifForwarder-" + "addListenerForMBeanRemovedNotif", "Add a listener to receive UNREGISTRATION_NOTIFICATION");
    NotificationFilterSupport clientFilter = new NotificationFilterSupport();
    clientFilter.enableType(MBeanServerNotification.UNREGISTRATION_NOTIFICATION);
    final ObjectName[] names = { ClientIntermediary.delegateName };
    final Object wrappedFilter = intermediary.serialization.wrap(clientFilter);
    final Object[] filters = { wrappedFilter };
    final Object[] params = { names, filters };
    try {
      int code = MBeanServerRequestMessage.ADD_NOTIFICATION_LISTENERS;
      return (Integer) intermediary.mBeanServerRequest(code, params, null);
    } catch (InstanceNotFoundException n) {
      throw n;
    } catch (Exception e) {
      throw ClientIntermediary.appropriateException(e);
    }
  }

  @Override
  protected void removeListenerForMBeanRemovedNotif(final Integer id) throws IOException {
    logger.trace("GenericClientNotifForwarder-" + "removeListenerForMBeanRemovedNotif", "Remove the listener used to receive " + "UNREGISTRATION_NOTIFICATION.");
    try {
      int code = MBeanServerRequestMessage.REMOVE_NOTIFICATION_LISTENER_FILTER_HANDBACK;
      intermediary.mBeanServerRequest(code, new Object[] { intermediary.delegateName, id }, null, false);
    } catch (Exception e) {
      throw ClientIntermediary.appropriateException(e);
    }
  }

  @Override
  protected void lostNotifs(final String message, final long number) {
    final String notifType = JMXConnectionNotification.NOTIFS_LOST;
    final JMXConnectionNotification n = new JMXConnectionNotification(notifType, intermediary, intermediary.connection.getConnectionId(), intermediary.lostNotifCounter++, message, new Long(number));
    intermediary.client.sendNotification(n);
  }
}
