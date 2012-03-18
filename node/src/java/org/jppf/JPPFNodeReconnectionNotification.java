/*
 * JPPF.
 * Copyright (C) 2005-2012 JPPF Team.
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
 * This error is thrown to notify a node that its code is obsolete and it should dynamically reload itself.
 * @author Laurent Cohen
 * @exclude
 */
public class JPPFNodeReconnectionNotification extends JPPFError
{
  /**
   * Initialize this notification with a specified message.
   * @param message a text message indicating the reason for this notification.
   */
  public JPPFNodeReconnectionNotification(final String message)
  {
    super(message);
  }

  /**
   * Initialize this error with a specified message and cause exception.
   * @param message the message for this error.
   * @param cause the cause exception.
   */
  public JPPFNodeReconnectionNotification(final String message, final Throwable cause)
  {
    super(message, cause);
  }

  /**
   * Initialize this error with a specified cause exception.
   * @param cause the cause exception.
   */
  public JPPFNodeReconnectionNotification(final Throwable cause)
  {
    super(cause);
  }
}
