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

package javax.management.remote.generic;

import java.io.*;

import javax.management.InstanceNotFoundException;
import javax.management.remote.message.MBeanServerRequestMessage;

import com.sun.jmx.remote.opt.internal.*;
import com.sun.jmx.remote.opt.util.*;

/**
 * 
 * @author Laurent Cohen
 */
public class GenericClientCommunicatorAdmin extends ClientCommunicatorAdmin {
  /** */
  private static final ClassLogger logger = new ClassLogger("javax.management.remote.generic", "GenericClientCommunicatorAdmin");
  /** */
  private final ClientIntermediary intermediary;

  /**
   * Initialize this client admin.
   * @param intermediary .
   * @param period .
   */
  public GenericClientCommunicatorAdmin(final ClientIntermediary intermediary, final long period) {
    super(period);
    this.intermediary = intermediary;
  }

  @Override
  protected void checkConnection() throws IOException {
    try {
      intermediary.mBeanServerRequest(MBeanServerRequestMessage.GET_DEFAULT_DOMAIN, null, null, false);
    } catch (InterruptedIOException irie) { // see  6496038
      logger.trace("GenericClientCommunicatorAdmin-" + "checkConnection", "Timeout?", irie);
      if (intermediary.requestTimeoutReconn) throw (IOException) EnvHelp.initCause(new IOException(irie.getMessage()), irie); // force the heartbeat to do reconnection
      // no exception. not sure that the connection is lost, let the heartbeat to try again
      return;
    } catch (Exception e) {
      throw ClientIntermediary.appropriateException(e);
    }
  }

  /**
   * @param old .
   * @throws IOException .
   */
  public void reconnectNotificationListeners(final ClientListenerInfo[] old) throws IOException {
    ClientListenerInfo[] clis = new ClientListenerInfo[old.length];
    int j = 0;
    for (int i = 0; i < old.length; i++) { // reconnect listeners one by one...
      try {
        Integer id = intermediary.addListenerWithSubject(old[i].getObjectName(), intermediary.serialization.wrap(old[i].getNotificationFilter()), old[i].getDelegationSubject(), false);
        clis[j++] = new ClientListenerInfo(id, old[i].getObjectName(), old[i].getListener(), old[i].getNotificationFilter(), old[j].getHandback(), old[i].getDelegationSubject());
      } catch (InstanceNotFoundException infe) {
        logger.warning("reconnectNotificationListeners", "Can't reconnect a listener for " + old[i].getObjectName(), infe);
      }
    }
    // we should call postReconnection even j == 0, because we have to inform the notif forwarder of end of reconnection.
    if (j != old.length) {
      ClientListenerInfo[] tmp = clis;
      clis = new ClientListenerInfo[j];
      System.arraycopy(tmp, 0, clis, 0, j);
    }
    intermediary.notifForwarder.postReconnection(clis);
  }

  @Override
  protected void doStart() throws IOException {
    intermediary.connection = intermediary.client.reconnect();
    final ClientListenerInfo[] old = intermediary.notifForwarder.preReconnection();
    reconnectNotificationListeners(old);
  }

  @Override
  protected void doStop() {
    try {
      intermediary.client.close();
    } catch (IOException ioe) {
      logger.info("close", ioe);
    }
  }
}
