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

package org.jppf.node.idle;

import java.util.Timer;

import org.jppf.JPPFException;
import org.jppf.utils.*;
import org.jppf.utils.configuration.JPPFProperties;
import org.slf4j.*;

/**
 * This class detects that no mouse or keyboard has occurred for a specified time,
 * and performs a specified action when this happens.
 * @author Laurent Cohen
 */
public class IdleDetector implements Runnable {
  /**
   * Logger for this class.
   */
  private static final Logger log = LoggerFactory.getLogger(IdleDetector.class);
  /**
   * A timer that runs a periodic task that determines whether the computer is idle or not.
   */
  private Timer timer = null;
  /**
   * A factory that instantiates a platform-specific idle detector.
   */
  private IdleTimeDetectorFactory factory;
  /**
   * The timer task that polls the native APIs for idle time.
   */
  private IdleDetectionTask task;
  /**
   * The time of inactivity after which the system is considered idle, in milliseconds.
   */
  private long idleTimeout = -1L;
  /**
   * The interval between two successive calls to the native APIs to determine whether the system idle state has changed.
   */
  private long pollInterval = -1L;
  /**
   * Specifies the action to perform upon idle state changes.
   */
  private IdleStateListener listener;

  /**
   * Default constructor.
   * @param listener specifies the action to perform upon idle state changes.
   */
  public IdleDetector(final IdleStateListener listener) {
    this.listener = listener;
  }

  /**
   * Initialize this detector from the JPPF configuration
   * @throws Exception if any error occurs during initialization.
   */
  private void init() throws Exception {
    log.debug("initializing IdleTimeDetectorFactory");
    final TypedProperties config = JPPFConfiguration.getProperties();
    final String factoryName = "org.jppf.node.idle.IdleTimeDetectorFactoryImpl";
    idleTimeout = config.get(JPPFProperties.IDLE_TIMEOUT);
    pollInterval = config.get(JPPFProperties.IDLE_POLL_INTEFRVAL);
    final Class<?> c = Class.forName(factoryName);
    factory = (IdleTimeDetectorFactory) c.newInstance();
    log.debug("IdleTimeDetectorFactory initialized");
  }

  @Override
  public void run() {
    try {
      if (factory == null) init();
      final IdleStateListener tmp = new IdleStateListener() {
        @Override
        public void idleStateChanged(final IdleStateEvent event) {
          System.out.println("System is now " + event.getState());
        }
      };
      task = new IdleDetectionTask(factory, idleTimeout, listener, tmp);
      timer = new Timer(IdleDetector.class.getSimpleName() + " Timer");
      timer.schedule(task, 0L, pollInterval);
    } catch(final Exception e) {
      log.debug(e.getMessage(), e);
    }
  }

  /**
   * Main entry point.
   * @param args not used.
   */
  public static void main(final String[] args) {
    try {
      final TypedProperties config = JPPFConfiguration.getProperties();
      final String factoryName = config.getProperty("jppf.idle.detector.factory", null);
      if (factoryName == null) throw new JPPFException("Idle detector factory name not specified");
      final Class<?> c = Class.forName(factoryName);
      final IdleTimeDetectorFactory factory = (IdleTimeDetectorFactory) c.newInstance();
      final Timer timer = new Timer(IdleDetector.class.getSimpleName() + " Timer");
      final IdleDetectionTask task = new IdleDetectionTask(factory, 6000L);
      task.addIdleStateListener(new IdleStateListener() {
        @Override
        public void idleStateChanged(final IdleStateEvent event) {
          if (IdleState.IDLE.equals(event.getState())) System.out.println("System is now idle !");
          else System.out.println("System is now busy");
        }
      });
      timer.schedule(task, 0L, 200L);
      Thread.sleep(60000L);
    } catch (final Throwable t) {
      t.printStackTrace();
    }
  }
}
