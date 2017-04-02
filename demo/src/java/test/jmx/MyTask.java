/*
 * JPPF.
 * Copyright (C) 2005-2015 JPPF Team.
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

package test.jmx;

import org.jppf.node.protocol.AbstractTask;
import org.jppf.utils.JPPFCallable;

/**
 * 
 * @author Laurent Cohen
 */
public class MyTask extends AbstractTask<String> {
  @Override
  public void run() {
    // ... some code ...

    // Execute some code on the client side via the ClientDataProvider
    //ClientDataProvider dataProvider = (ClientDataProvider) getDataProvider();
    //dataProvider.computeValue("computeResult", new MyCallable(this));
    try
    {
      compute(new MyCallable(this));
      // we can now cancel the job
      MyNodeListener.getInstance().cancelJob();
    }
    catch (Exception e)
    {
      setThrowable(e);
    }
  }

  /**
   * 
   */
  public static class MyCallable implements JPPFCallable<String> {
    /**
     * 
     */
    private MyTask task;

    /**
     * 
     * @param task .
     */
    public MyCallable(final MyTask task) {
      this.task = task;
    }

    // this method will be executed on the client side
    @Override
    public String call() throws Exception
    {
      try {
        // ... your code here ...
      } catch (Exception e) {
        return e.getMessage();
      }
      return "callable was successfully executed on the client side";
    }
  }
}
