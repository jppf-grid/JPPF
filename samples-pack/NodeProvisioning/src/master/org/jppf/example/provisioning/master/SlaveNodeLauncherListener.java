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

package org.jppf.example.provisioning.master;

import java.util.EventListener;

/**
 * Listener interface that must be implemented by all classes wishing
 * to receive notifications of when a node is started or stopped.
 * @author Laurent Cohen
 */
public interface SlaveNodeLauncherListener extends EventListener {
  /**
   * Notifies that a process was started.
   * @param event encapsulates the process that was started.
   */
  void processStarted(SlaveNodeLauncherEvent event);

  /**
   * Notifies that a process was stopped.
   * @param event encapsulates the process that was stopped.
   */
  void processStopped(SlaveNodeLauncherEvent event);
}
