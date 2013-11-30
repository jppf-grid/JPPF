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

package org.jppf.nio;

import org.jppf.utils.ThreadSynchronization;

/**
 * Instances of this class act as a NIO selector for local (in-VM) channels.
 * @author Laurent Cohen
 */
public class LocalChannelSelector extends ThreadSynchronization implements ChannelSelector
{
  /**
   * The channel polled by this selector.
   */
  private AbstractLocalChannelWrapper channel = null;

  /**
   * Initialize this selector with the specified channel.
   * @param channel the channel polled by this selector.
   */
  public LocalChannelSelector(final ChannelWrapper<?> channel)
  {
    this.channel = (AbstractLocalChannelWrapper) channel;
  }

  @Override
  public boolean select()
  {
    return select(0);
  }

  @Override
  public boolean select(final long timeout)
  {
    if (timeout < 0L) throw new IllegalArgumentException("timeout must be >= 0");
    //long start = System.currentTimeMillis();
    long start = System.nanoTime();
    final long timeoutNanos = timeout * 1000000L;
    long elapsed = 0;
    boolean selected =  channel.isSelectable();
    //while (((timeout == 0L) || (elapsed < timeout)) && !selected)
    while (((timeout == 0L) || (elapsed < timeoutNanos)) && !selected)
    {
      //goToSleep(timeout == 0L ? 0L : timeout - elapsed);
      goToSleep(1000L, 0);
      //elapsed = System.currentTimeMillis() - start;
      elapsed = System.nanoTime() - start;
      selected = channel.isSelectable();
    }
    return selected;
  }

  @Override
  public boolean selectNow()
  {
    return channel.isSelectable();
  }

  @Override
  public ChannelWrapper<?> getChannel()
  {
    return channel;
  }
}
