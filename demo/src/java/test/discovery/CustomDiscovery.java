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

import org.jppf.discovery.*;

/**
 * 
 * @author Laurent Cohen
 */
public class CustomDiscovery extends ClientDriverDiscovery {
  /**
   * Whether this discovery was shutdown.
   */
  private boolean shutdownFlag = false;

  /**
   * 
   */
  public CustomDiscovery() {
    System.out.printf("in %s() contructor%n", getClass().getSimpleName());
  }

  @Override
  public void discover() {
    String className = getClass().getSimpleName();
    for (int i=0; i<2; i++) {
      int port = 11111 + i;
      ClientConnectionPoolInfo info = new ClientConnectionPoolInfo(className + "_" + port, "localhost", port);
      System.out.printf("%s 'discovering' %s%n", className, info);
      newConnection(info);
    }
    try {
      Thread.sleep(Integer.MAX_VALUE);
    } catch (InterruptedException e) {
      synchronized(this) {
        if (shutdownFlag) System.out.println("discovery thread interrupted after shutdown: " + e);
        else e.printStackTrace();
      }
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  @Override
  public void shutdown() {
    synchronized(this) {
      shutdownFlag = true;
    }
  }
}
