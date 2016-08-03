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

package test.client;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.jppf.client.*;
import org.jppf.client.event.*;
import org.jppf.utils.streams.StreamUtils;

/**
 *
 * @author Laurent Cohen
 */
public class ConnectionPerformance extends ConnectionPoolListenerAdapter implements ClientConnectionStatusListener {
  /**
   *
   */
  private Map<String, ConnectionInfo> map = new ConcurrentHashMap<>();

  /**
   *
   * @param args not used.
   * @throws Throwable .
   */
  public static void main(final String[] args) throws Throwable {
    long start = System.nanoTime();
    try (JPPFClient client = new JPPFClient(new ConnectionPerformance())) {
      client.awaitActiveConnectionPool();
      long elapsed = (System.nanoTime() - start) / 1_000_000L;
      System.out.printf("done in %,d ms%n", elapsed);
    } catch (Exception e) {
      e.printStackTrace();
    }
    StreamUtils.waitKeyPressed();
    //Thread.sleep(10000L);
  }

  @Override
  public void statusChanged(final ClientConnectionStatusEvent event) {
    long time = System.nanoTime();
    JPPFClientConnection connection = (JPPFClientConnection) event.getClientConnectionStatusHandler();
    JPPFClientConnectionStatus status = event.getClientConnectionStatusHandler().getStatus();
    ConnectionInfo info = map.get(connection.getConnectionUuid());
    if (info == null) {
      info = new ConnectionInfo();
      info.connection = connection;
      map.put(connection.getConnectionUuid(), info);
    }
    long elapsed = (time - info.lastStatusTime) / 1_000_000L;
    info.lastStatusTime = time;
    if (status == JPPFClientConnectionStatus.ACTIVE) System.out.printf("connection %s connected in %,d ms%n", connection, elapsed);
    else if (status != JPPFClientConnectionStatus.NEW) System.out.printf("connection %s time since previous status: %,d ms%n", connection, elapsed);
    else System.out.printf("connection status change for %s%n", connection);
  }

  @Override
  public void connectionAdded(final ConnectionPoolEvent event) {
    event.getConnection().addClientConnectionStatusListener(this);
  }

  @Override
  public void connectionRemoved(final ConnectionPoolEvent event) {
    event.getConnection().removeClientConnectionStatusListener(this);
  }

  /** */
  private static class ConnectionInfo {
    /** */
    @SuppressWarnings("unused")
    public JPPFClientConnection connection;
    /** */
    public long lastStatusTime = 0L;
  }
}
