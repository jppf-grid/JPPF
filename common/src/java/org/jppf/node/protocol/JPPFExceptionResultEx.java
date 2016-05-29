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

package org.jppf.node.protocol;

/**
 *
 * @author Laurent Cohen
 */
public class JPPFExceptionResultEx extends JPPFExceptionResult {
  /**
   * Explicit serialVersionUID.
   */
  private static final long serialVersionUID = 1L;
  /**
   * These fields describe the {@code Throwable} object.
   */
  protected String throwableMessage, throwableClassName;
  /**
   * The throwable object's stack trace.
   */
  protected StackTraceElement[] throwableStackTrace;

  /**
   * Default constructor provided as a convenience for subclassing.
   */
  public JPPFExceptionResultEx() {
    super();
  }

  /**
   * Initialize this task with the specified error context.
   * @param throwable the throwable that is to be captured.
   * @param object the object on which the throwable applies.
   */
  public JPPFExceptionResultEx(final Throwable throwable, final Object object) {
    super(null, object);
    if (throwable != null) {
      throwableMessage = throwable.getMessage();
      throwableStackTrace = throwable.getStackTrace();
      throwableClassName = throwable.getClass().getName();
    }
  }

  /**
   * Get the message of the throwable.
   * @return the message as a string.
   */
  public String getThrowableMessage() {
    return throwableMessage;
  }

  /**
   * Get the stack trace of the throwable.
   * @return the stack trace as an array of {@code StackTraceElement}s.
   */
  public StackTraceElement[] getThrowableStackTrace() {
    return throwableStackTrace;
  }

  /**
   * Get the class name of the throwable.
   * @return the class name as a string.
   */
  public String getThrowableClassName() {
    return throwableClassName;
  }
}
