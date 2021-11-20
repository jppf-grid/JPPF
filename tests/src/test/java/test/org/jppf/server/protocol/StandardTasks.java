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

package test.org.jppf.server.protocol;

import java.util.concurrent.Callable;

import org.jppf.node.protocol.AbstractTask;
import org.jppf.utils.JPPFCallable;

import test.org.jppf.test.setup.common.LifeCycleTask;

/**
 * 
 * @author Laurent Cohen
 */
public class StandardTasks {

  /**
   * A simple <code>JPPFCallable</code>.
   */
  public static class MyComputeCallable implements JPPFCallable<String> {
    /** */
    private static final long serialVersionUID = 1L;
  
    @Override
    public String call() throws Exception {
      System.out.printf("result of MyCallable.call() = %s\n", TestJPPFTask.callableResult);
      return TestJPPFTask.callableResult;
    }
  }

  /**
   * A <code>JPPFCallable</code> whixh throws an exception in its <code>call()</code> method..
   */
  public static class MyExceptionalCallable implements JPPFCallable<String> {
    /** */
    private static final long serialVersionUID = 1L;
  
    @Override
    public String call() throws Exception {
      throw new UnsupportedOperationException("this exception is thrown intentionally");
    }
  }

  /**
   * An extension of LifeCycleTask which sets the result before calling {code super.run()}.
   */
  public static class MyTask extends LifeCycleTask {
    /** */
    private static final long serialVersionUID = 1L;
  
    /**
     * Initialize this task.
     * @param duration the  task duration.
     */
    public MyTask(final long duration) {
      super(duration);
    }
  
    @Override
    public void run() {
      setResult("result is set");
      super.run();
    }
  }

  /** */
  public static class NotifyingTask extends AbstractTask<Object> {
    /** */
    private static final long serialVersionUID = 1L;
  
    @Override
    public void run() {
      for (int i=1; i<=3; i++) {
        final int n = i;
        fireNotification((Callable<Object>) () -> "task notification " + n, false);
      }
    }
  }

  /** */
  public static class NotifyingTask2 extends AbstractTask<Object> {
    /** */
    private static final long serialVersionUID = 1L;
  
    @Override
    public void run() {
      fireNotification(getId() + "#1", true);
      fireNotification(getId() + "#2", false);
      fireNotification(getId() + "#3", true);
    }
  }

  /** */
  public static class CounterResetTask extends AbstractTask<Object> {
    /** */
    private static final long serialVersionUID = 1L;
  
    @Override
    public void run() {
      ResubmittingTask.counter = null;
    }
  }

}
