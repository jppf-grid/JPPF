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
package org.jppf;

/**
 * Class of JPPF-specific error that may be caught in special cases.
 * The goal of this class is to provide an unchecked exception, allowing a quick
 * propagation up the call stack, while still allowing it to be caught specifically, in case the
 * application chooses not to exit, in response to the problem.
 * @author Laurent Cohen
 */
public class JPPFError extends Error
{
  /**
   * Initialize this error with a specified message and cause exception.
   * @param message the message for this error.
   * @param cause the cause exception.
   */
  public JPPFError(final String message, final Throwable cause)
  {
    super(message, cause);
  }

  /**
   * Initialize this error with a specified message.
   * @param message the message for this error.
   */
  public JPPFError(final String message)
  {
    super(message);
  }

  /**
   * Initialize this error with a specified cause exception.
   * @param cause the cause exception.
   */
  public JPPFError(final Throwable cause)
  {
    super(cause);
  }
}
