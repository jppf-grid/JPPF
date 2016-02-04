/*
 * JPPF.
 * Copyright (C) 2005-2016 JPPF Team.
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


/**
 * Interface for objects that act as an NIO selector for local (in-VM) channels.
 * @author Laurent Cohen
 */
public interface ChannelSelector
{
  /**
   * This method blocks until the state of the channel has changed.
   * @return true if the channel state changed.
   */
  boolean select();
  /**
   * This method blocks until the state of the channel has changed or the timeout expires, whichever happens first.
   * @param timeout the maximum duration in milliseconds for this operation.
   * @return true if the channel state changed.
   */
  boolean select(long timeout);
  /**
   * Determine whether the channel state has changed and return immediately.
   * @return true if the channel state changed.
   */
  boolean selectNow();
  /**
   * Wake up this selector. If the selector was not engaged in a blocking operation, this method has no effect.
   */
  void wakeUp();
  /**
   * Get the channel this selector is polling.
   * @return a {@link ChannelWrapper} instance.
   */
  ChannelWrapper<?> getChannel();
}
