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

package sample.test.jppfcallable;

import org.jppf.node.protocol.AbstractTask;
import org.jppf.utils.ExceptionUtils;
import org.slf4j.*;


/**
 * 
 */
public class MyTask extends AbstractTask<String>
{
  /**
   * Logger for this class.
   */
  private static Logger log = LoggerFactory.getLogger(MyTask.class);
  /**
   * Determines whether the debug level is enabled in the log configuration, without the cost of a method call.
   */
  private static boolean debugEnabled = log.isDebugEnabled();
  /**
   * Duration of the callable.
   */
  private final long time;
  /**
   * Size of the data to create.
   */
  private final int size;

  /**
   * Intiialize this task with the specified callable duration.
   * @param time the duration of the callable.
   * @param size the size of the data to create.
   */
  public MyTask(final long time, final int size)
  {
    this.time = time;
    this.size = size;
  }

  @Override
  public void run()
  {
    try
    {
      MyCallable mc = new MyCallable(getId(), time, size);
      String s = compute(mc);
      //System.out.println("[node] result of MyCallable[id=" + getId() + "].call() = " + s);
      setResult(s);
      if (debugEnabled) log.debug(s);
    }
    catch (Throwable t)
    {
      //t.printStackTrace();
      setThrowable(t);
      log.error(getId() + " : " + ExceptionUtils.getMessage(t), t);
    }
  }
}
