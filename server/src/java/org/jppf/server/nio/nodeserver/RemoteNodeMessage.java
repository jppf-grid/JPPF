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

package org.jppf.server.nio.nodeserver;

import org.jppf.io.IOHelper;
import org.jppf.server.nio.AbstractTaskBundleMessage;
import org.jppf.server.protocol.JPPFTaskBundle;
import org.jppf.utils.SerializationHelperImpl;

/**
 * Representation of a message sent or received by a remote node.
 * @author Laurent Cohen
 */
public class RemoteNodeMessage extends AbstractTaskBundleMessage
{
  /**
   * Initialize this nio message with the specified sll flag.
   * @param ssl <code>true</code> is data is read from or written an SSL connection, <code>false</code> otherwise.
   */
  public RemoteNodeMessage(final boolean ssl)
  {
    super(ssl);
  }

  /**
   * Actions to take after the first object in the message has been fully read.
   * @throws Exception if an IO error occurs.
   */
  @Override
  protected void afterFirstRead() throws Exception
  {
    bundle = (JPPFTaskBundle) IOHelper.unwrappedData(locations.get(0), new SerializationHelperImpl().getSerializer());
    nbObjects = bundle.getTaskCount() + 1;
  }

  /**
   * Actions to take before the first object in the message is written.
   * @throws Exception if an IO error occurs.
   */
  @Override
  protected void beforeFirstWrite() throws Exception
  {
    nbObjects = bundle.getTaskCount() + 2;
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
    sb.append(", currentLength=").append(currentLength);
    sb.append(']');
    return sb.toString();
  }
}
