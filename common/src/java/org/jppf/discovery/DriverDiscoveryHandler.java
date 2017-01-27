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

import java.util.*;
import java.util.concurrent.atomic.*;

import org.jppf.comm.discovery.JPPFConnectionInformation;
import org.jppf.utils.*;
import org.slf4j.*;

/**
 *
 * @param <E> the type of notifications managed by this handler.
 * @author Laurent Cohen
 * @exclude
 */
public class DriverDiscoveryHandler<E extends DriverConnectionInfo> {
  /**
   * Logger for this class.
   */
  private static Logger log = LoggerFactory.getLogger(DriverDiscoveryHandler.class);
  /**
   * Count of instances of this class.
   */
  private static final AtomicInteger instanceCount = new AtomicInteger(0);
  /**
   * The instance number of this instance.
   */
  private final int instanceNumber = instanceCount.incrementAndGet();
  /**
   * The discovery mechanisms handled by this class.
   */
  private final Map<DriverDiscovery<E>, DiscoveryThread> discoveriesMap = new HashMap<>();
  /**
   * Count of created threads.
   */
  private final AtomicInteger threadCount = new AtomicInteger(0);
  /**
   * The registered listeners.
   */
  private List<DriverDiscoveryListener<E>> listeners = new ArrayList<>();
  /**
   * Whether this handler has been started.
   */
  private final AtomicBoolean started = new AtomicBoolean(false);

  /**
   * Load the discovery mechanisms from the SPI.
   * @param clazz the class of driver discoveries to llokup via SPI.
   */
  public DriverDiscoveryHandler(final Class<DriverDiscovery<E>> clazz) {
    ServiceFinder sf = new ServiceFinder();
    List<DriverDiscovery<E>> list = sf.findProviders(clazz);
    if (list != null) {
      for (DriverDiscovery<E> discovery: list) discoveriesMap.put(discovery, createThread(discovery));
    }
  }

  /**
   * Get the discovered discovery mechanisms.
   * @return a list of {@link DriverDiscovery} instances, eventually empty.
   */
  public List<DriverDiscovery<E>> getDiscoveries() {
    synchronized(discoveriesMap) {
      return new ArrayList<>(discoveriesMap.keySet());
    }
  }

  /**
   *Convert a {@link DriverConnectionInfo} into a {@link JPPFConnectionInformation}.
   * @param driverInfo to data to convert.
   * @return the converted data.
   */
  public static JPPFConnectionInformation toJPPFConnectionInformation(final DriverConnectionInfo driverInfo) {
    JPPFConnectionInformation info = new JPPFConnectionInformation();
    info.host = driverInfo.getHost();
    if (driverInfo.isSecure()) {
      info.sslServerPorts = new int[1];
      info.sslServerPorts[0] = driverInfo.getPort();
    } else {
      info.serverPorts = new int[1];
      info.serverPorts[0] = driverInfo.getPort();
    }
    return info;
  }

  /**
   * Add a new disocvery to this handler.
   * @param discovery the discovery to add.
   */
  public void addDiscovery(final DriverDiscovery<E> discovery) {
    if (discovery != null) {
      synchronized(discoveriesMap) {
        for (DriverDiscoveryListener<E> listener: listeners) discovery.addListener(listener);
        DiscoveryThread thread = createThread(discovery);
        discoveriesMap.put(discovery, thread);
        if (started.get()) thread.start();
      }
    }
  }

  /**
   * Remove an existing disocvery from this handler.
   * @param discovery the discovery to remove.
   */
  public void removeDiscovery(final DriverDiscovery<E> discovery) {
    if (discovery != null) {
      synchronized(discoveriesMap) {
        if (discoveriesMap.containsKey(discovery)) {
          DiscoveryThread thread = discoveriesMap.remove(discovery);
          discovery.stop();
          if (thread.isAlive()) thread.interrupt();
        }
      }
    }
  }

  /**
   * Register the specified disocvery listener with all disoveries found with SPI.
   * @param listener the listener to register.
   * @return this handler.
   */
  public DriverDiscoveryHandler<E> register(final DriverDiscoveryListener<E> listener) {
    if (listener != null) {
      synchronized(discoveriesMap) {
        listeners.add(listener);
        for (DriverDiscovery<E> discovery: discoveriesMap.keySet()) discovery.addListener(listener);
      }
    }
    return this;
  }

  /**
   * Unregister the specified disocvery listener from all discoveries found with SPI.
   * @param listener the listener to unregister.
   * @return this handler.
   */
  public DriverDiscoveryHandler<E> unregister(final DriverDiscoveryListener<E> listener) {
    if (listener != null) {
      synchronized(discoveriesMap) {
        listeners.remove(listener);
        for (DriverDiscovery<E> discovery: discoveriesMap.keySet()) discovery.removeListener(listener);
      }
    }
    return this;
  }

  /**
   * Start all discoveries found via SPI.
   * @return this handler.
   */
  public DriverDiscoveryHandler<E> start() {
    if (started.compareAndSet(false, true)) {
      synchronized(discoveriesMap) {
        for (Map.Entry<DriverDiscovery<E>, DiscoveryThread> entry: discoveriesMap.entrySet()) entry.getValue().start();
      }
    }
    return this;
  }

  /**
   * Closes this discovery and releases any resource it uses.
   * @return this handler.
   */
  public DriverDiscoveryHandler<E> stop() {
    if (started.compareAndSet(true, false)) {
      listeners.clear();
      synchronized(discoveriesMap) {
        for (DriverDiscovery<E> discovery: discoveriesMap.keySet()) discovery.stop();
        for (Map.Entry<DriverDiscovery<E>, DiscoveryThread> entry: discoveriesMap.entrySet()) {
          DiscoveryThread thread = entry.getValue();
          if (thread.isAlive()) thread.interrupt();
        }
        discoveriesMap.clear();
      }
    }
    return this;
  }

  /**
   * Create a thread for the specified disocvery.
   * @param discovery the disocvery for which to create a thread.
   * @return an instance of {@link DiscoveryThread}.
   */
  private DiscoveryThread createThread(final DriverDiscovery<E> discovery) {
    return new DiscoveryThread(String.format("DriverDiscovery-%04d-%04d", instanceNumber, threadCount.incrementAndGet()), discovery);
  }

  /**
   * Thread implementation wrapping the execution of the {@code discover()} method of each discovery.
   */
  class DiscoveryThread extends Thread {
    /**
     * The discovery to run in its own thread.
     */
    final DriverDiscovery<E> discovery;

    /**
     * Intiialize this thread witht he specified name and disocvery.
     * @param name the name given to this thread.
     * @param discovery the {@link DriverDiscovery} to run.
     */
    DiscoveryThread(final String name, final DriverDiscovery<E> discovery) {
      super(name);
      this.discovery = discovery;
    }

    @Override
    public void run() {
      try {
        discovery.discover();
      } catch (@SuppressWarnings("unused") InterruptedException  ignore) {
      } catch (Throwable t) {
        log.error(String.format("Error while running discovery %s in thread %s: %s", discovery, this, ExceptionUtils.getStackTrace(t)));
      }
    }
  }
}
