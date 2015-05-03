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

package org.jppf.server.nio.nodeserver;

import java.util.concurrent.*;

import org.jppf.management.JMXNodeConnectionWrapper;
import org.jppf.utils.JPPFThreadFactory;

/**
 * 
 * @author Laurent Cohen
 */
public class JMXInitializationHandler {
  /**
   * 
   */
  private final ExecutorService executor = Executors.newFixedThreadPool(5, new JPPFThreadFactory("JMXInitialization", true, true));

  /**
   * 
     * @param connection the JMX connection to initialize.
   */
  public void submit(final JMXNodeConnectionWrapper connection) {
    executor.submit(new JMXInitializationTask(connection));
  }

  /**
   * 
   */
  private class JMXInitializationTask implements Runnable {
    /**
     * The JMX connection to initialize.
     */
    private final JMXNodeConnectionWrapper connection;

    /**
     * 
     * @param connection the JMX connection to initialize.
     */
    public JMXInitializationTask(final JMXNodeConnectionWrapper connection) {
      this.connection = connection;
    }

    @Override
    public void run() {
      connection.connectAndWait(60_000L);
    }
  }
}
