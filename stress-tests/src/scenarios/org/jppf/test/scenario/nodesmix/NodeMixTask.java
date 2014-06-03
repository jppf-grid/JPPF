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

package org.jppf.test.scenario.nodesmix;

import org.jppf.node.protocol.AbstractTask;

/**
 * 
 * @author Laurent Cohen
 */
public class NodeMixTask extends AbstractTask<String> {
  /**
   * The duration of this task.
   */
  private final long duration;

  /**
   * Intiialize this task with the specified duration.
   * @param duration the duration of this task.
   */
  public NodeMixTask(final long duration) {
    this.duration = duration;
  }

  @Override
  public void run() {
    try {
      if (duration > 0L) Thread.sleep(duration);
      setResult("ok");
    } catch (Exception e) {
      setThrowable(e);
      setResult("ko");
    }
  }
}
