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

import java.util.EventListener;

/**
 * Instances of this class listen to execution status change events on channels.
 * @author Martin JANDA
 * @exclude
 */
public interface ExecutorChannelStatusListener extends EventListener
{
  /**
   * Invoked to notify of a executions status change event on a channel.
   * @param event the event to notify of.
   */
  void executionStatusChanged(final ExecutorChannelStatusEvent event);
}
