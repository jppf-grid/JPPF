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

package org.jppf.node.protocol;

import org.jppf.utils.ExceptionUtils;

/**
 * Instances of this class are used to signal that a task could not be sent back by the node to the server.
 * <p>This generally happens when a task cannot be serialized after its execution, or if a data transformation is
 * applied and fails with an exception. An instance of this class captures the context of the error, including the exception
 * that occurred, the object's <code>toString()</code> descriptor and its class name.
 * <p>When such an error occurs, an instance of this class will be sent instead of the initial JPPF task.
 * @author Laurent Cohen
 */
public class JPPFExceptionResult extends AbstractTask<Object> {
  /**
   * Explicit serialVersionUID.
   */
  private static final long serialVersionUID = 1L;
  /**
   * This captures the result of ("" + object).
   */
  protected String objectDescriptor;
  /**
   * The fully qualified class name of the object that triggered the error.
   */
  protected String className;

  /**
   * Default constructor provided as a convenience for subclassing.
   */
  public JPPFExceptionResult() {
  }

  /**
   * Initialize this task with the specified error context.
   * @param throwable the throwable that is to be captured.
   * @param object the object on which the throwable applies.
   */
  public JPPFExceptionResult(final Throwable throwable, final Object object) {
    setThrowable(throwable);
    if (object instanceof String) {
      objectDescriptor = (String) object;
      className = "unknown class";
    } else {
      objectDescriptor = String.valueOf(object);
      className = (object != null) ? object.getClass().getName() : "unknown class";
    }
  }

  /**
   * Display the error context captured in this task.
   * @see java.lang.Runnable#run()
   */
  @Override
  public void run() {
    //System.out.println(toString());
  }

  /**
   * Construct a string representation of this object.
   * @return a string representing this JPPFExceptionResult.
   * @see java.lang.Object#toString()
   */
  @Override
  public String toString() {
    final StringBuilder sb = new StringBuilder();
    sb.append("Error occurred on object [").append(objectDescriptor).append("], class=").append(className);
    if (getThrowable() != null) sb.append(" :\n").append(ExceptionUtils.getStackTrace(getThrowable()));
    return sb.toString();
  }

  /**
   * Get a string describing the object on which the error occurred.
   * @return "null" if the task was null, the result of its <code>toString()</code> method otherwise.
   */
  public String getObjectDescriptor() {
    return objectDescriptor;
  }

  /**
   * Get the fully qualified class name of the intiial task object on which the error occurred.
   * @return "unknown class" if the task object was null, its class name otherwise.
   */
  public String getTaskClassName() {
    return className;
  }
}
