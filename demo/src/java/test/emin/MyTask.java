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

package test.emin;

import org.jppf.node.protocol.*;
import org.slf4j.*;

/** */
public class MyTask extends AbstractTask<MyResult> implements Interruptibility {
  /** */
  private static final Logger logger = LoggerFactory.getLogger(MyTask.class);
  /** */
  private static final long serialVersionUID = 1L;
  /** */
  private final TaskData data;

  /**
   * @param data .
   */
  public MyTask(final TaskData data) {
    this.data = data;
  }

  @Override
  public void run() {
    print("Task " + getId() + " started");
    if (data == null) {
      print("task "  + getId() + " no data received");
      setResult(new MyResult(null, new IllegalArgumentException("No data received")));
      return;
    }
    // DO STUFF (more then 300ms)
    try {
      Thread.sleep(5000L);
    } catch (InterruptedException e) {
      print("Task " + getId()  + " interrupted");
    }
    setResult(new MyResult(new byte[10], null));
    print("Task " + getId() + " finished");
  }

  @Override
  public void onCancel() {
    print("Task " + getId() + " cancelled");
  }

  @Override
  public boolean isInterruptible() {
    return false;
  }

  /**
   * @param message .
   */
  private void print(final String message) {
    logger.info(message);
    System.out.println(message);
  }
}
