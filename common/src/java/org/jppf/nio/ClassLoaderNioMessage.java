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

import org.jppf.classloader.JPPFResourceWrapper;

/**
 * Nio message that reads or writes a single object from/to the network.
 * @author Laurent Cohen
 */
public class ClassLoaderNioMessage extends AbstractNioMessage {
  /**
   * The associated class loader resource.
   */
  private JPPFResourceWrapper resource;

  /**
   * Initialize this nio message with the specified sll flag.
   * @param channel the channel to read from or write to.
   */
  public ClassLoaderNioMessage(final NioContext<?> channel) {
    super(channel);
  }

  /**
   * Initialize this nio message with the specified sll flag.
   * @param channel the channel to read from or write to.
   * @param resource the associated class loader resource.
   */
  public ClassLoaderNioMessage(final NioContext<?> channel, final JPPFResourceWrapper resource) {
    super(channel);
    this.resource = resource;
  }

  /**
   * Initialize this nio message with the specified sll flag.
   * @param channel the channel to read from or write to.
   */
  public ClassLoaderNioMessage(final ChannelWrapper<?> channel) {
    super(channel);
  }

  /**
   * Actions to take after the first object in the message has been fully read.
   * @throws Exception if an IO error occurs.
   */
  @Override
  protected void afterFirstRead() throws Exception {
    nbObjects = 1;
  }

  /**
   * Actions to take before the first object in the message is written.
   * @throws Exception if an IO error occurs.
   */
  @Override
  protected void beforeFirstWrite() throws Exception {
    nbObjects = 1;
  }

  /**
   * @return the associated class loader resource.
   */
  public JPPFResourceWrapper getResource() {
    return resource;
  }
}
