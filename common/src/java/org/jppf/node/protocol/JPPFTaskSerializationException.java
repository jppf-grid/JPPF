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
public class JPPFTaskSerializationException extends Throwable {
  /**
   * Initialize this exception.
   * @param message the exception message.
   * @param stackTrace the stack trace to set onto this exception.
   */
  public JPPFTaskSerializationException(final String message, final StackTraceElement[] stackTrace) {
    super(message, null, false, true);
    if (stackTrace != null) setStackTrace(stackTrace);
  }

  /**
   * Initialize this exception from the specified {@code Throwable}.
   * @param throwable the {@code Throwable} to get information from.
   */
  public JPPFTaskSerializationException(final Throwable throwable) {
    super(String.format("[%s: %s]", throwable.getClass().getName(), throwable.getMessage()), null, false, true);
    StackTraceElement[] stackTrace = throwable.getStackTrace();
    if (stackTrace != null) setStackTrace(stackTrace);
  }
}
