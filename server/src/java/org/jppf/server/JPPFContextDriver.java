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

package org.jppf.server;

import org.jppf.queue.JPPFQueue;
import org.jppf.server.scheduler.bundle.JPPFContext;

/**
 * Context associated with a driver.
 * @author Martin JANDA
 */
public class JPPFContextDriver extends JPPFContext
{
  /**
   * Reference to the job queue.
   */
  private final JPPFQueue queue;

  /**
   * Default initializer.
   * @param queue        the reference queue to use.
   */
  public JPPFContextDriver(final JPPFQueue queue)
  {
    if (queue == null) throw new IllegalArgumentException("queue is null");

    this.queue = queue;
  }

  @Override
  public int getMaxBundleSize()
  {
    return queue.getMaxBundleSize();
  }
}
