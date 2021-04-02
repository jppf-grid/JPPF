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

package org.jppf.management;

import java.lang.management.ManagementFactory;
import java.util.concurrent.locks.*;

import javax.management.*;

import org.slf4j.*;

/**
 * Utility methods to obain and release {@link MBeanServer} instances for embedded nodes. 
 * @author Laurent Cohen
 */
public final class JPPFMBeanServerFactory {
  /**
   * Logger for this class.
   */
  private static final Logger log = LoggerFactory.getLogger(JPPFMBeanServerFactory.class);
  /**
   * Whether the platform mbean server is available, that is, whether no other node uses it.
   */
  private static boolean defaultMBeanServerAvailable = true;
  /**
   * Used to synchronize access tot he methods in this class.
   */
  private static final Lock lock = new ReentrantLock();

  /**
   * No instantiation permitted.
   */
  private JPPFMBeanServerFactory() {
  }

  /**
   * Obtain an MBeanServer.
   * @return an {@link MBeanServer} instance.
   */
  public static MBeanServer getMBeanServer() {
    lock.lock();
    try {
      if (defaultMBeanServerAvailable) {
        defaultMBeanServerAvailable = false;
        return ManagementFactory.getPlatformMBeanServer();
      } else {
        return MBeanServerFactory.createMBeanServer();
      }
    } finally {
      lock.unlock();
    }
  }

  /**
   * Release the specified MBeanServer.
   * @param server the {@link MBeanServer} to remove/release.
   */
  public static void releaseMBeanServer(final MBeanServer server) {
    lock.lock();
    try {
      if (server == ManagementFactory.getPlatformMBeanServer()) defaultMBeanServerAvailable = true;
      else MBeanServerFactory.releaseMBeanServer(server);
    } catch(final Exception e) {
      log.error("error releasing {}", server, e);
    } finally {
      lock.unlock();
    }
  }

  /**
   * Obtain a string representation of the specified {@link MBeanServer}.
   * @param server the server to represent as a string.
   * @return a string repressenttion of the server.
   */
  public static String toString(final MBeanServer server) {
    if (server == null) return null;
    else if (server == ManagementFactory.getPlatformMBeanServer()) return "platform mbean server";
    return server.toString();
  }
}
