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

package sample.test.notifications;

import org.jppf.node.protocol.AbstractTask;

/**
 * 
 */
public class NotifyingTask extends AbstractTask<String> {
  /**
   * 
   */
  private final int nbNotifs;

  /**
   * 
   * @param nbNotifs .
   */
  public NotifyingTask(final int nbNotifs) {
    this.nbNotifs = nbNotifs;
  }

  @Override
  public void run() {
    for (int i=0; i<nbNotifs; i++) fireNotification(i, true);
    setResult("exec successful");
  }
}
