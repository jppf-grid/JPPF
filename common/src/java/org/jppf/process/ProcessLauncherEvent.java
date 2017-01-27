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

package org.jppf.process;

import java.util.EventObject;

/**
 * Instances of this class represent notifications that a slave process was started or terminated.
 * @author Laurent Cohen
 * @since 4.1
 * @exclude
 */
public class ProcessLauncherEvent extends EventObject {
  /**
   * Intiialize this event with the specified process launcher as source.
   * @param source the event source.
   */
  public ProcessLauncherEvent(final AbstractProcessLauncher source) {
    super(source);
  }

  /**
   * Convenience method to return the event source as a {@link AbstractProcessLauncher}.
   * @return a {@link AbstractProcessLauncher} instance.
   */
  public AbstractProcessLauncher getProcessLauncher() {
    return (AbstractProcessLauncher) getSource();
  }
}
