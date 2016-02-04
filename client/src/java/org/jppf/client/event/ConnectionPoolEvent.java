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

package org.jppf.client.event;

import java.util.EventObject;

import org.jppf.client.*;

/**
 * Instances of this class represent events in a {@link JPPFConnectionPool}'s life cycle.
 * @author Laurent Cohen
 * @since 5.1
 */
public class ConnectionPoolEvent extends EventObject {
  /**
   * The connection that triggered this event.
   */
  private final JPPFClientConnection connection;

  /**
   * Initialize this event with the specified source.
   * @param pool the source of this event.
   * @exclude
   */
  public ConnectionPoolEvent(final JPPFConnectionPool pool) {
    this(pool, null);
  }

  /**
   * Initialize this event with the specified source and client connection.
   * @param pool the source of this event.
   * @param connection the connection that triggered this event.
   * @exclude
   */
  public ConnectionPoolEvent(final JPPFConnectionPool pool, final JPPFClientConnection connection) {
    super(pool);
    this.connection = connection;
  }

  /**
   * Get the source of this event.
   * @return a {@link JPPFConnectionPool} instance.
   */
  public JPPFConnectionPool getConnectionPool() {
    return (JPPFConnectionPool) getSource();
  }

  /**
   * Get the connection that triggered this event.
   * @return a {@link JPPFClientConnection} instance.
   */
  public JPPFClientConnection getConnection() {
    return connection;
  }
}
