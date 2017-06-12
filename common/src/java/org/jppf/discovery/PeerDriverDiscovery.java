/*
 * JPPF.
 * Copyright (C) 2005-2016 JPPF Team.
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
 * Abstract superclass for custom peer driver discovery mechanisms in JPPF drivers.
 * The discovery is asynchhronus and the {@link #discover()} method of each instance runs in a separate thread.
 * <p>To notify the driver that a new peer driver is discovered, the {@link DriverDiscovery#newConnection(DriverConnectionInfo) newConnection(DriverConnectionInfo)} method must be called.
 * <p>Example:
 * <pre>
 * public class MyPeerDiscovery extends PeerDriverDiscovery {
 *   &#64;Override
 *   public void discover() {
 *     newConnection(new DriverConnectionInfo("driver1", false, "localhost", 11111));
 *   }
 * }
 * </pre>
 * <p>Peer driver discovery mechanisms are found in the classpath via the service provider interface (SPI):
 * <ul>
 * <li>create a file named {@code META-INF/services/org.jppf.discovery.PeerDriverDiscovery}</li>
 * <li>in this file, for each discovery implementation add a line with the fully qualified name of the implementation class,
 * for instance: {@code org.jppf.example.MyPeerDiscovery}</li>
 * </ul>
 * @author Laurent Cohen
 */
public abstract class PeerDriverDiscovery extends DriverDiscovery<DriverConnectionInfo> {

}
