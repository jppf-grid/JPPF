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

package org.jppf.jca.demo;

import org.jppf.node.protocol.AbstractTask;

/**
 * Demonstration task to test the resource adaptor.
 * @author Laurent Cohen
 */
public class DemoTask extends AbstractTask<String> {
  /**
   * Explicit serialVersionUID.
   */
  private static final long serialVersionUID = -6106765904127535863L;
  /**
   * Duration of this task in milliseconds.
   */
  private final long duration;

  /**
   * Initialize this task withe specified duration.
   * @param duration duration of this task in milliseconds.
   */
  public DemoTask(final long duration) {
    this.duration = duration < 1L ? 1L : duration;
  }

  /**
   * Run this task.
   */
  @Override
  public void run() {
    String execType = isInNode() ? "remotely" : "locally";
    long start = System.nanoTime();
    try {
      synchronized(this) {
        wait(duration);
      }
      double elapsed = (System.nanoTime() - start) / 1e9d;
      String s = String.format("JPPF task [%s] successfully completed %s after %.3f seconds", getId(), execType, elapsed);
      System.out.println(s);
      setResult(s);
    } catch (InterruptedException e) {
      double elapsed = (System.nanoTime() - start) / 1e9d;
      setThrowable(e);
      String s = String.format("Exception for task [%s] executed %s after %.3f seconds : %s: %s", getId(), execType, elapsed, e.getClass().getName(), e.getMessage());
      setResult(s);
      System.out.println(s);
    }
  }
}
