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

import org.jppf.utils.ExceptionUtils;
import org.slf4j.*;

/** */
public class MyFutureTask extends AbstractFutureTask<MyResult> {
  /** */
  private static final Logger logger = LoggerFactory.getLogger(MyFutureTask.class);
  /** */
  private static final long serialVersionUID = 1L;
  /** */
  private final TaskData data;

  /**
   * @param data .
   */
  public MyFutureTask(final TaskData data) {
    this.data = data;
    setInterruptible(false);
  }

  @Override
  public void execute() {
    try {
      Thread.sleep(5000L);
      setResult(new MyResult(new byte[10], null));
      System.out.println("execution successful");
    } catch (Exception e) {
      System.out.println("execution failed:" + ExceptionUtils.getStackTrace(e));
      setResult(new MyResult(null, e));
      //setThrowable(e);
    }
  }

  @Override
  public void onCancel() {
    System.out.println("this task has been cancelled");
  }
}
