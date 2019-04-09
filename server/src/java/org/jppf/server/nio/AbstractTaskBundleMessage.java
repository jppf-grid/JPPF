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

package org.jppf.server.nio;

import org.jppf.nio.*;
import org.jppf.node.protocol.TaskBundle;

/**
 * Common abstract superclass representing a message sent or received by a node.
 * A message is the transformation of a job into an more easily transportable format.
 * @author Laurent Cohen
 */
public abstract class AbstractTaskBundleMessage extends AbstractNioMessage {
  /**
   * The latest bundle that was sent or received.
   */
  protected TaskBundle bundle;

  /**
   * Initialize this nio message with the specified context.
   * @param context the context to read from or write to.
   */
  public AbstractTaskBundleMessage(final NioContext<?> context) {
    super(context);
  }

  /**
   * Get the latest bundle that was sent or received.
   * @return a <code>JPPFTaskBundle</code> instance.
   */
  public TaskBundle getBundle() {
    return bundle;
  }

  /**
   * Set the latest bundle that was sent or received.
   * @param bundle a <code>JPPFTaskBundle</code> instance.
   */
  public void setBundle(final TaskBundle bundle) {
    this.bundle = bundle;
  }

  @Override
  public String toString() {
    final StringBuilder sb = new StringBuilder(getClass().getSimpleName()).append('[');
    sb.append("nb locations=").append(locations == null ? -1 : locations.size());
    sb.append(", position=").append(position);
    sb.append(", nbObjects=").append(nbObjects);
    sb.append(", count=").append(count);
    sb.append(", currentLength=").append(currentLength);
    sb.append(", bundle=").append(bundle);
    sb.append(']');
    return sb.toString();
  }
}
