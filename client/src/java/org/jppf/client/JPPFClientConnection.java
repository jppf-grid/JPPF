/*
 * JPPF.
 * Copyright (C) 2005-2013 JPPF Team.
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

package org.jppf.client;

import java.util.List;

import org.jppf.client.event.ClientConnectionStatusHandler;
import org.jppf.management.*;

/**
 * Interface for a client connection to a JPPF driver.
 * @author Laurent Cohen
 */
public interface JPPFClientConnection extends ClientConnectionStatusHandler
{
  /**
   * Initialize this client connection.
   */
  void init();

  /**
   * Get the priority assigned to this connection.
   * @return a priority as an int value.
   */
  int getPriority();

  /**
   * Shutdown this client and retrieve all pending executions for resubmission.
   * @return a list of <code>JPPFJob</code> instances to resubmit.
   */
  List<JPPFJob> close();

  /**
   * Determine whether this connection was closed.
   * @return <code>true</code> if the connection is closed, <code>false</code> otherwise.
   */
  boolean isClosed();

  /**
   * Get the name assigned to this client connection.
   * @return the name as a string.
   */
  String getName();

  /**
   * Determines if this connection is over SSL.
   * @return <code>true</code> if this is an SSL connection, <code>false</code> otherwise.
   */
  boolean isSSLEnabled();
  
  /**
   * Get the driver's host name or ip address.
   * @return the host as a stirng.
   */
  String getHost();

  /**
   * Get the port number on which the dirver is listeneing for connections.
   * @return the port number as an int.
   */
  int getPort();

  /**
   * Get the unique identifier of the remote driver.
   * @return the uuid as a string.
   */
  String getDriverUuid();

  /**
   * Get the object that provides access to the management functions of the driver.
   * @return a <code>JMXConnectionWrapper</code> instance.
   */
  JMXDriverConnectionWrapper getJmxConnection();

  /**
   * Get the system information.
   * @return a {@link JPPFSystemInformation} instance.
   */
  JPPFSystemInformation getSystemInfo();

  /**
   * Get the unique ID for this connection and its two channels.
   * @return the id as a string.
   */
  String getConnectionUuid();
}
