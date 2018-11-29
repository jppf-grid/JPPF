/*
 * JPPF.
 * Copyright (C) 2005-2018 JPPF Team.
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

package org.jppf.server.nio.nodeserver;

import org.jppf.server.nio.AbstractTaskBundleMessage;

/**
 * Context associated with a channel serving tasks to a node.
 * @author Laurent Cohen
 */
public class RemoteNodeContext extends AbstractNodeContext {
  /**
   * Determines whether the node works in offline mode.
   */
  private boolean offline = false;

  /**
   * Default constructor.
   * @param server the NIO server that created this context.
   */
  public RemoteNodeContext(final NodeNioServer server) {
    super(server);
  }

  @Override
  public AbstractTaskBundleMessage newMessage() {
    return new RemoteNodeMessage(getChannel());
  }

  @Override
  public boolean isLocal() {
    return false;
  }

  @Override
  public boolean isOffline() {
    return offline;
  }

  /**
   * Specify whether the node works in offline mode.
   * @param offline <code>true</code> if the node is in offline mode, <code>false</code> otherwise.
   */
  public void setOffline(final boolean offline) {
    this.offline = offline;
  }
}
