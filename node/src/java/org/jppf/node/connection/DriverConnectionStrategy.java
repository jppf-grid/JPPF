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

package org.jppf.node.connection;

/**
 * This interface defines which parameters should be used to connect to the driver.
 * <p>It gives the ability to create new sets of parameters whenever a connection attempt was unsuccessful,
 * therefore providing the nodes with a failover strategy when the connection to a driver cannot be established.
 * @author Laurent Cohen
 * @since 4.1
 */
public interface DriverConnectionStrategy {
  /**
   * Get a new connection information, eventually based on the one that was previously used.
   * @param currentInfo the {@link DriverConnectionInfo} that was previously used to connecto the driver,
   * or {@code null} if the node is connecting for the first time. 
   * @param context provides information on why a new connection is requested, so as to help deciding which connection information to provide.
   * @return a new {@link DriverConnectionInfo} object that the node will use to connect to the driver.
   */
  DriverConnectionInfo nextConnectionInfo(DriverConnectionInfo currentInfo, ConnectionContext context);
}
