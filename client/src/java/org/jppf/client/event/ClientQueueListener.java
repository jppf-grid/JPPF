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

package org.jppf.client.event;

import java.util.EventListener;

/**
 * Interface for classes that wish to be notified of jobs added to or removed from the job queue. 
 * @author Laurent Cohen
 * @since 4.1
 */
public interface ClientQueueListener extends EventListener {
  /**
   * Called to notify that a job was added to the queue.
   * @param event the event to notify of. 
   */
  void jobAdded(ClientQueueEvent event);

  /**
   * Called to notify that a job was removed from the queue.
   * @param event the event to notify of. 
   */
  void jobRemoved(ClientQueueEvent event);
}
