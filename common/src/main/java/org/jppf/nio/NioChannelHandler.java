/*
 * JPPF.
 * Copyright (C) 2005-2019 JPPF Team.
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

package org.jppf.nio;

import java.nio.channels.SelectionKey;

/**
 * 
 * @author Laurent Cohen
 */
public interface NioChannelHandler {
  /**
   * @return the socket channel's interest ops.
   */
  int getInterestOps();

  /**
   * Set the socket channel's interest ops.
   * @param interestOps the interest ops to set.
   */
  void setInterestOps(final int interestOps);

  /**
   * @return the associated selection key.
   */
  SelectionKey getSelectionKey();

  /**
   * Set the associated selection key.
   * @param selectionKey the key to set.
   */
  void setSelectionKey(final SelectionKey selectionKey);
}
