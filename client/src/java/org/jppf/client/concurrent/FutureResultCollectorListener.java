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

package org.jppf.client.concurrent;

import java.util.EventListener;

/**
 * Listener interface for {@link FutureResultCollector} events.
 * @author Laurent Cohen
 * @exclude
 */
interface FutureResultCollectorListener extends EventListener
{
  /**
   * Called some results from a job have been received.
   * @param event the event object.
   */
  void resultsReceived(FutureResultCollectorEvent event);

  /**
   * Called when all results from a job have been received.
   * @param event the event object.
   */
  void resultsComplete(FutureResultCollectorEvent event);
}
