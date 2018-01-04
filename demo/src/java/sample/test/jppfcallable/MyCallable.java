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

package sample.test.jppfcallable;

import org.jppf.utils.JPPFCallable;
import org.slf4j.*;

/**
 * 
 */
public class MyCallable implements JPPFCallable<String> {
  /**
   * Logger for this class.
   */
  static Logger log = LoggerFactory.getLogger(MyTask.class);
  /**
   * 
   */
  private final String id;
  /**
   * 
   */
  @SuppressWarnings("unused")
  private byte[] data = null;
  /**
   * Size of the data to create.
   */
  private final int size;
  /**
   * Duration of this callable.
   */
  private final long time;

  /**
   * 
   * @param id the id of the task.
   * @param time the duration of the callable.
   * @param size the size of the data to create.
   */
  public MyCallable(final String id, final long time, final int size) {
    this.id = id;
    this.time = time;
    this.size = size;
  }

  @Override
  public String call() throws Exception {
    data = new byte[size > 0 ? size : 0];
    if (time > 0L) {
      synchronized (this) {
        wait(time);
      }
    }
    //throw new RuntimeException();
    //throw new Error();
    final String s = id + " : OK";
    //log.info("callable " + s);
    return s;
  }
}
