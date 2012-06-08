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

package org.jppf.server;

import org.jppf.server.scheduler.bundle.JPPFContext;

/**
 * Context associated with a driver.
 * @author Martin JANDA
 */
public class JPPFContextDriver extends JPPFContext
{
  /**
   * Singleton instance of the JPPFContextDriver.
   */
  private static JPPFContextDriver instance = new JPPFContextDriver();

  /**
   * Default initializer.
   */
  protected JPPFContextDriver()
  {
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public int getMaxBundleSize()
  {
    return JPPFDriver.getQueue().getMaxBundleSize();
  }

  /**
   * Get the singleton instance of the JPPFContextDriver.
   * @return a <code>JPPFContextDriver</code> instance.
   */
  public static JPPFContextDriver getInstance()
  {
    return instance;
  }
}
