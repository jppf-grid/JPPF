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

package org.jppf.io;

import java.io.IOException;

/**
 * Exception raised when an attempt to serialize an object results in an array of bytes (or file) larger than {@link java.lang.Integer#MAX_VALUE}.
 * @author Laurent Cohen
 */
public class SerializationOverflowException extends IOException {
  /**
   * Default constructor.
   */
  public SerializationOverflowException() {
    super();
  }

  /**
   * Initialize this exception with the specified message and cause.
   * @param message the message associated witht his exception.
   * @param cause the cause exception.
   */
  public SerializationOverflowException(final String message, final Throwable cause) {
    super(message, cause);
  }

  /**
   * Initialize this exception with the specified message.
   * @param message the message associated witht his exception.
   */
  public SerializationOverflowException(final String message) {
    super(message);
  }

  /**
   * Initialize this exception with the specified cause.
   * @param cause the cause exception.
   */
  public SerializationOverflowException(final Throwable cause) {
    super(cause);
  }
}
