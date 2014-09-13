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

package org.jppf.example.webcrawler;

import java.io.IOException;

import org.apache.commons.httpclient.*;

/**
 * Retry handler used when a request results in an IO exception.
 * @author Laurent Cohen
 */
public class JPPFMethodExceptionHandler implements HttpMethodRetryHandler
{
  /**
   * the max retry count to use.
   */
  private int maxRetry = 1;

  /**
   * Initialize this exception handler with the specified maximum retry count.
   * @param maxRetry the max retry count to use.
   */
  public JPPFMethodExceptionHandler(final int maxRetry)
  {
    this.maxRetry = maxRetry;
  }

  /**
   * Determine whether a request should be resent.
   * @param method the http method for which the exception occurred.
   * @param e the exception that occurred.
   * @param retryCount number of times the request was resent.
   * @return true if the request should be resent, false otherwise.
   * @see org.apache.commons.httpclient.HttpMethodRetryHandler#retryMethod(org.apache.commons.httpclient.HttpMethod, java.io.IOException, int)
   */
  @Override
  public boolean retryMethod(final HttpMethod method, final IOException e, final int retryCount)
  {
    return retryCount < maxRetry;
  }
}
