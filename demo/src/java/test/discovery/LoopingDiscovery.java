/*
 * JPPF.
 * Copyright (C) 2005-2016 JPPF Team.
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

package test.discovery;

import java.util.*;

import org.jppf.discovery.*;

/** */
public class LoopingDiscovery extends ClientDriverDiscovery {
  /**
   * Whether this discovery was shutdown.
   */
  private boolean shutdownFlag = false;

  @Override
  public void discover() throws InterruptedException {
    while (!isShutdown()) {
      List<ClientConnectionPoolInfo> drivers = externalLookup();
      if (drivers != null) {
        for (ClientConnectionPoolInfo driver: drivers) {
          newConnection(driver);
        }
      }
      // wait 5s before the next lookup
      synchronized(this) {
        wait(5000L);
      }
    }
  }

  /**
   * Query an external service for discovered drivers.
   * @return the looked up driver info objects.
   */
  public List<ClientConnectionPoolInfo> externalLookup() {
    return new ArrayList<>();
  }

  /**
   * Determine whether this discovery was shutdown.
   * @return true if this discovery was shutdown, false otherwise.
   */
  public synchronized boolean isShutdown() {
    return shutdownFlag;
  }

  @Override
  public synchronized void shutdown() {
    shutdownFlag = false;
    // wake up the discover() thread
    notify();
  }
}
