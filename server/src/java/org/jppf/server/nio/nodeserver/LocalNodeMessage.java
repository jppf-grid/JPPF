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

import org.jppf.io.IOHelper;
import org.jppf.nio.ChannelWrapper;
import org.jppf.node.protocol.TaskBundle;
import org.jppf.server.JPPFDriver;
import org.jppf.server.nio.AbstractTaskBundleMessage;

/**
 * Node message implementation for an in-VM node.
 * @author Laurent Cohen
 */
public class LocalNodeMessage extends AbstractTaskBundleMessage
{
  /**
   * Build this nio message.
   * @param channel the channel to read from or write to.
   */
  public LocalNodeMessage(final ChannelWrapper<?> channel)
  {
    super(channel);
  }

  @Override
  public boolean read() throws Exception
  {
    bundle = (TaskBundle) IOHelper.unwrappedData(locations.get(0), JPPFDriver.getSerializer());
    return true;
  }

  @Override
  protected synchronized boolean readNextObject() throws Exception
  {
    return true;
  }

  @Override
  public boolean write() throws Exception
  {
    return true;
  }

  @Override
  protected boolean writeNextObject() throws Exception
  {
    return true;
  }
}
