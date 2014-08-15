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

package org.jppf.classloader;

/**
 * 
 * @author Laurent Cohen
 * @exclude
 */
interface ResourceRequestRunner extends Runnable
{

  /**
   * Get the throwable eventually raised during communication with the server.
   * @return a {@link Throwable} instance.
   */
  Throwable getThrowable();

  /**
   * Reset this request.
   */
  void reset();

  /**
   * Reset this request request.
   * @param request the request to send.
   */
  void setRequest(final JPPFResourceWrapper request);

  /**
   * Get the response received.
   * @return a {@link JPPFResourceWrapper} instance.
   */
  JPPFResourceWrapper getResponse();
}
