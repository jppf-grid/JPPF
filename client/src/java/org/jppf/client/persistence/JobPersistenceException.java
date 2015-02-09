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

package org.jppf.client.persistence;

import org.jppf.JPPFException;

/**
 * Exception thrown when an error occurs during a job persistence operation.
 * @author Laurent Cohen
 */
public class JobPersistenceException extends JPPFException
{
  /**
   * Initialize this exception with a specified message and cause exception.
   * @param message the message for this exception.
   * @param cause the cause exception.
   */
  public JobPersistenceException(final String message, final Throwable cause)
  {
    super(message, cause);
  }

  /**
   * Initialize this exception with a specified message.
   * @param message the message for this exception.
   */
  public JobPersistenceException(final String message)
  {
    super(message);
  }

  /**
   * Initialize this exception with a specified cause exception.
   * @param cause the cause exception.
   */
  public JobPersistenceException(final Throwable cause)
  {
    super(cause);
  }
}
