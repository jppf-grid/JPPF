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

package org.jppf.example.startup.client;

import org.jppf.startup.JPPFClientStartupSPI;


/**
 * This is a test of a driver startup class.
 * @author Laurent Cohen
 */
public class TestClientStartup implements JPPFClientStartupSPI
{
  /**
   * This is a test of a client startup class.
   * @see java.lang.Runnable#run()
   */
  @Override
  public void run()
  {
    System.out.println("I'm a client startup class");
  }
}
