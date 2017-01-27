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

package org.jppf.server.nio.nodeserver;

import org.jppf.nio.AbstractLocalChannelWrapper;

/**
 * Wrapper implementation for a local node's communication channel.
 * @author Laurent Cohen
 */
public class LocalNodeChannel extends AbstractLocalChannelWrapper<LocalNodeMessage, LocalNodeContext> {
  /**
   * Initialize this channel wrapper with the specified node context.
   * @param context the node context used as channel.
   */
  public LocalNodeChannel(final LocalNodeContext context) {
    super(context);
  }
}
