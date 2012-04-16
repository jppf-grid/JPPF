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

package org.jppf.server.nio;

import org.jppf.server.protocol.JPPFTaskBundle;

/**
 * Common abstract superclass representing a message sent or received by a node.
 * A message is the transformation of a job into an more easily transportable format.
 * @author Laurent Cohen
 */
public abstract class AbstractTaskBundleMessage extends AbstractNioMessage
{
  /**
   * The latest bundle that was sent or received.
   */
  protected JPPFTaskBundle bundle = null;

  /**
   * Initialize this nio message with the specified sll flag.
   * @param ssl <code>true</code> is data is read from or wirtten an SSL connection, <code>false</code> otherwise.
   */
  public AbstractTaskBundleMessage(final boolean ssl)
  {
    super(ssl);
  }

  /**
   * Get the latest bundle that was sent or received.
   * @return a <code>JPPFTaskBundle</code> instance.
   */
  public JPPFTaskBundle getBundle()
  {
    return bundle;
  }

  /**
   * Set the latest bundle that was sent or received.
   * @param bundle a <code>JPPFTaskBundle</code> instance.
   */
  public void setBundle(final JPPFTaskBundle bundle)
  {
    this.bundle = bundle;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public String toString()
  {
    StringBuilder sb = new StringBuilder(getClass().getSimpleName()).append('[');
    sb.append("nb locations=").append(locations == null ? -1 : locations.size());
    sb.append(", position=").append(position);
    sb.append(", nbObjects=").append(nbObjects);
    sb.append(", length=").append(length);
    sb.append(", count=").append(count);
    sb.append(", bundle=").append(bundle);
    sb.append(']');
    return sb.toString();
  }
}
