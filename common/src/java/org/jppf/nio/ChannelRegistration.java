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

package org.jppf.nio;

import java.nio.channels.*;

/**
 * @param <E> the type of attachments.
 * @author Laurent Cohen
 */
public class ChannelRegistration<E> {
  /**
   * 
   */
  public final SocketChannel channel;
  /**
   * 
   */
  public final int interestOps;
  /**
   * 
   */
  public final E attachment;
  /**
   * 
   */
  public SelectionKey key;

  /**
   * 
   * @param channel the associated socket channel.
   * @param interestOps the ops ot register for.
   * @param attachment contextuqlm stqtefulm infor;qtion qbout the chqnnel.
   * @throws Exception if any error occurs.
   */
  public ChannelRegistration(final SocketChannel channel, final int interestOps, final E attachment) throws Exception {
    this.channel = channel;
    this.interestOps = interestOps;
    this.attachment = attachment;
  }
}
