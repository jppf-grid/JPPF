/*
 * JPPF.
 * Copyright (C) 2005-2012 JPPF Team.
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

package org.jppf.node;


/**
 * Instances of this class represent the connection between a node's class loader and the driver.
 * @param <C> the type of communication channel used by this connection.
 * @author Laurent Cohen
 */
public interface NodeConnection<C>
{
  /**
   * Initialize this connection.
   * @throws Exception if any error occurs.
   */
  void init() throws Exception;

  /**
   * Reset this connection.
   * @throws Exception if any error occurs.
   */
  void reset() throws Exception;

  /**
   * Close this connection.
   * @throws Exception if any error occurs.
   */
  void close() throws Exception;

  /**
   * Get the communication channel for this connection.
   * @return an object trpesenting the channel.
   */
  C getChannel();
}

