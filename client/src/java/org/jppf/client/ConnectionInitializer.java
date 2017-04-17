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

package org.jppf.client;

import org.jppf.utils.LoggingUtils;
import org.slf4j.*;

/**
 * Wrapper class for the initialization of a client connection.
 * @exclude
 */
public class ConnectionInitializer implements Runnable {
  /**
   * Logger for this class.
   */
  private static Logger log = LoggerFactory.getLogger(ConnectionInitializer.class);
  /**
   * Determines whether debug-level logging is enabled.
   */
  private static boolean debugEnabled = LoggingUtils.isDebugEnabled(log);
  /**
   * The client connection to initialize.
   */
  private AbstractJPPFClientConnection connection = null;

  /**
   * Instantiate this connection initializer with the specified client connection.
   * @param connection the client connection to initialize.
   */
  public ConnectionInitializer(final AbstractJPPFClientConnection connection) {
    this.connection = connection;
  }

  @Override
  public void run() {
    if (debugEnabled) log.debug("initializing driver connection '" + connection + '\'');
    try {
    connection.setStatus(JPPFClientConnectionStatus.NEW);
    connection.init();
    } finally {
      connection.initializing.set(false);
    }
  }
}
