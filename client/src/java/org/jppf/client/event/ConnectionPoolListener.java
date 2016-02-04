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

import java.util.EventListener;

/**
 * Base interface for classes wishing to be notified of connection pool events.
 * @author Laurent Cohen
 * @since 5.1
 */
public interface ConnectionPoolListener extends EventListener {
  /**
   * Called when a new connection pool is created.
   * @param event encapsulates the information about the event.
   */
  void connectionPoolAdded(ConnectionPoolEvent event);

  /**
   * Called when a connection pool removed.
   * @param event encapsulates the information about the event.
   */
  void connectionPoolRemoved(ConnectionPoolEvent event);

  /**
   * Called when a new connection is created.
   * @param event encapsulates the information about the event.
   */
  void connectionAdded(ConnectionPoolEvent event);

  /**
   * Called when a connection pool is removed.
   * @param event encapsulates the information about the event.
   */
  void connectionRemoved(ConnectionPoolEvent event);
}
