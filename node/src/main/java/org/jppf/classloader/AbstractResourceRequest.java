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

package org.jppf.classloader;

/**
 * Encapsulates a remote resource request submitted asynchronously
 * via the single-thread executor.
 */
abstract class AbstractResourceRequest implements ResourceRequestRunner {
  /**
   * Used to collect any throwable raised during communication with the server.
   */
  protected Throwable throwable;
  /**
   * The request to send.
   */
  protected JPPFResourceWrapper request;
  /**
   * The response received.
   */
  protected JPPFResourceWrapper response;

  /**
   * Initialize with the specified request.
   */
  public AbstractResourceRequest() {
  }

  @Override
  public Throwable getThrowable() {
    return throwable;
  }

  @Override
  public void reset() {
    this.request = null;
    this.response = null;
    this.throwable = null;
  }

  @Override
  public void setRequest(final JPPFResourceWrapper request) {
    this.request = request;
  }

  @Override
  public JPPFResourceWrapper getResponse() {
    return response;
  }
}
