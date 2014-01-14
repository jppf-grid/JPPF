/*
 * JPPF.
 * Copyright (C) 2005-2014 JPPF Team.
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

package org.jppf.execute;

import java.util.EventObject;

/**
 * Event sent to notify of a status change for a client connection.
 * @author Martin JANDA
 */
public class ExecutorChannelStatusEvent extends EventObject
{
  /**
   * Explicit serialVersionUID.
   */
  private static final long serialVersionUID = 1L;
  /**
   * The channel status before the change.
   */
  private final ExecutorStatus oldValue;
  /**
   * The channel status after the change.
   */
  private final ExecutorStatus newValue;

  /**
   * Initialize this event with a channel as source.
   * @param source the event source.
   * @param oldValue the channel status before the change.
   * @param newValue the channel status after the change.
   */
  public ExecutorChannelStatusEvent(final Object source, final ExecutorStatus oldValue, final ExecutorStatus newValue)
  {
    super(source);

    this.oldValue = oldValue;
    this.newValue = newValue;
  }

  /**
   * Get the channel status before the change.
   * @return a {@link ExecutorStatus} enum value.
   */
  public ExecutorStatus getOldValue()
  {
    return oldValue;
  }

  /**
   * Get the channel status after the change.
   * @return a {@link ExecutorStatus} enum value.
   */
  public ExecutorStatus getNewValue()
  {
    return newValue;
  }

  @Override
  public String toString()
  {
    final StringBuilder sb = new StringBuilder();
    sb.append("ExecutorChannelStatusEvent");
    sb.append("{source=").append(getSource());
    sb.append(", oldValue=").append(oldValue);
    sb.append(", newValue=").append(newValue);
    sb.append('}');
    return sb.toString();
  }
}
