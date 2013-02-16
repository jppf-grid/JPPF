/*
 * JPPF.
 * Copyright (C) 2005-2013 JPPF Team.
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

package org.jppf.server.node.local;

import org.jppf.node.AbstractNodeConnection;
import org.jppf.server.nio.nodeserver.LocalNodeChannel;
import org.slf4j.*;

/**
 * 
 * @author Laurent Cohen
 */
public class LocalNodeConnection extends AbstractNodeConnection<LocalNodeChannel>
{
  /**
   * Logger for this class.
   */
  private static Logger log = LoggerFactory.getLogger(LocalNodeConnection.class);
  /**
   * Determines whether the debug level is enabled in the log configuration, without the cost of a method call.
   */
  private static boolean debugEnabled = log.isDebugEnabled();

  /**
   * Initialize this connection with the specified serializer.
   * @param channel the communicationchannel to use.
   */
  public LocalNodeConnection(final LocalNodeChannel channel)
  {
    this.channel = channel;
  }

  @Override
  public void init() throws Exception
  {
  }

  @Override
  public void close() throws Exception
  {
  }
}
