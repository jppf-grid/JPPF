/*
 * JPPF.
 * Copyright (C) 2005-2019 JPPF Team.
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

package org.jppf.classloader;

import static org.jppf.utils.StringUtils.build;

import org.jppf.utils.concurrent.ThreadSynchronization;
import org.slf4j.*;

/**
 * 
 * @author Laurent Cohen
 * @exclude
 */
public class AsyncLocalClassLoaderConnection extends AbstractClassLoaderConnection<AsyncLocalNodeClassloaderContext> {
  /**
   * Logger for this class.
   */
  private static final Logger log = LoggerFactory.getLogger(AsyncLocalClassLoaderConnection.class);
  /**
   * Determines whether the debug level is enabled in the log configuration, without the cost of a method call.
   */
  private static final boolean debugEnabled = log.isDebugEnabled();
  /**
   * Object used to synchronize threads when reading/writing the node message.
   */
  protected final ThreadSynchronization nodeLock = new ThreadSynchronization();
  /**
   * Object used to synchronize threads when reading/writing the server message.
   */
  protected final ThreadSynchronization serverLock = new ThreadSynchronization();

  /**
   * Initialize this connection with the specified channel.
   * @param uuid this node's uuid.
   * @param channel the communication channel with the driver.
   */
  public AsyncLocalClassLoaderConnection(final String uuid, final AsyncLocalNodeClassloaderContext channel) {
    super(uuid);
    this.channel = channel;
  }

  @Override
  public void init() throws Exception {
    lock.lock();
    try {
      if (initializing.compareAndSet(false, true)) {
        try {
          if (debugEnabled) log.debug("initializing {}", this);
          final ResourceRequestRunner rr = new AsyncLocalResourceRequest(channel);
          performCommonHandshake(rr);
          System.out.println(build(getClass().getSimpleName(), ": Reconnected to the class server"));
          if (debugEnabled) log.debug("{} initialized", this);
        } catch (final Exception e) {
          throw new RuntimeException(e);
        } finally {
          initializing.set(false);
        }
      }
    } finally {
      lock.unlock();
    }
  }

  @Override
  public void close() {
    lock.lock();
    try {
      if (requestHandler != null) {
        requestHandler.close();
        requestHandler = null;
      }
    } finally {
      lock.unlock();
    }
  }

  /**
   * Get the object used to synchronize threads when reading/writing the node resource.
   * @return a {@link ThreadSynchronization} instance.
   */
  public ThreadSynchronization getNodeLock() {
    return nodeLock;
  }

  /**
   * Get the object used to synchronize threads when reading/writing the server resource.
   * @return a {@link ThreadSynchronization} instance.
   */
  public ThreadSynchronization getServerLock() {
    return serverLock;
  }

  @Override
  public String toString() {
    return new StringBuilder(getClass().getSimpleName()).append('[')
      .append("uuid = ").append(uuid)
      .append(", channel = ").append(channel)
      .append(']').toString();
  }
}
