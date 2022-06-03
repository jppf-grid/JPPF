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

package test.org.jppf.node.protocol;

import org.jppf.node.protocol.AbstractTask;

/**
 * A simple test task.
 */
public class MyTask2 extends AbstractTask<String> {
  /** */
  static final String HELLO_WORLD = "Hello, world";
  /** */
  static final String START_NOTIF = "MyTask2 started";
  /** */
  static final String END_NOTIF = "MyTask2 complete";
  /**
   * How long to sleep.
   */
  private final long duration;
  /**
   * Whether to send a JMX notification upon startup and completion of this task.
   */
  private final boolean notify;

  /**
   * @param duration how long to sleep.
   */
  public MyTask2(final long duration) {
    this(duration, false);
  }

  /**
   * @param duration how long to sleep.
   * @param notify whether to send a JMX notification upon startup and completion of this task.
   */
  public MyTask2(final long duration, final boolean notify) {
    this.duration = duration;
    this.notify = notify;
    setId("MyTask2");
  }

  @Override
  public void run() {
    System.out.printf("MyTask2: duration = %,d,  notify = %b\n", duration, notify);
    try {
      if (notify) {
        System.out.println("MyTask2: sending jmx start notification '" + START_NOTIF + "'");
        fireNotification(START_NOTIF, true);
      }
      if (duration > 0L) Thread.sleep(duration);
      setResult(HELLO_WORLD);
      if (notify) {
        System.out.println("MyTask2: sending jmx end notification '" + END_NOTIF + "'");
        fireNotification(END_NOTIF, true);
      }
    } catch (final InterruptedException e) {
      System.out.println("MyTask2  was cancelled on node " + getNode().getUuid() + "(" + e + ")");
    }
  }

  @Override
  public void onCancel() {
    System.out.println("MyTask2.onCancel(): task was cancelled on node " + getNode().getUuid());
  }
}