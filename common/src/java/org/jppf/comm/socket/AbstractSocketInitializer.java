/*
 * JPPF.
 * Copyright (C) 2005-2015 JPPF Team.
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

package org.jppf.comm.socket;

import java.util.Random;

import org.jppf.utils.ThreadSynchronization;

/**
 * Common abstract superclass for objects that establish a connection with a remote socket.
 * @author Laurent Cohen
 */
public abstract class AbstractSocketInitializer extends ThreadSynchronization implements SocketInitializer
{
  /**
   * Determines whether any connection attempt succeeded.
   */
  protected boolean successful = false;
  /**
   * Used to compute a random start delay for this node.
   */
  protected Random rand = new Random(System.nanoTime());
  /**
   * Determine whether this socket initializer has been intentionally closed.
   */
  protected boolean closed = false;
  /**
   * Name given to this initializer.
   */
  protected String name = "";

  /**
   * Determine whether this socket initializer has been intentionally closed.
   * @return true if this socket initializer has been intentionally closed, false otherwise.
   */
  @Override
  public boolean isClosed()
  {
    return closed;
  }

  /**
   * Determine whether any connection attempt succeeded.
   * @return true if any attempt was successful, false otherwise.
   */
  @Override
  public boolean isSuccessful()
  {
    return successful;
  }
}
