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

import java.io.OutputStream;
import java.util.concurrent.locks.Lock;

import org.jppf.serialization.JPPFSerializationHelper;

/**
 * Instances of this class represent the persisted information for a load-balancer.
 * <p>The channel identifier represents a unique and reusable idenfier for a remote node if running within a driver,
 * or for a remote driver if running within a client.
 * @author Laurent Cohen
 * @since 6.0
 */
public class LoadBalancerPersistenceInfo {
  /**
   * A constant which specifies the "all nodes" scope for list and delete operations.
   */
  private static final byte[] SERIALIZED_NULL = initNullBytes();
  /**
   * Channel identifier in readable form.
   */
  private final String channelString;
  /**
   * Unique identifier for the related node, this is a hash of {@link #channelString}.
   */
  private final String channelID;
  /**
   * The load balancing algorithm name in clear.
   */
  private final String algorithm;
  /**
   * The load balancing portable identifier, computed a s a hash of {@link #algorithm}.
   */
  private final String algorithmID;
  /**
   * The state of the load-balancer.
   */
  private final Object state;
  /**
   * Lock used to synchronize access to the load-balancer state.
   */
  private final Lock lock;

  /**
   * Initialize this persistence information.
   * @param channelID the unique identifier for the related node or driver.
   * @param algorithmID the load balancing algorithm name.
   * @param state the state of the load-balancer.
   * @exclude
   */
  public LoadBalancerPersistenceInfo(final String channelID, final String algorithmID, final Object state) {
    this(channelID, channelID, algorithmID, algorithmID, state);
  }

  /**
   * Initialize this persistence information.
   * @param channelString the unique identifier for the related channel in readable text.
   * @param channelID the unique identifier for the related channel (hash of {@code channelString}).
   * @param algorithm the load balancing algorithm name.
   * @param algorithmID a hash of the load balancing algorithm name.
   * @param state the state of the load-balancer.
   * @exclude
   */
  public LoadBalancerPersistenceInfo(final String channelString, final String channelID, final String algorithm, final String algorithmID, final Object state) {
    this(channelString, channelID, algorithm, algorithmID, state, null);
  }

  /**
   * Initialize this persistence information.
   * @param channelString the unique identifier for the related channel in readable text.
   * @param channelID the unique identifier for the related channel (hash of {@code channelString}).
   * @param algorithm the load balancing algorithm name.
   * @param algorithmID a hash of the load balancing algorithm name.
   * @param state the state of the load-balancer.
   * @param lock lock used to synchronize access to the load-balancer state.
   * @exclude
   */
  public LoadBalancerPersistenceInfo(final String channelString, final String channelID, final String algorithm, final String algorithmID, final Object state, final Lock lock) {
    this.channelString = channelString;
    this.channelID = channelID;
    this.algorithm = algorithm;
    this.algorithmID = algorithmID;
    this.state = state;
    this.lock = lock;
  }

  /**
   * Get a unique identifier for the channel, reusable over restarts of the remote process (driver or node).
   * <br>This method is provided for debugging and logging purposes only, as the returned string is not used by the persistence mechanism.
   * @return a strign that uniquely identifies the channel.
   */
  public String getChannelString() {
    return channelString;
  }

  /**
   * Get the channel identifier, wich is a hash of the string returned by {@link #getChannelString()}.
   * @return the unique identifier for the channel.
   */
  public String getChannelID() {
    return channelID;
  }

  /**
   * Get the name of the related load-balancing algorithm.
   * <br>This method is provided for debugging and logging purposes only, as the returned string is not used by the persistence mechanism.
   * @return the load balancing algorithm name in clear.
   */
  public String getAlgorithm() {
    return algorithm;
  }

  /**
   * Get a hash of the load-balancing algorithm name (obtained via {@link #getAlgorithm()}).
   * @return algorithm a hash of the load balancing algorithm name.
   */
  public String getAlgorithmID() {
    return algorithmID;
  }

  /**
   * Get the load-balancer state.
   * @return the state of the load-balancer.
   */
  public Object getState() {
    return state;
  }

  /**
   * Get the lock used to synchronize access to the load-balancer state.
   * @return a {@link Lock} object.
   */
  public Lock getStateLock() {
    return lock;
  }

  /**
   * Get the serialized state of the load-balancer as an array of bytes.
   * This method synchronizes internally on the state object using the lock obtained by calling {@link #getStateLock()}.
   * @return the serialized state of the load-balancer as an array of bytes.
   * @throws Exception if any error occurs.
   */
  public byte[] getStateAsBytes() throws Exception {
    if (state == null) return SERIALIZED_NULL;
    lock.lock();
    try {
      return JPPFSerializationHelper.serializeToBytes(state);
    } finally {
      lock.unlock();
    }
  }

  /**
   * COnvenience method to serialize the load-balancer state into the specified output stream.
   * This method synchronizes internally on the state object using the lock obtained by calling {@link #getStateLock()}.
   * @param stream the stream to serialize into.
   * @throws Exception if any error occurs.
   */
  public void serializeToStream(final OutputStream stream) throws Exception {
    lock.lock();
    try {
      JPPFSerializationHelper.serialize(state, stream);
    } finally {
      lock.unlock();
    }
  }

  @Override
  public String toString() {
    return new StringBuilder(getClass().getSimpleName()).append('[')
      .append("nodeString=").append(channelString)
      .append(", nodeID=").append(channelID)
      .append(", algorithm=").append(algorithm)
      .append(", algorithmID=").append(algorithmID)
      .append(", state=").append(state)
      .append(']').toString();
  }

  /**
   * Initialize a serialized null value.
   * @return an array of bytes containing the seiralized form of {@code null}.
   */
  private static byte[] initNullBytes() {
    try {
      return JPPFSerializationHelper.serializeToBytes(null);
    } catch(Exception e) {
      throw new ExceptionInInitializerError(e);
    }
  }
}
