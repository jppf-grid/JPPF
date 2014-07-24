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

package org.jppf.node;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.ReentrantLock;

import org.slf4j.*;

/**
 * Abstract impelementation of {@link ClassLoaderConnection}.
 * @param <C> the type of communication channel used by this connection.
 * @author Laurent Cohen
 * @exclude
 */
public abstract class AbstractNodeConnection<C> implements NodeConnection<C> {
  /**
   * Logger for this class.
   */
  private static Logger log = LoggerFactory.getLogger(AbstractNodeConnection.class);
  /**
   * Determines whether the debug level is enabled in the log configuration, without the cost of a method call.
   */
  private static boolean debugEnabled = log.isDebugEnabled();
  /**
   * The channel used to communicate witht he driver.
   */
  protected C channel = null;
  /**
   * Used to synchronize access to the communication channel from multiple threads.
   */
  protected final ReentrantLock lock = new ReentrantLock();
  /**
   * Determines whether this connection is initializing.
   */
  protected final AtomicBoolean initializing = new AtomicBoolean(false);

  @Override
  public C getChannel() {
    return channel;
  }

  @Override
  public void reset() throws Exception {
    close();
    init();
  }
}
