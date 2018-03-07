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

package org.jppf.jmxremote.nio;

import org.jppf.jmxremote.message.JMXMessage;
import org.jppf.nio.NioMessage;

/**
 * Instances of this class associate a {@link JMXMessage} to its serialized form (a {@link NioMessage}, which holds a
 * {@link org.jppf.io.DataLocation DataLocation}}). This allows the serialization to happen in a thread different from
 * the I/O thread, while keeping meaningful information on the JMX message that can be used after the message is sent
 * over the network channel.
 * @author Laurent Cohen
 */
class MessageWrapper {
  /**
   * The JMX message.
   */
  final JMXMessage jmxMessage;
  /**
   * The {@code NioMessage} that holds the serialized JMX message.
   */
  final NioMessage nioMessage;

  /**
   * Initialize with the specified JMX message and associated serialized form.
   * @param jmxMessage the JMX message.
   * @param nioMessage the {@code NioMessage} that holds the serialized JMX message.
   */
  MessageWrapper(final JMXMessage jmxMessage, final NioMessage nioMessage) {
    this.jmxMessage = jmxMessage;
    this.nioMessage = nioMessage;
  }

  @Override
  public String toString() {
    return new StringBuilder(getClass().getSimpleName()).append('[').append("jmxMessage=").append(jmxMessage).append(']').toString();
  }
}
