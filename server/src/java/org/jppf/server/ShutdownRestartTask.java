/*
 * JPPF.
 * Copyright (C) 2005-2017 JPPF Team.
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

import java.util.*;

import org.jppf.JPPFError;
import org.jppf.utils.*;
import org.jppf.utils.concurrent.ThreadSynchronization;
import org.jppf.utils.configuration.JPPFProperties;
import org.slf4j.*;

/**
 * Task used by a timer to shutdown, and eventually restart, this server.<br>
 * Both shutdown and restart operations can be performed with a specified delay.
 * @exclude
 */
class ShutdownRestartTask extends TimerTask {
  /**
   * Logger for this class.
   */
  static Logger log = LoggerFactory.getLogger(ShutdownRestartTask.class);
  /**
   * Determines whether the server should restart after shutdown is complete.
   */
  private final boolean restart;
  /**
   * Delay, starting from shutdown completion, after which the server is restarted.
   */
  private final long restartDelay;
  /**
   * Reference to the driver.
   */
  private final JPPFDriver driver;
  /**
   * Synchronization lock.
   */
  private final ThreadSynchronization lock = new ThreadSynchronization();

  /**
   * Initialize this task with the specified parameters.<br>
   * The shutdown is initiated after the specified shutdown delay has expired.<br>
   * If the restart parameter is set to false then the JVM exits after the shutdown is complete.
   * @param restart determines whether the server should restart after shutdown is complete.
   * If set to false, then the JVM will exit.
   * @param restartDelay delay, starting from shutdown completion, after which the server is restarted.
   * A value of 0 or less means the server is restarted immediately after the shutdown is complete.
   * @param driver reference to the driver.
   */
  public ShutdownRestartTask(final boolean restart, final long restartDelay, final JPPFDriver driver) {
    if (driver == null) throw new IllegalArgumentException("driver is null");
    this.restart = restart;
    this.restartDelay = restartDelay;
    this.driver = driver;
  }

  /**
   * Perform the actual shutdown, and eventually restart, as specified in the constructor.
   */
  @Override
  public void run() {
    cancel();
    if (driver.shuttingDown.compareAndSet(false, true)) {
      log.info("Initiating shutdown");
      driver.shutdown();
      if (JPPFConfiguration.get(JPPFProperties.SERVER_EXIT_ON_SHUTDOWN)) {
        if (!restart) {
          log.info("Performing requested exit");
          System.exit(0);
        } else {
          try {
            lock.goToSleep(restartDelay);
            log.info("Initiating restart");
            System.exit(2);
          } catch (final Exception e) {
            log.error(e.getMessage(), e);
            throw new JPPFError("Could not restart the JPPFDriver");
          }
        }
      }
    }
  }
}
