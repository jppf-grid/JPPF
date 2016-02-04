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

import java.text.DecimalFormat;

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
   * Duration of this task in seconds.
   */
  private long duration = 1;

  /**
   * Initialize this task withe specified duration.
   * @param duration duration of this task in milliseconds.
   */
  public DemoTask(final long duration) {
    this.duration = duration;
  }

  /**
   * Run this task.
   * @see java.lang.Runnable#run()
   */
  @Override
  public void run() {
    DecimalFormat nf = new DecimalFormat("0.###");
    String res = nf.format(duration / 1000.0f);
    try {
      Thread.sleep(duration);
      String s = "JPPF task [" + getId() + "] successfully completed after " + res + " seconds";
      System.out.println(s);
      setResult(s);
    } catch (InterruptedException e) {
      setThrowable(e);
      setResult("Exception for task [" + getId() + "] with specified duration of " + res + " seconds: " + e.getMessage());
    }
  }
}
