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

package org.jppf.client;


/**
 * Interface for all class server clients.
 * @author Laurent Cohen
 */
interface ClassServerDelegate extends Runnable, ClientConnectionHandler
{
  /**
   * Determine whether the socket connection is closed
   * @return true if the socket connection is closed, false otherwise
   */
  @Override
  boolean isClosed();

  /**
   * Close the socket connection.
   */
  @Override
  void close();

  /**
   * Get the name of this delegate.
   * @return the name as a string.
   */
  String getName();

  /**
   * Set the name of this delegate.
   * @param name the name as a string.
   */
  void setName(String name);
}
