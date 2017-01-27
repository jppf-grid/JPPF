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

/**
 * Abstract superclass for custom driver discovery mechanisms in JPPF clients.
 * The discovery is asynchhronus and the {@link #discover()} method of each instance runs in a separate thread.
 * <p>To notify the client that a new driver is discovered, the {@link DriverDiscovery#newConnection(DriverConnectionInfo) newConnection(ClientConnectionPoolInfo)} method must be called.
 * <p>Example:
 * <pre>
 * public class MyDiscovery extends ClientDriverDiscovery {
 *   &#64;Override
 *   public void discover() {
 *     newConnection(new ClientConnectionPoolInfo("driver1", false, "localhost", 11111, 0, 1, 1));
 *   }
 * }
 * </pre>
 * <p>The name given to the connection pool information is used as a prefix for individual connection names, which are numbered starting from 1
 * and have the following format: "<i>pool_name</i>-<i>n</i>", where <i>n</i> is the connection number. In the example above, the individual
 * connections will be named "driver1-1", "driver1-2", etc ...
 * <p>Client driver discovery mechanisms are found in the classpath via the service provider interface (SPI):
 * <ul>
 * <li>create a file named {@code META-INF/services/org.jppf.discovery.ClientDriverDiscovery}</li>
 * <li>in this file, for each discovery implementation add a line with the fully qualified name of the implementation class,
 * for instance: {@code org.jppf.example.MyDiscovery}</li>
 * </ul>
 * @since 5.2.1
 */
public abstract class ClientDriverDiscovery extends DriverDiscovery<ClientConnectionPoolInfo> {
}
