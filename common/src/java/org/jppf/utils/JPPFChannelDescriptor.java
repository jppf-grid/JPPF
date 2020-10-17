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

package org.jppf.utils;

/**
 *
 * @author Laurent Cohen
 * @since 6.3
 */
public enum JPPFChannelDescriptor {
  /**
   * Identifier for an unidentified channel.
   */
  UNKNOWN(false, false, false, false, false, false, false),
  /**
   * Identifier for a driver-tot-driver heartbeat connection.
   */
  PEER_HEARTBEAT_CHANNEL(false, false, true, false, false, false, true),
  /**
   * Identifier for a client-driver heartbeat connection.
   */
  CLIENT_HEARTBEAT_CHANNEL(false, false, true, false, true, false, false),
  /**
   * Identifier for a nodet-driver heartbeat connection.
   */
  NODE_HEARTBEAT_CHANNEL(false, false, true, false, false, true, false),
  /**
   * Identifier for a JMX remote channel.
   */
  JMX_REMOTE_CHANNEL(false, false, false, true, false, false, false),
  /**
   * Identifier for an acceptor channel.
   */
  ACCEPTOR_CHANNEL(false, false, false, false, false, false, true),
  /**
   * Identifier for the job data channel of a client.
   */
  CLIENT_JOB_DATA_CHANNEL(false, true, false, false, true, false, false),
  /**
   * Identifier for the class loader channel of a client.
   */
  CLIENT_CLASSLOADER_CHANNEL(true, false, false, false, true, false, false),
  /**
   * Identifier for the job data channel of a node.
   */
  NODE_JOB_DATA_CHANNEL(false, true, false, false, false, true, false),
  /**
   * Identifier for the class loader channel of a node.
   */
  NODE_CLASSLOADER_CHANNEL(true, false, false, false, false, true, false);

  /**
   * Whether this is a classloader channel.
   */
  private final boolean classloader;
  /**
   * Whether this is a job data channel.
   */
  private final boolean jobData;
  /**
   * Whether this is a heartbeat channel.
   */
  private final boolean heartbeat;
  /**
   * Whether this is a JMX channel.
   */
  private final boolean jmx;
  /**
   * Whether this is a client-side channel.
   */
  private final boolean client;
  /**
   * Whether this is a node-side channel.
   */
  private final boolean node;
  /**
   * Whether this is a driver-side channel.
   */
  private final boolean driver;

  /**
   * 
   * @param classloader whether this is a classloader channel.
   * @param jobData whether this is a job data channel.
   * @param heartbeat whether this is a heartbeat channel.
   * @param jmx whether this is a JMX channel.
   * @param client whether this is a client-side channel.
   * @param node whether this is a node-side channel.
   * @param driver whether this is a driver-side data channel.
   */
  private JPPFChannelDescriptor(final boolean classloader, final boolean jobData, final boolean heartbeat, final boolean jmx,
    final boolean client, final boolean node, final boolean driver) {
    this.classloader = classloader;
    this.jobData = jobData;
    this.heartbeat = heartbeat;
    this.jmx = jmx;
    this.client = client;
    this.node = node;
    this.driver = driver;
  }

  /**
   * @return {@code true} if this is a class loader channel, {@code false} otherwise. 
   */
  public boolean isClassloader() {
    return classloader;
  }

  /**
   * @return {@code true} if this is a job data channel, {@code false} otherwise. 
   */
  public boolean isJobData() {
    return jobData;
  }

  /**
   * @return {@code true} if this is a heartbeat channel, {@code false} otherwise. 
   */
  public boolean isHeartbeat() {
    return heartbeat;
  }

  /**
   * @return {@code true} if this is a class loader channel, {@code false} otherwise. 
   */
  public boolean isJmx() {
    return jmx;
  }

  /**
   * @return {@code true} if this is a client-side channel, {@code false} otherwise. 
   */
  public boolean isClient() {
    return client;
  }

  /**
   * @return {@code true} if this is a node-side channel, {@code false} otherwise. 
   */
  public boolean isNode() {
    return node;
  }

  /**
   * @return {@code true} if this is a driver-side channel, {@code false} otherwise. 
   */
  public boolean isDriver() {
    return driver;
  }
}
