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

package org.jppf.nio;

import java.lang.reflect.Method;
import java.util.*;
import java.util.concurrent.*;

import org.jppf.nio.acceptor.AcceptorNioServer;
import org.jppf.utils.*;
import org.jppf.utils.concurrent.ConcurrentUtils;
import org.jppf.utils.configuration.JPPFProperties;
import org.slf4j.*;

/**
 *
 * @author Laurent Cohen
 */
public class NioHelper {
  /**
   * Logger for this class.
   */
  private static Logger log = LoggerFactory.getLogger(NioHelper.class);
  /**
   * Determines whether the debug level is enabled in the log configuration, without the cost of a method call.
   */
  private static boolean debugEnabled = log.isDebugEnabled();
  /**
   * Prefix for the NIO thread names.
   */
  public static final String NIO_THREAD_NAME_PREFIX = JPPFConfiguration.getProperties().getString("jppf.nio.thread.name.prefix", "JPPF");
  /**
   * Mapping of NIO servers to their identifier.
   */
  private static final Map<Integer, NioServer<?, ?>> identifiedServers = new HashMap<>();
  /**
   * Global thread pool used by all NIO servers.
   */
  private static final ExecutorService globalExecutor = createExecutorFromConfig(NIO_THREAD_NAME_PREFIX);
  /**
   * 
   */
  private static Method getJMXServerMethod;

  /**
   * Map the specified server tot he specified identifier.
   * @param identifier the JPPF identifier to mao to.
   * @param server the server to map.
   */
  public static void putServer(final int identifier, final NioServer<?, ?> server) {
    synchronized(identifiedServers) {
      identifiedServers.put(identifier, server);
    }
  }

  /**
   * Map the specified server tot he specified identifier.
   * @param identifier the JPPF identifier to mao to.
   * @return the server that was removed, or {@code null} if there was no server for the specified identifier.
   */
  public static NioServer<?, ?> removeServer(final int identifier) {
    synchronized(identifiedServers) {
      return identifiedServers.remove(identifier);
    }
  }

  /**
   * Get the server mapped to the specified identifier.
   * @param identifier the JPPF identifier to lookup.
   * @return a {@link NioServer} instance.
   * @throws Exception if any error occurs.
   */
  public static NioServer<?, ?> getServer(final int identifier) throws Exception {
    synchronized(identifiedServers) {
      if (identifier == JPPFIdentifiers.JMX_REMOTE_CHANNEL) {
        if (getJMXServerMethod == null) initializeJMXServerPool();
        return (NioServer<?, ?>) getJMXServerMethod.invoke(null); 
      }
      return identifiedServers.get(identifier);
    }
  }

  /**
   * Get the acceptor server.
   * @return a {@link NioServer} instance.
   * @throws Exception if any error occurs.
   */
  public static NioServer<?, ?> getAcceptorServer() throws Exception {
    synchronized(identifiedServers) {
      NioServer<?, ?> acceptor = identifiedServers.get(JPPFIdentifiers.ACCEPTOR_CHANNEL);
      if (acceptor == null) {
        if (debugEnabled) log.debug("starting acceptor");
        acceptor = new AcceptorNioServer(null, null);
        putServer(JPPFIdentifiers.ACCEPTOR_CHANNEL, acceptor);
        acceptor.start();
        if (debugEnabled) log.debug("acceptor started");
      }
      return acceptor;
    }
  }

  /**
   * Initialize the executor for this transition manager.
   * @param prefix name of the thread group and prefix for the thread names.
   * @return an {@link ExecutorService} object.
   * @since 5.0
   */
  public static ExecutorService createExecutorFromConfig(final String prefix) {
    final int core = NioConstants.THREAD_POOL_SIZE;
    final String poolType = JPPFConfiguration.get(JPPFProperties.NIO_THREAD_POOL_TYPE);
    ThreadPoolExecutor tpe = null;
    ExecutorService executor = null;
    if ("dynamic".equals(poolType)) {
      final int queueSize = JPPFConfiguration.get(JPPFProperties.NIO_THREAD_QUEUE_SIZE);
      final long ttl = JPPFConfiguration.get(JPPFProperties.NIO_THREAD_TTL);
      executor = tpe = ConcurrentUtils.newBoundedQueueExecutor(core, queueSize, ttl, prefix);
      if (debugEnabled) log.debug(String.format(Locale.US, "dynamic globalExecutor: core=%,d; queueSize=%,d; ttl=%,d; maxSize=%,d", core, queueSize, ttl, tpe.getMaximumPoolSize()));
    } else if ("sync".equals(poolType)) {
      final long ttl = JPPFConfiguration.get(JPPFProperties.NIO_THREAD_TTL);
      executor = tpe = ConcurrentUtils.newDirectHandoffExecutor(core, ttl, prefix);
      if (debugEnabled) log.debug(String.format(Locale.US, "sync globalExecutor: core=%,d; ttl=%,d; maxSize=%,d", core, ttl, tpe.getMaximumPoolSize()));
    } else if ("jppf".equals(poolType)) {
      final long ttl = JPPFConfiguration.get(JPPFProperties.NIO_THREAD_TTL);
      //executor = ConcurrentUtils.newJPPFThreadPool(core, Integer.MAX_VALUE, ttl, prefix);
      executor = ConcurrentUtils.newJPPFDirectHandoffExecutor(core, Integer.MAX_VALUE, ttl, prefix);
      if (debugEnabled) log.debug(String.format(Locale.US, "sync globalExecutor: core=%,d; ttl=%,d; maxSize=%,d", core, ttl, Integer.MAX_VALUE));
    } else {
      executor = tpe = ConcurrentUtils.newFixedExecutor(core, prefix);
      if (debugEnabled) log.debug(String.format(Locale.US, "fixed globalExecutor: core=%,d; maxSize=%,d", core, tpe.getMaximumPoolSize()));
    }
    return executor;
  }

  /**
   * @return the global thread pool used by all NIO servers.
   */
  public static ExecutorService getGlobalexecutor() {
    return globalExecutor;
  }

  /**
   * Shutdown the global executor for all transition managers.
   * @param now if {@code true} then call {@code shutdownNow()} on the executor, otherwise, call {@code shutdown()}.
   */
  public static void shutdown(final boolean now) {
    if (now) globalExecutor.shutdownNow();
    else globalExecutor.shutdown();
  }

  /**
   * Initialize the JMX server pool.
   * @throws Exception if any error occurs.
   */
  private static void initializeJMXServerPool() throws Exception {
    final String className = "org.jppf.jmxremote.nio.JMXNioServerPool";
    final Class<?> clazz = Class.forName(className);
    getJMXServerMethod = clazz.getDeclaredMethod("getServer");
  }
}
