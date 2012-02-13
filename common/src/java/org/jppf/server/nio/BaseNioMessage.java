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


/**
 * Nio message that reads or writes a single object from/to the network.
 * @author Laurent Cohen
 */
public class BaseNioMessage extends AbstractNioMessage
{
  /**
   * Initialize this nio message with the specified sll flag.
   * @param ssl <code>true</code> is data is read from or written an SSL connection, <code>false</code> otherwise.
   */
  public BaseNioMessage(final boolean ssl)
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
    nbObjects = 1;
  }

  /**
   * Actions to take before the first object in the message is written.
   * @throws Exception if an IO error occurs.
   */
  @Override
  protected void beforeFirstWrite() throws Exception
  {
    nbObjects = 1;
  }
}
