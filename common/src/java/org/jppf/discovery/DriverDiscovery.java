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

package org.jppf.discovery;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Abstract superclass for custom driver discovery mechanisms.
 * @param <E> the type of notifications sent by this discovery.
 * @since 5.2.1
 */
public abstract class DriverDiscovery<E extends DriverConnectionInfo> {
  /**
   * The registered listeners to this discovery source.
   */
  private List<DriverDiscoveryListener<E>> listeners = new CopyOnWriteArrayList<>();

  /**
   * Perform the driver discovery. This method runs in its own separate thread.
   * <p>To notify the client that a new driver is discovered, the {@link #newConnection(DriverConnectionInfo)} method must be called.
   * @throws InterruptedException if the thread running this method is still alive when the component that started it (a JPPF client or driver) is closed or shut down.
   */
  public abstract void discover() throws InterruptedException;

  /**
   * Notify that a new driver was discovered.
   * @param info encapsulates the driver connection information and configuration,
   */
  protected void newConnection(final E info) {
    for (DriverDiscoveryListener<E> listener: listeners) listener.onNewConnectionPool(info);
  }

  /**
   * Add the specified listener to the list of listeners.
   * @param listener the listener to add.
   */
  void addListener(final DriverDiscoveryListener<E> listener) {
    if (listener != null) listeners.add(listener);
  }

  /**
   * Remove the specified listener from the list of listeners.
   * @param listener the listener to remove.
   */
  void removeListener(final DriverDiscoveryListener<E> listener) {
    if (listener != null) listeners.remove(listener);
  }

  /**
   * Closes this discovery and releases any resource it uses.
   */
  void stop() {
    listeners.clear();
    shutdown();
  }

  /**
   * Shut this discovery down. This method is intended to be overriden in subclasses to allow user-defined cleanup operations.
   * <p>Caution: this method is called in a different thread from the one that runs the {@link #discover()} method.
   */
  public void shutdown() {
  }
}
