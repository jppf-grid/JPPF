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

package org.jppf.discovery;

/**
 * Listener interface to receive notifications of new discovered driver connection pools.
 * @param <E> the type of notifications received by this listener.
 * @author Laurent Cohen
 * @exclude
 */
public interface DriverDiscoveryListener<E extends DriverConnectionInfo> {
  /**
   * Called when a new driver connection pool is discovered.
   * @param info encapsulates the connection information about a remote driver.
   * @return {@code true} if the new connection pool was accepted, {@code false} if an identical one was already submitted
   * or this listener is no longer accepting notifications.
   */
  boolean onNewConnection(E info);
}
