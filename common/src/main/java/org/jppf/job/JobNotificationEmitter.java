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

package org.jppf.job;

/**
 * Notification interface for job manager events.
 * @author Martin JANDA
 * @exclude
 */
public interface JobNotificationEmitter {
  /**
   * Fire job listener event.
   * @param event the event to be fired.
   */
  void fireJobEvent(final JobNotification event);

  /**
   * Get the uuid asosciated with this job notifications emitter, which allows to know where the notifications come from.
   * @return a string uuid for this emitter.
   * @since 5.1
   */
  String getEmitterUuid();
}
