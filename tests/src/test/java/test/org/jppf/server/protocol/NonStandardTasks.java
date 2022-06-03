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

import java.io.Serializable;
import java.util.concurrent.Callable;

import org.jppf.node.protocol.JPPFRunnable;
import org.jppf.utils.*;

/**
 * 
 * @author Laurent Cohen
 */
public class NonStandardTasks {
  /**
   * @param resultMessage .
   * @return a JPPFRunnableTask.
   */
  public static JPPFRunnableTask getRunnableTaskAsLambda(final String resultMessage) {
    return () -> System.out.println(resultMessage);
  }

  /**
   * @param resultMessage .
   * @return a JPPFCallable .
   */
  public static JPPFCallable<String> getCallableTaskAsLambda(final String resultMessage) {
    return () -> resultMessage;
  }

  /** */
  public static class AnnotatedStaticMethodTask {
    /**
     * @param param .
     * @return .
     */
    @JPPFRunnable
    public static String staticMethod(final String param) {
      return "task ended for param " + param;
    }
  }

  /** */
  public static class AnnotatedInstanceMethodTask implements Serializable {
    /** Explicit serialVersionUID. */
    private static final long serialVersionUID = 1L;
    /** */
    public String result;
  
    /**
     * @param param .
     * @return .
     */
    @JPPFRunnable
    public String instanceMethod(final String param) {
      result = "task ended for param " + param;
      return result;
    }
  }

  /** */
  public static class AnnotatedConstructorTask implements Serializable {
    /** Explicit serialVersionUID. */
    private static final long serialVersionUID = 1L;
    /** */
    public final String result;
  
    /**
     * @param param .
     */
    @JPPFRunnable
    public AnnotatedConstructorTask(final String param) {
      this.result = "task ended for param " + param;
    }
  }

  /** */
  public static class PojoTask implements Serializable {
    /** Explicit serialVersionUID. */
    private static final long serialVersionUID = 1L;
    /** */
    public String result;
  
    /** */
    public PojoTask() {
    }
  
    /**
     * @param param .
     */
    public PojoTask(final String param) {
      this.result = "constructor result for param " + param;
    }
  
    /**
     * @param param .
     * @return .
     */
    public static String staticMethod(final String param) {
      return "static result for param " + param;
    }
  
    /**
     * @param param .
     * @return .
     */
    public String instanceMethod(final String param) {
      result = "instance result for param " + param;
      return result;
    }
  }

  /** */
  public static class RunnableTask implements Runnable, Serializable {
    /** Explicit serialVersionUID. */
    private static final long serialVersionUID = 1L;
    /** */
    public String result;
    /** */
    private final String param;
  
    /**
     * @param param .
     */
    public RunnableTask(final String param) {
      this.result = "constructor result for param " + param;
      this.param = param;
    }
  
    @Override
    public void run() {
      result = "runnable result for param " + param;
    }
  }

  /** */
  public static class CallableTask implements Callable<String>, Serializable {
    /** Explicit serialVersionUID. */
    private static final long serialVersionUID = 1L;
    /** */
    public String result;
    /** */
    private final String param;
  
    /**
     * @param param .
     */
    public CallableTask(final String param) {
      this.result = "constructor result for param " + param;
      this.param = param;
    }
  
    @Override
    public String call() {
      result = "callable result for param " + param;
      return result;
    }
  }

}
