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

package org.jppf.server.node;

/**
 * A connection checker is run asynchronusly while tasks are exected in a node and checks if the communication channel is avaialble.
 * When it is no more valid, this checker propagates the resulting exception to the node for proper handling.
 * @author Laurent Cohen
 * @exclude
 */
public interface NodeConnectionChecker
{
  /**
   * Start this checker.
   */
  void start();

  /**
   * Stop this checker.
   */
  void stop();

  /**
   * Determine whether this checker is stopped.
   * @return <code>true</code> if this checker is stopped, <code>false</code> otherwise.
   */
  boolean isStopped();

  /**
   * Resume this checker.
   */
  void resume();

  /**
   * Suspend this checker.
   */
  void suspend();

  /**
   * Determine whether this checker is suspended.
   * @return <code>true</code> if this checker is suspended, <code>false</code> otherwise.
   */
  boolean isSuspended();
  /**
   * Get an eventual exception resulting from the check.
   * @return an {@link Exception} if one occurred during the check, or <code>null</code> if none was raised.
   */
  Exception getException();
}
