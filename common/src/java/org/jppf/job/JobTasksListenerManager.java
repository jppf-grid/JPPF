/*
 * JPPF.
 * Copyright (C) 2005-2015 JPPF Team.
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
 * Implementations of this interface manage the registration and unregistration
 * of {@link JobTasksListener job tasks listeners} and notify these listeners of job tasks events.
 * @author Laurent Cohen
 */
@SuppressWarnings("deprecation")
public interface JobTasksListenerManager extends TaskReturnManager {
  /**
   * Add a listener to the list of job tasks listeners.
   * @param listener the listener to add to the list.
   */
  void addJobTasksListener(final JobTasksListener listener);

  /**
   * Remove a listener from the list of job tasks listeners.
   * @param listener the listener to remove from the list.
   */
  void removeJobTasksListener(final JobTasksListener listener);
}
