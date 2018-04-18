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

import java.util.concurrent.*;

import org.jppf.utils.*;
import org.jppf.utils.concurrent.JPPFThreadFactory;
import org.slf4j.*;

/**
 * This implementation uses a fixed thread pool in an attempt at reducing the JDK-level contentions triggered by
 * {@code SockerChannel.open()} and {@code SockerChannel.connect()}, when creating and connecting many channels concurrently.
 * @author Laurent Cohen
 */
class QueuingSocketInitializer extends SocketInitializerImpl {
  /**
   * Logger for this class.
   */
  private static final Logger log = LoggerFactory.getLogger(QueuingSocketInitializer.class);
  /**
   * 
   */
  private static final ExecutorService executor = initExecutor();

  /**
   * Instantiate this SocketInitializer with the global JPPF configuration.
   */
  public QueuingSocketInitializer() {
    super(JPPFConfiguration.getProperties());
  }

  /**
   * Instantiate this SocketInitializer with a specified configuration.
   * @param config the configuration to use.
   */
  QueuingSocketInitializer(final TypedProperties config) {
    super(config);
  }

  @Override
  public boolean initialize(final SocketWrapper socketWrapper) {
    final Future<Boolean> f = executor.submit(new Callable<Boolean>() {
      @Override
      public Boolean call() throws Exception {
        return QueuingSocketInitializer.super.initialize(socketWrapper);
      }
    });
    try {
      return f.get();
    } catch (final Exception e) {
      if (lastException == null) lastException = e;
      log.error(e.getMessage(), e);
    }
    return false;
  }

  /**
   * @return an {@link ExecutorService}.
   */
  private static ExecutorService initExecutor() {
    final TypedProperties config = JPPFConfiguration.getProperties();
    final long ttl = config.getLong("jppf.socket.initializer.thread.ttl", 5000L);
    final ThreadPoolExecutor tpe = new ThreadPoolExecutor(1, Integer.MAX_VALUE, ttl, TimeUnit.MILLISECONDS, new SynchronousQueue<Runnable>(), new JPPFThreadFactory("SocketInitializer"));
    tpe.allowCoreThreadTimeOut(true);
    return tpe;
  }
}
