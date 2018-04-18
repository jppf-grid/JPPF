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

package org.jppf.comm.socket;

import org.jppf.utils.*;

/**
 * Common interface for objects that establish a connection with a remote socket.
 * @author Laurent Cohen
 */
public interface SocketInitializer {
  /**
   * Initialize the underlying socket client, by starting a <code>Timer</code> and a corresponding
   * <code>TimerTask</code> until a specified amount of time has passed.
   * @param socketWrapper the socket wrapper to initialize.
   * @return whether the initialization was successful.
   */
  boolean initialize(SocketWrapper socketWrapper);

  /**
   * Close this initializer.
   */
  void close();

  /**
   * Determine whether this socket initializer has been intentionally closed.
   * @return true if this socket initializer has been intentionally closed, false otherwise.
   */
  boolean isClosed();

  /**
   * Get the last captured exception.
   * @return the last captured exception, if any, otherwise {@code null}.
   */
  Exception getLastException();

  /**
   * Factory class for {@code SocketInitializer}s.
   */
  public static class Factory {
    /**
     * The property to use to determine which implementation to use.
     */
    private static final String USE_QUEUING_PROP = "jppf.socket.initializer.queuing";

    /**
     * @return a new {@code SocketInitializer} concrete instance.
     */
    public static SocketInitializer newInstance() {
      return JPPFConfiguration.getProperties().getBoolean(USE_QUEUING_PROP, true) ? new QueuingSocketInitializer() : new SocketInitializerImpl();
    }

    /**
     * @param config the configuration to use.
     * @return a new {@code SocketInitializer} concrete instance.
     */
    public static SocketInitializer newInstance(final TypedProperties config) {
      return config.getBoolean(USE_QUEUING_PROP, true) ? new QueuingSocketInitializer(config) : new SocketInitializerImpl(config);
    }
  }
}
