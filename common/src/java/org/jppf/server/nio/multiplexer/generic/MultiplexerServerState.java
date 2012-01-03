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

package org.jppf.server.nio.multiplexer.generic;

import org.jppf.server.nio.NioState;

/**
 * Common abstract superclass for all states of a multiplexer connection.
 * @author Laurent Cohen
 */
public abstract class MultiplexerServerState extends NioState<MultiplexerTransition>
{
  /**
   * The server that handles this state.
   */
  protected MultiplexerNioServer server = null;

  /**
   * Initialize this state.
   * @param server the server that handles this state.
   */
  public MultiplexerServerState(final MultiplexerNioServer server)
  {
    this.server = server;
  }
}
