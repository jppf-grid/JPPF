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

package org.jppf.load.balancer.persistence;

import java.util.*;
import java.util.concurrent.locks.Lock;

import org.jppf.load.balancer.Bundler;
import org.jppf.load.balancer.spi.JPPFBundlerFactory;
import org.jppf.utils.Pair;
import org.slf4j.*;

/**
 * This class is a common facade to the load-balancer persistence for both drivers and clients.
 * It is also a common implementation of the load-balancer persistence's {@link LoadBalancerPersistenceManagement management interface}.
 * @author Laurent Cohen
 * @since 6.0
 * @exclude
 */
public class LoadBalancerPersistenceManager implements LoadBalancerPersistenceManagerMBean {
  /**
   * Logger for this class.
   */
  private static Logger log = LoggerFactory.getLogger(LoadBalancerPersistenceManager.class);
  /**
   * Determines whether the debug level is enabled in the log configuration, without the cost of a method call.
   */
  private static boolean debugEnabled = log.isDebugEnabled();
  /**
   * The load-balancer factory.
   */
  private final JPPFBundlerFactory factory;
  /**
   * The persistence implementation, or {@code null} if disabled.
   */
  private final LoadBalancerPersistence persistence;

  /**
   * Intiialize with the specified bundler factory.
   * @param factory the load-balancer factory.
   */
  public LoadBalancerPersistenceManager(final JPPFBundlerFactory factory) {
    if (factory == null) throw new IllegalArgumentException("JPPFBundlerFactory cannot be null");
    this.factory = factory;
    this.persistence = factory.getPersistence();
    log.info("load-balancer persistence is {}", (persistence == null) ? "not configured" : persistence);
  }

  @Override
  public boolean isPersistenceEnabled() {
    return persistence != null;
  }

  /**
   * Load the bundler state for the specified node, with the currently configured algorithm.
   * @param channelId identifier of the channel for which to load the load-balancer state.
   * @return a {@link Bundler} instance.
   */
  public Pair<String, Bundler<?>> loadBundler(final Pair<String, String> channelId) {
    final Bundler<?> bundler = factory.newBundler();
    final String algo = factory.getCurrentInfo().getAlgorithm();
    if (isPersistenceEnabled() && (bundler instanceof PersistentState)) {
      final LoadBalancerPersistenceInfo info = new LoadBalancerPersistenceInfo(channelId.first(), channelId.second(), algo, factory.getAlgorithmHash(algo), null);
      try {
        final Object state = persistence.load(info);
        if (debugEnabled) log.debug("persisted state for {} is " + state, info);
        if (state != null) ((PersistentState) bundler).setState(state);
      } catch (final Exception e) {
        log.error(e.getMessage(), e);
      }
    }
    return new Pair<String, Bundler<?>>(algo, bundler);
  }

  /**
   * Store the specified bundler's state for the specified channel, with the currently configured algorithm.
   * @param channelId identifier of the channel for which to store the bundler state.
   * @param bundler the load-balancer whose state to save.
   * @param algorithm the bundler's algorithm.
   */
  public void storeBundler(final Pair<String, String> channelId, final Bundler<?> bundler, final String algorithm) {
    if (isPersistenceEnabled() && (bundler instanceof PersistentState)) {
      try {
        final Object state = ((PersistentState) bundler).getState();
        if (state != null) {
          final Lock lock = ((PersistentState) bundler).getStateLock();
          //if (debugEnabled) log.debug("persisting state for {}, {}", nodeId, algorithm);
          final LoadBalancerPersistenceInfo info = new LoadBalancerPersistenceInfo(channelId.first(), channelId.second(), algorithm, factory.getAlgorithmHash(algorithm), state, lock);
          persistence.store(info);
        }
      } catch (final Exception e) {
        log.error(e.getMessage(), e);
      }
    }
  }

  @Override
  public List<String> listAllChannels() throws LoadBalancerPersistenceException {
    if (!isPersistenceEnabled()) return Collections.emptyList();
    return persistence.list(null);
  }

  @Override
  public List<String> listAlgorithms(final String channelID) throws LoadBalancerPersistenceException {
    if (!isPersistenceEnabled()) return Collections.emptyList();
    final List<String> hashes = persistence.list(new LoadBalancerPersistenceInfo(channelID, null, null));
    final List<String> names = hashes.isEmpty() ? Collections.<String>emptyList() : new ArrayList<String>(hashes.size());
    for (final String hash: hashes) names.add(factory.getAlgorithmNameFromHash(hash));
    return names;
  }

  @Override
  public List<String> listAllChannelsWithAlgorithm(final String algorithm) throws LoadBalancerPersistenceException {
    if (!isPersistenceEnabled()) return Collections.emptyList();
    return persistence.list(new LoadBalancerPersistenceInfo(null, null, algorithm, factory.getAlgorithmHash(algorithm), null));
  }

  @Override
  public boolean hasAlgorithm(final String channelID, final String algorithm) throws LoadBalancerPersistenceException {
    return isPersistenceEnabled() && !persistence.list(new LoadBalancerPersistenceInfo(channelID, channelID, algorithm, factory.getAlgorithmHash(algorithm), null)).isEmpty();
  }

  @Override
  public void deleteAll() throws LoadBalancerPersistenceException {
    if (isPersistenceEnabled()) persistence.delete(null);
  }

  @Override
  public void deleteChannel(final String channelID) throws LoadBalancerPersistenceException {
    if (isPersistenceEnabled()) persistence.delete(new LoadBalancerPersistenceInfo(channelID, null, null));
  }

  @Override
  public void deleteAlgorithm(final String algorithm) throws LoadBalancerPersistenceException {
    if (isPersistenceEnabled()) persistence.delete(new LoadBalancerPersistenceInfo(null, null, algorithm, factory.getAlgorithmHash(algorithm), null));
  }

  @Override
  public void delete(final String channelID, final String algorithm) throws LoadBalancerPersistenceException {
    if (!isPersistenceEnabled()) throw new IllegalStateException("load-balancer persistence is not configured");
    persistence.delete(new LoadBalancerPersistenceInfo(channelID, channelID, algorithm, factory.getAlgorithmHash(algorithm), null));
  }

  /**
   * @return the load-balancer factory.
   */
  public JPPFBundlerFactory getFactory() {
    return factory;
  }
}
