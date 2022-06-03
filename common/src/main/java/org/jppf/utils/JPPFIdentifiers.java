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

import java.util.*;

import org.jppf.nio.NioConstants;

/**
 * Channel identifiers sent over the network as part of the handshaking with a JPPF server.
 * @author Laurent Cohen
 * @exclude
 */
public final class JPPFIdentifiers {
  /**
   * Identifier for an unidentified channel.
   */
  public static final int UNKNOWN = 0;
  /**
   * Identifier for a driver-tot-driver heartbeat connection.
   */
  public static final int PEER_HEARTBEAT_CHANNEL = 0xFFF5;
  /**
   * Identifier for a client-driver heartbeat connection.
   */
  public static final int CLIENT_HEARTBEAT_CHANNEL = 0xFFF6;
  /**
   * Identifier for a nodet-driver heartbeat connection.
   */
  public static final int NODE_HEARTBEAT_CHANNEL = 0xFFF7;
  /**
   * Identifier for a JMX remote channel.
   */
  public static final int JMX_REMOTE_CHANNEL = 0xFFF8;
  /**
   * Identifier for an acceptor channel.
   */
  public static final int ACCEPTOR_CHANNEL = 0xFFF9;
  /**
   * Identifier for the job data channel of a client.
   */
  public static final int CLIENT_JOB_DATA_CHANNEL = 0xFFFA;
  /**
   * Identifier for the class loader channel of a client.
   */
  public static final int CLIENT_CLASSLOADER_CHANNEL = 0xFFFB;
  /**
   * Identifier for the job data channel of a node.
   */
  public static final int NODE_JOB_DATA_CHANNEL = 0xFFFC;
  /**
   * Identifier for the class loader channel of a node.
   */
  public static final int NODE_CLASSLOADER_CHANNEL = 0xFFFD;
  /**
   * Mapping of ids to readable names.
   */
  private static final Map<Integer, String> idMap = new HashMap<>();
  static {
    idMap.put(ACCEPTOR_CHANNEL, "ACCEPTOR_CHANNEL");
    idMap.put(CLIENT_JOB_DATA_CHANNEL, "CLIENT_JOB_DATA_CHANNEL");
    idMap.put(CLIENT_CLASSLOADER_CHANNEL, "CLIENT_CLASSLOADER_CHANNEL");
    idMap.put(NODE_JOB_DATA_CHANNEL, "NODE_JOB_DATA_CHANNEL");
    idMap.put(NODE_CLASSLOADER_CHANNEL, "NODE_CLASSLOADER_CHANNEL");
    idMap.put(JMX_REMOTE_CHANNEL, "JMX_REMOTE_CHANNEL");
    idMap.put(NODE_HEARTBEAT_CHANNEL, "NODE_HEARTBEAT_CHANNEL");
    idMap.put(CLIENT_HEARTBEAT_CHANNEL, "CLIENT_HEARTBEAT_CHANNEL");
  }
  /**
   * Mapping of ids to server names.
   */
  private static final Map<Integer, String> serverMap = new HashMap<>();
  static {
    serverMap.put(ACCEPTOR_CHANNEL, NioConstants.ACCEPTOR);
    serverMap.put(CLIENT_JOB_DATA_CHANNEL, NioConstants.CLIENT_SERVER);
    serverMap.put(CLIENT_CLASSLOADER_CHANNEL, NioConstants.CLIENT_CLASS_SERVER);
    serverMap.put(NODE_JOB_DATA_CHANNEL, NioConstants.NODE_SERVER);
    serverMap.put(NODE_CLASSLOADER_CHANNEL, NioConstants.NODE_CLASS_SERVER);
    serverMap.put(JMX_REMOTE_CHANNEL, NioConstants.JMX_REMOTE);
    serverMap.put(NODE_HEARTBEAT_CHANNEL, "NodeHeartBeatServer");
    serverMap.put(CLIENT_HEARTBEAT_CHANNEL, "ClientHeartBeatServer");
  }
  /**
   * Mapping of ids to channel descriptors.
   */
  private static final Map<Integer, JPPFChannelDescriptor> decsriptorMap = new HashMap<>();
  static {
    decsriptorMap.put(PEER_HEARTBEAT_CHANNEL, JPPFChannelDescriptor.PEER_HEARTBEAT_CHANNEL);
    decsriptorMap.put(CLIENT_HEARTBEAT_CHANNEL, JPPFChannelDescriptor.CLIENT_HEARTBEAT_CHANNEL);
    decsriptorMap.put(NODE_HEARTBEAT_CHANNEL, JPPFChannelDescriptor.NODE_HEARTBEAT_CHANNEL);
    decsriptorMap.put(JMX_REMOTE_CHANNEL, JPPFChannelDescriptor.JMX_REMOTE_CHANNEL);
    decsriptorMap.put(ACCEPTOR_CHANNEL, JPPFChannelDescriptor.ACCEPTOR_CHANNEL);
    decsriptorMap.put(CLIENT_JOB_DATA_CHANNEL, JPPFChannelDescriptor.CLIENT_JOB_DATA_CHANNEL);
    decsriptorMap.put(CLIENT_CLASSLOADER_CHANNEL, JPPFChannelDescriptor.CLIENT_CLASSLOADER_CHANNEL);
    decsriptorMap.put(NODE_JOB_DATA_CHANNEL, JPPFChannelDescriptor.NODE_JOB_DATA_CHANNEL);
    decsriptorMap.put(NODE_CLASSLOADER_CHANNEL, JPPFChannelDescriptor.NODE_CLASSLOADER_CHANNEL);
    decsriptorMap.put(UNKNOWN, JPPFChannelDescriptor.UNKNOWN);
  }

  /**
   * Get an identifier as a string.
   * @param id the id to lookup.
   * @return a readable string for the id.
   */
  public static String asString(final int id) {
    final String s = idMap.get(id);
    return s == null ? "UNKNOWN" : s;
  }

  /**
   * Get a server name for the psecified identifier.
   * @param id the id to lookup.
   * @return a readable string for the server name.
   */
  public static String serverName(final int id) {
    final String s = serverMap.get(id);
    return s == null ? "UNKNOWN" : s;
  }

  /**
   * Retrieve the JPPF channel descriptor for the specified channel identifier. 
   * @param identifier the channel identifier.
   * @return a corresponding {@code JPPFChannelDescriptor} instance.
   */
  public static JPPFChannelDescriptor descriptorFor(final int identifier) {
    final JPPFChannelDescriptor desc = decsriptorMap.get(identifier);
    return (desc == null) ? JPPFChannelDescriptor.UNKNOWN : desc;
  }
}
