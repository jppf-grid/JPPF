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
package org.jppf.android;

import android.util.Log;

import org.jppf.node.connection.ConnectionContext;
import org.jppf.node.connection.DriverConnectionInfo;
import org.jppf.node.connection.DriverConnectionStrategy;
import org.jppf.node.connection.JPPFDriverConnectionInfo;
import org.jppf.utils.JPPFConfiguration;
import org.jppf.utils.TypedProperties;

import java.util.ArrayList;
import java.util.List;

/**
 * Implementation of a connection strategy which configures itself from the node settings.
 * @author Laurent Cohen
 */
public class AndroidNodeConnectionStrategy implements DriverConnectionStrategy {
  /**
   * Tag used for logging.
   */
  private final static String LOG_TAG = AndroidNodeConnectionStrategy.class.getSimpleName();
  /**
   * Whether to use secure connections.
   */
  private final static boolean SECURE = true;
  /**
   * The list of available connection settings.
   */
  private List<DriverConnectionInfo> connections = new ArrayList<>();
  /**
   * The current position in the list of connections.
   */
  private int pos = 0;

  @Override
  public DriverConnectionInfo nextConnectionInfo(final DriverConnectionInfo currentInfo, final ConnectionContext context) {
    switch(context.getReason()) {
      case JOB_CHANNEL_INIT_ERROR:
        JPPFConfiguration.reset();
        AndroidHelper.changeConfigFromPrefs();
        //SSLHelper.resetConfig();
        break;
    }
    List<DriverConnectionInfo> list = fetchConnections();
    if (!connections.equals(list)) {
      connections = list;
      Log.d(LOG_TAG, String.format("new list of connections = %s", connections));
      pos = -1;
    }
    if (connections.isEmpty()) connections.add(new JPPFDriverConnectionInfo(SECURE, "192.168.1.1", 11111, -1));
    pos++;
    if (pos >= connections.size()) pos = 0;
    DriverConnectionInfo next = connections.get(pos);
    Log.d(LOG_TAG, String.format("attempting connection to %s, pos = %d", next, pos));
    return next;
  }

  /**
   * Compute a list of driver connections objects based on a configuration property updated from the shared preferences.
   * @return a list of {@link DriverConnectionInfo} objects, possibly empty.
   */
  private List<DriverConnectionInfo> fetchConnections() {
    List<DriverConnectionInfo> list = new ArrayList<>();
    TypedProperties config = JPPFConfiguration.getProperties();
    String servers = config.getString("jppf.node.android.connections", "");
    String[] connectionInfos = servers.split("\\s");
    for (String info: connectionInfos) {
      String[] comps = info.split(":");
      if (comps.length > 0) {
        String host = comps[0];
        boolean secure = (comps.length > 2) ? Boolean.valueOf(comps[2]) : true ;
        int port = secure ? 11443 : 11111;
        try {
          if ((comps.length > 1) && (comps[1] != null)) {
            String s = comps[1].trim();
            if (!s.isEmpty()) port = Integer.valueOf(s);
          }
        } catch (Exception e) {
          Log.e(LOG_TAG, String.format("error parsing port number for connection strategy '%s'", info), e);
        }
        list.add(new JPPFDriverConnectionInfo(secure, host, port, -1));
      }
    }
    return list;
  }
}
