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

import org.apache.commons.httpclient.params.*;

/**
 * Factory used to set the default parameters for the http client.
 */
public class JPPFHttpDefaultParamsFactory extends DefaultHttpParamsFactory
{
  /**
   * HTTP socket connection timeout in milliseconds.
   */
  private static int socketTimeout = 3000;
  /**
   * Maximum number of retries when an HTTP connection fails.
   */
  private static int maxConnectionRetries = 2;

  /**
   * Get the default set of parameters.
   * @return an <code>HttpParams</code> instance.
   * @see org.apache.commons.httpclient.params.DefaultHttpParamsFactory#getDefaultParams()
   */
  @Override
  public synchronized HttpParams getDefaultParams()
  {
    HttpParams params = super.getDefaultParams();
    params.setParameter("http.socket.timeout", Integer.valueOf(socketTimeout));
    params.setParameter("http.method.retry-handler", new JPPFMethodExceptionHandler(maxConnectionRetries));
    return params;
  }

  /**
   * Get the HTTP socket connection timeout in milliseconds.
   * @return the timeout as an int.
   */
  public static int getSocketTimeout()
  {
    return socketTimeout;
  }

  /**
   * Set the HTTP socket connection timeout in milliseconds.
   * @param socketTimeout the timeout as an int.
   */
  public static void setSocketTimeout(final int socketTimeout)
  {
    JPPFHttpDefaultParamsFactory.socketTimeout = socketTimeout;
  }

  /**
   * Get the maximum number of retries when an HTTP connection fails.
   * @return the number of retries as an int.
   */
  public static int getMaxConnectionRetries()
  {
    return maxConnectionRetries;
  }

  /**
   * Set the maximum number of retries when an HTTP connection fails.
   * @param maxConnectionRetries the number of retries as an int.
   */
  public static void setMaxConnectionRetries(final int maxConnectionRetries)
  {
    JPPFHttpDefaultParamsFactory.maxConnectionRetries = maxConnectionRetries;
  }
}
